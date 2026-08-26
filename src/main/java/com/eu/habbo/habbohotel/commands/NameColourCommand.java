package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.subscriptions.Subscription;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.events.users.UserLoginEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

public class NameColourCommand extends Command implements EventListener {
    public static final String CACHE_KEY = "name_colour";

    private static final Map<String, String> COLOURS = new LinkedHashMap<>();

    static {
        COLOURS.put("rainbow", "rainbow");
        COLOURS.put("gold", "#f59e0b");
        COLOURS.put("dorado", "#f59e0b");
        COLOURS.put("red", "#ef4444");
        COLOURS.put("rojo", "#ef4444");
        COLOURS.put("blue", "#3b82f6");
        COLOURS.put("azul", "#3b82f6");
        COLOURS.put("green", "#10b981");
        COLOURS.put("verde", "#10b981");
        COLOURS.put("purple", "#8b5cf6");
        COLOURS.put("morado", "#8b5cf6");
        COLOURS.put("pink", "#ec4899");
        COLOURS.put("rosa", "#ec4899");
        COLOURS.put("cyan", "#06b6d4");
        COLOURS.put("celeste", "#06b6d4");
        COLOURS.put("orange", "#f97316");
        COLOURS.put("naranja", "#f97316");
        COLOURS.put("lime", "#84cc16");
        COLOURS.put("lima", "#84cc16");
        COLOURS.put("white", "#ffffff");
        COLOURS.put("blanco", "#ffffff");
        COLOURS.put("yellow", "#eab308");
        COLOURS.put("amarillo", "#eab308");
        COLOURS.put("silver", "#94a3b8");
        COLOURS.put("plata", "#94a3b8");
    }

    public NameColourCommand() {
        super("cmd_chatcolor", Emulator.getTexts().getValue("commands.keys.cmd_namecolour", "namecolour;colorname;namecolor;colour;color").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Habbo habbo = gameClient.getHabbo();
        if (habbo == null) return false;

        boolean isVip = (habbo.getInventory() != null && habbo.getInventory().getBadgesComponent() != null && habbo.getInventory().getBadgesComponent().hasBadge("VIP"))
                || habbo.getHabboStats().hasSubscription("BATTLE_PASS_VIP")
                || habbo.getHabboInfo().getRank().getId() >= 2
                || habbo.hasPermission("acc_vip")
                || habbo.hasPermission("acc_chatcolor");
        if (!isVip) {
            habbo.whisper("⚠️ El comando :namecolour es exclusivo para miembros Habbten VIP. Adquiere tu membresía VIP en la Tienda Oficial para desbloquearlo.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (params.length < 2 || params[1].equalsIgnoreCase("help") || params[1].equalsIgnoreCase("list")) {
            String activeColor = (String) habbo.getHabboStats().cache.get(CACHE_KEY);
            StringBuilder sb = new StringBuilder();
            sb.append("<b>🎨 Tus Colores de Nombre Disponibles (Habbten VIP):</b>\r\r");
            
            for (Map.Entry<String, String> entry : COLOURS.entrySet()) {
                String colorName = entry.getKey();
                String hex = entry.getValue();
                boolean isActive = activeColor != null && (activeColor.equalsIgnoreCase(hex) || (activeColor.equalsIgnoreCase("rainbow") && hex.equalsIgnoreCase("rainbow")));
                
                sb.append("• <b>").append(colorName).append("</b>");
                if (isActive) {
                    sb.append(" <i><font color=\"#10b981\">[Equipado actualmente]</font></i>");
                }
                sb.append("\r");
            }
            
            sb.append("\r<i>Escribe: <code>:namecolour [color]</code> para equipar.</i>\r");
            sb.append("<i>Para restablecer al color normal: <code>:namecolour reset</code></i>");
            habbo.alert(sb.toString());
            return true;
        }

        String inputColor = params[1].toLowerCase();

        if (inputColor.equals("reset") || inputColor.equals("none") || inputColor.equals("normal") || inputColor.equals("off")) {
            habbo.getHabboStats().cache.remove(CACHE_KEY);
            saveColor(habbo.getHabboInfo().getId(), "");
            if (habbo.getHabboInfo().getCurrentRoom() != null) {
                habbo.getHabboInfo().getCurrentRoom().sendComposer(new com.eu.habbo.messages.outgoing.rooms.users.RoomUsersComposer(habbo).compose());
            }
            habbo.whisper("Has restablecido el color de tu nombre al color original por defecto.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        String colorHex = COLOURS.get(inputColor);
        if (colorHex == null) {
            if (inputColor.matches("^#?[0-9a-fA-F]{6}$")) {
                colorHex = inputColor.startsWith("#") ? inputColor : "#" + inputColor;
            } else {
                habbo.whisper("El color '" + inputColor + "' no existe. Escribe :namecolour list para ver los colores disponibles.", RoomChatMessageBubbles.ALERT);
                return true;
            }
        }

        habbo.getHabboStats().cache.put(CACHE_KEY, colorHex);
        saveColor(habbo.getHabboInfo().getId(), colorHex);
        if (habbo.getHabboInfo().getCurrentRoom() != null) {
            habbo.getHabboInfo().getCurrentRoom().sendComposer(new com.eu.habbo.messages.outgoing.rooms.users.RoomUsersComposer(habbo).compose());
        }
        habbo.whisper("¡Color de nombre actualizado con éxito a: <b>" + inputColor.toUpperCase() + "</b>!", RoomChatMessageBubbles.ALERT);
        return true;
    }

    private void saveColor(int userId, String color) {
        Emulator.getThreading().run(() -> {
            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement("UPDATE users SET name_colour = ? WHERE id = ?")) {
                statement.setString(1, color);
                statement.setInt(2, userId);
                statement.executeUpdate();
            } catch (SQLException e) {
                Emulator.getLogging().logSQLException(e);
            }
        });
    }

    @EventHandler
    public static void onUserLogin(UserLoginEvent event) {
        if (event.habbo == null) return;
        try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT name_colour FROM users WHERE id = ? LIMIT 1")) {
            statement.setInt(1, event.habbo.getHabboInfo().getId());
            try (ResultSet set = statement.executeQuery()) {
                if (set.next()) {
                    String color = set.getString("name_colour");
                        boolean isVip = (event.habbo.getInventory() != null && event.habbo.getInventory().getBadgesComponent() != null && event.habbo.getInventory().getBadgesComponent().hasBadge("VIP"))
                                || event.habbo.getHabboStats().hasSubscription("BATTLE_PASS_VIP")
                                || event.habbo.getHabboInfo().getRank().getId() >= 2
                                || event.habbo.hasPermission("acc_vip")
                                || event.habbo.hasPermission("acc_chatcolor");
                        if (isVip) {
                            event.habbo.getHabboStats().cache.put(CACHE_KEY, color);
                        } else {
                            event.habbo.getHabboStats().cache.remove(CACHE_KEY);
                        }
                }
            }
        } catch (SQLException e) {
            Emulator.getLogging().logSQLException(e);
        }
    }

    public static String formatName(String username, String color) {
        if (color == null || color.isEmpty()) return username;
        if (color.equalsIgnoreCase("rainbow")) {
            String[] rainbowColors = new String[]{"#ef4444", "#f97316", "#eab308", "#10b981", "#06b6d4", "#3b82f6", "#8b5cf6"};
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < username.length(); i++) {
                String c = rainbowColors[i % rainbowColors.length];
                sb.append("<font color=\"").append(c).append("\">").append(username.charAt(i)).append("</font>");
            }
            return sb.toString();
        }
        return "<font color=\"" + color + "\">" + username + "</font>";
    }
}
