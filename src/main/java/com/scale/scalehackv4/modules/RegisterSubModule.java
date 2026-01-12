package com.scale.scalehackv4.modules;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface RegisterSubModule {
    String name();
    String parent() default "";
}
