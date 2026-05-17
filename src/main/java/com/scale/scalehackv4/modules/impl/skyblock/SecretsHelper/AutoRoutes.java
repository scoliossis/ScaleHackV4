package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.ClickedEvent;
import com.scale.scalehackv4.events.impl.player.RotationEvent;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import com.scale.scalehackv4.utils.render.RenderUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.ChainUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.Node;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.NodeUtil;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.network.packet.Packet;
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
import net.minecraft.util.shape.VoxelShape;

import java.awt.*;
import java.util.*;
import java.util.List;
import java.util.function.Predicate;

public class AutoRoutes extends SecretsHelper {
    @RegisterSubModule(name = "Auto Routes", hasKeybind = true)
    public static boolean autoroutes = true;

    @RegisterSubModule(name = "Timeout", parent = "Auto Routes", max = 5000, increment = 100)
    public static long timeout = 1000;
    @RegisterSubModule(name = "Secret Timeout", parent = "Auto Routes", max = 5000, increment = 100)
    public static long secretTimeout = 2000;

    @RegisterSubModule(name = "Left Click Skip", parent = "Auto Routes")
    public static boolean leftClickSkip = true;
    @RegisterSubModule(name = "Zero Tick Stonk", parent = "Auto Routes", hasKeybind = true)
    public static boolean zeroTickStonk = true;

    @RegisterSubModule(name = "Show Auto Routes", hasKeybind = true)
    public static boolean showAutoRoutes = true;
    @RegisterSubModule(name = "Stonk Colour", parent = "Show Auto Routes")
    public static Color stonkColour = new Color(255, 51, 51, 92);
    @RegisterSubModule(name = "Secrets Colour", parent = "Show Auto Routes")
    public static Color secretsColour = new Color(0, 76, 255, 92);
    @RegisterSubModule(name = "Other Node Colour", parent = "Show Auto Routes")
    public static Color otherNodeColour = new Color(140, 0, 255, 92);

