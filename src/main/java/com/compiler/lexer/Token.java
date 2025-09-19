package com.compiler.lexer;

public class Token {
    public final String type;
    public final String lexeme;
    public final int startIndex; // inclusive
    public final int endIndex;   // exclusive

    public Token(String type, String lexeme, int startIndex, int endIndex) {
        this.type = type;
        this.lexeme = lexeme;
        this.startIndex = startIndex;
        this.endIndex = endIndex;
    }

    @Override
    public String toString() {
        return "(" + type + ", \"" + lexeme + "\", " + startIndex + "," + endIndex + ")";
    }
}
