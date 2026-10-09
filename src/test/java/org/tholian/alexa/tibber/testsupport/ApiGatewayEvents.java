package org.tholian.alexa.tibber.testsupport;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * KI-generiert: Events im Format von API Gateway HTTP API (Payload-Version 2.0), wie sie die
 * Account-Linking-Lambda erhält. Header-Namen sind – wie bei API Gateway – kleingeschrieben.
 */
public final class ApiGatewayEvents {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    private ApiGatewayEvents() {
    }

    public static String get(String path, Map<String, String> query, String host) {
        ObjectNode event = base("GET", path, host);
        ObjectNode params = event.putObject("queryStringParameters");
        query.forEach(params::put);
        // KI-generiert: API Gateway liefert zusätzlich den unveränderten Query-String; die Lambda wertet nur diesen aus
        event.put("rawQueryString", form(query));
        return event.toString();
    }

    /** KI-generiert: origin darf null sein (Header fehlt dann). */
    public static String post(String path, String formBody, String host, String origin) {
        ObjectNode event = base("POST", path, host);
        if (origin != null) {
            ((ObjectNode) event.get("headers")).put("origin", origin);
        }
        event.put("body", formBody);
        event.put("isBase64Encoded", false);
        return event.toString();
    }

    public static String postBase64(String path, String formBody, String host, String origin) {
        ObjectNode event = (ObjectNode) readTree(post(path, formBody, host, origin));
        event.put("body", Base64.getEncoder().encodeToString(formBody.getBytes(StandardCharsets.UTF_8)));
        event.put("isBase64Encoded", true);
        return event.toString();
    }

    /** KI-generiert: GET mit explizitem rohem Query-String (z. B. doppelte Parameter oder Prozent-Kodierung). */
    public static String getRaw(String path, String rawQueryString, String host) {
        ObjectNode event = base("GET", path, host);
        event.put("rawQueryString", rawQueryString);
        return event.toString();
    }

    public static String request(String method, String path, String host) {
        return base(method, path, host).toString();
    }

    /** KI-generiert: Baut einen x-www-form-urlencoded-Body in der Reihenfolge der Map. */
    public static String form(Map<String, String> fields) {
        return fields.entrySet().stream()
                .map(e -> URLEncoder.encode(e.getKey(), StandardCharsets.UTF_8) + "="
                          + URLEncoder.encode(e.getValue(), StandardCharsets.UTF_8))
                .collect(Collectors.joining("&"));
    }

    private static ObjectNode base(String method, String path, String host) {
        ObjectNode event = MAPPER.createObjectNode();
        event.put("version", "2.0");
        event.put("rawPath", path);
        event.putObject("headers").put("host", host).put("content-type", "application/x-www-form-urlencoded");
        event.putObject("requestContext").putObject("http").put("method", method).put("path", path);
        return event;
    }

    private static com.fasterxml.jackson.databind.JsonNode readTree(String json) {
        try {
            return MAPPER.readTree(json);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }
}
