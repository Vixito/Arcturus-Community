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

public class WiredTriggerUserPerformsAction extends InteractionWiredTrigger {
    public static final WiredTriggerType type = WiredTriggerType.USER_PERFORMS_ACTION;

    // 1: Saludar, 2: Lanzar beso, 3: Risas, 7: Confirmado, 6: Despierto, 5: Ausente,
    // 4: Sentarse, 9: De pie, 10: Tumbado, 11: Señales, 8: Bailar
    private int action = 1;

    public WiredTriggerUserPerformsAction(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredTriggerUserPerformsAction(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (stuff != null && stuff.length >= 1 && stuff[0] instanceof Integer) {
            int performedAction = (Integer) stuff[0];
            return performedAction == this.action;
        }
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.action));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.action = 1;
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null && data.action > 0) {
                this.action = data.action;
            }
        }
    }

    @Override
    public void onPickUp() {
        this.action = 1;
    }

    @Override
    public WiredTriggerType getType() {
        return WiredTriggerUserPerformsAction.type;
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
        message.appendInt(this.action);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        int intCount = packet.readInt();
        if (intCount >= 1) {
            this.action = packet.readInt();
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
        int action;

        public JsonData(int action) {
            this.action = action;
        }
    }
}
