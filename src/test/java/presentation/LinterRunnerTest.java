package presentation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import datastorage.ASMReader;
import domain.SnakeLinter;
import domain.TooManyParametersLinter;
import domain.UnusedImportLinter;

class LinterRunnerTest {

    @Test
    void routesClassAndSourceFilesToMatchingLinters(@TempDir Path tempDir) throws IOException {
        Path classFile = Files.write(tempDir.resolve("Sample.class"), new byte[] { 0 });
        Path javaFile = Files.writeString(tempDir.resolve("Sample.java"), "class sample_file {}\n");

        RecordingTooManyParametersLinter classLinter = new RecordingTooManyParametersLinter("class-result");
        RecordingSnakeLinter sourceLinter = new RecordingSnakeLinter("source-result");

        String result = new LinterRunner().run(
                List.of(classFile.toFile(), javaFile.toFile()),
                List.of(classLinter, sourceLinter));

        assertEquals(List.of("Sample.class"), classLinter.getSeenFileNames());
        assertEquals(List.of("Sample.java"), sourceLinter.getSeenFileNames());
        assertTrue(result.contains("=== .class Files ==="));
        assertTrue(result.contains("=== Non-.class Files ==="));
        assertTrue(result.contains("[RecordingTooManyParametersLinter]"));
        assertTrue(result.contains("[RecordingSnakeLinter]"));
        assertTrue(result.contains("class-result"));
        assertTrue(result.contains("source-result"));
    }

    @Test
    void runAllLintersReturnsHelpfulMessagesAndRunsEveryConfiguredLinter(@TempDir Path tempDir) throws IOException {
        LinterRunner runner = new LinterRunner();

        assertEquals("No files to lint.", runner.runAllLinters(List.of(), List.of(new RecordingSnakeLinter("unused"))));
        assertEquals("No linters configured.",
                runner.runAllLinters(List.of(tempDir.resolve("Example.java").toFile()), List.of()));

        Path javaFile = Files.writeString(tempDir.resolve("Example.java"), "class sample_file {}\n");
        String result = runner.runAllLinters(
                List.of(javaFile.toFile()),
                List.of(new RecordingSnakeLinter("snake-output"), new RecordingTooManyParametersLinter("params-output")));

        assertTrue(result.contains("[RecordingSnakeLinter]"));
        assertTrue(result.contains("[RecordingTooManyParametersLinter]"));
        assertTrue(result.contains("snake-output"));
        assertTrue(result.contains("params-output"));
    }

    @Test
    void runAllLintersProgressCallbackIsCalledAfterEachLinter(@TempDir Path tempDir) throws IOException {
        LinterRunner runner = new LinterRunner();
        Path javaFile = Files.writeString(tempDir.resolve("Example.java"), "class sample_file {}\n");

        AtomicInteger lastCompletedCount = new AtomicInteger();
        runner.runAllLinters(
                List.of(javaFile.toFile()),
                List.of(new RecordingSnakeLinter("snake-output"), new RecordingTooManyParametersLinter("params-output")),
                lastCompletedCount::set);

        assertEquals(2, lastCompletedCount.get());
    }

    @Test
    void runAllLintersCompilesJavaSourcesForAsmLinters(@TempDir Path tempDir) throws IOException {
        LinterRunner runner = new LinterRunner();
        Path javaFile = Files.writeString(
                tempDir.resolve("TooManyArgs.java"),
                String.join(System.lineSeparator(),
                        "public class TooManyArgs {",
                        "    public void makeFlag(boolean a, boolean b, boolean c) { }",
                        "}"));

        String result = runner.runAllLinters(
                List.of(javaFile.toFile()),
                List.of(new TooManyParametersLinter(2, new ASMReader())));

        assertTrue(result.contains("Total too-many-parameters issues: 1"));
        assertTrue(result.contains("makeFlag"));
    }

        @Test
        void runAllLintersCompilesWhatItCanWhenOneSourceHasErrors(@TempDir Path tempDir) throws IOException {
        LinterRunner runner = new LinterRunner();
        Path validJavaFile = Files.writeString(
            tempDir.resolve("ValidTooManyArgs.java"),
            String.join(System.lineSeparator(),
                "public class ValidTooManyArgs {",
                "    public void makeFlag(boolean a, boolean b, boolean c) { }",
                "}"));
        Path invalidJavaFile = Files.writeString(
            tempDir.resolve("BrokenFile.java"),
            String.join(System.lineSeparator(),
                "public class BrokenFile {",
                "    public void brokenMethod( {",
                "}"));

        String result = runner.runAllLinters(
            List.of(validJavaFile.toFile(), invalidJavaFile.toFile()),
            List.of(new TooManyParametersLinter(2, new ASMReader())));

        assertTrue(result.contains("Total too-many-parameters issues: 1"));
        assertTrue(result.contains("makeFlag"));
        }

        @Test
        void runAllLintersShowsFallbackMessageWhenLinterReturnsBlank(@TempDir Path tempDir) throws IOException {
        LinterRunner runner = new LinterRunner();
        Path javaFile = Files.writeString(
            tempDir.resolve("NoUnusedImports.java"),
            String.join(System.lineSeparator(),
                "public class NoUnusedImports {",
                "    public void ok() { }",
                "}"));

        String result = runner.runAllLinters(
            List.of(javaFile.toFile()),
            List.of(new UnusedImportLinter()));

        assertTrue(result.contains("[UnusedImportLinter]"));
        assertTrue(result.contains("No findings reported by this linter."));
        }

    private static final class RecordingSnakeLinter extends SnakeLinter {
        private final String response;
        private List<String> seenFileNames = List.of();

        private RecordingSnakeLinter(String response) {
            this.response = response;
        }

        @Override
        public String lint(List<File> files) {
            this.seenFileNames = files.stream().map(File::getName).toList();
            return response;
        }

        private List<String> getSeenFileNames() {
            return seenFileNames;
        }
    }

    private static final class RecordingTooManyParametersLinter extends TooManyParametersLinter {
        private final String response;
        private List<String> seenFileNames = List.of();

        private RecordingTooManyParametersLinter(String response) {
            super(2, new ASMReader());
            this.response = response;
        }

        @Override
        public String lint(List<File> files) {
            this.seenFileNames = files.stream().map(File::getName).toList();
            return response;
        }

        private List<String> getSeenFileNames() {
            return seenFileNames;
        }
    }
}
