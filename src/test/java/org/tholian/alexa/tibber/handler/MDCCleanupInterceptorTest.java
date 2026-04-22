
package org.tholian.alexa.tibber.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.model.LaunchRequest;
import com.amazon.ask.model.RequestEnvelope;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class MDCCleanupInterceptorTest {

    private final MDCCleanupInterceptor interceptor = new MDCCleanupInterceptor();

    @Test
    void clearsMDCAfterRequest() {
        MDC.put("intentName", "GetCurrentPriceIntent");
        MDC.put("requestId", "test-123");
        MDC.put("userId", "amzn1.ask.account.abc");
        MDC.put("locale", "de-DE");

        var request = LaunchRequest.builder().withRequestId("test-123").build();
        var envelope = RequestEnvelope.builder().withRequest(request).build();
        var input = HandlerInput.builder().withRequestEnvelope(envelope).build();

        interceptor.process(input, Optional.empty());

        assertThat(MDC.getCopyOfContextMap()).isNullOrEmpty();
    }
}
