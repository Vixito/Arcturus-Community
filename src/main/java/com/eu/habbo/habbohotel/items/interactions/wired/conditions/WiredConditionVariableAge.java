package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.variables.VariableScope;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionVariableAge extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.VARIABLE_AGE_MATCH;

    private String variableName = "";
    private int scope = 1; // 1: User, 2: Furni
    private int ageType = 0; // 0: Creation age, 1: Last update age
    private int comparison = 2; // 0: <, 1: <=, 2: ==, 3: >=, 4: >
    private int targetSeconds = 0;

    public WiredConditionVariableAge(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionVariableAge(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (this.variableName == null || this.variableName.trim().isEmpty())
            return false;

        VariableScope varScope = VariableScope.fromCode(this.scope);
        int entityId = 0;

        if (roomUnit != null) {
            Habbo habbo = room.getHabbo(roomUnit);
            if (habbo != null) {
                entityId = habbo.getHabboInfo().getId();
            }
        }

        if (varScope == VariableScope.FURNI && stuff != null) {
            for (Object obj : stuff) {
                if (obj instanceof HabboItem) {
                    entityId = ((HabboItem) obj).getId();
                    break;
                }
            }
        }

        long age = room.getVariableManager().getVariableAgeSeconds(this.variableName, entityId, varScope, this.ageType == 0);

        switch (this.comparison) {
            case 0: return age < this.targetSeconds;
            case 1: return age <= this.targetSeconds;
            case 2: return age == this.targetSeconds;
            case 3: return age >= this.targetSeconds;
            case 4: return age > this.targetSeconds;
            default: return age >= this.targetSeconds;
        }
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
                this.variableName, this.scope, this.ageType, this.comparison, this.targetSeconds
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.variableName = data.variableName != null ? data.variableName : "";
                this.scope = data.scope;
                this.ageType = data.ageType;
                this.comparison = data.comparison;
                this.targetSeconds = data.targetSeconds;
            }
        }
    }

    @Override
    public void onPickUp() {
        this.variableName = "";
        this.scope = 1;
        this.ageType = 0;
        this.comparison = 2;
        this.targetSeconds = 0;
    }

    @Override
    public WiredConditionType getType() {
        return WiredConditionVariableAge.type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0); // selected furni count
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.variableName);
        message.appendInt(4); // int params count
        message.appendInt(this.scope);
        message.appendInt(this.ageType);
        message.appendInt(this.comparison);
        message.appendInt(this.targetSeconds);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        packet.readInt(); // furni count
        this.variableName = packet.readString();

        int intCount = packet.readInt();
        if (intCount >= 4) {
            this.scope = packet.readInt();
            this.ageType = packet.readInt();
            this.comparison = packet.readInt();
            this.targetSeconds = packet.readInt();
        }

        return true;
    }

    static class JsonData {
        String variableName;
        int scope;
        int ageType;
        int comparison;
        int targetSeconds;

        public JsonData(String variableName, int scope, int ageType, int comparison, int targetSeconds) {
            this.variableName = variableName;
            this.scope = scope;
            this.ageType = ageType;
            this.comparison = comparison;
            this.targetSeconds = targetSeconds;
        }
    }
}
