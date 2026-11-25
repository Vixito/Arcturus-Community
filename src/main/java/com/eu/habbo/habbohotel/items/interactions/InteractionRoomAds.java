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
            StringBuilder html = new StringBuilder();
            html.append("<div class=\"is-game-instructions\" data-title=\"¡Guitar Hero 24/7!\" style=\"display:none;\"></div>");

            // Intro block with circular rock emblem floating on the right + curved text wrap (shape-outside: circle)
            html.append("<div style=\"font-size:12px; line-height:1.45; color:#111827; padding:2px 2px 10px 2px; overflow:hidden; border-bottom:1px solid rgba(0,0,0,0.14);\">");
            html.append("<img src=\"/img/guitar_hero_circle.png\" alt=\"Guitar Hero 24/7\" style=\"float:right; width:102px; height:102px; border-radius:50%; shape-outside:circle(50%); clip-path:circle(50%); margin:2px 0 6px 12px; border:2px solid #1e293b;\" />");
            html.append("¡Bienvenido/a a <b>Guitar Hero 24/7</b>! Tu objetivo es simple: toca cada nota de color al ritmo de la pista y sobrevive la canción completa para registrar la mayor puntuación en la tabla.");
            html.append("</div>");

            // Section 1: Cómo jugar (orange Habbo Info Terminal heading)
            html.append("<div style=\"font-size:12px; line-height:1.45; color:#111827; padding:10px 2px; border-bottom:1px solid rgba(0,0,0,0.14);\">");
            html.append("<div style=\"font-weight:bold; color:#d97706; font-size:12.5px; margin-bottom:6px;\">Cómo jugar:</div>");
            html.append("<div style=\"margin-bottom:5px;\">- Haz fila en los rollers negros pasando por la puerta negra hasta llegar a la casilla flecha (solo juega <b>1 persona a la vez</b>).</div>");
            html.append("<div style=\"margin-bottom:5px;\">- Al entrar a la cabina comenzará una canción de <b>2 minutos</b>. Las notas luminosas (Verde, Rojo, Amarillo y Azul) bajarán por los 4 carriles.</div>");
            html.append("<div style=\"margin-bottom:5px;\">- Haz doble clic en el interruptor del carril justo cuando la nota llegue a su casilla de color para sumar <b>+10 puntos</b> y encender los láseres del escenario.</div>");
            html.append("<div>- No necesitas salir manualmente: si sobrevives la canción entera, <b>¡ganas!</b> y el juego te sacará al terminar.</div>");
            html.append("</div>");

            // Section 2: Límites y Claves (orange heading + floating pixel heart on the right!)
            html.append("<div style=\"font-size:12px; line-height:1.45; color:#111827; padding:10px 2px 4px 2px; overflow:hidden;\">");
            html.append("<div style=\"font-weight:bold; color:#d97706; font-size:12.5px; margin-bottom:6px;\">Límites y Claves:</div>");
            html.append("<img src=\"/img/guitar_hero_heart.png\" alt=\"Vidas\" style=\"float:right; width:56px; height:52px; image-rendering:pixelated; shape-outside:circle(50%); margin:2px 4px 6px 10px;\" />");
            html.append("<div style=\"margin-bottom:4px;\">- <b>3 vidas:</b> fallar una nota o pulsar un interruptor a destiempo resta 1 vida; al llegar a 3 fallos el juego te expulsa y reinicia el tiempo.</div>");
            html.append("<div style=\"margin-bottom:4px;\">- <b>Duración:</b> 2 min (120 s). Si sobrevives la canción entera, <b>ganas</b> y tus puntos se registran en el ranking.</div>");
            html.append("<div style=\"margin-bottom:4px;\">- <b>Clave Espectadores:</b> escribe <b>!espectador</b> para ir al palco VIP.</div>");
            html.append("<div style=\"margin-bottom:8px;\">- <b>Volver a la fila:</b> escribe <b>!fila</b> para regresar a la cola.</div>");
            html.append("<div style=\"color:#334155;\">¡Echa un vistazo a la tabla de <b>Mejores Puntuaciones</b> siempre abierta en la esquina derecha para ver quién lidera el ranking!</div>");
            html.append("</div>");

            list.add(html.toString());
        }
        return list;
    }
}
