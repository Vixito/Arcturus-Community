package com.eu.habbo.habbohotel.items.interactions.wired.effects;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredEffect;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredEffectType;
import com.eu.habbo.habbohotel.wired.WiredFurniSourceHelper;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredUserSourceHelper;
import com.eu.habbo.messages.ClientMessage;
import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.incoming.wired.WiredSaveException;
import gnu.trove.set.hash.THashSet;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.stream.Collectors;

public class WiredEffectSetHighlight extends InteractionWiredEffect {
    public static final WiredEffectType type = WiredEffectType.SET_HIGHLIGHT;

    private int whoSees = 0;        // 0 = Usuarios seleccionados, 1 = Todos los usuarios
    private int highlightType = 0;  // 0 = Contorno, 1 = Relleno, 2 = Brillo
    private int intensity = 0;      // 0 = Desactivado, 1 = Baja, 2 = Media, 3 = Alta
    private int colorIndex = 0;     // 0..20 palette swatch
    private int animation = 0;      // 0 = Constante, 1 = Parpadeo, 2 = Pulso
    private int easing = 0;         // 0 = Instantáneo, 1 = Suave
    private int targetType = 0;     // 0 = Furni, 1 = Usuario
    private int userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
    private int furniSource = WiredFurniSourceHelper.SOURCE_SELECTED;
    private THashSet<HabboItem> items;

    public WiredEffectSetHighlight(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem);
        this.items = new THashSet<>();
    }

    public WiredEffectSetHighlight(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells);
        this.items = new THashSet<>();
    }

    @Override
    public boolean execute(RoomUnit roomUnit, Room room, Object[] stuff) {
        if (room == null) return false;
        THashSet<HabboItem> targets = WiredFurniSourceHelper.resolveItems(this, this.furniSource, this.items, room, stuff);
        for (HabboItem item : targets) {
            if (item != null && item.getRoomId() == room.getId()) {
                room.updateItem(item);
            }
        }
        return true;
    }

    @Override
    public String getWiredData() {
        return WiredHandler.getGsonBuilder().create().toJson(new JsonData(
            this.whoSees,
            this.highlightType,
            this.intensity,
            this.colorIndex,
            this.animation,
            this.easing,
            this.targetType,
            this.userSource,
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
                this.whoSees = Math.max(0, Math.min(1, data.whoSees));
                this.highlightType = Math.max(0, Math.min(2, data.highlightType));
                this.intensity = Math.max(0, Math.min(3, data.intensity));
                this.colorIndex = Math.max(0, Math.min(20, data.colorIndex));
                this.animation = Math.max(0, Math.min(2, data.animation));
                this.easing = Math.max(0, Math.min(1, data.easing));
                this.targetType = Math.max(0, Math.min(1, data.targetType));
                this.userSource = WiredUserSourceHelper.normalizeUserSourceCode(data.userSource);
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
        this.whoSees = 0;
        this.highlightType = 0;
        this.intensity = 0;
        this.colorIndex = 0;
        this.animation = 0;
        this.easing = 0;
        this.targetType = 0;
        this.userSource = WiredUserSourceHelper.USER_SOURCE_TRIGGERING;
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
        message.appendString("");
        message.appendInt(8);
        message.appendInt(this.whoSees);
        message.appendInt(this.highlightType);
        message.appendInt(this.intensity);
        message.appendInt(this.colorIndex);
        message.appendInt(this.animation);
        message.appendInt(this.easing);
        message.appendInt(this.targetType);
        message.appendInt(this.userSource);
        message.appendInt(this.furniSource);
        message.appendInt(this.getType().code);
        message.appendInt(this.getDelay());
        message.appendInt(0);
    }

    @Override
    public boolean saveData(ClientMessage packet, GameClient gameClient) throws WiredSaveException {
        int intCount = packet.readInt();
        int newWhoSees = intCount >= 1 ? Math.max(0, Math.min(1, packet.readInt())) : 0;
        int newType = intCount >= 2 ? Math.max(0, Math.min(2, packet.readInt())) : 0;
        int newIntensity = intCount >= 3 ? Math.max(0, Math.min(3, packet.readInt())) : 0;
        int newColor = intCount >= 4 ? Math.max(0, Math.min(20, packet.readInt())) : 0;
        int newAnim = intCount >= 5 ? Math.max(0, Math.min(2, packet.readInt())) : 0;
        int newEasing = intCount >= 6 ? Math.max(0, Math.min(1, packet.readInt())) : 0;
        int newTargetType = intCount >= 7 ? Math.max(0, Math.min(1, packet.readInt())) : 0;
        int newUserSource = intCount >= 8 ? WiredUserSourceHelper.normalizeUserSourceCode(packet.readInt()) : 0;
        for (int i = 8; i < intCount; i++) packet.readInt();

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

        int newFurniSource = WiredFurniSourceHelper.readOptionalSourceCode(packet);

        this.whoSees = newWhoSees;
        this.highlightType = newType;
        this.intensity = newIntensity;
        this.colorIndex = newColor;
        this.animation = newAnim;
        this.easing = newEasing;
        this.targetType = newTargetType;
        this.userSource = newUserSource;
        this.furniSource = newFurniSource;
        this.items.clear();
        this.items.addAll(newItems);
        this.setDelay(delay);
        return true;
    }

    static class JsonData {
        int whoSees;
        int highlightType;
        int intensity;
        int colorIndex;
        int animation;
        int easing;
        int targetType;
        int userSource;
        int furniSource;
        int delay;
        List<Integer> itemIds;

        public JsonData(int whoSees, int highlightType, int intensity, int colorIndex, int animation, int easing, int targetType, int userSource, int furniSource, int delay, List<Integer> itemIds) {
            this.whoSees = whoSees;
            this.highlightType = highlightType;
            this.intensity = intensity;
            this.colorIndex = colorIndex;
            this.animation = animation;
            this.easing = easing;
            this.targetType = targetType;
            this.userSource = userSource;
            this.furniSource = furniSource;
            this.delay = delay;
            this.itemIds = itemIds;
        }
    }
}
