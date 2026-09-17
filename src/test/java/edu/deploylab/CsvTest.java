package edu.deploylab;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;
class CsvTest {
    @Test void escapesSpreadsheetFormulasQuotesAndNewlines() {
        assertThat(JobWorker.escapeCsv("=1+1")).isEqualTo("\"'=1+1\"");
        assertThat(JobWorker.escapeCsv("a\"b\nc")).isEqualTo("\"a\"\"b\nc\"");
        assertThat(JobWorker.escapeCsv(null)).isEqualTo("\"\"");
    }
}
