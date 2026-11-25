package com.eu.habbo.habbohotel.items.interactions;

import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.items.Item;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomUnit;
import com.eu.habbo.messages.outgoing.generic.alerts.MessagesForYouComposer;
import gnu.trove.map.hash.THashMap;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class InteractionRoomAds extends InteractionCustomValues {
    public final static THashMap<String, String> defaultValues = new THashMap<String, String>() {
        {
            this.put("imageUrl", "");
        }

        {
            this.put("clickUrl", "");
        }

        {
            this.put("offsetX", "0");
        }

        {
            this.put("offsetY", "0");
        }

        {
            this.put("offsetZ", "0");
        }
    };

    public InteractionRoomAds(ResultSet set, Item baseItem) throws SQLException {
        super(set, baseItem, defaultValues);
    }

    public InteractionRoomAds(int id, int userId, Item item, String extradata, int limitedStack, int limitedSells) {
        super(id, userId, item, extradata, limitedStack, limitedSells, defaultValues);
    }

    @Override
    public boolean canWalkOn(RoomUnit roomUnit, Room room, Object[] objects) {
        return true;
    }

    @Override
    public boolean isWalkable() {
        return true;
    }

    @Override
    public void onClick(GameClient client, Room room, Object[] objects) throws Exception {
        if (client == null || client.getHabbo() == null) {
            return;
        }

        String clickUrl = this.values.get("clickUrl");
        if (clickUrl != null && clickUrl.startsWith("motd:")) {
            String key = clickUrl.substring("motd:".length()).trim().toLowerCase();
            List<String> msg = buildInstructionsMessage(key);
            if (!msg.isEmpty()) {
                client.sendResponse(new MessagesForYouComposer(msg));
                return;
            }
        }

        super.onClick(client, room, objects);
    }

    private List<String> buildInstructionsMessage(String key) {
        List<String> list = new ArrayList<>();
        if ("guitar_hero".equals(key)) {
            list.add("<b>GUITAR HERO 24/7 - Instrucciones Oficiales</b><div class=\"is-game-instructions\" style=\"display:none;\"></div>");

            list.add(buildSection("Como Entrar", "#0284c7", new String[][]{
                { "1. Haz fila", "Pasa por la puerta negra y avanza por los rollers negros hasta la casilla flecha." },
                { "2. Un jugador a la vez", "Solo 1 guitarrista puede estar en la cabina. Si esta libre, al pisar la casilla flecha entras automaticamente." },
                { "3. Si esta ocupada", "Espera en la casilla flecha y pulsa el interruptor de ENTRADA a tu lado en cuanto termine el turno actual." },
                { "4. Turno de 60 segundos", "Al entrar comienza tu ronda de 60 segundos con marcador y tabla de records en vivo." }
            }));

            list.add(buildSection("Como Jugar", "#16a34a", new String[][]{
                { "4 Carriles de Color", "Las notas luminosas (Verde, Rojo, Amarillo y Azul) bajan sin parar por los rollers hacia las 4 casillas de color." },
                { "Pulsa el Interruptor", "Haz doble clic en el interruptor frente a cada carril justo cuando su nota luminosa llegue a la casilla de color." },
                { "Acierto (+10 pts)", "Cada nota acertada suma +10 puntos reales, enciende los laseres del escenario, suena efecto rock y reinicia la nota." },
                { "Tabla de Records", "Al finalizar los 60 segundos tu puntuacion se guarda en Mejores Puntuaciones y la cabina queda libre para el siguiente." }
            }));

            list.add(buildSection("Salida y Reglas", "#d97706", new String[][]{
                { "Salir antes de tiempo", "Si quieres ceder tu turno antes de los 60s, haz doble clic en el interruptor derecho de SALIDA." },
                { "Sin bloqueos", "No te quedes AFK en la casilla flecha para no frenar la fila de rockeros." },
                { "Juego 24/7", "Las notas, luces y turnos funcionan automaticamente todo el dia sin necesidad de anfitrion." }
            }));
        }
        return list;
    }

    private String buildSection(String title, String accentColor, String[][] rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<div class=\"cmd-category-block\" data-category=\"").append(title).append("\" style=\"margin-bottom: 12px;\">");
        sb.append("<div class=\"cmd-cat-title\" style=\"margin-top: 8px; margin-bottom: 4px; font-weight: bold; color: ").append(accentColor).append("; background: rgba(0,0,0,0.06); padding: 4px 8px; border-radius: 4px;\">");
        sb.append(title);
        sb.append("</div>");
        sb.append("<table class=\"cmd-table\" style=\"width: 100%; border-collapse: collapse;\">");
        for (String[] r : rows) {
            sb.append("<tr class=\"cmd-row\" style=\"border-bottom: 1px solid rgba(0,0,0,0.12);\">");
            sb.append("<td style=\"width: 34%; padding: 5px 4px; vertical-align: middle; font-weight: bold; color: #0f172a;\">").append(r[0]).append("</td>");
            sb.append("<td style=\"width: 66%; padding: 5px 4px; vertical-align: middle; font-size: 11px; color: #334155;\">").append(r[1]).append("</td>");
            sb.append("</tr>");
        }
        sb.append("</table></div>");
        return sb.toString();
    }
}
