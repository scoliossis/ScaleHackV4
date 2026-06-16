package com.scale.scalehackv4.modules.impl.other;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.ChatEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.events.impl.render.RenderTickEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import net.minecraft.network.encryption.NetworkEncryptionException;
import net.minecraft.network.packet.s2c.login.LoginHelloS2CPacket;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.MessageDigest;
import java.security.PublicKey;

@RegisterModule(
        name = "Disabler",
        category = Category.OTHER,
        enabledByDefault = true
)
// todo: disable racism
public class DisablerModule extends Module {
    @RegisterSubModule(name = "Anti Rat")
    public boolean antirat = true;
    @RegisterSubModule(name = "Max Requests", min = 1, max = 10, parent = "Anti Rat")
    public static int maxRequests = 10;
    @RegisterSubModule(name = "Request Delay", min = 10, max = 200, parent = "Anti Rat", increment = 10)
    public static int requestDelay = 50;

    @RegisterSubModule(name = "Verus")
    public boolean verusDisablerGodBypahh20267ongRealest = true;

    private static long lastRequest = 0;
    private static int outgoingRequests = 0;

    public Event onRenderTick = new RenderTickEvent(this, params -> {
        if (!C.isInGame() || C.mc.isInSingleplayer() || serverPublicKey == null || !antirat) return false;

        // never ending requests.
        C.ASYNC.execute(() -> {
            if (System.currentTimeMillis() - lastRequest > requestDelay && outgoingRequests < maxRequests) {
                lastRequest = System.currentTimeMillis();
                outgoingRequests++;
                attemptLogin();
                outgoingRequests--;
            }
        });

        return false;
    });

    public Event onReceiveMessage = new ChatEvent.Received(this, params -> {
        // no real server uses verus so larping a full disabler is the same as actually having one
        return params.message.startsWith("Verus > ") || params.message.startsWith("[Verus] ");
    });

    private static String serverID = "";
    private static PublicKey serverPublicKey;

    public Event onPacket = new PacketEvent.Received(null, params -> {
        if (params.packet instanceof LoginHelloS2CPacket helloPacket) {
            serverID = "";
            serverPublicKey = null;

            try {
                serverPublicKey = helloPacket.getPublicKey();
                serverID = helloPacket.getServerId();
            } catch (NetworkEncryptionException e) {
                throw new RuntimeException(e);
            }
        }
        return false;
    });

    private static int attemptLogin() {
        try {
            if (!C.isInGame()) return 429;

            String accessToken = C.mc.getSession().getAccessToken();
            String selectedProfile = C.mc.getSession().getUuidOrNull().toString().replace("-", "");
            KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
            keyGenerator.init(128);

            String serverId = generateServerId(serverID, serverPublicKey, keyGenerator.generateKey());

            HttpRequest req = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/join"))
                    .setHeader("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{ \"accessToken\": \""+accessToken+"\", \"selectedProfile\": \""+selectedProfile+"\", \"serverId\": \""+serverId+"\"}"))
                    .build();

            return C.CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).statusCode();
        } catch (Exception e) {
            e.printStackTrace();
        }

        return 429;
    }

    public static String generateServerId(String baseServerId,   // Base server ID, usually an empty string""
                                          PublicKey publicKey,   // Server's RSA public key
                                          SecretKey secretKey    // The symmetric AES secret key used between server and client
    ) throws Exception {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-1");
        messageDigest.update(baseServerId.getBytes("ISO_8859_1"));
        messageDigest.update(secretKey.getEncoded());
        messageDigest.update(publicKey.getEncoded());
        byte[] digestData = messageDigest.digest();
        return new BigInteger(digestData).toString(16);
    }

    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {

    }
}
