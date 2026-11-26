package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredHighscore;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;

import java.sql.ResultSet;
import java.sql.SQLException;

public class WiredEffectResetHighscore extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.RESET_HIGHSCORE;

    private boolean confirmReset = false;

    public WiredEffectResetHighscore(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredEffectResetHighscore(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null || !this.confirmReset) return false;

        boolean resetAny = false;
        for (HabboItem item : room.getFloorItems()) {
            if (item instanceof InteractionWiredHighscore) {
                InteractionWiredHighscore hs = (InteractionWiredHighscore) item;
                Emulator.getGameEnvironment().getItemManager().getHighscoreManager().clearHighscoreDataForItem(hs.getId());
                hs.reloadData();
                room.updateItem(hs);
                resetAny = true;
            }
        }
        return resetAny;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.confirmReset, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.confirmReset = false;
        this.setDelay(0);
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.confirmReset = data.confirmReset;
                this.setDelay(data.delay);
            }
        }
    }

    @Override
    public void onPickUp() {
        this.confirmReset = false;
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
        message.appendInt(1);
        message.appendInt(this.confirmReset ? 1 : 0);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        boolean newConfirm = intCount >= 1 && packet.readInt() == 1;
        for (int i = 1; i < intCount; i++) packet.readInt();

        packet.readString();

        int itemCount = packet.readInt();
        for (int i = 0; i < itemCount; i++) packet.readInt();

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        this.confirmReset = newConfirm;
        this.setDelay(delay);
        return true;
    }

    static class JsonData {
        boolean confirmReset;
        int delay;

        public JsonData(boolean confirmReset, int delay) {
            this.confirmReset = confirmReset;
            this.delay = delay;
        }
    }
}
