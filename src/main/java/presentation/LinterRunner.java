package presentation;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

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
        return runAllLinters(files, availableLinters, null);
    }

    public String runAllLinters(List<File> files, List<Linter> availableLinters, IntConsumer onLinterCompleted) {
        if (files == null || files.isEmpty()) {
            return "No files to lint.";
        }
        if (availableLinters == null || availableLinters.isEmpty()) {
            return "No linters configured.";
        }

        List<File> preparedFiles = prepareFilesForAllLinters(files);
        return runLinters(availableLinters, preparedFiles, onLinterCompleted);
    }

    private List<File> prepareFilesForAllLinters(List<File> files) {
        List<File> preparedFiles = new ArrayList<>(files);
        List<File> javaSourceFiles = collectJavaSources(files);
        if (javaSourceFiles.isEmpty()) {
            return preparedFiles;
        }

        File compiledDirectory = compileJavaSources(javaSourceFiles);
        if (compiledDirectory != null) {
            preparedFiles.add(compiledDirectory);
        }

        return preparedFiles;
    }

    private List<File> collectJavaSources(List<File> files) {
        Set<File> javaSources = new LinkedHashSet<>();
        for (File file : files) {
            collectJavaSourcesRecursively(file, javaSources);
        }
        return new ArrayList<>(javaSources);
    }

    private void collectJavaSourcesRecursively(File file, Set<File> javaSources) {
        if (file == null || !file.exists() || !file.canRead()) {
            return;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return;
            }
            for (File child : children) {
                collectJavaSourcesRecursively(child, javaSources);
            }
            return;
        }

        if (file.getName().toLowerCase(Locale.ROOT).endsWith(".java")) {
            javaSources.add(file);
        }
    }

    private File compileJavaSources(List<File> javaSourceFiles) {
        JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
        if (compiler == null) {
            return null;
        }

        Path outputDirectory;
        try {
            outputDirectory = Files.createTempDirectory("linter-compiled-sources-");
        } catch (IOException e) {
            return null;
        }
        File outputDirectoryFile = outputDirectory.toFile();
        outputDirectoryFile.deleteOnExit();

        try (StandardJavaFileManager fileManager = compiler.getStandardFileManager(null, null, null)) {
            List<String> options = new ArrayList<>();
            options.add("-d");
            options.add(outputDirectory.toString());

            String runtimeClasspath = System.getProperty("java.class.path");
            if (runtimeClasspath != null && !runtimeClasspath.isBlank()) {
                options.add("-classpath");
                options.add(runtimeClasspath);
            }

            int compiledCount = 0;
            for (File javaSourceFile : javaSourceFiles) {
                Iterable<? extends JavaFileObject> compilationUnit =
                        fileManager.getJavaFileObjectsFromFiles(List.of(javaSourceFile));
                JavaCompiler.CompilationTask task = compiler.getTask(
                        null,
                        fileManager,
                        null,
                        options,
                        null,
                        compilationUnit);
                if (Boolean.TRUE.equals(task.call())) {
                    compiledCount++;
                }
            }

            if (compiledCount > 0) {
                return outputDirectoryFile;
            }
            return null;
        } catch (IOException ex) {
            return null;
        }
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
        return runLinters(linters, files, null);
    }

    private String runLinters(List<Linter> linters, List<File> files, IntConsumer onLinterCompleted) {
        if (linters == null || linters.isEmpty()) {
            return "No linters configured for this file type." + System.lineSeparator();
        }

        StringBuilder output = new StringBuilder();
        int completedLinters = 0;
        for (Linter linter : linters) {
            output.append("[")
                .append(linter.getClass().getSimpleName())
                .append("]")
                .append(System.lineSeparator());
            String lintResult = linter.lint(files);
            if (lintResult == null || lintResult.isBlank()) {
                lintResult = "No findings reported by this linter.";
            }
            output.append(lintResult)
                .append(System.lineSeparator())
                .append(System.lineSeparator());

            completedLinters++;
            if (onLinterCompleted != null) {
                onLinterCompleted.accept(completedLinters);
            }
        }

        return output.toString();
    }
}
