package com.scale.scalehackv4.utils.client;

import com.scale.scalehackv4.bridge.SelectedStackBridge;
import com.scale.scalehackv4.modules.impl.client.Targets;
import com.scale.scalehackv4.utils.C;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.FallingBlock;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.ItemEnchantmentsComponent;
import net.minecraft.component.type.PotionContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.ItemTags;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.EmptyBlockView;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.function.BiPredicate;
import java.util.function.Predicate;

// code from an unreleased injectable mushroom version
public class InventoryUtil {
    public static boolean isItemWeapon(ItemStack itemStack) {
        return (itemStack.isIn(ItemTags.SWORDS) && MultiSelectUtil.isEnabled(Targets.Valid_Weapons.Sword))
                || (getEnchantLevel(itemStack, Enchantments.KNOCKBACK) > 0&& MultiSelectUtil.isEnabled(Targets.Valid_Weapons.Knockback))
                || (itemStack.isIn(ItemTags.AXES) && MultiSelectUtil.isEnabled(Targets.Valid_Weapons.Axe))
                || (itemStack.getItem() == Items.TRIDENT && MultiSelectUtil.isEnabled(Targets.Valid_Weapons.Trident))
                || (itemStack.getItem() == Items.MACE && MultiSelectUtil.isEnabled(Targets.Valid_Weapons.Mace))
                || (itemStack.isIn(ItemTags.SPEARS) && MultiSelectUtil.isEnabled(Targets.Valid_Weapons.Spear))
                || (itemStack.isEmpty() && MultiSelectUtil.isEnabled(Targets.Valid_Weapons.Fist));
    }

    public static boolean isItemValidBlock(Item item) {
        return (
                // gotta be a block
                item instanceof BlockItem blockItem &&
                        // gotta NOT be sand
                        !(blockItem.getBlock() instanceof FallingBlock) &&
                        // gotta be a full block, not a sapling or smth
                        blockItem
                                .getBlock()
                                .getDefaultState()
                                .isFullCube(EmptyBlockView.INSTANCE, BlockPos.ORIGIN)
        );
    }

    // i hate this, thank you minecraft
    public static int localSlotToPacketSlot(int slot) {
        // hotbar
        if (slot <= 8) return slot + 36;
        // inventory
        if (slot < 36) return slot;
        // armor
        if (slot < 40) return 44 - slot;
        // i dont remember, maybe off hand
        if (slot == 40) return 45;
        return slot;
    }

    public static boolean isInventoryFull() {
        for (int i = 0; i < C.p().getInventory().getMainStacks().size(); i++) {
            if (C.p().getInventory().getMainStacks().get(i).isEmpty()) return false;
        }
        return true;
    }

    public static ArrayList<Integer> getBestBlocks(
            int arraySize,
            Inventory inventory
    ) {
        ArrayList<Integer> bestBlockSlots = new ArrayList<>();

        for (int i = 0; i < inventory.size(); i++) {
            ItemStack itemStack = inventory.getStack(i);
            if (InventoryUtil.isItemValidBlock(itemStack.getItem())) {
                if (
                        bestBlockSlots.size() < arraySize ||
                                itemStack.getCount() > bestBlockSlots.getFirst()
                ) {
                    if (bestBlockSlots.size() < arraySize) bestBlockSlots.add(
                            i
                    );
                    else bestBlockSlots.set(0, i);
                    bestBlockSlots.sort(
                            Comparator.comparingInt(
                                    e -> inventory.getStack(e).getCount()
                            )
                    );
                }
            }
        }

        return bestBlockSlots;
    }

