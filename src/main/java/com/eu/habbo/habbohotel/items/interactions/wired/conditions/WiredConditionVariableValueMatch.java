package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionVariableValueMatch extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.VARIABLE_VALUE_MATCH;

    private String variableName = "";
    private int comparison = 0; // 0: Equal, 1: Not Equal, 2: Greater, 3: Less, 4: Greater Equal, 5: Less Equal
    private int targetValue = 0;
    private int scope = 0; // 0: Room, 1: User

    public WiredConditionVariableValueMatch(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionVariableValueMatch(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (this.variableName == null || this.variableName.trim().isEmpty())
            return false;

        int userId = 0;
        if (this.scope == 1 && roomUnit != null) {
            Habbo habbo = room.getHabbo(roomUnit);
            if (habbo != null) {
                userId = habbo.getHabboInfo().getId();
            }
        }

        int current = room.getVariableManager().getVariable(this.variableName, userId, this.scope == 1);

        switch (this.comparison) {
            case 0: // ==
                return current == this.targetValue;
            case 1: // !=
                return current != this.targetValue;
            case 2: // >
                return current > this.targetValue;
            case 3: // <
                return current < this.targetValue;
            case 4: // >=
                return current >= this.targetValue;
            case 5: // <=
                return current <= this.targetValue;
            default:
                return current == this.targetValue;
        }
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.variableName, this.comparison, this.targetValue, this.scope));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.variableName = data.variableName != null ? data.variableName : "";
                this.comparison = data.comparison;
                this.targetValue = data.targetValue;
                this.scope = data.scope;
            }
        }
    }

    @Override
    public void onPickUp() {
        this.variableName = "";
        this.comparison = 0;
        this.targetValue = 0;
        this.scope = 0;
    }

    @Override
    public WiredConditionType getType() {
        return WiredConditionVariableValueMatch.type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0); // furni selection count
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.variableName);
        message.appendInt(3); // int count
        message.appendInt(this.comparison);
        message.appendInt(this.targetValue);
        message.appendInt(this.scope);
        message.appendInt(0);
        message.appendInt(this.getType().code);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        packet.readInt(); // furni count
        int intParamsCount = packet.readInt();
        if (intParamsCount >= 3) {
            this.comparison = packet.readInt();
            this.targetValue = packet.readInt();
            this.scope = packet.readInt();
        } else if (intParamsCount >= 2) {
            this.comparison = packet.readInt();
            this.targetValue = packet.readInt();
        }
        this.variableName = packet.readString();
        return true;
    }

    static class JsonData {
        String variableName;
        int comparison;
        int targetValue;
        int scope;

        public JsonData(String variableName, int comparison, int targetValue, int scope) {
            this.variableName = variableName;
            this.comparison = comparison;
            this.targetValue = targetValue;
            this.scope = scope;
        }
    }
}
