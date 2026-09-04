package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class WiredTriggerClickTile extends InteractionWiredTrigger {
    private static final WiredTriggerType type = WiredTriggerType.CLICK_TILE;

    private THashSet<HabboItem> items;

    public WiredTriggerClickTile(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredTriggerClickTile(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (stuff != null && stuff.length >= 1) {
            Habbo habbo = room.getHabbo(roomUnit);
            if (habbo != null) {
                for (Object object : stuff) {
                    if (object instanceof WiredEffectType) {
                        return false;
                    }
                }

                if (stuff[0] instanceof RoomTile) {
                    RoomTile tile = (RoomTile) stuff[0];
                    for (HabboItem item : this.items) {
                        boolean match = false;
                        if (item.getX() == tile.x && item.getY() == tile.y) {
                            match = true;
                        } else if (room.getLayout() != null) {
                            THashSet<RoomTile> occupied = room.getLayout().getTilesAt(room.getLayout().getTile(item.getX(), item.getY()), item.getBaseItem().getWidth(), item.getBaseItem().getLength(), item.getRotation());
                            if (occupied != null && occupied.contains(tile)) {
                                match = true;
                            }
                        }

                        if (match) {
                            long now = System.currentTimeMillis();
                            String key = "last_click_tile_" + item.getId() + "_" + tile.x + "_" + tile.y;
                            Long lastClick = (Long) roomUnit.getCacheable().get(key);
                            if (lastClick != null && (now - lastClick) < 350) {
                                return false;
                            }
                            roomUnit.getCacheable().put(key, now);
                            return true;
                        }
                    }
                } else if (stuff[0] instanceof HabboItem) {
                    HabboItem item = (HabboItem) stuff[0];
                    if (this.items.contains(item)) {
                        long now = System.currentTimeMillis();
                        String key = "last_click_tile_item_" + item.getId();
                        Long lastClick = (Long) roomUnit.getCacheable().get(key);
                        if (lastClick != null && (now - lastClick) < 350) {
                            return false;
                        }
                        roomUnit.getCacheable().put(key, now);
                        return true;
                    }
                }
            }
        }
        return false;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        String wiredData = set.getString("wired_data");

        if (wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null && data.itemIds != null) {
                for (Integer id : data.itemIds) {
                    HabboItem item = room.getHabboItem(id);
                    if (item != null) {
                        this.items.add(item);
                    }
                }
            }
        } else if (wiredData.split(":").length >= 3) {
            super.setDelay(Integer.parseInt(wiredData.split(":")[0]));

            if (!wiredData.split(":")[2].equals("\t")) {
                for (String s : wiredData.split(":")[2].split(";")) {
                    try {
                        HabboItem item = room.getHabboItem(Integer.parseInt(s));
                        if (item != null)
                            this.items.add(item);
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
    }

    @Override
    public WiredTriggerType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        THashSet<HabboItem> itemsToRemove = new THashSet<>();

        for (HabboItem item : this.items) {
            if (item.getRoomId() != this.getRoomId() || room.getHabboItem(item.getId()) == null) {
                itemsToRemove.add(item);
            }
        }

        for (HabboItem item : itemsToRemove) {
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
        message.appendInt(0);
        message.appendInt(this.getType().code);
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        packet.readInt();
        packet.readString();

        this.items.clear();

        int count = packet.readInt();
        for (int i = 0; i < count; i++) {
            HabboItem item = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId()).getHabboItem(packet.readInt());
            if (item != null) {
                this.items.add(item);
            }
        }

        return true;
    }

    @Override
    public boolean isTriggeredByRoomUnit() {
        return true;
    }

    static class JsonData {
        List<Integer> itemIds;

        public JsonData(List<Integer> itemIds) {
            this.itemIds = itemIds;
        }
    }
}
