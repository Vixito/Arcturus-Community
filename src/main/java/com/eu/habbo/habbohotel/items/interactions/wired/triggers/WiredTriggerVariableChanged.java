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

public class WiredTriggerVariableChanged extends InteractionWiredTrigger {
    public static final WiredTriggerType type = WiredTriggerType.VARIABLE_CHANGED;

    private String variableName = "";
    private int scope = 0; // 0: Room, 1: User

    public WiredTriggerVariableChanged(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredTriggerVariableChanged(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (this.variableName == null || this.variableName.trim().isEmpty())
            return true; // Any variable change in room if empty

        if (stuff != null && stuff.length >= 2) {
            String changedVar = (String) stuff[0];
            int targetUserId = (int) stuff[1];

            if (!this.variableName.equalsIgnoreCase(changedVar))
                return false;

            if (this.scope == 1 && targetUserId == 0)
                return false;

            if (this.scope == 0 && targetUserId != 0)
                return false;

            return true;
        }

        return false;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.variableName, this.scope));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.variableName = data.variableName != null ? data.variableName : "";
                this.scope = data.scope;
            }
        }
    }

    @Override
    public void onPickUp() {
        this.variableName = "";
        this.scope = 0;
    }

    @Override
    public WiredTriggerType getType() {
        return WiredTriggerVariableChanged.type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.variableName);
        message.appendInt(1);
        message.appendInt(this.scope);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        packet.readInt(); // furni count
        int intCount = packet.readInt();
        if (intCount >= 1) {
            this.scope = packet.readInt();
        }
        this.variableName = packet.readString();
        return true;
    }

    @Override
    public boolean isTriggeredByRoomUnit() {
        return this.scope == 1;
    }

    static class JsonData {
        String variableName;
        int scope;

        public JsonData(String variableName, int scope) {
            this.variableName = variableName;
            this.scope = scope;
        }
    }
}
