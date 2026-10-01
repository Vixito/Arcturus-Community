package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUserRotation;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserStatusComposer;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectMoveRotateUser extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.MOVE_ROTATE_USER;

    private int direction = 0; // 0: None, 1: N, 2: NE, 3: E, 4: SE, 5: S, 6: SW, 7: W, 8: NW, etc.
    private int rotation = 0;

    public WiredEffectMoveRotateUser(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectMoveRotateUser(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (roomUnit == null || room == null)
            return false;

        // 1. Rotation
        if (this.rotation > 0) {
            int newRot = (roomUnit.getBodyRotation().getValue() + this.rotation) % 8;
            roomUnit.setBodyRotation(RoomUserRotation.fromValue(newRot));
            roomUnit.setHeadRotation(RoomUserRotation.fromValue(newRot));
            room.sendComposer(new RoomUserStatusComposer(roomUnit).compose());
        }

        // 2. Movement
        if (this.direction > 0) {
            RoomUserRotation moveDir = RoomUserRotation.fromValue((this.direction - 1) % 8);
            short dx = 0;
            short dy = 0;

            if (moveDir == RoomUserRotation.WEST || moveDir == RoomUserRotation.NORTH_WEST || moveDir == RoomUserRotation.SOUTH_WEST) dx = -1;
            else if (moveDir == RoomUserRotation.EAST || moveDir == RoomUserRotation.NORTH_EAST || moveDir == RoomUserRotation.SOUTH_EAST) dx = 1;

            if (moveDir == RoomUserRotation.NORTH || moveDir == RoomUserRotation.NORTH_EAST || moveDir == RoomUserRotation.NORTH_WEST) dy = -1;
            else if (moveDir == RoomUserRotation.SOUTH || moveDir == RoomUserRotation.SOUTH_EAST || moveDir == RoomUserRotation.SOUTH_WEST) dy = 1;

            RoomTile targetTile = room.getLayout().getTile((short) (roomUnit.getX() + dx), (short) (roomUnit.getY() + dy));
            if (targetTile != null && room.tileWalkable(targetTile)) {
                roomUnit.setGoalLocation(targetTile);
            }
        }

        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.direction, this.rotation, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.direction = data.direction;
                this.rotation = data.rotation;
                this.setDelay(data.delay);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.direction = 0;
        this.rotation = 0;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return WiredEffectMoveRotateUser.type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(5);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(2);
        message.appendInt(this.direction);
        message.appendInt(this.rotation);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        packet.readInt();
        packet.readString();
        int intCount = packet.readInt();
        if (intCount >= 2) {
            this.direction = packet.readInt();
            this.rotation = packet.readInt();
        }
        this.setDelay(packet.readInt());
        return true;
    }

    static class JsonData {
        int direction;
        int rotation;
        int delay;

        public JsonData(int direction, int rotation, int delay) {
            this.direction = direction;
            this.rotation = rotation;
            this.delay = delay;
        }
    }
}
