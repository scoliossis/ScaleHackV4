package com.scale.scalehackv4.mixins.render;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.impl.render.RenderScoreboardEvent;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.hud.InGameHud;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.scoreboard.ScoreboardEntry;
import net.minecraft.scoreboard.ScoreboardObjective;
import net.minecraft.scoreboard.Team;
import net.minecraft.scoreboard.number.NumberFormat;
import net.minecraft.scoreboard.number.StyledNumberFormat;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Comparator;

@Mixin(InGameHud.class)
public abstract class RenderScoreboardEventMixin {
    @Shadow
    public abstract TextRenderer getTextRenderer();

    @Shadow
    @Final
    private static Comparator<ScoreboardEntry> SCOREBOARD_ENTRY_COMPARATOR;

    @Inject(method = "renderScoreboardSidebar(Lnet/minecraft/client/gui/DrawContext;Lnet/minecraft/scoreboard/ScoreboardObjective;)V", at = @At("HEAD"), cancellable = true)
    private void renderScoreboardSidebar(DrawContext context, ScoreboardObjective objective, CallbackInfo ci) {
        Scoreboard scoreboard = objective.getScoreboard();
        NumberFormat numberFormat = objective.getNumberFormatOr(StyledNumberFormat.RED);

        // tried using some magic to steal this function with a redirect, but it uses a local record.
        RenderScoreboardEvent.SidebarEntry[] sidebarEntrys = scoreboard.getScoreboardEntries(objective)
                .stream()
                .filter(score -> !score.hidden())
                .sorted(SCOREBOARD_ENTRY_COMPARATOR)
                .limit(15L)
                .map(scoreboardEntry -> {
                    Team team = scoreboard.getScoreHolderTeam(scoreboardEntry.owner());
                    Text textx = scoreboardEntry.name();
                    Text text2 = Team.decorateName(team, textx);
                    Text text3 = scoreboardEntry.formatted(numberFormat);
                    int ix = this.getTextRenderer().getWidth(text3);
                    return new RenderScoreboardEvent.SidebarEntry(text2, text3);
                })
                .toArray(RenderScoreboardEvent.SidebarEntry[]::new);

        if (Bus.post(RenderScoreboardEvent.class, new RenderScoreboardEvent.Params(sidebarEntrys))) ci.cancel();
        RenderScoreboardEvent.LAST_SIDEBAR = sidebarEntrys;
    }
}
