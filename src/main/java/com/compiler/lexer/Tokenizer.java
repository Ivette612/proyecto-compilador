package com.compiler.lexer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.TokenRuler;
import com.compiler.lexer.regex.RegexParser;

/**
 * Tokenizer
 * ---------
 * Construye un DFA por cada regla léxica y tokeniza una entrada aplicando:
 *  - Máxima coincidencia (max-munch)
 *  - Desempate por prioridad de la regla
 *  - Reglas "ignoradas" (p. ej. espacios) que no se devuelven como tokens
 *
 * Esta clase incluye los puntos de entrada que exige el test de reflexión:
 *  - Constructor público Tokenizer(List<TokenRuler>)
 *  - Método de fábrica público y estático build(List<TokenRuler>)
 */
public class Tokenizer {

    private final List<TokenRuler> rules;

    // DFA por regla
    private final Map<TokenRuler, DFA> dfaByRule = new HashMap<>();

    // Parser de regex -> NFA
    private final RegexParser parser = new RegexParser();

    // Bandera para evitar tokenizar sin haber construido antes
    private boolean built = false;

    public Tokenizer(List<TokenRuler> rules) {
        this.rules = (rules == null) ? new ArrayList<>() : new ArrayList<>(rules);
    }


    public static Tokenizer build(List<TokenRuler> rules) {
        Tokenizer tz = new Tokenizer(rules);
        tz.build();
        return tz;
    }

    // ---------------------------------------------------------------------
    //  Construcción de autómatas por regla
    // ---------------------------------------------------------------------

    /** Construye un DFA por cada regla.  */
    public void build() {
        dfaByRule.clear();

        if (rules == null || rules.isEmpty()) {
            throw new IllegalStateException("No hay reglas léxicas para construir el tokenizer.");
        }

        for (TokenRuler r : rules) {
            // 1) regex (infija) -> NFA
            NFA nfa = parser.parse(r.getRegex());

            // 2) Alfabeto: derivado heurísticamente de la propia regex
            Set<Character> alphabet = deriveAlphabetFromRegex(r.getRegex());

            // 3) NFA -> DFA
            DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);

            // 4) Minimizador opcional (si lo usas en tus pruebas)
         
            dfa = DfaMinimizer.minimizeDfa(dfa, alphabet);

            dfaByRule.put(r, dfa);
        }
        built = true;
    }

    // Deriva un alfabeto razonable a partir de una regex simple (excluye metacaracteres)
    private Set<Character> deriveAlphabetFromRegex(String regex) {
        Set<Character> sigma = new HashSet<>();
        if (regex == null) return sigma;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
         
            if (c == '(' || c == ')' || c == '|' || c == '*' || c == '+' || c == '?' || c == '·') continue;
            sigma.add(c);
        }
        return sigma;
    }

    // ---------------------------------------------------------------------
    //  Tokenización: max-munch + prioridad + ignorados
    // ---------------------------------------------------------------------

    /**
     * Tokeniza la cadena de entrada aplicando máxima coincidencia.
     * En empates por longitud, gana la regla con mayor prioridad (valor numérico mayor).
     * Las reglas marcadas como ignoradas consumen entrada pero no devuelven token.
     */
    public List<Token> tokenize(String input) {
        if (!built) {
            // Permite usar el constructor(List) y construir bajo demanda
            build();
        }
        if (input == null) input = "";

        List<Token> out = new ArrayList<>();
        int i = 0;
        while (i < input.length()) {
            Match best = null;

            // Probar TODAS las reglas desde la posición i
            for (int idx = 0; idx < rules.size(); idx++) {
                TokenRuler rule = rules.get(idx);
                DFA dfa = dfaByRule.get(rule);
                if (dfa == null) continue;

                int len = longestMatch(dfa, input, i);
                if (len <= 0) continue;

                // Empaquetar candidato
                Match cand = new Match(rule, len, idx);

                // Selección: mayor longitud; empate -> mayor prioridad; luego orden de regla
                if (best == null || cand.betterThan(best)) {
                    best = cand;
                }
            }

            if (best == null) {
                char bad = input.charAt(i);
                throw new RuntimeException("Error léxico cerca de índice " + i + ": '" + bad + "'");
            }

            // Consumir
            int end = i + best.length;
            String lexeme = input.substring(i, end);

            // Si la regla es "ignorada", no generamos token, solo avanzamos
            if (!best.rule.isIgnore()) {
                // Constructor de Token exigido por tus tests: (type, lexeme, start, end)
                out.add(new Token(best.rule.getType(), lexeme, i, end));
            }

            i = end;
        }

        return out;
    }

    // Simulación sencilla de DFA para obtener la longitud del mayor prefijo aceptado
    private int longestMatch(DFA dfa, String s, int start) {
        DfaState cur = dfa.getStartState();
        int lastAccept = -1;
        int i = start;

        while (i < s.length()) {
            char c = s.charAt(i);
            DfaState next = cur.getTransition(c);
            if (next == null) break;
            cur = next;
            i++;
            if (cur.isFinal()) lastAccept = i;
        }

        return (lastAccept == -1) ? 0 : (lastAccept - start);
    }

    // ---------------------------------------------------------------------
    //  Estructura auxiliar para comparar candidatos
    // ---------------------------------------------------------------------

    private static final class Match {
        final TokenRuler rule;
        final int length;
        final int ruleOrder; // posición en la lista original (para desempate final)

        Match(TokenRuler rule, int length, int ruleOrder) {
            this.rule = rule;
            this.length = length;
            this.ruleOrder = ruleOrder;
        }

        boolean betterThan(Match other) {
            if (this.length != other.length) {
                return this.length > other.length;
            }
            // prioridad numérica mayor gana (ajústalo si en tu proyecto es al revés)
            int p1 = this.rule.getPriority();
            int p2 = other.rule.getPriority();
            if (p1 != p2) return p1 > p2;

            // Último desempate: regla que apareció antes en la lista
            return this.ruleOrder < other.ruleOrder;
        }
    }
}
