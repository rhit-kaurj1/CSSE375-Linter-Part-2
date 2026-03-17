package presentation;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import domain.Linter;
import domain.LinterConfig;
import domain.SnakeLinter;
import domain.TooManyParametersLinter;
import testsupport.JavaClassFixtureCompiler;

class LinterFactoryTest {

    @Test
    void createsOnlyEnabledLinters() {
        LinterConfig config = new LinterConfig(Set.of("SnakeLinter", "TooManyParametersLinter"), 4, 3);

        List<Linter> linters = new LinterFactory().createLinters(config);

        assertTrue(linters.stream().anyMatch(SnakeLinter.class::isInstance));
        assertTrue(linters.stream().anyMatch(TooManyParametersLinter.class::isInstance));
        assertFalse(linters.stream().anyMatch(domain.SingletonPatternLinter.class::isInstance));
        assertFalse(linters.stream().anyMatch(domain.SRPLinter.class::isInstance));
    }

    @Test
    void buildsTooManyParametersLinterUsingConfiguredThreshold(@TempDir Path tempDir) throws IOException {
        JavaClassFixtureCompiler.compileTo(tempDir, Map.of(
                "fixtures.params.ConfiguredService",
                "package fixtures.params;\n"
                        + "public class ConfiguredService {\n"
                        + "    public void borderline(int a, int b, int c) {}\n"
                        + "}\n"));

        Path classFile = tempDir.resolve("fixtures/params/ConfiguredService.class");

        List<Linter> strictLinters = new LinterFactory()
                .createLinters(new LinterConfig(Set.of("TooManyParametersLinter"), 2, 2));
        String strictResult = strictLinters.stream()
                .filter(TooManyParametersLinter.class::isInstance)
                .findFirst()
                .orElseThrow()
                .lint(List.of(classFile.toFile()));
        assertTrue(strictResult.contains("Total too-many-parameters issues: 1"));

        List<Linter> relaxedLinters = new LinterFactory()
                .createLinters(new LinterConfig(Set.of("TooManyParametersLinter"), 3, 2));
        String relaxedResult = relaxedLinters.stream()
                .filter(TooManyParametersLinter.class::isInstance)
                .findFirst()
                .orElseThrow()
                .lint(List.of(classFile.toFile()));
        assertTrue(relaxedResult.contains("No too-many-parameters issues found."));
    }

    @Test
    void defaultConfigCreatesBroadCoverageCatalog() {
        List<String> linterNames = new LinterFactory().createLinters(LinterConfig.defaultConfig()).stream()
                .map(linter -> linter.getClass().getSimpleName())
                .collect(Collectors.toList());

        assertTrue(linterNames.contains("SnakeLinter"));
        assertTrue(linterNames.contains("TrailingWhitespaceLinter"));
        assertTrue(linterNames.contains("TooManyParametersLinter"));
        assertTrue(linterNames.contains("DesignRiskLinter"));
    }
}
