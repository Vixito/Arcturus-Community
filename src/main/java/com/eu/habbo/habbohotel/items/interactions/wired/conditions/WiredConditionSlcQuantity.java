package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

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

public class WiredConditionSlcQuantity extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.SLC_QUANTITY;

    private int min = 1;
    private int max = 50;

    public WiredConditionSlcQuantity(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionSlcQuantity(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public WiredConditionType getType() {
        return type;
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
        message.appendInt(this.min);
        message.appendInt(this.max);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        packet.readInt();
        this.min = packet.readInt();
        this.max = packet.readInt();
        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;

        int count = 0;
        if (stuff != null && stuff.length > 0 && stuff[0] instanceof Integer) {
            count = (int) stuff[0];
        } else {
            count = room.getUserCount();
        }

        return count >= this.min && count <= this.max;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.min, this.max));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.min = data.min;
                this.max = data.max;
            }
        } else if (wiredData != null && wiredData.contains(";")) {
            try {
                String[] parts = wiredData.split(";");
                this.min = Integer.parseInt(parts[0]);
                this.max = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onPickUp() {
        this.min = 1;
        this.max = 50;
    }

    static class JsonData {
        int min;
        int max;

        public JsonData(int min, int max) {
            this.min = min;
            this.max = max;
        }
    }
}
