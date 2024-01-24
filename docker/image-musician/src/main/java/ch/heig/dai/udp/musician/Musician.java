package ch.heig.dai.udp.musician;

import com.google.gson.Gson;
import java.util.*;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.DatagramPacket;
import java.net.InetSocketAddress;
import static java.nio.charset.StandardCharsets.*;

public class Musician {
    final static String IPADDRESS = "239.255.22.5";
    final static int PORT = 9904;

    final static Map<String, String> INSTRUMENT_TO_SOUND = new HashMap<>();

    static {
        INSTRUMENT_TO_SOUND.put("piano", "ti-ta-ti");
        INSTRUMENT_TO_SOUND.put("trumpet", "pouet");
        INSTRUMENT_TO_SOUND.put("flute", "trululu");
        INSTRUMENT_TO_SOUND.put("violin", "gzi-gzi");
        INSTRUMENT_TO_SOUND.put("drum", "boum-boum");
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length < 1) {
            throw new RuntimeException("Please specify the instrument to play");
        }
        String soundString = INSTRUMENT_TO_SOUND.get(args[0]);
        if (soundString == null) {
            throw new IllegalArgumentException("The instrument is not valid");
        }

        try (DatagramSocket socket = new DatagramSocket()) {
            Gson gson = new Gson();
            InetSocketAddress destAddress = new InetSocketAddress(IPADDRESS, PORT);
            Sound sound = new Sound(soundString);
            while(true) {
                String message = gson.toJson(sound);
                byte[] payload = message.getBytes(UTF_8);
                var packet = new DatagramPacket(payload, payload.length, destAddress);
                socket.send(packet);

                Thread.sleep(1000);
            }

        } 
        catch (IOException ex) {
            System.out.println(ex.getMessage());
        }
    }
}

class Sound {
    String sound;
    String uuid;

    Sound(String sound) {
        this.sound = sound;
        uuid = UUID.randomUUID().toString();
    }
}