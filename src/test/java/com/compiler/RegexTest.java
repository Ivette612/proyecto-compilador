package com.compiler;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import com.compiler.lexer.DfaSimulator;
import com.compiler.lexer.NfaSimulator;
import com.compiler.lexer.NfaToDfaConverter;
import com.compiler.lexer.dfa.DFA;
import com.compiler.lexer.nfa.NFA;
import com.compiler.lexer.regex.RegexParser;

public class RegexTest {

    private static Set<Character> alphabetFromRegex(String regex) {
        Set<Character> alpha = new HashSet<>();
        for (int i = 0; i < regex.length(); i++) {
            char c = regex.charAt(i);
            if (c == '|' || c == '*' || c == '?' || c == '+' || c == '(' || c == ')' || c == '·' || Character.isWhitespace(c)) {
                continue;
            }
            alpha.add(c);
        }
        return alpha;
    }

    @ParameterizedTest
    @CsvSource({
        "a,      true",
        "aa,     true",
        "aaaa,   true",
        "'',     false",
        "b,      false"
    })
    void testPlusOperator(String input, boolean expected) {
        String regex = "a+";
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);
        nfa.endState.isFinal = true;
        NfaSimulator nfaSimulator = new NfaSimulator();
        boolean actualNfa = nfaSimulator.simulate(nfa, input);
        Set<Character> alphabet = alphabetFromRegex(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        DfaSimulator dfaSimulator = new DfaSimulator();
        boolean actualDfa = dfaSimulator.simulate(dfa, input);
        assertEquals(expected, actualNfa);
        assertEquals(expected, actualDfa);
    }

    @ParameterizedTest
    @CsvSource({
        "'',     true",
        "a,      true",
        "aa,     false",
        "b,      false"
    })
    void testOptionalOperator(String input, boolean expected) {
        String regex = "a?";
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);
        nfa.endState.isFinal = true;
        NfaSimulator nfaSimulator = new NfaSimulator();
        boolean actualNfa = nfaSimulator.simulate(nfa, input);
        Set<Character> alphabet = alphabetFromRegex(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        DfaSimulator dfaSimulator = new DfaSimulator();
        boolean actualDfa = dfaSimulator.simulate(dfa, input);
        assertEquals(expected, actualNfa);
        assertEquals(expected, actualDfa);
    }

    @ParameterizedTest
    @CsvSource({
        "a,      true",
        "b,      true",
        "'',     false",
        "c,      false",
        "ab,     false"
    })
    void testUnionOperator(String input, boolean expected) {
        String regex = "a|b";
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);
        nfa.endState.isFinal = true;
        NfaSimulator nfaSimulator = new NfaSimulator();
        boolean actualNfa = nfaSimulator.simulate(nfa, input);
        Set<Character> alphabet = alphabetFromRegex(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        DfaSimulator dfaSimulator = new DfaSimulator();
        boolean actualDfa = dfaSimulator.simulate(dfa, input);
        assertEquals(expected, actualNfa);
        assertEquals(expected, actualDfa);
    }

    @ParameterizedTest
    @CsvSource({
        "ab,     true",
        "c,      true",
        "a,      false",
        "b,      false",
        "ac,     false"
    })
    void testPrecedenceConcatOverUnion(String input, boolean expected) {
        String regex = "ab|c";
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);
        nfa.endState.isFinal = true;
        NfaSimulator nfaSimulator = new NfaSimulator();
        boolean actualNfa = nfaSimulator.simulate(nfa, input);
        Set<Character> alphabet = alphabetFromRegex(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        DfaSimulator dfaSimulator = new DfaSimulator();
        boolean actualDfa = dfaSimulator.simulate(dfa, input);
        assertEquals(expected, actualNfa);
        assertEquals(expected, actualDfa);
    }

    @ParameterizedTest
    @CsvSource({
        "ac,     true",
        "abc,    true",
        "abbbc,  true",
        "a,      false",
        "c,      false",
        "ab,     false",
        "bc,     false"
    })
    void testPrecedenceKleeneOverConcat(String input, boolean expected) {
        String regex = "ab*c";
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);
        nfa.endState.isFinal = true;
        NfaSimulator nfaSimulator = new NfaSimulator();
        boolean actualNfa = nfaSimulator.simulate(nfa, input);
        Set<Character> alphabet = alphabetFromRegex(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        DfaSimulator dfaSimulator = new DfaSimulator();
        boolean actualDfa = dfaSimulator.simulate(dfa, input);
        assertEquals(expected, actualNfa);
        assertEquals(expected, actualDfa);
    }

