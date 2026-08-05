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

Apple 로그인을 사용하려면 `APPLE_OAUTH_CLIENT_IDS`에 iOS 앱 bundle ID와 필요 시
Services ID를 쉼표로 구분해 넣습니다. 백엔드는 Apple JWKS로 ID token의 서명, issuer,
audience, 만료 시각, subject와 인증된 이메일을 검증합니다. Apple private key는 현재
네이티브 앱의 ID token 로그인에는 필요하지 않습니다.

외부 기능은 가용성 boolean을 수동으로 켜는 방식이 아닙니다. 필요한 자격증명과 저장 경로가
모두 설정되면 `/api/v1/app-capabilities`가 현재 설정 상태를 자동으로 계산합니다.

| 기능 | 구현 | 필수 설정 |
| --- | --- | --- |
| 인증·재설정·초대 이메일 | AWS SES | `AWS_REGION`, AWS credential chain, `SES_FROM_EMAIL`, `PUBLIC_BASE_URL` |
| 장소·경로·날씨·지오코딩·시간대 | Google Maps Platform | `GOOGLE_MAPS_SERVER_API_KEY` |
| 환율 | ECB 기준환율 | 별도 키 없음 |
| 항공 운항·가격 | Amadeus | `AMADEUS_API_KEY`, `AMADEUS_API_SECRET` |
| 여행경보·안전공지 | 외교부 공공데이터 | `MOFA_OPENAPI_SERVICE_KEY` |
| 링크 미리보기 | 제한된 HTML/Open Graph 추출 | 별도 키 없음 |
| 파일 | private local storage + ClamAV | `OBJECT_STORAGE_LOCAL_ROOT`, `OBJECT_STORAGE_SIGNING_KEY_BASE64`, `CLAMAV_ENDPOINT`, `BACKEND_PUBLIC_BASE_URL` |
| 개인정보 내보내기 | private local export storage | `PERSONAL_DATA_EXPORT_ROOT` |
| Android/iOS 푸시 | FCM HTTP v1 | `FIREBASE_PROJECT_ID`, `GOOGLE_APPLICATION_CREDENTIALS`, `PUSH_TOKEN_ENCRYPTION_KEY_BASE64` |
| Google Calendar | OAuth + 단방향 일정 동기화 | Calendar OAuth 3개 값, `INTEGRATION_TOKEN_ENCRYPTION_KEYS` |

32-byte Base64 키는 다음처럼 생성할 수 있습니다.

```bash
openssl rand -base64 32
```

`INTEGRATION_TOKEN_ENCRYPTION_KEYS`는 `primary:<위에서 생성한 값>` 형식입니다. 운영 서버의
`.env`는 JAR 옆이 아니라 systemd `EnvironmentFile=/etc/earth-trip/earth-trip.env`로 읽게 하는
구성을 권장합니다. Firebase service-account JSON은 환경변수 본문이 아니라 접근 권한을
제한한 별도 파일로 두고 `GOOGLE_APPLICATION_CREDENTIALS`에는 그 절대 경로만 설정합니다.

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
Flyway V1~V30, Hibernate schema validation, 전체 Repository 생성, JPA version,
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
- `/ws/v1/trips/{tripId}`: access token과 여행 열람 권한을 확인한 뒤 presence, editing,
  커밋된 변경 신호를 전달합니다. 데이터 복구의 원본은 REST `changes` 커서이며 앱은 주기적인
  전체 reconciliation도 수행합니다.

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

OCI Rocky Linux의 systemd, Nginx, 제한 배포, MariaDB backup/restore와 운영 runbook은
[`deploy/README.md`](deploy/README.md)에 있습니다.
