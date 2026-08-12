package com.earthtrip;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.earthtrip.identity.application.port.in.AccessTokenAuthenticationUseCase;
import com.earthtrip.identity.application.port.out.AuthSessionStorePort;
import com.earthtrip.identity.application.port.out.CredentialPort;
import com.earthtrip.identity.application.port.out.UserAccountStorePort;
import com.earthtrip.identity.domain.AuthSession;
import com.earthtrip.identity.domain.EmailAddress;
import com.earthtrip.identity.domain.UserAccount;
import com.earthtrip.identity.domain.UserId;
import com.earthtrip.platform.application.port.in.InternalOperationsUseCase;
import com.earthtrip.trip.application.port.in.CreateTripCommand;
import com.earthtrip.trip.application.port.in.CreateTripResult;
import com.earthtrip.trip.application.port.in.CreateTripUseCase;
import com.earthtrip.trip.application.port.in.TripManagementUseCase;
import com.earthtrip.wallet.application.port.in.WalletRecordUseCase;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestConstructor;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.mariadb.MariaDBContainer;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(
        properties = {
            "earthtrip.internal.webhooks.malware-scan.secret=integration-test-secret",
            "earthtrip.internal.admin-token=integration-admin-token",
            "earthtrip.wallet.encryption-keys="
                    + "primary:AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
        })
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)
class MariaDbApplicationTest {

    private static final UUID REQUEST_ID = UUID.fromString("63ff50e2-7b97-47cc-ac0e-a4bacb6001a3");
    private static final UUID OWNER_ID = UUID.fromString("b5ba2a4e-f274-47f8-8dc0-1392d5774c98");

    @Container @ServiceConnection
    static final MariaDBContainer MARIA_DB = new MariaDBContainer("mariadb:11.8.8");

    private final CreateTripUseCase createTripUseCase;
    private final TripManagementUseCase tripManagementUseCase;
    private final InternalOperationsUseCase internalOperationsUseCase;
    private final WebApplicationContext webApplicationContext;
    private final WalletRecordUseCase walletRecordUseCase;
    private final JdbcTemplate jdbcTemplate;
    private final AuthSessionStorePort authSessionStore;
    private final UserAccountStorePort userAccountStore;
    private final CredentialPort credentialPort;
    private final AccessTokenAuthenticationUseCase accessTokenAuthentication;

    MariaDbApplicationTest(
            CreateTripUseCase createTripUseCase,
            TripManagementUseCase tripManagementUseCase,
            InternalOperationsUseCase internalOperationsUseCase,
            WebApplicationContext webApplicationContext,
            WalletRecordUseCase walletRecordUseCase,
            JdbcTemplate jdbcTemplate,
            AuthSessionStorePort authSessionStore,
            UserAccountStorePort userAccountStore,
            CredentialPort credentialPort,
            AccessTokenAuthenticationUseCase accessTokenAuthentication) {
        this.createTripUseCase = createTripUseCase;
        this.tripManagementUseCase = tripManagementUseCase;
        this.internalOperationsUseCase = internalOperationsUseCase;
        this.webApplicationContext = webApplicationContext;
        this.walletRecordUseCase = walletRecordUseCase;
        this.jdbcTemplate = jdbcTemplate;
        this.authSessionStore = authSessionStore;
        this.userAccountStore = userAccountStore;
        this.credentialPort = credentialPort;
        this.accessTokenAuthentication = accessTokenAuthentication;
    }

    @Test
    void 인증_세션_조인_조회는_엔티티_접근_오류_없이_동작한다() {
        Instant now = Instant.parse("2026-08-06T06:56:30Z");
        UserId userId = new UserId(UUID.randomUUID());
        UserAccount account =
                UserAccount.register(
                        userId,
                        new EmailAddress("auth-" + UUID.randomUUID() + "@example.com"),
                        "integration-password-hash",
                        "인증 여행자",
                        now);
        account.verifyEmail(now);
        userAccountStore.save(account);

        String rawAccessToken = "integration-access-" + UUID.randomUUID();
        AuthSession session =
                AuthSession.create(
                        UUID.randomUUID(),
                        userId,
                        credentialPort.hashToken(rawAccessToken),
                        credentialPort.hashToken("integration-refresh-" + UUID.randomUUID()),
                        "integration-device",
                        now.plusSeconds(3_600),
                        now.plusSeconds(86_400),
                        now);
        authSessionStore.save(session);

        var authenticated = accessTokenAuthentication.authenticate(rawAccessToken);

        assertThat(authenticated.userId()).isEqualTo(userId.value());
        assertThat(authenticated.sessionId()).isEqualTo(session.id());
        assertThat(authenticated.displayName()).isEqualTo("인증 여행자");
    }

    @Test
    void MariaDB와_QueryDSL로_중복_요청을_같은_여행으로_처리한다() {
        CreateTripCommand command =
                new CreateTripCommand(REQUEST_ID, OWNER_ID, "오사카 여행", "Asia/Tokyo", "JPY");

        CreateTripResult first = createTripUseCase.create(command);
        CreateTripResult second = createTripUseCase.create(command);

        assertThat(first.tripId()).isEqualTo(REQUEST_ID);
        assertThat(second).isEqualTo(first);
    }