    private final Event onRotateEvent = new RotationEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (!ChainUtil.INTERACTION_CHAINS.isEmpty() || !ChainUtil.EXPECTED_PACKETS.isEmpty()) return false;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) return false;

        getInteractionChain(C.p().getEntityPos().subtract(0, 1, 0));

        return false;
    });

    // todo: check that ur actually routing...
    private final Event handleLeftClick = new ClickedEvent.Left(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (!leftClickSkip || ChainUtil.EXPECTED_PACKETS.isEmpty()) return false;

        ChainUtil.forgetExpecting();
        return false;
    });

    public static boolean secretPacketsPredicate(Packet<?> packet) {
        if (packet instanceof ItemPickupAnimationS2CPacket) {
            return true;
        }
        if (packet instanceof EntityStatusS2CPacket entityDamageS2CPacket && entityDamageS2CPacket.getStatus() == 3 && entityDamageS2CPacket.getEntity(C.w()) instanceof BatEntity) {
            return true;
        }

        if (packet instanceof PlayerInteractBlockC2SPacket blockInteractPacket) {
            Block blockInteracted = C.w().getBlockState(blockInteractPacket.getBlockHitResult().getBlockPos()).getBlock();

            return blockInteracted == Blocks.PLAYER_HEAD || blockInteracted == Blocks.PLAYER_WALL_HEAD || blockInteracted == Blocks.LEVER || blockInteracted == Blocks.CHEST || blockInteracted == Blocks.TRAPPED_CHEST;
        }

        return false;
    }

    private final Event renderWorldEvent = new RenderWorldEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        renderAutoroutes();

        return false;
    });

    private static int priority = 0;

    private void getInteractionChain(Vec3d playerPos) {
        if (!autoroutes) return;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) return;

        ArrayList<ArrayList<Node>> groupedNodes = getChainedNodes(currentRoom, playerPos);
        if (groupedNodes.isEmpty()) return;

        for (ArrayList<Node> nodeGroup : groupedNodes) {
            Node.Type nodeGroupType = nodeGroup.getFirst().type;
            BlockPos startNodePos = NodeUtil.getNodePos(nodeGroup.getFirst().startPos, currentRoom);
            ChainUtil.UpdatedInteractionChain interactionChain = getInteractionChain(nodeGroupType, startNodePos);

            // zero tick stonk still god bypasses
            if (zeroTickStonk && nodeGroupType == Node.Type.DUNGEON_BREAKER) {
                interactionChain.action().add(() -> {
                    for (Node node : nodeGroup) {
                        Rotation rotation = NodeUtil.flipRotation(node.rotation, Direction.NORTH, currentRoom.facing);
                        node.type.attemptRoute.test(rotation, node.targetPos);
                    }
                });
            }
            else for (Node node : nodeGroup) {
                Rotation rotation = NodeUtil.flipRotation(node.rotation, Direction.NORTH, currentRoom.facing);
                interactionChain.action().add(() -> node.type.attemptRoute.test(rotation, node.targetPos));
            }

            ChainUtil.INTERACTION_CHAINS.add(interactionChain);
        }
    }

    private ChainUtil.UpdatedInteractionChain getInteractionChain(Node.Type nodeType, BlockPos startNodePos) {
        double distanceToNode = startNodePos.toBottomCenterPos().add(0,1,0).distanceTo(C.p().getEntityPos());
        Predicate<?> getItem = nodeType.getItem;
        Predicate<?> shouldSend = nodeType == Node.Type.SECRET ? e -> true : e -> ChainUtil.EXTERNAL_EXPECTED.isEmpty() && distanceToNode <= 1.5;
        Predicate<?> shouldCancel = e -> ChainUtil.EXPECTED_PACKETS.isEmpty() && distanceToNode > 1.5;
        boolean shouldBlink = false;
        Predicate<?> cancelBlink = e -> false;
        boolean shouldCrouch = nodeType == Node.Type.ETHERWARP;

        Predicate<Packet<?>> expectedPacket = switch (nodeType) {
            case ETHERWARP, AOTE, PEARL, HYPERION -> e -> e instanceof PlayerPositionLookS2CPacket;
            case DUNGEON_BREAKER, SUPERBOOM -> e ->
                    e instanceof BlockUpdateS2CPacket blockUpdateS2CPacket
                    // todo: better check than distance <3
                    && Math.sqrt(blockUpdateS2CPacket.getPos().getSquaredDistance(C.p().getEntityPos())) < C.p().getBlockInteractionRange();
            default -> e -> true;
        };

        return new ChainUtil.UpdatedInteractionChain(new ArrayList<>(), getItem, shouldSend, shouldCancel, shouldBlink, cancelBlink, expectedPacket, timeout, shouldCrouch);
    }

    // code from before porting to chainutil, ig its fine...
    private ArrayList<ArrayList<Node>> getChainedNodes(Room currentRoom, Vec3d playerPos) {
        HashSet<Node> passedNodes = new HashSet<>();
        ArrayList<ArrayList<Node>> nodes = new ArrayList<>();

        priority = 0;
        Node currentNode = getNodeAt(currentRoom, playerPos);
        Node.Type lastNodeType = null;
        while (currentNode != null) {
            priority++;

            // stops infinite looping routes crashing
            if (passedNodes.contains(currentNode)) break;

            passedNodes.add(currentNode);

            if (currentNode.type != lastNodeType) nodes.add(new ArrayList<>());
            nodes.getLast().add(currentNode);

            lastNodeType = currentNode.type;

            if (currentNode.type == Node.Type.ETHERWARP || currentNode.type == Node.Type.PEARL || currentNode.type == Node.Type.AOTE) {
                playerPos = NodeUtil.getNodePos(currentNode.targetPos, currentRoom).toBottomCenterPos();
                priority = 0;
            }

            currentNode = getNodeAt(currentRoom, playerPos);
        }

        return nodes;
    }

    private Node getNodeAt(Room currentRoom, Vec3d playerPos) {
        ArrayList<Node> nodes = new ArrayList<>();
        for (int i = 0; i < currentRoom.nodes.size(); i++) {
            Node node = currentRoom.nodes.get(i);

            BlockPos nodePos = NodeUtil.getNodePos(node.startPos, currentRoom);
            if (BlockPos.ofFloored(playerPos).equals(nodePos) && isValid(node, playerPos)) {
                nodes.add(node);
            }
        }
        nodes.sort(Comparator.comparingInt(e -> -e.type.ordinal()));

        if (nodes.isEmpty()) return null;
        if (priority >= nodes.size()) priority = 0;

        return nodes.get(priority);
    }

    // todo: fuh ts prob
    private boolean isValid(Node node, Vec3d playerPos) {
        Room room = AreaUtil.getCurrentRoom();
        if (room == null) return false;

        BlockPos nodePos = NodeUtil.getNodePos(node.targetPos, room);
        Rotation rotation = NodeUtil.flipRotation(node.rotation, Direction.NORTH, room.facing);
        Vec3d playerPosVec = playerPos.add(0, 1, 0);

        switch (node.type) {
            case ETHERWARP -> {
                Vec3i etherwarpBlock = EtherwarpUtil.getEtherwarpBlockSuccess(rotation.pitch, rotation.yaw, playerPosVec, 57);
                return etherwarpBlock != null && new BlockPos(etherwarpBlock).equals(nodePos);
            }
            case DUNGEON_BREAKER -> {
                BlockState blockState = C.w().getBlockState(nodePos);
                VoxelShape voxelShape = blockState.getOutlineShape(C.w(), nodePos);
                if (voxelShape.isEmpty()) return false;

                Vec3d targetMiddle = new Vec3d(nodePos.getX(), nodePos.getY(), nodePos.getZ()).add(voxelShape.getBoundingBox().getCenter());
                BlockHitResult blockHitResult = C.w().raycastBlock(
                        C.p().getEyePos(),
                        targetMiddle,
                        nodePos,
                        voxelShape,
                        blockState
                );

                return blockHitResult != null && blockHitResult.getType() != HitResult.Type.MISS && blockHitResult.getBlockPos().equals(nodePos);
            }
            // todo: check ender pearl / aote is valid
        }

        return true;
    }

    private static final ArrayList<ArrayList<Node>> CONNECTED_ETHERWARP_NODES = new ArrayList<>();
    private static Room lastRoom = null;
    private static int roomNodeAmount = -1;

    // bad code quality ig bro
    private static void renderAutoroutes() {
        if (!showAutoRoutes) return;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) return;
        if (lastRoom != currentRoom || roomNodeAmount != currentRoom.nodes.size()) {
            CONNECTED_ETHERWARP_NODES.clear();
            lastRoom = currentRoom;
            roomNodeAmount = currentRoom.nodes.size();
        }

        if (CONNECTED_ETHERWARP_NODES.isEmpty()) {
            HashMap<BlockPos, Node> positions = new HashMap<>();

            List<Node> etherwarpNodes = currentRoom.nodes.stream().filter(node -> node.type == Node.Type.ETHERWARP).toList();
            etherwarpNodes.forEach(node -> positions.put(node.startPos, node));

            HashSet<Node> uncheckedNodes = new HashSet<>(etherwarpNodes);

            for (Node node : etherwarpNodes) {
                if (!uncheckedNodes.contains(node)) continue;

                CONNECTED_ETHERWARP_NODES.add(new ArrayList<>(Collections.singletonList(node)));
                while (true) {
                    uncheckedNodes.remove(node);
                    positions.remove(node.startPos);
                    Node targetNode = positions.get(node.targetPos);
                    if (targetNode == null) break;

                    CONNECTED_ETHERWARP_NODES.getLast().add(targetNode);
                    node = targetNode;
                }
            }
        }

        List<Node> nonEtherwarpNodes = currentRoom.nodes.stream().filter(node -> node.type != Node.Type.ETHERWARP).toList();
        for (Node nonEtherwarpNode : nonEtherwarpNodes) {
            BlockPos nodePos = NodeUtil.getNodePos(nonEtherwarpNode.startPos, currentRoom);
            Color nodeColour = nonEtherwarpNode.type == Node.Type.SECRET ? secretsColour : otherNodeColour;
            Render3dUtil.drawBox(nodePos.getX(), nodePos.getY(), nodePos.getZ(), 1, 1, 1, nodeColour, false, false);

            BlockPos targetPos = NodeUtil.getNodePos(nonEtherwarpNode.targetPos, currentRoom);
            switch (nonEtherwarpNode.type) {
                case DUNGEON_BREAKER -> {
                    Render3dUtil.drawBox(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1, 1, 1, stonkColour, false, true);
                }
                case AOTE -> {
                    Render3dUtil.drawLine(nodePos.toCenterPos().toVector3f(), targetPos.toCenterPos().toVector3f(), new Color(nodeColour.getRed(), nodeColour.getGreen(), nodeColour.getBlue(), 255), false);
                }
            }
        }

        for (ArrayList<Node> nodeList : CONNECTED_ETHERWARP_NODES) {
            for (int i = 0; i < nodeList.size(); i++) {
                Node node = nodeList.get(i);
                Color nodeColour = RenderUtil.TRANS[i % RenderUtil.TRANS.length];
                BlockPos nodePos = NodeUtil.getNodePos(node.startPos, currentRoom);
                BlockPos nodeTargetPos = NodeUtil.getNodePos(node.targetPos, currentRoom);

                Render3dUtil.drawBox(nodePos.getX(), nodePos.getY(), nodePos.getZ(), 1, 1, 1, nodeColour, false, !node.firstNode);
                Render3dUtil.drawLine(nodePos.toCenterPos().toVector3f(), nodeTargetPos.toCenterPos().toVector3f(), nodeColour, false);
            }
        }
    }
}