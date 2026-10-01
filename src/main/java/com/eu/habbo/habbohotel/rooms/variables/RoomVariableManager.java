package com.eu.habbo.habbohotel.rooms.variables;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboItem;
import com.eu.habbo.habbohotel.wired.WiredHandler;
import com.eu.habbo.habbohotel.wired.WiredTriggerType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class RoomVariableManager {
    private static final Logger LOGGER = LoggerFactory.getLogger(RoomVariableManager.class);

    private final Room room;

    // Room-level variables: var_name_lowercase -> RoomVariable
    private final ConcurrentHashMap<String, RoomVariable> roomVariables = new ConcurrentHashMap<>();
    // User-level variables: user_id -> (var_name_lowercase -> RoomVariable)
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, RoomVariable>> userVariables = new ConcurrentHashMap<>();
    // Furni-level variables: furni_id -> (var_name_lowercase -> RoomVariable)
    private final ConcurrentHashMap<Integer, ConcurrentHashMap<String, RoomVariable>> furniVariables = new ConcurrentHashMap<>();
    // Context-level variables: execution thread local or signal chain
    private final ThreadLocal<ConcurrentHashMap<String, RoomVariable>> contextVariables = ThreadLocal.withInitial(ConcurrentHashMap::new);

    private static final Random RANDOM = new Random();
    private long loadTimestamp = System.currentTimeMillis();

    public RoomVariableManager(Room room) {
        this.room = room;
    }

    public void load() {
        if (this.room == null) return;

        this.roomVariables.clear();
        this.userVariables.clear();
        this.furniVariables.clear();
        this.loadTimestamp = System.currentTimeMillis();

        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT user_id, var_name, var_value FROM room_variables WHERE room_id = ?")) {
            statement.setInt(1, this.room.getId());
            try (ResultSet set = statement.executeQuery()) {
                while (set.next()) {
                    int userId = set.getInt("user_id");
                    String varName = set.getString("var_name").toLowerCase().trim();
                    int varValue = 0;
                    try {
                        varValue = Integer.parseInt(set.getString("var_value"));
                    } catch (Exception ignored) {
                        try {
                            varValue = set.getInt("var_value");
                        } catch (Exception ignored2) {}
                    }

                    RoomVariable rv = new RoomVariable(varName, varValue, true, true);
                    if (userId == 0) {
                        this.roomVariables.put(varName, rv);
                    } else if (userId > 0) {
                        this.userVariables.computeIfAbsent(userId, k -> new ConcurrentHashMap<>()).put(varName, rv);
                    } else {
                        // Negative userId is mapped to furni
                        this.furniVariables.computeIfAbsent(-userId, k -> new ConcurrentHashMap<>()).put(varName, rv);
                    }
                }
            }
        } catch (SQLException e) {
            LOGGER.error("Failed to load room_variables for room {}", this.room.getId(), e);
        }
    }

    public long getWiredTimerTicks() {
        return Math.max(0, (System.currentTimeMillis() - this.loadTimestamp) / 500L);
    }

    public void resetWiredTimer() {
        this.loadTimestamp = System.currentTimeMillis();
    }

    // Overload for backwards compatibility
    public int getVariable(String name, int userId, boolean isUserScope) {
        return getVariable(name, userId, isUserScope ? VariableScope.USER : VariableScope.ROOM, null, null, null);
    }

    public int getVariable(String name, int entityId, VariableScope scope, Habbo habbo, RoomUnit unit, HabboItem item) {
        if (name == null || name.trim().isEmpty()) return 0;
        String key = name.toLowerCase().trim();

        if (InternalVariableEvaluator.isInternal(key)) {
            return InternalVariableEvaluator.get(key, this.room, habbo, unit, item);
        }

        RoomVariable rv = getRoomVariable(key, entityId, scope);
        return rv != null ? rv.getValue() : 0;
    }

    public boolean hasVariable(String name, int entityId, VariableScope scope) {
        if (name == null || name.trim().isEmpty()) return false;
        String key = name.toLowerCase().trim();

        if (InternalVariableEvaluator.isInternal(key)) {
            return true;
        }

        RoomVariable rv = getRoomVariable(key, entityId, scope);
        return rv != null;
    }

    public RoomVariable getRoomVariable(String name, int entityId, VariableScope scope) {
        if (name == null || name.trim().isEmpty()) return null;
        String key = name.toLowerCase().trim();

        switch (scope) {
            case USER:
                if (entityId > 0) {
                    ConcurrentHashMap<String, RoomVariable> map = this.userVariables.get(entityId);
                    return map != null ? map.get(key) : null;
                }
                return null;
            case FURNI:
                if (entityId > 0) {
                    ConcurrentHashMap<String, RoomVariable> map = this.furniVariables.get(entityId);
                    return map != null ? map.get(key) : null;
                }
                return null;
            case CONTEXT:
                return this.contextVariables.get().get(key);
            case ROOM:
            default:
                return this.roomVariables.get(key);
        }
    }

    public void setVariable(String name, int entityId, VariableScope scope, int value, boolean hasValue, boolean isPermanent, RoomUnit triggeredUnit, Habbo habbo, HabboItem item) {
        if (name == null || name.trim().isEmpty()) return;
        String key = name.toLowerCase().trim();

        if (InternalVariableEvaluator.isInternal(key)) {
            InternalVariableEvaluator.set(key, value, this.room, habbo, triggeredUnit, item);
            return;
        }

        int oldValue = getVariable(key, entityId, scope, habbo, triggeredUnit, item);
        RoomVariable existing = getRoomVariable(key, entityId, scope);

        if (existing != null) {
            existing.setValue(value);
            existing.setHasValue(hasValue);
            existing.setPermanent(isPermanent);
        } else {
            RoomVariable newVar = new RoomVariable(key, value, hasValue, isPermanent);
            switch (scope) {
                case USER:
                    if (entityId > 0) {
                        this.userVariables.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>()).put(key, newVar);
                    }
                    break;
                case FURNI:
                    if (entityId > 0) {
                        this.furniVariables.computeIfAbsent(entityId, k -> new ConcurrentHashMap<>()).put(key, newVar);
                    }
                    break;
                case CONTEXT:
                    this.contextVariables.get().put(key, newVar);
                    break;
                case ROOM:
                default:
                    this.roomVariables.put(key, newVar);
                    break;
            }
        }

        // Persist if permanent
        if (isPermanent && scope != VariableScope.CONTEXT) {
            final int dbTargetId = (scope == VariableScope.USER) ? entityId : ((scope == VariableScope.FURNI) ? -entityId : 0);
            Emulator.getThreading().run(() -> {
                try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                     PreparedStatement statement = connection.prepareStatement(
                             "INSERT INTO room_variables (room_id, user_id, var_name, var_value) VALUES (?, ?, ?, ?) ON DUPLICATE KEY UPDATE var_value = ?")) {
                    statement.setInt(1, this.room.getId());
                    statement.setInt(2, dbTargetId);
                    statement.setString(3, key);
                    statement.setString(4, String.valueOf(value));
                    statement.setString(5, String.valueOf(value));
                    statement.executeUpdate();
                } catch (SQLException e) {
                    LOGGER.error("Failed to persist variable {} for room {}", key, this.room.getId(), e);
                }
            });
        }

        // Trigger VARIABLE_CHANGED wireds if value changed
        if (oldValue != value) {
            WiredHandler.handle(WiredTriggerType.VARIABLE_CHANGED, triggeredUnit, this.room, new Object[]{ key, entityId, oldValue, value });
        }
    }

    public void removeVariable(String name, int entityId, VariableScope scope) {
        if (name == null || name.trim().isEmpty()) return;
        String key = name.toLowerCase().trim();

        switch (scope) {
            case USER:
                if (entityId > 0) {
                    ConcurrentHashMap<String, RoomVariable> map = this.userVariables.get(entityId);
                    if (map != null) map.remove(key);
                }
                break;
            case FURNI:
                if (entityId > 0) {
                    ConcurrentHashMap<String, RoomVariable> map = this.furniVariables.get(entityId);
                    if (map != null) map.remove(key);
                }
                break;
            case CONTEXT:
                this.contextVariables.get().remove(key);
                break;
            case ROOM:
            default:
                this.roomVariables.remove(key);
                break;
        }

        // Remove from DB if exists
        final int dbTargetId = (scope == VariableScope.USER) ? entityId : ((scope == VariableScope.FURNI) ? -entityId : 0);
        Emulator.getThreading().run(() -> {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement(
                         "DELETE FROM room_variables WHERE room_id = ? AND user_id = ? AND var_name = ?")) {
                statement.setInt(1, this.room.getId());
                statement.setInt(2, dbTargetId);
                statement.setString(3, key);
                statement.executeUpdate();
            } catch (SQLException e) {
                LOGGER.error("Failed to delete variable {} for room {}", key, this.room.getId(), e);
            }
        });
    }

    // Overload for backwards compatibility
    public void modifyVariable(String name, int userId, boolean isUserScope, int operation, int operand, RoomUnit triggeredUnit) {
        modifyVariable(name, userId, isUserScope ? VariableScope.USER : VariableScope.ROOM, operation, operand, triggeredUnit, null, null);
    }

    public void modifyVariable(String name, int entityId, VariableScope scope, int operation, int operand, RoomUnit triggeredUnit, Habbo habbo, HabboItem item) {
        if (name == null || name.trim().isEmpty()) return;
        int current = getVariable(name, entityId, scope, habbo, triggeredUnit, item);
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
            case 6: // Modulo
                nextValue = (operand != 0) ? (current % operand) : 0;
                break;
            case 7: // Power
                nextValue = (int) Math.pow(current, operand);
                break;
            default:
                nextValue = operand;
                break;
        }

        RoomVariable existing = getRoomVariable(name, entityId, scope);
        boolean isPermanent = existing != null && existing.isPermanent();
        setVariable(name, entityId, scope, nextValue, true, isPermanent, triggeredUnit, habbo, item);
    }

    public long getVariableAgeSeconds(String name, int entityId, VariableScope scope, boolean creationAge) {
        RoomVariable rv = getRoomVariable(name, entityId, scope);
        if (rv == null) return 0;
        return creationAge ? rv.getAgeSeconds() : rv.getUpdateAgeSeconds();
    }

    public Map<String, RoomVariable> getRoomVariables() {
        return this.roomVariables;
    }

    public Map<Integer, ConcurrentHashMap<String, RoomVariable>> getUserVariables() {
        return this.userVariables;
    }

    public Map<Integer, ConcurrentHashMap<String, RoomVariable>> getFurniVariables() {
        return this.furniVariables;
    }

    public void clearContextVariables() {
        this.contextVariables.get().clear();
    }

    public void dispose() {
        this.roomVariables.clear();
        this.userVariables.clear();
        this.furniVariables.clear();
        this.contextVariables.remove();
    }
}
