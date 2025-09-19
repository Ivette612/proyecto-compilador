package com.compiler.lexer;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;

/**
 * DfaSimulator
 * ------------
 * This class simulates the execution of a Deterministic Finite Automaton (DFA) on a given input string.
 * It provides a method to determine whether a given input string is accepted by a specified DFA.
 * The simulation starts at the DFA's start state and processes each character in the input string,
 * following the corresponding transitions. If at any point there is no valid transition for a character,
 * the input is rejected. After processing all characters, the input is accepted if the final state reached
 * is an accepting (final) state.
 *
 * Example usage:
 * <pre>
 *     DfaSimulator simulator = new DfaSimulator();
 *     boolean accepted = simulator.simulate(dfa, "inputString");
 * </pre>
 */
/**
 * Simulator for running input strings on a DFA.
 */
public class DfaSimulator {
    /**
     * Default constructor for DfaSimulator.
     */
        public DfaSimulator() {
            // TODO: Implement constructor if needed
        }
    /**
     * Simulates the DFA on the given input string.
     * Starts at the DFA's start state and processes each character, following transitions.
     * If a transition does not exist for a character, the input is rejected.
     *
     * @param dfa The DFA to simulate.
     * @param input The input string to test.
     * @return True if the input is accepted by the DFA, false otherwise.
     */
    public boolean simulate(DFA dfa, String input) {
        if (dfa == null) return false;
        if (input == null) input = "";

        DfaState current = dfa.getStartState();
        for (int i = 0; i < input.length(); i++) {
            char a = input.charAt(i);
            DfaState next = current.getTransition(a);
            if (next == null) return false;
            current = next;
        }
        return current.isFinal;
    }
}