package com.scale.scalehackv4.utils.alts;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.utils.C;
import lombok.AllArgsConstructor;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

public class AltManagerUtil {
    public static final ArrayList<Alt> ALTS = new ArrayList<>();
    private static final File ALTS_FOLDER = new File(Main.ALTS_PATH);

    public static Alt currentAlt;

    public static void loadAlts() {
        currentAlt = Alt.getCurrent();
        ALTS.add(currentAlt);

        if (!ALTS_FOLDER.exists() && !ALTS_FOLDER.mkdirs()) return;

        for (File file : ALTS_FOLDER.listFiles()) {
            if (!file.getName().endsWith(".json")) continue;

            try {
                Alt alt = C.gson.fromJson(FileUtils.readFileToString(file, Charset.defaultCharset()), Alt.class);
                if (ALTS.stream().anyMatch(a -> a.uuid.equals(alt.uuid))) continue;

                ALTS.add(alt);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load alt " + file.getName());
            }
        }
    }

    public static void saveAlt(Alt alt) {
        if (!ALTS_FOLDER.exists() && !ALTS_FOLDER.mkdirs()) return;

        try {
            Files.write(new File(ALTS_FOLDER, alt.uuid.toString() + ".json").toPath(), alt.toString().getBytes());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Alt getAlt(UUID uuid) {
        return getAlt(new File(Main.ALTS_PATH, uuid + ".json"));
    }

    private static Alt getAlt(File file) {
        if (!file.exists()) return null;

        try {
            return C.gson.fromJson(FileUtils.readFileToString(file, Charset.defaultCharset()), Alt.class);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @AllArgsConstructor
    // todo: add skin, ban data, name change data
    public static class Alt {
        public String name;
        public UUID uuid;
        public LoginData loginData;
        public long lastLogin;
        public Identifier skin;

        public static Alt getCurrent() {
            Alt alt = getAlt(C.mc.getGameProfile().id());
            if (alt != null) return alt;

            return new Alt(
                    C.mc.getGameProfile().name(),
                    C.mc.getGameProfile().id(),
                    new LoginData(C.mc.getSession().getAccessToken(), null, null, null),
                    System.currentTimeMillis(),
                    SessionUtil.getSkinIdentifier(C.mc.getGameProfile().id())
            );
        }

        public void login() {
            if (loginData.attemptLogin()) {
                lastLogin = System.currentTimeMillis();
                currentAlt = this;
                this.skin = SessionUtil.getSkinIdentifier(uuid);
                saveAlt(this);
            }
        }

        @Override
        public String toString() {
            return C.gson.toJson(this);
        }
    }

    public record LoginData(String ssid, String cookie, String refreshToken, String clientID) {
        public boolean attemptLogin() {
            if (ssid != null && SessionUtil.login(SessionUtil.queryPlayerProfile(ssid))) return true;
            if (refreshToken != null && clientID != null && oAuthUtil.authWithRefreshToken(refreshToken, clientID)) return true;
            return cookie != null && CookieAltUtil.authenticate(cookie);
        }
    }

    public static void addProgressReport(String message) {
        System.out.println("PROGRESS PEOPLE! " + message);
    }
    public static void setErrorMessage(String message) {
        System.out.println("its all gone wrong: " + message);
    }
}
