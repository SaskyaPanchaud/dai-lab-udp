package ch.heig.dai.udp.musician;

import com.google.gson.Gson;
import java.util.*;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import static java.nio.charset.StandardCharsets.*;

/**
 * Main app emitting sounds every second.
 */
public class Musician {
    final static String IPADDRESS = "239.255.22.5";
    final static int PORT = 9904;
    final static Gson GSON = new Gson();

    final static Map<String, String> instrumentToSound = new HashMap<>();

    static {
        instrumentToSound.put("piano", "ti-ta-ti");
        instrumentToSound.put("trumpet", "pouet");
        instrumentToSound.put("flute", "trululu");
        instrumentToSound.put("violin", "gzi-gzi");
        instrumentToSound.put("drum", "boum-boum");
    }

    public static void main(String[] args) throws InterruptedException {
        // FIXME : pourquoi on ne verifie pas tout simplement == 1 ?
        if (args.length < 1) {
            throw new RuntimeException("Please specify the instrument to play");
        }
        String soundString = instrumentToSound.get(args[0]);
        if (soundString == null) {
            throw new IllegalArgumentException("The instrument is not valid");
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            InetSocketAddress destAddress = new InetSocketAddress(IPADDRESS, PORT);

            Sound sound = new Sound(soundString);
            var packet = createDatagramPacket(sound, destAddress);
            while(true) {
                socket.send(packet);

                Thread.sleep(1000);
            }
        } 
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }

    /**
     * Create a datagram packet for an emitted sound.
     * @param sound The emitted sound.
     * @param destAddress The address to send the packet to.
     * @return The created DatagramPacket.
     */
    private static DatagramPacket createDatagramPacket(Sound sound, InetSocketAddress destAddress) {
        String message = GSON.toJson(sound);
        byte[] payload = message.getBytes(UTF_8);

        return new DatagramPacket(payload, payload.length, destAddress);
    }
}

/**
 * Class allowing JSON serialization of an emitted sound.
 */
class Sound {
    private String sound;
    private String uuid;

    Sound(String sound) {
        this.sound = sound;
        uuid = UUID.randomUUID().toString();
    }
}
