
package org.tholian.alexa.tibber.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.interceptor.RequestInterceptor;
import com.amazon.ask.model.IntentRequest;
import com.amazon.ask.model.User;
import org.slf4j.MDC;

/**
 * KI-generiert: Schreibt Alexa-Request-Kontext in den MDC vor jeder Handler-Ausführung.
 * Die MDC-Felder (intentName, requestId, locale, userId) erscheinen automatisch
 * in jedem Log-Eintrag, der während der Request-Verarbeitung ausgelöst wird.
 *
 * SICHERHEIT: accessToken wird bewusst NICHT geloggt, da er sensible Zugangsdaten enthält.
 * SICHERHEIT: userId dient nur dem Tracing; keine personenbezogenen Inhalte werden geloggt.
 */
public class LoggingInterceptor implements RequestInterceptor {

    @Override
    public void process(HandlerInput input) {
        // KI-generiert: Reste eines vorherigen Aufrufs entfernen. In einer warmen Lambda-Instanz läuft der
        // nächste Request auf demselben Thread; schlägt ein Handler fehl, wird der MDCCleanupInterceptor
        // (ResponseInterceptor) nicht ausgeführt.
        MDC.clear();

        var envelope = input.getRequestEnvelope();
        var request = envelope.getRequest();

        // KI-generiert: Request-ID und Locale direkt aus dem Basis-Request-Objekt lesen
        // KI-generiert: null-safe MDC.put — SLF4J wirft NPE bei null-Wert
        if (request.getRequestId() != null) MDC.put("requestId", request.getRequestId());
        if (request.getLocale() != null)    MDC.put("locale", request.getLocale());

        // KI-generiert: Intent-Name bei IntentRequest, andernfalls einfacher Klassenname des Request-Typs
        String intentName = (request instanceof IntentRequest ir)
                ? ir.getIntent().getName()
                : request.getClass().getSimpleName();
        MDC.put("intentName", intentName);

        // KI-generiert: userId kann null sein (z.B. bei LaunchRequest ohne Account Linking)
        var system = envelope.getContext() != null ? envelope.getContext().getSystem() : null;
        User user = (system != null) ? system.getUser() : null;
        if (user != null && user.getUserId() != null) {
            MDC.put("userId", user.getUserId());
        }
    }
}
