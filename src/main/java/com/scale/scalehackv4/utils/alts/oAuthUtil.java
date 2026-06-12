package com.scale.scalehackv4.utils.alts;

import com.google.gson.JsonObject;

import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

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

            if (resJson.has("access_token"))
                return resJson.get("access_token").getAsString();
        } catch (Exception e) {
            e.printStackTrace();
        }

        AltManagerUtil.setErrorMessage("No microsoft access token present!");
        return null;
    }
}
