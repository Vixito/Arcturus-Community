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
import java.util.ArrayList;
import java.util.List;

public class WiredSelectorFurniSelection extends InteractionWiredCondition {
    protected THashSet<HabboItem> items = new THashSet<>();

    public WiredSelectorFurniSelection(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredSelectorFurniSelection(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    public THashSet<HabboItem> getSelectedItems() {
        return this.items;
    }

    @Override
    public WiredConditionType getType() {
        String name = this.getBaseItem().getName();
        switch (name) {
            case "wf_slc_users_onfurni":
                return WiredConditionType.SELECTOR_USERS_ONFURNI;
            case "wf_slc_furni_onfurni":
                return WiredConditionType.SELECTOR_FURNI_ONFURNI;
            case "wf_slc_furni_picks":
                return WiredConditionType.SELECTOR_FURNI_PICKS;
            case "wf_slc_furni_bytype":
                return WiredConditionType.SELECTOR_FURNI_BYTYPE;
            case "wf_slc_users_area":
            case "wf_slc_furni_area":
                return WiredConditionType.SELECTOR_AREA;
            default:
                return WiredConditionType.SELECTOR_FURNI_PICKS;
        }
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        this.refresh();

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
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        int intCount = packet.readInt();
        for (int i = 0; i < intCount; i++) {
            packet.readInt();
        }
        packet.readString();

        int count = packet.readInt();
        if (count > Emulator.getConfig().getInt("hotel.wired.furni.selection.count", 50)) return false;

        this.items.clear();
        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room != null) {
            for (int i = 0; i < count; i++) {
                HabboItem item = room.getHabboItem(packet.readInt());
                if (item != null) {
                    this.items.add(item);
                }
            }
        }

        return true;
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return true;
    }

    @Override
    public String getWiredData() {
        this.refresh();
        List<Integer> ids = new ArrayList<>();
        for (HabboItem item : this.items) {
            ids.add(item.getId());
        }
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(ids));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items.clear();
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null && data.itemIds != null) {
                for (int id : data.itemIds) {
                    HabboItem item = room.getHabboItem(id);
                    if (item != null) {
                        this.items.add(item);
                    }
                }
            }
        } else if (wiredData != null && !wiredData.isEmpty()) {
            String[] parts = wiredData.split(";");
            for (String part : parts) {
                try {
                    HabboItem item = room.getHabboItem(Integer.parseInt(part));
                    if (item != null) this.items.add(item);
                } catch (Exception ignored) {}
            }
        }
    }

    protected void refresh() {
        THashSet<HabboItem> toRemove = new THashSet<>();
        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room != null) {
            for (HabboItem item : this.items) {
                if (item.getRoomId() != room.getId()) {
                    toRemove.add(item);
                }
            }
        }
        this.items.removeAll(toRemove);
    }

    @Override
    public void onPickUp() {
        this.items.clear();
    }

    static class JsonData {
        List<Integer> itemIds;

        public JsonData(List<Integer> itemIds) {
            this.itemIds = itemIds;
        }
    }
}
