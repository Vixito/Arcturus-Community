package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.commands.undo.FurniAction;
import com.eu.habbo.habbohotel.commands.undo.UndoHandler;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;

import java.util.HashMap;
import java.util.List;

public class UndoCommand extends Command {
    public UndoCommand() {
        super("cmd_undo", Emulator.getTexts().getValue("commands.keys.cmd_undo", "undo").split(";"));
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Habbo habbo = gameClient.getHabbo();
        if (habbo == null) return false;
        Room room = habbo.getHabboInfo().getCurrentRoom();
        if (room == null) return false;

        HashMap<Integer, List<FurniAction>> furniActionList = (HashMap<Integer, List<FurniAction>>) habbo.getHabboStats().cache.get(UndoHandler.LAST_FURNI_ACTIONS);
        if (furniActionList == null || !furniActionList.containsKey(room.getId())) {
            habbo.whisper(Emulator.getTexts().getValue("undo.cmd_undo.error", "No tienes acciones recientes para deshacer en esta sala."), RoomChatMessageBubbles.ALERT);
            return true;
        }

        List<FurniAction> furniActions = furniActionList.get(room.getId());
        if (furniActions == null || furniActions.isEmpty()) {
            habbo.whisper(Emulator.getTexts().getValue("undo.cmd_undo.error", "No tienes acciones recientes para deshacer en esta sala."), RoomChatMessageBubbles.ALERT);
            return true;
        }

        FurniAction furniAction = furniActions.remove(furniActions.size() - 1);
        furniAction.undo();
        habbo.whisper(Emulator.getTexts().getValue("undo.cmd_undo.success", "Acción deshecha correctamente."), RoomChatMessageBubbles.ALERT);
        return true;
    }
}
