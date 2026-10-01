package com.eu.habbo.habbohotel.rooms.variables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.games.GameTeamColors;
import com.eu.habbo.habbohotel.items.interactions.InteractionWater;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomTile;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.rooms.RoomUnitStatus;
import com.eu.habbo.habbohotel.rooms.RoomUserRotation;
import com.eu.habbo.habbohotel.users.DanceType;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserDanceComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserEffectComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserHandItemComposer;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserStatusComposer;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoField;
import java.time.temporal.IsoFields;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class InternalVariableEvaluator {

    private static final Set<String> WRITABLE_VARS = new HashSet<>(Arrays.asList(
            "@position_x", "@position_y", "@rotation", "@altitude", "@state",
            "@direction", "@handitem_id", "@effect_id", "@dance", "@sign",
            "@wallitem_offset", "@team_red_score", "@team_green_score",
            "@team_blue_score", "@team_yellow_score"
    ));

    public static boolean isInternal(String name) {
        return name != null && name.trim().startsWith("@");
    }

    public static boolean isWritable(String name) {
        if (name == null) return false;
        return WRITABLE_VARS.contains(name.toLowerCase().trim());
    }

    public static int get(String name, Room room, Habbo habbo, RoomUnit unit, HabboItem item) {
        if (name == null || !name.startsWith("@")) return 0;
        String key = name.toLowerCase().trim();

        // 1. Furni variables
        if (item != null) {
            switch (key) {
                case "@id":
                    return item.getId();
                case "@class_id":
                    return item.getBaseItem() != null ? item.getBaseItem().getId() : 0;
                case "@height":
                    return item.getBaseItem() != null ? (int) Math.round(item.getBaseItem().getHeight() * 100) : 0;
                case "@state":
                    try {
                        return Integer.parseInt(item.getExtradata());
                    } catch (Exception ignored) {
                        return 0;
                    }
                case "@position_x":
                    return item.getX();
                case "@position_y":
                    return item.getY();
                case "@rotation":
                    return item.getRotation();
                case "@altitude":
                    return (int) Math.round(item.getZ() * 100);
                case "@is_invisible":
                    return 0;
                case "@type":
                    return item.isLimited() ? 1 : 0;
                case "@is_stackable":
                    return (item.getBaseItem() != null && item.getBaseItem().allowStack()) ? 1 : 0;
                case "@can_stand_on":
                    return (item.getBaseItem() != null && item.getBaseItem().allowWalk()) ? 1 : 0;
                case "@can_sit_on":
                    return (item.getBaseItem() != null && item.getBaseItem().allowSit()) ? 1 : 0;
                case "@can_lay_on":
                    return (item.getBaseItem() != null && item.getBaseItem().allowLay()) ? 1 : 0;
                case "@owner_id":
                    return item.getUserId();
                case "@wallitem_offset":
                    return 0;
            }
        }

        // 2. User / Bot / Pet variables
        if (unit != null || habbo != null) {
            switch (key) {
                case "@index":
                    return unit != null ? unit.getId() : 0;
                case "@type":
                    if (habbo != null) return 1; // User
                    if (unit != null) {
                        switch (unit.getRoomUnitType()) {
                            case PET: return 2; // Pet
                            case BOT: return 4; // Bot
                        }
                    }
                    return 1;
                case "@gender":
                    if (habbo != null && habbo.getHabboInfo() != null) {
                        return habbo.getHabboInfo().getGender().name().equalsIgnoreCase("M") ? 0 : 1;
                    }
                    return -1;
                case "@level":
                    if (habbo != null && habbo.getHabboInfo() != null && habbo.getHabboInfo().getRank() != null) {
                        return habbo.getHabboInfo().getRank().getLevel();
                    }
                    return 1;
                case "@achievement_score":
                    return (habbo != null && habbo.getHabboStats() != null) ? habbo.getHabboStats().getAchievementScore() : 0;
                case "@is_hc":
                    return (habbo != null && habbo.getHabboStats() != null && habbo.getHabboStats().hasActiveClub()) ? 1 : 0;
                case "@has_rights":
                    return (habbo != null && room != null && room.hasRights(habbo)) ? 1 : 0;
                case "@is_group_admin":
                    if (habbo != null && room != null && room.hasGuild() && room.getGuildId() != 0) {
                        com.eu.habbo.habbohotel.guilds.Guild g = Emulator.getGameEnvironment().getGuildManager().getGuild(room.getGuildId());
                        return (g != null && Emulator.getGameEnvironment().getGuildManager().getOnlyAdmins(g).containsKey(habbo.getHabboInfo().getId())) ? 1 : 0;
                    }
                    return 0;
                case "@is_owner":
                    return (habbo != null && room != null && room.getOwnerId() == habbo.getHabboInfo().getId()) ? 1 : 0;
                case "@position_x":
                    return unit != null ? unit.getX() : 0;
                case "@position_y":
                    return unit != null ? unit.getY() : 0;
                case "@direction":
                    return unit != null ? unit.getBodyRotation().getValue() : 0;
                case "@altitude":
                    return unit != null ? (int) Math.round(unit.getZ() * 100) : 0;
                case "@handitem_id":
                    return unit != null ? unit.getHandItem() : 0;
                case "@effect_id":
                    return unit != null ? unit.getEffectId() : 0;
                case "@is_frozen":
                    return (unit != null && !unit.canWalk()) ? 1 : 0;
                case "@is_muted":
                    return (habbo != null && room != null && room.isMuted(habbo)) ? 1 : 0;
                case "@favourite_group_id":
                    return (habbo != null && habbo.getHabboStats() != null) ? habbo.getHabboStats().guild : 0;
                case "@dance":
                    return unit != null ? unit.getDanceType().getType() : 0;
                case "@sign":
                    return (unit != null && unit.hasStatus(RoomUnitStatus.SIGN)) ? Integer.parseInt(unit.getStatus(RoomUnitStatus.SIGN)) : 0;
                case "@is_idle":
                    return (unit != null && unit.isIdle()) ? 1 : 0;
                case "@user_id":
                    return (habbo != null && habbo.getHabboInfo() != null) ? habbo.getHabboInfo().getId() : 0;
                case "@pet_id":
                    return 0;
                case "@bot_id":
                    return 0;
                case "@pet_owner_id":
                    return 0;
            }
        }

        // 3. Global Room variables
        if (room != null) {
            switch (key) {
                case "@furni_count":
                    return room.getFloorItems().size() + room.getWallItems().size();
                case "@user_count":
                    return room.getUserCount();
                case "@wired_timer":
                    return (int) (room.getVariableManager().getWiredTimerTicks());
                case "@team_red_score":
                    return getTeamScore(room, GameTeamColors.RED);
                case "@team_green_score":
                    return getTeamScore(room, GameTeamColors.GREEN);
                case "@team_blue_score":
                    return getTeamScore(room, GameTeamColors.BLUE);
                case "@team_yellow_score":
                    return getTeamScore(room, GameTeamColors.YELLOW);
                case "@team_red_size":
                    return getTeamSize(room, GameTeamColors.RED);
                case "@team_green_size":
                    return getTeamSize(room, GameTeamColors.GREEN);
                case "@team_blue_size":
                    return getTeamSize(room, GameTeamColors.BLUE);
                case "@team_yellow_size":
                    return getTeamSize(room, GameTeamColors.YELLOW);
                case "@room_id":
                    return room.getId();
                case "@group_id":
                    return room.getGuildId();
            }
        }

        // 4. Current Time variables
        if (key.startsWith("@current_time")) {
            ZonedDateTime now = ZonedDateTime.now();
            switch (key) {
                case "@current_time":
                case "@current_time.hour_of_day":
                    return now.getHour();
                case "@current_time.milliseconds_of_seconds":
                    return now.get(ChronoField.MILLI_OF_SECOND);
                case "@current_time.seconds_of_minute":
                    return now.getSecond();
                case "@current_time.minute_of_hour":
                    return now.getMinute();
                case "@current_time.day_of_week":
                case "@current_time.time.day_of_week":
                    return now.getDayOfWeek().getValue();
                case "@current_time.day_of_month":
                    return now.getDayOfMonth();
                case "@current_time.day_of_year":
                    return now.getDayOfYear();
                case "@current_time.week_of_year":
                    return now.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR);
                case "@current_time.month_of_year":
                    return now.getMonthValue();
                case "@current_time.year":
                    return now.getYear();
            }
        }

        return 0;
    }

    public static void set(String name, int value, Room room, Habbo habbo, RoomUnit unit, HabboItem item) {
        if (name == null || !name.startsWith("@")) return;
        String key = name.toLowerCase().trim();

        // 1. Modify Furni
        if (item != null && room != null) {
            switch (key) {
                case "@state":
                    item.setExtradata(String.valueOf(value));
                    room.updateItem(item);
                    return;
                case "@rotation":
                    item.setRotation(value % 8);
                    room.updateItem(item);
                    return;
                case "@position_x":
                    RoomTile newTileX = room.getLayout().getTile((short) value, item.getY());
                    if (newTileX != null) {
                        room.moveFurniTo(item, newTileX, item.getRotation(), null);
                    }
                    return;
                case "@position_y":
                    RoomTile newTileY = room.getLayout().getTile(item.getX(), (short) value);
                    if (newTileY != null) {
                        room.moveFurniTo(item, newTileY, item.getRotation(), null);
                    }
                    return;
                case "@altitude":
                    double newZ = value / 100.0;
                    item.setZ(newZ);
                    room.updateItem(item);
                    return;
            }
        }

        // 2. Modify User / Unit
        if (unit != null && room != null) {
            switch (key) {
                case "@direction":
                    unit.setBodyRotation(RoomUserRotation.fromValue(value % 8));
                    unit.setHeadRotation(RoomUserRotation.fromValue(value % 8));
                    room.sendComposer(new RoomUserStatusComposer(unit).compose());
                    return;
                case "@position_x":
                    RoomTile uTileX = room.getLayout().getTile((short) value, unit.getY());
                    if (uTileX != null) {
                        unit.setGoalLocation(uTileX);
                    }
                    return;
                case "@position_y":
                    RoomTile uTileY = room.getLayout().getTile(unit.getX(), (short) value);
                    if (uTileY != null) {
                        unit.setGoalLocation(uTileY);
                    }
                    return;
                case "@handitem_id":
                    unit.setHandItem(value);
                    room.sendComposer(new RoomUserHandItemComposer(unit).compose());
                    return;
                case "@effect_id":
                    unit.setEffectId(value, -1);
                    room.sendComposer(new RoomUserEffectComposer(unit).compose());
                    return;
                case "@sign":
                    unit.setStatus(RoomUnitStatus.SIGN, String.valueOf(value));
                    room.sendComposer(new RoomUserStatusComposer(unit).compose());
                    return;
                case "@dance":
                    unit.setDanceType(DanceType.values()[Math.min(DanceType.values().length - 1, Math.max(0, value))]);
                    room.sendComposer(new RoomUserDanceComposer(unit).compose());
                    return;
            }
        }

        // 3. Modify Team Scores
        if (room != null) {
            switch (key) {
                case "@team_red_score":
                    setTeamScore(room, GameTeamColors.RED, value);
                    return;
                case "@team_green_score":
                    setTeamScore(room, GameTeamColors.GREEN, value);
                    return;
                case "@team_blue_score":
                    setTeamScore(room, GameTeamColors.BLUE, value);
                    return;
                case "@team_yellow_score":
                    setTeamScore(room, GameTeamColors.YELLOW, value);
                    return;
            }
        }
    }

    private static int getTeamScore(Room room, GameTeamColors color) {
        if (room == null) return 0;
        for (com.eu.habbo.habbohotel.games.Game g : room.getGames()) {
            com.eu.habbo.habbohotel.games.GameTeam team = g.getTeam(color);
            if (team != null) return team.getTotalScore();
        }
        return 0;
    }

    private static int getTeamSize(Room room, GameTeamColors color) {
        if (room == null) return 0;
        for (com.eu.habbo.habbohotel.games.Game g : room.getGames()) {
            com.eu.habbo.habbohotel.games.GameTeam team = g.getTeam(color);
            if (team != null) return team.getMembers().size();
        }
        return 0;
    }

    private static void setTeamScore(Room room, GameTeamColors color, int score) {
        if (room == null) return;
        for (com.eu.habbo.habbohotel.games.Game g : room.getGames()) {
            com.eu.habbo.habbohotel.games.GameTeam team = g.getTeam(color);
            if (team != null) {
                team.resetScores();
                team.addTeamScore(score);
            }
        }
    }
}
