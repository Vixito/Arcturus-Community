package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserWhisperComposer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectRoomMessage extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.ROOM_MESSAGE;

    private String message = "";

    public WiredEffectRoomMessage(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectRoomMessage(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null || this.message == null || this.message.isEmpty()) return false;

        Habbo triggerHabbo = roomUnit != null ? room.getHabbo(roomUnit) : null;
        String triggerName = triggerHabbo != null ? triggerHabbo.getHabboInfo().getUsername() : "";

        for (Habbo habbo : room.getHabbos()) {
            if (habbo == null || habbo.getClient() == null) continue;
            String msg = this.message
                .replace("%user%", triggerName.isEmpty() ? habbo.getHabboInfo().getUsername() : triggerName)
                .replace("%username%", habbo.getHabboInfo().getUsername())
                .replace("%roomname%", room.getName())
                .replace("%online_count%", String.valueOf(Emulator.getGameEnvironment().getHabboManager().getOnlineCount()));
            habbo.getClient().sendResponse(new RoomUserWhisperComposer(new RoomChatMessage(msg, habbo, habbo, RoomChatMessageBubbles.WIRED)));
        }
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.message, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.message = "";
        this.setDelay(0);
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.message = data.message != null ? data.message : "";
                this.setDelay(data.delay);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.message = "";
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.message != null ? this.message : "");
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        for (int i = 0; i < intCount; i++) packet.readInt();

        String newMsg = packet.readString();
        if (newMsg == null) newMsg = "";
        int maxLen = Emulator.getConfig().getInt("hotel.wired.message.max_length", 100);
        if (newMsg.length() > maxLen) newMsg = newMsg.substring(0, maxLen);

        int itemCount = packet.readInt();
        for (int i = 0; i < itemCount; i++) packet.readInt();

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        this.message = newMsg;
        this.setDelay(delay);
        return true;
    }

    static class JsonData {
        String message;
        int delay;

        public JsonData(String message, int delay) {
            this.message = message;
            this.delay = delay;
        }
    }
}
