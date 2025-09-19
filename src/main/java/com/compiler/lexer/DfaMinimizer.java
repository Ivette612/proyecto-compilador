/**
 * DfaMinimizer
 * -------------
 * This class provides an implementation of DFA minimization using the table-filling algorithm.
 * It identifies and merges equivalent states in a deterministic finite automaton (DFA),
 * resulting in a minimized DFA with the smallest number of states that recognizes the same language.
 *
 * Main steps:
 *   1. Initialization: Mark pairs of states as distinguishable if one is final and the other is not.
 *   2. Iterative marking: Mark pairs as distinguishable if their transitions lead to distinguishable states,
 *      or if only one state has a transition for a given symbol.
 *   3. Partitioning: Group equivalent states and build the minimized DFA.
 *
 * Helper methods are provided for partitioning, union-find operations, and pair representation.
 */
package com.compiler.lexer;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;

/**
 * Implements DFA minimization using the table-filling algorithm.
 */
/**
 * Utility class for minimizing DFAs using the table-filling algorithm.
 */
public class DfaMinimizer {
    /**
     * Default constructor for DfaMinimizer.
     */
    public DfaMinimizer() {
        // no-op
    }

    /**
     * Minimizes a given DFA using the table-filling algorithm.
     *
     * @param originalDfa The original DFA to be minimized.
     * @param alphabet The set of input symbols.
     * @return A minimized DFA equivalent to the original.
     */
    public static DFA minimizeDfa(DFA originalDfa, Set<Character> alphabet) {
        if (originalDfa == null || originalDfa.getStartState() == null) {
            return originalDfa;
        }

        // Collect states
        List<DfaState> allStates = originalDfa.getStates();
        // Derive alphabet if null or empty
        if (alphabet == null || alphabet.isEmpty()) {
            alphabet = deriveAlphabet(allStates);
        }

        // Build table: Pair -> isDistinguishable
        Map<Pair, Boolean> table = new java.util.HashMap<Pair, Boolean>();

        // Initialize: mark pairs (p,q) where finality differs
        int n = allStates.size();
        for (int i = 0; i < n; i++) {
            DfaState si = allStates.get(i);
            for (int j = i + 1; j < n; j++) {
                DfaState sj = allStates.get(j);
                Pair pq = new Pair(si, sj);
                boolean distinguishable = (si.isFinal() != sj.isFinal());
                table.put(pq, java.lang.Boolean.valueOf(distinguishable));
            }
        }

        // Iterative marking
        boolean changed = true;
        while (changed) {
            changed = false;

            for (int i = 0; i < n; i++) {
                DfaState si = allStates.get(i);
                for (int j = i + 1; j < n; j++) {
                    DfaState sj = allStates.get(j);
                    Pair pq = new Pair(si, sj);

                    // If already marked as distinguishable, skip
                    java.lang.Boolean mark = table.get(pq);
                    if (mark != null && mark.booleanValue()) {
                        continue;
                    }

                    boolean toMark = false;

                    // For each symbol, compare destinations
                    for (Character a : alphabet) {
                        DfaState ti = si.getTransition(a.charValue());
                        DfaState tj = sj.getTransition(a.charValue());

                        // Rule: if exactly one has transition -> distinguishable
                        if ((ti == null) ^ (tj == null)) {
                            toMark = true;
                            break;
                        }

                        // If both have transitions, check if their pair is distinguishable
                        if (ti != null && tj != null) {
                            if (ti != tj) {
                                Pair destPair = new Pair(ti, tj);
                                java.lang.Boolean destMark = table.get(destPair);
                                if (destMark != null && destMark.booleanValue()) {
                                    toMark = true;
                                    break;
                                }
                            }
                        }
                    }

                    if (toMark) {
                        table.put(pq, java.lang.Boolean.TRUE);
                        changed = true;
                    }
                }
            }
        }

        // Partition equivalent states (unmarked pairs) and rebuild minimized DFA
        List<Set<DfaState>> partitions = createPartitions(allStates, table);

        // Create a representative minimized state per partition
        java.util.Map<Set<DfaState>, DfaState> partToNew = new java.util.HashMap<Set<DfaState>, DfaState>();
        java.util.List<DfaState> minStates = new java.util.ArrayList<DfaState>();

        for (Set<DfaState> block : partitions) {
            // Union of underlying NFA-state names
            java.util.Set<com.compiler.lexer.nfa.State> union = new java.util.HashSet<com.compiler.lexer.nfa.State>();
            boolean isFinal = false;
            for (DfaState s : block) {
                union.addAll(s.getName());
                if (s.isFinal()) isFinal = true;
            }
            DfaState newS = new DfaState(union);
            newS.setFinal(isFinal);
            partToNew.put(block, newS);
            minStates.add(newS);
        }

        // Helper: map original state to its partition (block)
        java.util.Map<DfaState, Set<DfaState>> stateToBlock = new java.util.HashMap<DfaState, Set<DfaState>>();
        for (Set<DfaState> block : partitions) {
            for (DfaState s : block) stateToBlock.put(s, block);
        }

        // Reconstruct transitions: use any representative of each block
        for (Set<DfaState> block : partitions) {
            DfaState rep = block.iterator().next();
            DfaState fromNew = partToNew.get(block);

            for (Character a : alphabet) {
                DfaState dst = rep.getTransition(a.charValue());
                if (dst != null) {
                    Set<DfaState> dstBlock = stateToBlock.get(dst);
                    if (dstBlock != null) {
                        DfaState toNew = partToNew.get(dstBlock);
                        fromNew.addTransition(a, toNew);
                    }
                }
            }
        }

        // Start state of minimized DFA
        Set<DfaState> startBlock = stateToBlock.get(originalDfa.getStartState());
        DfaState minStart = partToNew.get(startBlock);

        return new DFA(minStart, minStates);
    }

