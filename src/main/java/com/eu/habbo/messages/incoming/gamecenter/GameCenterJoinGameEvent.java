package com.eu.habbo.messages.incoming.gamecenter;

import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.gamecenter.GameCenterAchievementsConfigurationComposer;
import com.eu.habbo.messages.outgoing.gamecenter.SnowStormLoadGameURLComposer;
import com.eu.habbo.messages.outgoing.gamecenter.basejump.BaseJumpJoinQueueComposer;
import com.eu.habbo.messages.outgoing.gamecenter.basejump.BaseJumpLoadGameURLComposer;

public class GameCenterJoinGameEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        int gameId = this.packet.readInt();

        if (gameId == 0) // SnowStorm
        {
            this.client.sendResponse(new SnowStormLoadGameURLComposer());
        } else if (gameId == 3) // Fast Food (BaseJump)
        {
            this.client.sendResponse(new GameCenterAchievementsConfigurationComposer());
            this.client.sendResponse(new BaseJumpLoadGameURLComposer(3, "/game/games/basejump/index.html"));
        } else if (gameId == 4) // SlotCar
        {
            this.client.sendResponse(new BaseJumpJoinQueueComposer(gameId));
            this.client.sendResponse(new BaseJumpLoadGameURLComposer(4, "/game/games/slotcar/index.html"));
        } else if (gameId == 5) // Battle Ball
        {
            this.client.sendResponse(new BaseJumpJoinQueueComposer(gameId));
            this.client.sendResponse(new BaseJumpLoadGameURLComposer(5, "/game/games/battleball/index.html"));
        }
    }
}