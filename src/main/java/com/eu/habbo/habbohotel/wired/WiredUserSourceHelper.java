package com.eu.habbo.habbohotel.wired;

import com.eu.habbo.habbohotel.items.interactions.InteractionWired;
import com.eu.habbo.habbohotel.items.interactions.InteractionWiredCondition;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredSelectorByName;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredSelectorFurniSelection;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredSelectorGroup;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredSelectorHandItem;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredSelectorNeighborhood;
import com.eu.habbo.habbohotel.items.interactions.wired.extra.WiredSelectorTeam;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import gnu.trove.set.hash.THashSet;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class WiredUserSourceHelper {
    public static final int USER_SOURCE_TRIGGERING = 0;
    public static final int USER_SOURCE_SELECTOR = 200;
    public static final int USER_SOURCE_SIGNAL = 201;
    public static final int USER_SOURCE_ALL = 900;

    private WiredUserSourceHelper() {}

    public static int normalizeUserSourceCode(int code) {
        if (code == USER_SOURCE_SELECTOR || code == USER_SOURCE_SIGNAL || code == USER_SOURCE_ALL) {
            return code;
        }
        return USER_SOURCE_TRIGGERING;
    }

    public static List<Habbo> resolveUsers(InteractionWired wired, int userSourceCode, RoomUnit triggeringUnit, Room room, Object[] stuff) {
        Set<Habbo> result = new LinkedHashSet<>();
        if (room == null) return new ArrayList<>();

        int normalized = normalizeUserSourceCode(userSourceCode);

        if (normalized == USER_SOURCE_ALL) {
            result.addAll(room.getHabbos());
            return new ArrayList<>(result);
        }

        if (normalized == USER_SOURCE_SIGNAL) {
            if (stuff != null) {
                for (Object obj : stuff) {
                    if (obj instanceof Habbo) {
                        result.add((Habbo) obj);
                    } else if (obj instanceof RoomUnit) {
                        Habbo h = room.getHabbo((RoomUnit) obj);
                        if (h != null) result.add(h);
                    } else if (obj instanceof Collection) {
                        for (Object entry : (Collection<?>) obj) {
                            if (entry instanceof Habbo) {
                                result.add((Habbo) entry);
                            } else if (entry instanceof RoomUnit) {
                                Habbo h = room.getHabbo((RoomUnit) entry);
                                if (h != null) result.add(h);
                            }
                        }
                    }
                }
            }
            if (result.isEmpty() && triggeringUnit != null) {
                Habbo h = room.getHabbo(triggeringUnit);
                if (h != null) result.add(h);
            }
            return new ArrayList<>(result);
        }

        if (normalized == USER_SOURCE_SELECTOR && wired != null && room.getRoomSpecialTypes() != null) {
            THashSet<InteractionWiredCondition> stackConditions = room.getRoomSpecialTypes().getConditions(wired.getX(), wired.getY());
            if (stackConditions != null) {
                for (InteractionWiredCondition cond : stackConditions) {
                    if (cond instanceof WiredSelectorTeam) {
                        int teamColor = ((WiredSelectorTeam) cond).getSelectedTeam();
                        for (Habbo h : room.getHabbos()) {
                            if (h != null && h.getHabboInfo().getGamePlayer() != null && h.getHabboInfo().getGamePlayer().getTeamColor() != null) {
                                int t = h.getHabboInfo().getGamePlayer().getTeamColor().type;
                                if (t == teamColor) result.add(h);
                            }
                        }
                    } else if (cond instanceof WiredSelectorByName) {
                        String targetName = ((WiredSelectorByName) cond).getUsername();
                        if (targetName != null && !targetName.trim().isEmpty()) {
                            for (String part : targetName.split("[,\\r\\n]+")) {
                                String clean = part.trim();
                                if (clean.isEmpty()) continue;
                                Habbo h = room.getHabbo(clean);
                                if (h != null) result.add(h);
                            }
                        }
                    } else if (cond instanceof WiredSelectorGroup) {
                        int guildId = room.getGuildId();
                        if (guildId > 0) {
                            for (Habbo h : room.getHabbos()) {
                                if (h != null && h.getHabboStats().hasGuild(guildId)) {
                                    result.add(h);
                                }
                            }
                        }
                    } else if (cond instanceof WiredSelectorHandItem) {
                        int handItem = ((WiredSelectorHandItem) cond).getHandItemId();
                        for (Habbo h : room.getHabbos()) {
                            if (h != null && h.getRoomUnit() != null && h.getRoomUnit().getHandItem() == handItem) {
                                result.add(h);
                            }
                        }
                    } else if (cond instanceof WiredSelectorFurniSelection) {
                        WiredSelectorFurniSelection sel = (WiredSelectorFurniSelection) cond;
                        String baseName = sel.getBaseItem() != null ? sel.getBaseItem().getName() : "";
                        if ("wf_slc_users_onfurni".equals(baseName)) {
                            for (HabboItem p : sel.getSelectedItems()) {
                                if (p == null) continue;
                                for (Habbo h : room.getHabbosAt(p.getX(), p.getY())) {
                                    if (h != null) result.add(h);
                                }
                            }
                        } else if ("wf_slc_users_area".equals(baseName) && !sel.getSelectedItems().isEmpty()) {
                            int minX = Integer.MAX_VALUE, maxX = Integer.MIN_VALUE;
                            int minY = Integer.MAX_VALUE, maxY = Integer.MIN_VALUE;
                            for (HabboItem p : sel.getSelectedItems()) {
                                if (p == null) continue;
                                if (p.getX() < minX) minX = p.getX();
                                if (p.getX() > maxX) maxX = p.getX();
                                if (p.getY() < minY) minY = p.getY();
                                if (p.getY() > maxY) maxY = p.getY();
                            }
                            for (Habbo h : room.getHabbos()) {
                                if (h != null && h.getRoomUnit() != null && h.getRoomUnit().getCurrentLocation() != null) {
                                    int ux = h.getRoomUnit().getCurrentLocation().x;
                                    int uy = h.getRoomUnit().getCurrentLocation().y;
                                    if (ux >= minX && ux <= maxX && uy >= minY && uy <= maxY) {
                                        result.add(h);
                                    }
                                }
                            }
                        }
                    } else if (cond instanceof WiredSelectorNeighborhood) {
                        int radius = Math.max(0, ((WiredSelectorNeighborhood) cond).getRadius());
                        int cx = wired.getX();
                        int cy = wired.getY();
                        if (triggeringUnit != null && triggeringUnit.getCurrentLocation() != null) {
                            cx = triggeringUnit.getCurrentLocation().x;
                            cy = triggeringUnit.getCurrentLocation().y;
                        }
                        for (Habbo h : room.getHabbos()) {
                            if (h != null && h.getRoomUnit() != null && h.getRoomUnit().getCurrentLocation() != null) {
                                int ux = h.getRoomUnit().getCurrentLocation().x;
                                int uy = h.getRoomUnit().getCurrentLocation().y;
                                if (Math.abs(ux - cx) <= radius && Math.abs(uy - cy) <= radius) {
                                    result.add(h);
                                }
                            }
                        }
                    }
                }
            }
            return new ArrayList<>(result);
        }

        if (triggeringUnit != null) {
            Habbo h = room.getHabbo(triggeringUnit);
            if (h != null) result.add(h);
        }
        return new ArrayList<>(result);
    }
}
