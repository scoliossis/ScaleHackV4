package com.scale.scalehackv4.utils.skyblock;

import com.scale.scalehackv4.events.Bus;
import com.scale.scalehackv4.events.Event;
import com.scale.scalehackv4.events.RegisterListeners;
import com.scale.scalehackv4.events.impl.client.WorldJoinEvent;
import com.scale.scalehackv4.events.impl.player.*;
import com.scale.scalehackv4.utils.client.ChatUtil;
import com.scale.scalehackv4.utils.client.PacketUtil;
import com.scale.scalehackv4.utils.player.BlinkUtil;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

@RegisterListeners
public class ChainUtil {
    // i hope the comments are slightly more helpful than the variable names
    public record UpdatedInteractionChain(
            List<Runnable> action, // takes the packet(s) to be sent
            Predicate<?> getItem, // the item to switch to
            Predicate<?> shouldSend, // the case at which to send the packets
            Predicate<?> shouldCancel, // the case to cancel the list
            boolean shouldBlink, // self explanatory
            Predicate<?> cancelBlink, // if shouldBlink is true, then this determines when to cancel the blink
            Predicate<Packet<?>> expectedPackets, // whatever packet we are expecting to receive after executing
            long packetTimeout,
            boolean shouldCrouch
    ) {
        public UpdatedInteractionChain(List<Runnable> action, Predicate<?> getItem, Predicate<?> shouldSend, Predicate<?> shouldCancel, boolean shouldBlink, Predicate<?> cancelBlink, Predicate<Packet<?>> expectedPackets, boolean shouldCrouch) {
            this(action, getItem, shouldSend, shouldCancel, shouldBlink, cancelBlink, expectedPackets, DEFAULT_PACKET_TIMEOUT, shouldCrouch);
        }

        public void queueInteract(float yaw, float pitch) {
            this.action().add(() -> PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, yaw, pitch)));
        }
        public void queueInteracts(int amount, float yaw, float pitch) {
            this.action().add(() -> {
                for (int i = 0; i < amount; i++) {
                    PacketUtil.sendSequencedPacket(sequence -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, sequence, yaw, pitch));
                }
            });
        }
    }

    public static final ArrayList<UpdatedInteractionChain> INTERACTION_CHAINS = new ArrayList<>();
    public static final ArrayList<ExpectedPacket> EXPECTED_PACKETS = new ArrayList<>();
    public static final ArrayList<ExpectedPacket> EXTERNAL_EXPECTED = new ArrayList<>();
    public record ExpectedPacket(Predicate<Packet<?>> expectedPacket, long timeSent, long timeout) {}

    public static final int DEFAULT_PACKET_TIMEOUT = 1000;

    private static Runnable currentAction = null;
    private static boolean currentlyBlinking = false;
    private static boolean hasBlinked = false;
    public static int packetsSent = 0;

    public static final Event onRotationEvent = new RotationEvent(null, params -> {
        // forget evil packets we didnt end up getting for whatever reason
        EXPECTED_PACKETS.removeIf(expectedPacket -> System.currentTimeMillis() - expectedPacket.timeSent > expectedPacket.timeout);
        EXTERNAL_EXPECTED.removeIf(expectedPacket -> System.currentTimeMillis() - expectedPacket.timeSent > expectedPacket.timeout);

        if (!Bus.isEventCancelled(RotationEvent.class) && !handleNextChain()) onFinishChain();

        return false;
    }, 9999);

    public static final Event onReceiveExpectedPacket = new PacketEvent.Received(null, params -> {
        handlePacket(params.packet);

        return false;
    });

    public static final Event onSendExpectedPacket = new PacketEvent.Sent(null, params -> {
        handlePacket(params.packet);

        return false;
    });

    private static void handlePacket(Packet<?> packet) {
        // shoutout to ouuu for the .toArray(type) idea, ive been swapping to for (int i; i < size; i++) for a while now
        for (ExpectedPacket expectedPacket : EXPECTED_PACKETS.toArray(new ExpectedPacket[0])) {
            if (expectedPacket.expectedPacket.test(packet)) {
                EXPECTED_PACKETS.remove(expectedPacket);
                EXTERNAL_EXPECTED.remove(expectedPacket);
                return;
            }
        }
    }

    private static UpdatedInteractionChain lastChain = null;

    private static boolean handleNextChain() {
        if (INTERACTION_CHAINS.isEmpty()) {
            lastChain = null;
            return false;
        }

        // get next chain to handle
        UpdatedInteractionChain chain = INTERACTION_CHAINS.getFirst();
        if (lastChain != chain) {
            EXTERNAL_EXPECTED.clear();
            EXTERNAL_EXPECTED.addAll(EXPECTED_PACKETS);

            packetsSent = 0;
            lastChain = chain;
        }

        // we MAY wanna stop blinking, idk yet
        boolean stopBlinking = shouldStopBlinking(chain);
        if (stopBlinking) stopBlinking();

        if (chain.action.isEmpty()) {
            // we dont wanna finish w this chain if we are still wanting blink
            // because we cant move on if this chain is still doing anything, even blinking.
            if (!stopBlinking && currentlyBlinking) return true;

            // this chain is DONE! GOODBYE CHUD
            onFinishChain();

            // go find a new chain, gogogo quick!
            while (chain.action.isEmpty()) {
                INTERACTION_CHAINS.removeFirst();
                if (INTERACTION_CHAINS.isEmpty()) return false;
                chain = INTERACTION_CHAINS.getFirst();
            }
        }

        // self explanatory
        if (chain.shouldCancel.test(null)) {
            INTERACTION_CHAINS.clear();
            ChatUtil.prefixMessage("CANCELLED!");
            return false;
        }

        // if they dont have the item we need, its over. (dont ever pregnant man emoji react me again)
        if (!chain.getItem.test(null)) {
            INTERACTION_CHAINS.clear();
            ChatUtil.prefixMessage("MISSING ITEM!");
            return false;
        }

        // we sometimes wanna delay the packets for a lil, edging n shi
        if (!chain.shouldSend.test(null)) return true;

        currentAction = chain.action.removeFirst();
        handleBlink(chain);
        // we gotta know what packets we are expecting to receive before performing the next action
        if (!chain.expectedPackets.test(null)) EXPECTED_PACKETS.add(new ExpectedPacket(chain.expectedPackets, System.currentTimeMillis(), DEFAULT_PACKET_TIMEOUT));

        return true;
    }

    private static void handleBlink(UpdatedInteractionChain chain) {
        if (chain.shouldBlink) {
            if (chain.cancelBlink.test(null)) {
                if (currentlyBlinking) {
                    //ChatUtil.prefixMessage("no mo blink...");
                    BlinkUtil.popBlink(true, false);
                    currentlyBlinking = false;
                }
            }
            else if (!hasBlinked) {
                //ChatUtil.prefixMessage("BLINKING!");
                BlinkUtil.pushBlink(true, false);
                hasBlinked = currentlyBlinking = true;
            }
        }
    }

    // stops moving while there is a pending interaction chain
    private final Event stopMoving = new KeyboardInputEvent(null, params -> {
        if (shouldBlockInputs()) params.reset();
        if (shouldCrouch()) params.sneakKey = true;

        return false;
    });


    private final Event stopRightClick = new ClickedEvent.Right(null, params -> shouldBlockInputs());
    private final Event stopLeftClick = new ClickedEvent.Left(null, params -> shouldBlockInputs());

    private boolean shouldBlockInputs() {
        return !INTERACTION_CHAINS.isEmpty() || !ChainUtil.EXPECTED_PACKETS.isEmpty();
    }
    private boolean shouldCrouch() {
        return !INTERACTION_CHAINS.isEmpty() && INTERACTION_CHAINS.getFirst().shouldCrouch;
    }

    // todo: technically shouldnt be in SendMovementPacketEvent because ur supposed to interact before sending input packets
    //  as per: net.minecraft.client.network.ClientPlayerEntity.tick
    public static final Event performAction = new SendMovementPacketEvent(null, params -> {
        if (currentAction == null) return false;

        currentAction.run();
        currentAction = null;
        packetsSent++;

        return false;
    });

    private static void onFinishChain() {
        stopBlinking();
        currentAction = null;
    }

    private static boolean shouldStopBlinking(UpdatedInteractionChain chain) {
        return currentlyBlinking && chain.cancelBlink.test(null);
    }

    private static void stopBlinking() {
        if (currentlyBlinking) BlinkUtil.popBlink(true, false);
        currentlyBlinking = hasBlinked = false;
    }

    private final Event onWorldJoin = new WorldJoinEvent(null, params -> {
        onFinishChain();
        INTERACTION_CHAINS.clear();

        return false;
    });

    public static void forgetExpecting() {
        EXPECTED_PACKETS.clear();
        EXTERNAL_EXPECTED.clear();
    }
}