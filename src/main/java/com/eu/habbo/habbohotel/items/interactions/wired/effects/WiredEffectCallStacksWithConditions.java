package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredConditionOperator;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredFurniSourceHelper;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectCallStacksWithConditions extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.CALL_STACKS_CONDITIONS;

    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    private THashSet<HabboItem> items;

    public WiredEffectCallStacksWithConditions(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectCallStacksWithConditions(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null || room.getLayout() == null || room.getRoomSpecialTypes() == null) return false;
        if (stuff != null && stuff.length >= 1 && stuff[stuff.length - 1] instanceof WiredEffectCallStacksWithConditions) {
            return false;
        }

        THashSet<HabboItem> targetItems = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        if (targetItems.isEmpty()) return false;

        THashSet<RoomTile> validTiles = new THashSet<>();
        THashSet<RoomTile> checkedTiles = new THashSet<>();

        for (HabboItem item : targetItems) {
            if (item == null) continue;
            RoomTile tile = room.getLayout().getTile(item.getX(), item.getY());
            if (tile == null || checkedTiles.contains(tile)) continue;
            checkedTiles.add(tile);

            THashSet<InteractionWiredCondition> conditions = room.getRoomSpecialTypes().getConditions(tile.x, tile.y);
            boolean conditionsPassed = true;

            if (conditions != null && !conditions.isEmpty()) {
                ArrayList<WiredConditionType> matchedOrTypes = new ArrayList<>();
                for (InteractionWiredCondition c : conditions) {
                    if (!matchedOrTypes.contains(c.getType()) && c.operator() == WiredConditionOperator.OR && c.execute(roomUnit, room, stuff)) {
                        matchedOrTypes.add(c.getType());
                    }
                }

                for (InteractionWiredCondition c : conditions) {
                    boolean ok = (c.operator() == WiredConditionOperator.OR && matchedOrTypes.contains(c.getType()))
                            || (c.operator() == WiredConditionOperator.AND && c.execute(roomUnit, room, stuff));
                    if (!ok) {
                        conditionsPassed = false;
                        break;
                    }
                }
            }

            if (conditionsPassed) {
                validTiles.add(tile);
            }
        }

        if (validTiles.isEmpty()) return false;

        Object[] baseStuff = stuff != null ? stuff : new Object[0];
        Object[] newStuff = new Object[baseStuff.length + 1];
        System.arraycopy(baseStuff, 0, newStuff, 0, baseStuff.length);
        newStuff[newStuff.length - 1] = this;

        for (RoomTile tile : validTiles) {
            THashSet<HabboItem> tileItems = room.getItemsAt(tile);
            long millis = room.getCycleTimestamp();
            for (final HabboItem ti : tileItems) {
                if (ti instanceof InteractionWiredEffect
                        && !(ti instanceof WiredEffectTriggerStacks)
                        && !(ti instanceof WiredEffectCallStacksWithConditions)) {
                    InteractionWiredEffect eff = (InteractionWiredEffect) ti;
                    if (!eff.requiresTriggeringUser() || roomUnit != null) {
                        Emulator.getThreading().run(() -> {
                            if (room.isLoaded()) {
                                if (eff.execute(roomUnit, room, newStuff)) {
                                    eff.setCooldown(millis);
                                    eff.activateBox(room, roomUnit, millis);
                                }
                            }
                        }, eff.getDelay() * 500L);
                    }
                }
            }
        }

        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.getDelay(),
            this.furniSource,
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.setDelay(0);

        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.setDelay(data.delay);
                this.furniSource = WiredFurniSourceHelper.normalizeSourceCode(data.furniSource);
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
        for (HabboItem item : toRemove) {
            this.items.remove(item);
        }

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items) {
            message.appendInt(item.getId());
        }
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(0);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        for (int i = 0; i < intCount; i++) packet.readInt();
        packet.readString();

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room == null) throw new WiredSaveException("Trying to save wired in unloaded room");

        int itemsCount = packet.readInt();
        if (itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) {
            throw new WiredSaveException("Too many furni selected");
        }

        List<HabboItem> newItems = new ArrayList<>();
        for (int i = 0; i < itemsCount; i++) {
            int itemId = packet.readInt();
            HabboItem it = room.getHabboItem(itemId);
            if (it != null) newItems.add(it);
        }

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        int newSource = WiredFurniSourceHelper.readOptionalSourceCode(packet);

        this.items.clear();
        this.items.addAll(newItems);
        this.furniSource = newSource;
        this.setDelay(delay);
        return true;
    }

    @Override
    protected long requiredCooldown() {
        return 250;
    }

    static class JsonData {
        int delay;
        int furniSource;
        List<Integer> itemIds;

        public JsonData(int delay, int furniSource, List<Integer> itemIds) {
            this.delay = delay;
            this.furniSource = furniSource;
            this.itemIds = itemIds;
        }
    }
}
