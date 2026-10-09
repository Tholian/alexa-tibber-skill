package org.tholian.alexa.tibber.linking;

import org.junit.jupiter.api.Test;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// KI-generiert: Strenge Prüfung der OAuth-Parameter (SICHERHEIT: keine Weiterleitung an fremde Adressen)
class AuthorizationRequestTest {

    private static final String REDIRECT = "https://layla.amazon.com/api/skill/link/M2ABCDEFGHIJ";
    private static final LinkingSettings SETTINGS = new LinkingSettings("tibber-skill-client", Set.of(REDIRECT),
            URI.create("https://api.tibber.com/v1-beta/gql"), null);

    private static Map<String, String> params() {
        Map<String, String> params = new HashMap<>();
        params.put("state", "abc-123");
        params.put("client_id", "tibber-skill-client");
        params.put("response_type", "token");
        params.put("redirect_uri", REDIRECT);
        return params;
    }

    @Test
    void validate_withValidParams_returnsRequest() {
        var request = AuthorizationRequest.validate(params(), SETTINGS);

        assertThat(request.state()).isEqualTo("abc-123");
        assertThat(request.redirectUri()).isEqualTo(REDIRECT);
        assertThat(request.scope()).isNull();
    }

    @Test
    void successRedirect_putsTokenIntoFragment() {
        var request = AuthorizationRequest.validate(params(), SETTINGS);

        assertThat(request.successRedirect("Tok3n-Value"))
                .isEqualTo(REDIRECT + "#state=abc-123&access_token=Tok3n-Value&token_type=Bearer");
    }

    @Test
    void successRedirect_percentEncodesStateAndToken() {
        var params = params();
        params.put("state", "a b&c");
        var request = AuthorizationRequest.validate(params, SETTINGS);

        assertThat(request.successRedirect("x+y/z="))
                .isEqualTo(REDIRECT + "#state=a%20b%26c&access_token=x%2By%2Fz%3D&token_type=Bearer");
    }

    @Test
    void validate_rejectsForeignOrMissingRedirectUri() {
        for (String redirect : new String[] {"https://evil.example/cb", REDIRECT + "x", REDIRECT.toUpperCase(), ""}) {
            var params = params();
            params.put("redirect_uri", redirect);
            assertThatThrownBy(() -> AuthorizationRequest.validate(params, SETTINGS))
                    .as(redirect)
                    .isInstanceOf(InvalidAuthorizationRequestException.class)
                    .hasMessage("Unzulässige Weiterleitungsadresse.");
        }
        var params = params();
        params.remove("redirect_uri");
        assertThatThrownBy(() -> AuthorizationRequest.validate(params, SETTINGS))
                .isInstanceOf(InvalidAuthorizationRequestException.class);
    }

    @Test
    void validate_rejectsOtherResponseType() {
        var params = params();
        params.put("response_type", "code");

        assertThatThrownBy(() -> AuthorizationRequest.validate(params, SETTINGS))
                .isInstanceOf(InvalidAuthorizationRequestException.class);
    }

    @Test
    void validate_rejectsUnknownClientId() {
        var params = params();
        params.put("client_id", "someone-else");

        assertThatThrownBy(() -> AuthorizationRequest.validate(params, SETTINGS))
                .isInstanceOf(InvalidAuthorizationRequestException.class);
    }

    @Test
    void validate_rejectsMissingState() {
        var params = params();
        params.remove("state");

        assertThatThrownBy(() -> AuthorizationRequest.validate(params, SETTINGS))
                .isInstanceOf(InvalidAuthorizationRequestException.class);
    }

    @Test
    void validate_rejectsControlCharactersAndOverlongValues() {
        var withCrlf = params();
        withCrlf.put("state", "abc\r\nLocation: https://evil.example");
        assertThatThrownBy(() -> AuthorizationRequest.validate(withCrlf, SETTINGS))
                .isInstanceOf(InvalidAuthorizationRequestException.class);

        var overlong = params();
        overlong.put("state", "a".repeat(AuthorizationRequest.MAX_PARAMETER_LENGTH + 1));
        assertThatThrownBy(() -> AuthorizationRequest.validate(overlong, SETTINGS))
                .isInstanceOf(InvalidAuthorizationRequestException.class);
    }

    @Test
    void invalidRequest_hasStatus400ByDefault() {
        assertThat(new InvalidAuthorizationRequestException("x").status()).isEqualTo(400);
        assertThat(new InvalidAuthorizationRequestException(413, "x").status()).isEqualTo(413);
    }
}
