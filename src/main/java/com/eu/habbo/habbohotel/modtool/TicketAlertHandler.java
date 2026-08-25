package com.eu.habbo.habbohotel.modtool;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.events.support.SupportTicketEvent;

public class TicketAlertHandler implements EventListener {

    @EventHandler
    public static void onSupportTicketEvent(SupportTicketEvent event) throws Exception {
        if (event == null || event.ticket == null) return;

        String message = Emulator.getTexts().getValue("ticketalert.message", "⚠️ <b>Nuevo Ticket de Ayuda:</b> El usuario <b>%username%</b> necesita asistencia. Abre el ModTool para revisarlo.").replace("%username%", event.ticket.senderUsername);

        for (Habbo habbo : Emulator.getGameEnvironment().getHabboManager().getOnlineHabbos().values()) {
            if (habbo != null && (habbo.hasPermission("acc_ticket_alert") || habbo.hasPermission("acc_supporttool")) && habbo.getHabboInfo().getCurrentRoom() != null) {
                habbo.whisper(message, RoomChatMessageBubbles.ALERT);
            }
        }
    }
}
