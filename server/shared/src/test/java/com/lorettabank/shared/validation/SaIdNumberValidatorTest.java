package com.lorettabank.shared.validation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SaIdNumberValidatorTest {

    @ParameterizedTest
    @ValueSource(strings = {"9501015800087", "8001015009087"})
    void acceptsValidIds(String id) {
        assertThat(SaIdNumberValidator.isValid(id)).isTrue();
    }

    @ParameterizedTest
    @ValueSource(
            strings = {
                "9501015800089", // bad check digit
                "9513015800082", // month 13 (valid check digit)
                "950101580008", // 12 digits
                "95010158000AB", // letters
                "9501015800285" // citizenship digit 2 (valid check digit)
            })
    void rejectsInvalidIds(String id) {
        assertThat(SaIdNumberValidator.isValid(id)).isFalse();
    }
}
