package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessage;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomUserAction;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserActionComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTalkComposer;

public class ClapCommand extends Command {
    public ClapCommand() {
        super(null, new String[]{"clap", "aplaudir"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null) return true;

        room.sendComposer(new RoomUserActionComposer(gameClient.getHabbo().getRoomUnit(), RoomUserAction.WAVE).compose());
        room.sendComposer(new RoomUserTalkComposer(new RoomChatMessage("*Aplausos*", gameClient.getHabbo(), RoomChatMessageBubbles.NORMAL)).compose());
        return true;
    }
}
