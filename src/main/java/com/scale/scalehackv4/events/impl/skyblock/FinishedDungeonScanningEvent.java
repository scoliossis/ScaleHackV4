package com.scale.scalehackv4.events.impl.skyblock;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.modules.Module;

import java.util.function.Predicate;

public class FinishedDungeonScanningEvent extends Event {
    public static class Params {}

    public FinishedDungeonScanningEvent(Module parent, Predicate<Params> predicate, int priority) {
        super(parent, predicate, priority);
    }

    public FinishedDungeonScanningEvent(Module parent, Predicate<Params> predicate) {
        super(parent, predicate);
    }
}
