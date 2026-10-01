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

public class WiredSelectorVariable extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.SELECTOR_VARIABLE;

    private String variableName = "";
    private int targetType = 0; // 0: Furni, 1: User
    private int checkValue = 0; // 0: Just has var, 1: Value match
    private int comparison = 0; // 0: ==, 1: !=, 2: >, 3: <, 4: >=, 5: <=
    private int targetValue = 0;

    public WiredSelectorVariable(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredSelectorVariable(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    public String getVariableName() {
        return this.variableName;
    }

    public int getTargetType() {
        return this.targetType;
    }

    public int getCheckValue() {
        return this.checkValue;
    }

    public int getComparison() {
        return this.comparison;
    }

    public int getTargetValue() {
        return this.targetValue;
    }

    @Override
    public WiredConditionType getType() {
        return WiredSelectorVariable.type;
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        packet.readInt();
        this.variableName = packet.readString();
        int count = packet.readInt();
        if (count >= 4) {
            this.targetType = packet.readInt();
            this.checkValue = packet.readInt();
            this.comparison = packet.readInt();
            this.targetValue = packet.readInt();
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
                this.variableName, this.targetType, this.checkValue, this.comparison, this.targetValue
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
                this.checkValue = data.checkValue;
                this.comparison = data.comparison;
                this.targetValue = data.targetValue;
            }
        }
    }

    @Override
    public void onPickUp() {
        this.variableName = "";
        this.targetType = 0;
        this.checkValue = 0;
        this.comparison = 0;
        this.targetValue = 0;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.variableName);
        message.appendInt(4);
        message.appendInt(this.targetType);
        message.appendInt(this.checkValue);
        message.appendInt(this.comparison);
        message.appendInt(this.targetValue);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    static class JsonData {
        String variableName;
        int targetType;
        int checkValue;
        int comparison;
        int targetValue;

        public JsonData(String variableName, int targetType, int checkValue, int comparison, int targetValue) {
            this.variableName = variableName;
            this.targetType = targetType;
            this.checkValue = checkValue;
            this.comparison = comparison;
            this.targetValue = targetValue;
        }
    }
}
