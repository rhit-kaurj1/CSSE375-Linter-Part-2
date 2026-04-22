package datastorage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OutputExporterTest {

    @Test
    void exportsNormalTextToFile(@TempDir Path tempDir) throws IOException {
        Path outputFile = tempDir.resolve("lint-output.txt");

        new OutputExporter().export(outputFile, "Line 1\nLine 2");

        assertEquals("Line 1\nLine 2", Files.readString(outputFile));
    }

    @Test
    void exportsEmptyOutputAsEmptyFile(@TempDir Path tempDir) throws IOException {
        Path outputFile = tempDir.resolve("empty-output.txt");

        new OutputExporter().export(outputFile, "");

        assertEquals("", Files.readString(outputFile));
    }

    @Test
    void throwsWhenFileWriteFails(@TempDir Path tempDir) throws IOException {
        Path outputTarget = Files.createDirectory(tempDir.resolve("blocked"));

        IOException thrown = assertThrows(IOException.class, () -> new OutputExporter().export(outputTarget, "data"));
        assertTrue(thrown.getMessage() == null || thrown.getMessage().contains("blocked"));
    }
}