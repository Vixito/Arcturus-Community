package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;

public class TestCommand extends Command {
    public TestCommand() {
        super(null, new String[]{"test"});
    }

    @Override
    public boolean handle(GameClient gameClient, String[] params) throws Exception {
        if (gameClient.getHabbo() == null) return false;

        long freeMem = Emulator.getRuntime().freeMemory() / (1024 * 1024);
        long totalMem = Emulator.getRuntime().totalMemory() / (1024 * 1024);
        int onlineCount = Emulator.getGameEnvironment().getHabboManager().getOnlineCount();
        String roomInfo = (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null) 
            ? "Sala #" + gameClient.getHabbo().getHabboInfo().getCurrentRoom().getId() + " (" + gameClient.getHabbo().getRoomUnit().getX() + ", " + gameClient.getHabbo().getRoomUnit().getY() + ", " + gameClient.getHabbo().getRoomUnit().getZ() + ")"
            : "Fuera de sala";

        String testReport = "🧪 [TEST OK] Emulador Habbten funcionando correctamente.\n"
            + "• Usuario: " + gameClient.getHabbo().getHabboInfo().getUsername() + " (Rango " + gameClient.getHabbo().getHabboInfo().getRank().getId() + ")\n"
            + "• Ubicación: " + roomInfo + "\n"
            + "• Usuarios online: " + onlineCount + "\n"
            + "• Memoria: " + (totalMem - freeMem) + "MB / " + totalMem + "MB RAM";

        gameClient.getHabbo().whisper(testReport, RoomChatMessageBubbles.ALERT);
        return true;
    }
}
