package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.games.GameTeamColors;
import com.eu.habbo.habbohotel.games.wired.WiredGame;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredFurniSourceHelper;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredUserSourceHelper;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectTeleportTeam extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.TELEPORT_TEAM;

    private final List<HabboItem> items = new ArrayList<>();
    private int walkMode = WiredEffectTeleport.WALK_CONTINUE_IF_NEAR;
    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    private int userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;

    public WiredEffectTeleportTeam(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectTeleportTeam(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    private GameTeamColors resolveTeamColor() {
        String name = this.getBaseItem() != null && this.getBaseItem().getName() != null
            ? this.getBaseItem().getName().toLowerCase()
            : "";
        if (name.contains("blue")) return GameTeamColors.BLUE;
        if (name.contains("green")) return GameTeamColors.GREEN;
        if (name.contains("yellow")) return GameTeamColors.YELLOW;
        return GameTeamColors.RED;
    }

    private boolean isHabboOnTeam(Habbo habbo, GameTeamColors targetColor) {
        if (habbo == null || habbo.getHabboInfo().getGamePlayer() == null) return false;
        return habbo.getHabboInfo().getGamePlayer().getTeamColor() == targetColor;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;

        THashSet<HabboItem> selectedSet = new THashSet<>(this.items);
        THashSet<HabboItem> resolvedFurnis = WiredFurniSourceHelper.resolveItems(this, this.furniSource, selectedSet, room, stuff);
        if (resolvedFurnis.isEmpty()) return false;
        List<HabboItem> destList = new ArrayList<>(resolvedFurnis);

        GameTeamColors teamColor = resolveTeamColor();
        List<Habbo> candidates = new ArrayList<>();

        if (this.userSource != WiredUserSourceHelper.USER_SOURCE_TRIGGERING) {
            for (Habbo h : WiredUserSourceHelper.resolveUsers(this, this.userSource, roomUnit, room, stuff)) {
                if (isHabboOnTeam(h, teamColor)) candidates.add(h);
            }
        } else {
            // If triggered by a specific user on that team, teleport them; if triggered without user (e.g. timer/game), teleport all room users on that team!
            Habbo triggerHabbo = roomUnit != null ? room.getHabbo(roomUnit) : null;
            if (triggerHabbo != null) {
                if (isHabboOnTeam(triggerHabbo, teamColor)) candidates.add(triggerHabbo);
            } else {
                for (Habbo h : room.getHabbos()) {
                    if (isHabboOnTeam(h, teamColor)) candidates.add(h);
                }
            }
        }

        if (candidates.isEmpty()) return false;

        for (Habbo habbo : candidates) {
            if (habbo == null || habbo.getRoomUnit() == null) continue;
            HabboItem dest = destList.get(Emulator.getRandom().nextInt(destList.size()));
            if (dest == null) continue;
            RoomTile tile = room.getLayout().getTile(dest.getX(), dest.getY());
            if (tile != null) {
                WiredEffectTeleport.teleportUnitToTile(habbo.getRoomUnit(), tile, this.walkMode);
            }
        }
        return true;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        this.items.removeIf(item -> item == null || item.getRoomId() != this.getRoomId() || room.getHabboItem(item.getId()) == null);

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items) message.appendInt(item.getId());
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(2);
        message.appendInt(this.walkMode);
        message.appendInt(this.userSource);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        int newWalkMode = intCount >= 1 ? Math.max(0, Math.min(2, packet.readInt())) : 0;
        int newUserSource = intCount >= 2 ? WiredUserSourceHelper.normalizeUserSourceCode(packet.readInt()) : 0;
        for (int i = 2; i < intCount; i++) packet.readInt();

        packet.readString();

        int itemsCount = packet.readInt();
        if (itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count", 20)) {
            throw new WiredSaveException("Too many furni selected");
        }

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        List<HabboItem> newItems = new ArrayList<>();
        for (int i = 0; i < itemsCount; i++) {
            int itemId = packet.readInt();
            HabboItem it = room != null ? room.getHabboItem(itemId) : null;
            if (it == null) throw new WiredSaveException(String.format("Item %s not found", itemId));
            newItems.add(it);
        }

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        int newFurniSource = WiredFurniSourceHelper.readOptionalSourceCode(packet);

        this.walkMode = newWalkMode;
        this.furniSource = newFurniSource;
        this.userSource = newUserSource;
        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.getDelay(),
            this.walkMode,
            this.furniSource,
            this.userSource,
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items.clear();
        this.walkMode = WiredEffectTeleport.WALK_CONTINUE_IF_NEAR;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.setDelay(data.delay);
                this.walkMode = Math.max(0, Math.min(2, data.walkMode));
                this.furniSource = WiredFurniSourceHelper.normalizeSourceCode(data.furniSource);
                this.userSource = WiredUserSourceHelper.normalizeUserSourceCode(data.userSource);
                if (data.itemIds != null) {
                    for (Integer id : data.itemIds) {
                        HabboItem item = room.getHabboItem(id);
                        if (item != null) this.items.add(item);
                    }
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.walkMode = WiredEffectTeleport.WALK_CONTINUE_IF_NEAR;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    static class JsonData {
        int delay;
        int walkMode;
        int furniSource;
        int userSource;
        List<Integer> itemIds;

        public JsonData(int delay, int walkMode, int furniSource, int userSource, List<Integer> itemIds) {
            this.delay = delay;
            this.walkMode = walkMode;
            this.furniSource = furniSource;
            this.userSource = userSource;
            this.itemIds = itemIds;
        }
    }
}
