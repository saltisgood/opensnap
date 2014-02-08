package com.nickstephen.opensnap.util.play;

/**
 * Created by Nick on 19/12/13.
 */
public final class SKU {
    private SKU() {}

    public static final String PREMIUM_FEATURES = "premium_content";
    public static final int REQUEST_PREMIUM = 10101;

    public static String retrieveB64() {
        return start() +
                "+zQh0o60fWbqYRmrHlXsz51xWrTp7XviFEwvtUpuBV6FSoc/dTuV7OYzHCpAKhyU8QZC3A5N+BcOOeRjrTa6o3ngdqbZGVO0cSd3qoyhfX6r2RmOc/EcHkIBKS+SG1AupL77pQBbKNoP7VK23gTAGWr0r"
                + "/+yZK3xn+9fW21U9ZN51+OCtZAmbRpvoFiri3CM6fYrnjhaRrvcqKlYqHPFXmqSn+"
                + end();
    }

    private static String start() {
        return "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEApAdUiRrhqamWGcMQRRlsgt0ECz1hqM0g8yFLmpfVUltMp";
    }

    private static String end() {
        return "334567uq+qS7j5VTZqXrH9bE4hny4t39naS7FPjgK1CU8JQlnpQnWYCcjTV1+2fS9su7Ap4Tcu25BJu6oQdQQIDAQAB-YOUSUCK-Bldfjti872jv 3jk".substring(6, 91);
    }
}
