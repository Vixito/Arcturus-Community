package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.FurnitureMovementError;
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
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemOnRollerComposer;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectFurniToUser extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.FURNI_TO_USER;

    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    private int userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
    private THashSet<HabboItem> items;

    public WiredEffectFurniToUser(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectFurniToUser(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null || room.getLayout() == null) return false;

        THashSet<HabboItem> targetItems = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        if (targetItems.isEmpty()) return false;

        List<Habbo> targetUsers = WiredUserSourceHelper.resolveUsers(this, this.userSource, roomUnit, room, stuff);
        if (targetUsers.isEmpty()) return false;

        Habbo targetHabbo = targetUsers.get(0);
        if (targetHabbo == null || targetHabbo.getRoomUnit() == null || targetHabbo.getRoomUnit().getCurrentLocation() == null) {
            return false;
        }

        RoomTile destTile = targetHabbo.getRoomUnit().getCurrentLocation();
        if (destTile.state == RoomTileState.INVALID) return false;

        boolean moved = false;
        for (HabboItem item : targetItems) {
            if (item == null || item.getRoomId() != room.getId()) continue;
            RoomTile oldTile = room.getLayout().getTile(item.getX(), item.getY());
            double oldZ = item.getZ();

            if (oldTile == destTile) continue;

            if (room.moveFurniTo(item, destTile, item.getRotation(), null, false) == FurnitureMovementError.NONE) {
                if (oldTile != null) {
                    room.sendComposer(new FloorItemOnRollerComposer(item, null, oldTile, oldZ, destTile, item.getZ(), 0, room).compose());
                }
                moved = true;
            }
        }
        return moved;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.furniSource,
            this.userSource,
            this.getDelay(),
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        this.setDelay(0);

        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
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
        message.appendInt(1);
        message.appendInt(this.userSource);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        int newUserSource = intCount >= 1
                ? WiredUserSourceHelper.normalizeUserSourceCode(packet.readInt())
                : WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        for (int i = 1; i < intCount; i++) packet.readInt();

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

        this.userSource = newUserSource;
        this.furniSource = newFurniSource;
        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);
        return true;
    }

    static class JsonData {
        int furniSource;
        int userSource;
        int delay;
        List<Integer> itemIds;

        public JsonData(int furniSource, int userSource, int delay, List<Integer> itemIds) {
            this.furniSource = furniSource;
            this.userSource = userSource;
            this.delay = delay;
            this.itemIds = itemIds;
        }
    }
}
