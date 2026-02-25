package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.utils.C;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;

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
                // duplicates are very possible
                if (gScore.containsKey(neighbor)) continue;

                // d(current,neighbor) is the weight of the edge from current to neighbor
                // tentative_gScore is the distance from start to the neighbor through current
                int tentative_gScore = gScore.get(current) + d(current, neighbor);
                if (tentative_gScore < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
                    // todo: this node is good, double check its validity

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
        // todo: make heuristic influence as much
        return 0;

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
            // todo: jumpboost changes EVERYTHING + blocks below are ignored
            for (int y = 0; y <= 1; y++) {
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
}