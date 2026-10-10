package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredHighscore;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.inventory.AddHabboItemComposer;
import com.eu.habbo.messages.outgoing.inventory.InventoryRefreshComposer;
import gnu.trove.map.hash.THashMap;
import gnu.trove.set.hash.THashSet;

import java.util.Map;

public class PickWiredCommand extends Command {
    public PickWiredCommand() {
        super("cmd_pickwired", Emulator.getTexts().getValue("commands.keys.cmd_pickwired", "pickwired;pickupwired;pickwireds;pickupwireds").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Habbo habbo = gameClient.getHabbo();
        if (habbo == null) return false;

        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return false;

        boolean isOwner = room.isOwner(habbo) || habbo.hasPermission("acc_anyroomowner");
        boolean hasRights = isOwner || room.hasRights(habbo);

        if (!hasRights) {
            habbo.whisper(Emulator.getTexts().getValue("commands.errors.cmd_pickwired.permission", "Debes ser el dueño de la sala o tener derechos para recoger wireds."));
            return true;
        }

        THashSet<HabboItem> floorItems = room.getFloorItems();
        if (floorItems == null || floorItems.isEmpty()) {
            habbo.whisper(Emulator.getTexts().getValue("commands.errors.cmd_pickwired.empty", "No se encontraron wireds para recoger en esta sala."));
            return true;
        }

        THashMap<Integer, THashSet<HabboItem>> userItemsMap = new THashMap<>();
        int myUserId = habbo.getHabboInfo().getId();

        for (HabboItem item : floorItems) {
            if (item instanceof InteractionWired || item instanceof InteractionWiredHighscore) {
                if (!isOwner && item.getUserId() != myUserId) {
                    continue;
                }
                userItemsMap.computeIfAbsent(item.getUserId(), k -> new THashSet<>()).add(item);
            }
        }

        if (userItemsMap.isEmpty()) {
            habbo.whisper(Emulator.getTexts().getValue("commands.errors.cmd_pickwired.empty", "No se encontraron wireds para recoger en esta sala."));
            return true;
        }

        int totalCount = 0;
        for (Map.Entry<Integer, THashSet<HabboItem>> entry : userItemsMap.entrySet()) {
            for (HabboItem item : entry.getValue()) {
                room.pickUpItem(item, null);
                totalCount++;
            }

            Habbo targetUser = Emulator.getGameEnvironment().getHabboManager().getHabbo(entry.getKey());
            if (targetUser != null && targetUser.getInventory() != null && targetUser.getInventory().getItemsComponent() != null) {
                targetUser.getInventory().getItemsComponent().addItems(entry.getValue());
                targetUser.getClient().sendResponse(new AddHabboItemComposer(entry.getValue()));
                targetUser.getClient().sendResponse(new InventoryRefreshComposer());
            }
        }

        habbo.whisper(Emulator.getTexts().getValue("commands.succes.cmd_pickwired", "Se han recogido %count% wireds de la sala a tu inventario.").replace("%count%", String.valueOf(totalCount)));
        return true;
    }
}
