package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WiredEffectClickConf extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.CLICK_CONF;

    protected THashSet<HabboItem> items;
    private int clickAction = 0; // 0 = default interaction, 1 = block click, 2 = walk to furni
    private int permissions = 0; // 0 = everyone, 1 = rights/owner only

    public WiredEffectClickConf(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectClickConf(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        // Effect executes successfully when triggered
        return true;
    }

    @Override
    public String getWiredData() {
        List<Integer> itemIds = new ArrayList<>();
        for (HabboItem item : this.items) {
            itemIds.add(item.getId());
        }
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(itemIds, this.clickAction, this.permissions, this.getDelay()));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items.clear();
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.clickAction = data.clickAction;
                this.permissions = data.permissions;
                this.setDelay(data.delay);
                if (data.itemIds != null) {
                    for (int id : data.itemIds) {
                        HabboItem item = room.getHabboItem(id);
                        if (item != null) {
                            this.items.add(item);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.clickAction = 0;
        this.permissions = 0;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        this.items.removeIf(item -> item == null || item.getRoomId() != this.getRoomId());

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items) {
            message.appendInt(item.getId());
        }
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(2);
        message.appendInt(this.clickAction);
        message.appendInt(this.permissions);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room == null) return false;

        int intCount = packet.readInt();
        if (intCount >= 1) {
            this.clickAction = packet.readInt();
        }
        if (intCount >= 2) {
            this.permissions = packet.readInt();
        }
        for (int i = 2; i < intCount; i++) {
            packet.readInt();
        }

        packet.readString();

        int furniCount = packet.readInt();
        if (furniCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count", 5)) {
            return false;
        }

        this.items.clear();
        for (int i = 0; i < furniCount; i++) {
            HabboItem item = room.getHabboItem(packet.readInt());
            if (item != null) {
                this.items.add(item);
            }
        }

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }
        this.setDelay(delay);

        return true;
    }

    public int getClickAction() {
        return this.clickAction;
    }

    public int getPermissions() {
        return this.permissions;
    }

    public THashSet<HabboItem> getItems() {
        return this.items;
    }

    static class JsonData {
        List<Integer> itemIds;
        int clickAction;
        int permissions;
        int delay;

        public JsonData(List<Integer> itemIds, int clickAction, int permissions, int delay) {
            this.itemIds = itemIds;
            this.clickAction = clickAction;
            this.permissions = permissions;
            this.delay = delay;
        }
    }
}
