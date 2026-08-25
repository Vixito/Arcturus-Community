package com.eu.habbo.habbohotel.commands.undo;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.events.furniture.FurnitureMovedEvent;
import com.eu.habbo.plugin.events.furniture.FurniturePickedUpEvent;
import com.eu.habbo.plugin.events.furniture.FurniturePlacedEvent;
import com.eu.habbo.plugin.events.furniture.FurnitureRotatedEvent;
import com.eu.habbo.plugin.events.users.UserLoginEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class UndoHandler implements EventListener {
    public static final String LAST_FURNI_ACTIONS = "last.furni.actions.key";

    @EventHandler
    public static void onUserLoginEvent(UserLoginEvent event) {
        if (event.habbo == null) return;
        event.habbo.getHabboStats().cache.put(LAST_FURNI_ACTIONS, new HashMap<Integer, List<FurniAction>>());
    }

    @EventHandler
    public static void onFurnitureRotatedEvent(FurnitureRotatedEvent event) {
        if (event.habbo == null || event.furniture == null) return;
        Room room = event.habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return;

        FurniRotateAction action = new FurniRotateAction(room, event.furniture, event.furniture.getZ(), event.oldRotation);
        addAction(event.habbo.getHabboStats().cache, room.getId(), action);
    }

    @EventHandler
    public static void onFurniturePlacedEvent(FurniturePlacedEvent event) {
        if (event.habbo == null || event.furniture == null) return;
        Room room = event.habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return;

        FurniPlaceAction action = new FurniPlaceAction(room, event.furniture);
        addAction(event.habbo.getHabboStats().cache, room.getId(), action);
    }

    @EventHandler
    public static void onFurnitureMovedEvent(FurnitureMovedEvent event) {
        if (event.habbo == null || event.furniture == null || event.oldPosition == null) return;
        Room room = event.habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return;

        FurniMoveAction action = new FurniMoveAction(room, event.furniture, event.oldPosition.x, event.oldPosition.y, event.furniture.getZ());
        addAction(event.habbo.getHabboStats().cache, room.getId(), action);
    }

    @EventHandler
    public static void onFurniturePickedUpEvent(FurniturePickedUpEvent event) {
        if (event.habbo == null || event.furniture == null) return;
        if (event.habbo.getHabboInfo().getId() != event.furniture.getUserId()) return;
        Room room = event.habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return;

        FurniPickupAction action = new FurniPickupAction(room, event.furniture, event.furniture.getX(), event.furniture.getY(), event.furniture.getZ(), event.furniture.getRotation(), event.furniture.getExtradata());
        addAction(event.habbo.getHabboStats().cache, room.getId(), action);
    }

    @SuppressWarnings("unchecked")
    private static void addAction(gnu.trove.map.hash.THashMap<String, Object> cache, int roomId, FurniAction action) {
        HashMap<Integer, List<FurniAction>> furniActionList = (HashMap<Integer, List<FurniAction>>) cache.get(LAST_FURNI_ACTIONS);
        if (furniActionList == null) {
            furniActionList = new HashMap<>();
            cache.put(LAST_FURNI_ACTIONS, furniActionList);
        }
        List<FurniAction> actions = furniActionList.computeIfAbsent(roomId, k -> new ArrayList<>());
        actions.add(action);
        if (actions.size() > 50) {
            actions.remove(0);
        }
    }
}
