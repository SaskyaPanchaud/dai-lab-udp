package ch.heig.dai.udp.auditor;

import java.io.*;
import java.net.*;
import static java.nio.charset.StandardCharsets.*;
import java.net.MulticastSocket;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.google.gson.Gson;

/**
 * Main application, creating a SoundListener and a StatusSender.
 */
public class Auditor {
    final static String IPADDRESS = "239.255.22.5";
    final static int LISTENER_PORT = 9904;
    final static int SENDER_PORT = 2205;

    public static void main(String[] args) throws InterruptedException {
        SoundListener listener = new SoundListener(IPADDRESS, LISTENER_PORT);
        StatusSender sender = new StatusSender(SENDER_PORT, listener);

        Thread listenerThread = new Thread(listener);
        listenerThread.start();

        sender.run();
    }
}

/**
 * This class listens sounds sent by musicians to it's multicast address,
 * and keeps track of the active musicians.
 */
class SoundListener implements Runnable {
    private final String IPAddress;
    private final int port; 
    private final Map<String, Musician> idToMusician = Collections.synchronizedMap(new ConcurrentHashMap<String, Musician>());
    private static final Gson GSON = new Gson();

    public SoundListener(String IPAddress, int port) {
        this.IPAddress = IPAddress;
        this.port = port;
    }

    /**
     * Run the sound listener.
     */
    @Override
    public void run() {
        try (MulticastSocket socket = new MulticastSocket(port)) {

            InetSocketAddress group_address =  new InetSocketAddress(IPAddress, port);

            // FIXME : comment tu recuperes le nom de l'interface de l'ordi ?
            NetworkInterface netif = NetworkInterface.getByName("eth0");
            socket.joinGroup(group_address, netif);

            while (true) {
                String message = receiveMessage(socket);
                updateMusicians(message);
            }

        } 
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Recieve a message from the socket.
     * @param socket The socker from which to recieve the message.
     * @return The message as a String.
     * @throws IOException
     */
    private String receiveMessage(MulticastSocket socket) throws IOException {
        byte[] buffer = new byte[1024];
        DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
        socket.receive(packet);
        
        return new String(packet.getData(), 0, packet.getLength(), UTF_8);
    }

    /**
     * Update the musicians map with the sound stored in the message.
     * @param message The message containing a sound emmited by a musician.
     */
    private void updateMusicians(String message) {
        RecievedSound sound = GSON.fromJson(message, RecievedSound.class);

        if (!idToMusician.containsKey(sound.getUuid())) {
            idToMusician.put(sound.getUuid(), new Musician(sound.getUuid(), sound.getInstrument()));
        }

        idToMusician.get(sound.getUuid()).updateLastActivity();
    }

    /**
     * Remove any inactive musician.
     */
    private void purgeInactiveMusicians() {
        for (var musician : idToMusician.values()) {
            if (!musician.isActive()) {
                idToMusician.remove(musician.getUuid());
            }
        }
    }

    /**
     * Get the auditor's up-to-date payload with information
     * about the musicians currently playing.
     * @return The payload as a JSON String.
     */
    public String getStatusPayLoad() {
        purgeInactiveMusicians();
        return new Gson().toJson(idToMusician.values());
    }
}

/**
 * This class sends informations about the musicians currently playing
 * upon recieving a TCP connection.
 */
class StatusSender implements Runnable {
    private final int port;
    private SoundListener listener;

    public StatusSender(int port, SoundListener listener) {
        this.port = port;
        this.listener = listener;
    }

    /**
     * Run the status sender.
     */
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

// FIXME : pourquoi on retrouve la classe Musician aussi ici ? On pourrait pas rajouter l'attribut concernant l'activite dans l'autre classe ?
/**
 * Class allowing serialization and storing of Musicians. 
 * The attributes match the information sent by the StatusSender.
 */
class Musician {
    private final String uuid;
    private String instrument;
    private long lastActivity;

    Musician(String uuid, String instrument) {
        this.uuid = uuid;
        this.instrument = instrument;
        updateLastActivity();
    }

    /**
     * Set the lastActivity as the current time.
     */
    void updateLastActivity() {
        lastActivity = System.currentTimeMillis();
    }

    public String getUuid() {
        return uuid;
    }

    public String getInstrument() {
        return instrument;
    }

    /**
     * Check if the musician is active, i.e. if it has emmited a sound in 
     * the last 5 seconds.
     * @return true if the musician is active, false otherwise.
     */
    public boolean isActive() {
        return System.currentTimeMillis() - lastActivity < 5000;
    }
}

/**
 * Class allowing de-serialization of recieved sounds from musicians,
 * which are formatted as JSON objects matching this class' attributes.
 */
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

    public String getInstrument() {
        return SOUND_TO_INSTRUMENT.get(sound);
    }
}
