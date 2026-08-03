# Earth Trip Backend

Earth Trip 백엔드는 하나의 실행·배포 단위를 유지하면서 도메인 경계를 강제하는
모듈러 모놀리스입니다.

## 기술 스택

- Java 21
- Spring Boot 4.1.0
- Gradle 8.14.3 (Groovy DSL)
- MariaDB 11.8 LTS
- Spring Data JPA
- OpenFeign QueryDSL 7.5
- Flyway
- Spring Modulith
- ArchUnit
- Testcontainers
- Apache PDFBox 3.0.8

## 모듈

| 모듈 | 책임 |
| --- | --- |
| `app` | Spring Boot 실행과 의존성 조립 |
| `shared-kernel` | 안정적인 공통 값 타입 |
| `identity` | 계정, 인증, 초대, 참여 권한 |
| `trip` | 여행과 여행 생명주기 |
| `planning` | 장소 후보, 일정, 투표, 댓글, 좋아요 |
| `expense` | 지출, 분담, 환불, 정산 |
| `wallet` | 예약정보, 티켓, 문서 메타데이터와 권한 |
| `notification` | 알림 요청과 발송 정책 |
| `platform` | 파일, 외부 연동, 검색·공유·내보내기, 내부 운영 지원 |

각 도메인 모듈은 다음 의존 방향을 따릅니다.

```text
adapter/in ──> application/port/in <── application/service
                                             │
                                             v
                                           domain
                                             ^
                                             │
adapter/out ─> application/port/out <────────┘
```

HTTP API는 최종 URL Path 하나당 패키지 하나를 사용합니다. 예를 들어
`/api/v1/trips`의 Controller, Request, Response는 모두
`adapter.in.web.api.v1.trips` 패키지에 위치하며 가능한 한 package-private로
선언합니다. Path·패키지 대응과 접근 제한자는 테스트가 자동으로 검증합니다.

## 로컬 실행

```bash
cp .env.example .env
docker compose up -d mariadb
./gradlew :app:bootRun
```

Google 로그인을 사용하려면 `.env`의 `GOOGLE_OAUTH_CLIENT_IDS`에 Google Cloud에서 만든
backend용 **웹 애플리케이션 OAuth 클라이언트 ID**를 넣습니다. Flutter의
`GOOGLE_SERVER_CLIENT_ID`와 같은 값이어야 하며 client secret은 사용하지 않습니다.
백엔드는 Google JWKS로 ID token의 RS256 서명, issuer, audience, 시간, subject와
`email_verified`를 검증한 뒤에만 Earth Trip access/refresh token을 발급합니다.

상태와 인증된 API 확인:

```bash
curl http://localhost:8080/actuator/health
curl -X POST http://localhost:8080/api/v1/trips \
  -H 'Authorization: Bearer <access-token>' \
  -H 'Content-Type: application/json' \
  -d '{"requestId":"63ff50e2-7b97-47cc-ac0e-a4bacb6001a3","title":"첫 여행"}'
```

## 검증

```bash
./gradlew clean test
./gradlew build
```

Docker를 사용할 수 있으면 `MariaDbApplicationTest`가 실제 MariaDB 11.8 컨테이너로
Flyway V1~V28, Hibernate schema validation, 전체 Repository 생성, JPA version,
QueryDSL, 내부 운영 작업과 보안 필터를 함께 검증합니다. Docker가 없으면 해당 테스트만
건너뛰고 아키텍처 및 단위 테스트는 계속 실행됩니다.

`ApiContractCompletenessTest`는 기획 문서의 공개 API 335개와 내부 운영 API 11개,
WebSocket 경로가 코드에서 빠지지 않도록 검증합니다. Actuator 3개 경로는 실제
MariaDB 통합 테스트에서 접근 제어와 응답을 검증합니다.

여행 내보내기는 JSON, CSV, ICS, KML, PDF를 지원합니다. PDF는 Noto Sans KR TrueType
폰트를 subset으로 문서에 포함해 서버와 사용자 기기의 설치 폰트에 의존하지 않으며,
한글 텍스트 보존과 여러 페이지 생성을 자동 테스트합니다. 폰트 출처·해시·OFL 1.1은
[`modules/platform/src/main/resources/fonts/README.md`](modules/platform/src/main/resources/fonts/README.md)에
고정했습니다.

## 내부 운영 인터페이스

- `/internal/webhooks/**`: 제공자별 HMAC-SHA256 서명, timestamp, 이벤트 ID를 검증합니다.
- `/internal/admin/**`: `X-EarthTrip-Internal-Token` 헤더가 필요합니다.
- `/actuator/prometheus`: 내부 운영 토큰이 필요하고, health liveness/readiness는 공개됩니다.
- `/ws/v1/trips/{tripId}`: access token과 여행 열람 권한을 확인한 뒤 presence와 editing
  신호를 전달합니다. 데이터 복구의 원본은 REST `changes` 커서입니다.

웹훅 서명 대상 문자열은 다음 형식입니다.

```text
timestamp + "." + X-EarthTrip-Webhook-Id + "." + rawBody
```

서명 헤더는 `X-EarthTrip-Webhook-Signature: sha256=<hex>` 형식입니다. 필요한 환경변수는
[`.env.example`](.env.example)에 정리되어 있습니다.

예약번호·booking reference·탑승자명·개인 메모는
`WALLET_DATA_ENCRYPTION_KEYS`의 AES-256-GCM key ring으로 필드 암호화합니다. primary key를
회전할 때는 기존 레코드 복호화를 위해 이전 key를 key ring에 유지해야 합니다.

## 환경변수

실제 비밀정보는 `.env`에 두고 커밋하지 않습니다. 필요한 키와 개발 기본값은
[`.env.example`](.env.example)에 있습니다.
