package com.scale.scalehackv4.utils.alts;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.render.RenderUtil;
import lombok.AllArgsConstructor;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.util.Identifier;
import org.apache.commons.io.FileUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.UUID;

public class AltManagerUtil {
    public static final ArrayList<Alt> ALTS = new ArrayList<>();
    private static final File ALTS_FOLDER = new File(Main.ALTS_PATH);

    public static Alt currentAlt;

    public static void loadAlts() {
        if (SessionUtil.queryPlayerProfile(C.mc.getSession().getAccessToken()) != null) {
            Alt.getCurrent().save();
        }
        currentAlt = Alt.getCurrent();
        ALTS.add(currentAlt);

        if (!ALTS_FOLDER.exists() && !ALTS_FOLDER.mkdirs()) return;

        for (File file : ALTS_FOLDER.listFiles()) {
            if (!file.getName().endsWith(".json")) continue;

            try {
                Alt alt = C.gson.fromJson(FileUtils.readFileToString(file, Charset.defaultCharset()), Alt.class);
                if (ALTS.stream().anyMatch(a -> a.uuid.equals(alt.uuid))) continue;

                ALTS.add(alt);
                alt.loadSkin();
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to load alt " + file.getName());
            }
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

        public BufferedImage skinImage;
        public Identifier skin;

        public static Alt getCurrent() {
            Alt alt = getAlt(C.mc.getGameProfile().id());
            if (alt != null) return alt;

            return new Alt(
                    C.mc.getGameProfile().name(),
                    C.mc.getGameProfile().id(),
                    new LoginData(C.mc.getSession().getAccessToken(), null, null, null, null),
                    System.currentTimeMillis(),
                    null,
                    DefaultSkinHelper.getTexture()
            );
        }

        public void login() {
            if (loginData.attemptLogin()) {
                lastLogin = System.currentTimeMillis();
                currentAlt = this;
                this.loginData.ssid = C.mc.getSession().getAccessToken();
                this.save();
            }
        }

        public void save() {
            if (!ALTS_FOLDER.exists() && !ALTS_FOLDER.mkdirs()) return;

            C.mc.execute(() -> {
                try {
                    updateSkin();
                    Files.write(new File(ALTS_FOLDER, this.uuid.toString() + ".json").toPath(), this.toString().getBytes());
                } catch (IOException e) {
                    e.printStackTrace();
                    setErrorMessage("Failed to save alt: " + e.getMessage());
                }
            });
        }


        public void delete() {
            ALTS.remove(this);
            try {
                Files.delete(getFile().toPath());
            } catch (IOException e) {
                setErrorMessage("Failed to delete alt: " + e.getMessage());
            }
        }

        public void updateSkin() {
            try {
                this.skinImage = ImageIO.read(new URL(SessionUtil.getSkin(this.uuid.toString())));
                this.skin = getSkinIdentifier();
                RenderUtil.createTexture(this.skin, this.skinImage);
            } catch (IOException e) {
                e.printStackTrace();
                System.err.println("Failed to update alt skin: " + e.getMessage());
            }
        }

        public void loadSkin() {
            if (this.skinImage == null) return;

            C.mc.execute(() -> {
                RenderUtil.createTexture(getSkinIdentifier(), this.skinImage);
            });
        }

        private Identifier getSkinIdentifier() {
            return Identifier.of("skincache/" + this.uuid.toString());
        }

        public File getFile() {
            return new File(ALTS_FOLDER, this.uuid.toString() + ".json");
        }

        @Override
        public String toString() {
            return C.gson.toJson(this);
        }
    }

    @AllArgsConstructor
    public static class LoginData {
        String ssid, cookie, refreshToken, clientId, scope;

        public boolean attemptLogin() {
            System.out.println(clientId);
            if (ssid != null && SessionUtil.login(SessionUtil.queryPlayerProfile(ssid))) return true;
            if (refreshToken != null && clientId != null && oAuthUtil.authWithRefreshToken(refreshToken, clientId, scope, oAuthUtil.DEFAULT_REDIRECT, false)) return true;
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
