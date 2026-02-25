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
        fScore.put(start, h(start, goal));

        while (!openSet.isEmpty()) {
            // This operation can occur in O(Log(N)) time if openSet is a min-heap or a priority queue
            openSet.sort(Comparator.comparingInt(fScore::get));
            BlockPos current = openSet.getFirst();
            if (current.equals(goal)) {
                return reconstructPath(cameFrom, current);
            }

            openSet.remove(current);

            for (BlockPos neighbor : neighbours(current)) {
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

    // i think this is manhattan distance, its fast, thats all the matters
    private static int h(BlockPos pos, BlockPos goal) {
        return (int) Math.sqrt(pos.getSquaredDistance(goal));
    }

    private static int d(BlockPos pos) {
        return C.w().getBlockState(pos.up()).isLiquid() || C.w().getBlockState(pos.up().up()).isLiquid() ? 14 : 1;
    }

    private static ArrayList<BlockPos> neighbours(BlockPos pos) {
        ArrayList<BlockPos> validNeighbours = new ArrayList<>();

        for (Direction direction : Direction.values()) {
            BlockPos newPos = pos.offset(direction);

            if (direction != Direction.UP) {
                if (isValid(newPos.up())) validNeighbours.add(newPos.up());
            }

            if (isValid(newPos)) {
                validNeighbours.add(newPos);
            }
            // assuming u dont care about fall damage, you can go anywhere below the positions
            else {
                while (!isValid(newPos) && newPos.getY() >= -64) newPos = newPos.down();
                if (isValid(newPos)) validNeighbours.add(newPos);
            }
        }

        return validNeighbours;
    }

    private static boolean isValid(BlockPos pos) {
        return C.w().getBlockState(pos).isSolid()
                && !C.w().getBlockState(pos.up()).isSolid()
                && !C.w().getBlockState(pos.offset(Direction.UP, 2)).isSolid();
    }
}