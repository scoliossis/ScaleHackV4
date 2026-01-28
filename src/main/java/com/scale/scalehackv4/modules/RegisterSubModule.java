package com.scale.scalehackv4.modules;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterSubModule {
    String name();
    String parent() default "";
    String[] modeParentString() default "";
    boolean hasKeybind() default false;

    double min() default 0;
    double max() default 1;
    double increment() default 0.01;
}
