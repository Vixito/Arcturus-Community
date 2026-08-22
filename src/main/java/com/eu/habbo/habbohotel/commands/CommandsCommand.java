package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;

import java.util.List;

public class CommandsCommand extends Command {
    public CommandsCommand() {
        super("cmd_commands", Emulator.getTexts().getValue("commands.keys.cmd_commands").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        List<Command> commands = Emulator.getGameEnvironment().getCommandHandler().getCommandsForRank(gameClient.getHabbo().getHabboInfo().getRank().getId());
        java.util.List<String> messageList = new java.util.ArrayList<>();
        messageList.add("<b>" + Emulator.getTexts().getValue("commands.generic.cmd_commands.text") + " (" + commands.size() + "):</b><div class=\"is-commands-list\" style=\"display:none;\"></div>");

        java.util.TreeMap<Integer, java.util.List<Command>> categorized = new java.util.TreeMap<>();

        for (Command c : commands) {
            int minRank = gameClient.getHabbo().getHabboInfo().getRank().getId();
            if (c.permission == null) {
                minRank = 1;
            } else {
                // Buscar el rango mínimo que tiene este permiso habilitado
                for (int r = 1; r <= 20; r++) {
                    if (Emulator.getGameEnvironment().getPermissionsManager().rankExists(r)) {
                        com.eu.habbo.habbohotel.permissions.Permission p = Emulator.getGameEnvironment().getPermissionsManager().getRank(r).getPermissions().get(c.permission);
                        if (p != null && p.setting != com.eu.habbo.habbohotel.permissions.PermissionSetting.DISALLOWED) {
                            minRank = r;
                            break;
                        }
                    }
                }
            }
            categorized.putIfAbsent(minRank, new java.util.ArrayList<>());
            categorized.get(minRank).add(c);
        }

        String searchFilter = params.length > 1 ? params[1].toLowerCase() : null;

        for (java.util.Map.Entry<Integer, java.util.List<Command>> entry : categorized.entrySet()) {
            com.eu.habbo.habbohotel.permissions.Rank rankObj = Emulator.getGameEnvironment().getPermissionsManager().getRank(entry.getKey());
            String rankName = (rankObj != null) ? rankObj.getName() : "Rango " + entry.getKey();
            
            StringBuilder categoryBuilder = new StringBuilder();
            int cmdCount = 0;
            
            for (Command c : entry.getValue()) {
                String cmdName = ":" + String.join(", :", c.keys);
                String descKey = (c.permission != null) ? "commands.description." + c.permission : (c.keys != null && c.keys.length > 0 ? "commands.description.cmd_" + c.keys[0] : "");
                String description = !descKey.isEmpty() ? Emulator.getTexts().getValue(descKey, "Sin descripción") : "Sin descripción";

                if (description == null || description.isEmpty() || description.equals("Sin descripción")) {
                    if (c.keys != null && c.keys.length > 0) {
                        String key = c.keys[0].toLowerCase();
                        if (key.equals("kiss")) description = "Besa a otro usuario cercano. Uso: :kiss [usuario]";
                        else if (key.equals("hug")) description = "Abraza a otro usuario cercano. Uso: :hug [usuario]";
                        else if (key.equals("slap")) description = "Le da una bofetada a un usuario cercano. Uso: :slap [usuario]";
                        else if (key.equals("kill")) description = "Derrota a un usuario y lo tumba al suelo. Uso: :kill [usuario]";
                        else if (key.equals("clap")) description = "Realiza una animación de aplausos. Uso: :clap";
                        else if (key.equals("setmax")) description = "Cambia el límite de usuarios de tu sala. (Requiere derechos) Uso: :setmax [número]";
                        else if (key.equals("setspeed")) description = "Ajusta la velocidad de los rollers en tu sala. (Requiere derechos) Uso: :setspeed [0-4]";
                        else if (key.equals("hidewired")) description = "Oculta o muestra los wireds en tu sala. (Requiere derechos)";
                        else if (key.equals("reload") || key.equals("reload_room")) description = "Recarga la sala actual. (Requiere derechos)";
                        else if (key.equals("pickall")) description = "Recoge todos tus furnis en la sala. (Requiere ser dueño)";
                        else if (key.equals("ejectall")) description = "Expulsa los furnis de otros en tu sala. (Requiere ser dueño)";
                        else if (key.equals("diagonal")) description = "Activa/desactiva caminar en diagonal en tu sala. (Requiere derechos)";
                        else if (key.equals("freeze_bots")) description = "Congela o descongela los bots de la sala. (Requiere derechos)";
                        else if (key.equals("test")) description = "Ejecuta un diagnóstico del emulador: sala, usuarios y memoria RAM. Uso: :test";
                        else if (key.equals("warp")) description = "Teletransporta a un usuario a tu posición.";
                        else if (key.equals("wordquiz")) description = "Inicia un quiz de preguntas en la sala.";
                        else description = "Ejecuta :" + key;
                    } else {
                        description = "Sin descripción";
                    }
                }
                
                if (searchFilter != null && !cmdName.toLowerCase().contains(searchFilter) && !description.toLowerCase().contains(searchFilter)) {
                    continue;
                }
                
                categoryBuilder.append("<tr class=\"cmd-row\" style=\"border-bottom: 1px solid rgba(0,0,0,0.12);\">");
                categoryBuilder.append("<td style=\"width: 38%; padding: 5px 4px; vertical-align: middle; font-weight: bold; color: #0f172a;\">").append(cmdName).append("</td>");
                categoryBuilder.append("<td style=\"width: 62%; padding: 5px 4px; vertical-align: middle; font-size: 11px; color: #334155;\">").append(description).append("</td>");
                categoryBuilder.append("</tr>");
                cmdCount++;
            }

            if (cmdCount > 0) {
                StringBuilder categoryBlock = new StringBuilder();
                categoryBlock.append("<div class=\"cmd-category-block\" data-category=\"").append(rankName).append("\" style=\"margin-bottom: 12px;\">");
                categoryBlock.append("<div class=\"cmd-cat-title\" style=\"margin-top: 8px; margin-bottom: 4px; font-weight: bold; color: #1e293b; background: rgba(0,0,0,0.06); padding: 4px 8px; border-radius: 4px;\">");
                categoryBlock.append("Categoría: ").append(rankName);
                categoryBlock.append("</div>");
                categoryBlock.append("<table class=\"cmd-table\" style=\"width: 100%; border-collapse: collapse;\">");
                categoryBlock.append(categoryBuilder.toString());
                categoryBlock.append("</table>");
                categoryBlock.append("</div>");
                messageList.add(categoryBlock.toString());
            }
        }

        gameClient.sendResponse(new com.eu.habbo.messages.outgoing.generic.alerts.MessagesForYouComposer(messageList));

        return true;
    }
}
