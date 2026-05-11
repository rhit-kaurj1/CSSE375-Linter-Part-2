package domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class SnakeLinterAllowlistTest {

    @Test
    void checksOnlyMethodVariableAndParameterDeclarations(@TempDir Path tempDir) throws IOException {
        Path javaFile = tempDir.resolve("AllowlistExample.java");
        Files.writeString(javaFile, String.join(System.lineSeparator(),
                "public class BadTypeName {",
                "    private static final int MAX_PLAYERS = 10;",
                "    private int bad_field = 1;",
                "    public BadTypeName() {",
                "        JPanel panel = new JPanel();",
                "        throw new IllegalArgumentException(\"oops\");",
                "    }",
                "    void BadMethodName(String BadParam) {",
                "        int bad_local = 0;",
                "    }",
                "}"));

        SnakeLinter linter = new SnakeLinter();
        String result = linter.lint(List.of(javaFile.toFile()));

        assertTrue(result.contains("MethodDeclaration 'BadMethodName' does not follow camelCase"));
        assertTrue(result.contains("VariableDeclarator 'bad_field' does not follow camelCase"));
        assertTrue(result.contains("VariableDeclarator 'bad_local' does not follow camelCase"));
        assertTrue(result.contains("Parameter 'BadParam' does not follow camelCase"));

        assertFalse(result.contains("BadTypeName' does not follow camelCase"));
        assertFalse(result.contains("MAX_PLAYERS' does not follow camelCase"));
        assertFalse(result.contains("IllegalArgumentException' does not follow camelCase"));
        assertFalse(result.contains("JPanel' does not follow camelCase"));
    }
}
