package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.modtool.ModToolBan;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.habbohotel.users.HabboInfo;
import com.eu.habbo.habbohotel.users.HabboManager;
import gnu.trove.iterator.TIntIntIterator;

import java.text.SimpleDateFormat;
import java.util.*;

public class UserInfoCommand extends Command {
    public UserInfoCommand() {
        super("cmd_userinfo", Emulator.getTexts().getValue("commands.keys.cmd_userinfo").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (params.length < 2) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_userinfo.forgot_username"), RoomChatMessageBubbles.ALERT);
            return true;
        }

        Habbo onlineHabbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(params[1]);
        HabboInfo habbo = (onlineHabbo != null ? onlineHabbo.getHabboInfo() : null);

        if (habbo == null) {
            habbo = HabboManager.getOfflineHabboInfo(params[1]);
        }

        if (habbo == null) {
            gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.error.cmd_userinfo.not_found").replace("%user%", params[1]), RoomChatMessageBubbles.ALERT);
            return true;
        }

        StringBuilder message = new StringBuilder("<b>" + Emulator.getTexts().getValue("command.cmd_userinfo.userinfo", "Información de Usuario") + ": " + habbo.getUsername() + " (" + habbo.getId() + ")</b>\r" +
                "<b>" + Emulator.getTexts().getValue("command.cmd_userinfo.user_id", "ID") + ":</b> " + habbo.getId() + "\r" +
                "<b>" + Emulator.getTexts().getValue("command.cmd_userinfo.user_name", "Nombre de usuario") + ":</b> " + habbo.getUsername() + "\r" +
                "<b>" + Emulator.getTexts().getValue("command.cmd_userinfo.motto", "Misión") + ":</b> " + habbo.getMotto().replace("<", "[").replace(">", "]") + "\r" +
                "<b>" + Emulator.getTexts().getValue("command.cmd_userinfo.rank", "Rango") + ":</b> " + habbo.getRank().getName() + " (" + habbo.getRank().getId() + ")\r" +
                "<b>" + Emulator.getTexts().getValue("command.cmd_userinfo.online", "En línea") + ":</b> " + (onlineHabbo == null ? Emulator.getTexts().getValue("generic.no", "No") : Emulator.getTexts().getValue("generic.yes", "Sí")) + "\r" +
                ((habbo.getRank().hasPermission(Permission.ACC_HIDE_MAIL, true)) ? "" : "<b>" + Emulator.getTexts().getValue("command.cmd_userinfo.email", "Correo") + ":</b> " + habbo.getMail() + "\r") +
                ((habbo.getRank().hasPermission(Permission.ACC_HIDE_IP, true)) ? "" : "<b>" + Emulator.getTexts().getValue("command.cmd_userinfo.ip_register", "IP de Registro") + ":</b> " + habbo.getIpRegister() + "\r") +
                ((habbo.getRank().hasPermission(Permission.ACC_HIDE_IP, true)) || onlineHabbo == null ? "" : "<b>" + Emulator.getTexts().getValue("command.cmd_userinfo.ip_current", "IP Actual") + ":</b> " + onlineHabbo.getHabboInfo().getIpLogin() + "\r") +
                (onlineHabbo != null ? "<b>" + Emulator.getTexts().getValue("command.cmd_userinfo.achievement_score", "Puntos de logro") + ":</b> " + onlineHabbo.getHabboStats().achievementScore + "\r" : ""));

        ModToolBan ban = Emulator.getGameEnvironment().getModToolManager().checkForBan(habbo.getId());

        message.append("<b>").append(Emulator.getTexts().getValue("command.cmd_userinfo.total_bans", "Total de baneos")).append(":</b> ").append(Emulator.getGameEnvironment().getModToolManager().totalBans(habbo.getId())).append("\r");
        message.append("<b>").append(Emulator.getTexts().getValue("command.cmd_userinfo.banned", "Baneado actualmente")).append(":</b> ").append(Emulator.getTexts().getValue(ban != null ? "generic.yes" : "generic.no", ban != null ? "Sí" : "No")).append("\r\r");
        if (ban != null) {
            message.append("<b>").append(Emulator.getTexts().getValue("command.cmd_userinfo.ban_info", "Detalles del baneo")).append("</b>\r");
            message.append(ban.listInfo()).append("\r");
        }

        message.append("<b>").append(Emulator.getTexts().getValue("command.cmd_userinfo.currencies", "Monedas y Economía")).append("</b>\r");
        message.append(Emulator.getTexts().getValue("command.cmd_userinfo.credits", "Créditos")).append(": ").append(habbo.getCredits()).append("\r");
        TIntIntIterator iterator = habbo.getCurrencies().iterator();

        for (int i = habbo.getCurrencies().size(); i-- > 0; ) {
            try {
                iterator.advance();
            } catch (Exception e) {
                break;
            }

            String curName = Emulator.getTexts().getValue("seasonal.name." + iterator.key());
            if (curName == null || curName.trim().isEmpty()) {
                if (iterator.key() == 0) curName = "Duckets";
                else if (iterator.key() == 5 || iterator.key() == 105) curName = "Diamantes";
                else curName = "Puntos (" + iterator.key() + ")";
            }

            message.append(curName).append(": ").append(iterator.value()).append("\r");
        }
        message.append("\r").append(onlineHabbo != null ? "<b>" + Emulator.getTexts().getValue("command.cmd_userinfo.current_activity", "Actividad Actual") + "</b>\r" : "").append(onlineHabbo != null ? Emulator.getTexts().getValue("command.cmd_userinfo.room", "Sala") + ": " + (onlineHabbo.getHabboInfo().getCurrentRoom() != null ? onlineHabbo.getHabboInfo().getCurrentRoom().getName() + " (ID: " + onlineHabbo.getHabboInfo().getCurrentRoom().getId() + ")\r" : "Fuera de sala\r") : "").append(onlineHabbo != null ? Emulator.getTexts().getValue("command.cmd_userinfo.respect_left", "Respetos restantes") + ": " + onlineHabbo.getHabboStats().respectPointsToGive + "\r" : "").append(onlineHabbo != null ? Emulator.getTexts().getValue("command.cmd_userinfo.pet_respect_left", "Caricias a mascotas restantes") + ": " + onlineHabbo.getHabboStats().petRespectPointsToGive + "\r" : "").append(onlineHabbo != null ? Emulator.getTexts().getValue("command.cmd_userinfo.allow_trade", "Permite intercambios") + ": " + ((onlineHabbo.getHabboStats().allowTrade()) ? Emulator.getTexts().getValue("generic.yes", "Sí") : Emulator.getTexts().getValue("generic.no", "No")) + "\r" : "").append(onlineHabbo != null ? Emulator.getTexts().getValue("command.cmd_userinfo.allow_follow", "Permite ser seguido") + ": " + ((onlineHabbo.getHabboStats().blockFollowing) ? Emulator.getTexts().getValue("generic.no", "No") : Emulator.getTexts().getValue("generic.yes", "Sí")) + "\r" : "").append(onlineHabbo != null ? Emulator.getTexts().getValue("command.cmd_userinfo.allow_friend_request", "Permite peticiones de amistad") + ": " + ((onlineHabbo.getHabboStats().blockFriendRequests) ? Emulator.getTexts().getValue("generic.no", "No") : Emulator.getTexts().getValue("generic.yes", "Sí")) + "\r" : "");

        SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        List<Map.Entry<Integer, String>> nameChanges = Emulator.getGameEnvironment().getHabboManager().getNameChanges(habbo.getId(), 3);
        if (!nameChanges.isEmpty()) {
            message.append("\r<b>Últimos cambios de nombre:</b><br/>");
            for (Map.Entry<Integer, String> entry : nameChanges) {
                message.append(format.format(new Date((long) entry.getKey() * 1000L))).append(" : ").append(entry.getValue()).append("<br/>");
            }
        }

        if (onlineHabbo != null) {
            message.append("\r" + "<b>Otras cuentas asociadas (");

            ArrayList<HabboInfo> users = Emulator.getGameEnvironment().getHabboManager().getCloneAccounts(onlineHabbo, 10);
            users.sort(new Comparator<HabboInfo>() {
                @Override
                public int compare(HabboInfo o1, HabboInfo o2) {
                    return o1.getId() - o2.getId();
                }
            });

            message.append(users.size()).append("):</b>\r");

            message.append("<b>Usuario, ID, Fecha de registro, Última conexión</b>\r");

            for (HabboInfo info : users) {
                message.append(info.getUsername()).append(", ").append(info.getId()).append(", ").append(format.format(new Date((long) info.getAccountCreated() * 1000L))).append(", ").append(format.format(new Date((long) info.getLastOnline() * 1000L))).append("\r");
            }
        }
        gameClient.getHabbo().alert(message.toString());

        return true;
    }
}
