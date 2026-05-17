package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.bridge.UseItemBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.ClientTickEvent;
import com.scale.scalehackv4.events.impl.player.CheckClickEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.utils.C;
import lombok.Getter;
import net.minecraft.client.option.KeyBinding;

import java.util.ArrayList;
import java.util.HashMap;

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


    @Getter
    private static final HashMap<Integer, Boolean> overriddenKeybinds = new HashMap<>();

    public static void oneTickKeybind(KeyBinding keyBinding, boolean to) {
        overriddenKeybinds.put(keyBinding.hashCode(), to);
    }

    private final Event onClientTick = new ClientTickEvent(null, params -> {
        overriddenKeybinds.clear();
        return false;
    }, 1);
}