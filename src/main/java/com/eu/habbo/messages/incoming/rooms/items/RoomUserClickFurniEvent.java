package com.eu.habbo.messages.incoming.rooms.items;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.incoming.MessageHandler;

public class RoomUserClickFurniEvent extends MessageHandler {
    @Override
    public int getRatelimit() {
        return 200;
    }

    @Override
    public void handle() throws Exception {
        if (this.client.getHabbo() == null || this.client.getHabbo().getHabboInfo() == null) {
            return;
        }

        Room room = this.client.getHabbo().getHabboInfo().getCurrentRoom();
        if (room == null) {
            return;
        }

        RoomUnit roomUnit = this.client.getHabbo().getRoomUnit();
        if (roomUnit == null) {
            return;
        }

        int itemId = this.packet.readInt();
        HabboItem item = room.getHabboItem(itemId);
        if (item == null) {
            return;
        }

        WiredHandler.handle(WiredTriggerType.CLICK_FURNI, roomUnit, room, new Object[]{ item });
    }
}
