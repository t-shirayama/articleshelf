package com.articleshelf.architecture;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class CleanArchitectureDependencyTest {
    private static final Path MAIN_SOURCE_ROOT = Path.of("src/main/java");
    private static final Pattern IMPORT_PATTERN = Pattern.compile("^import\\s+(?:static\\s+)?([^;]+);$");

    @Test
    void domainDoesNotDependOnOuterLayersOrFrameworks() throws IOException {
        List<String> violations = importsUnder("com.articleshelf.domain").stream()
                .filter(importLine -> importLine.importedPackage().startsWith("com.articleshelf.application.")
                        || importLine.importedPackage().startsWith("com.articleshelf.adapter.")
                        || importLine.importedPackage().startsWith("com.articleshelf.infrastructure.")
                        || importLine.importedPackage().startsWith("com.articleshelf.config.")
                        || importLine.importedPackage().startsWith("org.springframework.")
                        || importLine.importedPackage().startsWith("jakarta."))
                .map(ImportLine::format)
                .toList();

        assertThat(violations)
                .as("domain must stay framework-free and independent from application/adapter/infrastructure/config")
                .isEmpty();
    }

    @Test
    void applicationDoesNotDependOnOuterLayers() throws IOException {
        List<String> violations = importsUnder("com.articleshelf.application").stream()
                .filter(importLine -> importLine.importedPackage().startsWith("com.articleshelf.adapter.")
                        || importLine.importedPackage().startsWith("com.articleshelf.infrastructure.")
                        || importLine.importedPackage().startsWith("com.articleshelf.config."))
                .map(ImportLine::format)
                .toList();

        assertThat(violations)
                .as("application must use ports instead of depending on adapter/infrastructure/config")
                .isEmpty();
    }

    @Test
    void adapterDoesNotDependOnInfrastructureOrConfig() throws IOException {
        List<String> violations = importsUnder("com.articleshelf.adapter").stream()
                .filter(importLine -> importLine.importedPackage().startsWith("com.articleshelf.infrastructure.")
                        || importLine.importedPackage().startsWith("com.articleshelf.config."))
                .map(ImportLine::format)
                .toList();

        assertThat(violations)
                .as("adapter must delegate to application instead of reaching into infrastructure/config")
                .isEmpty();
    }

    @Test
    void infrastructureDoesNotDependOnAdapter() throws IOException {
        List<String> violations = importsUnder("com.articleshelf.infrastructure").stream()
                .filter(importLine -> importLine.importedPackage().startsWith("com.articleshelf.adapter."))
                .map(ImportLine::format)
                .toList();

        assertThat(violations)
                .as("infrastructure must not depend on web adapters")
                .isEmpty();
    }

    private List<ImportLine> importsUnder(String packageName) throws IOException {
        Path packageRoot = MAIN_SOURCE_ROOT.resolve(packageName.replace('.', '/'));
        if (!Files.exists(packageRoot)) {
            return List.of();
        }

        List<ImportLine> imports = new ArrayList<>();
        try (Stream<Path> files = Files.walk(packageRoot)) {
            for (Path path : files.filter(path -> path.toString().endsWith(".java")).toList()) {
                List<String> lines = Files.readAllLines(path);
                for (int index = 0; index < lines.size(); index += 1) {
                    Matcher matcher = IMPORT_PATTERN.matcher(lines.get(index).trim());
                    if (matcher.matches()) {
                        imports.add(new ImportLine(path, index + 1, matcher.group(1)));
                    }
                }
            }
        }
        return imports;
    }

    private record ImportLine(Path path, int line, String importedPackage) {
        private String format() {
            return path + ":" + line + " imports " + importedPackage;
        }
    }
}
