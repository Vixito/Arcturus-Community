package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.messages.outgoing.inventory.RemoveHabboItemComposer;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SyncRoomBackup extends RCONMessage<SyncRoomBackup.SyncRoomBackupJSON> {

    public SyncRoomBackup() {
        super(SyncRoomBackupJSON.class);
    }

    @Override
    public void handle(Gson gson, SyncRoomBackupJSON json) {
        if (json == null || json.action == null || json.room_id <= 0) {
            this.status = RCONMessage.STATUS_ERROR;
            this.message = "invalid_payload";
            return;
        }

        String action = json.action.toLowerCase().trim();
        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(json.room_id);

        if ("flush".equals(action)) {
            if (room != null) {
                try {
                    room.run();
                    for (HabboItem item : new ArrayList<>(room.getFloorItems())) {
                        if (item != null && item.needsUpdate()) {
                            item.run();
                        }
                    }
                    for (HabboItem item : new ArrayList<>(room.getWallItems())) {
                        if (item != null && item.needsUpdate()) {
                            item.run();
                        }
                    }
                } catch (Exception e) {
                    this.status = RCONMessage.STATUS_ERROR;
                    this.message = "flush_failed:" + e.getMessage();
                    return;
                }
            }
            this.status = RCONMessage.STATUS_OK;
            this.message = "flushed";
            return;
        }

        if ("prepare".equals(action)) {
            if (room != null) {
                try {
                    // Move all online users out of the room cleanly before unloading
                    for (Habbo h : new ArrayList<>(room.getCurrentHabbos().values())) {
                        if (h != null) {
                            Emulator.getGameEnvironment().getRoomManager().leaveRoom(h, room, true);
                        }
                    }
                    room.preventUnloading = false;
                    room.dispose();
                    Emulator.getGameEnvironment().getRoomManager().uncacheRoom(room);
                } catch (Exception e) {
                    this.status = RCONMessage.STATUS_ERROR;
                    this.message = "prepare_failed:" + e.getMessage();
                    return;
                }
            }
            this.status = RCONMessage.STATUS_OK;
            this.message = "prepared";
            return;
        }

        if ("finish".equals(action)) {
            // Ensure room stays uncached so next RoomEnter loads 100% fresh from MySQL
            if (room != null) {
                try {
                    for (Habbo h : new ArrayList<>(room.getCurrentHabbos().values())) {
                        if (h != null) {
                            Emulator.getGameEnvironment().getRoomManager().leaveRoom(h, room, true);
                        }
                    }
                    room.preventUnloading = false;
                    room.dispose();
                    Emulator.getGameEnvironment().getRoomManager().uncacheRoom(room);
                } catch (Exception ignored) {
                }
            }

            // Remove placed item IDs from the owner's in-memory inventory so there is zero desync
            if (json.user_id > 0 && json.placed_ids != null && !json.placed_ids.isEmpty()) {
                Habbo owner = Emulator.getGameEnvironment().getHabboManager().getHabbo(json.user_id);
                if (owner != null) {
                    boolean changed = false;
                    for (Integer id : json.placed_ids) {
                        if (id == null || id <= 0) continue;
                        HabboItem invItem = owner.getInventory().getItemsComponent().getHabboItem(id);
                        if (invItem != null) {
                            owner.getInventory().getItemsComponent().removeHabboItem(invItem);
                            if (owner.getClient() != null) {
                                owner.getClient().sendResponse(new RemoveHabboItemComposer(invItem.getGiftAdjustedId()));
                            }
                            changed = true;
                        }
                    }
                    if (changed && owner.getClient() != null) {
                        owner.getClient().sendResponse(new InventoryRefreshComposer());
                    }
                }
            }

            // Load any items returned from the previous room layout back into each online owner's inventory
            if (json.returned_by_user != null && !json.returned_by_user.isEmpty()) {
                for (Map.Entry<String, List<Integer>> entry : json.returned_by_user.entrySet()) {
                    int uid;
                    try {
                        uid = Integer.parseInt(entry.getKey());
                    } catch (NumberFormatException ignored) {
                        continue;
                    }
                    if (uid <= 0 || entry.getValue() == null || entry.getValue().isEmpty()) continue;

                    Habbo targetHabbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(uid);
                    if (targetHabbo == null) continue;

                    boolean addedAny = false;
                    for (Integer itemId : entry.getValue()) {
                        if (itemId == null || itemId <= 0) continue;
                        if (targetHabbo.getInventory().getItemsComponent().getHabboItem(itemId) != null) continue;
                        HabboItem loaded = Emulator.getGameEnvironment().getItemManager().loadHabboItem(itemId);
                        if (loaded != null && loaded.getRoomId() == 0 && loaded.getUserId() == uid) {
                            targetHabbo.getInventory().getItemsComponent().addItem(loaded);
                            if (targetHabbo.getClient() != null) {
                                targetHabbo.getClient().sendResponse(new AddHabboItemComposer(loaded));
                            }
                            addedAny = true;
                        }
                    }
                    if (addedAny && targetHabbo.getClient() != null) {
                        targetHabbo.getClient().sendResponse(new InventoryRefreshComposer());
                    }
                }
            }

            this.status = RCONMessage.STATUS_OK;
            this.message = "finished";
            return;
        }

        this.status = RCONMessage.STATUS_ERROR;
        this.message = "unknown_action";
    }

    static class SyncRoomBackupJSON {
        public String action = "";
        public int room_id = -1;
        public int user_id = -1;
        public List<Integer> placed_ids = new ArrayList<>();
        public Map<String, List<Integer>> returned_by_user = new HashMap<>();
    }
}
