package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
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
import com.eu.habbo.messages.outgoing.rooms.items.AddFloorItemComposer;
import com.eu.habbo.messages.outgoing.rooms.items.RemoveFloorItemComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserRemoveComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUsersComposer;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectSetVisibility extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.SET_VISIBILITY;

    private int whoSees = 0;       // 0 = Usuarios seleccionados, 1 = Todos los usuarios
    private int visibility = 0;    // 0 = Visible, 1 = Invisible
    private boolean clickThrough = false;
    private int easing = 0;        // 0 = Instantáneo, 1 = Suave
    private int targetType = 0;    // 0 = Furni, 1 = Usuario
    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    private int userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
    private THashSet<HabboItem> items;

    public WiredEffectSetVisibility(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectSetVisibility(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;

        List<Habbo> viewers;
        if (this.whoSees == 1) {
            viewers = new ArrayList<>(room.getHabbos());
        } else {
            viewers = WiredUserSourceHelper.resolveUsers(this, this.userSource, roomUnit, room, stuff);
        }
        if (viewers.isEmpty()) return false;

        if (this.targetType == 1) {
            List<Habbo> targetUsers = WiredUserSourceHelper.resolveUsers(this, this.userSource, roomUnit, room, stuff);
            if (targetUsers.isEmpty()) return false;
            for (Habbo target : targetUsers) {
                if (target == null || target.getRoomUnit() == null) continue;
                for (Habbo viewer : viewers) {
                    if (viewer == null || viewer.getClient() == null) continue;
                    if (this.visibility == 1) {
                        viewer.getClient().sendResponse(new RoomUserRemoveComposer(target.getRoomUnit()));
                    } else {
                        viewer.getClient().sendResponse(new RoomUsersComposer(target));
                    }
                }
            }
            return true;
        }

        THashSet<HabboItem> targetItems = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        if (targetItems.isEmpty()) return false;

        for (HabboItem item : targetItems) {
            if (item == null || item.getRoomId() != room.getId()) continue;
            String ownerName = room.getFurniOwnerNames().get(item.getUserId());
            for (Habbo viewer : viewers) {
                if (viewer == null || viewer.getClient() == null) continue;
                if (this.visibility == 1) {
                    viewer.getClient().sendResponse(new RemoveFloorItemComposer(item, true));
                } else {
                    viewer.getClient().sendResponse(new AddFloorItemComposer(item, ownerName));
                }
            }
        }
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.whoSees,
            this.visibility,
            this.clickThrough,
            this.easing,
            this.targetType,
            this.furniSource,
            this.userSource,
            this.getDelay(),
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.whoSees = (data.whoSees == 1) ? 1 : 0;
                this.visibility = (data.visibility == 1) ? 1 : 0;
                this.clickThrough = data.clickThrough;
                this.easing = (data.easing == 1) ? 1 : 0;
                this.targetType = (data.targetType == 1) ? 1 : 0;
                this.furniSource = WiredFurniSourceHelper.normalizeSourceCode(data.furniSource);
                this.userSource = WiredUserSourceHelper.normalizeUserSourceCode(data.userSource);
                this.setDelay(data.delay);
                if (data.itemIds != null && room != null) {
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
        this.whoSees = 0;
        this.visibility = 0;
        this.clickThrough = false;
        this.easing = 0;
        this.targetType = 0;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        THashSet<HabboItem> toRemove = new THashSet<>();
        for (HabboItem item : this.items) {
            if (item.getRoomId() != this.getRoomId() || room.getHabboItem(item.getId()) == null) {
                toRemove.add(item);
            }
        }
        for (HabboItem item : toRemove) this.items.remove(item);

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items) message.appendInt(item.getId());
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(6);
        message.appendInt(this.whoSees);
        message.appendInt(this.visibility);
        message.appendInt(this.clickThrough ? 1 : 0);
        message.appendInt(this.easing);
        message.appendInt(this.targetType);
        message.appendInt(this.userSource);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        int newWhoSees = intCount >= 1 && packet.readInt() == 1 ? 1 : 0;
        int newVisibility = intCount >= 2 && packet.readInt() == 1 ? 1 : 0;
        boolean newClickThrough = intCount >= 3 && packet.readInt() == 1;
        int newEasing = intCount >= 4 && packet.readInt() == 1 ? 1 : 0;
        int newTargetType = intCount >= 5 && packet.readInt() == 1 ? 1 : 0;
        int newUserSource = intCount >= 6
                ? WiredUserSourceHelper.normalizeUserSourceCode(packet.readInt())
                : WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        for (int i = 6; i < intCount; i++) packet.readInt();

        packet.readString();

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room == null) throw new WiredSaveException("Trying to save wired in unloaded room");

        int itemsCount = packet.readInt();
        if (itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) {
            throw new WiredSaveException("Too many furni selected");
        }

        THashSet<HabboItem> newItems = new THashSet<>();
        for (int i = 0; i < itemsCount; i++) {
            HabboItem it = room.getHabboItem(packet.readInt());
            if (it != null) newItems.add(it);
        }

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        int newFurniSource = WiredFurniSourceHelper.readOptionalSourceCode(packet);

        this.whoSees = newWhoSees;
        this.visibility = newVisibility;
        this.clickThrough = newClickThrough;
        this.easing = newEasing;
        this.targetType = newTargetType;
        this.userSource = newUserSource;
        this.furniSource = newFurniSource;
        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);
        return true;
    }

    static class JsonData {
        int whoSees;
        int visibility;
        boolean clickThrough;
        int easing;
        int targetType;
        int furniSource;
        int userSource;
        int delay;
        List<Integer> itemIds;

        public JsonData(int whoSees, int visibility, boolean clickThrough, int easing, int targetType, int furniSource, int userSource, int delay, List<Integer> itemIds) {
            this.whoSees = whoSees;
            this.visibility = visibility;
            this.clickThrough = clickThrough;
            this.easing = easing;
            this.targetType = targetType;
            this.furniSource = furniSource;
            this.userSource = userSource;
            this.delay = delay;
            this.itemIds = itemIds;
        }
    }
}
