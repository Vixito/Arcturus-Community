package com.eu.habbo.habbohotel.items.interactions.wired.extra;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredConditionType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.stream.Collectors;

public class WiredSelectorRemoteStack extends InteractionWiredCondition {
    public static final WiredConditionType type = WiredConditionType.SELECTOR_REMOTE;

    private final THashSet<HabboItem> targetStacks = new THashSet<>();
    private int mode = 0; // 0: Union, 1: Intersection
    private int filterCount = 0; // 0: all stacks, >0: random X stacks

    public WiredSelectorRemoteStack(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredSelectorRemoteStack(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    public int getMode() {
        return this.mode;
    }

    public int getFilterCount() {
        return this.filterCount;
    }

    public THashSet<HabboItem> getTargetStacks() {
        return this.targetStacks;
    }

    @Override
    public WiredConditionType getType() {
        return WiredSelectorRemoteStack.type;
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        packet.readInt();
        this.targetStacks.clear();
        int count = packet.readInt();
        for (int i = 0; i < count; i++) {
            this.targetStacks.add(Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(packet.readInt()));
        }
        packet.readString();
        int paramCount = packet.readInt();
        if (paramCount >= 2) {
            this.mode = packet.readInt();
            this.filterCount = packet.readInt();
        }
        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
                this.mode,
                this.filterCount,
                this.targetStacks.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.targetStacks.clear();
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.mode = data.mode;
                this.filterCount = data.filterCount;
                if (room != null && data.itemIds != null) {
                    for (Integer id : data.itemIds) {
                        HabboItem item = room.getHabboItem(id);
                        if (item != null) {
                            this.targetStacks.add(item);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.targetStacks.clear();
        this.mode = 0;
        this.filterCount = 0;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        if (room != null) {
            this.targetStacks.removeIf(item -> item.getRoomId() != this.getRoomId() || room.getHabboItem(item.getId()) == null);
        }

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.targetStacks.size());
        for (HabboItem item : this.targetStacks) {
            message.appendInt(item.getId());
        }
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(2);
        message.appendInt(this.mode);
        message.appendInt(this.filterCount);
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    static class JsonData {
        int mode;
        int filterCount;
        java.util.List<Integer> itemIds;

        public JsonData(int mode, int filterCount, java.util.List<Integer> itemIds) {
            this.mode = mode;
            this.filterCount = filterCount;
            this.itemIds = itemIds;
        }
    }
}
