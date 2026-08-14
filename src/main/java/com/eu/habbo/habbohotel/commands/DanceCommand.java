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
                        UserIdleEvent event = new UserIdleEvent(gameClient.getHabbo(), UserIdleEvent.IdleReason.DANCE, false);
                        Emulator.getPluginManager().fireEvent(event);
                        
                        if (!event.isCancelled() && !event.idle) {
                            gameClient.getHabbo().getHabboInfo().getCurrentRoom().unIdle(gameClient.getHabbo());
                        }
                        
                        gameClient.getHabbo().getHabboInfo().getCurrentRoom().dance(gameClient.getHabbo(), DanceType.values()[danceId]);
                    }
                }
            } catch (Exception e) {
                return false;
            }
        }
        return true;
    }
}
