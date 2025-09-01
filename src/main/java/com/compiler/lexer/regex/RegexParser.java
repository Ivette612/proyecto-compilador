package com.compiler.lexer.regex;

import java.util.Stack;

import com.compiler.lexer.nfa.NFA;

/**
 * RegexParser
 * -----------
 * This class provides functionality to convert infix regular expressions into nondeterministic finite automata (NFA)
 * using Thompson's construction algorithm. It supports standard regex operators: concatenation (·), union (|),
 * Kleene star (*), optional (?), and plus (+). The conversion process uses the Shunting Yard algorithm to transform
 * infix regex into postfix notation, then builds the corresponding NFA.
 *
 * Features:
 * - Parses infix regular expressions and converts them to NFA.
 * - Supports regex operators: concatenation, union, Kleene star, optional, plus.
 * - Implements Thompson's construction rules for NFA generation.
 *
 * Example usage:
 * <pre>
 *     RegexParser parser = new RegexParser();
 *     NFA nfa = parser.parse("a(b|c)*");
 * </pre>
 */
/**
 * Parses regular expressions and constructs NFAs using Thompson's construction.
 */
public class RegexParser {
    /**
     * Default constructor for RegexParser.
     */
        public RegexParser() {
            // TODO: Implement constructor if needed
        }

    /**
     * Converts an infix regular expression to an NFA.
     *
     * @param infixRegex The regular expression in infix notation.
     * @return The constructed NFA.
     */
    public NFA parse(String infixRegex) {
        if (infixRegex == null) {
            throw new IllegalArgumentException("regex cannot be null");
        }
        String postfix = ShuntingYard.toPostfix(infixRegex);
        if (postfix.isEmpty()) {
            // ε-NFA (opcional): si prefieres lanzar error para regex vacía, cámbialo.
            com.compiler.lexer.nfa.State s = new com.compiler.lexer.nfa.State();
            com.compiler.lexer.nfa.State f = new com.compiler.lexer.nfa.State();
            s.transitions.add(new com.compiler.lexer.nfa.Transition(null, f)); // ε
            f.isFinal = true;
            return new NFA(s, f);
        }
        return buildNfaFromPostfix(postfix);
    }

    /**
     * Builds an NFA from a postfix regular expression.
     *
     * @param postfixRegex The regular expression in postfix notation.
     * @return The constructed NFA.
     */
   private NFA buildNfaFromPostfix(String postfixRegex) {
        Stack<NFA> stack = new Stack<>();

        for (int i = 0; i < postfixRegex.length(); i++) {
            char t = postfixRegex.charAt(i);

            switch (t) {
                case '·': // concatenación
                    handleConcatenation(stack);
                    break;
                case '|': // unión
                    handleUnion(stack);
                    break;
                case '*': // Kleene
                    handleKleeneStar(stack);
                    break;
                case '?': // opcional
                    handleOptional(stack);
                    break;
                case '+': // una o más
                    handlePlus(stack);
                    break;
                default:  // operando
                    if (!isOperand(t)) {
                        throw new IllegalArgumentException("Unknown token in postfix: " + t);
                    }
                    stack.push(createNfaForCharacter(t));
            }
        }

        if (stack.size() != 1) {
            throw new IllegalStateException("Malformed expression: stack has " + stack.size() + " NFAs");
        }
        return stack.pop();
    }

    /**
     * Handles the '?' operator (zero or one occurrence).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or one occurrence.
     * @param stack The NFA stack.
     */
    private void handleOptional(Stack<NFA> stack) {
        if (stack.isEmpty()) throw new IllegalStateException("Operator '?' with empty stack");
        NFA A = stack.pop();

        // Construcción de Thompson para A? = A | ε
        com.compiler.lexer.nfa.State s = new com.compiler.lexer.nfa.State();
        com.compiler.lexer.nfa.State f = new com.compiler.lexer.nfa.State();

        // s -> ε -> A.start   y   s -> ε -> f
        s.transitions.add(new com.compiler.lexer.nfa.Transition(null, A.startState));
        s.transitions.add(new com.compiler.lexer.nfa.Transition(null, f));

        // A.end deja de ser final; A.end -> ε -> f
        A.endState.isFinal = false;
        A.endState.transitions.add(new com.compiler.lexer.nfa.Transition(null, f));

        f.isFinal = true;
        stack.push(new NFA(s, f));
    }

    /**
     * Handles the '+' operator (one or more occurrences).
     * Pops an NFA from the stack and creates a new NFA that accepts one or more occurrences.
     * @param stack The NFA stack.
     */
    private void handlePlus(Stack<NFA> stack) {
        if (stack.isEmpty()) throw new IllegalStateException("Operator '+' with empty stack");
        NFA A = stack.pop();

        com.compiler.lexer.nfa.State s = new com.compiler.lexer.nfa.State();
        com.compiler.lexer.nfa.State f = new com.compiler.lexer.nfa.State();

        // s -> ε -> A.start
        s.transitions.add(new com.compiler.lexer.nfa.Transition(null, A.startState));

        // A.end deja de ser final; bucle a A.start y salida a f
        A.endState.isFinal = false;
        A.endState.transitions.add(new com.compiler.lexer.nfa.Transition(null, A.startState)); // bucle
        A.endState.transitions.add(new com.compiler.lexer.nfa.Transition(null, f));            // salida

        f.isFinal = true;
        stack.push(new NFA(s, f));
    }
    
    /**
     * Creates an NFA for a single character.
     * @param c The character to create an NFA for.
     * @return The constructed NFA.
     */
    private NFA createNfaForCharacter(char c) {
        return NFA.basic(c);
    }

    /**
     * Handles the concatenation operator (·).
     * Pops two NFAs from the stack and connects them in sequence.
     * @param stack The NFA stack.
     */
    private void handleConcatenation(Stack<NFA> stack) {
        if (stack.size() < 2) throw new IllegalStateException("Operator '·' needs two NFAs");
        NFA B = stack.pop();
        NFA A = stack.pop();
        stack.push(NFA.concat(A, B));
    }


    /**
     * Handles the union operator (|).
     * Pops two NFAs from the stack and creates a new NFA that accepts either.
     * @param stack The NFA stack.
     */
    private void handleUnion(Stack<NFA> stack) {
        if (stack.size() < 2) throw new IllegalStateException("Operator '|' needs two NFAs");
        NFA B = stack.pop();
        NFA A = stack.pop();
        stack.push(NFA.union(A, B));
    }

    /**
     * Handles the Kleene star operator (*).
     * Pops an NFA from the stack and creates a new NFA that accepts zero or more repetitions.
     * @param stack The NFA stack.
     */
    private void handleKleeneStar(Stack<NFA> stack) {
        if (stack.isEmpty()) throw new IllegalStateException("Operator '*' with empty stack");
        NFA A = stack.pop();
        stack.push(NFA.kleene(A));
    }

    /**
     * Checks if a character is an operand (not an operator).
     * @param c The character to check.
     * @return True if the character is an operand, false if it is an operator.
     */
    private boolean isOperand(char c) {
        return !(c == '|' || c == '·' || c == '*' || c == '+' || c == '?');
    }
}