package com.eu.habbo.habbohotel.commands.undo;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomLayout;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.inventory.RemoveHabboItemComposer;
import com.eu.habbo.messages.outgoing.rooms.items.AddFloorItemComposer;
import gnu.trove.set.hash.THashSet;

public class FurniPickupAction extends FurniAction {
    public int x;
    public int y;
    public double z;
    public int rotation;
    public String extraData;

    public FurniPickupAction(Room room, HabboItem habboItem, int x, int y, double z, int rotation, String extraData) {
        this.room = room;
        this.habboItem = habboItem;
        this.x = x;
        this.y = y;
        this.z = z;
        this.rotation = rotation;
        this.extraData = extraData;
    }

    @Override
    public void undo() {
        if (this.habboItem == null || this.room == null) return;
        RoomLayout layout = this.room.getLayout();
        if (layout == null) return;

        RoomTile newTile = layout.getTile((short) this.x, (short) this.y);
        if (newTile == null) return;
        if (this.habboItem.getRoomId() != this.room.getId() && this.habboItem.getRoomId() != 0) return;
        if (this.room.getOwnerId() != this.habboItem.getUserId()) return;

        Habbo habbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(this.habboItem.getUserId());
        if (habbo == null) return;

        if (layout.fitsOnMap(layout.getTile((short) this.x, (short) this.y), this.habboItem.getBaseItem().getWidth(), this.habboItem.getBaseItem().getLength(), this.habboItem.getRotation())) {
            this.habboItem.setZ(this.z);
            this.habboItem.setX((short) this.x);
            this.habboItem.setY((short) this.y);
            this.habboItem.setRotation(this.rotation);
            this.habboItem.setExtradata(this.extraData);
            this.room.addHabboItem(this.habboItem);
            this.habboItem.needsUpdate(true);
            this.habboItem.setRoomId(this.room.getId());
            this.habboItem.onPlace(this.room);
            THashSet<RoomTile> occupiedTiles = layout.getTilesAt(newTile, this.habboItem.getBaseItem().getWidth(), this.habboItem.getBaseItem().getLength(), this.habboItem.getRotation());
            this.room.updateTiles(occupiedTiles);
            this.room.sendComposer(new AddFloorItemComposer(this.habboItem, this.room.getFurniOwnerName(this.habboItem.getUserId())).compose());
            for (RoomTile t : occupiedTiles) {
                this.room.updateHabbosAt(t.x, t.y);
                this.room.updateBotsAt(t.x, t.y);
            }
            Emulator.getThreading().run(this.habboItem);
            habbo.getClient().sendResponse(new RemoveHabboItemComposer(this.habboItem.getGiftAdjustedId()));
            habbo.getClient().getHabbo().getInventory().getItemsComponent().removeHabboItem(this.habboItem.getId());
            this.habboItem.setFromGift(false);
        }
    }
}
