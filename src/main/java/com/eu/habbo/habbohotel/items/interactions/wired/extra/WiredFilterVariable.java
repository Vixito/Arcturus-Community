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

public class WiredFilterVariable extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.FILTER_VARIABLE;

    private String variableName = "";
    private int targetType = 0; // 0: Furni, 1: User
    private int filterMode = 0; // 0: Highest, 1: Lowest
    private int count = 1;

    public WiredFilterVariable(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredFilterVariable(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    public String getVariableName() {
        return this.variableName;
    }

    public int getTargetType() {
        return this.targetType;
    }

    public int getFilterMode() {
        return this.filterMode;
    }

    public int getCount() {
        return this.count;
    }

    @Override
    public WiredConditionType getType() {
        return WiredFilterVariable.type;
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        packet.readInt();
        this.variableName = packet.readString();
        int paramCount = packet.readInt();
        if (paramCount >= 3) {
            this.targetType = packet.readInt();
            this.filterMode = packet.readInt();
            this.count = packet.readInt();
        }
        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
                this.variableName, this.targetType, this.filterMode, this.count
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.variableName = data.variableName != null ? data.variableName : "";
                this.targetType = data.targetType;
                this.filterMode = data.filterMode;
                this.count = data.count;
            }
        }
    }

    @Override
    public void onPickUp() {
        this.variableName = "";
        this.targetType = 0;
        this.filterMode = 0;
        this.count = 1;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.variableName);
        message.appendInt(3);
        message.appendInt(this.targetType);
        message.appendInt(this.filterMode);
        message.appendInt(this.count);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    static class JsonData {
        String variableName;
        int targetType;
        int filterMode;
        int count;

        public JsonData(String variableName, int targetType, int filterMode, int count) {
            this.variableName = variableName;
            this.targetType = targetType;
            this.filterMode = filterMode;
            this.count = count;
        }
    }
}
