package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredFurniSourceHelper;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import com.eu.habbo.habbohotel.wired.WiredUserSourceHelper;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectSendSignal extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.SEND_SIGNAL;

    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED; // Antenas
    private int forwardFurniSource = WiredFurniSourceHelper.SOURCE_SELECTOR; // Furni a adelantar
    private int forwardUserSource = WiredUserSourceHelper.USER_SOURCE_SELECTOR; // Usuarios para avanzar
    private boolean perFurni = false; // Enviar señal para cada furni
    private boolean perUser = false; // Enviar señal para cada usuario
    private THashSet<HabboItem> items;

    public WiredEffectSendSignal(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectSendSignal(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;

        THashSet<HabboItem> antennas = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        if (antennas.isEmpty()) return false;

        THashSet<HabboItem> forwardedFurnis = new THashSet<>();
        if (this.forwardFurniSource >= 0) {
            forwardedFurnis = WiredFurniSourceHelper.resolveItems(this, this.forwardFurniSource, this.items, room, stuff);
        }

        List<Habbo> forwardedUsers = new ArrayList<>();
        if (this.forwardUserSource >= 0) {
            forwardedUsers = WiredUserSourceHelper.resolveUsers(this, this.forwardUserSource, roomUnit, room, stuff);
        }

        for (HabboItem antenna : antennas) {
            if (antenna == null) continue;

            if (this.perFurni && this.perUser && !forwardedFurnis.isEmpty() && !forwardedUsers.isEmpty()) {
                for (HabboItem f : forwardedFurnis) {
                    for (Habbo u : forwardedUsers) {
                        RoomUnit targetUnit = u != null ? u.getRoomUnit() : roomUnit;
                        WiredHandler.handle(WiredTriggerType.RECEIVE_SIGNAL, targetUnit, room, new Object[]{ antenna, Collections.singletonList(f), Collections.singletonList(u) });
                    }
                }
            } else if (this.perFurni && !forwardedFurnis.isEmpty()) {
                for (HabboItem f : forwardedFurnis) {
                    RoomUnit targetUnit = !forwardedUsers.isEmpty() && forwardedUsers.get(0) != null ? forwardedUsers.get(0).getRoomUnit() : roomUnit;
                    WiredHandler.handle(WiredTriggerType.RECEIVE_SIGNAL, targetUnit, room, new Object[]{ antenna, Collections.singletonList(f), forwardedUsers });
                }
            } else if (this.perUser && !forwardedUsers.isEmpty()) {
                for (Habbo u : forwardedUsers) {
                    RoomUnit targetUnit = u != null ? u.getRoomUnit() : roomUnit;
                    WiredHandler.handle(WiredTriggerType.RECEIVE_SIGNAL, targetUnit, room, new Object[]{ antenna, forwardedFurnis, Collections.singletonList(u) });
                }
            } else {
                RoomUnit targetUnit = !forwardedUsers.isEmpty() && forwardedUsers.get(0) != null ? forwardedUsers.get(0).getRoomUnit() : roomUnit;
                WiredHandler.handle(WiredTriggerType.RECEIVE_SIGNAL, targetUnit, room, new Object[]{ antenna, forwardedFurnis, forwardedUsers });
            }
        }
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.furniSource,
            this.forwardFurniSource,
            this.forwardUserSource,
            this.perFurni,
            this.perUser,
            this.getDelay(),
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.forwardFurniSource = WiredFurniSourceHelper.SOURCE_SELECTOR;
        this.forwardUserSource = WiredUserSourceHelper.USER_SOURCE_SELECTOR;
        this.perFurni = false;
        this.perUser = false;
        this.setDelay(0);

        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.furniSource = WiredFurniSourceHelper.normalizeSourceCode(data.furniSource);
                this.forwardFurniSource = data.forwardFurniSource < 0 ? -1 : WiredFurniSourceHelper.normalizeSourceCode(data.forwardFurniSource);
                this.forwardUserSource = data.forwardUserSource < 0 ? -1 : WiredUserSourceHelper.normalizeUserSourceCode(data.forwardUserSource);
                this.perFurni = data.perFurni;
                this.perUser = data.perUser;
                this.setDelay(data.delay);
                if (data.itemIds != null && room != null) {
                    for (Integer id : data.itemIds) {
                        HabboItem item = room.getHabboItem(id);
                        if (item != null) this.items.add(item);
                    }
                }
            }
        }
    }

    @Override
    public void onPickUp() {
        this.items.clear();
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
        this.forwardFurniSource = WiredFurniSourceHelper.SOURCE_SELECTOR;
        this.forwardUserSource = WiredUserSourceHelper.USER_SOURCE_SELECTOR;
        this.perFurni = false;
        this.perUser = false;
        this.setDelay(0);
    }

    @Override
    public WiredEffectType getType() {
        return type;
    }

    @Override
    public void serializeWiredData(ServerMessage message, Room room) {
        THashSet<HabboItem> toRemove = new THashSet<>();
        for (HabboItem item : this.items) {
            if (item.getRoomId() != this.getRoomId() || room.getHabboItem(item.getId()) == null) {
                toRemove.add(item);
            }
        }
        for (HabboItem item : toRemove) this.items.remove(item);

        message.appendBoolean(false);
        message.appendInt(WiredHandler.MAXIMUM_FURNI_SELECTION);
        message.appendInt(this.items.size());
        for (HabboItem item : this.items) message.appendInt(item.getId());
        message.appendInt(this.getBaseItem().getSpriteId());
        message.appendInt(this.getId());
        message.appendString("");
        message.appendInt(4);
        message.appendInt(this.forwardFurniSource);
        message.appendInt(this.forwardUserSource);
        message.appendInt(this.perFurni ? 1 : 0);
        message.appendInt(this.perUser ? 1 : 0);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        int newFwdFurni = WiredFurniSourceHelper.SOURCE_SELECTOR;
        int newFwdUser = WiredUserSourceHelper.USER_SOURCE_SELECTOR;
        boolean newPerFurni = false;
        boolean newPerUser = false;
        if (intCount >= 1) {
            int raw = packet.readInt();
            newFwdFurni = raw < 0 ? -1 : WiredFurniSourceHelper.normalizeSourceCode(raw);
        }
        if (intCount >= 2) {
            int raw = packet.readInt();
            newFwdUser = raw < 0 ? -1 : WiredUserSourceHelper.normalizeUserSourceCode(raw);
        }
        if (intCount >= 3) newPerFurni = (packet.readInt() == 1);
        if (intCount >= 4) newPerUser = (packet.readInt() == 1);
        for (int i = 4; i < intCount; i++) packet.readInt();

        packet.readString();

        Room room = Emulator.getGameEnvironment().getRoomManager().getRoom(this.getRoomId());
        if (room == null) throw new WiredSaveException("Trying to save wired in unloaded room");

        int itemsCount = packet.readInt();
        if (itemsCount > Emulator.getConfig().getInt("hotel.wired.furni.selection.count")) {
            throw new WiredSaveException("Too many furni selected");
        }

        THashSet<HabboItem> newItems = new THashSet<>();
        for (int i = 0; i < itemsCount; i++) {
            HabboItem it = room.getHabboItem(packet.readInt());
            if (it != null) newItems.add(it);
        }

        int delay = packet.readInt();
        if (delay > Emulator.getConfig().getInt("hotel.wired.max_delay", 20)) {
            throw new WiredSaveException("Delay too long");
        }

        int newSource = WiredFurniSourceHelper.readOptionalSourceCode(packet);
        this.furniSource = newSource;
        this.forwardFurniSource = newFwdFurni;
        this.forwardUserSource = newFwdUser;
        this.perFurni = newPerFurni;
        this.perUser = newPerUser;
        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);
        return true;
    }

    static class JsonData {
        int furniSource;
        int forwardFurniSource;
        int forwardUserSource;
        boolean perFurni;
        boolean perUser;
        int delay;
        List<Integer> itemIds;

        public JsonData(int furniSource, int forwardFurniSource, int forwardUserSource, boolean perFurni, boolean perUser, int delay, List<Integer> itemIds) {
            this.furniSource = furniSource;
            this.forwardFurniSource = forwardFurniSource;
            this.forwardUserSource = forwardUserSource;
            this.perFurni = perFurni;
            this.perUser = perUser;
            this.delay = delay;
            this.itemIds = itemIds;
        }
    }
}
