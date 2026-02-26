package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.utils.C;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

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

        // The set of discovered nodes that may need to be (re-)expanded.
        // Initially, only the start node is known.
        // This is usually implemented as a min-heap or priority queue rather than a hash-set.
        ArrayList<BlockPos> openSet = new ArrayList<>();
        openSet.add(start);

        // For node n, cameFrom[n] is the node immediately preceding it on the cheapest path from the start
        // to n currently known.
        HashMap<BlockPos, BlockPos> cameFrom = new HashMap<>();

        // For node n, gScore[n] is the currently known cost of the cheapest path from start to n.
        HashMap<BlockPos, Integer> gScore = new HashMap<>();
        gScore.put(start, 0);

        // For node n, fScore[n] := gScore[n] + h(n). fScore[n] represents our current best guess as to
        // how cheap a path could be from start to finish if it goes through n.
        HashMap<BlockPos, Integer> fScore = new HashMap<>();
        int startDistance = d(start, goal);
        fScore.put(start, startDistance);

        while (!openSet.isEmpty()) {
            // This operation can occur in O(Log(N)) time if openSet is a min-heap or a priority queue
            openSet.sort(Comparator.comparingInt(fScore::get));
            BlockPos current = openSet.getFirst();
            if (current.equals(goal)) return reconstructPath(cameFrom, current);

            // if we have gone miles of track, there probably isnt a path
            else if (fScore.get(current)-startDistance > fScore.get(start)*100) return null;

            openSet.remove(current);

            for (BlockPos neighbor : getNeighbours(current)) {
                // d(current,neighbor) is the weight of the edge from current to neighbor
                // tentative_gScore is the distance from start to the neighbor through current
                int distance = d(current, neighbor);
                int tentative_gScore = gScore.get(current) + distance;
                if (tentative_gScore < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    if (!canJumpLand(current, neighbor)) continue;

                    // This path to neighbor is better than any previous one. Record it!
                    cameFrom.put(neighbor, current);
                    gScore.put(neighbor, tentative_gScore);
                    fScore.put(neighbor, tentative_gScore + h(neighbor, goal));
                    if (!openSet.contains(neighbor)) {
                        openSet.add(neighbor);
                    }
                }
            }
        }

        // Open set is empty but goal was never reached
        return null;
    }

    private static int h(BlockPos pos, BlockPos goal) {
        // todo: make heuristic
        return pos.getManhattanDistance(goal);

        //int blockSpeed = 10;
        //if (C.w().getBlockState(pos.up()).isLiquid() || C.w().getBlockState(pos.up().up()).isLiquid()) blockSpeed *= 14;
        //return d(pos, goal) + blockSpeed;
    }

    private static int d(BlockPos current, BlockPos neighbour) {
        return (int) Math.sqrt(current.getSquaredDistance(neighbour));
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
    public static boolean canJumpLand(BlockPos startPos, BlockPos targetPos) {
        Box startBox = new Box(
                startPos.getX()-PLAYER_RADIUS, startPos.getY(), startPos.getZ()-PLAYER_RADIUS,
                startPos.getX()+1+PLAYER_RADIUS, startPos.getY()+1, startPos.getZ()+1+PLAYER_RADIUS
        );
        Box targetBox = new Box(
                targetPos.getX()-PLAYER_RADIUS, targetPos.getY(), targetPos.getZ()-PLAYER_RADIUS,
                targetPos.getX()+1+PLAYER_RADIUS, targetPos.getY()+1, targetPos.getZ()+1+PLAYER_RADIUS
        );
        Box targetBoxXZ = new Box(targetPos.getX(),-64, targetPos.getZ(), targetPos.getX()+1, 255, targetPos.getZ()+1);

        Vec3d closestStartPos = getClosestPoint(startBox, targetBox);
        Vec3d closestTargetPos = getClosestPoint(targetBox, startBox);

        Vec3d velocity = new Vec3d(0, 0, 0);
        Vec3d pos = new Vec3d(startPos.getX(), startPos.getY(), startPos.getZ());

        // start at edge if possible (maybe actually try walking to edge, wtv ig)
        Vec3d distanceToEdge = closestStartPos.subtract(pos);
        distanceToEdge = Entity.adjustMovementForCollisions(C.p(), distanceToEdge, new Box(pos.x - PLAYER_RADIUS, pos.y, pos.z - PLAYER_RADIUS, pos.x + PLAYER_RADIUS, pos.y+PLAYER_HEIGHT, pos.z + PLAYER_RADIUS), C.w(), List.of());
        pos = pos.add(distanceToEdge);

        double yaw = Math.toRadians(-RotationUtil.getRotation(RotationUtil.getClientRotation(), closestStartPos, closestTargetPos).yaw);
        boolean onGround = true;
        double prevVelocityY = Double.MAX_VALUE;

        while (pos.y >= startPos.getY() || pos.y >= targetPos.getY()) {
            velocity = new Vec3d(
                    PathfindUtil.getMovementXvelocity(velocity.x, Math.sin(yaw), true, onGround),
                    PathfindUtil.getMovementYvelocity(velocity.y, true, onGround),
                    PathfindUtil.getMovementXvelocity(velocity.z, Math.cos(yaw), true, onGround)
            );

            Box playerBox = new Box(pos.x - PLAYER_RADIUS, pos.y, pos.z - PLAYER_RADIUS, pos.x + PLAYER_RADIUS, pos.y+PLAYER_HEIGHT, pos.z + PLAYER_RADIUS);
            velocity = Entity.adjustMovementForCollisions(C.p(), velocity, playerBox, C.w(), List.of());

            pos = pos.add(velocity);
            onGround = false;

            if (targetBoxXZ.contains(pos)) return true;
            // we have landed, blehhh ig
            if (velocity.y > prevVelocityY) break;

            prevVelocityY = velocity.y;
        }

        return false;
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