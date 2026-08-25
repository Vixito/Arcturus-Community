package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.events.furniture.FurnitureMovedEvent;
import com.eu.habbo.plugin.events.furniture.FurniturePlacedEvent;
import com.eu.habbo.plugin.events.users.UserExitRoomEvent;

public class SetRotationCommand extends Command implements EventListener {
    public static final String SET_ROTATION_KEY = "setrot.set_rotation";

    public SetRotationCommand() {
        super("cmd_setrotation", Emulator.getTexts().getValue("commands.keys.cmd_setrotation", "setrotation;setrot;rot").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length >= 2) {
            int rotation = -1;
            try {
                rotation = Integer.parseInt(params[1]);
            } catch (Exception e) {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("building.cmd_setrotation.invalid_state", "Por favor ingresa una rotación válida (0, 2, 4, 6 o 0-7)."), RoomChatMessageBubbles.ALERT);
                return true;
            }

            if (rotation > 7 || rotation < 0) {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("building.cmd_setrotation.invalid_state", "Por favor ingresa una rotación válida (0 a 7)."), RoomChatMessageBubbles.ALERT);
                return true;
            }

            gameClient.getHabbo().getHabboStats().cache.put(SET_ROTATION_KEY, rotation);
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("building.cmd_setrotation.changed", "Modo SetRotation activado con rotación: %rotation%.").replace("%rotation%", String.valueOf(rotation)), RoomChatMessageBubbles.ALERT);
        } else {
            if (gameClient.getHabbo().getHabboStats().cache.containsKey(SET_ROTATION_KEY)) {
                gameClient.getHabbo().getHabboStats().cache.remove(SET_ROTATION_KEY);
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("building.cmd_setrotation.disabled", "Modo SetRotation desactivado."), RoomChatMessageBubbles.ALERT);
                return true;
            }
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("building.cmd_setrotation.not_specified", "Uso: :setrotation [0-7] para fijar rotación o :setrotation para desactivar."), RoomChatMessageBubbles.ALERT);
        }
        return true;
    }

    @EventHandler
    public static void onUserExitRoomEvent(UserExitRoomEvent event) {
        if (event.habbo != null) {
            event.habbo.getHabboStats().cache.remove(SET_ROTATION_KEY);
        }
    }

    @EventHandler
    public static void onFurniturePlaced(final FurniturePlacedEvent event) {
        if (event.location != null && event.habbo != null && event.furniture != null && event.habbo.getHabboStats().cache.containsKey(SET_ROTATION_KEY)) {
            final int rotation = (Integer) event.habbo.getHabboStats().cache.get(SET_ROTATION_KEY);
            Emulator.getThreading().run(() -> {
                if (event.habbo.getHabboInfo().getCurrentRoom() != null) {
                    event.furniture.setRotation(rotation);
                    event.furniture.needsUpdate(true);
                    event.habbo.getHabboInfo().getCurrentRoom().updateItem(event.furniture);
                }
            }, 25L);
        }
    }

    @EventHandler
    public static void onFurnitureMoved(final FurnitureMovedEvent event) {
        if (event.newPosition != null && event.habbo != null && event.furniture != null && event.habbo.getHabboStats().cache.containsKey(SET_ROTATION_KEY)) {
            final int rotation = (Integer) event.habbo.getHabboStats().cache.get(SET_ROTATION_KEY);
            Emulator.getThreading().run(() -> {
                if (event.habbo.getHabboInfo().getCurrentRoom() != null) {
                    event.furniture.setRotation(rotation);
                    event.furniture.needsUpdate(true);
                    event.habbo.getHabboInfo().getCurrentRoom().updateItem(event.furniture);
                }
            }, 25L);
        }
    }
}
