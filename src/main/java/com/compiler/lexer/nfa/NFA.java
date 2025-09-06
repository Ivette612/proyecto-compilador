package com.compiler.lexer.nfa;

/**
 * Represents a Non-deterministic Finite Automaton (NFA) with a start and end state.
 * <p>
 * An NFA is used in lexical analysis to model regular expressions and pattern matching.
 * This class encapsulates the start and end states of the automaton.
 */

public class NFA {
    /**
     * The initial (start) state of the NFA.
     */
    public final State startState;

    /**
     * The final (accepting) state of the NFA.
     */
    public final State endState;

    /**
     * Constructs a new NFA with the given start and end states.
     * @param start The initial state.
     * @param end The final (accepting) state.
     */
    public NFA(State start, State end) {
        this.startState = start;
        this.endState = end;
        if (this.endState != null) {
            this.endState.isFinal = true;
        }
    }

    /**
     * Returns the initial (start) state of the NFA.
     * @return the start state
     */
    public State getStartState() {
        return this.startState;
    }

    /* =======================
       Thompson constructions
       ======================= */

    /**
     * Basic NFA for a single symbol: s --symbol--> f
     */
    public static NFA basic(char symbol) {
        State s = new State();
        State f = new State();
        // transición con símbolo
        s.transitions.add(new Transition(symbol, f));
        f.isFinal = true;
        return new NFA(s, f);
    }

    /**
     * Concatenation: A.B
     * Conecta el estado final de A por ε con el estado inicial de B.
     * El inicio es A.start y el final es B.end.
     */
    public static NFA concat(NFA A, NFA B) {
        // A.end deja de ser final al enlazar
        A.endState.isFinal = false;
        // ε-transición (null) de A.end -> B.start
        A.endState.transitions.add(new Transition(null, B.startState));
        // El nuevo NFA inicia en A.start y termina en B.end
        return new NFA(A.startState, B.endState);
    }

    /**
     * Union: A|B
     * Crea nuevo inicio S y final F; ε: S->A.start, S->B.start; A.end->F, B.end->F
     */
    public static NFA union(NFA A, NFA B) {
        State s = new State();
        State f = new State();

        // Inicio con ramas ε a A y B
        s.transitions.add(new Transition(null, A.startState));
        s.transitions.add(new Transition(null, B.startState));

        // Quitar marca de finales anteriores y conectarlos a F por ε
        A.endState.isFinal = false;
        B.endState.isFinal = false;
        A.endState.transitions.add(new Transition(null, f));
        B.endState.transitions.add(new Transition(null, f));

        f.isFinal = true;
        return new NFA(s, f);
    }

    /**
     * Kleene star: A*
     * Nuevo inicio S y final F; ε: S->A.start, S->F, A.end->A.start, A.end->F
     */
    public static NFA kleene(NFA A) {
        State s = new State();
        State f = new State();

        // S -> ε -> A.start  y  S -> ε -> F
        s.transitions.add(new Transition(null, A.startState));
        s.transitions.add(new Transition(null, f));

        // A.end deja de ser final; bucle y salida a F por ε
        A.endState.isFinal = false;
        A.endState.transitions.add(new Transition(null, A.startState));
        A.endState.transitions.add(new Transition(null, f));

        f.isFinal = true;
        return new NFA(s, f);
    }
}