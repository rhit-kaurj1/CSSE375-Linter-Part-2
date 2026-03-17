package datastorage;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileLoaderTest {

    @Test
    void skipsMissingPathsRemovesDuplicatesAndExpandsDirectories(@TempDir Path tempDir) throws IOException {
        Path nestedDirectory = Files.createDirectories(tempDir.resolve("folder/nested"));
        Path alphaFile = Files.writeString(tempDir.resolve("folder/Alpha.java"), "class alpha_file {}\n");
        Path betaFile = Files.writeString(nestedDirectory.resolve("Beta.java"), "class beta_file {}\n");
        Path standaloneFile = Files.writeString(tempDir.resolve("Standalone.java"), "class standalone_file {}\n");
        Path missingPath = tempDir.resolve("missing.java");

        String input = String.join(",",
                tempDir.resolve("folder").toString(),
                standaloneFile.toString(),
                standaloneFile.toString(),
                missingPath.toString());

        FileLoader loader = new FileLoader();
        List<File> files = loader.loadFiles(input);

        assertEquals(3, files.size());
        assertEquals(alphaFile.toFile().getAbsolutePath(), files.get(0).getAbsolutePath());
        assertEquals(betaFile.toFile().getAbsolutePath(), files.get(1).getAbsolutePath());
        assertEquals(standaloneFile.toFile().getAbsolutePath(), files.get(2).getAbsolutePath());
        assertEquals(new File(missingPath.toString()), loader.getFile());
    }

    @Test
    void returnsEmptyListForBlankInput() {
        FileLoader loader = new FileLoader();

        assertTrue(loader.loadFiles(null).isEmpty());
        assertTrue(loader.loadFiles("   ").isEmpty());
    }
}
