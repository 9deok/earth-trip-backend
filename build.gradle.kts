plugins {
    java
    id("io.spring.dependency-management") version "1.1.7"
}

group = "com.sunam"
version = "0.1"

allprojects {
    repositories {
        mavenCentral()
    }

    apply(plugin = "java")
    apply(plugin = "io.spring.dependency-management")

    java {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }

    dependencyManagement {
        imports {
            mavenBom("org.springframework.boot:spring-boot-dependencies:3.5.5")
        }
    }

    dependencies {
        compileOnly("org.projectlombok:lombok")
        annotationProcessor("org.projectlombok:lombok")

        testCompileOnly("org.projectlombok:lombok")
        testAnnotationProcessor("org.projectlombok:lombok")

        testImplementation("org.springframework.boot:spring-boot-starter-test")
        testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    }
}

// Spring Boot 애플리케이션은 서비스 모듈들에만 적용
configure(subprojects) {
    if (name.startsWith("service-")) {
        apply(plugin = "org.springframework.boot")
        
        dependencies {
            implementation("ch.qos.logback:logback-classic")
        }
    }
}
