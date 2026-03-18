package com.scale.scalehackv4.modules.impl.skyblock.SecretsHelper;

import com.scale.scalehackv4.bridge.UseItemBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.player.*;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.ModuleUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.Node;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.NodeUtil;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3i;

import java.awt.*;

// todo: fix secrets awaiting to allow secrets to be gotten before the node is reached
//  change rotations to be serverside
public class AutoRoutes extends SecretsHelper {
    @RegisterSubModule(name = "Auto Routes", hasKeybind = true)
    public static boolean autoroutes = true;

    @RegisterSubModule(name = "Skip On Click", parent = "Auto Routes")
    public static boolean skipOnClick = true;

    @RegisterSubModule(name = "Timeout", parent = "Auto Routes", max = 5000, increment = 100)
    public static long timeout = 1000;
    @RegisterSubModule(name = "Delay", parent = "Auto Routes", max = 2000, increment = 50)
    public static long delay = 500;
    @RegisterSubModule(name = "Retry Delay", parent = "Auto Routes", max = 2000, increment = 50)
    public static long retryDelay = 1000;
    @RegisterSubModule(name = "Max Secret Delay", parent = "Auto Routes", max = 10000, increment = 100)
    public static long maxSecretDelay = 5000;

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

    private static Node currentNode = null;
    private static boolean swapped = false;
    private static long startedAwaiting = -1;
    private static long lastNode = -1;
    private static boolean failed = false;

    private static int receivedSecretPackets = 0;
    private static int lastNodeIndex = 0;
    private static int ticksAwaiting = 0;

    private static long lastItemPickup = -1;

    private static boolean startAwaiting = false;

