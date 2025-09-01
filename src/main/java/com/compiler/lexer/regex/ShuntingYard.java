package com.compiler.lexer.regex;

/**
 * Utility class for regular expression parsing using the Shunting Yard
 * algorithm.
 * <p>
 * Provides methods to preprocess regular expressions by inserting explicit
 * concatenation operators, and to convert infix regular expressions to postfix
 * notation for easier parsing and NFA construction.
 */
/**
 * Utility class for regular expression parsing using the Shunting Yard
 * algorithm.
 */
public class ShuntingYard {

    /**
     * Default constructor for ShuntingYard.
     */
    public ShuntingYard() {
        // No-op
    }

    /**
     * Inserts the explicit concatenation operator ('·') into the regular
     * expression according to standard rules. This makes implicit
     * concatenations explicit, simplifying later parsing.
    * Reglas (c1 actual, c2 siguiente) -> insertar '·' si:
     * 1) operando c1 y operando c2           (ab -> a·b)
     * 2) operando c1 y c2 == '('             (a( -> a·()
     * 3) c1 == ')'    y operando c2          ()a -> )·a
     * 4) unario c1    y operando c2          (*a -> *·a, +a, ?a)
     * 5) c1 == ')'    y c2 == '('            ( )( -> )·(
     * @param regex Input regular expression (may have implicit concatenation).
     * @return Regular expression with explicit concatenation operators.
     */
     public static String insertConcatenationOperator(String regex) {
        if (regex == null || regex.isEmpty()) return regex;
        StringBuilder out = new StringBuilder(regex.length() * 2);
        for (int i = 0; i < regex.length(); i++) {
            char c1 = regex.charAt(i);
            out.append(c1);
            if (i + 1 < regex.length()) {
                char c2 = regex.charAt(i + 1);

                boolean cond1 = isOperand(c1) && isOperand(c2);
                boolean cond2 = isOperand(c1) && c2 == '(';
                boolean cond3 = c1 == ')' && isOperand(c2);
                boolean cond4 = isUnary(c1)   && isOperand(c2);
                boolean cond5 = c1 == ')' && c2 == '(';

                if (cond1 || cond2 || cond3 || cond4 || cond5) {
                    out.append('·'); // concatenación explícita
                }
            }
        }
        return out.toString();
    }


    /**
     * Determines if the given character is an operand (not an operator or
     * parenthesis).
     *
     * @param c Character to evaluate.
     * @return true if it is an operand, false otherwise.
     */
     private static boolean isOperand(char c) {
        return !(c == '|' || c == '*' || c == '?' || c == '+' || c == '(' || c == ')' || c == '·');
    }

    private static boolean isUnary(char c) {
        return (c == '*' || c == '+' || c == '?');
    }

    private static int precedence(char op) {
        if (op == '*' || op == '+' || op == '?') return 3; // unarios
        if (op == '·') return 2;                            // concatenación
        if (op == '|') return 1;                            // unión
        return -1;
    }

    private static boolean isLeftAssociative(char op) {
        // Unarios se consideran de asociatividad a la derecha
        return !(op == '*' || op == '+' || op == '?');
    }

    /**
     * Converts an infix regular expression to postfix notation using the
     * Shunting Yard algorithm. This is useful for constructing NFAs from
     * regular expressions.
     *
     * @param infixRegex Regular expression in infix notation.
     * @return Regular expression in postfix notation.
     */
      public static String toPostfix(String infixRegex) {
        if (infixRegex == null || infixRegex.isEmpty()) return "";

        // 1) Preprocesar: insertar concatenación explícita con '·'
        String s = insertConcatenationOperator(infixRegex);

        // 2) Shunting–Yard con pila implementada con arreglo para evitar imports
        StringBuilder output = new StringBuilder(s.length());
        char[] stack = new char[s.length()];
        int top = -1; // pila vacía

        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);

            if (isOperand(ch)) {
                output.append(ch);
            } else if (ch == '(') {
                stack[++top] = ch;
            } else if (ch == ')') {
                // desapilar hasta '('
                while (top >= 0 && stack[top] != '(') {
                    output.append(stack[top--]);
                }
                if (top < 0) {
                    throw new IllegalArgumentException("Unbalanced parentheses: missing '('");
                }
                top--; // descartar '('
            } else { // operador
                int pCh = precedence(ch);
                if (pCh < 0) {
                    throw new IllegalArgumentException("Unknown operator: " + ch);
                }
                // desapilar mientras top tiene mayor/igual precedencia (si izq-asoc)
                while (top >= 0 && stack[top] != '(') {
                    char topOp = stack[top];
                    int pTop = precedence(topOp);
                    if (pTop > pCh || (pTop == pCh && isLeftAssociative(ch))) {
                        output.append(stack[top--]);
                    } else {
                        break;
                    }
                }
                stack[++top] = ch;
            }
        }

        // 3) Vaciar pila
        while (top >= 0) {
            char op = stack[top--];
            if (op == '(' || op == ')') {
                throw new IllegalArgumentException("Unbalanced parentheses at end");
            }
            output.append(op);
        }

        return output.toString();
    }
}
