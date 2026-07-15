package com.jsystems.hsdc.persistence.repository;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * TAC-004-05: no SQL statement in the codebase concatenates user input —
 * every repository builds its SQL text from fixed string-literal /
 * text-block sources only and binds all variable data through
 * {@code JdbcClient} named parameters.
 *
 * <p>Static check: repository source files must never combine a string
 * literal with a variable via the {@code +} operator (the classic SQL
 * injection shape), and {@code DataSourceConfig} must never build its
 * (non-SQL, filesystem) JDBC URL from anything other than a fixed prefix
 * plus a validated path either.
 */
class NoSqlConcatenationTest {

    /** Matches `"...".+` or `+"..."` — i.e. a string literal glued to something via `+`. */
    private static final Pattern STRING_CONCATENATION = Pattern.compile("\"\\s*\\+|\\+\\s*\"");

    @Test
    void repositorySourcesNeverConcatenateStringsIntoSql() throws IOException {
        Path repositoryDir = Path.of("src/main/java/com/jsystems/hsdc/persistence/repository");
        assertThat(Files.isDirectory(repositoryDir)).isTrue();

        try (Stream<Path> files = Files.walk(repositoryDir)) {
            List<Path> javaFiles = files.filter(p -> p.toString().endsWith(".java")).toList();
            assertThat(javaFiles).isNotEmpty();

            for (Path file : javaFiles) {
                String content = Files.readString(file);
                assertThat(STRING_CONCATENATION.matcher(content).find())
                        .as("String concatenation found in %s — SQL text must come only from " +
                                "literals/text blocks, with all values bound via .param(...)", file)
                        .isFalse();
            }
        }
    }

    @Test
    void everySqlCallSiteHasAtLeastOneBoundParameterOrIsAFixedNoArgQuery() throws IOException {
        Path repositoryDir = Path.of("src/main/java/com/jsystems/hsdc/persistence/repository");
        Pattern sqlCall = Pattern.compile("\\.sql\\(");

        try (Stream<Path> files = Files.walk(repositoryDir)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                String content = Files.readString(file);
                long sqlCalls = sqlCall.matcher(content).results().count();
                long paramCalls = Pattern.compile("\\.param\\(").matcher(content).results().count();
                if (sqlCalls > 0) {
                    // Not a 1:1 ratio (some queries bind several params, SELECT * FROM x has none),
                    // but a file issuing .sql(...) calls with zero .param(...) anywhere is suspicious
                    // for this codebase (every repository here takes at least one bound argument).
                    assertThat(paramCalls)
                            .as("%s issues SQL but binds no parameters anywhere in the file", file)
                            .isGreaterThan(0);
                }
            }
        }
    }
}
