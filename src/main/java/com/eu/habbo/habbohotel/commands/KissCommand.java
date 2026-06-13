package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.rooms.RoomUserAction;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserActionComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTalkComposer;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class KissCommand extends Command {
    public KissCommand() {
        super("cmd_sit", new String[]{"kiss", "beso"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length == 2) {
            Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
            if (room == null) return true;

            Habbo target = room.getHabbo(params[1]);
            if (target == null) {
                gameClient.getHabbo().whisper("Ese usuario no está en la sala.", RoomChatMessageBubbles.ALERT);
                return true;
            }

            if (target == gameClient.getHabbo()) {
                gameClient.getHabbo().whisper("No puedes besarte a ti mismo.", RoomChatMessageBubbles.ALERT);
                return true;
            }

            if (gameClient.getHabbo().getRoomUnit().getCurrentLocation().distance(target.getRoomUnit().getCurrentLocation()) > 1.5) {
                gameClient.getHabbo().whisper("Estás muy lejos para besar a " + target.getHabboInfo().getUsername() + ".", RoomChatMessageBubbles.ALERT);
                return true;
            }

            // Face each other
            gameClient.getHabbo().getRoomUnit().lookAtPoint(target.getRoomUnit().getCurrentLocation());
            target.getRoomUnit().lookAtPoint(gameClient.getHabbo().getRoomUnit().getCurrentLocation());

            // Increment kisses in DB
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE users SET kisses_received = kisses_received + 1 WHERE id = ?")) {
                statement.setInt(1, target.getHabboInfo().getId());
                statement.execute();
            } catch (SQLException e) {
                Emulator.getLogging().logSQLException(e);
            }

            // Send Action
            room.sendComposer(new RoomUserActionComposer(gameClient.getHabbo().getRoomUnit(), RoomUserAction.BLOW_KISS).compose());
            
            // Send Chat Bubble Action
            room.sendComposer(new RoomUserTalkComposer(new RoomChatMessage("*Da un beso a " + target.getHabboInfo().getUsername() + "*", gameClient.getHabbo(), RoomChatMessageBubbles.NORMAL)).compose());
            
            // Optional: target also blows kiss or has effect
            // room.sendComposer(new RoomUserActionComposer(target.getRoomUnit(), RoomUserAction.BLOW_KISS).compose());
            
        } else {
            gameClient.getHabbo().whisper("Uso correcto: :beso [usuario]", RoomChatMessageBubbles.ALERT);
        }
        return true;
    }
}
