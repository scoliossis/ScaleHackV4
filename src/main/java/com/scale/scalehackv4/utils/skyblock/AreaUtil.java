package com.scale.scalehackv4.utils.skyblock;

import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.render.RenderScoreboardEvent;

@RegisterListeners
public class AreaUtil {
    public static boolean isInDungeons() {
        for (RenderScoreboardEvent.SidebarEntry sidebarEntry : RenderScoreboardEvent.LAST_SIDEBAR) {
            if (sidebarEntry.name().getString().matches("( )+§7⏣ §cThe Catac§[a-zA-Z0-9]§combs §7\\([MFE].+")) {
                return true;
            }
        }

        return false;
    }
}
