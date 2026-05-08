package presentation;

import java.io.File;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class LintResultHtmlFormatterTest {

    @Test
    void toHtmlAddsGoToErrorLinkForFileAndLineBlock(@TempDir Path tempDir) throws Exception {
        Path filePath = Files.writeString(tempDir.resolve("Example.java"), "class Example {}\n");
        String report = String.join(System.lineSeparator(),
                "File: " + filePath,
                "  Line 3: Law of Demeter violation",
                "Total least knowledge principle issues: 1");

        String html = new LintResultHtmlFormatter().toHtml(report, List.of(filePath.toFile()));

        assertTrue(html.contains("Go to error"));
        assertTrue(html.contains("goto?file="));
        assertTrue(html.contains("Line 3: Law of Demeter violation"));
    }

    @Test
    void toHtmlResolvesFilenameOnlyEntriesAgainstSelectedFiles(@TempDir Path tempDir) throws Exception {
        Path filePath = Files.writeString(tempDir.resolve("Example.java"), "import java.util.List;\n");
        String report = "Example.java:4 - Unused import: java.util.List";

        String html = new LintResultHtmlFormatter().toHtml(report, List.of(filePath.toFile()));

        assertTrue(html.contains("Go to error"));
        assertTrue(html.contains("Example.java:4 - Unused import: java.util.List"));
    }

    @Test
    void toHtmlResolvesFilenameOnlyEntriesInsideSelectedDirectory(@TempDir Path tempDir) throws Exception {
        Path nestedDir = Files.createDirectories(tempDir.resolve("main"));
        Path filePath = Files.writeString(nestedDir.resolve("City.java"), "class City {}\n");
        String report = "City.java:2 - Unused import: java.util.List";

        String html = new LintResultHtmlFormatter().toHtml(report, List.of(tempDir.toFile()));

        String expectedEncodedPath = URLEncoder.encode(filePath.toString(), StandardCharsets.UTF_8);
        assertTrue(html.contains("goto?file=" + expectedEncodedPath + "&line=2"));
        assertTrue(html.contains("Go to error"));
    }

    @Test
    void toHtmlAddsGoToErrorLinksForEachTrailingWhitespaceLine(@TempDir Path tempDir) throws Exception {
        File file = Files.writeString(tempDir.resolve("Example.java"), "class Example {}\n").toFile();
        String report = String.join(System.lineSeparator(),
                "File: " + file.getPath(),
                "Trailing whitespace found on line(s): 2, 4");

        String html = new LintResultHtmlFormatter().toHtml(report, List.of(file));

        int linkCount = html.split("Go to error", -1).length - 1;
        assertTrue(linkCount >= 2);
    }

        @Test
        void toHtmlAddsGoToErrorLinksForClassBasedLinterOutput(@TempDir Path tempDir) throws Exception {
        Path sourceFile = Files.writeString(tempDir.resolve("City.java"), String.join(System.lineSeparator(),
            "package main;",
            "public class City {",
            "    public String name;",
            "}"));

        String publicFieldReport = String.join(System.lineSeparator(),
            "Total public non-final field issues: 1",
            "Class: main.City",
            "  Public non-final field: name (suggest: make it private or final)");

        String srpReport = String.join(System.lineSeparator(),
            "Found 1 SRP violation(s).",
            "SRP Violation: main.City",
            "  LCOM Score: 6 (threshold: 2)");

        String publicFieldHtml = new LintResultHtmlFormatter().toHtml(publicFieldReport, List.of(sourceFile.toFile()));
        String srpHtml = new LintResultHtmlFormatter().toHtml(srpReport, List.of(sourceFile.toFile()));

        assertTrue(publicFieldHtml.contains("Go to error"));
        assertTrue(publicFieldHtml.contains("City.java"));
        assertTrue(srpHtml.contains("Go to error"));
        assertTrue(srpHtml.contains("City.java"));
        }
}