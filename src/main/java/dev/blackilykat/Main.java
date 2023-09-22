package dev.blackilykat;

import com.github.steveice10.mc.protocol.MinecraftProtocol;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundKeepAlivePacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundLoginPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.ClientboundPingPacket;
import com.github.steveice10.mc.protocol.packet.ingame.clientbound.entity.player.ClientboundSetHealthPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatCommandPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundChatPacket;
import com.github.steveice10.mc.protocol.packet.ingame.serverbound.ServerboundPongPacket;
import com.github.steveice10.packetlib.Session;
import com.github.steveice10.packetlib.event.session.DisconnectedEvent;
import com.github.steveice10.packetlib.packet.Packet;
import com.github.steveice10.packetlib.tcp.TcpClientSession;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.*;

public class Main {
    public static Map<String, Boolean> connected = new HashMap<>();
//    public static String ip = "kaboom.fusselig.xyz";
    public static String ip = "localhost";
    public static String usernamePrefix = "Bot_";
    public static int delay = 200;
    public static int port = 25565;
    public static ArrayList<Bot> botArrayList = new ArrayList<>();
    public static int bots = 16;
    public static String autoCommand = "";
    public static boolean autoRespawn = true;
    public static boolean announceHealthChanges = true;
    public static void main(String[] args) throws InterruptedException {
        relogAll();
        while(true) switch (Console.readln()) {
            case "relog" -> {
                relogAll();
            }
            case "chat" -> {
                Console.print("Enter message: ");
                String message = Console.readln();
                botArrayList.forEach(bot -> bot.chat(message));
            }
            case "command" -> {
                Console.print("Enter command: ");
                String command = Console.readln();
                botArrayList.forEach(bot -> bot.command(command));
            }
            case "autocommand" ->  {
                Console.print("Enter command (or leave empty to remove): ");
                autoCommand = Console.readln();
                Console.println(
                        autoCommand.isEmpty()
                                ? "Bots will no longer automatically send commands when they join."
                                : "Bots will automatically send '"+autoCommand+"' when they join.");
            }
            case "autorespawn" -> {
                Console.print("Enter true or false: ");
                autoRespawn = Boolean.parseBoolean(Console.readln());
            }
            case "announcehealthchanges" -> {
                Console.print("Enter true or false: ");
                announceHealthChanges = Boolean.parseBoolean(Console.readln());
            }
            case "respawn" -> {
                Console.println("I don't know how to make bots respawn so it's going to relog, if you do know please contribute thankss");
                for (Bot bot : botArrayList) {
                    if(bot.health == 0) bot.respawn();
                }
            }
            case "quit" -> {
                botArrayList.forEach(bot -> bot.session.disconnect("Quitting bot program"));
                botArrayList.clear();
                System.exit(0);
            }
            default -> {
                Console.println("Unknown command");
            }
        }
    }
    public static void relogAll() throws InterruptedException {
        for (Bot bot : botArrayList) {
            bot.session.disconnect("Relogging all bots :3");
        }
        botArrayList.clear();
        for (int i = 0; i < bots; i++) {
            Bot bot = new Bot(usernamePrefix+i, ip, port, usernamePrefix+i);
            bot.login();
            botArrayList.add(bot);
            Thread.sleep(delay);
        }
    }
    public static class SessionAdapter extends com.github.steveice10.packetlib.event.session.SessionAdapter {
        public final Bot owner;
        public final String name;
        public SessionAdapter(String name, Bot owner) {
            this.name = name;
            this.owner = owner;
            connected.put(this.name, false);
        }

        @Override
        public void packetReceived(Session session, Packet packet) {
            if(packet instanceof ClientboundLoginPacket) {
                Console.println("Bot '"+name+"' connected!");
                connected.put(name, true);
            } else if (packet instanceof ClientboundSetHealthPacket) {
                owner.health = ((ClientboundSetHealthPacket) packet).getHealth();
                owner.food = ((ClientboundSetHealthPacket) packet).getFood();
                owner.saturation = ((ClientboundSetHealthPacket) packet).getSaturation();
                if(owner.health == 0 && autoRespawn) owner.respawn();
                if(announceHealthChanges) owner.chat(String.format("I now have %s health, %s hunger and %s saturation.", owner.health, owner.food, owner.saturation));
            } else if (packet instanceof ClientboundPingPacket) {
                int id = ((ClientboundPingPacket) packet).getId();
//                Console.println(owner.name + " recieved keep alive packet #" + id);
                session.send(
                        new ServerboundPongPacket(
                                id
                        )
                );
            } else if (packet instanceof ClientboundKeepAlivePacket) {
                long id = ((ClientboundKeepAlivePacket) packet).getPingId();
//                Console.println(owner.name + " recieved keep alive packet #" + id);
                session.send(
                        new ServerboundPongPacket(
                                (int) id
                        )
                );
            }
        }

        @Override
        public void disconnected(DisconnectedEvent event) {
            PlainTextComponentSerializer serializer = PlainTextComponentSerializer.builder().build();
            Console.println("Bot '"+name+"' disconnected: "+serializer.serialize(event.getReason()));
            connected.put(name, false);
        }
    }

    public static class Bot {
        public String name;
        public String username;
        public Session session;
        public String ip;
        public int port;
        public float health = 0;
        public float food = 0;
        public float saturation = 0;
        private final List<String> chatQueue = new ArrayList<>();
        public Bot(String name, String ip, int port, String username) {
            this.name = name;
            this.session = new TcpClientSession(ip, port, new MinecraftProtocol(username), null);
            session.addListener(new SessionAdapter(name, this));
            this.username = username;
            this.ip = ip;
            this.port = port;
        }
        public void login() {
            session.connect();
            new Thread(() -> {
//                while(!session.isConnected()) {
//                    Console.println("not conetde yet");
//                    try {
//                        Thread.sleep(250);
//                    } catch (InterruptedException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
                while(session.isConnected()) {
//                    Console.println("connected !!");
                    try {
                        Thread.sleep(250);
                    } catch (InterruptedException e) {
                        throw new RuntimeException(e);
                    }
//                    session.send(new ServerboundKeepAlivePacket(0));
                    if(chatQueue.isEmpty()) continue;
//                    Console.println(chatQueue.get(0));
                    session.send(new ServerboundChatPacket(
                            chatQueue.get(0), // first message in queue
                            Timestamp.from(Instant.now()).getTime(),
                            0,
                            null,
                            0,
                            new BitSet() // if this is null it throws an exception and disconnects the bot
                    ));
                    chatQueue.remove(0);
                }
            }).start();
            new Thread(() -> {
                if(autoCommand.isEmpty()) return;
                try {
                    Thread.sleep(50);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                command(autoCommand);
            }).start();
        }
        public void chat(String message) {
            chatQueue.add(message);
        }

        public void command(String command) {
            session.send(new ServerboundChatCommandPacket(
                    command,
                    Instant.now().toEpochMilli(),
                    0,
                    new ArrayList<>(),
                    0,
                    new BitSet()
            ));
        }
        public void respawn() {
            session.disconnect("no clue how to respawn");
            this.session = new TcpClientSession(ip, port, new MinecraftProtocol(username), null);
            session.addListener(new SessionAdapter(name, this));
            login();
        }
    }
}