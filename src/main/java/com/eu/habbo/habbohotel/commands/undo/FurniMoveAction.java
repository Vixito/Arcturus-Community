package com.eu.habbo.habbohotel.commands.undo;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemUpdateComposer;
import gnu.trove.set.hash.THashSet;

public class FurniMoveAction extends FurniAction {
    public int x;
    public int y;
    public double z;

    public FurniMoveAction(Room room, HabboItem habboItem, int x, int y, double z) {
        this.room = room;
        this.habboItem = habboItem;
        this.x = x;
        this.y = y;
        this.z = z;
    }

    @Override
    public void undo() {
        if (this.habboItem == null || this.room == null) return;
        RoomLayout layout = this.room.getLayout();
        if (layout == null) return;

        RoomTile prevTile = layout.getTile(this.habboItem.getX(), this.habboItem.getY());
        RoomTile newTile = layout.getTile((short) this.x, (short) this.y);
        if (newTile == null || prevTile == null) return;
        if (this.habboItem.getRoomId() != this.room.getId()) return;

        THashSet<RoomTile> occupiedTiles = layout.getTilesAt(prevTile, this.habboItem.getBaseItem().getWidth(), this.habboItem.getBaseItem().getLength(), this.habboItem.getRotation());
        if (layout.fitsOnMap(layout.getTile((short) this.x, (short) this.y), this.habboItem.getBaseItem().getWidth(), this.habboItem.getBaseItem().getLength(), this.habboItem.getRotation())) {
            this.habboItem.setZ(this.z);
            this.habboItem.setX((short) this.x);
            this.habboItem.setY((short) this.y);
            this.habboItem.onMove(this.room, prevTile, newTile);
            this.habboItem.needsUpdate(true);
            Emulator.getThreading().run(this.habboItem);
            this.room.sendComposer(new FloorItemUpdateComposer(this.habboItem).compose());
            occupiedTiles.addAll(layout.getTilesAt(newTile, this.habboItem.getBaseItem().getWidth(), this.habboItem.getBaseItem().getLength(), this.habboItem.getRotation()));
            this.room.updateTiles(occupiedTiles);
            for (RoomTile t : occupiedTiles) {
                this.room.updateHabbosAt(t.x, t.y);
                this.room.updateBotsAt(t.x, t.y);
            }
        }
    }
}
