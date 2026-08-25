package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.events.furniture.FurnitureToggleEvent;
import com.eu.habbo.plugin.events.users.UserExitRoomEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SearchFurniCommand extends Command implements EventListener {
    public static final String SEARCH_FURNI_KEY = "searchfurni_cmd";

    public SearchFurniCommand() {
        super("cmd_searchfurni", Emulator.getTexts().getValue("commands.keys.cmd_searchfurni", "searchfurni;findfurni").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo().getHabboStats().cache.containsKey(SEARCH_FURNI_KEY)) {
            gameClient.getHabbo().getHabboStats().cache.remove(SEARCH_FURNI_KEY);
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.texts.cmd_searchfurni.disabled", "Modo de búsqueda de furnis desactivado."), RoomChatMessageBubbles.ALERT);
        } else {
            gameClient.getHabbo().getHabboStats().cache.put(SEARCH_FURNI_KEY, true);
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.texts.cmd_searchfurni.1", "Haz doble clic en cualquier furni de la sala para buscarlo en el catálogo. Escribe :searchfurni de nuevo para desactivarlo."), RoomChatMessageBubbles.ALERT);
        }
        return true;
    }

    @EventHandler
    public static void onUserExitRoomEvent(UserExitRoomEvent event) {
        if (event.habbo != null) {
            event.habbo.getHabboStats().cache.remove(SEARCH_FURNI_KEY);
        }
    }

    @EventHandler
    public static void onFurnitureToggleEvent(FurnitureToggleEvent event) {
        if (event.habbo == null || event.furniture == null || event.furniture.getBaseItem() == null) return;

        if (event.habbo.getHabboStats().cache.containsKey(SEARCH_FURNI_KEY)) {
            event.setCancelled(true);
            int baseItemId = event.furniture.getBaseItem().getId();

            try (Connection connection = Emulator.getDatabase().getDataSource().getConnection();
                 PreparedStatement statement = connection.prepareStatement("SELECT cp.caption FROM catalog_items ci INNER JOIN catalog_pages cp ON ci.page_id = cp.id WHERE ci.item_ids = ? OR ci.item_ids LIKE ? LIMIT 3")) {
                statement.setInt(1, baseItemId);
                statement.setString(2, "%" + baseItemId + "%");
                try (ResultSet set = statement.executeQuery()) {
                    boolean found = false;
                    while (set.next()) {
                        found = true;
                        String pageName = set.getString("caption");
                        event.habbo.whisper(Emulator.getTexts().getValue("commands.texts.cmd_searchfurni.2", "🔎 Puedes encontrar este furni en la página del catálogo: \"%pagename%\"").replace("%pagename%", pageName), RoomChatMessageBubbles.ALERT);
                    }
                    if (!found) {
                        event.habbo.whisper("Este furni (" + event.furniture.getBaseItem().getName() + ") no se encuentra disponible actualmente en el catálogo.", RoomChatMessageBubbles.ALERT);
                    }
                }
            } catch (SQLException e) {
                Emulator.getLogging().logSQLException(e);
            }
        }
    }
}
