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

        boolean isVip = habbo.getHabboStats().hasSubscription(Subscription.HABBO_CLUB) || habbo.getHabboInfo().getRank().getId() >= 2 || habbo.hasPermission("acc_chatcolor") || habbo.hasPermission("acc_vip");
        if (!isVip) {
            habbo.whisper("⚠️ El comando :namecolour es exclusivo para miembros VIP. Adquiere tu membresía VIP en la Tienda Oficial (/tienda) para desbloquearlo.", RoomChatMessageBubbles.ALERT);
            return true;
        }

        if (params.length < 2 || params[1].equalsIgnoreCase("help") || params[1].equalsIgnoreCase("list")) {
            StringBuilder sb = new StringBuilder();
            sb.append("<b>🎨 Colores de Nombre VIP Disponibles:</b>\r\r");
            sb.append("• <b>rainbow</b> (Efecto Multicolor Arcoíris)\r");
            sb.append("• <b>gold / dorado</b> (Dorado brillante)\r");
            sb.append("• <b>red / rojo</b> (Rojo pasión)\r");
            sb.append("• <b>blue / azul</b> (Azul zafiro)\r");
            sb.append("• <b>green / verde</b> (Verde esmeralda)\r");
            sb.append("• <b>purple / morado</b> (Púrpura real)\r");
            sb.append("• <b>pink / rosa</b> (Rosa chicle)\r");
            sb.append("• <b>cyan / celeste</b> (Cian eléctrico)\r");
            sb.append("• <b>orange / naranja</b> (Naranja intenso)\r");
            sb.append("• <b>lime / lima</b> (Verde lima)\r");
            sb.append("• <b>yellow / amarillo</b> (Amarillo solar)\r");
            sb.append("• <b>silver / plata</b> (Plateado elegante)\r\r");
            sb.append("Uso: <code>:namecolour [color]</code>\r");
            sb.append("Para restablecer: <code>:namecolour reset</code>");
            habbo.alert(sb.toString());
            return true;
        }

        String inputColor = params[1].toLowerCase();

        if (inputColor.equals("reset") || inputColor.equals("none") || inputColor.equals("normal") || inputColor.equals("off")) {
            habbo.getHabboStats().cache.remove(CACHE_KEY);
            saveColor(habbo.getHabboInfo().getId(), "");
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
                    if (color != null && !color.isEmpty()) {
                        if (event.habbo.getHabboStats().hasSubscription(Subscription.HABBO_CLUB) || event.habbo.getHabboInfo().getRank().getId() >= 2) {
                            event.habbo.getHabboStats().cache.put(CACHE_KEY, color);
                        } else {
                            event.habbo.getHabboStats().cache.remove(CACHE_KEY);
                        }
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
