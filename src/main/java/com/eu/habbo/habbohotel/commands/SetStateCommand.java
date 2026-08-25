package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.events.furniture.FurnitureMovedEvent;
import com.eu.habbo.plugin.events.furniture.FurniturePlacedEvent;

public class SetStateCommand extends Command implements EventListener {
    public static final String SETSTATE_KEY = "setstate.cmd_setstate";

    public SetStateCommand() {
        super("cmd_setstate", Emulator.getTexts().getValue("commands.keys.cmd_setstate", "setstate;state").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length >= 2) {
            int state = 0;
            try {
                state = Integer.parseInt(params[1]);
            } catch (Exception e) {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("setstate.cmd_setstate.invalid", "Por favor ingresa un número de estado válido (0-100)."), RoomChatMessageBubbles.ALERT);
                return true;
            }

            if (state > 100 || state < 0) {
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("setstate.cmd_setstate.invalid", "El estado debe estar entre 0 y 100."), RoomChatMessageBubbles.ALERT);
                return true;
            }

            gameClient.getHabbo().getHabboStats().cache.put(SETSTATE_KEY, state);
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("setstate.cmd_setstate.changed", "Modo SetState activado con el estado: %state%.").replace("%state%", String.valueOf(state)), RoomChatMessageBubbles.ALERT);
        } else {
            if (gameClient.getHabbo().getHabboStats().cache.containsKey(SETSTATE_KEY)) {
                gameClient.getHabbo().getHabboStats().cache.remove(SETSTATE_KEY);
                gameClient.getHabbo().whisper(Emulator.getTexts().getValue("setstate.cmd_setstate.disabled", "Modo SetState desactivado."), RoomChatMessageBubbles.ALERT);
                return true;
            }
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("setstate.cmd_setstate.not_specified", "Uso: :setstate [0-100] para fijar estado o :setstate para desactivar."), RoomChatMessageBubbles.ALERT);
        }
        return true;
    }

    @EventHandler
    public static void onFurniturePlaced(FurniturePlacedEvent event) {
        if (event.location != null && event.habbo != null && event.furniture != null && event.habbo.getHabboStats().cache.containsKey(SETSTATE_KEY)) {
            int state = (Integer) event.habbo.getHabboStats().cache.get(SETSTATE_KEY);
            if (event.furniture.getBaseItem() != null && state <= Math.max(1, event.furniture.getBaseItem().getStateCount() - 1)) {
                event.furniture.setExtradata(String.valueOf(state));
            }
        }
    }

    @EventHandler
    public static void onFurnitureMoved(FurnitureMovedEvent event) {
        if (event.newPosition != null && event.habbo != null && event.furniture != null && event.habbo.getHabboStats().cache.containsKey(SETSTATE_KEY)) {
            int state = (Integer) event.habbo.getHabboStats().cache.get(SETSTATE_KEY);
            if (event.furniture.getBaseItem() != null && state <= Math.max(1, event.furniture.getBaseItem().getStateCount() - 1)) {
                event.furniture.setExtradata(String.valueOf(state));
            }
        }
    }
}
