package com.compiler;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;

/**
 * Test por reflexión para desacoplarse de los cambios de firma en
 * Tokenizer, TokenRuler y Token.
 *
 * Reglas usadas:
 *  - IF  : "if" (palabra reservada, prioridad alta)
 *  - ID  : "a+" (identificador simplificado)
 *  - WS  : " +" (espacios, ignorada)
 *
 * Cadena de prueba: "if aaaa"
 * Esperado: tokens [IF, ID("aaaa")] sin WS.
 */
public class TokenizerReflectionTest {

    private static final String CLS_TOKENIZER  = "com.compiler.lexer.Tokenizer";
    private static final String CLS_TOKENRULER = "com.compiler.lexer.TokenRuler";
    private static final String CLS_TOKEN      = "com.compiler.lexer.Token";

    // ---------- Utilidades de reflexión robustas ----------

    /** Crea una instancia de TokenRuler probando varios constructores posibles. */
    private Object makeRule(String type, String regex, int priority, boolean isKeyword, boolean ignore) throws Exception {
        Class<?> ruleK = Class.forName(CLS_TOKENRULER);

        // Probar constructores comunes (5, 4, 3, 2 parámetros)
        for (Constructor<?> c : ruleK.getConstructors()) {
            Class<?>[] p = c.getParameterTypes();
            try {
                if (p.length == 5 &&
                    p[0] == String.class && p[1] == String.class && p[2] == int.class &&
                    p[3] == boolean.class && p[4] == boolean.class) {
                    return c.newInstance(type, regex, priority, isKeyword, ignore);
                }
                if (p.length == 4 &&
                    p[0] == String.class && p[1] == String.class && p[2] == int.class &&
                    p[3] == boolean.class) {
                    // Asumimos último = ignore (isKeyword implícito o no usado)
                    return c.newInstance(type, regex, priority, ignore);
                }
                if (p.length == 3 &&
                    p[0] == String.class && p[1] == String.class && p[2] == int.class) {
                    Object r = c.newInstance(type, regex, priority);
                    // Intentar setear flags por setters si existen
                    tryCallSetter(r, "setKeyword", boolean.class, isKeyword);
                    tryCallSetter(r, "setIgnore",  boolean.class, ignore);
                    return r;
                }
                if (p.length == 2 &&
                    p[0] == String.class && p[1] == String.class) {
                    Object r = c.newInstance(type, regex);
                    tryCallSetter(r, "setPriority", int.class, priority);
                    tryCallSetter(r, "setKeyword", boolean.class, isKeyword);
                    tryCallSetter(r, "setIgnore",  boolean.class, ignore);
                    return r;
                }
            } catch (Throwable ignored2) {
                // Intentar siguiente firma
            }
        }
        throw new IllegalStateException("No encontré un constructor compatible para TokenRuler.");
    }

    /** Intenta invocar un setter opcional sin fallar si no existe. */
    private void tryCallSetter(Object target, String name, Class<?> argT, Object argV) {
        try {
            Method m = target.getClass().getMethod(name, argT);
            m.invoke(target, argV);
        } catch (Throwable ignored) { }
    }

    /** Instancia el Tokenizer probando varias rutas posibles. */
    private Object makeTokenizer(List<?> rules) throws Exception {
        Class<?> tzK = Class.forName(CLS_TOKENIZER);

        // 1) Constructor (List)
        try {
            Constructor<?> c = tzK.getConstructor(List.class);
            return c.newInstance(rules);
        } catch (Throwable ignored) { }

        // 2) Ctor vacío + build(List)
        try {
            Object tz = tzK.getConstructor().newInstance();
            for (String mname : Arrays.asList("build", "init", "compile", "prepare")) {
                try {
                    Method m = tzK.getMethod(mname, List.class);
                    m.invoke(tz, rules);
                    return tz;
                } catch (Throwable ignored2) { }
            }
            // 3) setRules(List) + build()
            try {
                Method setRules = tzK.getMethod("setRules", List.class);
                setRules.invoke(tz, rules);
                for (String mname : Arrays.asList("build", "init", "compile", "prepare")) {
                    try {
                        Method m = tzK.getMethod(mname);
                        m.invoke(tz);
                        return tz;
                    } catch (Throwable ignored3) { }
                }
                return tz; // algunas implementaciones tokenizan "on the fly"
            } catch (Throwable ignored3) { }
            return tz;
        } catch (Throwable ignored) { }

        throw new IllegalStateException("No pude crear Tokenizer (prueba constructor(List) o build(List)).");
    }

