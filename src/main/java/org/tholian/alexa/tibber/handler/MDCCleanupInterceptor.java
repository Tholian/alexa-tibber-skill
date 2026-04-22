
package org.tholian.alexa.tibber.handler;

import com.amazon.ask.dispatcher.request.handler.HandlerInput;
import com.amazon.ask.dispatcher.request.interceptor.ResponseInterceptor;
import com.amazon.ask.model.Response;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * KI-generiert: Bereinigt den MDC nach jeder Handler-Ausführung.
 * Verhindert MDC-Leakage zwischen aufeinanderfolgenden Requests
 * (relevant bei Thread-Reuse in Servlet-Containern und Lambda Execution Environments).
 */
@Component
public class MDCCleanupInterceptor implements ResponseInterceptor {

    @Override
    public void process(HandlerInput input, Optional<Response> output) {
        MDC.clear();
    }
}
