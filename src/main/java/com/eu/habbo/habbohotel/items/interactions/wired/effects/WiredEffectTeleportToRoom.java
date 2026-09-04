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
import com.eu.habbo.messages.outgoing.rooms.ForwardToRoomComposer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectTeleportToRoom extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.TELEPORT_TO_ROOM;

    private int destinationRoomId = 0;

    public WiredEffectTeleportToRoom(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectTeleportToRoom(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null || roomUnit == null || this.destinationRoomId <= 0) {
            return false;
        }

        Habbo habbo = room.getHabbo(roomUnit);
        if (habbo != null && habbo.getClient() != null) {
            habbo.getClient().sendResponse(new ForwardToRoomComposer(this.destinationRoomId));
            Emulator.getGameEnvironment().getRoomManager().enterRoom(habbo, this.destinationRoomId, "", true);
            return true;
        }

        return false;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.destinationRoomId, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.destinationRoomId = data.destinationRoomId;
                this.setDelay(data.delay);
            }
        } else if (wiredData != null && !wiredData.isEmpty()) {
            try {
                String[] parts = wiredData.split("\t");
                if (parts.length >= 1) {
                    this.setDelay(Integer.parseInt(parts[0]));
                }
                if (parts.length >= 2) {
                    this.destinationRoomId = Integer.parseInt(parts[1]);
                }
            } catch (Exception ignored) {
            }
        }
    }

    @Override
    public void onPickUp() {
        this.destinationRoomId = 0;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(0); // maximum item selection count
        message.appendInt(0); // selected item count
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString(this.destinationRoomId > 0 ? String.valueOf(this.destinationRoomId) : "");
        message.appendInt(1);
        message.appendInt(this.destinationRoomId);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0); // conflicting triggers count
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        if (intCount > 0) {
            this.destinationRoomId = packet.readInt();
            for (int i = 1; i < intCount; i++) {
                packet.readInt();
            }
        }

        String strParam = packet.readString();
        if (this.destinationRoomId <= 0 && strParam != null && !strParam.trim().isEmpty()) {
            try {
                this.destinationRoomId = Integer.parseInt(strParam.trim());
            } catch (NumberFormatException ignored) {
            }
        }

        int furniCount = packet.readInt();
        for (int i = 0; i < furniCount; i++) {
            packet.readInt();
        }

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }
        this.setDelay(delay);

        return true;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return true;
    }

    public int getDestinationRoomId() {
        return this.destinationRoomId;
    }

    static class JsonData {
        int destinationRoomId;
        int delay;

        public JsonData(int destinationRoomId, int delay) {
            this.destinationRoomId = destinationRoomId;
            this.delay = delay;
        }
    }
}
