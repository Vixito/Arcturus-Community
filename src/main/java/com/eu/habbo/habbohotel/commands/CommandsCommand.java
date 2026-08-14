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
        StringBuilder message = new StringBuilder(Emulator.getTexts().getValue("commands.generic.cmd_commands.text"));
        List<Command> commands = Emulator.getGameEnvironment().getCommandHandler().getCommandsForRank(gameClient.getHabbo().getHabboInfo().getRank().getId());
        message.append("(").append(commands.size()).append("):\r\n\r\n<div class=\"is-commands-list\" style=\"display:none;\"></div>");

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
                        if (key.equals("kiss") || key.equals("beso")) description = "Besa a otro usuario.";
                        else if (key.equals("hug") || key.equals("abrazo")) description = "Abraza a otro usuario.";
                        else if (key.equals("test")) description = "Comando de prueba técnica.";
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
                message.append("<div class=\"cmd-category-block\" data-category=\"").append(rankName).append("\" style=\"margin-bottom: 12px;\">");
                message.append("<div class=\"cmd-cat-title\" style=\"margin-top: 8px; margin-bottom: 4px; font-weight: bold; color: #1e293b; background: rgba(0,0,0,0.06); padding: 4px 8px; border-radius: 4px;\">");
                message.append("Categoría: ").append(rankName);
                message.append("</div>");
                message.append("<table class=\"cmd-table\" style=\"width: 100%; border-collapse: collapse;\">");
                message.append(categoryBuilder.toString());
                message.append("</table>");
                message.append("</div>");
            }
        }

        gameClient.getHabbo().alert(new String[]{message.toString()});

        return true;
    }
}
