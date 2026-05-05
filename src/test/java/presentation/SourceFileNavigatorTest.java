package presentation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SourceFileNavigatorTest {

    @Test
    void openSourceFileShowsClearErrorWhenFileDoesNotExist(@TempDir Path tempDir) {
        File missingFile = tempDir.resolve("Missing.java").toFile();

        IOException thrown = assertThrows(IOException.class,
                () -> new SourceFileNavigator().openSourceFile(null, missingFile, 1));

        assertTrue(thrown.getMessage().contains("File does not exist"));
        assertTrue(thrown.getMessage().contains(missingFile.getPath()));
    }

    @Test
    void openSourceFileShowsClearErrorWhenLineNumberIsOutOfRange(@TempDir Path tempDir) throws Exception {
        Path filePath = Files.writeString(tempDir.resolve("Example.java"), "class Example {}\n");

        IOException thrown = assertThrows(IOException.class,
                () -> new SourceFileNavigator().openSourceFile(null, filePath.toFile(), 5));

        assertTrue(thrown.getMessage().contains("Line 5 is not available"));
    }
}