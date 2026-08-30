package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.InventoryUtil;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.tag.FluidTags;
import net.minecraft.util.math.BlockPos;

// mushroom recode stolen code
public class MiningUtil {
    // minecraft's calc block delta code, stole so i can change which slot
    // AbstractBlock's calcBlockBreakingDelta is stolen
    public static float calcBlockBreakingDelta(BlockPos pos, ItemStack slot) {
        BlockState blockState = C.w().getBlockState(pos);
        float hardness = blockState.getHardness(C.w(), pos);
        if (hardness == -1.0F) {
            return 0.0F;
        } else {
            //int i = player.canHarvest(state) ? 30 : 100;
            int i = (!blockState.isToolRequired() ||
                    slot.isSuitableFor(blockState))
                    ? 30
                    : 100;

            float blockBreakingSpeed = slot.getMiningSpeedMultiplier(
                    blockState
            );
            if (blockBreakingSpeed > 1.0F) {
                float efficiencyMulti = (float) (Math.pow(
                        InventoryUtil.getEnchantLevel(
                                slot,
                                Enchantments.EFFICIENCY
                        ),
                        2
                ) +
                        1);
                blockBreakingSpeed +=
                        efficiencyMulti == 1 ? 0 : efficiencyMulti;
            }

            if (StatusEffectUtil.hasHaste(C.p())) {
                blockBreakingSpeed *=
                        1.0F +
                                (float) (StatusEffectUtil.getHasteAmplifier(C.p()) + 1) * 0.2F;
            }

            if (C.p().hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
                float g =
                        switch (
                                C.p()
                                        .getStatusEffect(StatusEffects.MINING_FATIGUE)
                                        .getAmplifier()
                                ) {
                            case 0 -> 0.3F;
                            case 1 -> 0.09F;
                            case 2 -> 0.0027F;
                            default -> 8.1E-4F;
                        };

                blockBreakingSpeed *= g;
            }

            blockBreakingSpeed *=
                    (float) C.p()
                            .getAttributeValue(EntityAttributes.BLOCK_BREAK_SPEED);
            if (C.p().isSubmergedIn(FluidTags.WATER)) {
                blockBreakingSpeed *=
                        (float) C.p()
                                .getAttributeInstance(
                                        EntityAttributes.SUBMERGED_MINING_SPEED
                                )
                                .getValue();
            }

            if (!C.p().isOnGround()) {
                blockBreakingSpeed /= 5.0F;
            }

            return blockBreakingSpeed / hardness / (float) i;
        }
    }
}
