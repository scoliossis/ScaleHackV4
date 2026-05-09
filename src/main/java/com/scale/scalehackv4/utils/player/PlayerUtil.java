package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.bridge.UseItemBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.utils.C;

import java.util.ArrayList;

@RegisterListeners
public class PlayerUtil {
    public static ArrayList<Runnable> onAttack = new ArrayList<>();

    private final Event clearQueuedAttack = new RotationEvent(null, params -> {
        onAttack.clear();
        return false;
    }, 1);

    private final Event handleAttack = new CheckClickEvent.Left(null, params -> {
        if (!onAttack.isEmpty()) {
            // stops "this.handleBlockBreaking(this.currentScreen == null && !bl3 && this.options.attackKey.isPressed() && this.mouse.isCursorLocked());" causing a cooldown
            C.mc.options.attackKey.setPressed(false);
            UseItemBridge.from(C.mc).bridge$doAttack();
            onAttack.forEach(Runnable::run);
            onAttack.clear();

            return true;
        }
        return false;
    }, 9999);
}