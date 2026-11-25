package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredFurniSourceHelper;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class WiredTriggerStuffState extends InteractionWiredTrigger {
    private static final WiredTriggerType type = WiredTriggerType.STUFF_STATE;

    // 0 = Causante para el estado actual (match saved state), 1 = Para todos los estados
    private int mode = 0;
    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    private THashSet<HabboItem> items;
    private Map<Integer, String> savedStates;

    public WiredTriggerStuffState(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
        this.savedStates = new HashMap<>();
    }

    public WiredTriggerStuffState(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
        this.savedStates = new HashMap<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null || stuff == null || stuff.length < 1 || !(stuff[0] instanceof HabboItem)) {
            return false;
        }

        HabboItem changedItem = (HabboItem) stuff[0];
        THashSet<HabboItem> resolved = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        if (!resolved.contains(changedItem)) {
            return false;
        }

        if (this.mode == 0) {
            String targetState = this.savedStates.get(changedItem.getId());
            if (targetState == null && !this.savedStates.isEmpty()) {
                targetState = this.savedStates.values().iterator().next();
            }
            if (targetState != null) {
                String current = changedItem.getExtradata() != null ? changedItem.getExtradata() : "";
                return current.equals(targetState);
            }
        }

        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.mode,
            this.furniSource,
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList()),
            this.savedStates
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        this.savedStates = new HashMap<>();
        this.mode = 0;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;

        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.mode = (data.mode == 1) ? 1 : 0;
                this.furniSource = WiredFurniSourceHelper.normalizeSourceCode(data.furniSource);
                if (data.itemIds != null && room != null) {
                    for (Integer id : data.itemIds) {
                        HabboItem item = room.getHabboItem(id);
                        if (item != null) {
                            this.items.add(item);
                        }
                    }
                }
                if (data.savedStates != null) {
                    this.savedStates.putAll(data.savedStates);
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.savedStates.clear();
        this.mode = 0;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    }

    @Override
    public WiredTriggerType getType() {
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
            this.savedStates.remove(item.getId());
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
        message.appendInt(1);
        message.appendInt(this.mode);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        int intCount = packet.readInt();
        if (intCount >= 1) {
            this.mode = (packet.readInt() == 1) ? 1 : 0;
        } else {
            this.mode = 0;
        }
        for (int i = 1; i < intCount; i++) {
            packet.readInt();
        }

        packet.readString();

        this.items.clear();
        this.savedStates.clear();

        int count = packet.readInt();
        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room != null) {
            int limit = Math.min(count, WiredHandler.MAXIMUM_FURNI_SELECTION);
            for (int i = 0; i < count; i++) {
                int itemId = packet.readInt();
                if (i >= limit) continue;
                HabboItem item = room.getHabboItem(itemId);
                if (item != null) {
                    this.items.add(item);
                    this.savedStates.put(item.getId(), item.getExtradata() != null ? item.getExtradata() : "");
                }
            }
        }

        this.furniSource = WiredFurniSourceHelper.readOptionalSourceCode(packet);
        return true;
    }

    @Override
    public boolean isTriggeredByRoomUnit() {
        return false;
    }

    static class JsonData {
        int mode;
        int furniSource;
        List<Integer> itemIds;
        Map<Integer, String> savedStates;

        public JsonData(int mode, int furniSource, List<Integer> itemIds, Map<Integer, String> savedStates) {
            this.mode = mode;
            this.furniSource = furniSource;
            this.itemIds = itemIds;
            this.savedStates = savedStates;
        }
    }
}
