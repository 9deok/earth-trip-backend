# Earth Trip Backend 작업 지침

## 기술 기준

- 모든 모듈은 Java 21 toolchain과 `--release 21`을 사용한다.
- 실행 애플리케이션은 Spring Boot 4.1.x를 사용한다.
- Gradle 빌드 스크립트는 Groovy DSL(`build.gradle`, `settings.gradle`)만 사용하고
  Kotlin DSL(`*.gradle.kts`)을 추가하지 않는다.
- 데이터베이스는 MariaDB이며 스키마 변경은 Flyway로만 관리한다.
- JPA의 `ddl-auto`는 `validate`로 유지한다.
- 동적 조회와 복합 조회는 QueryDSL을 기본으로 사용한다.

## 아키텍처

- 전체 시스템은 하나의 실행·배포 단위를 가진 모듈러 모놀리스다.
- 각 도메인 모듈은 헥사고날 아키텍처와 클린 아키텍처를 따른다.
- 의존 방향은 `adapter -> application -> domain`이다.
- `domain`은 Spring, JPA, QueryDSL, HTTP 등 프레임워크 타입을 참조하지 않는다.
- 인바운드 어댑터는 `application.port.in`의 유스케이스만 호출한다.
- 아웃바운드 어댑터는 `application.port.out`을 구현한다.
- 트랜잭션 경계는 Application Service에 둔다.
- 다른 모듈의 Entity, Repository, Q 클래스 또는 내부 구현체를 직접 참조하지 않는다.
- 모듈 간 협력은 공개 포트 또는 명시적인 도메인 이벤트를 사용한다.

## QueryDSL

- `JPAQueryFactory`는 `*QuerydslSupportImpl`에서만 직접 사용한다.
- `PersistenceAdapter`가 `JPAQueryFactory`를 직접 주입받지 않도록 한다.
- `QuerydslSupport`와 `QuerydslSupportImpl`을 같은 Aggregate의 persistence 패키지에 둔다.
- 다른 모듈의 Q 클래스를 import하지 않는다.
- 단순 저장은 Spring Data Repository를 사용하고, 조건 조회·조인·집계는 QueryDSL을 사용한다.

## 패키지와 코드

- HTTP 인바운드는 **최종 URL Path 하나당 패키지 하나**를 사용한다.
- `adapter.in.web` 뒤의 패키지 경로는 URL Path와 일치시킨다.
  - `/api/v1/trips` → `adapter.in.web.api.v1.trips`
  - `/api/v1/trips/{tripId}` → `adapter.in.web.api.v1.trips.by_trip_id`
  - URL의 `-`는 패키지에서 `_`로 표현한다.
- 같은 최종 Path에서 HTTP Method만 다른 API는 같은 패키지와 하나의 Controller에 둔다.
- 서로 다른 최종 Path를 하나의 Controller에 함께 넣지 않는다.
- 해당 Path의 `*Controller`, `*Request`, `*Response`는 모두 같은 패키지에 둔다.
- Controller, Request, Response와 그 메서드·생성자는 가능한 한 package-private로 둔다.
- 구현체는 기본적으로 package-private이며, 다른 패키지의 컴파일 타임 계약에 꼭 필요한
  Port, Command, Result, Domain 타입만 최소 범위로 공개한다.
- 테스트 편의를 위해 프로덕션 타입을 `public`으로 넓히지 않는다.
- Java에서 부모 패키지와 하위 패키지는 서로 다른 패키지라는 점을 고려한다.
- 수기 작성 파일은 가능하면 500줄을 넘기지 않는다.
- 공통 모듈에는 ID, 시간, 통화처럼 안정적인 값 타입만 둔다.

## SOLID 적용 기준

- **SRP**: Controller는 HTTP 변환과 검증, Application Service는 한 유스케이스 조정,
  Domain은 비즈니스 규칙, Adapter는 외부 기술 변환만 담당한다.
- **OCP**: 결제수단, 알림 채널처럼 실제 변경 축이 확인된 경우 Port와 Strategy로 확장한다.
  미래를 추측한 인터페이스나 계층은 만들지 않는다.
- **LSP**: Port 구현체는 Port가 약속한 입력 검증, 결과 의미, 오류 의미를 바꾸지 않는다.
- **ISP**: 하나의 거대한 Service/Repository 인터페이스 대신 유스케이스·조회 목적별로
  작은 Port를 둔다.
- **DIP**: Domain과 Application은 Adapter 구현체에 의존하지 않고 자기 모듈의 Port에
  의존한다.
- 의존성 주입은 생성자 주입만 사용하고 필드 주입은 금지한다.
- `switch`/`if`가 변경 축마다 계속 증가하거나 클래스의 변경 이유가 둘 이상이면
  Policy, Strategy, Port 분리를 검토한다.
- SOLID를 이유로 의미 없는 인터페이스, DTO 복사, 계층을 추가하지 않는다.

## 검증

- 변경 후 `./gradlew clean test`와 `./gradlew build`를 실행한다.
- Docker가 가능하면 MariaDB Testcontainers 통합 테스트도 확인한다.
- 비밀정보를 커밋하지 않으며 환경변수와 `.env.example`을 사용한다.
- `ApiPackageConventionTest`로 URL Path-패키지 일치, Path당 Controller 하나,
  Request/Response 동일 패키지, package-private 규칙을 검증한다.
- `CleanArchitectureTest`로 계층 의존 방향, 구현체 공개 범위, 필드 주입 금지를 검증한다.
- `ModularityTest`로 모듈 간 순환과 내부 구현 참조를 검증한다.
- 배포 전 로컬에서 같은 전체 검증을 실행하고, 실패한 상태를 배포하거나 병합하지 않는다.

## 새 API 완료 조건

- Path에 대응하는 전용 패키지를 만들었다.
- Controller, Request, Response를 같은 패키지에 두고 package-private로 선언했다.
- Controller는 `application.port.in`만 호출한다.
- 하나의 Application Service가 하나의 유스케이스와 트랜잭션 경계를 담당한다.
- 외부 기술 접근은 `application.port.out` 뒤의 Adapter에만 존재한다.
- 단위 테스트와 필요한 통합 테스트를 추가했다.
- 전체 아키텍처 테스트와 빌드가 통과한다.
