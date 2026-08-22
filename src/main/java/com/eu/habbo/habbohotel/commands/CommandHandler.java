package com.eu.habbo.habbohotel.commands;

import com.eu.habbo.Emulator;
import com.eu.habbo.core.CommandLog;
import com.eu.habbo.habbohotel.gameclients.GameClient;
import com.eu.habbo.habbohotel.permissions.Permission;
import com.eu.habbo.habbohotel.permissions.PermissionSetting;
import com.eu.habbo.habbohotel.pets.Pet;
import com.eu.habbo.habbohotel.pets.PetCommand;
import com.eu.habbo.habbohotel.pets.PetVocalsType;
import com.eu.habbo.habbohotel.pets.RideablePet;
import com.eu.habbo.habbohotel.rooms.Room;
import com.eu.habbo.habbohotel.rooms.RoomChatMessageBubbles;
import com.eu.habbo.habbohotel.rooms.RoomRightLevels;
import com.eu.habbo.habbohotel.users.Habbo;
import com.eu.habbo.messages.outgoing.rooms.users.RoomUserTypingComposer;
import com.eu.habbo.plugin.events.users.UserCommandEvent;
import com.eu.habbo.plugin.events.users.UserExecuteCommandEvent;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.map.hash.THashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

public class CommandHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandHandler.class);

    private final static THashMap<String, Command> commands = new THashMap<>(5);
    private static final Comparator<Command> ALPHABETICAL_ORDER = new Comparator<Command>() {
        public int compare(Command c1, Command c2) {
            String p1 = (c1 != null && c1.permission != null) ? c1.permission : (c1 != null && c1.keys != null && c1.keys.length > 0 ? c1.keys[0] : "");
            String p2 = (c2 != null && c2.permission != null) ? c2.permission : (c2 != null && c2.keys != null && c2.keys.length > 0 ? c2.keys[0] : "");
            int res = String.CASE_INSENSITIVE_ORDER.compare(p1, p2);
            return (res != 0) ? res : p1.compareTo(p2);
        }
    };

    public CommandHandler() {
        long millis = System.currentTimeMillis();
        this.reloadCommands();
        LOGGER.info("Command Handler -> Loaded! (" + (System.currentTimeMillis() - millis) + " MS)");
    }

    public static void addCommand(Command command) {
        if (command == null)
            return;

        commands.put(command.getClass().getName(), command);
    }


    public static void addCommand(Class<? extends Command> command) {
        try {
            //command.getConstructor().setAccessible(true);
            addCommand(command.newInstance());
            LOGGER.debug("Added command: {}", command.getName());
        } catch (Exception e) {
            LOGGER.error("Caught exception", e);
        }
    }


    public static boolean handleCommand(GameClient gameClient, String commandLine) {
        if (gameClient != null) {
            if (commandLine.startsWith(":")) {
                commandLine = commandLine.replaceFirst(":", "");

                String[] parts = commandLine.split(" ");

                if (parts.length >= 1) {
                    for (Command command : commands.values()) {
                        for (String s : command.keys) {
                            if (s.toLowerCase().equals(parts[0].toLowerCase())) {
                                boolean succes = false;
                                if (command.permission == null || gameClient.getHabbo().hasPermission(command.permission, gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null && (gameClient.getHabbo().getHabboInfo().getCurrentRoom().hasRights(gameClient.getHabbo())) || gameClient.getHabbo().hasPermission(Permission.ACC_PLACEFURNI) || (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null && gameClient.getHabbo().getHabboInfo().getCurrentRoom().getGuildId() > 0 && gameClient.getHabbo().getHabboInfo().getCurrentRoom().getGuildRightLevel(gameClient.getHabbo()).isEqualOrGreaterThan(RoomRightLevels.GUILD_RIGHTS)))) {
                                    try {
                                        UserExecuteCommandEvent userExecuteCommandEvent = new UserExecuteCommandEvent(gameClient.getHabbo(), command, parts);
                                        Emulator.getPluginManager().fireEvent(userExecuteCommandEvent);

                                        if(userExecuteCommandEvent.isCancelled()) {
                                            return userExecuteCommandEvent.isSuccess();
                                        }

                                        String[] eventParts = parts.clone();
                                        if (command instanceof KissCommand) eventParts[0] = "kiss";
                                        else if (command instanceof HugCommand) eventParts[0] = "hug";
                                        else if (command instanceof SlapCommand) eventParts[0] = "slap";
                                        else if (command instanceof KillCommand) eventParts[0] = "kill";

                                        UserCommandEvent event = new UserCommandEvent(gameClient.getHabbo(), eventParts, command.handle(gameClient, parts));
                                        Emulator.getPluginManager().fireEvent(event);

                                        succes = event.succes;
                                        if (succes) {
                                            if (command instanceof KissCommand) {
                                                Emulator.getGameEnvironment().getBattlePassManager().progress(gameClient.getHabbo(), "habbo_command_kiss");
                                            } else if (command instanceof HugCommand) {
                                                Emulator.getGameEnvironment().getBattlePassManager().progress(gameClient.getHabbo(), "habbo_command_hug");
                                            } else if (command instanceof SlapCommand) {
                                                Emulator.getGameEnvironment().getBattlePassManager().progress(gameClient.getHabbo(), "habbo_command_slap");
                                            } else if (command instanceof KillCommand) {
                                                Emulator.getGameEnvironment().getBattlePassManager().progress(gameClient.getHabbo(), "habbo_command_kill");
                                            } else if (command instanceof EmptyInventoryCommand) {
                                                Emulator.getGameEnvironment().getBattlePassManager().progress(gameClient.getHabbo(), "habbo_command_empty");
                                            }
                                        }
                                    } catch (Exception e) {
                                        LOGGER.error("Caught exception", e);
                                    }

                                    if (gameClient.getHabbo().getHabboInfo().getRank().isLogCommands()) {
                                        Emulator.getDatabaseLogger().store(new CommandLog(gameClient.getHabbo().getHabboInfo().getId(), command, commandLine, succes));
                                    }
                                } else {
                                    gameClient.getHabbo().whisper(Emulator.getTexts().getValue("commands.generic.no_permission", "No tienes permisos suficientes para usar este comando."), RoomChatMessageBubbles.ALERT);
                                    return true;
                                }

                                return succes;
                            }
                        }
                    }
                }
            } else {
                String[] args = commandLine.split(" ");

                if (args.length <= 1)
                    return false;

                if (gameClient.getHabbo().getHabboInfo().getCurrentRoom() != null) {
                    Room room = gameClient.getHabbo().getHabboInfo().getCurrentRoom();

                    if (room.getCurrentPets().isEmpty())
                        return false;

                    TIntObjectIterator<Pet> petIterator = room.getCurrentPets().iterator();

                    for (int j = room.getCurrentPets().size(); j-- > 0; ) {
                        try {
                            petIterator.advance();
                        } catch (NoSuchElementException e) {
                            break;
                        }

                        Pet pet = petIterator.value();

                        if (pet != null && pet.getPetData() != null && pet.getPetData().getPetCommands() != null) {
                            if (pet.getName().equalsIgnoreCase(args[0])) {
                                StringBuilder s = new StringBuilder();

                                for (int i = 1; i < args.length; i++) {
                                    s.append(args[i]).append(" ");
                                }

                                String inputKey = s.substring(0, s.length() - 1).trim();
                                String normalizedInput = java.text.Normalizer.normalize(inputKey, java.text.Normalizer.Form.NFD)
                                        .replaceAll("\\p{M}", "")
                                        .replaceAll("[^a-zA-Z0-9 ]", "")
                                        .trim();

                                for (PetCommand command : pet.getPetData().getPetCommands()) {
                                    if (command == null) continue;

                                    if (matchesPetCommand(command, inputKey, normalizedInput)) {
                                        if (pet instanceof RideablePet && ((RideablePet) pet).getRider() != null) {
                                            if (((RideablePet) pet).getRider().getHabboInfo().getId() == gameClient.getHabbo().getHabboInfo().getId()) {
                                                ((RideablePet) pet).getRider().getHabboInfo().dismountPet();
                                            }
                                            break;
                                        }

                                        final Pet targetPet = pet;
                                        final PetCommand targetCommand = command;
                                        final Habbo targetHabbo = gameClient.getHabbo();
                                        final String[] targetArgs = args;

                                        Emulator.getThreading().run(() -> {
                                            if (targetCommand.level <= targetPet.getLevel())
                                                targetPet.handleCommand(targetCommand, targetHabbo, targetArgs);
                                            else
                                                targetPet.say(targetPet.getPetData().randomVocal(PetVocalsType.UNKNOWN_COMMAND));
                                        }, 350);

                                        break;
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        return false;
    }

    public static Command getCommand(String key) {
        for (Command command : commands.values()) {
            for (String k : command.keys) {
                if (key.equalsIgnoreCase(k)) {
                    return command;
                }
            }
        }

        return null;
    }

    public void reloadCommands() {
        addCommand(new AboutCommand());
        addCommand(new AlertCommand());
        addCommand(new AllowTradingCommand());
        addCommand(new ArcturusCommand());
        addCommand(new BadgeCommand());
        addCommand(new BanCommand());
        addCommand(new BlockAlertCommand());
        addCommand(new BotsCommand());
        addCommand(new CalendarCommand());
        addCommand(new ChatTypeCommand());
        addCommand(new CommandsCommand());
        addCommand(new ControlCommand());
        addCommand(new CoordsCommand());
        addCommand(new CreditsCommand());
        addCommand(new DiagonalCommand());
        addCommand(new DisconnectCommand());
        addCommand(new EjectAllCommand());
        addCommand(new EmptyInventoryCommand());
        addCommand(new EmptyBotsInventoryCommand());
        addCommand(new EmptyPetsInventoryCommand());
        addCommand(new EnableCommand());
        addCommand(new DanceCommand());
        addCommand(new EventCommand());
        addCommand(new FacelessCommand());
        addCommand(new FastwalkCommand());
        addCommand(new FilterWordCommand());
        addCommand(new FloorPlanCommand());
        addCommand(new FreezeBotsCommand());
        addCommand(new FreezeCommand());
        addCommand(new GiftCommand());
        addCommand(new GiveRankCommand());
        addCommand(new HabnamCommand());
        addCommand(new HandItemCommand());
        addCommand(new HappyHourCommand());
        addCommand(new HideWiredCommand());
        addCommand(new HotelAlertCommand());
        addCommand(new HotelAlertLinkCommand());
        addCommand(new InvisibleCommand());
        addCommand(new IPBanCommand());
        addCommand(new LayCommand());
        addCommand(new MachineBanCommand());
        addCommand(new MassBadgeCommand());
        addCommand(new RoomBadgeCommand());
        addCommand(new MassCreditsCommand());
        addCommand(new MassGiftCommand());
        addCommand(new MassPixelsCommand());
        addCommand(new MassPointsCommand());
        addCommand(new MimicCommand());
        addCommand(new MoonwalkCommand());
        addCommand(new MultiCommand());
        addCommand(new MuteBotsCommand());
        addCommand(new MuteCommand());
        addCommand(new MutePetsCommand());
        addCommand(new PetInfoCommand());
        addCommand(new PickallCommand());
        addCommand(new PixelCommand());
        addCommand(new PluginsCommand());
        addCommand(new PointsCommand());
        addCommand(new PromoteTargetOfferCommand());
        addCommand(new PullCommand());
        addCommand(new PushCommand());
        addCommand(new RedeemCommand());
        addCommand(new ReloadRoomCommand());
        addCommand(new RoomAlertCommand());
        addCommand(new RoomBundleCommand());
        addCommand(new RoomCreditsCommand());
        addCommand(new RoomDanceCommand());
        addCommand(new RoomEffectCommand());
        addCommand(new RoomItemCommand());
        addCommand(new RoomKickCommand());
        addCommand(new RoomMuteCommand());
        addCommand(new RoomPixelsCommand());
        addCommand(new RoomPointsCommand());
        addCommand(new SayAllCommand());
        addCommand(new SayCommand());
        addCommand(new SetMaxCommand());
        addCommand(new SetPollCommand());
        addCommand(new SetSpeedCommand());
        addCommand(new ShoutAllCommand());
        addCommand(new ShoutCommand());
        addCommand(new ShutdownCommand());
        addCommand(new SitCommand());
        addCommand(new StandCommand());
        addCommand(new SitDownCommand());
        addCommand(new StaffAlertCommand());
        addCommand(new StaffOnlineCommand());
        addCommand(new StalkCommand());
        addCommand(new SummonCommand());
        addCommand(new SummonRankCommand());
        addCommand(new SuperbanCommand());
        addCommand(new SuperPullCommand());
        addCommand(new TakeBadgeCommand());
        addCommand(new TeleportCommand());
        addCommand(new TransformCommand());
        addCommand(new TrashCommand());
        addCommand(new UnbanCommand());
        addCommand(new UnloadRoomCommand());
        addCommand(new UnmuteCommand());
        addCommand(new UpdateAchievements());
        addCommand(new UpdateBotsCommand());
        addCommand(new UpdateCalendarCommand());
        addCommand(new UpdateCatalogCommand());
        addCommand(new UpdateConfigCommand());
        addCommand(new UpdateGuildPartsCommand());
        addCommand(new UpdateHotelViewCommand());
        addCommand(new UpdateItemsCommand());
        addCommand(new UpdateNavigatorCommand());
        addCommand(new UpdatePermissionsCommand());
        addCommand(new UpdatePetDataCommand());
        addCommand(new UpdatePluginsCommand());
        addCommand(new UpdatePollsCommand());
        addCommand(new UpdateTextsCommand());
        addCommand(new UpdateWordFilterCommand());
        addCommand(new UserInfoCommand());
        addCommand(new WordQuizCommand());
        addCommand(new UpdateYoutubePlaylistsCommand());
        addCommand(new AddYoutubePlaylistCommand());
        addCommand(new SoftKickCommand());
        addCommand(new SubscriptionCommand());
        addCommand(new KissCommand());
        addCommand(new HugCommand());
        addCommand(new SlapCommand());
        addCommand(new KillCommand());
        addCommand(new ClapCommand());

        addCommand(new TestCommand());
    }

    public List<Command> getCommandsForRank(int rankId) {
        List<Command> allowedCommands = new ArrayList<>();
        if (Emulator.getGameEnvironment().getPermissionsManager().rankExists(rankId)) {
            THashMap<String, Permission> permissions = Emulator.getGameEnvironment().getPermissionsManager().getRank(rankId).getPermissions();

            for (Command command : commands.values()) {
                if (allowedCommands.contains(command))
                    continue;

                if (command.permission == null || (permissions.containsKey(command.permission) && permissions.get(command.permission).setting != PermissionSetting.DISALLOWED)) {
                    allowedCommands.add(command);
                }
            }
        }

        allowedCommands.sort(CommandHandler.ALPHABETICAL_ORDER);

        return allowedCommands;
    }

    public static boolean matchesPetCommand(PetCommand command, String inputKey, String normalizedInput) {
        if (command == null) return false;

        if (command.key != null) {
            String cmdKey = command.key;
            String normalizedCmd = java.text.Normalizer.normalize(cmdKey, java.text.Normalizer.Form.NFD)
                    .replaceAll("\\p{M}", "")
                    .replaceAll("[^a-zA-Z0-9 ]", "")
                    .trim();

            if (cmdKey.equalsIgnoreCase(inputKey) || normalizedCmd.equalsIgnoreCase(normalizedInput)) {
                return true;
            }
        }

        String nInput = normalizedInput.toLowerCase();
        switch (command.id) {
            case 0:
                return nInput.equals("descansa") || nInput.equals("free") || nInput.equals("relax");
            case 1:
                return nInput.equals("sientate") || nInput.equals("sientese") || nInput.equals("sit");
            case 2:
                return nInput.equals("tumbate") || nInput.equals("acuestate") || nInput.equals("lay") || nInput.equals("down");
            case 3:
                return nInput.equals("ven aqui") || nInput.equals("aqui") || nInput.equals("here") || nInput.equals("come");
            case 4:
                return nInput.equals("pide") || nInput.equals("beg");
            case 5:
                return nInput.equals("haz el muerto") || nInput.equals("muerto") || nInput.equals("dead") || nInput.equals("play dead");
            case 6:
                return nInput.equals("quieto") || nInput.equals("stay") || nInput.equals("stop");
            case 7:
                return nInput.equals("sigueme") || nInput.equals("follow") || nInput.equals("follow me");
            case 8:
                return nInput.equals("de pie") || nInput.equals("levanta") || nInput.equals("levantate") || nInput.equals("stand") || nInput.equals("up");
            case 9:
                return nInput.equals("salta") || nInput.equals("jump");
            case 10:
                return nInput.equals("habla") || nInput.equals("ladra") || nInput.equals("speak") || nInput.equals("talk");
            case 11:
                return nInput.equals("juega") || nInput.equals("play");
            case 12:
                return nInput.equals("silencio") || nInput.equals("calla") || nInput.equals("callate") || nInput.equals("silent") || nInput.equals("mute");
            case 13:
                return nInput.equals("a la cesta") || nInput.equals("a casa") || nInput.equals("casa") || nInput.equals("cesta") || nInput.equals("nest") || nInput.equals("basket");
            case 14:
                return nInput.equals("bebe") || nInput.equals("beber") || nInput.equals("toma agua") || nInput.equals("drink");
            case 15:
                return nInput.equals("sigueme izquierda") || nInput.equals("izquierda") || nInput.equals("follow left") || nInput.equals("left");
            case 16:
                return nInput.equals("sigueme derecha") || nInput.equals("derecha") || nInput.equals("follow right") || nInput.equals("right");
            case 17:
                return nInput.equals("juega al futbol") || nInput.equals("futbol") || nInput.equals("pelota") || nInput.equals("football") || nInput.equals("soccer");
            case 18:
                return nInput.equals("arrodillate") || nInput.equals("kneel");
            case 19:
                return nInput.equals("bota") || nInput.equals("bounce");
            case 20:
                return nInput.equals("estatua") || nInput.equals("statue");
            case 21:
                return nInput.equals("baila") || nInput.equals("dance");
            case 22:
                return nInput.equals("gira") || nInput.equals("spin") || nInput.equals("turn");
            case 23:
                return nInput.equals("enciende tv") || nInput.equals("cambia tv") || nInput.equals("tv") || nInput.equals("switch tv");
            case 24:
                return nInput.equals("adelante") || nInput.equals("forward");
            case 25:
                return nInput.equals("gira izquierda") || nInput.equals("turn left");
            case 26:
                return nInput.equals("gira derecha") || nInput.equals("turn right");
            case 27:
                return nInput.equals("relajate") || nInput.equals("relax");
            case 28:
                return nInput.equals("croa") || nInput.equals("croak");
            case 29:
                return nInput.equals("inmersion") || nInput.equals("dip") || nInput.equals("dive");
            case 30:
                return nInput.equals("saluda") || nInput.equals("wave");
            case 31:
                return nInput.equals("mambo") || nInput.equals("marcha");
            case 32:
                return nInput.equals("gran salto") || nInput.equals("high jump");
            case 33:
                return nInput.equals("baile pollo") || nInput.equals("baile del pollo") || nInput.equals("chicken dance");
            case 34:
                return nInput.equals("triple salto") || nInput.equals("triple jump");
            case 35:
                return nInput.equals("muestra alas") || nInput.equals("abre alas") || nInput.equals("alas") || nInput.equals("wings");
            case 36:
                return nInput.equals("echa fuego") || nInput.equals("fuego") || nInput.equals("breathe fire") || nInput.equals("fire");
            case 37:
                return nInput.equals("planea") || nInput.equals("glide");
            case 38:
                return nInput.equals("antorcha") || nInput.equals("torch");
            case 40:
                return nInput.equals("cambia vuelo") || nInput.equals("cambia de vuelo") || nInput.equals("flight");
            case 41:
                return nInput.equals("voltereta") || nInput.equals("roll");
            case 42:
                return nInput.equals("anillo fuego") || nInput.equals("aro de fuego") || nInput.equals("ring of fire");
            case 43:
                return nInput.equals("come") || nInput.equals("comer") || nInput.equals("eat");
            case 44:
                return nInput.equals("mover cola") || nInput.equals("mueve la cola") || nInput.equals("mueve cola") || nInput.equals("cola") || nInput.equals("wag") || nInput.equals("wag tail");
            case 45:
                return nInput.equals("cuenta") || nInput.equals("count");
            case 46:
                return nInput.equals("cruzar") || nInput.equals("cria") || nInput.equals("breed");
            default:
                return false;
        }
    }

    public void dispose() {
        commands.clear();
        LOGGER.info("Command Handler -> Disposed!");
    }
}
