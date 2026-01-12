package com.scale.scalehackv4.utils.render;

import lombok.AllArgsConstructor;
import net.minecraft.util.math.Box;

@AllArgsConstructor
public class Vec3f {
    public float x, y, z;

    public static final Vec3f ZERO = new Vec3f(0, 0, 0);

    public static Vec3f middle(Box box) {
        return lerpBox(box, 0.5f, 0.5f, 0.5f);
    }

    public static Vec3f lerpBox(Box box, float x, float y, float z) {
        return new Vec3f(
                (float) ((box.maxX * x) + (box.minX * (1 - x))),
                (float) ((box.maxY * y) + (box.minY * (1 - y))),
                (float) ((box.maxZ * z) + (box.minZ * (1 - z)))
        );
    }
}
