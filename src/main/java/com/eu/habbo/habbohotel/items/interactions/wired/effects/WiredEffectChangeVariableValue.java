package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectChangeVariableValue extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.SET_VARIABLE_VALUE;

    private String variableName = "";
    private int operation = 1; // 0: Set, 1: Add, 2: Subtract, 3: Multiply, 4: Divide, 5: Random
    private int operand = 1;
    private int scope = 0; // 0: Room, 1: User

    public WiredEffectChangeVariableValue(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectChangeVariableValue(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
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

        room.getVariableManager().modifyVariable(this.variableName, userId, this.scope == 1, this.operation, this.operand, roomUnit);
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.variableName, this.operation, this.operand, this.scope, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.variableName = data.variableName != null ? data.variableName : "";
                this.operation = data.operation;
                this.operand = data.operand;
                this.scope = data.scope;
                this.setDelay(data.delay);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.variableName = "";
        this.operation = 1;
        this.operand = 1;
        this.scope = 0;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return WiredEffectChangeVariableValue.type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0); // selected furni count
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.variableName);
        message.appendInt(3); // int params count
        message.appendInt(this.operation);
        message.appendInt(this.operand);
        message.appendInt(this.scope);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0); // invalid triggers
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        packet.readInt(); // furni count
        int intParamsCount = packet.readInt();
        if (intParamsCount >= 3) {
            this.operation = packet.readInt();
            this.operand = packet.readInt();
            this.scope = packet.readInt();
        } else if (intParamsCount >= 2) {
            this.operation = packet.readInt();
            this.operand = packet.readInt();
        }
        this.variableName = packet.readString();
        packet.readInt(); // selection type
        int delay = packet.readInt();

        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20))
            throw new WiredSaveException("Delay too long");

        this.setDelay(delay);
        return true;
    }

    static class JsonData {
        String variableName;
        int operation;
        int operand;
        int scope;
        int delay;

        public JsonData(String variableName, int operation, int operand, int scope, int delay) {
            this.variableName = variableName;
            this.operation = operation;
            this.operand = operand;
            this.scope = scope;
            this.delay = delay;
        }
    }
}
