package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.DanceType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredUserSourceHelper;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserDanceComposer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WiredEffectGiveDance extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.GIVE_DANCE;

    private int danceId = 0;
    private int userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;

    public WiredEffectGiveDance(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectGiveDance(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    private static DanceType resolveDanceType(int id) {
        for (DanceType d : DanceType.values()) {
            if (d.getType() == id) return d;
        }
        return DanceType.NONE;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;

        List<Habbo> targets = WiredUserSourceHelper.resolveUsers(this, this.userSource, roomUnit, room, stuff);
        if (targets.isEmpty()) return false;

        DanceType dance = resolveDanceType(this.danceId);
        for (Habbo habbo : targets) {
            if (habbo == null || habbo.getRoomUnit() == null) continue;
            habbo.getRoomUnit().setDanceType(dance);
            room.sendComposer(new RoomUserDanceComposer(habbo.getRoomUnit()).compose());
        }
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.danceId,
            this.userSource,
            this.getDelay()
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.danceId = 0;
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        this.setDelay(0);

        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.danceId = Math.max(0, Math.min(4, data.danceId));
                this.userSource = WiredUserSourceHelper.normalizeUserSourceCode(data.userSource);
                this.setDelay(data.delay);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.danceId = 0;
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
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
        message.appendString("");
        message.appendInt(2);
        message.appendInt(this.danceId);
        message.appendInt(this.userSource);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        int newDanceId = intCount >= 1 ? Math.max(0, Math.min(4, packet.readInt())) : 0;
        int newUserSource = intCount >= 2 ? WiredUserSourceHelper.normalizeUserSourceCode(packet.readInt()) : 0;
        for (int i = 2; i < intCount; i++) packet.readInt();

        packet.readString();

        int itemCount = packet.readInt();
        for (int i = 0; i < itemCount; i++) packet.readInt();

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        this.danceId = newDanceId;
        this.userSource = newUserSource;
        this.setDelay(delay);
        return true;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return this.userSource == WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
    }

    static class JsonData {
        int danceId;
        int userSource;
        int delay;

        public JsonData(int danceId, int userSource, int delay) {
            this.danceId = danceId;
            this.userSource = userSource;
            this.delay = delay;
        }
    }
}