    @ParameterizedTest
    @CsvSource({
        "'',     true",
        "a,      true",
        "b,      true",
        "aa,     true",
        "bb,     true",
        "ab,     true",
        "ba,     true",
        "bababa, true",
        "c,      false",
        "ac,     false",
        "bc,     false"
    })
    void testGroupingKleene(String input, boolean expected) {
        String regex = "(a|b)*";
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);
        nfa.endState.isFinal = true;
        NfaSimulator nfaSimulator = new NfaSimulator();
        boolean actualNfa = nfaSimulator.simulate(nfa, input);
        Set<Character> alphabet = alphabetFromRegex(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        DfaSimulator dfaSimulator = new DfaSimulator();
        boolean actualDfa = dfaSimulator.simulate(dfa, input);
        assertEquals(expected, actualNfa);
        assertEquals(expected, actualDfa);
    }

    @ParameterizedTest
    @CsvSource({
        "abd,    true",
        "acd,    true",
        "ad,     false",
        "abcd,   false",
        "'abd d',false"
    })
    void testConcatWithGroupUnion(String input, boolean expected) {
        String regex = "a(b|c)d";
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);
        nfa.endState.isFinal = true;
        NfaSimulator nfaSimulator = new NfaSimulator();
        boolean actualNfa = nfaSimulator.simulate(nfa, input);
        Set<Character> alphabet = alphabetFromRegex(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        DfaSimulator dfaSimulator = new DfaSimulator();
        boolean actualDfa = dfaSimulator.simulate(dfa, input);
        assertEquals(expected, actualNfa);
        assertEquals(expected, actualDfa);
    }

    @ParameterizedTest
    @CsvSource({
        "ad,     true",
        "abd,    true",
        "abbbd,  true",
        "acd,    true",
        "acccd,  true",
        "'a c d',false",
        "abcd,   false",
        "abbc,   false"
    })
    void testComplexNesting(String input, boolean expected) {
        String regex = "a(b*|c+)?d";
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);
        nfa.endState.isFinal = true;
        NfaSimulator nfaSimulator = new NfaSimulator();
        boolean actualNfa = nfaSimulator.simulate(nfa, input);
        Set<Character> alphabet = alphabetFromRegex(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        DfaSimulator dfaSimulator = new DfaSimulator();
        boolean actualDfa = dfaSimulator.simulate(dfa, input);
        assertEquals(expected, actualNfa);
        assertEquals(expected, actualDfa);
    }

    @ParameterizedTest
    @CsvSource({
        "'',     true",
        "a,      true",
        "aa,     true",
        "b,      false"
    })
    void testNestedKleene(String input, boolean expected) {
        String regex = "(a*)*";
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);
        nfa.endState.isFinal = true;
        NfaSimulator nfaSimulator = new NfaSimulator();
        boolean actualNfa = nfaSimulator.simulate(nfa, input);
        Set<Character> alphabet = alphabetFromRegex(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        DfaSimulator dfaSimulator = new DfaSimulator();
        boolean actualDfa = dfaSimulator.simulate(dfa, input);
        assertEquals(expected, actualNfa);
        assertEquals(expected, actualDfa);
    }

    @ParameterizedTest
    @CsvSource({
        "ac,     true",
        "abc,    true",
        "a,      false",
        "c,      false",
        "ab,     false",
        "bc,     false"
    })
    void testOptionalInsideConcat(String input, boolean expected) {
        String regex = "a(b?)c";
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);
        nfa.endState.isFinal = true;
        NfaSimulator nfaSimulator = new NfaSimulator();
        boolean actualNfa = nfaSimulator.simulate(nfa, input);
        Set<Character> alphabet = alphabetFromRegex(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        DfaSimulator dfaSimulator = new DfaSimulator();
        boolean actualDfa = dfaSimulator.simulate(dfa, input);
        assertEquals(expected, actualNfa);
        assertEquals(expected, actualDfa);
    }

    @ParameterizedTest
    @CsvSource({
        "a,      true",
        "aa,     true",
        "ab,     true",
        "ba,     true",
        "bab,    true",
        "bbaabb, true",
        "'',     false",
        "b,      false",
        "bb,     false",
        "bbbb,   false"
    })
    void testContainsAtLeastOneA(String input, boolean expected) {
        String regex = "(a|b)*a(a|b)*";
        RegexParser parser = new RegexParser();
        NFA nfa = parser.parse(regex);
        nfa.endState.isFinal = true;
        NfaSimulator nfaSimulator = new NfaSimulator();
        boolean actualNfa = nfaSimulator.simulate(nfa, input);
        Set<Character> alphabet = alphabetFromRegex(regex);
        DFA dfa = NfaToDfaConverter.convertNfaToDfa(nfa, alphabet);
        DfaSimulator dfaSimulator = new DfaSimulator();
        boolean actualDfa = dfaSimulator.simulate(dfa, input);
        assertEquals(expected, actualNfa);
        assertEquals(expected, actualDfa);
    }
}
