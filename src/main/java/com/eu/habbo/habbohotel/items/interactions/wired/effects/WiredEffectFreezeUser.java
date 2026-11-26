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

public class WiredEffectFreezeUser extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.FREEZE_USER;

    // 0: Sin efecto (0), 1: Wired Freeze (12), 2: Congelado (12), 3: Rayos X (53), 4: Pajaritos (108), 5: Trampa de arena (110)
    private static final int[] EFFECT_IDS = new int[] { 0, 12, 12, 53, 108, 110 };

    private int effectType = 1;
    private boolean unfreezeOnTeleport = false;
    private int userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
    private String message = "";

    public WiredEffectFreezeUser(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectFreezeUser(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;

        List<Habbo> targets = WiredUserSourceHelper.resolveUsers(this, this.userSource, roomUnit, room, stuff);
        if (targets.isEmpty()) return false;

        int effectId = EFFECT_IDS[Math.max(0, Math.min(EFFECT_IDS.length - 1, this.effectType))];

        for (Habbo habbo : targets) {
            if (habbo == null || habbo.getRoomUnit() == null) continue;
            RoomUnit unit = habbo.getRoomUnit();
            unit.stopWalking();
            unit.setCanWalk(false);
            unit.getCacheable().put("wired_frozen", Boolean.TRUE);
            unit.getCacheable().put("wired_freeze_unfreeze_tp", this.unfreezeOnTeleport);
            if (effectId > 0) {
                room.giveEffect(habbo, effectId, -1);
            }
            if (this.message != null && !this.message.isEmpty() && habbo.getClient() != null) {
                String msg = this.message.replace("%user%", habbo.getHabboInfo().getUsername());
                habbo.getClient().sendResponse(new RoomUserWhisperComposer(new RoomChatMessage(msg, habbo, habbo, RoomChatMessageBubbles.WIRED)));
            }
        }
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.effectType,
            this.unfreezeOnTeleport,
            this.userSource,
            this.message,
            this.getDelay()
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.effectType = 1;
        this.unfreezeOnTeleport = false;
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
        this.message = "";
        this.setDelay(0);

        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.effectType = Math.max(0, Math.min(5, data.effectType));
                this.unfreezeOnTeleport = data.unfreezeOnTeleport;
                this.userSource = WiredUserSourceHelper.normalizeUserSourceCode(data.userSource);
                this.message = data.message != null ? data.message : "";
                this.setDelay(data.delay);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.effectType = 1;
        this.unfreezeOnTeleport = false;
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
        message.appendInt(3);
        message.appendInt(this.effectType);
        message.appendInt(this.unfreezeOnTeleport ? 1 : 0);
        message.appendInt(this.userSource);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        int newEffect = intCount >= 1 ? Math.max(0, Math.min(5, packet.readInt())) : 1;
        boolean newUnfreezeTp = intCount >= 2 && packet.readInt() == 1;
        int newUserSource = intCount >= 3 ? WiredUserSourceHelper.normalizeUserSourceCode(packet.readInt()) : 0;
        for (int i = 3; i < intCount; i++) packet.readInt();

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

        this.effectType = newEffect;
        this.unfreezeOnTeleport = newUnfreezeTp;
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
        int effectType;
        boolean unfreezeOnTeleport;
        int userSource;
        String message;
        int delay;

        public JsonData(int effectType, boolean unfreezeOnTeleport, int userSource, String message, int delay) {
            this.effectType = effectType;
            this.unfreezeOnTeleport = unfreezeOnTeleport;
            this.userSource = userSource;
            this.message = message;
            this.delay = delay;
        }
    }
}
