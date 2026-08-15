package com.eu.habbo.messages.outgoing.gamecenter.basejump;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class BaseJumpLoadGameURLComposer extends MessageComposer {
    private final int gameId;
    private final String url;

    public BaseJumpLoadGameURLComposer() {
        this.gameId = 3;
        this.url = "/game/games/basejump/index.html";
    }

    public BaseJumpLoadGameURLComposer(int gameId, String url) {
        this.gameId = gameId;
        this.url = url;
    }

    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.BaseJumpLoadGameURLComposer);
        this.response.appendInt(this.gameId);
        this.response.appendString(String.valueOf(System.currentTimeMillis()));
        this.response.appendString(this.url);
        return this.response;
    }
}