    // EnchantmentHelper.getEnchantments(item).getEnchantments() returns a Set<RegistryEntry<Enchantment>> which I do NOT want.
    public static HashMap<RegistryKey<Enchantment>, Integer> getEnchants(
            ItemStack item
    ) {
        HashMap<RegistryKey<Enchantment>, Integer> enchantments =
                new HashMap<>();

        ItemEnchantmentsComponent enchantmentsComponent =
                EnchantmentHelper.getEnchantments(item);

        enchantmentsComponent
                .getEnchantments()
                .forEach(enchant -> {
                    enchantments.put(
                            enchant.getKey().get(),
                            enchantmentsComponent.getLevel(enchant)
                    );
                });

        return enchantments;
    }

    // minecraft get enchantmentlevel is EVIL
    // EnchantmentHelper.getEnchantments(item).getLevel(RegistryEntry<Enchantment> enchantment)
    // Enchantments.ENCHANT are RegistryKey's idk how to get a RegistryEntry
    public static int getEnchantLevel(
            ItemStack item,
            RegistryKey<Enchantment> enchantment
    ) {
        return getEnchants(item).getOrDefault(enchantment, 0);
    }

    public static int getPotionLevel(
            ItemStack item,
            RegistryEntry<StatusEffect> potion
    ) {
        for (StatusEffectInstance effect : item
                .getOrDefault(
                        DataComponentTypes.POTION_CONTENTS,
                        PotionContentsComponent.DEFAULT
                )
                .getEffects()) {
            if (effect.getEffectType() == potion) return effect.getAmplifier();
        }

        return 0;
    }

    public static double getSwordDamage(ItemStack item) {
        double baseDamage = item.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
                .modifiers()
                .stream()
                .filter(entry -> entry.attribute().equals(EntityAttributes.ATTACK_DAMAGE))
                .mapToDouble(entry -> entry.modifier().value())
                .sum();

        int sharpnessLevel = getEnchantLevel(item, Enchantments.SHARPNESS);
        // multiply by 2 because u can hit twice a second
        float sharpnessDPS =
                (sharpnessLevel == 0 ? 0 : 0.5f * sharpnessLevel + 0.5f) * 2;

        int fireAspectLevel = getEnchantLevel(item, Enchantments.FIRE_ASPECT);
        // fire aspect does not help in fights! when hurtframes end, the highest damage is applied, the highest damage is NEVER fire aspect!
        float fireAspectDPS = fireAspectLevel * 10e-4f;

        return baseDamage + sharpnessDPS + fireAspectDPS;
    }

    public static float getBowDamage(ItemStack item) {
        return getEnchantLevel(item, Enchantments.POWER);
    }

    // botmon ratioed me
    public static float getDurabilityRatio(ItemStack item) {
        // item damage = how many times its been used.
        return (
                (float) (item.getMaxDamage() - item.getDamage()) /
                        item.getMaxDamage()
        );
    }

    /// warning: if instant is true, it sends the update selected slot packet, avoid using twice in one tick.
    public static void swapSlot(int slot, boolean instant) {
        // don't swap to current slot.
        if (slot == C.p().getInventory().getSelectedSlot()) return;
        // don't swap to invalid slots.
        if (slot < 0 || slot > 9) return;

        C.p().getInventory().setSelectedSlot(slot);

        if (instant) {
            SelectedStackBridge.from(C.im()).bridge$setSelectedStack(C.p().getInventory().getStack(slot));
            SelectedStackBridge.from(C.im()).bridge$setLastSelectedSlot(slot);
            PacketUtil.sendImmediately(new UpdateSelectedSlotC2SPacket(slot));
        }
    }

    public static int getBestHotbarSlotToBreakBlock(Block block) {
        int slot = C.p().getInventory().getSelectedSlot();
        float bestSpeed = getBreakSpeed(
                C.p().getInventory().getStack(slot),
                block
        );

        for (int i = 0; i < 9; i++) {
            ItemStack itemStack = C.p().getInventory().getStack(i);
            float breakSpeed = getBreakSpeed(itemStack, block);
            if (breakSpeed > bestSpeed) {
                slot = i;
                bestSpeed = breakSpeed;
            }
        }

        return slot;
    }

