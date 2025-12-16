package com.scale.scalehackv4.utils;

import com.scale.scalehackv4.Main;
import org.reflections.Reflections;

public class C {
    public static final Reflections REFLECTIONS = new Reflections(Main.class.getPackage().getName());
}
