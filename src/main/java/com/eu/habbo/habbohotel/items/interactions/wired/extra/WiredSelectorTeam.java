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

public class WiredSelectorTeam extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.SELECTOR_USERS_TEAM;

    private int selectedTeam = 1; // 1: red, 2: green, 3: blue, 4: yellow

    public WiredSelectorTeam(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredSelectorTeam(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    public int getSelectedTeam() {
        return this.selectedTeam;
    }

    @Override
    public WiredConditionType getType() {
        return WiredSelectorTeam.type;
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
        message.appendInt(this.selectedTeam);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        int count = packet.readInt();
        if (count > 0) {
            this.selectedTeam = packet.readInt();
        }
        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.selectedTeam));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.selectedTeam = data.selectedTeam;
            }
        } else if (wiredData != null && !wiredData.isEmpty()) {
            try {
                this.selectedTeam = Integer.parseInt(wiredData);
            } catch (Exception ignored) {}
        }
    }

    @Override
    public void onPickUp() {
        this.selectedTeam = 1;
    }

    static class JsonData {
        int selectedTeam;

        public JsonData(int selectedTeam) {
            this.selectedTeam = selectedTeam;
        }
    }
}
