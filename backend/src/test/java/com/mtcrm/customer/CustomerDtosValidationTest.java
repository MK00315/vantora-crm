package com.mtcrm.customer;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CustomerDtosValidationTest {
    private static final ValidatorFactory FACTORY = Validation.buildDefaultValidatorFactory();
    private static final Validator VALIDATOR = FACTORY.getValidator();

    @AfterAll
    static void closeValidatorFactory() {
        FACTORY.close();
    }

    @Test
    void acceptsOnlyWebUrlsForCustomerWebsites() {
        assertThat(VALIDATOR.validate(request("https://example.com/about"))).isEmpty();
        assertThat(VALIDATOR.validate(request("http://localhost:3000"))).isEmpty();
        assertThat(VALIDATOR.validate(request(null))).isEmpty();
        assertThat(VALIDATOR.validate(request(""))).isEmpty();

        assertThat(VALIDATOR.validate(request("javascript:alert(1)"))).isNotEmpty();
        assertThat(VALIDATOR.validate(request("data:text/html,unsafe"))).isNotEmpty();
        assertThat(VALIDATOR.validate(request("ftp://example.com/file"))).isNotEmpty();
    }

    private static CustomerDtos.Request request(String website) {
        return new CustomerDtos.Request("Acme", null, null, null, website, CustomerStatus.ACTIVE, null, null);
    }
}
