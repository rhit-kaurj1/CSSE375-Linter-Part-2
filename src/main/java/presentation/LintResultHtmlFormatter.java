package presentation;

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class LintResultHtmlFormatter {
    static final String LINK_PREFIX = "goto?";
    static final String FILE_PARAMETER = "file";
    static final String LINE_PARAMETER = "line";

    private static final Pattern FILE_HEADER_PATTERN = Pattern.compile("^File: (.+)$");
    private static final Pattern LINE_ENTRY_PATTERN = Pattern.compile("^(\\s*)Line (\\d+): (.+)$");
    private static final Pattern MULTI_LINE_ENTRY_PATTERN = Pattern.compile(
            "^(\\s*)Trailing whitespace found on line\\(s\\): (.+)$");
    private static final Pattern SIMPLE_FILE_LINE_PATTERN = Pattern.compile("^([^:]+):(\\d+) - (.+)$");
    private static final Pattern CLASS_HEADER_PATTERN = Pattern.compile("^(?:SRP Violation|Class): (.+)$");
    private static final Pattern PUBLIC_FIELD_PATTERN = Pattern.compile("^(\\s*)Public non-final field: (.+)$");

    String toHtml(String plainText, List<File> availableFiles) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family:monospace; font-size:9.5px;'><pre>");

        if (plainText != null && !plainText.isEmpty()) {
            String[] lines = plainText.split("\\R", -1);
            String currentFilePath = null;
            String currentClassName = null;

            for (int i = 0; i < lines.length; i++) {
                String line = lines[i];
                String renderedLine = renderLine(line, currentFilePath, currentClassName, availableFiles);

                Matcher fileHeaderMatcher = FILE_HEADER_PATTERN.matcher(line);
                if (fileHeaderMatcher.matches()) {
                    currentFilePath = fileHeaderMatcher.group(1);
                    currentClassName = null;
                } else {
                    Matcher classHeaderMatcher = CLASS_HEADER_PATTERN.matcher(line);
                    if (classHeaderMatcher.matches()) {
                        currentClassName = classHeaderMatcher.group(1).trim();
                        String resolvedClassPath = resolveClassSourcePath(currentClassName, availableFiles);
                        if (resolvedClassPath != null) {
                            currentFilePath = resolvedClassPath;
                        }
                    }

                    Matcher simpleFileMatcher = SIMPLE_FILE_LINE_PATTERN.matcher(line);
                    if (simpleFileMatcher.matches()) {
                        currentFilePath = resolveFilePath(simpleFileMatcher.group(1), availableFiles);
                        currentClassName = null;
                    }
                }

                html.append(renderedLine);
                if (i < lines.length - 1) {
                    html.append("<br>");
                }
            }
        }

        html.append("</pre></body></html>");
        return html.toString();
    }

    private String renderLine(String line, String currentFilePath, String currentClassName, List<File> availableFiles) {
        Matcher lineEntryMatcher = LINE_ENTRY_PATTERN.matcher(line);
        if (lineEntryMatcher.matches() && currentFilePath != null) {
            int lineNumber = Integer.parseInt(lineEntryMatcher.group(2));
            return escapeHtml(lineEntryMatcher.group(1))
                    + "Line " + lineNumber + ": "
                    + escapeHtml(lineEntryMatcher.group(3))
                    + " " + buildGoToLink(currentFilePath, lineNumber);
        }

        Matcher trailingWhitespaceMatcher = MULTI_LINE_ENTRY_PATTERN.matcher(line);
        if (trailingWhitespaceMatcher.matches() && currentFilePath != null) {
            return escapeHtml(trailingWhitespaceMatcher.group(1))
                    + "Trailing whitespace found on line(s): "
                    + buildNumberLinkList(currentFilePath, trailingWhitespaceMatcher.group(2));
        }

        Matcher publicFieldMatcher = PUBLIC_FIELD_PATTERN.matcher(line);
        if (publicFieldMatcher.matches() && currentFilePath != null) {
            return escapeHtml(publicFieldMatcher.group(1))
                    + "Public non-final field: "
                    + escapeHtml(publicFieldMatcher.group(2))
                    + " " + buildGoToLink(currentFilePath, resolveHighlightLine(currentFilePath, currentClassName));
        }

        Matcher classHeaderMatcher = CLASS_HEADER_PATTERN.matcher(line);
        if (classHeaderMatcher.matches()) {
            String className = classHeaderMatcher.group(1).trim();
            String resolvedClassPath = resolveClassSourcePath(className, availableFiles);
            if (resolvedClassPath != null) {
                return escapeHtml(line) + " " + buildGoToLink(resolvedClassPath, resolveHighlightLine(resolvedClassPath, className));
            }
        }

        Matcher simpleFileMatcher = SIMPLE_FILE_LINE_PATTERN.matcher(line);
        if (simpleFileMatcher.matches()) {
            String resolvedFilePath = resolveFilePath(simpleFileMatcher.group(1), availableFiles);
            int lineNumber = Integer.parseInt(simpleFileMatcher.group(2));
            return escapeHtml(simpleFileMatcher.group(1))
                    + ":" + lineNumber + " - "
                    + escapeHtml(simpleFileMatcher.group(3))
                    + " " + buildGoToLink(resolvedFilePath, lineNumber);
        }

        return escapeHtml(line);
    }

    private int resolveHighlightLine(String filePath, String className) {
        if (filePath == null) {
            return 1;
        }

        File file = new File(filePath);
        if (!file.exists() || file.isDirectory() || !file.getName().endsWith(".java")) {
            return 1;
        }

        String simpleName = simpleClassName(className);
        try {
            List<String> lines = Files.readAllLines(file.toPath(), StandardCharsets.UTF_8);
            for (int i = 0; i < lines.size(); i++) {
                String trimmed = lines.get(i).trim();
                if (trimmed.contains(" class " + simpleName + " ")
                        || trimmed.endsWith(" class " + simpleName + "{")
                        || trimmed.contains(" class " + simpleName + "{")
                        || trimmed.contains(" interface " + simpleName + " ")
                        || trimmed.contains(" record " + simpleName + " ")) {
                    return i + 1;
                }
            }
        } catch (IOException ex) {
            return 1;
        }

        return 1;
    }

    private String resolveClassSourcePath(String className, List<File> availableFiles) {
        String simpleName = simpleClassName(className);
        String expectedJavaName = simpleName + ".java";
        String expectedClassName = simpleName + ".class";

        if (availableFiles != null) {
            for (File file : availableFiles) {
                String resolved = findMatchingPath(file, className, expectedJavaName, expectedClassName);
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        return null;
    }

    private String findMatchingPath(File file, String className, String expectedJavaName, String expectedClassName) {
        if (file == null) {
            return null;
        }

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return null;
            }

            for (File child : children) {
                String resolved = findMatchingPath(child, className, expectedJavaName, expectedClassName);
                if (resolved != null) {
                    return resolved;
                }
            }
            return null;
        }

        String path = file.getPath();
        String fileName = file.getName();
        String packageSuffix = className.replace('.', File.separatorChar);

        if (path.endsWith(packageSuffix + ".java")
                || path.endsWith(packageSuffix + ".class")
                || fileName.equals(expectedJavaName)
                || fileName.equals(expectedClassName)) {
            if (path.endsWith(".java")) {
                return path;
            }
            return path;
        }

        return null;
    }

    private String simpleClassName(String className) {
        int lastDot = className.lastIndexOf('.');
        if (lastDot >= 0 && lastDot < className.length() - 1) {
            return className.substring(lastDot + 1);
        }
        return className;
    }

    private String buildNumberLinkList(String filePath, String lineNumbers) {
        StringBuilder builder = new StringBuilder();
        String[] parts = lineNumbers.split(",");

        for (String rawPart : parts) {
            String part = rawPart.trim();
            if (part.isEmpty()) {
                continue;
            }

            if (builder.length() > 0) {
                builder.append(", ");
            }

            try {
                int lineNumber = Integer.parseInt(part);
                builder.append(escapeHtml(part)).append(" ").append(buildGoToLink(filePath, lineNumber));
            } catch (NumberFormatException ex) {
                builder.append(escapeHtml(part));
            }
        }

        return builder.toString();
    }

    private String resolveFilePath(String fileToken, List<File> availableFiles) {
        if (availableFiles != null) {
            for (File file : availableFiles) {
                String resolved = findMatchingFilePath(file, fileToken);
                if (resolved != null) {
                    return resolved;
                }
            }
        }

        return fileToken;
    }

    private String findMatchingFilePath(File file, String fileToken) {
        if (file == null || !file.exists()) {
            return null;
        }

        String normalizedToken = normalizePath(fileToken);

        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children == null) {
                return null;
            }

            for (File child : children) {
                String resolved = findMatchingFilePath(child, fileToken);
                if (resolved != null) {
                    return resolved;
                }
            }
            return null;
        }

        String path = file.getPath();
        String absolutePath = file.getAbsolutePath();
        String fileName = file.getName();

        if (path.equals(fileToken)
                || absolutePath.equals(fileToken)
                || fileName.equals(fileToken)
                || normalizePath(path).endsWith("/" + normalizedToken)
                || normalizePath(absolutePath).endsWith("/" + normalizedToken)) {
            return path;
        }

        return null;
    }

    private String normalizePath(String value) {
        return value.replace('\\', '/');
    }

    private String buildGoToLink(String filePath, int lineNumber) {
        return "<a href='" + LINK_PREFIX
                + FILE_PARAMETER + "=" + encode(filePath)
                + "&" + LINE_PARAMETER + "=" + encode(Integer.toString(lineNumber))
                + "'>Go to error</a>";
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String escapeHtml(String value) {
        return value
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}