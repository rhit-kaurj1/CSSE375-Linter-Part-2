package presentation;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import domain.AdapterPatternLinter;
import domain.BooleanFlagMethodLinter;
import domain.DecoratorPatternLinter;
import domain.DesignRiskLinter;
import domain.FacadePatternLinter;
import domain.Linter;
import domain.PlantUMLGenerator;
import domain.PublicNonFinalFieldLinter;
import domain.SRPLinter;
import domain.SingletonPatternLinter;
import domain.SnakeLinter;
import domain.StrategyPatternLinter;
import domain.TooManyParametersLinter;
import domain.TrailingWhitespaceLinter;
import domain.UnusedImportLinter;

public class LinterRunner {

    private static final Map<String, List<Class<? extends Linter>>> LINTERS_BY_FILE_TYPE = Map.of(
        "class", List.of(
            SRPLinter.class,
            FacadePatternLinter.class,
            StrategyPatternLinter.class,
            SingletonPatternLinter.class,
            DecoratorPatternLinter.class,
            AdapterPatternLinter.class,
            BooleanFlagMethodLinter.class,
            PublicNonFinalFieldLinter.class,
            TooManyParametersLinter.class,
            PlantUMLGenerator.class,
            DesignRiskLinter.class
        ),
        "nonClass", List.of(
            SnakeLinter.class,
            UnusedImportLinter.class,
            TrailingWhitespaceLinter.class
        )
    );


    public String run(List<File> files, List<Linter> availableLinters) {
        if (files == null || files.isEmpty()) {
            return "No files to lint.";
        }
        if (availableLinters == null || availableLinters.isEmpty()) {
            return "No linters configured.";
        }

        Map<Class<? extends Linter>, Linter> indexedLinters = indexLintersByType(availableLinters);
        Map<String, List<File>> filesByType = splitFilesByType(files);

        StringBuilder output = new StringBuilder();
        appendLintSection(output, ".class Files", filesByType.get("class"),
            getConfiguredLinters("class", indexedLinters));
        appendLintSection(output, "Non-.class Files", filesByType.get("nonClass"),
            getConfiguredLinters("nonClass", indexedLinters));

        return output.length() == 0 ? "No files to lint." : output.toString();
    }

    public String runAllLinters(List<File> files, List<Linter> availableLinters) {
        if (files == null || files.isEmpty()) {
            return "No files to lint.";
        }
        if (availableLinters == null || availableLinters.isEmpty()) {
            return "No linters configured.";
        }

        return runLinters(availableLinters, files);
    }

    private Map<Class<? extends Linter>, Linter> indexLintersByType(List<Linter> availableLinters) {
        Map<Class<? extends Linter>, Linter> indexedLinters = new LinkedHashMap<>();
        for (Linter linter : availableLinters) {
            indexedLinters.put(linter.getClass(), linter);
        }
        return indexedLinters;
    }

    private Map<String, List<File>> splitFilesByType(List<File> files) {
        Map<String, List<File>> filesByType = new LinkedHashMap<>();
        filesByType.put("class", new ArrayList<>());
        filesByType.put("nonClass", new ArrayList<>());

        for (File file : files) {
            if (isClassFile(file)) {
                filesByType.get("class").add(file);
            } else {
                filesByType.get("nonClass").add(file);
            }
        }

        return filesByType;
    }

    private boolean isClassFile(File file) {
        return file.getName().toLowerCase(Locale.ROOT).endsWith(".class");
    }

    private List<Linter> getConfiguredLinters(
        String fileType,
        Map<Class<? extends Linter>, Linter> indexedLinters) {

        List<Linter> selectedLinters = new ArrayList<>();
        List<Class<? extends Linter>> configuredTypes =
                LINTERS_BY_FILE_TYPE.getOrDefault(fileType, List.of());

        for (Class<? extends Linter> linterType : configuredTypes) {
            Linter linter = indexedLinters.get(linterType);
            if (linter != null) {
                selectedLinters.add(linter);
            }
        }

        return selectedLinters;
    }

    private void appendLintSection(
        StringBuilder output,
        String sectionTitle,
        List<File> files,
        List<Linter> linters) {

        if (files == null || files.isEmpty()) {
            return;
        }

        if (output.length() > 0) {
            output.append(System.lineSeparator());
        }

        output.append("=== ").append(sectionTitle).append(" ===")
            .append(System.lineSeparator());
        output.append(runLinters(linters, files));
    }

    private String runLinters(List<Linter> linters, List<File> files) {
        if (linters == null || linters.isEmpty()) {
            return "No linters configured for this file type." + System.lineSeparator();
        }

        StringBuilder output = new StringBuilder();
        for (Linter linter : linters) {
            output.append("[")
                .append(linter.getClass().getSimpleName())
                .append("]")
                .append(System.lineSeparator());
            output.append(linter.lint(files))
                .append(System.lineSeparator())
                .append(System.lineSeparator());
        }

        return output.toString();
    }
}
