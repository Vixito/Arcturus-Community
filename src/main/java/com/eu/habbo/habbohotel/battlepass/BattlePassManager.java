package com.eu.habbo.habbohotel.battlepass;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import gnu.trove.map.hash.THashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class BattlePassManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(BattlePassManager.class);

    private final THashMap<String, List<BattlePassMission>> missionsByType = new THashMap<>();
    private final THashMap<Integer, BattlePassMission> missionsById = new THashMap<>();

    public BattlePassManager() {
        this.load();
    }

    public synchronized void load() {
        this.missionsByType.clear();
        this.missionsById.clear();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT * FROM battle_pass_missions ORDER BY id ASC");
             ResultSet set = statement.executeQuery()) {

            while (set.next()) {
                BattlePassMission mission = new BattlePassMission(set);
                this.missionsById.put(mission.getId(), mission);

                this.missionsByType.putIfAbsent(mission.getType(), new ArrayList<>());
                this.missionsByType.get(mission.getType()).add(mission);
            }

            LOGGER.info("BattlePass Manager -> Loaded {} missions across {} types.", this.missionsById.size(), this.missionsByType.size());
        } catch (SQLException e) {
            LOGGER.error("Failed to load BattlePass missions from database", e);
        }
    }

    public void progress(Habbo habbo, String missionType) {
        this.progress(habbo, missionType, 1);
    }

    public void progress(Habbo habbo, String missionType, int amount) {
        if (habbo == null || missionType == null || amount <= 0) {
            return;
        }

        List<BattlePassMission> targetMissions = this.missionsByType.get(missionType);
        if (targetMissions == null || targetMissions.isEmpty()) {
            return;
        }

        final int userId = habbo.getHabboInfo().getId();
        final int currentTimestamp = Emulator.getIntUnixTimestamp();

        Emulator.getThreading().run(() -> {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection()) {
                for (BattlePassMission mission : targetMissions) {
                    int currentProgress = 0;

                    try (PreparedStatement checkStmt = connection.prepareStatement("SELECT task FROM battle_pass_users_missions WHERE user_id = ? AND mission_id = ? LIMIT 1")) {
                        checkStmt.setInt(1, userId);
                        checkStmt.setInt(2, mission.getId());
                        try (ResultSet rs = checkStmt.executeQuery()) {
                            if (rs.next()) {
                                currentProgress = rs.getInt("task");
                            }
                        }
                    }

                    if (currentProgress >= mission.getTask()) {
                        continue; // Ya completada previamente
                    }

                    int newProgress = Math.min(currentProgress + amount, mission.getTask());

                    try (PreparedStatement updateStmt = connection.prepareStatement(
                            "INSERT INTO battle_pass_users_missions (user_id, mission_id, task, timestamp) VALUES (?, ?, ?, ?) " +
                                    "ON DUPLICATE KEY UPDATE task = ?, timestamp = ?")) {
                        updateStmt.setInt(1, userId);
                        updateStmt.setInt(2, mission.getId());
                        updateStmt.setInt(3, newProgress);
                        updateStmt.setInt(4, currentTimestamp);
                        updateStmt.setInt(5, newProgress);
                        updateStmt.setInt(6, currentTimestamp);
                        updateStmt.execute();
                    }

                    if (newProgress >= mission.getTask()) {
                        // Misión completada por primera vez: otorgar XP
                        int rewardXp = mission.getRewardXp();
                        int userLevel = 1;
                        int userXp = 0;

                        try (PreparedStatement userStmt = connection.prepareStatement("SELECT level, xp FROM battle_pass_users WHERE user_id = ? LIMIT 1")) {
                            userStmt.setInt(1, userId);
                            try (ResultSet rs = userStmt.executeQuery()) {
                                if (rs.next()) {
                                    userLevel = Math.max(1, rs.getInt("level"));
                                    userXp = rs.getInt("xp");
                                }
                            }
                        }

                        userXp += rewardXp;
                        boolean leveledUp = false;
                        int oldLevel = userLevel;

                        while (userXp >= userLevel * 100) {
                            userXp -= userLevel * 100;
                            userLevel++;
                            leveledUp = true;
                        }

                        try (PreparedStatement saveUserStmt = connection.prepareStatement(
                                "INSERT INTO battle_pass_users (user_id, level, xp) VALUES (?, ?, ?) " +
                                        "ON DUPLICATE KEY UPDATE level = ?, xp = ?")) {
                            saveUserStmt.setInt(1, userId);
                            saveUserStmt.setInt(2, userLevel);
                            saveUserStmt.setInt(3, userXp);
                            saveUserStmt.setInt(4, userLevel);
                            saveUserStmt.setInt(5, userXp);
                            saveUserStmt.execute();
                        }

                        if (habbo.isOnline()) {
                            habbo.whisper("¡Has completado el reto \"" + mission.getName() + "\" del Pase de Batalla! (+" + rewardXp + " XP)", RoomChatMessageBubbles.ALERT);
                            if (leveledUp) {
                                habbo.whisper("¡Felicidades! Has subido al Nivel " + userLevel + " en el Pase de Batalla.", RoomChatMessageBubbles.FRANK);
                            }
                        }
                    }
                }
            } catch (SQLException e) {
                LOGGER.error("Error updating BattlePass progress for user: " + userId + " (missionType: " + missionType + ")", e);
            }
        });
    }
}
