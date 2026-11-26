package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.items.interactions.games.InteractionGameTimer;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredFurniSourceHelper;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectMatchCounter extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.MATCH_COUNTER;

    private boolean useGroupMembers = false;
    private String teamAName = "";
    private int teamAColor = 0; // 0=Red, 1=Green, 2=Blue, 3=Yellow
    private String teamBName = "";
    private int teamBColor = 1;
    private int durationSeconds = 60;
    private int onFinishMode = 0; // 0=Nothing, 1=Hide, 2=HideAfter
    private int hideAfterSeconds = 0;
    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    private THashSet<HabboItem> items;

    public WiredEffectMatchCounter(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectMatchCounter(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;

        THashSet<HabboItem> targetItems = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        if (targetItems.isEmpty()) {
            for (HabboItem floorItem : room.getFloorItems()) {
                if (floorItem instanceof InteractionGameTimer) {
                    targetItems.add(floorItem);
                }
            }
        }

        boolean started = false;
        for (HabboItem item : targetItems) {
            if (item instanceof InteractionGameTimer) {
                InteractionGameTimer timer = (InteractionGameTimer) item;
                timer.setTimeNow(this.durationSeconds);
                timer.controlStart(room);
                timer.setTimeNow(this.durationSeconds);
                room.updateItem(timer);
                started = true;
            }
        }
        return started || true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.useGroupMembers,
            this.teamAName,
            this.teamAColor,
            this.teamBName,
            this.teamBColor,
            this.durationSeconds,
            this.onFinishMode,
            this.hideAfterSeconds,
            this.furniSource,
            this.getDelay(),
            this.items.stream().map(HabboItem::getId).collect(Collectors.toList())
        ));
    }

    @Override
    public void loadWiredData(ResultSet set, Room room) throws SQLException {
        this.items = new THashSet<>();
        String wiredData = set.getString("wired_data");
        if (wiredData != null && wiredData.startsWith("{")) {
            JsonData data = WiredHandler.getGsonBuilder().create().fromJson(wiredData, JsonData.class);
            if (data != null) {
                this.useGroupMembers = data.useGroupMembers;
                this.teamAName = data.teamAName != null ? data.teamAName : "";
                this.teamAColor = Math.max(0, Math.min(3, data.teamAColor));
                this.teamBName = data.teamBName != null ? data.teamBName : "";
                this.teamBColor = Math.max(0, Math.min(3, data.teamBColor));
                this.durationSeconds = Math.max(1, Math.min(600, data.durationSeconds));
                this.onFinishMode = Math.max(0, Math.min(2, data.onFinishMode));
                this.hideAfterSeconds = Math.max(0, Math.min(600, data.hideAfterSeconds));
                this.furniSource = WiredFurniSourceHelper.normalizeSourceCode(data.furniSource);
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
        this.useGroupMembers = false;
        this.teamAName = "";
        this.teamAColor = 0;
        this.teamBName = "";
        this.teamBColor = 1;
        this.durationSeconds = 60;
        this.onFinishMode = 0;
        this.hideAfterSeconds = 0;
        this.furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
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
        message.appendString(this.teamAName + "\t" + this.teamBName);
        message.appendInt(6);
        message.appendInt(this.useGroupMembers ? 1 : 0);
        message.appendInt(this.teamAColor);
        message.appendInt(this.teamBColor);
        message.appendInt(this.durationSeconds);
        message.appendInt(this.onFinishMode);
        message.appendInt(this.hideAfterSeconds);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        boolean newUseGroup = intCount >= 1 && packet.readInt() == 1;
        int newTeamAColor = intCount >= 2 ? Math.max(0, Math.min(3, packet.readInt())) : 0;
        int newTeamBColor = intCount >= 3 ? Math.max(0, Math.min(3, packet.readInt())) : 1;
        int newDuration = intCount >= 4 ? Math.max(1, Math.min(600, packet.readInt())) : 60;
        int newOnFinish = intCount >= 5 ? Math.max(0, Math.min(2, packet.readInt())) : 0;
        int newHideAfter = intCount >= 6 ? Math.max(0, Math.min(600, packet.readInt())) : 0;
        for (int i = 6; i < intCount; i++) packet.readInt();

        String str = packet.readString();
        String[] parts = str != null ? str.split("\t", -1) : new String[]{"", ""};
        String newTeamAName = parts.length > 0 ? parts[0].substring(0, Math.min(40, parts[0].length())) : "";
        String newTeamBName = parts.length > 1 ? parts[1].substring(0, Math.min(40, parts[1].length())) : "";

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

        this.useGroupMembers = newUseGroup;
        this.teamAName = newTeamAName;
        this.teamAColor = newTeamAColor;
        this.teamBName = newTeamBName;
        this.teamBColor = newTeamBColor;
        this.durationSeconds = newDuration;
        this.onFinishMode = newOnFinish;
        this.hideAfterSeconds = newHideAfter;
        this.furniSource = newSource;
        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);
        return true;
    }

    static class JsonData {
        boolean useGroupMembers;
        String teamAName;
        int teamAColor;
        String teamBName;
        int teamBColor;
        int durationSeconds;
        int onFinishMode;
        int hideAfterSeconds;
        int furniSource;
        int delay;
        List<Integer> itemIds;

        public JsonData(boolean useGroupMembers, String teamAName, int teamAColor, String teamBName, int teamBColor, int durationSeconds, int onFinishMode, int hideAfterSeconds, int furniSource, int delay, List<Integer> itemIds) {
            this.useGroupMembers = useGroupMembers;
            this.teamAName = teamAName;
            this.teamAColor = teamAColor;
            this.teamBName = teamBName;
            this.teamBColor = teamBColor;
            this.durationSeconds = durationSeconds;
            this.onFinishMode = onFinishMode;
            this.hideAfterSeconds = hideAfterSeconds;
            this.furniSource = furniSource;
            this.delay = delay;
            this.itemIds = itemIds;
        }
    }
}
