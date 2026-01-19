package com.scale.scalehackv4.modules;

import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.ArrayList;

public class SubModule {
    private final Module parentModule;

    private final Field field;
    @Getter private final ArrayList<SubModule> children;

    @Setter private SubModule parent;

    @Getter public final RegisterSubModule annotation;

    public SubModule(Module parentModule, Field field) {
        this.parentModule = parentModule;
        this.field = field;
        this.children = new ArrayList<>();
        this.annotation = field.getAnnotation(RegisterSubModule.class);
    }
}
