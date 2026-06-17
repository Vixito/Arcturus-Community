package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.users.DanceType;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserDanceComposer;
import com.eu.habbo.plugin.events.users.UserIdleEvent;

public class DanceCommand extends Command {
    public DanceCommand() {
        super("cmd_dance", Emulator.getTexts().getValue("commands.keys.cmd_dance").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length == 2) {
            try {
                int danceId = Integer.parseInt(params[1]);
                if (danceId >= 0 && danceId <= 4) {
                    if (gameClient.getHabbo().getRoomUnit().isInRoom()) {
                        
                        // Check HC for dances 2, 3, 4
                        if (danceId > 1 && !gameClient.getHabbo().getHabboStats().hasActiveClub()) {
                            gameClient.getHabbo().whisper("Solo los miembros del Habbo Club pueden usar este baile.", com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles.ALERT);
                            return true; // do nothing if not HC
                        }
                        
                        gameClient.getHabbo().getRoomUnit().setDanceType(DanceType.values()[danceId]);
                        
                        UserIdleEvent event = new UserIdleEvent(gameClient.getHabbo(), UserIdleEvent.IdleReason.DANCE, false);
                        Emulator.getPluginManager().fireEvent(event);
                        
                        if (!event.isCancelled() && !event.idle) {
                            gameClient.getHabbo().getHabboInfo().getCurrentRoom().unIdle(gameClient.getHabbo());
                        }
                        
                        gameClient.getHabbo().getHabboInfo().getCurrentRoom().sendComposer(new RoomUserDanceComposer(gameClient.getHabbo().getRoomUnit()).compose());
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
}
