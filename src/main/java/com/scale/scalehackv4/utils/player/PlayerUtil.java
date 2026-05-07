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
            UseItemBridge.from(C.mc).bridge$doAttack();
            onAttack.forEach(Runnable::run);
            onAttack.clear();

            return true;
        }
        return false;
    }, 9999);
}