package com.compiler.lexer.dfa;

import java.util.List;

/**
 * DFA
 * ---
 * Represents a complete Deterministic Finite Automaton (DFA).
 * Contains the start state and a list of all states in the automaton.
 */
public class DFA {
    /**
     * The starting state of the DFA.
     */
    public final DfaState startState;

    /**
     * A list of all states in the DFA (nombre esperado por algunos clientes).
     */
    public final List<DfaState> allStates;

    /**
     * Alias del listado de estados (por compatibilidad con otros clientes).
     */
    public final List<DfaState> states;

    /**
     * Constructs a new DFA.
     * @param startState The starting state of the DFA.
     * @param states  A list of all states in the DFA.
     */
    public DFA(DfaState startState, List<DfaState> states) {
        this.startState = startState;
        this.states = states;
        this.allStates = states; // alias para compatibilidad (Main.java usa allStates)
    }

    /** Devuelve el estado inicial del DFA. */
    public DfaState getStartState() {
        return startState;
    }

    /** Devuelve la lista de estados del DFA (alias a allStates). */
    public List<DfaState> getStates() {
        return states;
    }

    /** (Opcional) Getter explícito si lo quieres usar en algún lado. */
    public List<DfaState> getAllStates() {
        return allStates;
    }
}
