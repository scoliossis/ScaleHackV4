package com.scale.scalehackv4.modules;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

// todo: add order variable to sort by for gui, so obf can shuffle their positions in the files
@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterSubModule {
    String name();
    String parent() default "";
    String[] modeParentString() default "";
    // stupid ass name, i lwk give up on making ts code good.
    String[] NOTmodeParentString() default "";
    boolean hasKeybind() default false;

    double min() default 0;
    double max() default 1;
    double increment() default 0.01;
}
