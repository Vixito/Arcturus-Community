package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;

public class WiredCommand extends Command {
    public WiredCommand() {
        super("cmd_wired", Emulator.getTexts().getValue("commands.keys.cmd_wired", "wired;wf").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        return true;
    }
}
