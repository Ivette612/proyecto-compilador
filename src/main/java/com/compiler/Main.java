package com.compiler;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.compiler.lexer.DfaMinimizer;
import com.compiler.lexer.NfaToDfaConverter;
import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.dfa.DfaState;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.regex.RegexParser;

public class Main {

    public static void main(String[] args) {
        // Regex de ejemplo o desde args
        String regex = (args != null && args.length > 0) ? args[0] : "a(b|c)*";

        // 1) Parseo a NFA
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);

        // 2) Alfabeto para la construcción NFA->DFA (extraído de la regex)
        Set<Character> alphabetForNfaToDfa = extractAlphabetFromRegex(regex);

        // 3) NFA -> DFA
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabetForNfaToDfa);

        // 4) Alfabeto real del DFA (desde sus transiciones)
        Set<Character> dfaAlphabet = alphabetFromDfa(dfa);

        // 5) Minimización (usando la firma existente)
        DFA minimized = DfaMinimizer.minimizeDfa(dfa, dfaAlphabet);

        // 6) Salida simple
        System.out.println("DFA states: " + dfa.getStates().size());
        System.out.println("Min DFA states: " + minimized.getStates().size());
    }

    /** Extrae el alfabeto “visible” de la regex (ignora operadores y espacios). */
    private static Set<Character> extractAlphabetFromRegex(String regex) {
        Set<Character> a = new HashSet<>();
        if (regex == null) return a;
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (isOperand(c)) a.add(c);
        }
        return a;
    }

    private static boolean isOperand(char c) {
        switch (c) {
            case '|': case '*': case '+': case '?': case '(': case ')': case '.':
                return false;
            default:
                return !Character.isWhitespace(c);
        }
    }

    /** Deriva el alfabeto real del DFA a partir de sus transiciones. */
    private static Set<Character> alphabetFromDfa(DFA dfa) {
        Set<Character> a = new HashSet<>();
        for (DfaState s : dfa.getStates()) {
            Map<Character, DfaState> tr = s.getTransitions();
            if (tr != null) a.addAll(tr.keySet());
        }
        return a;
    }
}
