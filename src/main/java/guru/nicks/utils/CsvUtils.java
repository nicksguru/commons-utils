package guru.nicks.utils;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvParser;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.experimental.UtilityClass;

import java.io.IOException;
import java.io.InputStream;

/**
 * CSV-related utility methods.
 */
@UtilityClass
public class CsvUtils {

    private static final CsvMapper CSV_MAPPER = CsvMapper.builder()
            // accept properties in any case: 'URL' = 'Url' = 'url'
            .configure(MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES, true)
            // trim leading/trailing spaces
            .enable(CsvParser.Feature.TRIM_SPACES)
            .enable(CsvParser.Feature.SKIP_EMPTY_LINES)
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            // Java 8 dates
            .addModule(new JavaTimeModule())
            .build();

    /**
     * Converts input stream to objects. The first line must contain headers (case-insensitive property names).
     *
     * @param is    input stream
     * @param clazz item class to convert each CSV row to
     * @param <T>   item type
     * @return iterator (to read all records, use {@link MappingIterator#readAll()})
     * @throws IllegalArgumentException error during parsing
     */
    public static <T> MappingIterator<T> parseCsv(InputStream is, Class<T> clazz) {
        CsvSchema schema = CSV_MAPPER.schemaFor(clazz)
                .withHeader()
                .withColumnReordering(true);

        try {
            return CSV_MAPPER.readerWithSchemaFor(clazz)
                    .with(schema)
                    .readValues(is);
        } catch (IOException e) {
            throw new IllegalArgumentException(e.getMessage(), e);
        }
    }

}
