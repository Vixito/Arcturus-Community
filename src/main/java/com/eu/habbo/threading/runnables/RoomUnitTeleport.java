package com.eu.habbo.threading.runnables;

import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUnitOnRollerComposer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.LinkedList;

public class RoomUnitTeleport implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomUnitTeleport.class);

    private RoomUnit roomUnit;
    private Room room;
    private int x;
    private int y;
    private double z;

    private int newEffect;
    private int walkMode = 2; // 0 = continue if near goal, 1 = continue walking, 2 = stop walking
    private RoomTile resumeGoal = null;

    public RoomUnitTeleport(RoomUnit roomUnit, Room room, int x, int y, double z, int newEffect) {
        this(roomUnit, room, x, y, z, newEffect, 2, null);
    }

    public RoomUnitTeleport(RoomUnit roomUnit, Room room, int x, int y, double z, int newEffect, int walkMode, RoomTile resumeGoal) {
        this.roomUnit = roomUnit;
        this.room = room;
        this.x = x;
        this.y = y;
        this.z = z;
        this.newEffect = newEffect;
        this.walkMode = walkMode;
        this.resumeGoal = resumeGoal;
    }

    @Override
    public void run() {
        if (roomUnit == null || roomUnit.getRoom() == null || room.getLayout() == null || roomUnit.isLeavingTeleporter)
            return;

        RoomTile lastLocation = this.roomUnit.getCurrentLocation();
        RoomTile newLocation = this.room.getLayout().getTile((short) this.x, (short) this.y);
        RoomTile goalToResume = this.resumeGoal != null ? this.resumeGoal : this.roomUnit.getGoal();
        boolean wasWalking = (this.resumeGoal != null) || this.roomUnit.isWalking();

        HabboItem topItem = this.room.getTopItemAt(this.roomUnit.getCurrentLocation().x, this.roomUnit.getCurrentLocation().y);
        if (topItem != null) {
            try {
                topItem.onWalkOff(this.roomUnit, this.room, new Object[]{this});
            } catch (Exception e) {
                LOGGER.error("Caught exception", e);
            }
        }
        this.roomUnit.setPath(new LinkedList<>());
        this.roomUnit.setCurrentLocation(newLocation);
        this.roomUnit.setPreviousLocation(newLocation);
        this.roomUnit.setZ(this.z);
        this.roomUnit.setPreviousLocationZ(this.z);
        this.roomUnit.removeStatus(RoomUnitStatus.MOVE);
        this.roomUnit.setLocation(newLocation);
        this.roomUnit.statusUpdate(true);
        roomUnit.isWiredTeleporting = false;

        this.room.updateHabbosAt(newLocation.x, newLocation.y);
        this.room.updateBotsAt(newLocation.x, newLocation.y);

        topItem = room.getTopItemAt(x, y);
        if (topItem != null && roomUnit.getCurrentLocation().equals(room.getLayout().getTile((short) x, (short) y))) {
            try {
                topItem.onWalkOn(roomUnit, room, new Object[]{ lastLocation, newLocation, this });
            } catch (Exception e) {
            }
        }

        if (wasWalking && goalToResume != null && newLocation != null && !goalToResume.equals(newLocation)) {
            boolean shouldResume = false;
            if (this.walkMode == 1) {
                shouldResume = true;
            } else if (this.walkMode == 0) {
                int dist = Math.max(Math.abs(newLocation.x - goalToResume.x), Math.abs(newLocation.y - goalToResume.y));
                shouldResume = (dist <= 3);
            }
            if (shouldResume && this.roomUnit.canWalk()) {
                this.roomUnit.setGoalLocation(goalToResume);
            }
        }
    }
}
