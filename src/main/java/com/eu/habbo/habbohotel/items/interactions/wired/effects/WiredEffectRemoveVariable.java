package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.variables.VariableScope;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectRemoveVariable extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.REMOVE_VARIABLE;

    private String variableName = "";
    private int scope = 1; // 1: User, 2: Furni

    public WiredEffectRemoveVariable(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectRemoveVariable(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
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

        room.getVariableManager().removeVariable(this.variableName, entityId, varScope);
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.variableName, this.scope, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.variableName = data.variableName != null ? data.variableName : "";
                this.scope = data.scope;
                this.setDelay(data.delay);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.variableName = "";
        this.scope = 1;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return WiredEffectRemoveVariable.type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0); // selected furni count
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.variableName);
        message.appendInt(1); // int params count
        message.appendInt(this.scope);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        packet.readInt(); // furni count
        this.variableName = packet.readString();

        int intCount = packet.readInt();
        if (intCount >= 1) {
            this.scope = packet.readInt();
        }

        this.setDelay(packet.readInt());
        return true;
    }

    static class JsonData {
        String variableName;
        int scope;
        int delay;

        public JsonData(String variableName, int scope, int delay) {
            this.variableName = variableName;
            this.scope = scope;
            this.delay = delay;
        }
    }
}
