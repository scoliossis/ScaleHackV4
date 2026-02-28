package com.scale.scalehackv4.events.impl.player;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.utils.player.Rotation;
import lombok.AllArgsConstructor;

import java.util.function.Predicate;

public class RotationEvent extends Event {
    @AllArgsConstructor
    public static class Params {
        public Rotation rotation;
    }

    public RotationEvent(Module parent, Predicate<Params> predicate) {
        super(parent, predicate);
    }

    public static Rotation lastRotation;
}