    // todo: use this in a scaffold visual <3
    public static int getBlocksInInventory() {
        int amountOfBlocks = 0;

        for (int i = 0; i < 36; i++) {
            ItemStack itemStack = C.p().getInventory().getStack(i);
            if (
                    InventoryUtil.isItemValidBlock(itemStack.getItem())
            ) amountOfBlocks += itemStack.getCount();
        }

        return amountOfBlocks;
    }

    /// booleanPredicate checks if item is correct type.
    /// bestNumberPredicate is a predicate to check if it's better.
    /// pass screen as null if searching inventory
    public static int getBestItemSlot(
            Predicate<ItemStack> booleanPredicate,
            BiPredicate<ItemStack, ItemStack> bestNumberPredicate,
            Inventory inventory
    ) {
        int bestSlot = -1;

        int slots = inventory.size();

        for (int i = 0; i < slots; i++) {
            ItemStack itemStack = inventory.getStack(i);

            // checks if item is the type of item we are looking for!
            if (booleanPredicate.test(itemStack)) {
                // this is the first item of its type found, its automatically the best
                if (
                        bestSlot == -1 ||
                                // if we don't have this item already, check that this item is better than the previous best
                                bestNumberPredicate.test(
                                        itemStack,
                                        inventory.getStack(bestSlot)
                                )
                ) {
                    bestSlot = i;
                }
            }
        }

        return bestSlot;
    }

    // used for autotool!
    public static float getBreakSpeed(ItemStack item, Block block) {
        float efficiencyMulti = item.isSuitableFor(block.getDefaultState())
                ? (float) (Math.pow(
                getEnchantLevel(item, Enchantments.EFFICIENCY),
                2
        ) +
                           1)
                : 0;
        return (
                item.getMiningSpeedMultiplier(block.getDefaultState()) +
                        efficiencyMulti
        );
    }

    public static float getPickaxeBreakSpeed(ItemStack item) {
        return getBreakSpeed(item, Blocks.STONE);
    }

    public static float getAxeBreakSpeed(ItemStack item) {
        return getBreakSpeed(item, Blocks.OAK_LOG);
    }

    public static float getShovelBreakSpeed(ItemStack item) {
        return getBreakSpeed(item, Blocks.DIRT);
    }

    public static double getArmorProtection(ItemStack item) {
        double baseArmorProtection = item.getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT)
                .modifiers()
                .stream()
                .filter(entry -> entry.attribute().equals(EntityAttributes.ARMOR))
                .mapToDouble(entry -> entry.modifier().value())
                .sum();

        int protectionLevel = getEnchantLevel(item, Enchantments.PROTECTION);
        double protectionAmount = protectionLevel * 0.1f * baseArmorProtection;

        return baseArmorProtection + protectionAmount;
    }

    // shift clicks
    public static void quickMove(int slot, boolean inventory) {
        // minecraft prefers you to use their arbitrary, superfluous packet slot instead of local slot
        if (inventory) slot = InventoryUtil.localSlotToPacketSlot(slot);

        C.im()
                .clickSlot(
                        C.p().currentScreenHandler.syncId,
                        slot,
                        0,
                        SlotActionType.QUICK_MOVE,
                        C.p()
                );
    }

    // swaps for hotbar slots
    public static void swap(int startSlot, int endSlot) {
        // minecraft prefers you to use their arbitrary, superfluous packet slot instead of local slot
        int packetStartSlot = InventoryUtil.localSlotToPacketSlot(startSlot);

        C.im()
                .clickSlot(
                        C.p().currentScreenHandler.syncId,
                        packetStartSlot,
                        endSlot,
                        SlotActionType.SWAP,
                        C.p()
                );
    }

    public static void drop(int slot) {
        int packetSlot = localSlotToPacketSlot(slot);

        C.im()
                .clickSlot(
                        C.p().currentScreenHandler.syncId,
                        packetSlot,
                        1,
                        SlotActionType.THROW,
                        C.p()
                );
    }
}