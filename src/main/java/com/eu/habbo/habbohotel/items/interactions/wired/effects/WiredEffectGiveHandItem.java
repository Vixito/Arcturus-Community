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
import com.eu.habbo.habbohotel.wired.WiredUserSourceHelper;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WiredEffectGiveHandItem extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.GIVE_HANDITEM;

    private String message = "0";
    private int userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;

    public WiredEffectGiveHandItem(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectGiveHandItem(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;
        List<Habbo> targets = WiredUserSourceHelper.resolveUsers(this, this.userSource, roomUnit, room, stuff);
        if (targets.isEmpty()) return false;

        int itemId = 0;
        try {
            itemId = Integer.parseInt(this.message.trim());
        } catch (NumberFormatException ignored) {
            return false;
        }
        if (itemId < 0) itemId = 0;

        for (Habbo habbo : targets) {
            if (habbo == null || habbo.getRoomUnit() == null) continue;
            room.giveHandItem(habbo, itemId);
        }
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.message, this.userSource, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.message = "0";
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        this.setDelay(0);
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.message = data.message != null ? data.message : "0";
                this.userSource = WiredUserSourceHelper.normalizeUserSourceCode(data.userSource);
                this.setDelay(data.delay);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.message = "0";
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
        message.appendString(this.message != null ? this.message : "");
        message.appendInt(1);
        message.appendInt(this.userSource);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        int newSource = intCount >= 1 ? WiredUserSourceHelper.normalizeUserSourceCode(packet.readInt()) : 0;
        for (int i = 1; i < intCount; i++) packet.readInt();

        String newMsg = packet.readString();
        if (newMsg == null) newMsg = "0";
        if (newMsg.length() > 32) newMsg = newMsg.substring(0, 32);

        int itemCount = packet.readInt();
        for (int i = 0; i < itemCount; i++) packet.readInt();

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        this.message = newMsg.trim();
        this.userSource = newSource;
        this.setDelay(delay);
        return true;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return this.userSource == WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
    }

    static class JsonData {
        String message;
        int userSource;
        int delay;

        public JsonData(String message, int userSource, int delay) {
            this.message = message;
            this.userSource = userSource;
            this.delay = delay;
        }
    }
}