    /**
     * Groups equivalent states into partitions using union-find.
     *
     * @param allStates List of all DFA states.
     * @param table Table indicating which pairs are distinguishable.
     * @return List of partitions, each containing equivalent states.
     */
    private static List<Set<DfaState>> createPartitions(List<DfaState> allStates, Map<Pair, Boolean> table) {
        // Union-Find parent map
        Map<DfaState, DfaState> parent = new java.util.HashMap<DfaState, DfaState>();
        for (DfaState s : allStates) parent.put(s, s);

        // Union unmarked pairs (equivalent)
        int n = allStates.size();
        for (int i = 0; i < n; i++) {
            DfaState si = allStates.get(i);
            for (int j = i + 1; j < n; j++) {
                DfaState sj = allStates.get(j);
                Pair pq = new Pair(si, sj);
                java.lang.Boolean marked = table.get(pq);
                if (marked == null || !marked.booleanValue()) {
                    union(parent, si, sj);
                }
            }
        }

        // Group by root
        Map<DfaState, Set<DfaState>> groups = new java.util.HashMap<DfaState, Set<DfaState>>();
        for (DfaState s : allStates) {
            DfaState root = find(parent, s);
            Set<DfaState> set = groups.get(root);
            if (set == null) {
                set = new java.util.HashSet<DfaState>();
                groups.put(root, set);
            }
            set.add(s);
        }

        return new java.util.ArrayList<Set<DfaState>>(groups.values());
    }

    /**
     * Finds the root parent of a state in the union-find structure.
     * Implements path compression for efficiency.
     *
     * @param parent Parent map.
     * @param state State to find.
     * @return Root parent of the state.
     */
    private static DfaState find(Map<DfaState, DfaState> parent, DfaState state) {
        DfaState p = parent.get(state);
        if (p == state) return state;
        DfaState root = find(parent, p);
        parent.put(state, root);
        return root;
    }

    /**
     * Unites two states in the union-find structure.
     *
     * @param parent Parent map.
     * @param s1 First state.
     * @param s2 Second state.
     */
    private static void union(Map<DfaState, DfaState> parent, DfaState s1, DfaState s2) {
        DfaState r1 = find(parent, s1);
        DfaState r2 = find(parent, s2);
        if (r1 != r2) {
            parent.put(r2, r1);
        }
    }

    /**
     * Helper: derive alphabet from transitions when not provided.
     */
    private static Set<Character> deriveAlphabet(List<DfaState> states) {
        Set<Character> alpha = new java.util.HashSet<Character>();
        for (DfaState s : states) {
            Map<Character, DfaState> t = s.getTransitions();
            if (t == null) continue;
            for (Character c : t.keySet()) {
                if (c != null) alpha.add(c);
            }
        }
        return alpha;
    }

    /**
     * Helper class to represent a pair of DFA states in canonical order.
     * Used for table indexing and comparison.
     */
    private static class Pair {
        final DfaState s1;
        final DfaState s2;

        /**
         * Constructs a pair in canonical order (lowest id first).
         * @param a First state.
         * @param b Second state.
         */
        public Pair(DfaState a, DfaState b) {
            if (a.id <= b.id) {
                this.s1 = a;
                this.s2 = b;
            } else {
                this.s1 = b;
                this.s2 = a;
            }
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Pair)) return false;
            Pair other = (Pair) o;
            return this.s1.id == other.s1.id && this.s2.id == other.s2.id;
        }

        @Override
        public int hashCode() {
            // simple combination of ids
            int h = 17;
            h = 31 * h + s1.id;
            h = 31 * h + s2.id;
            return h;
        }
    }
}
