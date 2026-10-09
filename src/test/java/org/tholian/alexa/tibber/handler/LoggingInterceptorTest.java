
package org.tholian.alexa.tibber.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.*;
import com.amazon.ask.model.interfaces.system.SystemState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.assertj.core.api.Assertions.assertThat;

class LoggingInterceptorTest {

    private final LoggingInterceptor interceptor = new LoggingInterceptor();

    @AfterEach
    void cleanUp() {
        MDC.clear();
    }

    @Test
    void setsIntentNameInMDC() {
        var input = intentInput("GetCurrentPriceIntent", "de-DE", "test-request-001", "amzn1.ask.account.abc");
        interceptor.process(input);
        assertThat(MDC.get("intentName")).isEqualTo("GetCurrentPriceIntent");
    }

    @Test
    void setsRequestIdInMDC() {
        var input = intentInput("GetCurrentPriceIntent", "de-DE", "test-request-001", "amzn1.ask.account.abc");
        interceptor.process(input);
        assertThat(MDC.get("requestId")).isEqualTo("test-request-001");
    }

    @Test
    void setsLocaleInMDC() {
        var input = intentInput("GetCurrentPriceIntent", "de-DE", "test-request-001", "amzn1.ask.account.abc");
        interceptor.process(input);
        assertThat(MDC.get("locale")).isEqualTo("de-DE");
    }

    @Test
    void setsUserIdInMDC() {
        var input = intentInput("GetCurrentPriceIntent", "de-DE", "test-request-001", "amzn1.ask.account.abc");
        interceptor.process(input);
        assertThat(MDC.get("userId")).isEqualTo("amzn1.ask.account.abc");
    }

    @Test
    void usesRequestTypeForNonIntentRequest() {
        var request = LaunchRequest.builder().withRequestId("launch-001").build();
        var envelope = RequestEnvelope.builder().withRequest(request).build();
        var input = HandlerInput.builder().withRequestEnvelope(envelope).build();

        interceptor.process(input);

        assertThat(MDC.get("intentName")).isEqualTo("LaunchRequest");
    }

    @Test
    // KI-generiert: Prüft dass kein NPE entsteht wenn userId null ist
    void doesNotSetUserIdInMDCWhenNull() {
        var user = User.builder().build(); // userId = null
        var system = SystemState.builder().withUser(user).build();
        var context = Context.builder().withSystem(system).build();
        var request = LaunchRequest.builder().withRequestId("req-null-user").build();
        var envelope = RequestEnvelope.builder().withRequest(request).withContext(context).build();
        var input = HandlerInput.builder().withRequestEnvelope(envelope).build();

        interceptor.process(input);

        assertThat(MDC.get("userId")).isNull();
    }

    @Test
    // KI-generiert: Warme Lambda-Instanz – nach einer Handler-Exception läuft der MDCCleanupInterceptor nicht.
    // Der nächste Request darf keine Werte des vorherigen Aufrufers übernehmen.
    void clearsStaleMdcEntriesBeforePopulating() {
        MDC.put("userId", "amzn1.ask.account.previous-caller");
        MDC.put("intentName", "StaleIntent");
        var request = LaunchRequest.builder().withRequestId("launch-002").build();
        var envelope = RequestEnvelope.builder().withRequest(request).build();
        var input = HandlerInput.builder().withRequestEnvelope(envelope).build();

        interceptor.process(input);

        assertThat(MDC.get("userId")).isNull();
        assertThat(MDC.get("intentName")).isEqualTo("LaunchRequest");
    }

    // KI-generiert: Baut einen HandlerInput mit IntentRequest inkl. aller MDC-relevanten Felder
    private static HandlerInput intentInput(String intentName, String locale, String requestId, String userId) {
        var user = User.builder().withUserId(userId).build();
        var system = SystemState.builder().withUser(user).build();
        var context = Context.builder().withSystem(system).build();
        var intent = Intent.builder().withName(intentName).build();
        var request = IntentRequest.builder()
                .withRequestId(requestId)
                .withLocale(locale)
                .withIntent(intent)
                .build();
        var envelope = RequestEnvelope.builder()
                .withRequest(request)
                .withContext(context)
                .build();
        return HandlerInput.builder().withRequestEnvelope(envelope).build();
    }
}
