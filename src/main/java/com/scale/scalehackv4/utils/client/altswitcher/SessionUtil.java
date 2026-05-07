package com.scale.scalehackv4.utils.client.altswitcher;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.scale.scalehackv4.utils.C;
import net.minecraft.client.session.Session;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;

// https://github.com/scoliossis/ScaleHackV3/blob/master/src/main/java/com/github/scoliossis/utils/alts/SessionUtil.java
// https://minecraft.wiki/w/Mojang_API
public class SessionUtil {
    private static final String VALID_NAME_CHARATERS_REGEX = "[a-zA-Z0-9]";
    public static Session sessionOverride = null;

    public static JsonObject getJSONObject(HttpRequest req) throws IOException, InterruptedException {
        return C.gson.fromJson(C.CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).body(), JsonElement.class).getAsJsonObject();
    }

    // https://minecraft.wiki/w/Mojang_API#Query_player_profile
    public static Session queryPlayerProfile(String accessToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile"))
                    .setHeader("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            JsonObject res = getJSONObject(req);

            if (res.has("id") && res.has("name")) {
                String uuid = res.get("id").getAsString();
                // uuids need to be formatted as "bce89d36-2b7c-4730-b776-4e5ad56839a2", the dashes stop "Invalid UUID string: bce89d362b7c4730b7764e5ad56839a2"
                uuid = uuid.substring(0, 8)+"-"+uuid.substring(8,12)+"-"+uuid.substring(12,16)+"-"+uuid.substring(16,20)+"-"+uuid.substring(20);

                String username = res.get("name").getAsString();

                return new Session(username, UUID.fromString(uuid), accessToken, Optional.empty(), Optional.empty());
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }

        return null;
    }

    // https://minecraft.wiki/w/Mojang_API#Check_name_availability
    public static String checkNameAvailability(String accessToken, String name) {
        if (name.length() > 16) return "Name is too long: " + name.length() + " > 16";
        if (name.length() <= 3) return "Name is too short: " + name.length() + " < 3";
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile/name/"+name+"/available"))
                    .setHeader("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            JsonObject res = getJSONObject(req);
            if (!res.has("status")) return "Access token is invalid.";

            return res.get("status").getAsString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "";
        }
    }

    // https://minecraft.wiki/w/Mojang_API#Query_player's_name_change_information
    public static String checkLastNameChange(String accessToken) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile/namechange"))
                    .setHeader("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();

            JsonObject res = getJSONObject(req);

            if (res.has("nameChangeAllowed")) {
                boolean allowed = res.get("nameChangeAllowed").getAsBoolean();

                if (allowed) return "ALLOWED";

                if (res.has("changedAt"))
                    return res.get("changedAt").getAsString();

                if (res.has("createdAt"))
                    return res.get("createdAt").getAsString();
            }

            return res.toString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "";
        }
    }

    // https://minecraft.wiki/w/Mojang_API#Change_name
    public static String changeName(String accessToken, String newName) {
        String lastChange = checkLastNameChange(accessToken);
        if (!lastChange.equals("ALLOWED")) {
            if (lastChange.isEmpty() || lastChange.contains("path")) return "Cannot change name, access token is invalid.";

            return "Cannot change name, last name change was: " + lastChange;
        }

        String invalidCharacters = newName.replaceAll(VALID_NAME_CHARATERS_REGEX, "");
        if (!invalidCharacters.isEmpty()) return "Name contains invalid characters: \"" + invalidCharacters + "\"";
        if (newName.length() > 16) return "Name is " + newName.length() + " characters, 16 is the limit.";

        String nameAvailability = checkNameAvailability(accessToken, newName);
        switch (nameAvailability) {
            case "AVAILABLE":
                break;
            case "DUPLICATE":
                return "Name is already taken.";
            case "NOT_ALLOWED":
                return "Name is BANNED by microsoft.";
            default:
                // shouldnt appear, maybe if server doesnt respond? normally only replies if character limit reached or invalid charaters
                return nameAvailability;
        }

        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile/name/"+newName))
                    .setHeader("Authorization", "Bearer " + accessToken)
                    .PUT(HttpRequest.BodyPublishers.noBody())
                    .build();

            switch (C.CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).statusCode()) {
                case 400:
                    return "Name does not meet requirement. The name must have less than or equal to 16 characters and must consist of alphanumericals and underscores.";
                case 403:
                    return "Cannot change name, last name change was: ";
                case 429:
                    return "Too many rename requests sent.";
                case 200:
                    sessionOverride = new Session(newName, C.mc.getSession().getUuidOrNull(), C.mc.getSession().getSessionId(), Optional.empty(), Optional.empty());

                    return "Name changed successfully to: " + newName + "!";
                default:
                    return "Unknown error, what happened.";
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "";
        }
    }

    public static String changeSkin(String accessToken, String skinURL) {
        boolean slim = true;
        try {
            BufferedImage skinImage = ImageIO.read(new URL(skinURL));

            // slim skins SHOULDNT have a coloured pixel here, because it wont be drawn
            slim = skinImage.getRGB(54, 20) == 0;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return changeSkin(accessToken, skinURL, slim ? "slim" : "classic");
    }

    // https://minecraft.wiki/w/Mojang_API#Change_skin
    public static String changeSkin(String accessToken, String skinURL, String skinVariant) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://api.minecraftservices.com/minecraft/profile/skins"))
                    .header("Authorization", "Bearer " + accessToken)
                    .setHeader("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString("{ \"variant\": \""+skinVariant+"\", \"url\": \""+skinURL+"\"}"))
                    .build();

            switch (C.CLIENT.send(req, HttpResponse.BodyHandlers.ofString()).statusCode()) {
                case 400:
                    return "Request failed, skin URL could be invalid.";
                case 403:
                    return "Cannot change skin.";
                case 429:
                    return "Too many skin change requests sent.";
                case 200:
                    return "Skin changed successfully!";
                default:
                    return "Unknown error, what happened.";
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            return "";
        }
    }

    // https://minecraft.wiki/w/Mojang_API#Query_player's_skin_and_cape
    public static String getSkin(String uuid) {
        try {
            HttpRequest req = HttpRequest.newBuilder(URI.create("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid))
                    .GET()
                    .build();

            JsonObject res = getJSONObject(req);

            String value = res.get("properties").getAsJsonArray()
                    .get(0).getAsJsonObject()
                    .get("value").getAsString();

            String valueDecoded = new String(Base64.getDecoder().decode(value));

            JsonObject valueObject = C.gson.fromJson(valueDecoded, JsonObject.class);

            return valueObject.get("textures").getAsJsonObject()
                    .get("SKIN").getAsJsonObject()
                    .get("url").getAsString();
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            // the api claims if you have a steve skin it returns an empty SKIN field, didn't test.
            return "https://s.namemc.com/i/12b92a9206470fe2.png";
        }
    }

    // great name, well done scale
    public static BufferedImage getHead(String uuid) {
        try {
            BufferedImage skinImage = ImageIO.read(new URL(getSkin(uuid)));

            return getHead(skinImage);
        } catch (IOException e) {
            e.printStackTrace();
            return new BufferedImage(1,1, BufferedImage.TYPE_INT_ARGB);
        }
    }

    public static BufferedImage getHead(BufferedImage skinImage) {
        BufferedImage head = skinImage.getSubimage(8, 8, 8, 8);
        BufferedImage secondaryLayer = skinImage.getSubimage(40, 8, 8, 8);

        head.getGraphics().drawImage(secondaryLayer, 0, 0, null);

        return head;
    }
}