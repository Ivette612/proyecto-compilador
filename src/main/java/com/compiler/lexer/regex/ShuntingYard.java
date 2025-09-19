package com.compiler.lexer.regex;

/**
 * ShuntingYard ------------ Inserta el operador de concatenación explícita '·'
 * y convierte expresiones regulares infijas a postfijas (notación polaca
 * inversa) usando Shunting–Yard.
 *
 * Cambios clave: - Se considera el espacio ' ' como OPERANDO literal, para que
 * reglas como " +" (espacio uno o más) funcionen correctamente.
 */
public final class ShuntingYard {

    private ShuntingYard() {
        /* util class */ }

    /**
     * Inserta el operador de concatenación explícita '·' donde corresponde. No
     * modifica la semántica de la expresión.
     *
     * @param infixRegex expresión regular en notación infija
     * @return cadena con concatenaciones explícitas
     */
    public static String insertConcatenationOperator(String infixRegex) {
        if (infixRegex == null || infixRegex.isEmpty()) {
            return "";
        }

        String s = infixRegex;
        StringBuilder out = new StringBuilder(s.length() * 2);

        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            out.append(c);

            if (i + 1 < s.length()) {
                char n = s.charAt(i + 1);
                if (needsConcat(c, n)) {
                    out.append('·'); // operador de concatenación explícita
                }
            }
        }
        return out.toString();
    }

    /**
     * Convierte de infijo a postfijo. Primero inserta '·' y luego aplica
     * Shunting–Yard.
     *
     * @param infixRegex expresión regular en infijo
     * @return expresión regular en notación postfija
     */
    public static String toPostfix(String infixRegex) {
        if (infixRegex == null || infixRegex.isEmpty()) {
            return "";
        }

        // 1) Preprocesar: insertar concatenación explícita con '·'
        String s = insertConcatenationOperator(infixRegex);

        // 2) Shunting–Yard con una pila simple (sin imports)
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
            
                while (top >= 0 && stack[top] != '(') {
                    output.append(stack[top--]);
                }
                if (top < 0) {
                    throw new IllegalArgumentException("Unbalanced parentheses: missing '('");
                }
                top--; // descarta '('
            } else { // operador
                int pCh = precedence(ch);
                if (pCh < 0) {
                    throw new IllegalArgumentException("Unknown operator: " + ch);
                }

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

    /* ===================== Helpers ===================== */
    // ¿Debe insertarse concatenación entre a y b?
    private static boolean needsConcat(char a, char b) {
        boolean left = isOperand(a) || a == ')' || a == '*' || a == '+' || a == '?';
        boolean right = isOperand(b) || b == '(';
        return left && right;
    }

    // ¿Es un operador?
    private static boolean isOperator(char c) {
        return c == '|' || c == '·' || c == '*' || c == '+' || c == '?';
    }

   
    private static boolean isOperand(char c) {
        if (c == ' ') {
            return true;                 

                }return !isOperator(c) && c != '(' && c != ')';
    }

    // Precedencias: mayor valor = mayor precedencia
    private static int precedence(char op) {
        switch (op) {
            case '*':
            case '+':
            case '?':
                return 3;           // unarios (postfijo)
            case '·':
                return 2;           // concatenación explícita
            case '|':
                return 1;           // unión
            default:
                return -1;          // no es operador
        }
    }

    // Asociatividad de operadores binarios
    private static boolean isLeftAssociative(char op) {
        // '·' y '|' suelen ser izquierdos; los unarios postfijo no usan este chequeo
        return op == '·' || op == '|';
    }
}
