package presentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import domain.Linter;

class LintRunCoordinatorTest {

    @Test
    void prepareReturnsNoFilesWhenNothingIsSelected() {
        LintRunCoordinator coordinator = new LintRunCoordinator(new LinterRunner(), new LintRunSummaryFormatter());

        LintRunCoordinator.Preparation preparation = coordinator.prepare(List.of(), List.of(new TestLinter()));

        assertEquals(LintRunCoordinator.PreparationStatus.NO_FILES, preparation.getStatus());
    }

    @Test
    void prepareReturnsNoLintersWhenNothingIsChecked(@TempDir Path tempDir) throws IOException {
        LintRunCoordinator coordinator = new LintRunCoordinator(new LinterRunner(), new LintRunSummaryFormatter());
        Path filePath = Files.writeString(tempDir.resolve("Example.java"), "class Example {}\n");

        LintRunCoordinator.Preparation preparation = coordinator.prepare(List.of(filePath.toFile()), List.of());

        assertEquals(LintRunCoordinator.PreparationStatus.NO_LINTERS, preparation.getStatus());
    }

    @Test
    void prepareReturnsNoReadableFilesWhenAllFilesAreInvalid(@TempDir Path tempDir) {
        LintRunCoordinator coordinator = new LintRunCoordinator(new LinterRunner(), new LintRunSummaryFormatter());
        File missingFile = tempDir.resolve("Missing.java").toFile();

        LintRunCoordinator.Preparation preparation = coordinator.prepare(List.of(missingFile), List.of(new TestLinter()));

        assertEquals(LintRunCoordinator.PreparationStatus.NO_READABLE_FILES, preparation.getStatus());
        assertTrue(preparation.getReadableFiles().isEmpty());
        assertEquals(1, preparation.getSkippedFiles().size());
    }

    @Test
    void prepareReturnsReadyWhenReadableFilesAndLintersExist(@TempDir Path tempDir) throws IOException {
        LintRunCoordinator coordinator = new LintRunCoordinator(new LinterRunner(), new LintRunSummaryFormatter());
        Path filePath = Files.writeString(tempDir.resolve("Example.java"), "class Example {}\n");

        LintRunCoordinator.Preparation preparation = coordinator.prepare(List.of(filePath.toFile()), List.of(new TestLinter()));

        assertEquals(LintRunCoordinator.PreparationStatus.READY, preparation.getStatus());
        assertEquals(1, preparation.getReadableFiles().size());
        assertTrue(preparation.getSkippedFiles().isEmpty());
    }

    private static final class TestLinter implements Linter {
        @Override
        public String lint(List<File> files) {
            return "";
        }
    }
}