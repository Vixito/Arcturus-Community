package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import gnu.trove.map.hash.THashMap;

import java.util.Map;

public class FurniCommand extends Command {
    public FurniCommand() {
        super(null, new String[]{"furni"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null) return false;

        THashMap<String, Integer> furniCounts = new THashMap<>();
        int totalItems = 0;

        for (HabboItem item : room.getFloorItems()) {
            if (item != null && item.getBaseItem() != null) {
                String name = item.getBaseItem().getName();
                furniCounts.put(name, furniCounts.getOrDefault(name, 0) + 1);
                totalItems++;
            }
        }

        for (HabboItem item : room.getWallItems()) {
            if (item != null && item.getBaseItem() != null) {
                String name = item.getBaseItem().getName();
                furniCounts.put(name, furniCounts.getOrDefault(name, 0) + 1);
                totalItems++;
            }
        }

        StringBuilder message = new StringBuilder();
        message.append("<b>Furnis en la sala (").append(totalItems).append(" total):</b>\r\r");

        int count = 0;
        for (Map.Entry<String, Integer> entry : furniCounts.entrySet()) {
            message.append("• ").append(entry.getKey()).append(" (x").append(entry.getValue()).append(")\r");
            count++;
            if (count >= 50) {
                message.append("... y más furnis.");
                break;
            }
        }

        gameClient.getHabbo().alert(message.toString());
        return true;
    }
}
