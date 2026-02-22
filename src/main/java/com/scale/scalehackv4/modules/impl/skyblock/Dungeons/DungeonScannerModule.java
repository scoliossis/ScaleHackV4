package com.scale.scalehackv4.modules.impl.skyblock.Dungeons;

import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.MapParser;

@RegisterModule(
        name = "Dungeon Scanner",
        category = Category.SKYBLOCK
)
// todo: wither door esp
public class DungeonScannerModule extends Module {
    @Override
    public void onEnable() {

    }

    @Override
    public void onDisable() {
        MapParser.MAP_PLAYERS.clear();
    }
}