package com.earthtrip;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

class ApiContractCompletenessTest {

    private static final Pattern PUBLIC_CONTRACT = Pattern.compile(
        "\\|\\s*((?:GET|POST|PUT|PATCH|DELETE)(?:/(?:GET|POST|PUT|PATCH|DELETE))*)"
            + "\\s*\\|\\s*`(/api/v1/[^`]+)`"
    );
    private static final Pattern INTERNAL_CONTRACT = Pattern.compile(
        "\\|\\s*(GET|POST)\\s*\\|\\s*`(/internal/[^`]+)`"
    );

    @Test
    void 기획문서의_공개_API와_내부_API와_WebSocket_계약을_모두_구현한다()
        throws IOException {
        Path workspace = workspaceRoot();
        Path planning = workspace.resolve("docs/planning");
        Set<Operation> expectedPublic = new LinkedHashSet<>();
        for (String name : List.of(
            "backend-api-data-ownership-v0.1.md",
            "backend-api-completeness-audit-v0.2.md"
        )) {
            expectedPublic.addAll(readOperations(planning.resolve(name), PUBLIC_CONTRACT, true));
        }
        Set<Operation> expectedInternal = readOperations(
            planning.resolve("backend-api-completeness-audit-v0.2.md"),
            INTERNAL_CONTRACT,
            false
        );
        Set<Operation> actual = controllerOperations();

        assertThat(expectedPublic)
            .as("공개 API 기획 계약 수")
            .hasSize(335);
        assertThat(actual)
            .as("구현되지 않은 공개 API 계약")
            .containsAll(expectedPublic);
        assertThat(actual)
            .as("구현되지 않은 내부 운영 API 계약")
            .containsAll(expectedInternal);

        Path realtime = workspace.resolve(
            "earth-trip-backend/modules/platform/src/main/java/com/earthtrip/platform/adapter/in/"
                + "realtime/TripRealtimeWebSocketConfiguration.java"
        );
        assertThat(Files.readString(realtime))
            .contains("/ws/v1/trips/{tripId}");
    }

    private static Set<Operation> controllerOperations() {
        Set<Operation> operations = new LinkedHashSet<>();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        new ClassFileImporter().importPackages("com.earthtrip").stream()
            .map(javaClass -> load(loader, javaClass.getName()))
            .filter(type -> AnnotatedElementUtils.hasAnnotation(type, RestController.class))
            .forEach(controller -> {
                RequestMapping controllerMapping = AnnotatedElementUtils.findMergedAnnotation(
                    controller,
                    RequestMapping.class
                );
                List<String> bases = paths(controllerMapping);
                for (Method method : controller.getDeclaredMethods()) {
                    RequestMapping methodMapping = AnnotatedElementUtils.findMergedAnnotation(
                        method,
                        RequestMapping.class
                    );
                    if (methodMapping == null) {
                        continue;
                    }
                    for (String base : bases) {
                        for (String suffix : paths(methodMapping)) {
                            String path = normalize(base, suffix);
                            for (RequestMethod verb : methodMapping.method()) {
                                operations.add(new Operation(verb.name(), path));
                            }
                        }
                    }
                }
            });
        return operations;
    }

    private static Set<Operation> readOperations(
        Path document,
        Pattern pattern,
        boolean splitMethods
    ) throws IOException {
        Set<Operation> operations = new LinkedHashSet<>();
        for (String line : Files.readAllLines(document)) {
            Matcher matcher = pattern.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String[] methods = splitMethods
                ? matcher.group(1).split("/")
                : new String[] {matcher.group(1)};
            for (String method : methods) {
                operations.add(new Operation(method, matcher.group(2)));
            }
        }
        return operations;
    }

    private static List<String> paths(RequestMapping mapping) {
        if (mapping == null) {
            return List.of("");
        }
        List<String> values = new ArrayList<>();
        values.addAll(Arrays.asList(mapping.path()));
        values.addAll(Arrays.asList(mapping.value()));
        values.removeIf(String::isEmpty);
        return values.isEmpty() ? List.of("") : List.copyOf(values);
    }

    private static String normalize(String base, String suffix) {
        String path = ("/" + base + "/" + suffix).replaceAll("/+", "/");
        return path.length() > 1 && path.endsWith("/")
            ? path.substring(0, path.length() - 1)
            : path;
    }

    private static Class<?> load(ClassLoader loader, String name) {
        try {
            return Class.forName(name, false, loader);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("API 계약 검사 중 클래스를 읽지 못했습니다: " + name, exception);
        }
    }

    private static Path workspaceRoot() {
        Path current = Path.of("").toAbsolutePath().normalize();
        for (Path candidate = current; candidate != null; candidate = candidate.getParent()) {
            if (Files.isDirectory(candidate.resolve("docs/planning"))
                && Files.isDirectory(candidate.resolve("earth-trip-backend"))) {
                return candidate;
            }
        }
        throw new IllegalStateException("Earth Trip workspace 루트를 찾을 수 없습니다.");
    }

    private record Operation(String method, String path) { }
}
