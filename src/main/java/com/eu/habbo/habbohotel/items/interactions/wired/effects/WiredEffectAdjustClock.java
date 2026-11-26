package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameTimer;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredFurniSourceHelper;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectAdjustClock extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.ADJUST_CLOCK;

    public static final int MODE_INCREASE = 0;
    public static final int MODE_DECREASE = 1;
    public static final int MODE_SET = 2;

    private int mode = MODE_INCREASE;
    private int minutes = 0;
    private int seconds = 0;
    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    private THashSet<HabboItem> items;

    public WiredEffectAdjustClock(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectAdjustClock(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;

        THashSet<HabboItem> targetItems = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        if (targetItems.isEmpty()) return false;

        int totalSeconds = Math.max(0, this.minutes * 60 + this.seconds);
        boolean acted = false;
        for (HabboItem item : targetItems) {
            if (item instanceof InteractionGameTimer) {
                ((InteractionGameTimer) item).adjustTime(room, this.mode, totalSeconds);
                acted = true;
            }
        }
        return acted;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.mode,
            this.minutes,
            this.seconds,
            this.furniSource,
            this.getDelay(),
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        this.mode = MODE_INCREASE;
        this.minutes = 0;
        this.seconds = 0;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.setDelay(0);

        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.mode = Math.max(0, Math.min(2, data.mode));
                this.minutes = Math.max(0, Math.min(99, data.minutes));
                this.seconds = Math.max(0, Math.min(59, data.seconds));
                this.furniSource = WiredFurniSourceHelper.normalizeSourceCode(data.furniSource);
                this.setDelay(data.delay);
                if (data.itemIds != null && room != null) {
                    for (Integer id : data.itemIds) {
                        HabboItem item = room.getHabboItem(id);
                        if (item != null) this.items.add(item);
                    }
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.mode = MODE_INCREASE;
        this.minutes = 0;
        this.seconds = 0;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        THashSet<HabboItem> toRemove = new THashSet<>();
        for (HabboItem item : this.items) {
            if (item.getRoomId() != this.getRoomId() || room.getHabboItem(item.getId()) == null) {
                toRemove.add(item);
            }
        }
        for (HabboItem item : toRemove) this.items.remove(item);

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items) message.appendInt(item.getId());
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(3);
        message.appendInt(this.mode);
        message.appendInt(this.minutes);
        message.appendInt(this.seconds);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        int newMode = MODE_INCREASE;
        int newMinutes = 0;
        int newSeconds = 0;
        if (intCount >= 1) newMode = Math.max(0, Math.min(2, packet.readInt()));
        if (intCount >= 2) newMinutes = Math.max(0, Math.min(99, packet.readInt()));
        if (intCount >= 3) newSeconds = Math.max(0, Math.min(59, packet.readInt()));
        for (int i = 3; i < intCount; i++) packet.readInt();

        packet.readString();

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room == null) throw new WiredSaveException("Trying to save wired in unloaded room");

        int itemsCount = packet.readInt();
        if (itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) {
            throw new WiredSaveException("Too many furni selected");
        }

        THashSet<HabboItem> newItems = new THashSet<>();
        for (int i = 0; i < itemsCount; i++) {
            HabboItem it = room.getHabboItem(packet.readInt());
            if (it instanceof InteractionGameTimer) newItems.add(it);
        }

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        int newSource = WiredFurniSourceHelper.readOptionalSourceCode(packet);

        this.mode = newMode;
        this.minutes = newMinutes;
        this.seconds = newSeconds;
        this.furniSource = newSource;
        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);
        return true;
    }

    static class JsonData {
        int mode;
        int minutes;
        int seconds;
        int furniSource;
        int delay;
        List<Integer> itemIds;

        public JsonData(int mode, int minutes, int seconds, int furniSource, int delay, List<Integer> itemIds) {
            this.mode = mode;
            this.minutes = minutes;
            this.seconds = seconds;
            this.furniSource = furniSource;
            this.delay = delay;
            this.itemIds = itemIds;
        }
    }
}
