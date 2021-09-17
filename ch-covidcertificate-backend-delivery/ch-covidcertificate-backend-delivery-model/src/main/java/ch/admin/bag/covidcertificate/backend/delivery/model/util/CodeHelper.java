package ch.admin.bag.covidcertificate.backend.delivery.model.util;

public class CodeHelper {

    private CodeHelper() {}

    public static String getSanitizedCode(String unsanitized) {
        return (unsanitized != null) ? unsanitized.replaceAll("\\s", "").toUpperCase() : null;
    }
}
