package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.SubModuleToggledEvent;
import com.scale.scalehackv4.events.impl.player.*;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
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

// todo: doesnt do them in order correctly still im done
public class AutoRoutes extends SecretsHelper {
    @RegisterSubModule(name = "Auto Routes", hasKeybind = true)
    public static boolean autoroutes = true;

    @RegisterSubModule(name = "Timeout", parent = "Auto Routes", max = 5000, increment = 100)
    public static long timeout = 1000;
    @RegisterSubModule(name = "Secret Timeout", parent = "Auto Routes", max = 5000, increment = 100)
    public static long secretTimeout = 2000;

    @RegisterSubModule(name = "Left Click Skip", parent = "Auto Routes")
    public static boolean leftClickSkip = true;
    @RegisterSubModule(name = "Zero Tick Etherwarp", parent = "Auto Routes", hasKeybind = true)
    public static boolean zeroTickEtherwarp = true;
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

    private static ChainUtil.InteractionChain interactionChain;
    private static int interactionChainIndex = 0;
    private static final ArrayList<Long> expectedPackets = new ArrayList<>();
    // u can receive secrets before we expect them, so make sure we take care of that
    private static final ArrayList<Long> interactedSecrets = new ArrayList<>();
    private static final ArrayList<ChainUtil.InteractionChainNode> pendingInteractions = new ArrayList<>();
    private static Node.Type lastNode = null;
    private static Node.Type lastCheckedNode = null;

    private static boolean finishedLastChain = true;
    private static int prevPriority = 0;

    public static void disable(boolean resetPriority) {
        interactionChain = null;
        interactionChainIndex = 0;
        if (resetPriority) prevPriority = 0;
    }

    private final Event handleLeftClick = new ClickedEvent.Left(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (!leftClickSkip) return false;
        if (!expectedPackets.isEmpty()) {
            expectedPackets.clear();
            return true;
        }
        return false;
    });

    private final Event onRotateEvent = new RotationEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        // handle received secrets
        while (lastNode == Node.Type.SECRET && !interactedSecrets.isEmpty()) {
            interactedSecrets.removeFirst();
            if (!expectedPackets.isEmpty()) expectedPackets.removeFirst();
        }

        // forget failed teleports
        while (!expectedPackets.isEmpty() && System.currentTimeMillis() - expectedPackets.getFirst() > (lastNode == Node.Type.SECRET ? secretTimeout : timeout)) {
            expectedPackets.removeFirst();
        }
        if (expectedPackets.isEmpty()) finishedLastChain = true;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) {
            disable(true);
            return false;
        }
        if (!finishedLastChain) return false;

        interactionChain = getInteractionChain(C.p().getEntityPos().subtract(0, 1, 0));
        if (interactionChain == null) return false;

        ChainUtil.InteractionChainNode interactionChainNode = interactionChain.interactionChainNodes()[interactionChainIndex];
        Node.Type nodeItem = interactionChainNode.itemType();
        if (!nodeItem.getItem.test(null)) {
            ChatUtil.prefixMessage("NODE ITEM REQUIRED: " + nodeItem.name());
            return false;
        }

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

            if ((nodeItem != Node.Type.ETHERWARP || !zeroTickEtherwarp) && (nodeItem != Node.Type.DUNGEON_BREAKER || !zeroTickStonk))
                break;
        }

        disable(interactionChainIndex >= interactionChain.interactionChainNodes().length);

        return false;
    });

    private final Event fixMovement = new KeyboardInputEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (expectedPackets.isEmpty()) return false;
        params.backKey = params.rightKey = params.leftKey = params.forwardKey = params.jumpKey = false;

        params.sneakKey = !pendingInteractions.isEmpty() && pendingInteractions.getFirst().itemType() == Node.Type.ETHERWARP;

        return false;
    });

    // execute as in run, but also as in KILLING THEM by removing them from the list
    private final Event executeNodes = new PlayerUpdateEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        while (!pendingInteractions.isEmpty()) {
            Node.Type nodeType = pendingInteractions.getFirst().itemType();
            if (nodeType == Node.Type.ETHERWARP && !C.p().isSneaking()) return false;

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
            case ETHERWARP, PEARL, AOTE -> {
                if (params.packet instanceof PlayerPositionLookS2CPacket) {
                    expectedPackets.removeFirst();
                }
            }
            case DUNGEON_BREAKER, SUPERBOOM -> {
                if (params.packet instanceof BlockUpdateS2CPacket blockUpdateS2CPacket) {
                    // todo: better check than distance <3
                    if (Math.sqrt(blockUpdateS2CPacket.getPos().getSquaredDistance(C.p().getEntityPos())) < 4.5) {
                        expectedPackets.removeFirst();
                    }
                }
            }
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

    private final Event onToggleRoutes = new SubModuleToggledEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (!params.field.getName().equals("autoroutes")) return false;

        if (!(boolean) params.value) disable(true);
        return false;
    });

    private ChainUtil.InteractionChain getInteractionChain(Vec3d playerPos) {
        if (!autoroutes) return null;
        if (interactionChain != null) return interactionChain;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) return null;

        ArrayList<ChainUtil.InteractionChainNode> nodes = new ArrayList<>();
        ArrayList<Node> passedNodes = new ArrayList<>();

        Node currentNode = getNodeAt(currentRoom, playerPos);
        Node.Type nodeType = currentNode != null ? currentNode.type : null;
        while (currentNode != null) {
            lastCheckedNode = currentNode.type;
            // stops infinite looping routes crashing
            if (passedNodes.contains(currentNode)) break;

            passedNodes.add(currentNode);

            nodes.add(new ChainUtil.InteractionChainNode(currentNode.rotation, currentNode.targetPos, currentNode.type));
            if (currentNode.type == Node.Type.ETHERWARP || currentNode.type == Node.Type.PEARL || currentNode.type == Node.Type.AOTE) {
                playerPos = NodeUtil.getNodePos(currentNode.targetPos, currentRoom).toBottomCenterPos();
                prevPriority = 0;
            }
            else prevPriority++;
            currentNode = getNodeAt(currentRoom, playerPos);
            if (currentNode != null && currentNode.type != nodeType) break;
        }

        interactionChainIndex = 0;
        if (nodes.isEmpty()) {
            prevPriority = 0;
            return null;
        }
        return new ChainUtil.InteractionChain(nodes.toArray(ChainUtil.InteractionChainNode[]::new), null);
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
        if (prevPriority >= nodes.size()) prevPriority = 0;

        return nodes.get(prevPriority);
    }

    // todo: fuh ts prob
    private boolean isValid(Node node, Vec3d playerPos) {
        Room room = AreaUtil.getCurrentRoom();
        if (room == null) return false;

        if (node.type == Node.Type.SECRET && lastCheckedNode == Node.Type.SECRET) return false;

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
            // todo: check ender pearl is valid
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
            // only just found this guy, ive been using hashmaps for ages ig
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
                    Render3dUtil.drawLine(nodePos.toCenterPos().toVector3f(), targetPos.toCenterPos().toVector3f(), new Color(nodeColour.getRed(), nodeColour.getGreen(), nodeColour.getBlue(), 255));
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
                Render3dUtil.drawLine(nodePos.toCenterPos().toVector3f(), nodeTargetPos.toCenterPos().toVector3f(), nodeColour);
            }
        }
    }
}
