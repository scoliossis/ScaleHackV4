package com.scale.scalehackv4.utils.player;

import lombok.AllArgsConstructor;

// https://github.com/scoliossis/ScaleHackOpenSrc/blob/master/src/main/java/com/github/scoliossis/utils/minecraft/RotationUtil.java#L124
@AllArgsConstructor
public class Rotation {
    public float yaw;
    public float pitch;

    public Rotation difference(Rotation other) {
        return new Rotation(other.yaw - yaw, other.pitch - pitch);
    }

    public Rotation add(Rotation other) {
        return new Rotation(yaw + other.yaw, pitch + other.pitch);
    }

    public Rotation subtract(Rotation other) {
        return new Rotation(yaw - other.yaw, pitch - other.pitch);
    }

    @Override
    public String toString() {
        return "Rotation(" + yaw + ", " + pitch + ")";
    }
}
