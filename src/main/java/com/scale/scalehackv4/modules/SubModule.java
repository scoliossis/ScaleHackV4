package com.scale.scalehackv4.modules;

import com.scale.scalehackv4.utils.client.MathUtil;
import lombok.Getter;
import lombok.Setter;

import java.lang.reflect.Field;
import java.util.ArrayList;

@Getter
public class SubModule {
    private final Module parentModule;

    private final Field field;
    private final ArrayList<SubModule> children;

    @Setter private SubModule parent;

    private final RegisterSubModule annotation;

    public SubModule(Module parentModule, Field field) {
        this.parentModule = parentModule;
        this.field = field;
        this.children = new ArrayList<>();
        this.annotation = field.getAnnotation(RegisterSubModule.class);
    }

    public Object get() {
        try {
            return field.get(parentModule);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void set(Object value) {
        try {
            // this is copied from scalehack, boring.
            if (isSlider()) {
                double doubleValue = Math.clamp(
                        MathUtil.roundTo(
                                MathUtil.toNearest(Double.parseDouble(value.toString()), annotation.increment()),
                                String.valueOf(annotation.increment()).split("\\.")[1].length()
                        ),
                        annotation.min(),
                        annotation.max()
                );

                if (field.getType() == double.class) field.set(parentModule, doubleValue);
                else if (field.getType() == float.class) field.set(parentModule, (float) doubleValue);
                else if (field.getType() == long.class) field.set(parentModule, (long) doubleValue);
                else field.set(parentModule, (int) doubleValue);

                return;
            }

            field.set(parentModule, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public boolean isSlider() {
        return this.getField().getType() == double.class
                || this.getField().getType() == float.class
                || this.getField().getType() == long.class
                || this.getField().getType() == int.class;
    }

    public boolean open = false;
}
