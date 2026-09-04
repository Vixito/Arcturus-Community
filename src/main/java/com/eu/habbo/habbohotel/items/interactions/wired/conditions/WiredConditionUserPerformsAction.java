package com.eu.habbo.habbohotel.items.interactions.wired.conditions;

import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.DanceType;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredConditionUserPerformsAction extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.USER_PERFORMS_ACTION;

    protected int action = 1; // 1: Wave, 8: Dance, 2: Blow kiss, 3: Laugh, 4: Sit, 5: Idle, 7: Thumb up

    public WiredConditionUserPerformsAction(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredConditionUserPerformsAction(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public WiredConditionType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(1);
        message.appendInt(this.action);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        packet.readInt();
        this.action = packet.readInt();
        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (roomUnit == null) return false;

        boolean matches = checkAction(roomUnit, stuff);
        return isNegative() ? !matches : matches;
    }

    protected boolean isNegative() {
        return false;
    }

    private boolean checkAction(RoomUnit roomUnit, Object[] stuff) {
        if (stuff != null && stuff.length >= 1 && stuff[0] instanceof Integer) {
            int performedAction = (int) stuff[0];
            if (performedAction == this.action) return true;
        }

        switch (this.action) {
            case 8: // Dance
                return roomUnit.getDanceType() != null && roomUnit.getDanceType() != DanceType.NONE;
            case 4: // Sit
                return roomUnit.hasStatus(RoomUnitStatus.SIT);
            case 5: // Idle
                return roomUnit.isIdle();
            case 1: // Wave
                return roomUnit.hasStatus(RoomUnitStatus.WAVE);
            default:
                return false;
        }
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.action));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.action = data.action;
            }
        } else if (wiredData != null && !wiredData.isEmpty()) {
            try {
                this.action = Integer.parseInt(wiredData);
            } catch (NumberFormatException ignored) {}
        }
    }

    @Override
    public void onPickUp() {
        this.action = 1;
    }

    static class JsonData {
        int action;

        public JsonData(int action) {
            this.action = action;
        }
    }
}
