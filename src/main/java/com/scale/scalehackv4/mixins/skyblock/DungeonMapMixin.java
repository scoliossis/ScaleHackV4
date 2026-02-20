package com.scale.scalehackv4.mixins.skyblock;

import com.scale.scalehackv4.bridge.MapDecorationsBridge;
import net.minecraft.item.map.MapDecoration;
import net.minecraft.item.map.MapState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

import java.util.Map;

@Mixin(MapState.class)
public class DungeonMapMixin implements MapDecorationsBridge {
    @Shadow @Final Map<String, MapDecoration> decorations;

    @Override
    public Map<String, MapDecoration> bridge$getDecorations() {
        return this.decorations;
    }
}
