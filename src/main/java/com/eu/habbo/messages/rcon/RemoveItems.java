package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import com.eu.habbo.messages.outgoing.inventory.RemoveHabboItemComposer;
import com.eu.habbo.threading.runnables.QueryDeleteHabboItem;
import com.google.gson.Gson;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class RemoveItems extends RCONMessage<RemoveItems.RemoveItemsJSON> {

    public RemoveItems() {
        super(RemoveItemsJSON.class);
    }

    @Override
    public void handle(Gson gson, RemoveItemsJSON json) {
        if (json == null || json.user_id <= 0 || json.item_ids == null || json.item_ids.isEmpty()) {
            this.status = RCONMessage.STATUS_ERROR;
            this.message = "invalid_payload";
            return;
        }

        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(json.user_id);
        if (habbo == null) {
            this.status = RCONMessage.STATUS_OK;
            this.message = "user_offline";
            return;
        }

        Set<Integer> uniqueIds = new HashSet<>();
        for (Integer id : json.item_ids) {
            if (id != null && id > 0) uniqueIds.add(id);
        }

        List<HabboItem> found = new ArrayList<>();
        for (int id : uniqueIds) {
            HabboItem item = habbo.getInventory().getItemsComponent().getHabboItem(id);
            if (item == null || item.getRoomId() != 0) {
                this.status = RCONMessage.STATUS_ERROR;
                this.message = "item_not_in_inventory:" + id;
                return;
            }
            found.add(item);
        }

        for (HabboItem item : found) {
            habbo.getInventory().getItemsComponent().removeHabboItem(item);
            if (habbo.getClient() != null) {
                habbo.getClient().sendResponse(new RemoveHabboItemComposer(item.getGiftAdjustedId()));
            }
            Emulator.getThreading().run(new QueryDeleteHabboItem(item.getId()));
        }

        if (habbo.getClient() != null) {
            habbo.getClient().sendResponse(new InventoryRefreshComposer());
        }

        this.status = RCONMessage.STATUS_OK;
        this.message = "removed:" + found.size();
    }

    static class RemoveItemsJSON {
        public int user_id = -1;
        public List<Integer> item_ids = new ArrayList<>();
    }
}
