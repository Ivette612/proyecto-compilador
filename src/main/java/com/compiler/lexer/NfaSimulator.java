package com.compiler.lexer;

import java.util.Set;

import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.nfa.State;

/**
 * NfaSimulator
 * ------------
 * This class provides functionality to simulate a Non-deterministic Finite Automaton (NFA)
 * on a given input string. It determines whether the input string is accepted by the NFA by processing
 * each character and tracking the set of possible states, including those reachable via epsilon (ε) transitions.
 *
 * Simulation steps:
 * - Initialize the set of current states with the ε-closure of the NFA's start state.
 * - For each character in the input, compute the next set of states by following transitions labeled with that character,
 *   and include all states reachable via ε-transitions from those states.
 * - After processing the input, check if any of the current states is a final (accepting) state.
 *
 * The class also provides a helper method to compute the ε-closure of a given state, which is the set of all states
 * reachable from the given state using only ε-transitions.
 */
/**
 * Simulator for running input strings on an NFA.
 */
public class NfaSimulator {
    /**
     * Default constructor for NfaSimulator.
     */
        public NfaSimulator() {
            // TODO: Implement constructor if needed
        }

    /**
     * Simulates the NFA on the given input string.
     * Starts at the NFA's start state and processes each character, following transitions and epsilon closures.
     * If any final state is reached after processing the input, the string is accepted.
     *
     * @param nfa The NFA to simulate.
     * @param input The input string to test.
     * @return True if the input is accepted by the NFA, false otherwise.
     */
    public boolean simulate(NFA nfa, String input) {
        if (nfa == null) throw new IllegalArgumentException("NFA cannot be null");
        if (input == null) input = "";

        // 1) ε-closure del estado inicial
        java.util.Set<State> current = new java.util.HashSet<>();
        addEpsilonClosure(nfa.startState, current);

        // 2) Consumir cada carácter
        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            java.util.Set<State> next = new java.util.HashSet<>();
            for (State s : current) {
                if (s.transitions == null) continue;
                for (com.compiler.lexer.nfa.Transition t : s.transitions) {
                    // Transición con símbolo c
                    if (t.symbol != null && t.symbol == c) {
                        // Agregar ε-closure del destino
                        addEpsilonClosure(t.toState, next);
                    }
                }
            }
            // Avanzar el conjunto actual
            current = next;

            // Si no hay estados alcanzables, rechazo temprano
            if (current.isEmpty()) return false;
        }

        // 3) Aceptación si algún estado actual es final
        for (State s : current) {
            if (s.isFinal) return true;
        }
        return false;
    }

    /**
     * Computes the epsilon-closure: all states reachable from 'start' using only epsilon (null) transitions.
     *
     * @param start The starting state.
     * @param closureSet The set to accumulate reachable states.
     */
    private void addEpsilonClosure(State start, Set<State> closureSet) {
        // Si ya lo visitamos, no repetir
        if (!closureSet.add(start)) return;

        if (start.transitions == null) return;
        for (com.compiler.lexer.nfa.Transition t : start.transitions) {
            if (t.symbol == null) { // ε-transición
                addEpsilonClosure(t.toState, closureSet);
            }
        }
    }
}