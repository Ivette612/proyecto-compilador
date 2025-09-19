package com.compiler.lexer;

public class TokenRuler {

    public final String type;
    public final String regex;
    public final int priority;
    public final boolean keyword;
    public final boolean ignore;

    public TokenRuler(String type, String regex, int priority, boolean keyword, boolean ignore) {
        this.type = type;
        this.regex = regex;
        this.priority = priority;
        this.keyword = keyword;
        this.ignore = ignore;
    }


    public String getRegex() {
        return this.regex;
    }

    public String getType() {
        return this.type;
    }

    public int getPriority() {
        return this.priority;
    }

    public boolean isKeyword() {
        return keyword;
    }

    public boolean isIgnore() {
        return this.ignore;
    }

}
