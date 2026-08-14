package com.eu.habbo.messages.incoming.camera;

import com.eu.habbo.Emulator;
import com.eu.habbo.messages.incoming.MessageHandler;
import com.eu.habbo.messages.outgoing.camera.CameraRoomThumbnailSavedComposer;
import com.eu.habbo.networking.camera.CameraClient;
import com.eu.habbo.networking.camera.messages.outgoing.CameraRenderImageComposer;
import com.eu.habbo.util.crypto.ZIP;

public class CameraRoomThumbnailEvent extends MessageHandler {
    @Override
    public void handle() throws Exception {
        if (!this.client.getHabbo().hasPermission("acc_camera")) {
            this.client.getHabbo().alert(Emulator.getTexts().getValue("camera.permission"));
            return;
        }

        if (!this.client.getHabbo().getHabboInfo().getCurrentRoom().isOwner(this.client.getHabbo()))
            return;

        // Bypass CameraClient check for Nitro
        this.packet.getBuffer().readFloat();
        byte[] data = this.packet.getBuffer().readBytes(this.packet.getBuffer().readableBytes()).array();
        String content = new String(ZIP.inflate(data));

        int timestamp = Emulator.getIntUnixTimestamp();
        this.client.getHabbo().getHabboInfo().setPhotoJSON(Emulator.getConfig().getValue("camera.extradata").replace("%timestamp%", timestamp + ""));
        this.client.getHabbo().getHabboInfo().setPhotoTimestamp(timestamp);

        this.client.sendResponse(new CameraRoomThumbnailSavedComposer());
    }
}