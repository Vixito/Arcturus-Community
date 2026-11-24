package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.generic.alerts.BubbleAlertComposer;
import gnu.trove.map.hash.THashMap;

import java.util.Map;

public class EventCommand extends Command {
    public EventCommand() {
        super("cmd_event", Emulator.getTexts().getValue("commands.keys.cmd_event").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() == null) {
            gameClient.getHabbo().whisper("Debes estar dentro de la sala del evento para usar este comando.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (params.length < 2) {
            gameClient.getHabbo().whisper("Uso correcto: :event <descripción del evento>", RoomChatMessageBubbles.ALERT);
            return true;
        }

        StringBuilder message = new StringBuilder();
        for (int i = 1; i < params.length; i++) {
            message.append(params[i]);
            message.append(" ");
        }

        THashMap<String, String> codes = new THashMap<>();
        codes.put("ROOMNAME", gameClient.getHabbo().getHabboInfo().getCurrentRoom().getName());
        codes.put("ROOMID", gameClient.getHabbo().getHabboInfo().getCurrentRoom().getId() + "");
        codes.put("USERNAME", gameClient.getHabbo().getHabboInfo().getUsername());
        codes.put("LOOK", gameClient.getHabbo().getHabboInfo().getLook());
        codes.put("TIME", Emulator.getDate().toString());
        codes.put("MESSAGE", message.toString().trim());

        final int roomId = gameClient.getHabbo().getHabboInfo().getCurrentRoom().getId();
        final String roomName = gameClient.getHabbo().getHabboInfo().getCurrentRoom().getName();
        final String username = gameClient.getHabbo().getHabboInfo().getUsername();
        final String eventMessage = message.toString().trim();

        ServerMessage msg = new BubbleAlertComposer("hotel.event", codes).compose();

        for (Map.Entry<Integer, Habbo> set : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().entrySet()) {
            Habbo habbo = set.getValue();
            if (habbo.getHabboStats().blockStaffAlerts)
                continue;

            habbo.getClient().sendResponse(msg);
        }

        // Copia del evento para el puente de Discord. Se hace fuera del hilo
        // del juego para no bloquear el emulador con una escritura en MySQL.
        Emulator.getThreading().run(() -> {
            try (java.sql.Connection connection =
                         Emulator.getDatabase().getDataSource().getConnection();
                 java.sql.PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO cms_discord_event_queue " +
                                 "(room_id, room_name, username, message, created_at) " +
                                 "VALUES (?, ?, ?, ?, ?)")) {
                statement.setInt(1, roomId);
                statement.setString(2, roomName);
                statement.setString(3, username);
                statement.setString(4, eventMessage);
                statement.setInt(5, (int) (System.currentTimeMillis() / 1000L));
                statement.executeUpdate();
            } catch (Exception exception) {
                Emulator.getLogging().logErrorLine(exception);
            }
        });

        return true;
    }
}
