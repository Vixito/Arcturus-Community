package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.outgoing.generic.alerts.MessagesForYouComposer;
import com.eu.habbo.plugin.HabboPlugin;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class PluginsCommand extends Command {
    public PluginsCommand() {
        super("cmd_plugins", Emulator.getTexts().getValue("commands.keys.cmd_plugins", "plugins;pl").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        Collection<HabboPlugin> plugins = Emulator.getPluginManager().getPlugins();
        List<String> messageList = new ArrayList<>();

        StringBuilder builder = new StringBuilder();
        builder.append("<div style=\"margin-bottom: 8px; font-weight: bold; font-size: 13px; color: #0284c7;\">");
        builder.append("🔌 Plugins Instalados (").append(plugins.size()).append("):");
        builder.append("</div>");

        builder.append("<table style=\"width: 100%; border-collapse: collapse;\">");

        for (HabboPlugin plugin : plugins) {
            String name = (plugin.configuration != null && plugin.configuration.name != null) ? plugin.configuration.name : "Plugin";
            String author = (plugin.configuration != null && plugin.configuration.author != null) ? plugin.configuration.author : "Desconocido";

            builder.append("<tr style=\"border-bottom: 1px solid rgba(0,0,0,0.12);\">");
            builder.append("<td style=\"padding: 6px 4px; vertical-align: middle;\">");
            builder.append("<div style=\"font-weight: bold; color: #0f172a; font-size: 12px;\">⚡ ").append(name).append("</div>");
            builder.append("<div style=\"font-size: 11px; color: #64748b; margin-top: 1px;\">Desarrollado por: <span style=\"color: #475569; font-style: italic;\">").append(author).append("</span></div>");
            builder.append("</td>");
            builder.append("</tr>");
        }

        builder.append("</table>");

        messageList.add(builder.toString());
        gameClient.sendResponse(new MessagesForYouComposer(messageList));
        return true;
    }
}
