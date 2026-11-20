
package uk.gov.companieshouse.search.comparison.util;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import uk.gov.companieshouse.search.comparison.exception.SearchComparisonException;

class ValidationUtilsTest {

    @ParameterizedTest
    @MethodSource("blankIndexNameCases")
    void validateIndexName_throwsWhenNullOrBlank(String indexName, String fieldName, String expectedMessage) {
        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class,
                () -> ValidationUtils.validateIndexName(indexName, fieldName)
        );

        assertEquals(expectedMessage, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("invalidCharacterCases")
    void validateIndexName_throwsWhenContainsInvalidCharacters(
            String indexName,
            String fieldName,
            String expectedMessage) {
        SearchComparisonException exception = assertThrows(
                SearchComparisonException.class,
                () -> ValidationUtils.validateIndexName(indexName, fieldName)
        );

        assertEquals(expectedMessage, exception.getMessage());
    }

    @ParameterizedTest
    @MethodSource("validIndexNameCases")
    void validateIndexName_returnsOriginalValueWhenValid(String indexName, String fieldName) {
        String result = ValidationUtils.validateIndexName(indexName, fieldName);

        assertEquals(indexName, result);
    }

    private static Stream<Arguments> blankIndexNameCases() {
        return Stream.of(
                Arguments.of(null, "indexName", "indexName is required and must not be blank"),
                Arguments.of("   ", "indexName", "indexName is required and must not be blank")
        );
    }

    private static Stream<Arguments> invalidCharacterCases() {
        return Stream.of(
                Arguments.of("index$name", "indexName", "indexName contains invalid characters"),
                Arguments.of("index name", "indexName", "indexName contains invalid characters"),
                Arguments.of("index.name", "indexName", "indexName contains invalid characters")
        );
    }

    private static Stream<Arguments> validIndexNameCases() {
        return Stream.of(
                Arguments.of("search_index-01", "indexName"),
                Arguments.of("alpha_search", "blue_index_name"),
                Arguments.of("GREEN-INDEX_2", "green_index_name")
        );
    }
}