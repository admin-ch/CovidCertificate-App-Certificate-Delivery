package ch.admin.bag.covidcertificate.backend.delivery.data.util;

import org.testcontainers.shaded.org.apache.commons.lang.RandomStringUtils;

public class RandomGenerator {

    public static String randomAlphaNumericString() {
        return RandomStringUtils.random(40, true, true);
    }
}
