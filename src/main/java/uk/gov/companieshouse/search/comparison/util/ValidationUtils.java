package uk.gov.companieshouse.search.comparison.util;

import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;

public final class ValidationUtils {

    private static final String INDEX_NAME_PATTERN = "^[a-zA-Z0-9_-]+$";

    private ValidationUtils() {
    }

    public static String validateIndexName(String indexName, String fieldName)
            throws SearchComparisonException {

        if (indexName == null || indexName.isBlank()) {
            throw new SearchComparisonException(
                    fieldName + " is required and must not be blank");
        }

        if (!indexName.matches(INDEX_NAME_PATTERN)) {
            throw new SearchComparisonException(
                    fieldName + " contains invalid characters");
        }

        return indexName;
    }
}