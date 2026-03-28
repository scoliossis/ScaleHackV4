package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.PacketEvent;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.ChainUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.Node;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.NodeUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.awt.*;
import java.util.ArrayList;
import java.util.Comparator;

// todo: fix secrets awaiting to allow secrets to be gotten before the node is reached
//  change rotations to be serverside
//
public class AutoRoutes extends SecretsHelper {
    @RegisterSubModule(name = "Auto Routes", hasKeybind = true)
    public static boolean autoroutes = true;

    @RegisterSubModule(name = "Timeout", parent = "Auto Routes", max = 5000, increment = 100)
    public static long timeout = 1000;

    @RegisterSubModule(name = "Show Auto Routes", hasKeybind = true)
    public static boolean showAutoRoutes = true;
    @RegisterSubModule(name = "Etherwarp Node Colour", parent = "Show Auto Routes")
    public static Color etherwarpNodeColour = new Color(135, 148, 255, 92);
    @RegisterSubModule(name = "Line Colour", parent = "Show Auto Routes")
    public static Color lineColours = new Color(198, 0, 255, 92);
    @RegisterSubModule(name = "Pearl Colour", parent = "Show Auto Routes")
    public static Color pearlColour = new Color(113, 0, 255, 92);
    @RegisterSubModule(name = "Stonk Colour", parent = "Show Auto Routes")
    public static Color stonkColour = new Color(255, 51, 51, 92);
    @RegisterSubModule(name = "Await Secret Colour", parent = "Show Auto Routes")
    public static Color awaitSecretColour = new Color(255, 215, 0, 92);

    private static ChainUtil.InteractionChain interactionChain;
    private static int interactionChainIndex = 0;
    private static final ArrayList<Long> expectedPackets = new ArrayList<>();
    // u can receive secrets before we expect them, so make sure we take care of that
    private static final ArrayList<Long> interactedSecrets = new ArrayList<>();
    private static final ArrayList<ChainUtil.InteractionChainNode> pendingInteractions = new ArrayList<>();
    private static Node.Type lastNode = null;

    private static boolean finishedLastChain = true;

    public static void disable() {
        interactionChain = null;
        interactionChainIndex = 0;
    }

    private final Event onRotateEvent = new RotationEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        // handle received secrets
        while (lastNode == Node.Type.SECRET && !interactedSecrets.isEmpty()) {
            interactedSecrets.removeFirst();
            expectedPackets.removeFirst();
        }

