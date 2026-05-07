package com.scale.scalehackv4.utils.client;

import java.util.HashSet;

// todo: terrible impl, maybe use HashSet instead of enum? idk, i dont like anything about this
public class MultiSelectUtil {
    public static boolean isEnabled(Enum<?> value) {
        return ENABLED_ENUMS.contains(value);
    }
    public static HashSet<Enum<?>> ENABLED_ENUMS = new HashSet<>();
}
