package domain;

import java.io.File;

import com.github.javaparser.ParseProblemException;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.FieldDeclaration;
import com.github.javaparser.ast.body.MethodDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.VariableDeclarator;
import com.github.javaparser.ast.expr.SimpleName;

/**
 * Linter that checks camelCase naming for a strict allowlist of declarations:
 * MethodDeclaration, VariableDeclarator, and Parameter.
 * Everything else is ignored by default.
 */
public class SnakeLinter extends AbstractSourceLinter {

    private static final String CAMEL_CASE_REGEX = "^[a-z][a-zA-Z0-9]*$";

    @Override
    protected void lintFile(File file, String content, StringBuilder violations) {
        try {
            CompilationUnit compilationUnit = StaticJavaParser.parse(content);

            for (MethodDeclaration method : compilationUnit.findAll(MethodDeclaration.class)) {
                checkCamelCase(file, "MethodDeclaration", method.getName(), method.getNameAsString(), violations);
            }

            for (VariableDeclarator variable : compilationUnit.findAll(VariableDeclarator.class)) {
                if (isStaticFinalField(variable)) {
                    continue;
                }
                checkCamelCase(file, "VariableDeclarator", variable.getName(), variable.getNameAsString(), violations);
            }

            for (Parameter parameter : compilationUnit.findAll(Parameter.class)) {
                checkCamelCase(file, "Parameter", parameter.getName(), parameter.getNameAsString(), violations);
            }
        } catch (ParseProblemException ex) {
            // Ignore unparsable files so naming checks stay non-blocking.
        }
    }

    private void checkCamelCase(File file, String declarationType, SimpleName nameNode, String identifier,
            StringBuilder violations) {
        if (identifier.matches(CAMEL_CASE_REGEX)) {
            return;
        }

        int line = nameNode.getBegin().map(position -> position.line).orElse(1);
        violations.append(file.getName()).append(":").append(line)
                .append(" - ").append(declarationType).append(" '").append(identifier)
                .append("' does not follow camelCase\n");
    }

    private boolean isStaticFinalField(VariableDeclarator variable) {
        return variable.findAncestor(FieldDeclaration.class)
                .map(field -> field.isStatic() && field.isFinal())
                .orElse(false);
    }
}