        // forget failed teleports
        while (!expectedPackets.isEmpty() && System.currentTimeMillis() - expectedPackets.getFirst() > timeout) {
            expectedPackets.removeFirst();
        }
        if (expectedPackets.isEmpty()) finishedLastChain = true;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) {
            disable();
            return false;
        }
        if (!finishedLastChain) return false;

        interactionChain = getInteractionChain(C.p().getEntityPos().subtract(0, 1, 0));
        if (interactionChain == null) return false;

        Node.Type nodeItem = interactionChain.interactionChainNodes()[interactionChainIndex].itemType();
        if (!nodeItem.getItem.test(null)) return false;

        for (; interactionChainIndex < interactionChain.interactionChainNodes().length; interactionChainIndex++) {
            ChainUtil.InteractionChainNode node = interactionChain.interactionChainNodes()[interactionChainIndex];
            // take a tick gap between switching items
            if (node.itemType() != nodeItem) return false;

            pendingInteractions.add(node);
            if (nodeItem == Node.Type.SECRET) {
                for (int i = 0; i < node.targetPos().getX(); i++) {
                    expectedPackets.add(System.currentTimeMillis());
                }
            }
            else expectedPackets.add(System.currentTimeMillis());

            finishedLastChain = false;
            // todo: ik u can nuke somehow, but im VERY scared to test multi swinging cause i KNOW that insta bans outside sb
            if (nodeItem == Node.Type.DUNGEON_BREAKER)
                break;
        }

        disable();

        return false;
    });

    private final Event fixMovement = new KeyboardInputEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (expectedPackets.isEmpty()) return false;
        params.backKey = params.rightKey = params.leftKey = params.forwardKey = params.jumpKey = false;

        params.sneakKey = !pendingInteractions.isEmpty() && pendingInteractions.getFirst().itemType() == Node.Type.ETHERWARP;

        return false;
    });

    private final Event executeTeleports = new PlayerUpdateEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        while (!pendingInteractions.isEmpty()) {
            if (pendingInteractions.getFirst().itemType() == Node.Type.ETHERWARP && !C.p().isSneaking()) return false;
            ChainUtil.InteractionChainNode node = pendingInteractions.removeFirst();

            Rotation rotation = NodeUtil.flipRotation(node.rotation(), Direction.NORTH, AreaUtil.getCurrentRoom().facing);
            node.itemType().attemptRoute.test(rotation, node.targetPos());
            lastNode = node.itemType();
        }

        return false;
    });

    private final Event handleReceive = new PacketEvent.Received(null, params -> {
        if (!autoroutes) return false;
        if (params.packet instanceof ItemPickupAnimationS2CPacket) {
            interactedSecrets.add(System.currentTimeMillis());
        }
        if (params.packet instanceof EntityStatusS2CPacket entityDamageS2CPacket && entityDamageS2CPacket.getStatus() == 3 && entityDamageS2CPacket.getEntity(C.w()) instanceof BatEntity) {
            interactedSecrets.add(System.currentTimeMillis());
        }

        if (expectedPackets.isEmpty() || lastNode == null) return false;

        switch (lastNode) {
            case ETHERWARP, PEARL -> {
                if (params.packet instanceof PlayerPositionLookS2CPacket) {
                    expectedPackets.removeFirst();
                }
            }
            case DUNGEON_BREAKER -> {
                if (params.packet instanceof BlockUpdateS2CPacket blockUpdateS2CPacket) {
                    // todo: better check than distance <3
                    if (Math.sqrt(blockUpdateS2CPacket.getPos().getSquaredDistance(C.p().getEntityPos())) < 4.5) {
                        expectedPackets.removeFirst();
                    }
                }
            }
        }

        if (expectedPackets.isEmpty() && pendingInteractions.isEmpty()) {
            finishedLastChain = true;
            return false;
        }

        return false;
    });

    private final Event awaitSecretInteraction = new PacketEvent.Sent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (!autoroutes) return false;

        if (params.packet instanceof PlayerInteractBlockC2SPacket blockInteractPacket) {
            Block blockInteracted = C.w().getBlockState(blockInteractPacket.getBlockHitResult().getBlockPos()).getBlock();

            if (blockInteracted == Blocks.PLAYER_HEAD || blockInteracted == Blocks.PLAYER_WALL_HEAD || blockInteracted == Blocks.LEVER || blockInteracted == Blocks.CHEST || blockInteracted == Blocks.TRAPPED_CHEST) {
                interactedSecrets.add(System.currentTimeMillis());
            }
        }

        return false;
    });

    private final Event renderWorldEvent = new RenderWorldEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        renderAutoroutes();

        return false;
    });

    private ChainUtil.InteractionChain getInteractionChain(Vec3d playerPos) {
        if (!autoroutes) return null;
        if (interactionChain != null) return interactionChain;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) return null;

        ArrayList<ChainUtil.InteractionChainNode> nodes = new ArrayList<>();
        ArrayList<Node> passedNodes = new ArrayList<>();

        int posPriority = 0;
        Node currentNode = getNodeAt(currentRoom, playerPos, 0);
        while (currentNode != null) {
            // stops infinite looping routes crashing
            if (passedNodes.contains(currentNode)) break;

            passedNodes.add(currentNode);

            nodes.add(new ChainUtil.InteractionChainNode(currentNode.rotation, currentNode.targetPos, currentNode.type));
            if (currentNode.type == Node.Type.ETHERWARP || currentNode.type == Node.Type.PEARL) {
                playerPos = NodeUtil.getNodePos(currentNode.targetPos, currentRoom).toBottomCenterPos();
                posPriority = 0;
            }
            else posPriority++;
            currentNode = getNodeAt(currentRoom, playerPos, posPriority);
        }

        interactionChainIndex = 0;
        return nodes.isEmpty() ? null : new ChainUtil.InteractionChain(nodes.toArray(ChainUtil.InteractionChainNode[]::new), null);
    }

    private Node getNodeAt(Room currentRoom, Vec3d playerPos, int posPriority) {
        ArrayList<Node> nodes = new ArrayList<>();
        for (int i = 0; i < currentRoom.nodes.size(); i++) {
            Node node = currentRoom.nodes.get(i);

            BlockPos nodePos = NodeUtil.getNodePos(node.startPos, currentRoom);
            if (BlockPos.ofFloored(playerPos).equals(nodePos) && isValid(node, playerPos)) {
                nodes.add(node);
            }
        }
        nodes.sort(Comparator.comparingInt(e -> e.type.priority));

        return posPriority >= nodes.size() || nodes.isEmpty() ? null : nodes.get(posPriority);
    }

    // todo: fuh ts prob
    private boolean isValid(Node node, Vec3d playerPos) {
        Room room = AreaUtil.getCurrentRoom();
        if (room == null) return false;

        if (node.type == Node.Type.SECRET && lastNode == Node.Type.SECRET) return false;

        BlockPos nodePos = NodeUtil.getNodePos(node.targetPos, room);
        Rotation rotation = NodeUtil.flipRotation(node.rotation, Direction.NORTH, room.facing);
        Vec3d playerPosVec = playerPos.add(0, 1, 0);

        switch (node.type) {
            case ETHERWARP -> {
                Vec3i etherwarpBlock = EtherwarpUtil.getEtherwarpBlockSuccess(rotation.pitch, rotation.yaw, playerPosVec, 57);
                return etherwarpBlock != null && new BlockPos(etherwarpBlock).equals(nodePos);
            }
            case DUNGEON_BREAKER -> {
                BlockHitResult blockHitResult = WorldUtil.raytrace(playerPosVec.add(0, C.p().getEyeHeight(EntityPose.STANDING), 0), C.p().getBlockInteractionRange(), rotation);
                return blockHitResult.getType() != HitResult.Type.MISS && blockHitResult.getBlockPos().equals(nodePos);
            }
            // todo: check ender pearl is valid
        }

        return true;
    }

    private static void renderAutoroutes() {
        if (!showAutoRoutes) return;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) return;

        for (Node node : currentRoom.nodes) {
            Color startNodeColour = node.type == Node.Type.SECRET ? awaitSecretColour
                    : node.type == Node.Type.PEARL ? pearlColour
                    : node.type == Node.Type.DUNGEON_BREAKER ? stonkColour
                    : etherwarpNodeColour;

            BlockPos nodePos = NodeUtil.getNodePos(node.startPos, currentRoom);
            Render3dUtil.drawBox(nodePos.getX(), nodePos.getY(), nodePos.getZ(), 1, 1, 1, startNodeColour, false);

            if (node.type != Node.Type.ETHERWARP) continue;

            BlockPos targetNodePos = NodeUtil.getNodePos(node.targetPos, currentRoom);
            Render3dUtil.drawLine(nodePos.toCenterPos().toVector3f(), targetNodePos.toCenterPos().toVector3f(), lineColours);
        }
    }
}
