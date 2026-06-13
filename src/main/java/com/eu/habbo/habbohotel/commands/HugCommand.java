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

public class HugCommand extends Command {
    public HugCommand() {
        super("cmd_sit", new String[]{"hug", "abrazo"});
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
                gameClient.getHabbo().whisper("No puedes abrazarte a ti mismo.", RoomChatMessageBubbles.ALERT);
                return true;
            }

            if (gameClient.getHabbo().getRoomUnit().getCurrentLocation().distance(target.getRoomUnit().getCurrentLocation()) > 1.5) {
                gameClient.getHabbo().whisper("Estás muy lejos para abrazar a " + target.getHabboInfo().getUsername() + ".", RoomChatMessageBubbles.ALERT);
                return true;
            }

            // Face each other
            gameClient.getHabbo().getRoomUnit().lookAtPoint(target.getRoomUnit().getCurrentLocation());
            target.getRoomUnit().lookAtPoint(gameClient.getHabbo().getRoomUnit().getCurrentLocation());

            // Increment hugs in DB
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE users SET hugs_received = hugs_received + 1 WHERE id = ?")) {
                statement.setInt(1, target.getHabboInfo().getId());
                statement.execute();
            } catch (SQLException e) {
                Emulator.getLogging().logSQLException(e);
            }

            // Send Action
            room.sendComposer(new RoomUserActionComposer(gameClient.getHabbo().getRoomUnit(), RoomUserAction.NONE).compose());
            
            // Send Chat Bubble Action
            room.sendComposer(new RoomUserTalkComposer(new RoomChatMessage("*Abraza a " + target.getHabboInfo().getUsername() + "*", gameClient.getHabbo(), RoomChatMessageBubbles.NORMAL)).compose());
            
        } else {
            gameClient.getHabbo().whisper("Uso correcto: :abrazo [usuario]", RoomChatMessageBubbles.ALERT);
        }
        return true;
    }
}
