package com.scale.scalehackv4.utils.alts;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import net.minecraft.client.session.Session;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

// https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/alts/microsoft/MSAuth.java
public class oAuthUtil {
    public static String getMinecraftAccessToken(String userHashcode, String XSTSaccessToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/authentication/login_with_xbox"))
                    .setHeader("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{ \"identityToken\": \"XBL3.0 x="+userHashcode+";"+XSTSaccessToken+"\" }"))
                    .build();

            HttpResponse<String> res = NetworkUtil.getServerResponse(req);
            JsonObject resJson = NetworkUtil.getJSONObject(res);

            // https://http.cat/status/429
            if (res.statusCode() == 429) {
                AltManagerUtil.setErrorMessage("Rate limited, please wait.");
                return null;
            }

            if (resJson.has("access_token")) {
                AltManagerUtil.addProgressReport("Access token found!");
                return resJson.get("access_token").getAsString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        AltManagerUtil.setErrorMessage("No microsoft access token present!");
        return null;
    }

    private static AccessRefreshToken refreshToken;

    public static final String clientId = "00000000402b5328";
    private static final String scope = "service::user.auth.xboxlive.com::MBI_SSL";
    private static final String redirectUri = "https://login.live.com/oauth20_desktop.srf";

    //public static final String clientId = "54fd49e4-2103-4044-9603-2b028c814ec3";
    //private static final String scope = "XboxLive.signin XboxLive.offline_access";
    //private static final String redirectUri = "http://localhost:59125/";

    // refreshToken from authCode() or refreshToken()
    private static AccessRefreshToken refreshToken(String refreshToken, String clientId) throws Exception {
        // shrug, works, previously this used EVIL http3 stuff
        String postContent =
                "client_id=" + URLEncoder.encode(clientId, StandardCharsets.UTF_8) +
                        "&refresh_token=" + URLEncoder.encode(refreshToken, StandardCharsets.UTF_8) +
                        "&grant_type=refresh_token" +
                        "&redirect_uri=" + URLEncoder.encode(redirectUri, StandardCharsets.UTF_8) +
                        "&scope=" + URLEncoder.encode(scope, StandardCharsets.UTF_8);


        HttpRequest req = HttpRequest.newBuilder(URI.create("https://login.live.com/oauth20_token.srf"))
                .setHeader("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(postContent))
                .build();

        JsonObject res = NetworkUtil.getJSONObject(req);
        String accessToken = res.get("access_token").getAsString();
        String newRefreshToken = res.get("refresh_token").getAsString();

        AltManagerUtil.addProgressReport("Successfully refreshed refresh token!");
        return new AccessRefreshToken(accessToken, newRefreshToken);
    }

    // authToken is accessToken from authFlow() or refreshToken()
    private static String authXBL(String authToken) throws Exception {
        JsonObject payload = new JsonObject();
        JsonObject payloadProps = new JsonObject();
        payloadProps.addProperty("AuthMethod", "RPS");
        payloadProps.addProperty("SiteName", "user.auth.xboxlive.com");
        payloadProps.addProperty("RpsTicket", authToken);
        payload.add("Properties", payloadProps);
        payload.addProperty("RelyingParty", "http://auth.xboxlive.com");
        payload.addProperty("TokenType", "JWT");

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://user.auth.xboxlive.com/user/authenticate"))
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject res = NetworkUtil.getJSONObject(req);
        if (res.has("Token")) {
            AltManagerUtil.addProgressReport("Successfully authed xbl!");
            return NetworkUtil.getJSONObject(req).get("Token").getAsString();
        }

        AltManagerUtil.setErrorMessage("Failed to auth xbl!");
        return null;
    }

    // XBLToken from authXBL()
    private static XBLTokenUhs authXSTS(String XBLToken) throws Exception {
        JsonObject payload = new JsonObject();
        JsonObject payloadProps = new JsonObject();
        JsonArray userTokens = new JsonArray();
        userTokens.add(new JsonPrimitive(XBLToken));
        payloadProps.add("UserTokens", userTokens);
        payloadProps.addProperty("SandboxId", "RETAIL");
        payload.add("Properties", payloadProps);
        payload.addProperty("RelyingParty", "rp://api.minecraftservices.com/");
        payload.addProperty("TokenType", "JWT");

        HttpRequest req = HttpRequest.newBuilder(URI.create("https://xsts.auth.xboxlive.com/xsts/authorize"))
                .setHeader("Content-Type", "application/json")
                .setHeader("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload.toString()))
                .build();

        JsonObject res = NetworkUtil.getJSONObject(req);
        String token = res.getAsJsonObject().get("Token").getAsString();
        String uhs = res
                .getAsJsonObject()
                .get("DisplayClaims")
                .getAsJsonObject()
                .get("xui")
                .getAsJsonArray()
                .get(0)
                .getAsJsonObject()
                .get("uhs")
                .getAsString();

        AltManagerUtil.addProgressReport("Successfully authed xsts!");
        return new XBLTokenUhs(token, uhs);
    }

    public static Session authWithAccessRefreshToken(
            AccessRefreshToken accessRefreshToken
    ) throws Exception {
        String XBLToken = authXBL(accessRefreshToken.accessToken);
        XBLTokenUhs xblTokenUhs = authXSTS(XBLToken);
        String accessToken = getMinecraftAccessToken(xblTokenUhs.uhs, xblTokenUhs.XBLToken);

        return SessionUtil.queryPlayerProfile(accessToken);
    }

    public static boolean authWithRefreshToken(String refreshToken, String clientId) {
        try {
            Session session = authWithAccessRefreshToken(refreshToken(refreshToken, clientId));
            if (session == null) return false;

            SessionUtil.login(session);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public static class AccessRefreshToken {

        public final String accessToken;
        public final String refreshToken;

        public AccessRefreshToken(String accessToken, String refreshToken) {
            this.accessToken = accessToken;
            this.refreshToken = refreshToken;
        }
    }

    private static class XBLTokenUhs {

        public final String XBLToken;
        public final String uhs;

        public XBLTokenUhs(String XBLToken, String uhs) {
            this.XBLToken = XBLToken;
            this.uhs = uhs;
        }
    }

    public static class MSASession {

        public final String uuid;
        public final String username;
        public final String refreshToken;
        public final String accessToken;

        public MSASession(
                String uuid,
                String username,
                String refreshToken,
                String accessToken
        ) {
            this.uuid = uuid;
            this.username = username;
            this.refreshToken = refreshToken;
            this.accessToken = accessToken;
        }
    }
}
