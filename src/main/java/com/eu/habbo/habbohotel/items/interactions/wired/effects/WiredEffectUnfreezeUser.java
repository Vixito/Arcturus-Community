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
import com.eu.habbo.habbohotel.wired.WiredUserSourceHelper;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserWhisperComposer;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

public class WiredEffectUnfreezeUser extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.UNFREEZE_USER;

    private int userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
    private String message = "";

    public WiredEffectUnfreezeUser(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectUnfreezeUser(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;

        List<Habbo> targets = WiredUserSourceHelper.resolveUsers(this, this.userSource, roomUnit, room, stuff);
        if (targets.isEmpty()) return false;

        for (Habbo habbo : targets) {
            if (habbo == null || habbo.getRoomUnit() == null) continue;
            RoomUnit unit = habbo.getRoomUnit();
            unit.setCanWalk(true);
            unit.getCacheable().remove("wired_frozen");
            unit.getCacheable().remove("wired_freeze_unfreeze_tp");
            room.giveEffect(habbo, 0, -1);
            if (this.message != null && !this.message.isEmpty() && habbo.getClient() != null) {
                String msg = this.message.replace("%user%", habbo.getHabboInfo().getUsername());
                habbo.getClient().sendResponse(new RoomUserWhisperComposer(new RoomChatMessage(msg, habbo, habbo, RoomChatMessageBubbles.WIRED)));
            }
        }
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.userSource, this.message, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        this.message = "";
        this.setDelay(0);
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.userSource = WiredUserSourceHelper.normalizeUserSourceCode(data.userSource);
                this.message = data.message != null ? data.message : "";
                this.setDelay(data.delay);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
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
        int newUserSource = intCount >= 1 ? WiredUserSourceHelper.normalizeUserSourceCode(packet.readInt()) : 0;
        for (int i = 1; i < intCount; i++) packet.readInt();

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

        this.userSource = newUserSource;
        this.message = newMsg;
        this.setDelay(delay);
        return true;
    }

    @Override
    public boolean requiresTriggeringUser() {
        return this.userSource == WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
    }

    static class JsonData {
        int userSource;
        String message;
        int delay;

        public JsonData(int userSource, String message, int delay) {
            this.userSource = userSource;
            this.message = message;
            this.delay = delay;
        }
    }
}
