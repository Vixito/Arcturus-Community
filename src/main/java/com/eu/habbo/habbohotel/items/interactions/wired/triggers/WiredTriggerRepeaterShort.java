package com.eu.habbo.habbohotel.items.interactions.wired.triggers;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.items.ICycleable;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredTrigger;
import com.eu.habbo.habbohotel.items.interactions.wired.WiredTriggerReset;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import gnu.trove.procedure.TObjectProcedure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class WiredTriggerRepeaterShort extends InteractionWiredTrigger implements ICycleable, WiredTriggerReset {
    public static final WiredTriggerType type = WiredTriggerType.PERIODICALLY_SHORT;
    public static final int DEFAULT_DELAY_MS = 500;

    protected int repeatTimeMs = DEFAULT_DELAY_MS;
    private volatile long lastTriggerMs = 0L;

    public WiredTriggerRepeaterShort(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
    }

    public WiredTriggerRepeaterShort(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(this.repeatTimeMs));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.repeatTimeMs = DEFAULT_DELAY_MS;
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.repeatTimeMs = clampMs(data.repeatTimeMs);
            }
        } else if (wiredData != null && !wiredData.trim().isEmpty()) {
            try {
                this.repeatTimeMs = clampMs(Integer.parseInt(wiredData.trim()));
            } catch (NumberFormatException ignored) {}
        }
    }

    private static int clampMs(int ms) {
        int clamped = Math.max(50, Math.min(500, ms));
        return ((clamped + 25) / 50) * 50;
    }

    @Override
    public void onPickUp() {
        this.repeatTimeMs = DEFAULT_DELAY_MS;
        this.lastTriggerMs = 0L;
    }

    @Override
    public WiredTriggerType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        message.appendBoolean(false);
        message.appendInt(0);
        message.appendInt(0);
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(1);
        message.appendInt(Math.max(1, Math.min(10, this.repeatTimeMs / 50)));
        message.appendInt(0);
        message.appendInt(this.getType().code);

        if (!this.isTriggeredByRoomUnit()) {
            List<Integer> invalidTriggers = new ArrayList<>();
            room.getRoomSpecialTypes().getEffects(this.getX(), this.getY()).forEach(new TObjectProcedure<InteractionWiredEffect>() {
                @Override
                public boolean execute(InteractionWiredEffect object) {
                    if (object.requiresTriggeringUser()) {
                        invalidTriggers.add(object.getBaseItem().getSpriteId());
                    }
                    return true;
                }
            });
            message.appendInt(invalidTriggers.size());
            for (Integer i : invalidTriggers) message.appendInt(i);
        } else {
            message.appendInt(0);
        }
    }

    @Override
    public boolean saveData(ClientMessage packet) {
        int intCount = packet.readInt();
        int step = intCount >= 1 ? packet.readInt() : 10;
        for (int i = 1; i < intCount; i++) packet.readInt();
        this.repeatTimeMs = clampMs(step * 50);
        this.lastTriggerMs = 0L;
        return true;
    }

    @Override
    public void cycle(Room room) {
        if (this.getRoomId() == 0 || room == null || !room.isLoaded()) return;

        int interval = clampMs(this.repeatTimeMs);
        if (interval >= 500) {
            long now = System.currentTimeMillis();
            if (now - this.lastTriggerMs >= 480) {
                this.lastTriggerMs = now;
                WiredHandler.handle(this, null, room, new Object[]{this});
            }
            return;
        }

        // Sub-500ms pulses within this 500ms room cycle tick
        int pulses = Math.max(1, 500 / interval);
        for (int i = 0; i < pulses; i++) {
            final long delayMs = (long) i * interval;
            if (delayMs == 0) {
                WiredHandler.handle(this, null, room, new Object[]{this});
            } else {
                Emulator.getThreading().run(() -> {
                    if (this.getRoomId() != 0 && room.isLoaded() && room.getHabboItem(this.getId()) != null) {
                        WiredHandler.handle(this, null, room, new Object[]{this});
                    }
                }, delayMs);
            }
        }
    }

    @Override
    public void resetTimer() {
        this.lastTriggerMs = 0L;
        if (this.getRoomId() != 0) {
            Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
            if (room != null && room.isLoaded()) {
                WiredHandler.handle(this, null, room, new Object[]{this});
            }
        }
    }

    static class JsonData {
        int repeatTimeMs;

        public JsonData(int repeatTimeMs) {
            this.repeatTimeMs = repeatTimeMs;
        }
    }
}
