package com.eu.habbo.habbohotel.rooms;

import com.eu.habbo.Emulator;
import com.eu.habbo.habbohotel.messenger.MessengerBuddy;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.plugin.EventHandler;
import com.eu.habbo.plugin.EventListener;
import com.eu.habbo.plugin.events.users.UserShoutEvent;
import com.eu.habbo.plugin.events.users.UserTalkEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MentionHandler implements EventListener {
    private static final Pattern MENTION_PATTERN = Pattern.compile("@(\\w+)");

    @EventHandler
    public static void onUserTalkEvent(UserTalkEvent event) {
        if (event == null || event.chatMessage == null || event.habbo == null) return;
        handleMention(event.habbo, event.chatMessage.getMessage());
    }

    @EventHandler
    public static void onUserShoutEvent(UserShoutEvent event) {
        if (event == null || event.chatMessage == null || event.habbo == null) return;
        handleMention(event.habbo, event.chatMessage.getMessage());
    }

    private static void handleMention(Habbo sender, String text) {
        if (sender == null || text == null || !text.contains("@")) return;
        Room room = sender.getHabboInfo().getCurrentRoom();
        if (room == null) return;

        Matcher matcher = MENTION_PATTERN.matcher(text);
        while (matcher.find()) {
            String target = matcher.group(1).toLowerCase();

            if (target.equals("everyone") || target.equals("todos")) {
                if (sender.hasPermission("acc_mention_everyone") || sender.hasPermission("acc_supporttool")) {
                    for (Habbo h : room.getHabbos()) {
                        if (h != null && h.getId() != sender.getId()) {
                            h.whisper("📢 <b>" + sender.getHabboInfo().getUsername() + "</b> ha mencionado a todos en la sala: " + text, RoomChatMessageBubbles.ALERT);
                        }
                    }
                    sender.whisper("Has mencionado a todos los usuarios de la sala.", RoomChatMessageBubbles.ALERT);
                }
                break;
            } else if (target.equals("friends") || target.equals("amigos")) {
                if (sender.hasPermission("acc_mention_friends") || sender.hasPermission("acc_supporttool")) {
                    int notified = 0;
                    for (Habbo h : room.getHabbos()) {
                        if (h != null && h.getId() != sender.getId()) {
                            MessengerBuddy buddy = sender.getMessenger() != null ? sender.getMessenger().getFriend(h.getId()) : null;
                            if (buddy != null) {
                                h.whisper("📢 Tu amigo/a <b>" + sender.getHabboInfo().getUsername() + "</b> te ha mencionado en la sala: " + text, RoomChatMessageBubbles.ALERT);
                                notified++;
                            }
                        }
                    }
                    sender.whisper("Has mencionado a " + notified + " amigos en la sala.", RoomChatMessageBubbles.ALERT);
                }
                break;
            } else {
                if (sender.hasPermission("acc_mention") || sender.hasPermission("acc_supporttool") || sender.getHabboInfo().getRank().getId() >= 1) {
                    Habbo targetHabbo = room.getHabbo(target);
                    if (targetHabbo == null) {
                        targetHabbo = Emulator.getGameEnvironment().getHabboManager().getHabbo(target);
                    }
                    if (targetHabbo != null && targetHabbo.getId() != sender.getId()) {
                        targetHabbo.whisper("📢 <b>" + sender.getHabboInfo().getUsername() + "</b> te ha mencionado: " + text, RoomChatMessageBubbles.ALERT);
                    }
                }
            }
        }
    }
}
