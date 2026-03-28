package com.scale.scalehackv4.utils.skyblock;

import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.Node;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

import java.util.function.Predicate;

public class ChainUtil {
    public record InteractionChainNode(Rotation rotation, BlockPos targetPos, Node.Type itemType) {}
    public record InteractionChain(InteractionChainNode[] interactionChainNodes, Vec3d endPos) {}
    public record QueuedInteractionChain(InteractionChain interactionChain, Predicate<?> activationPredicate) {}
}
