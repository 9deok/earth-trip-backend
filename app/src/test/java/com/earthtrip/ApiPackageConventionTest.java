package com.earthtrip;

import static org.assertj.core.api.Assertions.assertThat;

import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

class ApiPackageConventionTest {

    private static final String BASE_PACKAGE = "com.earthtrip";
    private static final String WEB_PACKAGE_MARKER = ".adapter.in.web.";

    @Test
    void URL_Path마다_전용_패키지와_하나의_Controller를_사용한다() {
        List<Class<?>> webTypes = importWebTypes();
        List<Class<?>> controllers =
                webTypes.stream()
                        .filter(
                                type ->
                                        AnnotatedElementUtils.hasAnnotation(
                                                type, RestController.class))
                        .toList();

        assertThat(controllers).as("최소 하나의 @RestController가 있어야 한다").isNotEmpty();

        Map<String, List<Class<?>>> controllersByPackage =
                controllers.stream().collect(Collectors.groupingBy(Class::getPackageName));

        controllersByPackage.forEach(
                (packageName, packageControllers) ->
                        assertThat(packageControllers)
                                .as("%s에는 Controller가 하나만 있어야 한다", packageName)
                                .hasSize(1));

        Map<String, Set<String>> packagesByPath = new java.util.LinkedHashMap<>();

        controllers.forEach(
                controller -> {
                    Set<String> paths = mappedPaths(controller);

                    assertThat(paths)
                            .as("%s는 하나의 최종 URL Path만 처리해야 한다", controller.getName())
                            .hasSize(1);

                    String path = paths.iterator().next();
                    String packagePath = packagePathOf(controller);

                    assertThat(packagePath)
                            .as("%s의 패키지는 URL Path %s와 일치해야 한다", controller.getName(), path)
                            .isEqualTo(toPackagePath(path));

                    packagesByPath
                            .computeIfAbsent(path, ignored -> new LinkedHashSet<>())
                            .add(controller.getPackageName());

                    mappedMethods(controller)
                            .forEach(
                                    method ->
                                            assertTransportTypesStayWithController(
                                                    controller, method));
                });

        packagesByPath.forEach(
                (path, packageNames) ->
                        assertThat(packageNames)
                                .as("URL Path %s는 하나의 패키지에만 있어야 한다", path)
                                .hasSize(1));

        webTypes.stream()
                .filter(ApiPackageConventionTest::isTransportType)
                .forEach(
                        transportType -> {
                            assertThat(controllersByPackage)
                                    .as(
                                            "%s는 같은 패키지의 Controller와 함께 있어야 한다",
                                            transportType.getName())
                                    .containsKey(transportType.getPackageName());
                            assertPackagePrivateTypeAndConstructors(transportType);
                        });

        controllers.forEach(
                controller -> {
                    assertPackagePrivateTypeAndConstructors(controller);
                    mappedMethods(controller)
                            .forEach(
                                    method ->
                                            assertThat(
                                                            Modifier.isPublic(method.getModifiers())
                                                                    || Modifier.isProtected(
                                                                            method.getModifiers()))
                                                    .as(
                                                            "%s의 HTTP 매핑 메서드는 package-private여야 한다",
                                                            method)
                                                    .isFalse());
                });
    }

