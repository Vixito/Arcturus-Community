package com.eu.habbo.messages.outgoing.gamecenter;

import com.eu.habbo.messages.ServerMessage;
import com.eu.habbo.messages.outgoing.MessageComposer;
import com.eu.habbo.messages.outgoing.Outgoing;

public class SnowStormLoadGameURLComposer extends MessageComposer {
    @Override
    protected ServerMessage composeInternal() {
        this.response.init(Outgoing.BaseJumpLoadGameURLComposer);
        this.response.appendInt(0);
        this.response.appendString("snowstorm_version_1");
        this.response.appendString("/games/snowstorm/index.html");
        return this.response;
    }
}
