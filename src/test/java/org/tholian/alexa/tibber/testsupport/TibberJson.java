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

    /** KI-generiert: Antwort der Preisabfrage (nur priceInfo). Alle Parameter sind JSON-Fragmente. */
    public static String priceInfoResponse(String current, String today, String tomorrow) {
        return """
                {"data":{"viewer":{"homes":[{"currentSubscription":{"priceInfo":{"current":%s,"today":%s,"tomorrow":%s}}}]}}}
                """.formatted(current, today, tomorrow);
    }

    /** KI-generiert: Preis mit Aufschlüsselung in Energiepreis und Steuern/Abgaben. */
    public static String price(double total, double energy, double tax, String level, String startsAt) {
        return """
                {"total":%s,"energy":%s,"tax":%s,"currency":"EUR","level":"%s","startsAt":"%s"}
                """.formatted(Double.toString(total), Double.toString(energy), Double.toString(tax), level, startsAt).strip();
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

    /** KI-generiert: GraphQL-Antwort, wie Tibber sie bei ungültigem oder widerrufenem Token liefert. */
    public static String unauthenticatedResponse() {
        return """
                {"errors":[{"message":"Context creation failed: invalid token","extensions":{"code":"UNAUTHENTICATED"}}]}
                """;
    }

    /** KI-generiert: Minimalantwort der Token-Prüfung (nur Home-IDs). */
    public static String homesResponse(String... homeIds) {
        StringBuilder homes = new StringBuilder();
        for (int i = 0; i < homeIds.length; i++) {
            if (i > 0) {
                homes.append(',');
            }
            homes.append("{\"id\":\"").append(homeIds[i]).append("\"}");
        }
        return "{\"data\":{\"viewer\":{\"homes\":[" + homes + "]}}}";
    }

    /** KI-generiert: Antwort der Verbrauchsabfrage; {@code nodes} ist ein JSON-Array. */
    public static String consumptionResponse(String nodes) {
        return """
                {"data":{"viewer":{"homes":[{"consumption":{"nodes":%s}}]}}}
                """.formatted(nodes);
    }

    /** KI-generiert: Verbrauchswert; {@code null} = noch nicht abgerechnet. */
    public static String consumptionNode(String from, Double consumption, Double cost) {
        return """
                {"from":"%s","to":"%s","consumption":%s,"cost":%s,"currency":"EUR"}
                """.formatted(from, from, consumption, cost).strip();
    }

    /** KI-generiert: Antwort der Einspeiseabfrage; {@code nodes} ist ein JSON-Array. */
    public static String productionResponse(String nodes) {
        return """
                {"data":{"viewer":{"homes":[{"production":{"nodes":%s}}]}}}
                """.formatted(nodes);
    }

    /** KI-generiert: Einspeisewert; {@code null} = noch nicht abgerechnet. */
    public static String productionNode(String from, Double production, Double profit) {
        return """
                {"from":"%s","to":"%s","production":%s,"profit":%s,"currency":"EUR"}
                """.formatted(from, from, production, profit).strip();
    }

    /** KI-generiert: Antwort der Live-Vorabfrage (WebSocket-URL, Home-ID, Pulse-Flag, letzte Stunde). */
    public static String livePrerequisiteResponse(String websocketUrl, boolean realTimeConsumptionEnabled, String nodes) {
        return """
                {"data":{"viewer":{"websocketSubscriptionUrl":"%s","homes":[{
                  "id":"home-1","features":{"realTimeConsumptionEnabled":%s},
                  "consumption":{"nodes":%s}
                }]}}}
                """.formatted(websocketUrl, realTimeConsumptionEnabled, nodes);
    }
}