    private static List<Class<?>> importWebTypes() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        return new ClassFileImporter()
                .importPackages(BASE_PACKAGE).stream()
                        .filter(
                                javaClass ->
                                        javaClass.getPackageName().contains(WEB_PACKAGE_MARKER))
                        .map(JavaClass::getName)
                        .<Class<?>>map(className -> loadClass(classLoader, className))
                        .toList();
    }

    private static Class<?> loadClass(ClassLoader classLoader, String className) {
        try {
            return Class.forName(className, false, classLoader);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("아키텍처 검증 중 클래스를 불러오지 못했습니다: " + className, exception);
        }
    }

    private static Set<String> mappedPaths(Class<?> controller) {
        List<String> controllerPaths =
                mappingPaths(
                        AnnotatedElementUtils.findMergedAnnotation(
                                controller, RequestMapping.class));

        return mappedMethods(controller)
                .flatMap(
                        method -> {
                            RequestMapping mapping =
                                    AnnotatedElementUtils.findMergedAnnotation(
                                            method, RequestMapping.class);
                            return mappingPaths(mapping).stream();
                        })
                .flatMap(
                        methodPath ->
                                controllerPaths.stream()
                                        .map(
                                                controllerPath ->
                                                        normalizePath(controllerPath, methodPath)))
                .collect(Collectors.toCollection(TreeSet::new));
    }

    private static Stream<Method> mappedMethods(Class<?> controller) {
        return Arrays.stream(controller.getDeclaredMethods())
                .filter(
                        method ->
                                AnnotatedElementUtils.findMergedAnnotation(
                                                method, RequestMapping.class)
                                        != null);
    }

    private static List<String> mappingPaths(RequestMapping mapping) {
        if (mapping == null) {
            return List.of("");
        }

        Set<String> paths = new LinkedHashSet<>();
        paths.addAll(Arrays.asList(mapping.path()));
        paths.addAll(Arrays.asList(mapping.value()));
        paths.remove("");

        return paths.isEmpty() ? List.of("") : List.copyOf(paths);
    }

    private static String normalizePath(String controllerPath, String methodPath) {
        String combined = "/" + controllerPath + "/" + methodPath;
        String normalized = combined.replaceAll("/+", "/");

        if (normalized.length() > 1 && normalized.endsWith("/")) {
            return normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private static String packagePathOf(Class<?> controller) {
        String packageName = controller.getPackageName();
        int markerIndex = packageName.indexOf(WEB_PACKAGE_MARKER);

        assertThat(markerIndex)
                .as("%s는 adapter.in.web 하위에 있어야 한다", controller.getName())
                .isGreaterThanOrEqualTo(0);

        return packageName.substring(markerIndex + WEB_PACKAGE_MARKER.length());
    }

    private static String toPackagePath(String path) {
        String packagePath =
                Arrays.stream(path.split("/"))
                        .filter(segment -> !segment.isBlank())
                        .map(ApiPackageConventionTest::toPackageSegment)
                        .collect(Collectors.joining("."));

        return packagePath.isBlank() ? "root" : packagePath;
    }

    private static String toPackageSegment(String pathSegment) {
        if (pathSegment.startsWith("{") && pathSegment.endsWith("}")) {
            String variable = pathSegment.substring(1, pathSegment.length() - 1).split(":", 2)[0];
            return "by_" + camelToSnakeCase(variable);
        }

        String normalized =
                pathSegment
                        .replace('-', '_')
                        .replaceAll("[^A-Za-z0-9_]", "_")
                        .toLowerCase(Locale.ROOT);

        return Character.isDigit(normalized.charAt(0)) ? "_" + normalized : normalized;
    }

    private static String camelToSnakeCase(String value) {
        return value.replaceAll("([a-z0-9])([A-Z])", "$1_$2")
                .replace('-', '_')
                .toLowerCase(Locale.ROOT);
    }

    private static void assertTransportTypesStayWithController(Class<?> controller, Method method) {
        List<Type> signatureTypes =
                new ArrayList<>(Arrays.asList(method.getGenericParameterTypes()));
        signatureTypes.add(method.getGenericReturnType());

        signatureTypes.stream()
                .flatMap(ApiPackageConventionTest::classesInside)
                .filter(ApiPackageConventionTest::isTransportType)
                .forEach(
                        transportType ->
                                assertThat(transportType.getPackageName())
                                        .as(
                                                "%s가 사용하는 %s는 Controller와 같은 패키지에 있어야 한다",
                                                method, transportType.getName())
                                        .isEqualTo(controller.getPackageName()));
    }

    private static Stream<Class<?>> classesInside(Type type) {
        if (type instanceof Class<?> typeClass) {
            return Stream.of(typeClass);
        }
        if (type instanceof ParameterizedType parameterizedType) {
            return Stream.concat(
                    classesInside(parameterizedType.getRawType()),
                    Arrays.stream(parameterizedType.getActualTypeArguments())
                            .flatMap(ApiPackageConventionTest::classesInside));
        }
        if (type instanceof GenericArrayType arrayType) {
            return classesInside(arrayType.getGenericComponentType());
        }
        if (type instanceof WildcardType wildcardType) {
            return Stream.concat(
                    Arrays.stream(wildcardType.getUpperBounds())
                            .flatMap(ApiPackageConventionTest::classesInside),
                    Arrays.stream(wildcardType.getLowerBounds())
                            .flatMap(ApiPackageConventionTest::classesInside));
        }
        if (type instanceof TypeVariable<?> typeVariable) {
            return Arrays.stream(typeVariable.getBounds())
                    .flatMap(ApiPackageConventionTest::classesInside);
        }
        return Stream.empty();
    }

    private static boolean isTransportType(Class<?> type) {
        return type.getPackageName().contains(WEB_PACKAGE_MARKER)
                && (type.getSimpleName().endsWith("Request")
                        || type.getSimpleName().endsWith("Response"));
    }

    private static void assertPackagePrivateTypeAndConstructors(Class<?> type) {
        assertThat(
                        Modifier.isPublic(type.getModifiers())
                                || Modifier.isProtected(type.getModifiers()))
                .as("%s는 package-private여야 한다", type.getName())
                .isFalse();

        Arrays.stream(type.getDeclaredConstructors())
                .forEach(
                        constructor ->
                                assertThat(
                                                Modifier.isPublic(constructor.getModifiers())
                                                        || Modifier.isProtected(
                                                                constructor.getModifiers()))
                                        .as("%s의 생성자는 package-private 또는 private여야 한다", constructor)
                                        .isFalse());
    }
}
