package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameTimer;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredFurniSourceHelper;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class WiredTriggerClockCounter extends InteractionWiredTrigger {
    private static final WiredTriggerType type = WiredTriggerType.CLOCK_COUNTER;

    private int minutes = 0;
    private int seconds = 0;
    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    private THashSet<HabboItem> items;

    public WiredTriggerClockCounter(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredTriggerClockCounter(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null || stuff == null || stuff.length < 1 || !(stuff[0] instanceof InteractionGameTimer)) {
            return false;
        }

        InteractionGameTimer timer = (InteractionGameTimer) stuff[0];
        THashSet<HabboItem> targetItems = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        if (!targetItems.contains(timer)) {
            return false;
        }

        int targetSeconds = (this.minutes * 60) + this.seconds;
        return timer.getTimeNow() == targetSeconds;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.minutes,
            this.seconds,
            this.furniSource,
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        this.minutes = 0;
        this.seconds = 0;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        String wiredData = set.getString("wired_data");

        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.minutes = Math.max(0, Math.min(10, data.minutes));
                this.seconds = Math.max(0, Math.min(59, data.seconds));
                this.furniSource = WiredFurniSourceHelper.normalizeSourceCode(data.furniSource);
                if (data.itemIds != null && room != null) {
                    for (Integer id : data.itemIds) {
                        HabboItem item = room.getHabboItem(id);
                        if (item != null) {
                            this.items.add(item);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.minutes = 0;
        this.seconds = 0;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    }

    @Override
    public WiredTriggerType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        THashSet<HabboItem> itemsToRemove = new THashSet<>();
        for (HabboItem item : this.items) {
            if (item.getRoomId() != this.getRoomId() || room.getHabboItem(item.getId()) == null) {
                itemsToRemove.add(item);
            }
        }
        for (HabboItem item : itemsToRemove) {
            this.items.remove(item);
        }

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items) {
            message.appendInt(item.getId());
        }
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(2);
        message.appendInt(this.minutes);
        message.appendInt(this.seconds);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        int intCount = packet.readInt();
        if (intCount >= 1) {
            this.minutes = Math.max(0, Math.min(10, packet.readInt()));
        } else {
            this.minutes = 0;
        }
        if (intCount >= 2) {
            this.seconds = Math.max(0, Math.min(59, packet.readInt()));
        } else {
            this.seconds = 0;
        }
        for (int i = 2; i < intCount; i++) {
            packet.readInt();
        }

        packet.readString();

        this.items.clear();
        int count = packet.readInt();
        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room != null) {
            int limit = Math.min(count, WiredHandler.MAXIMUM_FURNI_SELECTION);
            for (int i = 0; i < count; i++) {
                int itemId = packet.readInt();
                if (i >= limit) continue;
                HabboItem item = room.getHabboItem(itemId);
                if (item instanceof InteractionGameTimer) {
                    this.items.add(item);
                }
            }
        }

        this.furniSource = WiredFurniSourceHelper.readOptionalSourceCode(packet);
        return true;
    }

    @Override
    public boolean isTriggeredByRoomUnit() {
        return false;
    }

    static class JsonData {
        int minutes;
        int seconds;
        int furniSource;
        List<Integer> itemIds;

        public JsonData(int minutes, int seconds, int furniSource, List<Integer> itemIds) {
            this.minutes = minutes;
            this.seconds = seconds;
            this.furniSource = furniSource;
            this.itemIds = itemIds;
        }
    }
}
