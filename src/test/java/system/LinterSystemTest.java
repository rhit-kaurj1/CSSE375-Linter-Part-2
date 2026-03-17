package system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import datastorage.ConfigLoader;
import datastorage.FileLoader;
import domain.Linter;
import domain.LinterConfig;
import presentation.LinterFactory;
import presentation.LinterRunner;

class LinterSystemTest {

    @Test
    void processesSelectedDirectoryThroughRealWorkflow(@TempDir Path tempDir) throws IOException {
        Path inputDirectory = Files.createDirectory(tempDir.resolve("input"));
        Path javaFile = inputDirectory.resolve("BadName.java");
        Files.writeString(javaFile, String.join(System.lineSeparator(),
                "import java.util.List;",
                "public class BadName {    ",
                "    private int badField = 1;    ",
                "}"));

        Path configFile = tempDir.resolve("linter.properties");
        Files.writeString(configFile, String.join(System.lineSeparator(),
                "enabled_linters=SnakeLinter,UnusedImportLinter,TrailingWhitespaceLinter",
                "too_many_parameters_limit=4",
                "srp_lcom_threshold=2"));

        FileLoader fileLoader = new FileLoader();
        List<File> files = fileLoader.loadFiles(inputDirectory.toString());
        assertEquals(1, files.size());
        assertEquals(javaFile.toAbsolutePath().toString(), files.get(0).getAbsolutePath());

        LinterConfig config = new ConfigLoader().loadConfig(configFile.toString());
        List<Linter> linters = new LinterFactory().createLinters(config);

        String output = new LinterRunner().run(files, linters);

        assertTrue(output.contains("=== Non-.class Files ==="));
        assertTrue(output.contains("[SnakeLinter]"));
        assertTrue(output.contains("[UnusedImportLinter]"));
        assertTrue(output.contains("[TrailingWhitespaceLinter]"));
        assertTrue(output.contains("Class name 'BadName' does not follow snake_case"));
        assertTrue(output.contains("Field name 'badField' does not follow snake_case"));
        assertTrue(output.contains("Unused import: java.util.List"));
        assertTrue(output.contains("Total trailing whitespace issues: 2"));
    }
}
