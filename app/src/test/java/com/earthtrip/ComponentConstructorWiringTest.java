package com.earthtrip;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Constructor;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.ClassPathScanningCandidateComponentProvider;
import org.springframework.core.type.filter.AnnotationTypeFilter;
import org.springframework.stereotype.Component;

class ComponentConstructorWiringTest {

    @Test
    void 여러_생성자를_가진_컴포넌트는_주입_생성자를_명시한다() {
        var scanner = new ClassPathScanningCandidateComponentProvider(false);
        scanner.addIncludeFilter(new AnnotationTypeFilter(Component.class));

        List<String> ambiguousComponents =
                scanner.findCandidateComponents("com.earthtrip").stream()
                        .map(definition -> definition.getBeanClassName())
                        .filter(name -> name != null)
                        .map(ComponentConstructorWiringTest::loadClass)
                        .filter(
                                type -> {
                                    Constructor<?>[] constructors = type.getDeclaredConstructors();
                                    return constructors.length > 1
                                            && java.util.Arrays.stream(constructors)
                                                    .noneMatch(
                                                            constructor ->
                                                                    constructor.getParameterCount()
                                                                            == 0)
                                            && java.util.Arrays.stream(constructors)
                                                    .noneMatch(
                                                            constructor ->
                                                                    constructor.isAnnotationPresent(
                                                                            Autowired.class));
                                })
                        .map(Class::getName)
                        .sorted(Comparator.naturalOrder())
                        .toList();

        assertThat(ambiguousComponents).as("기본 생성자가 없는 다중 생성자 Spring 컴포넌트").isEmpty();
    }

    private static Class<?> loadClass(String name) {
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException exception) {
            throw new IllegalStateException("컴포넌트 클래스를 불러올 수 없습니다: " + name, exception);
        }
    }
}