    @Test
    void JPA_version을_갱신한_응답에_즉시_반영한다() {
        UUID tripId = UUID.randomUUID();
        createTripUseCase.create(
                new CreateTripCommand(tripId, OWNER_ID, "버전 확인 여행", "Asia/Seoul", "KRW"));

        var updated =
                tripManagementUseCase.update(
                        tripId,
                        OWNER_ID,
                        new TripManagementUseCase.UpdateTripCommand(
                                "버전 확인 여행 수정", null, null, null, null, null, null, null, 0));

        assertThat(updated.version()).isEqualTo(1);
        assertThat(tripManagementUseCase.get(tripId, OWNER_ID).version()).isEqualTo(1);
    }

    @Test
    void 실패한_내부_웹훅을_작업과_데드레터와_감사이력으로_저장한다() {
        String eventId = "integration-scan-" + UUID.randomUUID();
        String timestamp = Long.toString(Instant.now().getEpochSecond());
        String body = "{\"fileId\":\"" + UUID.randomUUID() + "\",\"result\":\"SAFE\"}";

        var accepted =
                internalOperationsUseCase.acceptWebhook(
                        "malware-scan",
                        eventId,
                        timestamp,
                        "sha256=" + signature(timestamp, eventId, body),
                        body);

        assertThat(accepted.status()).isEqualTo("FAILED");
        assertThat(internalOperationsUseCase.jobs("FAILED", "MALWARE_SCAN_WEBHOOK", 20))
                .extracting(InternalOperationsUseCase.JobResult::jobId)
                .contains(accepted.jobId());
        assertThat(internalOperationsUseCase.deadLetters("OPEN", 20))
                .extracting(InternalOperationsUseCase.DeadLetterResult::jobId)
                .contains(accepted.jobId());
        assertThat(
                        internalOperationsUseCase.auditEvents(
                                null, "OPERATIONAL_JOB", accepted.jobId().toString(), 20))
                .isNotEmpty();
    }

    @Test
    void 내부_운영_API와_Prometheus는_별도_토큰으로_보호한다() throws Exception {
        MockMvc mockMvc =
                MockMvcBuilders.webAppContextSetup(webApplicationContext)
                        .apply(springSecurity())
                        .build();

        mockMvc.perform(get("/internal/admin/jobs")).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        get("/internal/admin/jobs")
                                .header("X-EarthTrip-Internal-Token", "integration-admin-token"))
                .andExpect(status().isOk());
        mockMvc.perform(get("/actuator/health/liveness")).andExpect(status().isOk());
        mockMvc.perform(get("/actuator/prometheus")).andExpect(status().isUnauthorized());
        mockMvc.perform(
                        get("/actuator/prometheus")
                                .header("X-EarthTrip-Internal-Token", "integration-admin-token"))
                .andExpect(status().isOk());
    }

    @Test
    void 민감_예약정보는_DB에_AES_GCM_envelope로_저장하고_API에서는_복호화한다() {
        UUID tripId = UUID.randomUUID();
        UUID reservationId = UUID.randomUUID();
        createTripUseCase.create(
                new CreateTripCommand(tripId, OWNER_ID, "민감정보 확인 여행", "Asia/Seoul", "KRW"));

        var created =
                walletRecordUseCase.create(
                        tripId,
                        OWNER_ID,
                        "RESERVATION",
                        false,
                        new WalletRecordUseCase.Command(
                                reservationId,
                                null,
                                Map.of(
                                        "confirmationNumber", "SECRET-ABC-1234",
                                        "passengerNames", List.of("홍길동"),
                                        "details", Map.of("personalNote", "창가 좌석 요청")),
                                "CONFIRMED",
                                "TRIP",
                                0,
                                0));

        assertThat(created.payload().get("confirmationNumber")).isEqualTo("SECRET-ABC-1234");
        String storedPayload =
                jdbcTemplate.queryForObject(
                        "select payload from wallet_records where id = ?",
                        String.class,
                        reservationId.toString());
        assertThat(storedPayload)
                .contains("_earthTripProtected", "AES-256-GCM")
                .doesNotContain("SECRET-ABC-1234", "홍길동", "창가 좌석 요청");
        assertThat(
                        walletRecordUseCase
                                .get(tripId, OWNER_ID, "RESERVATION", reservationId)
                                .payload())
                .isEqualTo(created.payload());
    }

    private static String signature(String timestamp, String eventId, String body) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(
                    new SecretKeySpec(
                            "integration-test-secret".getBytes(StandardCharsets.UTF_8),
                            "HmacSHA256"));
            return HexFormat.of()
                    .formatHex(
                            mac.doFinal(
                                    (timestamp + "." + eventId + "." + body)
                                            .getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException exception) {
            throw new AssertionError(exception);
        }
    }
}
