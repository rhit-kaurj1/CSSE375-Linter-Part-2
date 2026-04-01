package presentation;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import domain.Linter;

final class LintRunCoordinator {
    enum PreparationStatus {
        NO_FILES,
        NO_LINTERS,
        NO_READABLE_FILES,
        READY
    }

    static final class Preparation {
        private final PreparationStatus status;
        private final List<File> readableFiles;
        private final List<File> skippedFiles;
        private final List<Linter> selectedLinters;

        private Preparation(PreparationStatus status, List<File> readableFiles, List<File> skippedFiles,
                List<Linter> selectedLinters) {
            this.status = status;
            this.readableFiles = readableFiles;
            this.skippedFiles = skippedFiles;
            this.selectedLinters = selectedLinters;
        }

        PreparationStatus getStatus() {
            return status;
        }

        List<File> getReadableFiles() {
            return readableFiles;
        }

        List<File> getSkippedFiles() {
            return skippedFiles;
        }

        List<Linter> getSelectedLinters() {
            return selectedLinters;
        }
    }

    private final LinterRunner linterRunner;
    private final LintRunSummaryFormatter summaryFormatter;

    LintRunCoordinator(LinterRunner linterRunner, LintRunSummaryFormatter summaryFormatter) {
        this.linterRunner = linterRunner;
        this.summaryFormatter = summaryFormatter;
    }

    Preparation prepare(List<File> selectedFiles, List<Linter> selectedLinters) {
        if (selectedFiles.isEmpty()) {
            return new Preparation(PreparationStatus.NO_FILES, List.of(), List.of(), List.of());
        }

        if (selectedLinters.isEmpty()) {
            return new Preparation(PreparationStatus.NO_LINTERS, List.of(), List.of(), List.of());
        }

        List<File> readableFiles = new ArrayList<>();
        List<File> skippedFiles = new ArrayList<>();
        for (File file : selectedFiles) {
            if (file.exists() && file.isFile() && file.canRead()) {
                readableFiles.add(file);
            } else {
                skippedFiles.add(file);
            }
        }

        if (readableFiles.isEmpty()) {
            return new Preparation(PreparationStatus.NO_READABLE_FILES, List.of(), List.copyOf(skippedFiles),
                    List.copyOf(selectedLinters));
        }

        return new Preparation(PreparationStatus.READY, List.copyOf(readableFiles), List.copyOf(skippedFiles),
                List.copyOf(selectedLinters));
    }

    String run(Preparation preparation) {
        String output = linterRunner.runAllLinters(preparation.getReadableFiles(), preparation.getSelectedLinters());
        return summaryFormatter.addRunSummary(output, preparation.getReadableFiles().size(), preparation.getSkippedFiles());
    }

    String formatNoReadableFilesMessage(Preparation preparation) {
        StringBuilder builder = new StringBuilder();
        builder.append("No readable files selected.").append(System.lineSeparator());
        if (!preparation.getSkippedFiles().isEmpty()) {
            builder.append("Skipped files:").append(System.lineSeparator());
            for (File skippedFile : preparation.getSkippedFiles()) {
                builder.append("- ").append(skippedFile.getPath()).append(System.lineSeparator());
            }
        }
        return builder.toString();
    }
}