    /** Ejecuta tokenize/scan sobre el tokenizer. */
    @SuppressWarnings("unchecked")
    private List<Object> runTokenize(Object tokenizer, String input) throws Exception {
        Class<?> tzK = tokenizer.getClass();
        for (String mname : Arrays.asList("tokenize", "scan", "lex")) {
            for (Method m : tzK.getMethods()) {
                if (m.getName().equals(mname) &&
                    m.getParameterCount() == 1 &&
                    m.getParameterTypes()[0] == String.class) {
                    Object ret = m.invoke(tokenizer, input);
                    if (ret instanceof List<?>) return (List<Object>) ret;
                }
            }
        }
        throw new IllegalStateException("No encontré tokenize(String) / scan(String) en Tokenizer.");
    }

    /** Obtiene (type, lexeme) de un Token por getters o campos comunes. */
    private String[] tokenPair(Object tok) throws Exception {
        String type = tryGet(tok, "getType");
        if (type == null) type = tryGetField(tok, "type");
        String lex  = tryGet(tok, "getLexeme");
        if (lex == null) lex = tryGetField(tok, "lexeme");
        if (lex == null) lex = tryGet(tok, "getText"); // por si usas otro nombre
        return new String[] { type, lex };
    }

    private String tryGet(Object o, String getter) {
        try {
            Method m = o.getClass().getMethod(getter);
            Object v = m.invoke(o);
            return v == null ? null : v.toString();
        } catch (Throwable ignored) { return null; }
    }

    private String tryGetField(Object o, String fname) {
        try {
            Field f = o.getClass().getField(fname);
            Object v = f.get(o);
            return v == null ? null : v.toString();
        } catch (Throwable ignored) { return null; }
    }

    // ---------- TESTS ----------

    @Test
    public void respetaMaxMunchYPrioridad_Y_ignoraWS() throws Exception {
        // Reglas mínimas para la prueba
        List<Object> rules = new ArrayList<>();
        // IF con prioridad alta (p.ej. 100), palabra reservada
        rules.add(makeRule("IF", "if", 100, true, false));
        // ID muy simple: a+
        rules.add(makeRule("ID", "a+", 10, false, false));
        // WS ignorado: uno o más espacios
        rules.add(makeRule("WS", " +", 1, false, true));

        Object tokenizer = makeTokenizer(rules);
        List<Object> toks = runTokenize(tokenizer, "if aaaa");

        // Prints para depurar
        System.out.println("=== TOKENS OBTENIDOS ===");
        for (Object t : toks) {
            String[] p = tokenPair(t);
            System.out.println("  [" + p[0] + "] '" + p[1] + "'");
        }

        // Verificaciones: sin WS, keyword gana a ID
        assertTrue(toks.size() >= 2, "Se esperaban al menos 2 tokens (IF e ID).");

        String[] t0 = tokenPair(toks.get(0));
        assertEquals("IF", t0[0], "La palabra reservada IF debe tener prioridad sobre ID.");

        String[] t1 = tokenPair(toks.get(1));
        // ID o IDENT/IDENTIFIER (por si tu implementación usa otro nombre):
        assertTrue(
            "ID".equals(t1[0]) || "IDENT".equalsIgnoreCase(t1[0]) || "IDENTIFIER".equalsIgnoreCase(t1[0]),
            "El segundo token debe ser un identificador (ID). Obtuve: " + t1[0]
        );
        assertEquals("aaaa", t1[1], "El lexema del ID debe ser 'aaaa'.");

        // Asegurar que NO aparezca WS ignorado:
        for (Object tk : toks) {
            String ty = tokenPair(tk)[0];
            assertFalse("WS".equals(ty), "Los espacios (WS) deben ser ignorados y no emitidos.");
        }
    }
}
