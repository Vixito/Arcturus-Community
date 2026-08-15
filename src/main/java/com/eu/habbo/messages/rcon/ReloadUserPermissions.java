package com.eu.habbo.messages.rcon;

import com.eu.habbo.Emulator;
import com.google.gson.Gson;

public class ReloadUserPermissions extends RCONMessage<ReloadUserPermissions.JSONReloadUserPermissions> {

    public ReloadUserPermissions() {
        super(JSONReloadUserPermissions.class);
    }

    @Override
    public void handle(Gson gson, JSONReloadUserPermissions object) {
        Emulator.getGameEnvironment().getPermissionsManager().reloadUserPermissions();
        this.message = "reloaded";
    }

    static class JSONReloadUserPermissions {
        public int user_id;
    }
}
