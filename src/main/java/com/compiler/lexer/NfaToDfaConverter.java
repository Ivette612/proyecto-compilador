package com.compiler.lexer;

import java.util.List;
import java.util.Set;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;

/**
 * NfaToDfaConverter
 * -----------------
 * This class provides a static method to convert a Non-deterministic Finite Automaton (NFA)
 * into a Deterministic Finite Automaton (DFA) using the standard subset construction algorithm.
 */
/**
 * Utility class for converting NFAs to DFAs using the subset construction algorithm.
 */
public class NfaToDfaConverter {
    /**
     * Default constructor for NfaToDfaConverter.
     */
    public NfaToDfaConverter() {
        // No-op
    }

    /**
     * Converts an NFA to a DFA using the subset construction algorithm.
     * Each DFA state represents a set of NFA states. Final states are marked if any NFA state in the set is final.
     *
     * @param nfa The input NFA
     * @param alphabet The input alphabet (set of characters)
     * @return The resulting DFA
     */
    public static DFA convertNfaToDfa(NFA nfa, Set<Character> alphabet) {
        // 1) Estado inicial del DFA: ε-closure({nfa.startState})
        java.util.Set<State> startSet = new java.util.HashSet<State>();
        startSet.add(nfa.startState);
        java.util.Set<State> startClosure = epsilonClosure(startSet);

        // Estructuras: lista de estados DFA y “cola” de procesamiento (BFS)
        java.util.List<DfaState> dfaStates = new java.util.ArrayList<DfaState>();
        java.util.List<DfaState> worklist = new java.util.ArrayList<DfaState>();
        int head = 0;

        // Crear estado DFA inicial
        DfaState startDfa = findDfaState(dfaStates, startClosure);
        if (startDfa == null) {
            startDfa = new DfaState(startClosure);
            dfaStates.add(startDfa);
            worklist.add(startDfa);
        }

        // 2) Mientras haya estados DFA sin procesar
        while (head < worklist.size()) {
            DfaState current = worklist.get(head++);
            java.util.Set<State> currentSubset = current.getName(); // conjunto de NFA-states

            // Para cada símbolo en el alfabeto (no incluye epsilon)
            for (Character symObj : alphabet) {
                if (symObj == null) continue;
                char a = symObj.charValue();

                // move y luego ε-closure
                java.util.Set<State> moved = move(currentSubset, a);
                if (moved.isEmpty()) {
                    // Sin transición por 'a' desde este subconjunto
                    continue;
                }
                java.util.Set<State> target = epsilonClosure(moved);

                // Buscar/crear estado DFA destino
                DfaState to = findDfaState(dfaStates, target);
                if (to == null) {
                    to = new DfaState(target);
                    dfaStates.add(to);
                    worklist.add(to);
                }

                // Registrar transición determinista
                current.addTransition(Character.valueOf(a), to);
            }
        }

        // 3) Los estados finales se marcaron en el constructor de DfaState
        // 4) Construir y devolver el DFA
        return new DFA(startDfa, dfaStates);
    }

    /**
     * Computes the epsilon-closure of a set of NFA states.
     * The epsilon-closure is the set of states reachable by epsilon (null) transitions.
     *
     * @param states The set of NFA states.
     * @return The epsilon-closure of the input states.
     */
    private static java.util.Set<State> epsilonClosure(java.util.Set<State> states) {
        java.util.Set<State> closure = new java.util.HashSet<State>();
        java.util.List<State> stack = new java.util.ArrayList<State>();

        // Inicializar con los estados de entrada
        for (State s : states) {
            if (!closure.contains(s)) {
                closure.add(s);
                stack.add(s);
            }
        }

        // DFS iterativa por transiciones epsilon (symbol == null)
        while (!stack.isEmpty()) {
            State u = stack.remove(stack.size() - 1);
            java.util.List<State> eps = u.getEpsilonTransitions();
            for (int i = 0; i < eps.size(); i++) {
                State v = eps.get(i);
                if (!closure.contains(v)) {
                    closure.add(v);
                    stack.add(v);
                }
            }
        }
        return closure;
    }

    /**
     * Returns the set of states reachable from a set of NFA states by a given symbol.
     *
     * @param states The set of NFA states.
     * @param symbol The input symbol.
     * @return The set of reachable states.
     */
    private static java.util.Set<State> move(java.util.Set<State> states, char symbol) {
        java.util.Set<State> out = new java.util.HashSet<State>();
        for (State s : states) {
            java.util.List<State> nexts = s.getTransitions(symbol);
            for (int i = 0; i < nexts.size(); i++) {
                out.add(nexts.get(i));
            }
        }
        return out;
    }

    /**
     * Finds an existing DFA state representing a given set of NFA states.
     *
     * @param dfaStates The list of DFA states.
     * @param targetNfaStates The set of NFA states to search for.
     * @return The matching DFA state, or null if not found.
     */
    private static DfaState findDfaState(List<DfaState> dfaStates, Set<State> targetNfaStates) {
        for (int i = 0; i < dfaStates.size(); i++) {
            DfaState d = dfaStates.get(i);
            // Igualdad por conjunto de NFA-states (mismo objeto State, misma cardinalidad)
            if (d.getName().equals(targetNfaStates)) {
                return d;
            }
        }
        return null;
    }
}
