package com.scale.scalehackv4.screens.mainmenu;

import com.scale.scalehackv4.Main;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.alts.AltManagerUtil;
import com.scale.scalehackv4.utils.client.EasingUtil;
import com.scale.scalehackv4.utils.render.FontUtil;
import com.scale.scalehackv4.utils.render.MouseUtil;
import com.scale.scalehackv4.utils.render.Render2dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import lombok.AllArgsConstructor;
import net.minecraft.client.gui.Click;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.option.OptionsScreen;
import net.minecraft.client.gui.screen.world.SelectWorldScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Matrix3x2fStack;

import java.awt.*;

// todo: make a changelog :heart_eyes:
//  maybe copyright text in bottom right like minecraft
public class MainMenuScreen extends Screen {
    public static MainMenuScreen INSTANCE = new MainMenuScreen(Text.of("official scalehake main menu boom chika wowow"));
    public MainMenuScreen(Text title) {
        super(title);
    }

    private final static int TEXT_INDENT_X = 10;
    private final static int TEXT_INDENT_Y = 5;

    protected static double lastMouseScroll = 0;

    /*
    private static final RotatingCubeMapRenderer SINGLEPLAYER_PANORAMA =
            new RotatingCubeMapRenderer(new CubeMapRenderer(Identifier.of(Main.MOD_ID, "mainmenu/panorama/panorama")));
    private static final RotatingCubeMapRenderer MULTIPLAYER_PANORAMA =
            new RotatingCubeMapRenderer(new CubeMapRenderer(Identifier.of(Main.MOD_ID, "mainmenu/panorama_21/panorama")));

    public static void registerTextures(TextureManager textureManager) {
        SINGLEPLAYER_PANORAMA.registerTextures(textureManager);
        MULTIPLAYER_PANORAMA.registerTextures(textureManager);
    }
     */

    private static int getFontSize() {
        return C.res().getScaledWidth() / 15;
    }

    // todo: lots of redundant code
    @AllArgsConstructor
    private enum Button {
        SINGLEPLAYER {
            @Override int[][] getBounds() {return getSingleplayerPolygon();}
            @Override void render() {this.renderTitle();}
            @Override void onClick() {C.mc.setScreen(new SelectWorldScreen(INSTANCE));}
        },
        MULTIPLAYER {
            @Override int[][] getBounds() {return getMultiplayerPolygon();}
            @Override void render() {this.renderTitle();}
            @Override void onClick() {C.mc.setScreen(new MultiplayerScreen(INSTANCE));}
        },
        OPTIONS {
            @Override void render() {this.renderButton();}
            @Override void onClick() {C.mc.setScreen(new OptionsScreen(INSTANCE, C.mc.options));}
        },
        DISCORD {
            @Override void render() {this.renderButton();}
            @Override void onClick() {C.mc.setScreen(new OptionsScreen(INSTANCE, C.mc.options));}
        },
        TWITTER {
            @Override void render() {this.renderButton();}
            @Override void onClick() {C.mc.setScreen(new OptionsScreen(INSTANCE, C.mc.options));}
        },
        YOUTUBE {
            @Override void render() {this.renderButton();}
            @Override void onClick() {C.mc.setScreen(new OptionsScreen(INSTANCE, C.mc.options));}
        },
        GITHUB {
            @Override void render() {this.renderButton();}
            @Override void onClick() {C.mc.setScreen(new OptionsScreen(INSTANCE, C.mc.options));}
        },
        QUIT {
            @Override void render() {this.renderButton();}
            @Override void onClick() {C.mc.close();}
        };

        void render() {}
        int[][] getBounds() {return getExtraOptionsBounds(this.ordinal());}
        void onClick() {}
        void onHover() {
            EasingUtil.Animation animation = EasingUtil.getAnimationDetails(this.name());
            if (animation != null && animation.up) return;

            EasingUtil.addAnimation(this.name(), 250, true, EasingUtil.EasingFunctions.Ease_In_Out_Sine);
        }
        void onNotHover() {
            EasingUtil.Animation animation = EasingUtil.getAnimationDetails(this.name());
            if (animation == null || !animation.up) return;
            EasingUtil.addAnimation(this.name(), 250, false, EasingUtil.EasingFunctions.Ease_In_Out_Sine);
        }

        void renderTitle() {
            int[][] bounds = this.getBounds();

            Render2dUtil.drawTexturedPolygon(this.BLURRED_TEXTURE, bounds[0], bounds[1], new Color(150,150,150));

            float animationProgress = Math.clamp((float) EasingUtil.getAnimation(this.name()), 0, 1);
            if (animationProgress == -1) return;

            Render2dUtil.drawTexturedPolygon(this.TEXTURE, bounds[0], bounds[1], new Color(1,1,1,animationProgress));

            int fontSize = getFontSize();
            int fontHeight = FontUtil.getFontHeight(fontSize);

            String mode = this.name().split("PLAYER")[0];
            int modeX = this.ordinal() > 0
                    ? C.res().getScaledWidth() - FontUtil.getStringWidth(mode, fontSize) - TEXT_INDENT_X
                    : bounds[0][0] + TEXT_INDENT_X;
            FontUtil.drawString(
                    mode,
                    modeX,
                    bounds[1][1] - fontHeight * 2 - TEXT_INDENT_Y,
                    fontSize,
                    Color.WHITE,
                    true
            );

            int playerX = this.ordinal() > 0
                    ? C.res().getScaledWidth() - FontUtil.getStringWidth("PLAYER", fontSize) - TEXT_INDENT_X
                    : modeX;
            FontUtil.drawString(
                    "PLAYER",
                    playerX,
                    bounds[1][1] - fontHeight - TEXT_INDENT_Y,
                    fontSize,
                    Color.WHITE,
                    true
            );
        }

