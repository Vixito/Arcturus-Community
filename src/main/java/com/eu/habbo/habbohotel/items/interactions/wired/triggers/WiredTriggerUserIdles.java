package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredTriggerUserIdles extends InteractionWiredTrigger {
    private static final WiredTriggerType type = WiredTriggerType.IDLES;

    private int idleSeconds = 0;

    public WiredTriggerUserIdles(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredTriggerUserIdles(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (roomUnit == null || room == null) {
            return false;
        }

        if (stuff != null && stuff.length >= 2 && stuff[1] instanceof Integer) {
            int elapsedSeconds = (Integer) stuff[1];
            return this.idleSeconds > 0 && elapsedSeconds == this.idleSeconds;
        }

        return this.idleSeconds == 0;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.idleSeconds));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.idleSeconds = 0;
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.idleSeconds = Math.max(0, Math.min(60, data.idleSeconds));
            }
        } else if (wiredData != null && !wiredData.isEmpty()) {
            try {
                this.idleSeconds = Math.max(0, Math.min(60, Integer.parseInt(wiredData.trim())));
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public void onPickUp() {
        this.idleSeconds = 0;
    }

    @Override
    public WiredTriggerType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(1);
        message.appendInt(this.idleSeconds);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        int intCount = packet.readInt();
        if (intCount >= 1) {
            this.idleSeconds = Math.max(0, Math.min(60, packet.readInt()));
        } else {
            this.idleSeconds = 0;
        }
        for (int i = 1; i < intCount; i++) {
            packet.readInt();
        }
        packet.readString();
        int itemCount = packet.readInt();
        for (int i = 0; i < itemCount; i++) {
            packet.readInt();
        }
        return true;
    }

    @Override
    public boolean isTriggeredByRoomUnit() {
        return true;
    }

    static class JsonData {
        int idleSeconds;

        public JsonData(int idleSeconds) {
            this.idleSeconds = idleSeconds;
        }
    }
}
