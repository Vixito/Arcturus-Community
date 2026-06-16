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
        message.append("(").append(commands.size()).append("):\r\n\r\n");

        java.util.TreeMap<Integer, java.util.List<Command>> categorized = new java.util.TreeMap<>();

        for (Command c : commands) {
            int minRank = 1;
            // Buscar el rango mínimo que tiene este permiso habilitado
            for (int r = 1; r <= 15; r++) {
                if (Emulator.getGameEnvironment().getPermissionsManager().rankExists(r)) {
                    com.eu.habbo.habbohotel.permissions.Permission p = Emulator.getGameEnvironment().getPermissionsManager().getRank(r).getPermissions().get(c.permission);
                    if (p != null && p.setting != com.eu.habbo.habbohotel.permissions.PermissionSetting.DISALLOWED) {
                        minRank = r;
                        break;
                    }
                }
            }
            categorized.putIfAbsent(minRank, new java.util.ArrayList<>());
            categorized.get(minRank).add(c);
        }

        for (java.util.Map.Entry<Integer, java.util.List<Command>> entry : categorized.entrySet()) {
            String rankName = Emulator.getGameEnvironment().getPermissionsManager().getRank(entry.getKey()).getName();
            message.append("--- Categoría: ").append(rankName).append(" ---\r\n");
            for (Command c : entry.getValue()) {
                String cmdName = ":" + c.keys[0];
                String description = Emulator.getTexts().getValue("commands.description." + c.permission, "Sin descripción");
                message.append(cmdName).append(" - ").append(description).append("\r\n");
            }
            message.append("\r\n");
        }

        gameClient.getHabbo().alert(new String[]{message.toString()});

        return true;
    }
}