        void renderButton() {
            int[][] bounds = getBounds();

            float animationProgress = Math.clamp((float) EasingUtil.getAnimation(this.name()), 0, 1);

            Matrix3x2fStack matrices = Render2dUtil.getDrawContext().getMatrices();
            matrices.pushMatrix();

            int width = bounds[0][2]-bounds[0][0];
            int height = bounds[1][3]-bounds[1][1];

            matrices.translate(bounds[0][0] + width/2f, bounds[1][1] + height/2f);
            matrices.scale(1 + (animationProgress / 5));

            Render2dUtil.drawTexturedRect(
                    this.TEXTURE,
                    -width/2,
                    -height/2,
                    width,
                    height,
                    new Color(1,1,1,(1+animationProgress)/2)
            );
            matrices.popMatrix();
        }

        final Identifier TEXTURE = Identifier.of(Main.MOD_ID, "mainmenu/" + this.name().toLowerCase() + ".png");
        final Identifier BLURRED_TEXTURE = Identifier.of(Main.MOD_ID, "mainmenu/" + this.name().toLowerCase() + "blurred.png");
    }

    private static int[][] getExtraOptionsBounds(int index) {
        index -= 2;
        Rectangle dividerBounds = getDividerBounds();

        int diameter = (int) (getDividerBounds().y / 2);
        int gap = diameter / 2;
        int extras = Button.values().length - 2;

        int maxWidth = extras * diameter + ((extras-1) * (gap));

        int x = (C.res().getScaledWidth() / 2)
                - (maxWidth / 2)
                + ((diameter + gap) * index);
        int y = dividerBounds.height + ((dividerBounds.y - diameter) / 2);

        return new int[][] {new int[] {x, x+diameter, x+diameter, x}, new int[] {y, y, y+diameter, y+diameter}};
    }

    private static final int DIVIDER_THICKNESS = 5;
    private static Rectangle getDividerBounds() {
        int titleScreenDivider = (int)
                ((Math.clamp(EasingUtil.getAnimation(Button.SINGLEPLAYER.name()), 0, 1)
                - Math.clamp(EasingUtil.getAnimation(Button.MULTIPLAYER.name()), 0, 1))
                * C.res().getScaledWidth() / 10);

        int leftMinX = C.res().getScaledWidth() / 2;
        int leftWidth = C.res().getScaledWidth() - leftMinX + titleScreenDivider;

        int bottomHeight = C.res().getScaledHeight() / 4;
        int bottomRowY = C.res().getScaledHeight() - bottomHeight;

        return new Rectangle(leftMinX, bottomHeight, leftWidth, bottomRowY);
    }

    private static int[][] getSingleplayerPolygon() {
        Rectangle dividerBounds = getDividerBounds();

        return new int[][] {
                new int[] {0, 0, dividerBounds.x- DIVIDER_THICKNESS, dividerBounds.width- DIVIDER_THICKNESS},
                new int[] {0, dividerBounds.height, dividerBounds.height, 0}
        };
    }

    private static int[][] getMultiplayerPolygon() {
        Rectangle dividerBounds = getDividerBounds();

        return new int[][] {
                new int[] {dividerBounds.width, dividerBounds.x, C.res().getScaledWidth(), C.res().getScaledWidth()},
                new int[] {0, dividerBounds.height, dividerBounds.height, 0},
        };
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float deltaTicks) {}

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float deltaTicks) {
        RenderUtil.setDrawContext(context);

        RenderUtil.setDrawContext(context);

        // todo: make the singleplayer and multiplayer images panoramas, i cant figure out how to apply scissor or translate to it.
        //SINGLEPLAYER_PANORAMA.render(context, leftWidth, bottomRowY, true);
        //context.enableScissor(leftMinX, bottomRowY, leftWidth, bottomHeight);
        //MULTIPLAYER_PANORAMA.render(context, leftWidth, bottomRowY, true);
        //context.disableScissor();

        Rectangle dividerBounds = getDividerBounds();

        Render2dUtil.drawRect(
                0,
                dividerBounds.height,
                C.res().getScaledWidth(),
                dividerBounds.y,
                new Color(20, 20, 20)
        );

        Render2dUtil.drawRect(
                0,
                dividerBounds.height,
                C.res().getScaledWidth(),
                DIVIDER_THICKNESS,
                Color.WHITE
        );

        Render2dUtil.drawPolygon(
                new int[] {
                        dividerBounds.x - DIVIDER_THICKNESS,
                        dividerBounds.width - DIVIDER_THICKNESS,
                        dividerBounds.width,
                        dividerBounds.x
                },
                new int[] {
                        dividerBounds.height,
                        0,
                        0,
                        dividerBounds.height
                },
                Color.WHITE,
                false
        );

        boolean altsHovered = AltManager.isHoveringAlts();

        if (altsHovered) {
            int maxHeight = AltManager.getAltsHeight();
            AltManager.scrolledAmount += (int) (lastMouseScroll * maxHeight / 10);
        }

        for (Button button : Button.values()) {
            if (MouseUtil.isMouseInPolygon(button.getBounds()) && !altsHovered) button.onHover();
            else button.onNotHover();

            button.render();
        }

        AltManager.render();

        lastMouseScroll = 0;
    }


    @Override
    public boolean mouseClicked(Click click, boolean doubled) {
        return AltManager.handleClick(click) || handleClickingOptions(click);
    }

    private boolean handleClickingOptions(Click click) {
        if (click.button() != 0) return false;

        for (Button button : Button.values()) {
            if (MouseUtil.isMouseInPolygon(button.getBounds())) {
                button.onClick();
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        lastMouseScroll = verticalAmount;
        return true;
    }

    @Override
    protected void init() {
        AltManagerUtil.loadAlts();
    }
}