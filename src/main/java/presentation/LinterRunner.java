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

        Map<String, List<File>> filesByType = splitFilesByType(files);

        StringBuilder output = new StringBuilder();
        appendLintSection(output, ".class Files", filesByType.get("class"),
            getConfiguredLinters("class", availableLinters));
        appendLintSection(output, "Non-.class Files", filesByType.get("nonClass"),
            getConfiguredLinters("nonClass", availableLinters));

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
        List<Linter> availableLinters) {

        List<Linter> selectedLinters = new ArrayList<>();
        List<Class<? extends Linter>> configuredTypes =
                LINTERS_BY_FILE_TYPE.getOrDefault(fileType, List.of());

        for (Class<? extends Linter> linterType : configuredTypes) {
            Linter linter = findCompatibleLinter(linterType, availableLinters, selectedLinters);
            if (linter != null) {
                selectedLinters.add(linter);
            }
        }

        return selectedLinters;
    }

    private Linter findCompatibleLinter(
        Class<? extends Linter> expectedType,
        List<Linter> availableLinters,
        List<Linter> alreadySelected) {

        for (Linter candidate : availableLinters) {
            if (alreadySelected.contains(candidate)) {
                continue;
            }
            if (expectedType.isInstance(candidate)) {
                return candidate;
            }
        }
        return null;
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
