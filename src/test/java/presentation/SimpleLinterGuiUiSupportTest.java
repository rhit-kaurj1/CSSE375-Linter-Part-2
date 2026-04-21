package presentation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SimpleLinterGuiUiSupportTest {

    @Test
    void buildFilePreviewIncludesReadableFileContents(@TempDir Path tempDir) throws IOException {
        Path filePath = Files.writeString(tempDir.resolve("Example.java"), "class Example {}\n");

        String preview = new SimpleLinterGuiUiSupport().buildFilePreview(List.of(filePath.toFile()));

        assertTrue(preview.contains("File: " + filePath));
        assertTrue(preview.contains("class Example {}"));
    }

    @Test
    void buildFilePreviewMarksClassFilesAsBinary(@TempDir Path tempDir) throws IOException {
        Path classFile = Files.write(tempDir.resolve("Example.class"), new byte[] { 0, 1, 2 });

        String preview = new SimpleLinterGuiUiSupport().buildFilePreview(List.of(classFile.toFile()));

        assertTrue(preview.contains("<Binary .class file preview unavailable>"));
    }

    @Test
    void buildFilePreviewShowsMissingFileMessage(@TempDir Path tempDir) {
        File missingFile = tempDir.resolve("Missing.java").toFile();

        String preview = new SimpleLinterGuiUiSupport().buildFilePreview(List.of(missingFile));

        assertTrue(preview.contains("<File does not exist>"));
    }

    @Test
    void buildFilePreviewIncludesEachSelectedFile(@TempDir Path tempDir) throws IOException {
        Path firstFile = Files.writeString(tempDir.resolve("First.java"), "class First {}\n");
        Path secondFile = Files.writeString(tempDir.resolve("Second.java"), "class Second {}\n");

        String preview = new SimpleLinterGuiUiSupport().buildFilePreview(List.of(firstFile.toFile(), secondFile.toFile()));

        assertTrue(preview.contains("File: " + firstFile));
        assertTrue(preview.contains("File: " + secondFile));
        assertTrue(preview.contains("class First {}"));
        assertTrue(preview.contains("class Second {}"));
    }

    @Test
    void buildFilePreviewHandlesEmptySelection() {
        String preview = new SimpleLinterGuiUiSupport().buildFilePreview(List.of());

        assertTrue(preview.contains("No files selected."));
    }
}