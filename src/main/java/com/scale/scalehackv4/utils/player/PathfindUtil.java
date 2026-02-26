package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.utils.C;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;

import java.util.*;

// basic A* impl from https://en.wikipedia.org/wiki/A*_search_algorithm
// (yes minecraft has their own impl, but im making this to prove a point)
public class PathfindUtil {
    private static ArrayList<BlockPos> reconstructPath(HashMap<BlockPos, BlockPos> cameFrom, BlockPos current) {
        ArrayList<BlockPos> total_path = new ArrayList<>();
        total_path.add(current);

        while (cameFrom.containsKey(current)) {
            current = cameFrom.get(current);
            total_path.addFirst(current);
        }

        return total_path;
    }

    // A* finds a path from start to goal.
    // h is the heuristic function. h(n) estimates the cost to reach goal from node n.
    public static ArrayList<BlockPos> findPath(BlockPos start, BlockPos goal) {
        if (!isValid(goal)) return null;

        // For node n, cameFrom[n] is the node immediately preceding it on the cheapest path from the start
        // to n currently known.
        HashMap<BlockPos, BlockPos> cameFrom = new HashMap<>();

        // For node n, gScore[n] is the currently known cost of the cheapest path from start to n.
        HashMap<BlockPos, Integer> gScore = new HashMap<>();
        gScore.put(start, 0);

        // For node n, fScore[n] := gScore[n] + h(n). fScore[n] represents our current best guess as to
        // how cheap a path could be from start to finish if it goes through n.
        HashMap<BlockPos, Integer> fScore = new HashMap<>();
        int startDistance = h(start, goal);
        fScore.put(start, startDistance);

        // The set of discovered nodes that may need to be (re-)expanded.
        // Initially, only the start node is known.
        // This is usually implemented as a min-heap or priority queue rather than a hash-set.
        PriorityQueue<BlockPos> openSet = new PriorityQueue<>(Comparator.comparingInt(fScore::get));
        HashSet<BlockPos> openSetMap = new HashSet<>();
        openSet.add(start);
        openSetMap.add(start);
        HashSet<BlockPos> closedSet = new HashSet<>();

        while (!openSet.isEmpty()) {
            // This operation can occur in O(Log(N)) time if openSet is a min-heap or a priority queue
            BlockPos current = openSet.poll();
            openSetMap.remove(current);
            if (current.equals(goal)) return reconstructPath(cameFrom, current);

            if (!closedSet.add(current)) continue;

            for (BlockPos neighbor : getNeighbours(current)) {
                if (closedSet.contains(neighbor)) continue;

                // d(current,neighbor) is the weight of the edge from current to neighbor
                // tentative_gScore is the distance from start to the neighbor through current
                int ticksToJumpTo = d(current, neighbor, true);

                if (ticksToJumpTo == -1) {
                    ticksToJumpTo = d(current, neighbor, false);
                    if (ticksToJumpTo == -1) continue;
                }

                int tentative_gScore = gScore.get(current) + ticksToJumpTo;
                if (tentative_gScore < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    // This path to neighbor is better than any previous one. Record it!
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentative_gScore);
                    fScore.put(neighbor, tentative_gScore + h(neighbor, goal));
                    if (openSetMap.add(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        // Open set is empty but goal was never reached
        return null;
    }

    private static int h(BlockPos pos, BlockPos goal) {
        // todo: make better heuristic
        return (int) (Math.sqrt(pos.getSquaredDistance(goal)) * 10);

        //int blockSpeed = 10;
        //if (C.w().getBlockState(pos.up()).isLiquid() || C.w().getBlockState(pos.up().up()).isLiquid()) blockSpeed *= 14;
        //return d(pos, goal) + blockSpeed;
    }

    private static ArrayList<BlockPos> getNeighbours(BlockPos pos) {
        ArrayList<BlockPos> validNeighbours = new ArrayList<>();

        for (BlockPos offset : VALID_NEIGHBOUR_OFFSETS) {
            BlockPos possiblePos = pos.add(offset);

            if (isValid(possiblePos)) {
                validNeighbours.add(possiblePos);
            }
        }

        return validNeighbours;
    }

    // precomputing these saves a heck of a lot of time
    private static final BlockPos[] VALID_NEIGHBOUR_OFFSETS = getBlocksInRange(5);
    private static BlockPos[] getBlocksInRange(int range) {
        ArrayList<BlockPos> blocks = new ArrayList<>();
        for (int x = -range; x <= range; x++) {
            // todo: jumpboost changes EVERYTHING
            //   it only goes down 1 block, think of smth smart to go down more, 10 should be enough
            for (int y = -1; y <= 1; y++) {
                for (int z = -range; z <= range; z++) {
                    if (x + z == 0) continue;

                    // block gotta be within a circular range, sadly u dont go further diagonally :pensive:
                    if (Math.abs(x * x + y * y + z * z) >= range * (range + 1)) continue;

                    blocks.add(new BlockPos(x, y, z));
                }
            }
        }


        return blocks.toArray(new BlockPos[0]);
    }

    private static boolean isValid(BlockPos pos) {
        return C.w().getBlockState(pos).isSolid()
                && !C.w().getBlockState(pos.up()).isSolid()
                && !C.w().getBlockState(pos.offset(Direction.UP, 2)).isSolid();
    }

    private static final double PLAYER_RADIUS = 0.3;
    private static final double PLAYER_HEIGHT = 1.8;

    // todo: slime blocks, vines/ladders and non square blocks
    //  this also only works for straight jumps, neos are considered impossible...
    public static int d(BlockPos startPos, BlockPos targetPos, boolean jump) {
        Box startBox = C.w().getBlockState(startPos).getCollisionShape(C.w(), startPos).getBoundingBox().expand(PLAYER_RADIUS, 0, PLAYER_RADIUS).offset(startPos.getX(), startPos.getY(), startPos.getZ());

        VoxelShape targetCollisionShape = C.w().getBlockState(targetPos).getCollisionShape(C.w(), targetPos);
        if (targetCollisionShape.isEmpty()) return -1;

        Box targetCollisionBox = targetCollisionShape.getBoundingBox().offset(targetPos.getX(), targetPos.getY(), targetPos.getZ());
        Box targetBox = targetCollisionBox.expand(PLAYER_RADIUS, 0, PLAYER_RADIUS);
        Box targetBoxXZ = targetCollisionBox.withMinY(-64).withMaxY(255);

        Vec3d closestStartPos = getClosestPoint(startBox, targetBox);

        Vec3d velocity = new Vec3d(0, 0, 0);
        Vec3d pos = startPos.toCenterPos().add(0, 0.5, 0);

        // start at edge if possible (maybe actually try walking to edge, wtv ig)
        Vec3d distanceToEdge = closestStartPos.subtract(pos);
        distanceToEdge = Entity.adjustMovementForCollisions(null, distanceToEdge, new Box(pos.x - PLAYER_RADIUS, pos.y, pos.z - PLAYER_RADIUS, pos.x + PLAYER_RADIUS, pos.y+PLAYER_HEIGHT, pos.z + PLAYER_RADIUS), C.w(), List.of());
        pos = pos.add(distanceToEdge);

        double yaw = Math.toRadians(-RotationUtil.getRotation(RotationUtil.getClientRotation(), closestStartPos, targetBox.getCenter()).yaw);
        boolean onGround = true;
        double prevVelocityY = Double.MAX_VALUE;

        int ticks = 0;
        while ((pos.y >= startPos.getY() || pos.y >= targetPos.getY()) && ticks < 20) {
            ticks++;

            velocity = new Vec3d(
                    PathfindUtil.getMovementXvelocity(velocity.x, Math.sin(yaw), jump, onGround),
                    PathfindUtil.getMovementYvelocity(velocity.y, jump, onGround),
                    PathfindUtil.getMovementXvelocity(velocity.z, Math.cos(yaw), jump, onGround)
            );

            Box playerBox = new Box(pos.x - PLAYER_RADIUS, pos.y, pos.z - PLAYER_RADIUS, pos.x + PLAYER_RADIUS, pos.y+PLAYER_HEIGHT, pos.z + PLAYER_RADIUS);
            velocity = Entity.adjustMovementForCollisions(null, velocity, playerBox, C.w(), List.of());

            pos = pos.add(velocity);
            onGround = false;

            if (targetBoxXZ.contains(pos)) return ticks;
            // we have landed, blehhh ig
            if (velocity.y > prevVelocityY) break;

            prevVelocityY = velocity.y;
        }

        return -1;
    }

    // todo: this is probably dumb ig
    public static Vec3d getClosestPoint(Box startBox, Box endBox) {
        double posX = (startBox.maxX + startBox.minX) / 2;
        double posZ = (startBox.maxZ + startBox.minZ) / 2;

        if (startBox.maxX < endBox.minX) posX = startBox.maxX;
        else if (startBox.minX > endBox.maxX) posX = startBox.minX;

        if (startBox.maxZ < endBox.minZ) posZ = startBox.maxZ;
        else if (startBox.minZ > endBox.maxZ) posZ = startBox.minZ;

        return new Vec3d(posX, startBox.maxY, posZ);
    }

    // https://www.mcpk.wiki/wiki/Horizontal_Movement_Formulas
    public static double getMovementXvelocity(double currentVelocity, double direction, boolean jump, boolean onGround) {
        double movementMultiplier = 1.3*0.98;
        double slipperiness = onGround ? 0.6 : 1.0;
        double momentum = (currentVelocity * slipperiness * 0.91);
        double effectsMultiplier = 1.0;

        if (!onGround) return momentum + 0.02 * movementMultiplier * direction;

        double acceleration = 0.1 * movementMultiplier * effectsMultiplier * Math.pow((0.6/slipperiness), 3);
        double sprintJumpBoost = jump ? 0.2 * direction : 0;
        return momentum + acceleration * direction + sprintJumpBoost;
    }

    // https://www.mcpk.wiki/wiki/Vertical_Movement_Formulas
    public static double getMovementYvelocity(double currentVelocity, boolean jump, boolean onGround) {
        if (onGround) currentVelocity = 0;
        if (jump && onGround) return 0.42;
        return Math.max((currentVelocity - 0.08) * 0.98, -3.92);
    }
}