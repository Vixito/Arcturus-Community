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
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredFurniSourceHelper;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemOnRollerComposer;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectRelativeMove extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.RELATIVE_MOVE;

    // dirX: 0 = NW (-X), 1 = SE (+X)
    // dirY: 0 = SW (+Y), 1 = NE (-Y)
    private int dirX = 1;
    private int stepsX = 0;
    private int dirY = 1;
    private int stepsY = 0;
    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    private THashSet<HabboItem> items;

    public WiredEffectRelativeMove(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectRelativeMove(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null || room.getLayout() == null) return false;

        int dx = (this.dirX == 0 ? -this.stepsX : this.stepsX);
        int dy = (this.dirY == 0 ? this.stepsY : -this.stepsY);
        if (dx == 0 && dy == 0) return false;

        THashSet<HabboItem> targetItems = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        if (targetItems.isEmpty()) return false;

        boolean acted = false;
        for (HabboItem item : targetItems) {
            if (item == null) continue;
            RoomTile oldLocation = room.getLayout().getTile(item.getX(), item.getY());
            RoomTile newLocation = room.getLayout().getTile((short) (item.getX() + dx), (short) (item.getY() + dy));
            if (oldLocation == null || newLocation == null || newLocation.state == RoomTileState.INVALID) continue;

            double oldZ = item.getZ();
            int rotation = item.getRotation();
            if (room.furnitureFitsAt(newLocation, item, rotation, false) == FurnitureMovementError.NONE
                    && room.moveFurniTo(item, newLocation, rotation, null, false) == FurnitureMovementError.NONE) {
                room.sendComposer(new FloorItemOnRollerComposer(item, null, oldLocation, oldZ, newLocation, item.getZ(), 0, room).compose());
                acted = true;
            }
        }
        return acted;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.dirX,
            this.stepsX,
            this.dirY,
            this.stepsY,
            this.furniSource,
            this.getDelay(),
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        this.dirX = 1;
        this.stepsX = 0;
        this.dirY = 1;
        this.stepsY = 0;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.setDelay(0);

        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.dirX = data.dirX == 0 ? 0 : 1;
                this.stepsX = Math.max(0, Math.min(10, data.stepsX));
                this.dirY = data.dirY == 0 ? 0 : 1;
                this.stepsY = Math.max(0, Math.min(10, data.stepsY));
                this.furniSource = WiredFurniSourceHelper.normalizeSourceCode(data.furniSource);
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
        this.dirX = 1;
        this.stepsX = 0;
        this.dirY = 1;
        this.stepsY = 0;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
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
        message.appendInt(4);
        message.appendInt(this.dirX);
        message.appendInt(this.stepsX);
        message.appendInt(this.dirY);
        message.appendInt(this.stepsY);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        int newDirX = 1;
        int newStepsX = 0;
        int newDirY = 1;
        int newStepsY = 0;
        if (intCount >= 1) newDirX = packet.readInt() == 0 ? 0 : 1;
        if (intCount >= 2) newStepsX = Math.max(0, Math.min(10, packet.readInt()));
        if (intCount >= 3) newDirY = packet.readInt() == 0 ? 0 : 1;
        if (intCount >= 4) newStepsY = Math.max(0, Math.min(10, packet.readInt()));
        for (int i = 4; i < intCount; i++) packet.readInt();

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

        int newSource = WiredFurniSourceHelper.readOptionalSourceCode(packet);
        this.dirX = newDirX;
        this.stepsX = newStepsX;
        this.dirY = newDirY;
        this.stepsY = newStepsY;
        this.furniSource = newSource;
        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);
        return true;
    }

    static class JsonData {
        int dirX;
        int stepsX;
        int dirY;
        int stepsY;
        int furniSource;
        int delay;
        List<Integer> itemIds;

        public JsonData(int dirX, int stepsX, int dirY, int stepsY, int furniSource, int delay, List<Integer> itemIds) {
            this.dirX = dirX;
            this.stepsX = stepsX;
            this.dirY = dirY;
            this.stepsY = stepsY;
            this.furniSource = furniSource;
            this.delay = delay;
            this.itemIds = itemIds;
        }
    }
}
