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

public class WiredSelectorAltitude extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.SELECTOR_FURNI_ALTITUDE;

    private int minAltitude = 0;
    private int maxAltitude = 40;

    public WiredSelectorAltitude(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredSelectorAltitude(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    public int getMinAltitude() {
        return this.minAltitude;
    }

    public int getMaxAltitude() {
        return this.maxAltitude;
    }

    @Override
    public WiredConditionType getType() {
        return WiredSelectorAltitude.type;
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
        message.appendInt(this.minAltitude);
        message.appendInt(this.maxAltitude);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        int count = packet.readInt();
        if (count >= 2) {
            this.minAltitude = packet.readInt();
            this.maxAltitude = packet.readInt();
        } else if (count == 1) {
            this.minAltitude = packet.readInt();
        }
        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.minAltitude, this.maxAltitude));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.minAltitude = data.minAltitude;
                this.maxAltitude = data.maxAltitude;
            }
        } else if (wiredData != null && wiredData.contains(";")) {
            try {
                String[] parts = wiredData.split(";");
                this.minAltitude = Integer.parseInt(parts[0]);
                this.maxAltitude = Integer.parseInt(parts[1]);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onPickUp() {
        this.minAltitude = 0;
        this.maxAltitude = 40;
    }

    static class JsonData {
        int minAltitude;
        int maxAltitude;

        public JsonData(int minAltitude, int maxAltitude) {
            this.minAltitude = minAltitude;
            this.maxAltitude = maxAltitude;
        }
    }
}
