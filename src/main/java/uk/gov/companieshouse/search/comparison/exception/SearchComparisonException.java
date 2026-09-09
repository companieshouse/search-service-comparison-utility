package uk.gov.companieshouse.search.comparison.exception;


public class SearchComparisonException extends RuntimeException {

    public SearchComparisonException(String message) {
        super(message);
    }

    public SearchComparisonException(String message, Throwable cause) {
        super(message, cause);
    }
}
