package com.eu.habbo.messages.incoming.camera;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.networking.camera.CameraClient;
import com.eu.habbo.networking.camera.messages.outgoing.CameraRenderImageComposer;
import com.eu.habbo.util.crypto.ZIP;

public class CameraRoomPictureEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission("acc_camera")) {
            this.client.getHabbo().alert(Emulator.getTexts().getValue("camera.permission"));
            return;
        }

        // Bypass CameraClient check for Nitro
        this.packet.getBuffer().readFloat();
        byte[] data = this.packet.getBuffer().readBytes(this.packet.getBuffer().readableBytes()).array();
        String content = new String(ZIP.inflate(data));
        
        // Mock timestamp
        int timestamp = Emulator.getIntUnixTimestamp();
        this.client.getHabbo().getHabboInfo().setPhotoJSON(Emulator.getConfig().getValue("camera.extradata").replace("%timestamp%", timestamp + ""));
        this.client.getHabbo().getHabboInfo().setPhotoTimestamp(timestamp);

        if (this.client.getHabbo().getHabboInfo().getCurrentRoom() != null) {
            this.client.getHabbo().getHabboInfo().setPhotoRoomId(this.client.getHabbo().getHabboInfo().getCurrentRoom().getId());
        }

        // Send a dummy URL back to the client to satisfy it (Nitro handles the real upload)
        this.client.sendResponse(new com.eu.habbo.messages.outgoing.camera.CameraURLComposer(""));

    }
}
