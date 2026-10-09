package org.tholian.alexa.tibber.linking;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

// KI-generiert: Strenger Parser für application/x-www-form-urlencoded
class FormDataTest {

    @Test
    void parse_decodesPairs() {
        assertThat(FormData.parse("a=1&b=x%20y&c=p%2Bq&d="))
                .containsEntry("a", "1").containsEntry("b", "x y").containsEntry("c", "p+q").containsEntry("d", "");
    }

    @Test
    void parse_emptyBody_returnsEmptyMap() {
        assertThat(FormData.parse("")).isEmpty();
    }

    @Test
    void parse_rejectsDuplicateNames() {
        assertThatThrownBy(() -> FormData.parse("redirect_uri=a&redirect_uri=b"))
                .isInstanceOf(InvalidAuthorizationRequestException.class);
    }

    @Test
    void parse_rejectsBrokenEncoding() {
        assertThatThrownBy(() -> FormData.parse("a=%zz"))
                .isInstanceOf(InvalidAuthorizationRequestException.class);
    }
}
