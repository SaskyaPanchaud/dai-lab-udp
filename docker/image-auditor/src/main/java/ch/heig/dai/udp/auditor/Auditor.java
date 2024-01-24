package ch.heig.dai.udp.auditor;

import java.io.*;
import java.net.*;
import static java.nio.charset.StandardCharsets.*;
import java.net.MulticastSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

public class Auditor {
    final static String IPADDRESS = "239.255.22.5";
    final static int LISTENER_PORT = 9904;
    final static int SENDER_PORT = 2205;

    public static void main(String[] args) throws InterruptedException {
        SoundListener listener = new SoundListener(IPADDRESS, LISTENER_PORT);
        StatusSender sender = new StatusSender(SENDER_PORT, listener);

        Thread listenerThread = new Thread(listener);
        listenerThread.start();

        Thread senderThread = new Thread(sender);
        senderThread.start();
    }
}

class SoundListener implements Runnable {
    private final String IPAddress;
    private final int port; 
    private final Map<String, Musician> idToMusician = Collections.synchronizedMap(new ConcurrentHashMap<String, Musician>());
    private static final Gson GSON = new Gson();

    public SoundListener(String IPAddress, int port) {
        this.IPAddress = IPAddress;
        this.port = port;
    }

    public void run() {
        try (MulticastSocket socket = new MulticastSocket(port)) {

            InetSocketAddress group_address =  new InetSocketAddress(IPAddress, port);
            NetworkInterface netif = NetworkInterface.getByName("eth0");
            socket.joinGroup(group_address, netif);

            while (true) {
                String message = recieveMessage(socket);
                updateMusicians(message);
            }

        } 
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    private String recieveMessage(MulticastSocket socket) throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        
        return new String(packet.getData(), 0, packet.getLength(), UTF_8);
    }

    private void updateMusicians(String message) {
        RecievedSound sound = GSON.fromJson(message, RecievedSound.class);

        if (!idToMusician.containsKey(sound.getUuid())) {
            idToMusician.put(sound.getUuid(), new Musician(sound.getUuid(), sound.getInstrument()));
        }

        idToMusician.get(sound.getUuid()).updateLastActivity();
    }

    private void purgeInactiveMusicians() {
        for (var musician : idToMusician.values()) {
            if (!musician.isActive()) {
                idToMusician.remove(musician.getUuid());
            }
        }
    }

    public String getStatusPayLoad() {
        purgeInactiveMusicians();
        return new Gson().toJson(idToMusician.values());
    }
}

class StatusSender implements Runnable {
    private final int port;
    private SoundListener listener;

    public StatusSender(int port, SoundListener listener) {
        this.port = port;
        this.listener = listener;
    }

    @Override
    public void run() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {

                try (Socket socket = serverSocket.accept();
                    var out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), UTF_8))) {
                    out.write(listener.getStatusPayLoad());
                } 
                catch (IOException e) {
                    System.out.println("Server: socket ex.: " + e);
                }
            }
        } 
        catch (IOException e) {
            System.out.println("Server: server socket ex.: " + e);
        }
    }
}

class Musician {
    private final String UUID;
    private String instrument;
    private long lastActivity;

    Musician(String uuid, String instrument) {
        UUID = uuid;
        this.instrument = instrument;
        updateLastActivity();
    }

    void updateLastActivity() {
        lastActivity = System.currentTimeMillis();
    }

    public String getUuid() {
        return UUID;
    }

    public String getInstrument() {
        return instrument;
    }

    public boolean isActive() {
        return System.currentTimeMillis() - lastActivity < 5000;
    }
}

class RecievedSound {
    private String uuid;
    private String sound;


    final static Map<String, String> SOUND_TO_INSTRUMENT = new HashMap<>();

    static {
        SOUND_TO_INSTRUMENT.put("ti-ta-ti", "piano");
        SOUND_TO_INSTRUMENT.put("pouet", "trumpet");
        SOUND_TO_INSTRUMENT.put("trululu", "flute");
        SOUND_TO_INSTRUMENT.put("gzi-gzi", "violin");
        SOUND_TO_INSTRUMENT.put("boum-boum", "drum");
    }

    RecievedSound(String uuid, String sound) {
        this.uuid = uuid;
        this.sound = sound;
    }

    public String getUuid() {
        return uuid;
    }

    public String getSound() {
        return sound;
    }

    public String getInstrument() {
        return SOUND_TO_INSTRUMENT.get(sound);
    }
}