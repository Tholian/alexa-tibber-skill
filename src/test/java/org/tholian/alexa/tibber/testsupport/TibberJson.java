package org.tholian.alexa.tibber.testsupport;

/**
 * KI-generiert: JSON-Fixtures im Format der Tibber GraphQL API.
 * Werte werden als Strings eingesetzt, damit auch {@code null} und leere Arrays abbildbar sind.
 */
public final class TibberJson {

    private TibberJson() {
    }

    /** KI-generiert: Vollständige Antwort mit genau einem Zuhause. Alle Parameter sind JSON-Fragmente. */
    public static String homeResponse(String current, String today, String tomorrow, String nodes) {
        return """
                {"data":{"viewer":{"homes":[{
                  "currentSubscription":{"priceInfo":{"current":%s,"today":%s,"tomorrow":%s}},
                  "consumption":{"nodes":%s}
                }]}}}
                """.formatted(current, today, tomorrow, nodes);
    }

    /** KI-generiert: Ein Preis-Objekt; Double.toString ist locale-unabhängig (Punkt als Dezimaltrenner). */
    public static String price(double total, String level, String startsAt) {
        return """
                {"total":%s,"currency":"EUR","level":"%s","startsAt":"%s"}
                """.formatted(Double.toString(total), level, startsAt).strip();
    }

    /** KI-generiert: JSON-Array aus bereits serialisierten Elementen. */
    public static String array(String... elements) {
        return "[" + String.join(",", elements) + "]";
    }

    /** KI-generiert: GraphQL-Fehlerantwort, wie Tibber sie z. B. bei ungültigem Token liefert. */
    public static String errorResponse(String message) {
        return """
                {"data":null,"errors":[{"message":"%s"}]}
                """.formatted(message);
    }
}
