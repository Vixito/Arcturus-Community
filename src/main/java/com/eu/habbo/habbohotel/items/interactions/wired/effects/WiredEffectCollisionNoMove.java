package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
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
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectCollisionNoMove extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.COLLISION_NO_MOVE;

    private final THashSet<HabboItem> items = new THashSet<>();
    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;

    public WiredEffectCollisionNoMove(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectCollisionNoMove(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;
        THashSet<HabboItem> targets = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        if (targets.isEmpty()) return false;

        for (HabboItem item : targets) {
            if (item == null || item.getRoomId() == 0) continue;
            int minX = item.getX();
            int minY = item.getY();
            int w = (item.getRotation() == 0 || item.getRotation() == 4) ? item.getBaseItem().getWidth() : item.getBaseItem().getLength();
            int l = (item.getRotation() == 0 || item.getRotation() == 4) ? item.getBaseItem().getLength() : item.getBaseItem().getWidth();
            int maxX = minX + Math.max(1, w) - 1;
            int maxY = minY + Math.max(1, l) - 1;

            for (Habbo habbo : room.getHabbos()) {
                if (habbo == null || habbo.getRoomUnit() == null) continue;
                RoomTile loc = habbo.getRoomUnit().getCurrentLocation();
                if (loc == null) continue;
                int dx = loc.x < minX ? (minX - loc.x) : (loc.x > maxX ? (loc.x - maxX) : 0);
                int dy = loc.y < minY ? (minY - loc.y) : (loc.y > maxY ? (loc.y - maxY) : 0);
                // On tile (0,0) or directly adjacent (orthogonal or diagonal <= 1)
                if (dx <= 1 && dy <= 1) {
                    WiredHandler.handle(WiredTriggerType.COLLISION, habbo.getRoomUnit(), room, new Object[]{ item });
                }
            }
        }
        return true;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        THashSet<HabboItem> toRemove = new THashSet<>();
        for (HabboItem item : this.items) {
            if (item.getRoomId() != this.getRoomId() || room.getHabboItem(item.getId()) == null) toRemove.add(item);
        }
        for (HabboItem item : toRemove) this.items.remove(item);

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items) message.appendInt(item.getId());
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(0);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        for (int i = 0; i < intCount; i++) packet.readInt();

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

        int newSource = WiredFurniSourceHelper.readOptionalSourceCode(packet);

        this.furniSource = newSource;
        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.getDelay(),
            this.furniSource,
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items.clear();
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.setDelay(data.delay);
                this.furniSource = WiredFurniSourceHelper.normalizeSourceCode(data.furniSource);
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
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    static class JsonData {
        int delay;
        int furniSource;
        List<Integer> itemIds;

        public JsonData(int delay, int furniSource, List<Integer> itemIds) {
            this.delay = delay;
            this.furniSource = furniSource;
            this.itemIds = itemIds;
        }
    }
}
