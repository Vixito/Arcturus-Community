package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredFilterCount extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.FILTER_COUNT;

    private int count = 1;
    private int mode = 0; // 0: first, 1: last, 2: random

    public WiredFilterCount(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredFilterCount(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    public int getCount() {
        return this.count;
    }

    public int getMode() {
        return this.mode;
    }

    @Override
    public WiredConditionType getType() {
        return WiredFilterCount.type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(2);
        message.appendInt(this.count);
        message.appendInt(this.mode);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        int numInts = packet.readInt();
        if (numInts >= 1) {
            this.count = packet.readInt();
        }
        if (numInts >= 2) {
            this.mode = packet.readInt();
        }
        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.count, this.mode));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.count = data.count;
                this.mode = data.mode;
            }
        } else if (wiredData != null && wiredData.contains(";")) {
            try {
                String[] parts = wiredData.split(";");
                this.count = Integer.parseInt(parts[0]);
                this.mode = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onPickUp() {
        this.count = 1;
        this.mode = 0;
    }

    static class JsonData {
        int count;
        int mode;

        public JsonData(int count, int mode) {
            this.count = count;
            this.mode = mode;
        }
    }
}
