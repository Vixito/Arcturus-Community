package com.eu.habbo.habbohotel.commands.undo;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.items.FloorItemUpdateComposer;
import gnu.trove.set.hash.THashSet;

public class FurniRotateAction extends FurniAction {
    public double z;
    public int rotation;

    public FurniRotateAction(Room room, HabboItem habboItem, double z, int rotation) {
        this.room = room;
        this.habboItem = habboItem;
        this.z = z;
        this.rotation = rotation;
    }

    @Override
    public void undo() {
        if (this.habboItem == null || this.room == null) return;
        RoomLayout layout = this.room.getLayout();
        if (layout == null) return;

        RoomTile prevTile = layout.getTile(this.habboItem.getX(), this.habboItem.getY());
        if (prevTile == null) return;
        if (this.habboItem.getRoomId() != this.room.getId()) return;

        THashSet<RoomTile> occupiedTiles = layout.getTilesAt(prevTile, this.habboItem.getBaseItem().getWidth(), this.habboItem.getBaseItem().getLength(), this.habboItem.getRotation());
        occupiedTiles.addAll(layout.getTilesAt(prevTile, this.habboItem.getBaseItem().getWidth(), this.habboItem.getBaseItem().getLength(), this.rotation));
        if (layout.fitsOnMap(prevTile, this.habboItem.getBaseItem().getWidth(), this.habboItem.getBaseItem().getLength(), this.rotation)) {
            this.habboItem.setZ(this.z);
            this.habboItem.setRotation(this.rotation);
            this.habboItem.needsUpdate(true);
            Emulator.getThreading().run(this.habboItem);
            this.room.sendComposer(new FloorItemUpdateComposer(this.habboItem).compose());
            this.room.updateTiles(occupiedTiles);
            for (RoomTile t : occupiedTiles) {
                this.room.updateHabbosAt(t.x, t.y);
                this.room.updateBotsAt(t.x, t.y);
            }
        }
    }
}
