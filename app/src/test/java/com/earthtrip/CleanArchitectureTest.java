package com.earthtrip;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

import com.querydsl.core.annotations.Generated;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ArchRule;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.transaction.annotation.Transactional;

@AnalyzeClasses(packages = "com.earthtrip")
class CleanArchitectureTest {

    @ArchTest
    static final ArchRule DOMAIN_IS_FRAMEWORK_INDEPENDENT =
            noClasses()
                    .that()
                    .resideInAPackage("..domain..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "com.querydsl..",
                            "com.fasterxml.jackson..",
                            "org.apache.pdfbox..",
                            "software.amazon.awssdk..",
                            "..application..",
                            "..adapter..");

    @ArchTest
    static final ArchRule APPLICATION_DOES_NOT_DEPEND_ON_ADAPTERS =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..adapter..");

    @ArchTest
    static final ArchRule APPLICATION_DOES_NOT_DEPEND_ON_JACKSON =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.fasterxml.jackson..");

    @ArchTest
    static final ArchRule APPLICATION_DOES_NOT_DEPEND_ON_INFRASTRUCTURE_LIBRARIES =
            noClasses()
                    .that()
                    .resideInAPackage("..application..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "jakarta.persistence..",
                            "com.querydsl..",
                            "org.apache.pdfbox..",
                            "software.amazon.awssdk..",
                            "org.mariadb..",
                            "org.springframework.data..",
                            "org.springframework.web.client..");

    @ArchTest
    static final ArchRule SHARED_KERNEL_IS_FRAMEWORK_AND_MODULE_INDEPENDENT =
            noClasses()
                    .that()
                    .resideInAPackage("com.earthtrip.sharedkernel..")
                    .and()
                    .doNotHaveSimpleName("package-info")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage(
                            "org.springframework..",
                            "jakarta.persistence..",
                            "com.querydsl..",
                            "com.fasterxml.jackson..",
                            "org.apache.pdfbox..",
                            "software.amazon.awssdk..",
                            "com.earthtrip.expense..",
                            "com.earthtrip.identity..",
                            "com.earthtrip.notification..",
                            "com.earthtrip.planning..",
                            "com.earthtrip.platform..",
                            "com.earthtrip.trip..",
                            "com.earthtrip.wallet..");

    @ArchTest
    static final ArchRule INBOUND_DOES_NOT_DEPEND_ON_OUTBOUND =
            noClasses()
                    .that()
                    .resideInAPackage("..adapter.in..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..adapter.out..");

