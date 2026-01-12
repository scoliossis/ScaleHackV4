package com.scale.scalehackv4.utils.render;

import java.util.concurrent.Callable;
import java.util.function.Predicate;

public class Draggable {
    public String id;
    public Callable<int[]> render;
    /// conditions to render if chat is NOT opened
    public Predicate<?> canRender;
    /// conditions to render if chat IS open
    public Predicate<?> conditions;

    public double x = 0.1;
    public double y = 0.1;

    public Draggable(String id, Callable<int[]> render, Predicate<?> canRender, Predicate<?> conditions) {
        this.id = id;
        this.render = render;
        this.canRender = canRender;
        this.conditions = conditions;

        DraggableRenderer.draggables.add(this);
    }

    public int[] render() {
        try {
            return this.render.call();
        } catch (Exception e) {
            return new int[] {0, 0};
        }
    }
}