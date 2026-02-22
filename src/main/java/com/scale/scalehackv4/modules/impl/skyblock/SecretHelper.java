package com.scale.scalehackv4.modules.impl.skyblock;

import com.scale.scalehackv4.bridge.UseItemBridge;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.impl.client.PacketEvent;
import com.scale.scalehackv4.events.impl.player.KeyboardInputEvent;
import com.scale.scalehackv4.events.impl.player.LeftClickedEvent;
import com.scale.scalehackv4.events.impl.player.PlayerUpdateEvent;
import com.scale.scalehackv4.events.impl.render.RenderWorldEvent;
import com.scale.scalehackv4.modules.Category;
import com.scale.scalehackv4.modules.Module;
import com.scale.scalehackv4.modules.RegisterModule;
import com.scale.scalehackv4.modules.RegisterSubModule;
import com.scale.scalehackv4.utils.C;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.player.Rotation;
import com.scale.scalehackv4.utils.player.RotationUtil;
import com.scale.scalehackv4.utils.player.WorldUtil;
import com.scale.scalehackv4.utils.render.Render3dUtil;
import com.scale.scalehackv4.utils.skyblock.AreaUtil;
import com.scale.scalehackv4.utils.skyblock.DungeonMap.Room;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.EtherwarpUtil;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.Node;
import com.scale.scalehackv4.utils.skyblock.SecretsHelper.NodeUtil;
import net.minecraft.block.Blocks;
import net.minecraft.entity.passive.BatEntity;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.s2c.play.EntityStatusS2CPacket;
import net.minecraft.network.packet.s2c.play.ItemPickupAnimationS2CPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.awt.*;

@RegisterModule(
        name = "Secret Helper",
        category = Category.SKYBLOCK
)
public class SecretHelper extends Module {
    @RegisterSubModule(name = "Etherwarp Overlay")
    public static boolean etherwarpOverlay = true;
    @RegisterSubModule(name = "Etherwarp Colour", parent = "Etherwarp Overlay")
    public static Color etherwarpColour = new Color(188, 0, 250, 90);

    @RegisterSubModule(name = "Auto Routes", hasKeybind = true)
    public static boolean autoroutes = true;
    @RegisterSubModule(name = "Skip On Click")
    public static boolean skipOnClick = true;

    @RegisterSubModule(name = "Timeout", parent = "Auto Routes", max = 5000, increment = 100)
    public static long timeout = 1000;
    @RegisterSubModule(name = "Delay", parent = "Auto Routes", max = 2000, increment = 50)
    public static long delay = 500;
    @RegisterSubModule(name = "Retry Delay", parent = "Auto Routes", max = 2000, increment = 50)
    public static long retryDelay = 1000;
    @RegisterSubModule(name = "Max Secret Delay", parent = "Auto Routes", max = 10000, increment = 100)
    public static long maxSecretDelay = 5000;

    @RegisterSubModule(name = "Show Auto Routes")
    public static boolean showAutoRoutes = true;
    @RegisterSubModule(name = "Etherwarp Node Colour", parent = "Show Auto Routes")
    public static Color etherwarpNodeColour = new Color(135, 148, 255, 92);
    @RegisterSubModule(name = "Line Colour", parent = "Show Auto Routes")
    public static Color lineColours = new Color(198, 0, 255, 92);
    @RegisterSubModule(name = "Stonk Colour", parent = "Show Auto Routes")
    public static Color stonkColour = new Color(255, 51, 51, 92);
    @RegisterSubModule(name = "Await Secret Colour", parent = "Show Auto Routes")
    public static Color awaitSecretColour = new Color(255, 215, 0, 92);

    private Vec3i etherwarpPos = null;

    private Node currentNode = null;
    private boolean swapped = false;
    private long startedAwaiting = -1;
    private long lastNode = -1;
    private boolean failed = false;

    private int lastNodeIndex = 0;

    private final Event onPlayerUpdate = new PlayerUpdateEvent(this, params -> {
        handleAutoRoutes();
        handleEtherwarpOverlay();

        return false;
    });

    private final Event onLeftClicked = new LeftClickedEvent(this, params -> {
        if (skipOnClick && currentNode != null) {
            ChatUtil.prefixMessage("Skipping node: " + currentNode.type.name());
            finishedNode(true);
        }

        return false;
    });

    private void handleAutoRoutes() {
        if (!autoroutes) return;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null || currentRoom.nodes.isEmpty()) {
            if (currentNode != null) finishedNode(true);
            return;
        }

