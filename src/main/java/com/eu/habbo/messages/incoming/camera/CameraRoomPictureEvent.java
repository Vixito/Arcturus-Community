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
        
        // Mock timestamp and URL
        int timestamp = Emulator.getIntUnixTimestamp();
        int roomId = (this.client.getHabbo().getHabboInfo().getCurrentRoom() != null) ? this.client.getHabbo().getHabboInfo().getCurrentRoom().getId() : 0;
        String photoUrl = "photo_" + this.client.getHabbo().getHabboInfo().getId() + "_" + timestamp + ".png";
        
        this.client.getHabbo().getHabboInfo().setPhotoURL(photoUrl);
        this.client.getHabbo().getHabboInfo().setPhotoJSON(Emulator.getConfig().getValue("camera.extradata")
            .replace("%timestamp%", timestamp + "")
            .replace("%url%", photoUrl)
            .replace("%room_id%", roomId + ""));
        this.client.getHabbo().getHabboInfo().setPhotoTimestamp(timestamp);
        this.client.getHabbo().getHabboInfo().setPhotoRoomId(roomId);

        // Send URL back to the client
        this.client.sendResponse(new com.eu.habbo.messages.outgoing.camera.CameraURLComposer(photoUrl));

    }
}
