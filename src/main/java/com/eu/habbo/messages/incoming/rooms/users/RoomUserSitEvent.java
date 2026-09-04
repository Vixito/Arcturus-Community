package com.eu.habbo.messages.incoming.rooms.users;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.plugin.events.users.UserIdleEvent;

public class RoomUserSitEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo().getHabboInfo().getCurrentRoom() != null) {
            if (this.client.getHabbo().getRoomUnit().isWalking()) {
                this.client.getHabbo().getRoomUnit().stopWalking();
            }
            this.client.getHabbo().getHabboInfo().getCurrentRoom().makeSit(this.client.getHabbo());
            WiredHandler.handle(WiredTriggerType.USER_PERFORMS_ACTION, this.client.getHabbo().getRoomUnit(), this.client.getHabbo().getHabboInfo().getCurrentRoom(), new Object[]{ 4 });

            UserIdleEvent event = new UserIdleEvent(this.client.getHabbo(), UserIdleEvent.IdleReason.WALKED, false);
            Emulator.getPluginManager().fireEvent(event);

            if (!event.isCancelled()) {
                if (!event.idle) {
                    this.client.getHabbo().getHabboInfo().getCurrentRoom().unIdle(this.client.getHabbo());
                }
            }
        }
    }
}