        if (currentNode == null && System.currentTimeMillis() - lastNode > (failed ? retryDelay : delay)) {
            for (int i = 0; i < currentRoom.nodes.size(); i++) {
                int index = (i + lastNodeIndex + 1) % currentRoom.nodes.size();
                Node node = currentRoom.nodes.get(index);

                BlockPos nodePos = NodeUtil.getNodePos(node.startPos, currentRoom);
                if (C.p().getBlockPos().offset(Direction.DOWN, 1).equals(nodePos) && (C.p().isOnGround() || node.type != Node.Type.ETHERWARP)) {
                    currentNode = node;
                    lastNodeIndex = index;
                    break;
                }
            }
        }

        if (currentNode == null) return;
        if (currentNode.type.isSecret || !handleSwap()) startAwaiting();
    }

    private boolean handleSwap() {
        if (!swapped) {
            Rotation rotation = getRotation();

            if (rotation == null) {
                if (currentNode.type == Node.Type.DUNGEON_BREAKER) {
                    finishedNode(true);
                    return true;
                }
                ChatUtil.prefixMessage("Target node invalid for " + currentNode.type.name());
                finishedNode(false);
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

    // i dont like this code
    private Rotation getRotation() {
        BlockPos nodePos = NodeUtil.getNodePos(currentNode.targetPos, AreaUtil.getCurrentRoom());
        switch (currentNode.type) {
            case DUNGEON_BREAKER -> {
                Rotation basicRotation = RotationUtil.getRotation(RotationUtil.getClientRotation(), C.p().getEyePos(), nodePos.toCenterPos());
                BlockHitResult blockHitResult = WorldUtil.raytrace(C.p().getEyePos(), C.p().getBlockInteractionRange(), basicRotation);
                if (blockHitResult.getBlockPos().equals(nodePos)) return basicRotation;

                for (float yaw = 0; yaw < 30; yaw+=0.25f) {
                    for (float pitch = 0; pitch < 30; pitch+=0.25f) {
                        if (WorldUtil.raytrace(C.p().getEyePos(), C.p().getBlockInteractionRange(), basicRotation.add(new Rotation(yaw, pitch))).getBlockPos().equals(nodePos)) {
                            return basicRotation.add(new Rotation(yaw, pitch));
                        }
                        else if (WorldUtil.raytrace(C.p().getEyePos(), C.p().getBlockInteractionRange(), basicRotation.subtract(new Rotation(yaw, pitch))).getBlockPos().equals(nodePos)) {
                            return basicRotation.subtract(new Rotation(yaw, pitch));
                        }
                    }
                }
            }
            case ETHERWARP -> {
                Vec3d etherwarpEyePos = C.p().getEntityPos().add(0, EtherwarpUtil.ETHERWARP_EYE_HEIGHT, 0);
                Rotation rotation = RotationUtil.getRotation(RotationUtil.getClientRotation(), etherwarpEyePos, nodePos.toCenterPos());
                Vec3i etherwarpPos = EtherwarpUtil.getEtherwarpBlockSuccess(rotation.pitch, rotation.yaw, C.p().getEntityPos(), 57);
                if (etherwarpPos != null && new BlockPos(etherwarpPos).equals(nodePos)) return rotation;

                for (float yaw = 0; yaw < 30; yaw+=0.1f) {
                    for (float pitch = 0; pitch < 30; pitch+=0.1f) {
                        Vec3i newPosA = EtherwarpUtil.getEtherwarpBlockSuccess(rotation.pitch+pitch, rotation.yaw+yaw, C.p().getEntityPos(), 57);
                        if (newPosA != null && new BlockPos(newPosA).equals(nodePos)) {
                            return new Rotation(rotation.yaw+yaw, rotation.pitch+pitch);
                        }
                        else {
                            Vec3i newPosB = EtherwarpUtil.getEtherwarpBlockSuccess(rotation.pitch-pitch, rotation.yaw-yaw, C.p().getEntityPos(), 57);
                            if (newPosB != null && new BlockPos(newPosB).equals(nodePos)) {
                                return new Rotation(rotation.yaw-yaw, rotation.pitch-pitch);
                            }
                        }
                    }
                }
            }
        }

        return null;
    }

    private void startAwaiting() {
        if (startedAwaiting != -1) return;

        switch (currentNode.type) {
            case ETHERWARP -> {
                UseItemBridge.from(C.mc).bridge$DoItemUse();
            }
            case DUNGEON_BREAKER -> {
                UseItemBridge.from(C.mc).bridge$doAttack();
            }
        }
        startedAwaiting = System.currentTimeMillis();
    }

    private final Event awaitAutoRouteSuccess = new PacketEvent.Received(this, params -> {
        if (startedAwaiting == -1) return false;
        if (System.currentTimeMillis() - startedAwaiting > (currentNode.type.isSecret ? maxSecretDelay : timeout)) {
            finishedNode(false);
            return false;
        }
        switch (currentNode.type) {
            case ETHERWARP -> {
                if (params.packet instanceof PlayerPositionLookS2CPacket) {
                    finishedNode(true);
                }
            }
            case DUNGEON_BREAKER -> {
                if (C.w().getBlockState(NodeUtil.getNodePos(currentNode.targetPos, AreaUtil.getCurrentRoom())).getBlock() == Blocks.AIR) {
                    finishedNode(true);
                }
            }
            case ITEM -> {
                if (params.packet instanceof ItemPickupAnimationS2CPacket) {
                    finishedNode(true);
                }
            }
            case BAT -> {
                if (params.packet instanceof EntityStatusS2CPacket entityDamageS2CPacket) {
                    if (entityDamageS2CPacket.getStatus() == 3 && entityDamageS2CPacket.getEntity(C.w()) instanceof BatEntity) {
                        finishedNode(true);
                    }
                }
            }
        }
        return false;
    });

    private final Event awaitSecretInteraction = new PacketEvent.Sent(this, params -> {
        if (currentNode == null || !currentNode.type.isSecret) return false;

        switch (currentNode.type) {
            case CHEST, WITHER_ESSENCE -> {
                if (params.packet instanceof PlayerInteractBlockC2SPacket) {
                    ChatUtil.prefixMessage("Secret found!");
                    finishedNode(true);
                }
            }
        }

        return false;
    });

    private void finishedNode(boolean success) {
        failed = !success;
        if (failed) {
            ChatUtil.prefixMessage(currentNode.type.isSecret ? "Secret not found" : "Node failed, oops *teehee*");
        }
        startedAwaiting = -1;
        swapped = false;
        currentNode = null;
        lastNode = System.currentTimeMillis();
    }

    private final Event overrideMovement = new KeyboardInputEvent(this, params -> {
        if (currentNode == null) return false;

        params.backKey = params.leftKey = params.rightKey = params.forwardKey = params.sneakKey = params.jumpKey = false;
        if (currentNode.type == Node.Type.ETHERWARP) params.sneakKey = true;
        return false;
    });

    private void handleEtherwarpOverlay() {
        if (!etherwarpOverlay || !C.p().isSneaking() || !EtherwarpUtil.isEtherwarpItem(C.p().getMainHandStack())) {
            etherwarpPos = null;
            return;
        }

        etherwarpPos = EtherwarpUtil.getEtherwarpBlockSuccess(C.p().getPitch(), C.p().getYaw(), C.p().getEntityPos(), 57);
    }

    private final Event renderWorldEvent = new RenderWorldEvent(this, params -> {
        renderEtherwarpPos();
        renderAutoroutes();

        return false;
    });

    private void renderEtherwarpPos() {
        if (etherwarpPos != null) {
            Render3dUtil.drawBox(etherwarpPos.getX(), etherwarpPos.getY(), etherwarpPos.getZ(), 1, 1, 1, etherwarpColour, false);
        }
    }

    private static void renderAutoroutes() {
        if (!showAutoRoutes) return;

        Room currentRoom = AreaUtil.getCurrentRoom();
        if (currentRoom == null) return;

        for (Node node : currentRoom.nodes) {
            Color startNodeColour = node.type.isSecret ? awaitSecretColour : node.type == Node.Type.DUNGEON_BREAKER ? stonkColour : etherwarpNodeColour;
            BlockPos nodePos = NodeUtil.getNodePos(node.startPos, currentRoom);
            Render3dUtil.drawBox(nodePos.getX(), nodePos.getY(), nodePos.getZ(), 1, 1, 1, startNodeColour, false);

            if (node.type != Node.Type.ETHERWARP) continue;

            BlockPos targetNodePos = NodeUtil.getNodePos(node.targetPos, currentRoom);
            Render3dUtil.drawLine(nodePos.toCenterPos().toVector3f(), targetNodePos.toCenterPos().toVector3f(), lineColours);
        }
    }

    @Override public void onEnable() {}
    @Override public void onDisable() {
        finishedNode(true);
    }
}