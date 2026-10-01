package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.variables.RoomVariable;
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

public class WiredEffectGiveVariable extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.GIVE_VARIABLE;

    private String variableName = "";
    private int scope = 1; // 1: User, 2: Furni
    private int initialValue = 0;
    private int hasValue = 1; // 1: Yes, 0: No (Flag)
    private int overwrite = 1; // 1: Overwrite existing, 0: Keep existing
    private int isPermanent = 1; // 1: Permanent, 0: Temporary

    public WiredEffectGiveVariable(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectGiveVariable(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (this.variableName == null || this.variableName.trim().isEmpty())
            return false;

        VariableScope varScope = VariableScope.fromCode(this.scope);
        int entityId = 0;
        Habbo habbo = null;
        HabboItem targetItem = null;

        if (roomUnit != null) {
            habbo = room.getHabbo(roomUnit);
            if (habbo != null) {
                entityId = habbo.getHabboInfo().getId();
            }
        }

        if (varScope == VariableScope.FURNI && stuff != null) {
            for (Object obj : stuff) {
                if (obj instanceof HabboItem) {
                    targetItem = (HabboItem) obj;
                    entityId = targetItem.getId();
                    break;
                }
            }
        }

        RoomVariable existing = room.getVariableManager().getRoomVariable(this.variableName, entityId, varScope);
        if (existing != null && this.overwrite == 0) {
            return true; // Keep existing variable
        }

        room.getVariableManager().setVariable(
                this.variableName,
                entityId,
                varScope,
                this.initialValue,
                this.hasValue == 1,
                this.isPermanent == 1,
                roomUnit,
                habbo,
                targetItem
        );

        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
                this.variableName, this.scope, this.initialValue, this.hasValue, this.overwrite, this.isPermanent, this.getDelay()
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
                this.initialValue = data.initialValue;
                this.hasValue = data.hasValue;
                this.overwrite = data.overwrite;
                this.isPermanent = data.isPermanent;
                this.setDelay(data.delay);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.variableName = "";
        this.scope = 1;
        this.initialValue = 0;
        this.hasValue = 1;
        this.overwrite = 1;
        this.isPermanent = 1;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return WiredEffectGiveVariable.type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0); // selected furni count
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.variableName);
        message.appendInt(5); // int params count
        message.appendInt(this.scope);
        message.appendInt(this.initialValue);
        message.appendInt(this.hasValue);
        message.appendInt(this.overwrite);
        message.appendInt(this.isPermanent);
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
        if (intCount >= 5) {
            this.scope = packet.readInt();
            this.initialValue = packet.readInt();
            this.hasValue = packet.readInt();
            this.overwrite = packet.readInt();
            this.isPermanent = packet.readInt();
        } else if (intCount >= 2) {
            this.scope = packet.readInt();
            this.initialValue = packet.readInt();
        }

        this.setDelay(packet.readInt());
        return true;
    }

    static class JsonData {
        String variableName;
        int scope;
        int initialValue;
        int hasValue;
        int overwrite;
        int isPermanent;
        int delay;

        public JsonData(String variableName, int scope, int initialValue, int hasValue, int overwrite, int isPermanent, int delay) {
            this.variableName = variableName;
            this.scope = scope;
            this.initialValue = initialValue;
            this.hasValue = hasValue;
            this.overwrite = overwrite;
            this.isPermanent = isPermanent;
            this.delay = delay;
        }
    }
}