    @ArchTest
    static final ArchRule INBOUND_DOES_NOT_DEPEND_ON_DOMAIN =
            noClasses()
                    .that()
                    .resideInAPackage("..adapter.in..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("..domain..");

    @ArchTest
    static final ArchRule INBOUND_DOES_NOT_DEPEND_ON_OUTPUT_PORTS_OR_SERVICES =
            noClasses()
                    .that()
                    .resideInAPackage("..adapter.in..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..application.port.out..", "..application.service..");

    @ArchTest
    static final ArchRule INBOUND_INJECTS_COLLABORATORS_THROUGH_LOCAL_INPUT_PORTS =
            classes()
                    .that()
                    .resideInAPackage("..adapter.in..")
                    .should(injectCollaboratorsThroughLocalInputPorts());

    @ArchTest
    static final ArchRule OUTBOUND_DOES_NOT_DEPEND_ON_INBOUND_OR_SERVICES =
            noClasses()
                    .that()
                    .resideInAPackage("..adapter.out..")
                    .should()
                    .dependOnClassesThat()
                    .resideInAnyPackage("..adapter.in..", "..application.service..");

    @ArchTest
    static final ArchRule QUERYDSL_STAYS_IN_PERSISTENCE_SUPPORT =
            noClasses()
                    .that()
                    .areNotAnnotatedWith(Generated.class)
                    .and()
                    .resideOutsideOfPackage("com.earthtrip")
                    .and()
                    .haveSimpleNameNotEndingWith("QuerydslSupportImpl")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.querydsl..");

    @ArchTest
    static final ArchRule READ_QUERIES_USE_QUERYDSL_SUPPORT =
            noMethods()
                    .that()
                    .areDeclaredInClassesThat()
                    .haveSimpleNameEndingWith("JpaRepository")
                    .and()
                    .areNotAnnotatedWith(Modifying.class)
                    .should()
                    .beAnnotatedWith(Query.class);

    @ArchTest
    static final ArchRule OUTBOUND_ADAPTERS_IMPLEMENT_LOCAL_OUTPUT_PORTS =
            classes()
                    .that()
                    .resideInAPackage("..adapter.out..")
                    .and()
                    .haveSimpleNameEndingWith("Adapter")
                    .should(implementLocalOutputPort());

    @ArchTest
    static final ArchRule API_CONTRACTS_ARE_IMPLEMENTED_BY_THEIR_OWNING_MODULE =
            classes()
                    .that()
                    .resideInAPackage("..api")
                    .and()
                    .areInterfaces()
                    .and()
                    .doNotHaveSimpleName("package-info")
                    .should(beImplementedByOwningModule(true));

    @ArchTest
    static final ArchRule SPI_CONTRACTS_ARE_IMPLEMENTED_OUTSIDE_THEIR_OWNING_MODULE =
            classes()
                    .that()
                    .resideInAPackage("..spi")
                    .and()
                    .areInterfaces()
                    .and()
                    .doNotHaveSimpleName("package-info")
                    .should(beImplementedByOwningModule(false));

    @ArchTest
    static final ArchRule IMPLEMENTATION_TYPES_ARE_PACKAGE_PRIVATE =
            noClasses()
                    .that()
                    .resideInAnyPackage("..adapter.in..", "..application.service..")
                    .should()
                    .bePublic();

    @ArchTest
    static final ArchRule PERSISTENCE_IMPLEMENTATIONS_ARE_PACKAGE_PRIVATE =
            noClasses()
                    .that()
                    .haveSimpleNameEndingWith("PersistenceAdapter")
                    .or()
                    .haveSimpleNameEndingWith("JpaEntity")
                    .or()
                    .haveSimpleNameEndingWith("JpaRepository")
                    .or()
                    .haveSimpleNameEndingWith("QuerydslSupport")
                    .or()
                    .haveSimpleNameEndingWith("QuerydslSupportImpl")
                    .and()
                    .areNotAnnotatedWith(Generated.class)
                    .and()
                    .areTopLevelClasses()
                    .should()
                    .bePublic();

    @ArchTest
    static final ArchRule OUTBOUND_IMPLEMENTATION_TYPES_ARE_PACKAGE_PRIVATE =
            noClasses()
                    .that()
                    .resideInAPackage("..adapter.out..")
                    .and()
                    .areNotAnnotatedWith(Generated.class)
                    .and()
                    .areTopLevelClasses()
                    .should()
                    .bePublic();

    @ArchTest
    static final ArchRule FIELD_INJECTION_IS_FORBIDDEN =
            noFields().should().beAnnotatedWith(Autowired.class);

    @ArchTest
    static final ArchRule TRANSACTIONAL_TYPES_STAY_IN_APPLICATION_SERVICES =
            noClasses()
                    .that()
                    .resideOutsideOfPackage("..application.service..")
                    .should()
                    .beAnnotatedWith(Transactional.class);

    @ArchTest
    static final ArchRule TRANSACTIONAL_METHODS_STAY_IN_APPLICATION_SERVICES =
            noMethods()
                    .that()
                    .areDeclaredInClassesThat()
                    .resideOutsideOfPackage("..application.service..")
                    .should()
                    .beAnnotatedWith(Transactional.class);

    private static ArchCondition<JavaClass> injectCollaboratorsThroughLocalInputPorts() {
        return new ArchCondition<>("inject collaborators through the owning module's input ports") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                item.getFields()
                        .forEach(
                                field -> {
                                    JavaClass type = field.getRawType();
                                    String packageName = type.getPackageName();
                                    boolean moduleContract =
                                            packageName.matches(
                                                    "com\\.earthtrip\\.[^.]+\\.(api|spi)");
                                    boolean internalApplicationType =
                                            packageName.contains(".application.port.out")
                                                    || packageName.contains(".application.service");
                                    boolean foreignInputPort =
                                            packageName.contains(".application.port.in")
                                                    && !moduleOf(item).equals(moduleOf(type));
                                    boolean allowed =
                                            !moduleContract
                                                    && !internalApplicationType
                                                    && !foreignInputPort;
                                    events.add(
                                            new SimpleConditionEvent(
                                                    field,
                                                    allowed,
                                                    field.getFullName()
                                                            + " must inject an input port owned by "
                                                            + moduleOf(item)));
                                });
            }
        };
    }

    private static ArchCondition<JavaClass> implementLocalOutputPort() {
        return new ArchCondition<>("implement an output port owned by the same module") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied =
                        item.getAllRawInterfaces().stream()
                                .anyMatch(
                                        type ->
                                                type.getPackageName()
                                                                .contains(".application.port.out")
                                                        && moduleOf(item).equals(moduleOf(type)));
                events.add(
                        new SimpleConditionEvent(
                                item,
                                satisfied,
                                item.getName()
                                        + " must implement an application.port.out contract owned by "
                                        + moduleOf(item)));
            }
        };
    }

    private static ArchCondition<JavaClass> beImplementedByOwningModule(boolean sameModule) {
        String expectation = sameModule ? "inside" : "outside";
        return new ArchCondition<>("be implemented " + expectation + " the owning module") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean satisfied =
                        item.getAllSubclasses().stream()
                                .anyMatch(
                                        implementation ->
                                                sameModule
                                                        == moduleOf(item)
                                                                .equals(moduleOf(implementation)));
                events.add(
                        new SimpleConditionEvent(
                                item,
                                satisfied,
                                item.getName()
                                        + " must be implemented "
                                        + expectation
                                        + " module "
                                        + moduleOf(item)));
            }
        };
    }

    private static String moduleOf(JavaClass type) {
        String[] segments = type.getPackageName().split("\\.");
        return segments.length > 2 ? segments[2] : "root";
    }
}
