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
import java.util.HashMap;
import java.util.Map;

public class WiredExtraTextConnector extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.TEXT_CONNECTOR;

    private String variableName = "";
    private final Map<Integer, String> mappings = new HashMap<>();

    public WiredExtraTextConnector(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredExtraTextConnector(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    public String getText(int value) {
        return this.mappings.getOrDefault(value, String.valueOf(value));
    }

    public String getVariableName() {
        return this.variableName;
    }

    public Map<Integer, String> getMappings() {
        return this.mappings;
    }

    @Override
    public WiredConditionType getType() {
        return WiredExtraTextConnector.type;
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        packet.readInt();
        String fullString = packet.readString();
        this.mappings.clear();

        if (fullString != null && fullString.contains(";")) {
            String[] parts = fullString.split(";", 2);
            this.variableName = parts[0].trim();
            String mappingStr = parts[1];
            if (!mappingStr.isEmpty()) {
                String[] pairs = mappingStr.split("\\|");
                for (String pair : pairs) {
                    String[] kv = pair.split("=", 2);
                    if (kv.length == 2) {
                        try {
                            this.mappings.put(Integer.parseInt(kv[0].trim()), kv[1].trim());
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
        } else {
            this.variableName = fullString != null ? fullString.trim() : "";
        }

        packet.readInt(); // int count
        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.variableName, this.mappings));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.mappings.clear();
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.variableName = data.variableName != null ? data.variableName : "";
                if (data.mappings != null) {
                    this.mappings.putAll(data.mappings);
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.variableName = "";
        this.mappings.clear();
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        StringBuilder sb = new StringBuilder(this.variableName != null ? this.variableName : "");
        sb.append(";");
        boolean first = true;
        for (Map.Entry<Integer, String> entry : this.mappings.entrySet()) {
            if (!first) sb.append("|");
            sb.append(entry.getKey()).append("=").append(entry.getValue());
            first = false;
        }

        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(sb.toString());
        message.appendInt(1);
        message.appendInt(this.mappings.size());
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    static class JsonData {
        String variableName;
        Map<Integer, String> mappings;

        public JsonData(String variableName, Map<Integer, String> mappings) {
            this.variableName = variableName;
            this.mappings = mappings;
        }
    }
}
