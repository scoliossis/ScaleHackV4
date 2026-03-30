package com.scale.scalehackv4.events.impl.client;


import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;
import lombok.AllArgsConstructor;
import net.minecraft.util.math.ChunkPos;

import java.util.function.Predicate;

public class ChunkLoadEvent extends Event {
    @AllArgsConstructor
    public static class Params {
        public ChunkPos chunkPos;
    }

    public ChunkLoadEvent(Module parent, Predicate<Params> predicate, int priority) {
        super(parent, predicate, priority);
    }

    public ChunkLoadEvent(Module parent, Predicate<Params> predicate) {
        super(parent, predicate);
    }
}
