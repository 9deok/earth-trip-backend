package com.earthtrip;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noFields;

import com.querydsl.core.annotations.Generated;
import com.tngtech.archunit.junit.AnalyzeClasses;
import com.tngtech.archunit.junit.ArchTest;
import com.tngtech.archunit.lang.ArchRule;
import org.springframework.beans.factory.annotation.Autowired;

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
                    .resideOutsideOfPackages("..adapter.out.persistence..", "com.earthtrip")
                    .should()
                    .dependOnClassesThat()
                    .resideInAPackage("com.querydsl..");

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
                    .should()
                    .bePublic();

    @ArchTest
    static final ArchRule FIELD_INJECTION_IS_FORBIDDEN =
            noFields().should().beAnnotatedWith(Autowired.class);
}
