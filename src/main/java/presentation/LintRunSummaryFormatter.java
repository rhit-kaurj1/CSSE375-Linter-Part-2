package presentation;

import java.io.File;
import java.util.List;

final class LintRunSummaryFormatter {
    String addRunSummary(String linterOutput, int processedCount, List<File> skippedFiles) {
        StringBuilder summary = new StringBuilder();
        summary.append("Files processed: ").append(processedCount).append(System.lineSeparator());
        summary.append("Files skipped: ").append(skippedFiles.size()).append(System.lineSeparator())
                .append(System.lineSeparator());

        if (!skippedFiles.isEmpty()) {
            summary.append("Skipped files:").append(System.lineSeparator());
            for (File skippedFile : skippedFiles) {
                summary.append("- ").append(skippedFile.getPath()).append(System.lineSeparator());
            }
            summary.append(System.lineSeparator());
        }

        if (linterOutput == null || linterOutput.trim().isEmpty()) {
            summary.append("No issues found.");
            return summary.toString();
        }

        summary.append(linterOutput);
        return summary.toString();
    }
}