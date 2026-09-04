package com.eu.habbo.habbohotel.rooms.variables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class RoomVariableManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomVariableManager.class);

    private final Room room;
    // Room-level variables: var_name_lowercase -> value
    private final ConcurrentHashMap<String, Integer> roomVariables = new ConcurrentHashMap<>();
    // User-level variables: user_id -> (var_name_lowercase -> value)
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, Integer>> userVariables = new ConcurrentHashMap<>();

    private static final Random RANDOM = new Random();

    public RoomVariableManager(Room room) {
        this.room = room;
    }

    public void load() {
        if (this.room == null) return;

        this.roomVariables.clear();
        this.userVariables.clear();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT user_id, var_name, var_value FROM room_variables WHERE room_id = ?")) {
            statement.setInt(1, this.room.getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    int userId = set.getInt("user_id");
                    String varName = set.getString("var_name").toLowerCase().trim();
                    int varValue = set.getInt("var_value");

                    if (userId == 0) {
                        this.roomVariables.put(varName, varValue);
                    } else {
                        this.userVariables.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(varName, varValue);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to load room_variables for room {}", this.room.getId(), e);
        }
    }

    public int getVariable(String name, int userId, boolean isUserScope) {
        if (name == null || name.trim().isEmpty()) return 0;
        String key = name.toLowerCase().trim();

        if (isUserScope && userId > 0) {
            ConcurrentHashMap<String, Integer> userMap = this.userVariables.get(userId);
            if (userMap != null && userMap.containsKey(key)) {
                return userMap.get(key);
            }
            return 0;
        }

        return this.roomVariables.getOrDefault(key, 0);
    }

    public void setVariable(String name, int userId, boolean isUserScope, int value, RoomUnit triggeredUnit) {
        if (name == null || name.trim().isEmpty()) return;
        String key = name.toLowerCase().trim();
        int targetUserId = (isUserScope && userId > 0) ? userId : 0;
        int oldValue;

        if (targetUserId > 0) {
            ConcurrentHashMap<String, Integer> userMap = this.userVariables.computeIfAbsent(targetUserId, k -> new ConcurrentHashMap<>());
            oldValue = userMap.getOrDefault(key, 0);
            userMap.put(key, value);
        } else {
            oldValue = this.roomVariables.getOrDefault(key, 0);
            this.roomVariables.put(key, value);
        }

        // Asynchronously persist to database
        Emulator.getThreading().run(() -> {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "INSERT INTO room_variables (room_id, user_id, var_name, var_value) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE var_value = ?")) {
                statement.setInt(1, this.room.getId());
                statement.setInt(2, targetUserId);
                statement.setString(3, key);
                statement.setInt(4, value);
                statement.setInt(5, value);
                statement.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Failed to persist variable {} for room {}", key, this.room.getId(), e);
            }
        });

        // Trigger VARIABLE_CHANGED wireds if value changed
        if (oldValue != value) {
            WiredHandler.handle(WiredTriggerType.VARIABLE_CHANGED, triggeredUnit, this.room, new Object[]{ key, targetUserId, oldValue, value });
        }
    }

    public void modifyVariable(String name, int userId, boolean isUserScope, int operation, int operand, RoomUnit triggeredUnit) {
        if (name == null || name.trim().isEmpty()) return;
        int current = getVariable(name, userId, isUserScope);
        int nextValue;

        switch (operation) {
            case 0: // Set to operand
                nextValue = operand;
                break;
            case 1: // Add
                nextValue = current + operand;
                break;
            case 2: // Subtract
                nextValue = current - operand;
                break;
            case 3: // Multiply
                nextValue = current * operand;
                break;
            case 4: // Divide
                nextValue = (operand != 0) ? (current / operand) : current;
                break;
            case 5: // Random (1 to operand, or 0 to operand)
                nextValue = (operand > 0) ? (RANDOM.nextInt(operand) + 1) : 0;
                break;
            default:
                nextValue = operand;
                break;
        }

        setVariable(name, userId, isUserScope, nextValue, triggeredUnit);
    }

    public void dispose() {
        this.roomVariables.clear();
        this.userVariables.clear();
    }
}