    private final Event onRotateEvent = new RotationEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (!autoroutes) return false;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null || currentRoom.nodes.isEmpty()) {
            if (currentNode != null) finishedNode(true);
            return false;
        }

        findCurrentNode(currentRoom);

        if (currentNode == null) return false;
        if ((currentNode.type == Node.Type.SECRET || !handleSwap()) && startedAwaiting == -1) startAwaiting = true;

        return false;
    });

    private final Event onAllowRightClick = new CheckClickEvent.Right(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (!autoroutes || currentNode == null) {
            forceStop();
            return false;
        }

        ticksAwaiting++;

        // handle double pearling
        if (currentNode.type == Node.Type.PEARL && ticksAwaiting == currentNode.targetPos.getX()) UseItemBridge.from(C.mc).bridge$DoItemUse();
        // perform action based on node type
        if (startAwaiting && ticksAwaiting >= getTicksBeforeAwait()) startAwaiting();

        return false;
    });

    private final Event onLeftClicked = new ClickedEvent.Left(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (skipOnClick && currentNode != null) {
            ChatUtil.prefixMessage("Skipping node: " + currentNode.type.name());
            finishedNode(true);
        }

        return false;
    });

    private final Event overrideMovement = new KeyboardInputEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (currentNode == null) return false;

        params.backKey = params.leftKey = params.rightKey = params.forwardKey = params.sneakKey = params.jumpKey = false;
        if (currentNode.type == Node.Type.ETHERWARP) params.sneakKey = true;
        return false;
    });

    private final Event renderWorldEvent = new RenderWorldEvent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        renderAutoroutes();

        return false;
    });

    private final Event awaitAutoRouteSuccess = new PacketEvent.Received(ModuleUtil.getModule(SecretsHelper.class), params -> {
        // hypixel gives u the item before we expect it </3
        if (params.packet instanceof ItemPickupAnimationS2CPacket) {
            lastItemPickup = System.currentTimeMillis();
        }

        if (startedAwaiting == -1) return false;
        if (System.currentTimeMillis() - startedAwaiting > (currentNode.type == Node.Type.SECRET ? maxSecretDelay : timeout)) {
            finishedNode(false);
            return false;
        }
        switch (currentNode.type) {
            case ETHERWARP, PEARL -> {
                if (params.packet instanceof PlayerPositionLookS2CPacket) {
                    finishedNode(true);
                }
            }
            case DUNGEON_BREAKER -> {
                if (C.w().getBlockState(NodeUtil.getNodePos(currentNode.targetPos, AreaUtil.getCurrentRoom())).getBlock() == Blocks.AIR) {
                    finishedNode(true);
                }
            }
            case SECRET -> {
                if (System.currentTimeMillis() - lastItemPickup < 1000) {
                    finishedNode(true);
                    lastItemPickup = -1;
                }
                if (params.packet instanceof EntityStatusS2CPacket entityDamageS2CPacket) {
                    if (entityDamageS2CPacket.getStatus() == 3 && entityDamageS2CPacket.getEntity(C.w()) instanceof BatEntity) {
                        finishedNode(true);
                    }
                }
            }
        }
        return false;
    });

    private final Event awaitSecretInteraction = new PacketEvent.Sent(ModuleUtil.getModule(SecretsHelper.class), params -> {
        if (currentNode == null || currentNode.type != Node.Type.SECRET) return false;

        if (params.packet instanceof PlayerInteractBlockC2SPacket blockInteractPacket) {
            Block blockInteracted = C.w().getBlockState(blockInteractPacket.getBlockHitResult().getBlockPos()).getBlock();

            if (blockInteracted == Blocks.PLAYER_HEAD || blockInteracted == Blocks.PLAYER_WALL_HEAD || blockInteracted == Blocks.LEVER || blockInteracted == Blocks.CHEST || blockInteracted == Blocks.TRAPPED_CHEST) {
                finishedNode(true);
            }
        }

        return false;
    });

    private int getTicksBeforeAwait() {
        return switch (currentNode.type) {
            case ETHERWARP -> {
                // hypixel hates me for some reason if im looking at a block before etherwarping, likely mb
                HitResult hitResult = C.mc.crosshairTarget;
                yield hitResult == null || hitResult.getType() == HitResult.Type.MISS ? 2 : 3;
            }
            case PEARL, DUNGEON_BREAKER  -> 2;
            case SECRET -> 1;
        };
    }

    private void findCurrentNode(Room currentRoom) {
        if (currentNode == null && System.currentTimeMillis() - lastNode > (failed ? retryDelay : delay)) {
            for (int i = 0; i < currentRoom.nodes.size(); i++) {
                int index = (i + lastNodeIndex + 1) % currentRoom.nodes.size();
                Node node = currentRoom.nodes.get(index);

                BlockPos nodePos = NodeUtil.getNodePos(node.startPos, currentRoom);
                if (C.p().getBlockPos().offset(Direction.DOWN, 1).equals(nodePos) && isValid(node)) {
                    currentNode = node;
                    lastNodeIndex = index;
                    break;
                }
            }
        }
    }

    private boolean handleSwap() {
        if (!swapped) {
            Rotation rotation = NodeUtil.flipRotation(currentNode.rotation, Direction.NORTH, AreaUtil.getCurrentRoom().facing);
            if (!isValid(currentNode)) {
                ChatUtil.prefixMessage("Target node invalid for " + currentNode.type.name());
                finishedNode(true);
                return true;
            }

            if (!currentNode.type.getItem.test(null)) {
                ChatUtil.prefixMessage("Missing item in hotbar: " + currentNode.type.name());
                autoroutes = false;
                finishedNode(false);
                return true;
            }

            C.p().setYaw(rotation.yaw);
            C.p().setPitch(rotation.pitch);
            swapped = true;
            return true;
        }

        return false;
    }

    private boolean isValid(Node node) {
        Room room = AreaUtil.getCurrentRoom();

        if (room == null) return false;

        BlockPos nodePos = NodeUtil.getNodePos(node.targetPos, room);
        Rotation rotation = NodeUtil.flipRotation(node.rotation, Direction.NORTH, room.facing);

        switch (node.type) {
            case ETHERWARP -> {
                Vec3i etherwarpBlock = EtherwarpUtil.getEtherwarpBlockSuccess(rotation.pitch, rotation.yaw, C.p().getEntityPos(), 57);
                return etherwarpBlock != null && new BlockPos(etherwarpBlock).equals(nodePos);
            }
            case DUNGEON_BREAKER -> {
                BlockHitResult blockHitResult = WorldUtil.raytrace(C.p().getEyePos(), C.p().getBlockInteractionRange(), rotation);
                return blockHitResult.getType() != HitResult.Type.MISS && blockHitResult.getBlockPos().equals(nodePos);
            }
            // todo: check ender pearl is valid
        }

        return true;
    }

    private void startAwaiting() {
        startAwaiting = false;
        if (startedAwaiting != -1) return;

        ticksAwaiting = 0;
        switch (currentNode.type) {
            case ETHERWARP, PEARL -> {
                UseItemBridge.from(C.mc).bridge$DoItemUse();
            }
            case DUNGEON_BREAKER -> {
                UseItemBridge.from(C.mc).bridge$doAttack();
            }
        }
        startedAwaiting = System.currentTimeMillis();
    }

    private void finishedNode(boolean success) {
        failed = !success;
        // secrets nodes pass the amount of secrets as the the X coord
        if (failed || currentNode.type != Node.Type.SECRET || receivedSecretPackets+1 >= currentNode.targetPos.getX()) {
            if (failed) {
                ChatUtil.prefixMessage(currentNode.type == Node.Type.SECRET ? "Secret not found" : "Node failed, oops *teehee*");
            }
            forceStop();
        }
        else {
            ChatUtil.prefixMessage("Secret found: " + receivedSecretPackets+1);
            receivedSecretPackets++;
        }
    }

    protected static void forceStop() {
        startedAwaiting = -1;
        swapped = false;
        currentNode = null;
        lastNode = System.currentTimeMillis();
        receivedSecretPackets = 0;
        ticksAwaiting = 0;
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
