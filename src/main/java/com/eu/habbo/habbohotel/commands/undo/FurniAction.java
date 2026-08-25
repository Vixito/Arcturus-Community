package com.eu.habbo.habbohotel.commands.undo;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;

public abstract class FurniAction {
    public HabboItem habboItem;
    public Room room;

    public abstract void undo();
}
