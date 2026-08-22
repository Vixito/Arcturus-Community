package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.rooms.RoomUserAction;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserActionComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTalkComposer;

public class SlapCommand extends Command {
    public SlapCommand() {
        super(null, new String[]{"slap", "bofetada", "cachetada", "abofetear"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length >= 2) {
            Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
            if (room == null) return true;

            Habbo target = room.getHabbo(params[1]);
            if (target == null) {
                gameClient.getHabbo().whisper("Ese usuario no está en la sala.", RoomChatMessageBubbles.ALERT);
                return true;
            }

            if (target == gameClient.getHabbo()) {
                gameClient.getHabbo().whisper("No puedes darte una bofetada a ti mismo.", RoomChatMessageBubbles.ALERT);
                return true;
            }

            if (gameClient.getHabbo().getRoomUnit().getCurrentLocation().distance(target.getRoomUnit().getCurrentLocation()) > 1.5) {
                gameClient.getHabbo().whisper("Estás muy lejos para darle una bofetada a " + target.getHabboInfo().getUsername() + ".", RoomChatMessageBubbles.ALERT);
                return true;
            }

            // Face each other
            gameClient.getHabbo().getRoomUnit().lookAtPoint(target.getRoomUnit().getCurrentLocation());
            target.getRoomUnit().lookAtPoint(gameClient.getHabbo().getRoomUnit().getCurrentLocation());

            // Send Action
            room.sendComposer(new RoomUserActionComposer(gameClient.getHabbo().getRoomUnit(), RoomUserAction.WAVE).compose());
            room.sendComposer(new RoomUserTalkComposer(new RoomChatMessage("*Le da una bofetada a " + target.getHabboInfo().getUsername() + "*", gameClient.getHabbo(), RoomChatMessageBubbles.NORMAL)).compose());
        } else {
            gameClient.getHabbo().whisper("Uso correcto: :slap [usuario] o :bofetada [usuario]", RoomChatMessageBubbles.ALERT);
        }
        return true;
    }
}
