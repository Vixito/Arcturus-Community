package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class CommandsCommand extends Command {
    private static final Set<String> VIP_COMMAND_KEYS = new HashSet<>(Arrays.asList(
            "namecolour", "colorname", "namecolor", "colour", "color", "chatcolor",
            "prefix", "setprefix", "prefixo", "tag", "corprefixo", "prefixocor", "colorprefix", "prefixcolor"
    ));

    public CommandsCommand() {
        super("cmd_commands", Emulator.getTexts().getValue("commands.keys.cmd_commands", "commands;cmds;comandos").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        int userRank = gameClient.getHabbo().getHabboInfo().getRank().getId();
        List<Command> commands = Emulator.getGameEnvironment().getCommandHandler().getCommandsForRank(userRank);

        List<String> messageList = new ArrayList<>();
        messageList.add("<b>" + Emulator.getTexts().getValue("commands.generic.cmd_commands.text", "Lista de Comandos") + " (" + commands.size() + "):</b><div class=\"is-commands-list\" style=\"display:none;\"></div>");

        String searchFilter = params.length > 1 ? params[1].toLowerCase() : null;

        List<Command> userCmds = new ArrayList<>();
        List<Command> vipCmds = new ArrayList<>();
        TreeMap<Integer, List<Command>> staffCmds = new TreeMap<>();

        for (Command c : commands) {
            if (c.keys == null || c.keys.length == 0) continue;
            String primaryKey = c.keys[0].toLowerCase();

            if (VIP_COMMAND_KEYS.contains(primaryKey)) {
                vipCmds.add(c);
            } else {
                int minRank = 1;
                if (c.permission != null) {
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

                if (minRank <= 1) {
                    userCmds.add(c);
                } else {
                    staffCmds.putIfAbsent(minRank, new ArrayList<>());
                    staffCmds.get(minRank).add(c);
                }
            }
        }

        // 1. Categoría Usuario
        renderCategory(messageList, "Usuario", userCmds, searchFilter, "#0284c7");

        // 2. Categoría VIP
        renderCategory(messageList, "VIP", vipCmds, searchFilter, "#d97706");

        // 3. Categorías Staff clasificadas por cada rango correspondiente (solo nombre del rango)
        for (Map.Entry<Integer, List<Command>> entry : staffCmds.entrySet()) {
            com.eu.habbo.habbohotel.permissions.Rank rankObj = Emulator.getGameEnvironment().getPermissionsManager().getRank(entry.getKey());
            String rankName = (rankObj != null) ? rankObj.getName() : "Rango " + entry.getKey();
            renderCategory(messageList, rankName, entry.getValue(), searchFilter, "#7c3aed");
        }

        gameClient.sendResponse(new com.eu.habbo.messages.outgoing.generic.alerts.MessagesForYouComposer(messageList));
        return true;
    }

    private void renderCategory(List<String> messageList, String categoryName, List<Command> cmds, String searchFilter, String accentColor) {
        if (cmds == null || cmds.isEmpty()) return;

        StringBuilder categoryBuilder = new StringBuilder();
        int cmdCount = 0;

        for (Command c : cmds) {
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
                    else if (key.equals("setmax")) description = "Cambia el límite de usuarios de tu sala. (Requiere derechos)";
                    else if (key.equals("setspeed")) description = "Ajusta la velocidad de los rollers en tu sala. (Requiere derechos)";
                    else if (key.equals("hidewired")) description = "Oculta o muestra los wireds en tu sala. (Requiere derechos)";
                    else if (key.equals("wired") || key.equals("wf")) description = "Abre las herramientas de creación Wired (Wired Creator Tools). Uso: :wired";
                    else if (key.equals("reload") || key.equals("reload_room")) description = "Recarga la sala actual. (Requiere derechos)";
                    else if (key.equals("pickall")) description = "Recoge todos tus furnis en la sala. (Requiere ser dueño)";
                    else if (key.equals("ejectall")) description = "Expulsa los furnis de otros en tu sala. (Requiere ser dueño)";
                    else if (key.equals("diagonal")) description = "Activa o desactiva caminar en diagonal en la sala. (Requiere derechos)";
                    else if (key.equals("furni")) description = "Muestra la lista y cantidad de furnis colocados en la sala actual. Uso: :furni";
                    else if (key.equals("staffalert") || key.equals("sa")) description = "Envía un mensaje de alerta a todo el equipo staff en línea. Uso: :sa [mensaje]";
                    else if (key.equals("setstate") || key.equals("state")) description = "Fija el estado con el que se colocarán los furnis. Uso: :setstate [0-100] o :setstate";
                    else if (key.equals("setrotation") || key.equals("rot") || key.equals("setrot")) description = "Fija la rotación de los furnis al colocarlos. Uso: :setrotation [0-7] o :setrotation";
                    else if (key.equals("undo")) description = "Deshace la última acción de furni en la sala. Uso: :undo";
                    else if (key.equals("searchfurni") || key.equals("findfurni")) description = "Busca un furni en el catálogo haciendo doble clic sobre él. Uso: :searchfurni";
                    else if (key.equals("namecolour") || key.equals("colorname") || key.equals("colour") || key.equals("color")) description = "Personaliza el color de tu nombre de usuario. Uso: :namecolour [color / list / reset]";
                    else if (key.equals("moonwalk")) description = "Camina hacia atrás al estilo Michael Jackson. Uso: :moonwalk";
                    else if (key.equals("faceless")) description = "Oculta el rostro de tu avatar. Uso: :faceless";
                    else if (key.equals("push")) description = "Empuja a un usuario un paso adelante. Uso: :push [usuario]";
                    else if (key.equals("pull")) description = "Atrae a un usuario hacia tu posición. Uso: :pull [usuario]";
                    else if (key.equals("enable")) description = "Activa un efecto especial en tu personaje. Uso: :enable [id]";
                    else if (key.equals("chatcolor")) description = "Cambia el color o estilo de tu burbuja de chat. Uso: :chatcolor [id]";
                    else if (key.equals("test")) description = "Diagnóstico del emulador: sala, usuarios y memoria RAM. Uso: :test";
                    else if (key.equals("warp")) description = "Teletransporta a un usuario a tu posición. Uso: :warp [usuario]";
                    else if (key.equals("wordquiz")) description = "Inicia un quiz de preguntas en la sala. Uso: :wordquiz [pregunta]";
                    else description = "Ejecuta :" + key;
                } else {
                    description = "Sin descripción";
                }
            }

            description = description.replace("|", "/");
            cmdName = cmdName.replace("|", "/");

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
            categoryBlock.append("<div class=\"cmd-category-block\" data-category=\"").append(categoryName).append("\" style=\"margin-bottom: 12px;\">");
            categoryBlock.append("<div class=\"cmd-cat-title\" style=\"margin-top: 8px; margin-bottom: 4px; font-weight: bold; color: ").append(accentColor).append("; background: rgba(0,0,0,0.06); padding: 4px 8px; border-radius: 4px;\">");
            categoryBlock.append("Categoría: ").append(categoryName);
            categoryBlock.append("</div>");
            categoryBlock.append("<table class=\"cmd-table\" style=\"width: 100%; border-collapse: collapse;\">");
            categoryBlock.append(categoryBuilder.toString());
            categoryBlock.append("</table>");
            categoryBlock.append("</div>");
            messageList.add(categoryBlock.toString());
        }
    }
}
