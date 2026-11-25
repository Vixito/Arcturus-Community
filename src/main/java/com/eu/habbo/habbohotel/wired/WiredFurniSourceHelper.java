package com.eu.habbo.habbohotel.wired;

import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredSelectorAltitude;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredSelectorFurniSelection;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredSelectorNeighborhood;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.ClientMessage;
import gnu.trove.set.hash.THashSet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public final class WiredFurniSourceHelper {
    public static final int SOURCE_SELECTED = 0;
    public static final int SOURCE_TRIGGERING = 100;
    public static final int SOURCE_SELECTOR = 200;
    public static final int SOURCE_SIGNAL = 201;

    private WiredFurniSourceHelper() {}

    public static int readOptionalSourceCode(ClientMessage packet) {
        if (packet != null && packet.bytesAvailable() >= 4) {
            return normalizeSourceCode(packet.readInt());
        }
        return SOURCE_SELECTED;
    }

    public static int normalizeSourceCode(int code) {
        if (code == SOURCE_TRIGGERING || code == SOURCE_SELECTOR || code == SOURCE_SIGNAL) {
            return code;
        }
        return SOURCE_SELECTED;
    }

    @SuppressWarnings("unchecked")
    public static THashSet<HabboItem> resolveItems(InteractionWired wired, int sourceCode, Collection<HabboItem> selectedItems, Room room, Object[] stuff) {
        THashSet<HabboItem> result = new THashSet<>();
        if (room == null) return result;

        int normalized = normalizeSourceCode(sourceCode);

        if (normalized == SOURCE_TRIGGERING) {
            if (stuff != null) {
                for (Object obj : stuff) {
                    if (obj instanceof HabboItem) {
                        result.add((HabboItem) obj);
                        break;
                    }
                }
            }
            return result;
        }

        if (normalized == SOURCE_SIGNAL) {
            if (stuff != null) {
                for (Object obj : stuff) {
                    if (obj instanceof Collection) {
                        for (Object entry : (Collection<?>) obj) {
                            if (entry instanceof HabboItem) {
                                result.add((HabboItem) entry);
                            }
                        }
                    } else if (obj instanceof HabboItem) {
                        result.add((HabboItem) obj);
                    }
                }
            }
            return result;
        }

        if (normalized == SOURCE_SELECTOR && wired != null && room.getRoomSpecialTypes() != null) {
            THashSet<InteractionWiredCondition> stackConditions = room.getRoomSpecialTypes().getConditions(wired.getX(), wired.getY());
            if (stackConditions != null) {
                for (InteractionWiredCondition cond : stackConditions) {
                    if (cond instanceof WiredSelectorFurniSelection) {
                        WiredSelectorFurniSelection sel = (WiredSelectorFurniSelection) cond;
                        THashSet<HabboItem> picked = sel.getSelectedItems();
                        String baseName = sel.getBaseItem() != null ? sel.getBaseItem().getName() : "";

                        if ("wf_slc_furni_bytype".equals(baseName)) {
                            Set<Integer> baseIds = new HashSet<>();
                            for (HabboItem p : picked) {
                                if (p != null && p.getBaseItem() != null) baseIds.add(p.getBaseItem().getId());
                            }
                            if (!baseIds.isEmpty()) {
                                for (HabboItem floorItem : room.getFloorItems()) {
                                    if (floorItem != null && floorItem.getBaseItem() != null && baseIds.contains(floorItem.getBaseItem().getId())) {
                                        result.add(floorItem);
                                    }
                                }
                            }
                        } else if ("wf_slc_furni_onfurni".equals(baseName)) {
                            for (HabboItem p : picked) {
                                if (p == null) continue;
                                for (HabboItem atTile : room.getItemsAt(p.getX(), p.getY())) {
                                    if (atTile != null && atTile.getId() != p.getId() && atTile.getZ() >= p.getZ()) {
                                        result.add(atTile);
                                    }
                                }
                            }
                        } else if ("wf_slc_furni_area".equals(baseName)) {
                            if (!picked.isEmpty()) {
                                int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
                                int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
                                for (HabboItem p : picked) {
                                    if (p == null) continue;
                                    if (p.getX() < minX) minX = p.getX();
                                    if (p.getX() > maxX) maxX = p.getX();
                                    if (p.getY() < minY) minY = p.getY();
                                    if (p.getY() > maxY) maxY = p.getY();
                                }
                                for (HabboItem floorItem : room.getFloorItems()) {
                                    if (floorItem != null && floorItem.getX() >= minX && floorItem.getX() <= maxX && floorItem.getY() >= minY && floorItem.getY() <= maxY) {
                                        result.add(floorItem);
                                    }
                                }
                            }
                        } else {
                            for (HabboItem p : picked) {
                                if (p != null && room.getHabboItem(p.getId()) != null) {
                                    result.add(p);
                                }
                            }
                        }
                    } else if (cond instanceof WiredSelectorAltitude) {
                        WiredSelectorAltitude altSel = (WiredSelectorAltitude) cond;
                        double minAlt = altSel.getMinAltitude();
                        double maxAlt = altSel.getMaxAltitude();
                        for (HabboItem floorItem : room.getFloorItems()) {
                            if (floorItem != null && floorItem.getZ() >= minAlt && floorItem.getZ() <= maxAlt) {
                                result.add(floorItem);
                            }
                        }
                    } else if (cond instanceof WiredSelectorNeighborhood) {
                        WiredSelectorNeighborhood neigh = (WiredSelectorNeighborhood) cond;
                        int radius = Math.max(0, neigh.getRadius());
                        int cx = wired.getX();
                        int cy = wired.getY();
                        if (stuff != null && stuff.length > 0 && stuff[0] instanceof HabboItem) {
                            cx = ((HabboItem) stuff[0]).getX();
                            cy = ((HabboItem) stuff[0]).getY();
                        }
                        for (HabboItem floorItem : room.getFloorItems()) {
                            if (floorItem != null && Math.abs(floorItem.getX() - cx) <= radius && Math.abs(floorItem.getY() - cy) <= radius) {
                                result.add(floorItem);
                            }
                        }
                    }
                }
            }
            return result;
        }

        if (selectedItems != null) {
            for (HabboItem item : selectedItems) {
                if (item != null && room.getHabboItem(item.getId()) != null) {
                    result.add(item);
                }
            }
        }
        return result;
    }
}
