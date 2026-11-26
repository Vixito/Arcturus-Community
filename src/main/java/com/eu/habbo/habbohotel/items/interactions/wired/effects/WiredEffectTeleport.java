package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomTileState;
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
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserEffectComposer;
import com.eu.habbo.threading.runnables.RoomUnitTeleport;
import com.eu.habbo.threading.runnables.SendRoomUnitEffectComposer;
import gnu.trove.procedure.TObjectProcedure;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectTeleport extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.TELEPORT;

    public static final int WALK_CONTINUE_IF_NEAR = 0;
    public static final int WALK_CONTINUE = 1;
    public static final int WALK_STOP = 2;

    protected List<HabboItem> items;
    private int walkMode = WALK_CONTINUE_IF_NEAR;
    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    private int userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;

    public WiredEffectTeleport(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new ArrayList<>();
    }

    public WiredEffectTeleport(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new ArrayList<>();
    }

    public static void teleportUnitToTile(RoomUnit roomUnit, RoomTile tile) {
        teleportUnitToTile(roomUnit, tile, WALK_STOP);
    }

    public static void teleportUnitToTile(RoomUnit roomUnit, RoomTile tile, int walkMode) {
        if (roomUnit == null || tile == null || roomUnit.isWiredTeleporting)
            return;

        Room room = roomUnit.getRoom();
        if (room == null) return;

        RoomTile resumeGoal = roomUnit.isWalking() ? roomUnit.getGoal() : null;

        room.unIdle(room.getHabbo(roomUnit));
        room.sendComposer(new RoomUserEffectComposer(roomUnit, 4).compose());
        Emulator.getThreading().run(new SendRoomUnitEffectComposer(room, roomUnit), WiredHandler.TELEPORT_DELAY + 1000);

        if (tile == roomUnit.getCurrentLocation()) {
            return;
        }

        if (tile.state == RoomTileState.INVALID || tile.state == RoomTileState.BLOCKED) {
            RoomTile alternativeTile = null;
            List<RoomTile> optionalTiles = room.getLayout().getTilesAround(tile);

            Collections.reverse(optionalTiles);
            for (RoomTile optionalTile : optionalTiles) {
                if (optionalTile.state != RoomTileState.INVALID && optionalTile.state != RoomTileState.BLOCKED) {
                    alternativeTile = optionalTile;
                    break;
                }
            }

            if (alternativeTile != null) {
                tile = alternativeTile;
            }
        }

        final RoomTile targetTile = tile;
        Emulator.getThreading().run(() -> { roomUnit.isWiredTeleporting = true; }, Math.max(0, WiredHandler.TELEPORT_DELAY - 500));
        Emulator.getThreading().run(
            new RoomUnitTeleport(
                roomUnit,
                room,
                targetTile.x,
                targetTile.y,
                targetTile.getStackHeight() + (targetTile.state == RoomTileState.SIT ? -0.5 : 0),
                roomUnit.getEffectId(),
                walkMode,
                resumeGoal
            ),
            WiredHandler.TELEPORT_DELAY
        );
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        THashSet<HabboItem> toRemove = new THashSet<>();
        for (HabboItem item : this.items) {
            if (item.getRoomId() != this.getRoomId() || Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(item.getId()) == null)
                toRemove.add(item);
        }
        for (HabboItem item : toRemove) this.items.remove(item);

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items)
            message.appendInt(item.getId());

        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(2);
        message.appendInt(this.walkMode);
        message.appendInt(this.userSource);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        if (this.requiresTriggeringUser()) {
            List<Integer> invalidTriggers = new ArrayList<>();
            room.getRoomSpecialTypes().getTriggers(this.getX(), this.getY()).forEach(new TObjectProcedure<InteractionWiredTrigger>() {
                @Override
                public boolean execute(InteractionWiredTrigger object) {
                    if (!object.isTriggeredByRoomUnit()) {
                        invalidTriggers.add(object.getId());
                    }
                    return true;
                }
            });
            message.appendInt(invalidTriggers.size());
            for (Integer i : invalidTriggers) message.appendInt(i);
        } else {
            message.appendInt(0);
        }
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        int newWalkMode = WALK_CONTINUE_IF_NEAR;
        int newUserSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        if (intCount >= 1) newWalkMode = Math.max(0, Math.min(2, packet.readInt()));
        if (intCount >= 2) newUserSource = WiredUserSourceHelper.normalizeUserSourceCode(packet.readInt());
        for (int i = 2; i < intCount; i++) packet.readInt();

        packet.readString();

        int itemsCount = packet.readInt();
        if (itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) {
            throw new WiredSaveException("Too many furni selected");
        }

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room == null) throw new WiredSaveException("Room not loaded");

        List<HabboItem> newItems = new ArrayList<>();
        for (int i = 0; i < itemsCount; i++) {
            int itemId = packet.readInt();
            HabboItem it = room.getHabboItem(itemId);
            if (it == null) throw new WiredSaveException(String.format("Item %s not found", itemId));
            newItems.add(it);
        }

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20))
            throw new WiredSaveException("Delay too long");

        int newFurniSource = WiredFurniSourceHelper.readOptionalSourceCode(packet);

        this.walkMode = newWalkMode;
        this.userSource = newUserSource;
        this.furniSource = newFurniSource;
        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);

        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;

        this.items.removeIf(item -> item == null || item.getRoomId() != this.getRoomId() || room.getHabboItem(item.getId()) == null);

        THashSet<HabboItem> resolvedFurnis = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        if (resolvedFurnis.isEmpty()) return false;

        List<HabboItem> furniList = new ArrayList<>(resolvedFurnis);
        List<Habbo> targetUsers = WiredUserSourceHelper.resolveUsers(this, this.userSource, roomUnit, room, stuff);

        if (targetUsers.isEmpty() && roomUnit != null && this.userSource == WiredUserSourceHelper.USER_SOURCE_TRIGGERING) {
            HabboItem item = furniList.get(Emulator.getRandom().nextInt(furniList.size()));
            teleportUnitToTile(roomUnit, room.getLayout().getTile(item.getX(), item.getY()), this.walkMode);
            return true;
        }

        boolean acted = false;
        for (Habbo habbo : targetUsers) {
            if (habbo == null || habbo.getRoomUnit() == null) continue;
            HabboItem item = furniList.get(Emulator.getRandom().nextInt(furniList.size()));
            teleportUnitToTile(habbo.getRoomUnit(), room.getLayout().getTile(item.getX(), item.getY()), this.walkMode);
            acted = true;
        }

        return acted;
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
        this.items = new ArrayList<>();
        this.walkMode = WALK_CONTINUE_IF_NEAR;
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
        } else if (wiredData != null) {
            String[] wiredDataOld = wiredData.split("\t");
            if (wiredDataOld.length >= 1 && !wiredDataOld[0].isEmpty()) {
                try { this.setDelay(Integer.parseInt(wiredDataOld[0])); } catch (NumberFormatException ignored) {}
            }
            if (wiredDataOld.length == 2 && wiredDataOld[1].contains(";")) {
                for (String s : wiredDataOld[1].split(";")) {
                    try {
                        HabboItem item = room.getHabboItem(Integer.parseInt(s));
                        if (item != null) this.items.add(item);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.walkMode = WALK_CONTINUE_IF_NEAR;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return this.userSource == WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
    }

    @Override
    protected long requiredCooldown() {
        return 50L;
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
