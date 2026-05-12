package com.scale.scalehackv4.utils.player;

import com.scale.scalehackv4.utils.C;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

import java.util.*;

// basic A* impl from https://en.wikipedia.org/wiki/A*_search_algorithm
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
            if (current.equals(goal)) {
                return reconstructPath(cameFrom, current);
            }

            openSet.remove(current);

            for (BlockPos neighbor : getNeighbours(current)) {
                // d(current,neighbor) is the weight of the edge from current to neighbor
                // tentative_gScore is the distance from start to the neighbor through current
                int tentative_gScore = gScore.get(current) + d(neighbor);
                if (tentative_gScore < gScore.getOrDefault(neighbor, Integer.MAX_VALUE)) {
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
        // todo: make better heuristic
        return (int) (Math.sqrt(pos.getSquaredDistance(goal)) * 10);

        //int blockSpeed = 10;
        //if (C.w().getBlockState(pos.up()).isLiquid() || C.w().getBlockState(pos.up().up()).isLiquid()) blockSpeed *= 14;
        //return d(pos, goal) + blockSpeed;
    }

    private static ArrayList<BlockPos> getNeighbours(BlockPos pos) {
        ArrayList<BlockPos> validNeighbours = new ArrayList<>();

        // ignore block directly above/below as they are always invalid
        for (int i = 2; i < Direction.values().length; i++) {
            Direction direction = Direction.values()[i];
            BlockPos newPos = pos.offset(direction);

            // we can go up/down on each neighbour block too!
            if (isValid(newPos.up()) && C.w().getBlockState(pos.up(3)).isSolid()) validNeighbours.add(newPos.up());
            else if (isValid(newPos.down()) && C.w().getBlockState(newPos.up(3)).isSolid()) validNeighbours.add(newPos.down());
            // we cant go over it, we cant go under it, we have to go through it!
            else if (isValid(newPos)) validNeighbours.add(newPos);
        }

        return validNeighbours;
    }

    private static boolean isValid(BlockPos pos) {
        return C.w().getBlockState(pos).isSolid()
                && !C.w().getBlockState(pos.up()).isSolid()
                && !C.w().getBlockState(pos.offset(Direction.UP, 2)).isSolid();
    }

    private static int d(BlockPos pos) {
        return C.w().getBlockState(pos.up()).isLiquid() || C.w().getBlockState(pos.up().up()).isLiquid() ? 14 : 1;
    }
}