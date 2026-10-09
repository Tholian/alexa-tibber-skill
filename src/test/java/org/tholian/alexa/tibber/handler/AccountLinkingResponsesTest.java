package org.tholian.alexa.tibber.handler;

import com.amazon.ask.model.ui.LinkAccountCard;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

// KI-generiert: Gemeinsame Antworten rund um die Konto-Verknüpfung
class AccountLinkingResponsesTest {

    @Test
    void accessToken_withToken_returnsIt() {
        assertThat(AccountLinkingResponses.accessToken(HandlerTestHelper.intentInput("GetCurrentPriceIntent", "abc")))
                .isEqualTo("abc");
    }

    @Test
    void accessToken_withMissingToken_returnsNull() {
        assertThat(AccountLinkingResponses.accessToken(HandlerTestHelper.intentInput("GetCurrentPriceIntent", null)))
                .isNull();
    }

    @Test
    void accessToken_withBlankToken_returnsNull() {
        assertThat(AccountLinkingResponses.accessToken(HandlerTestHelper.intentInput("GetCurrentPriceIntent", "   ")))
                .isNull();
    }

    @Test
    void linkAccount_asksToLinkWithCard() {
        var response = AccountLinkingResponses.linkAccount(HandlerTestHelper.intentInput("GetCurrentPriceIntent", null));

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("verknüpfe");
        assertThat(response.get().getCard()).isInstanceOf(LinkAccountCard.class);
    }

    @Test
    void relinkAccount_asksToRelinkWithCardAndEndsSession() {
        var response = AccountLinkingResponses.relinkAccount(HandlerTestHelper.intentInput("GetCurrentPriceIntent", "abc"));

        assertThat(response).isPresent();
        assertThat(HandlerTestHelper.extractSpeech(response.get())).contains("nicht mehr gültig").contains("neu");
        assertThat(response.get().getCard()).isInstanceOf(LinkAccountCard.class);
        assertThat(response.get().getShouldEndSession()).isTrue();
    }
}
