package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.messages.outgoing.habboway.nux.NuxAlertComposer;

public class FloorPlanCommand extends Command {
    public FloorPlanCommand() {
        super("cmd_floor", Emulator.getTexts().getValue("commands.keys.cmd_floor", "floor;floorplan;plan;builder").split(";"));
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() == null) return false;

        if (!gameClient.getHabbo().getHabboInfo().getCurrentRoom().isOwner(gameClient.getHabbo()) && !gameClient.getHabbo().hasPermission("acc_anyroomowner")) {
            return false;
        }

        gameClient.sendResponse(new NuxAlertComposer("floor-editor/toggle"));
        return true;
    }
}
