# Earth Trip AWS SES + Cloudflare 운영 설정 가이드

이 문서는 Cloudflare가 DNS를 관리하는 `earth-trips.com`을 AWS SES 서울 리전
`ap-northeast-2`에 연결하는 실제 운영 절차다. 화면의 명칭은 AWS/Cloudflare UI 언어에 따라
한글 또는 영문으로 보일 수 있지만, 괄호 안 영문 메뉴명을 기준으로 찾으면 된다.

Earth Trip 백엔드는 SMTP가 아니라 AWS SDK의 SES `SendEmail` API를 직접 호출한다.
따라서 발신에는 Lambda, SES SMTP credential, SMTP port 개방이 필요하지 않다.

## 0. 먼저 정할 최종 구성

| 용도 | 실제 값 | Cloudflare 프록시 | 필수 여부 |
| --- | --- | --- | --- |
| 웹사이트 | `https://earth-trips.com` | Proxied, 주황 구름 | 기존 값 유지 |
| 백엔드 API | `https://api.earth-trips.com` | Proxied, 주황 구름 | 기존 값 유지 |
| SES 리전 | `ap-northeast-2` (서울) | 해당 없음 | 필수 |
| SES 발신 identity | `earth-trips.com` | SES CNAME은 DNS only | 필수 |
| 사용자에게 보이는 From | `no-reply@earth-trips.com` | 해당 없음 | 필수 |
| custom MAIL FROM | `mail.earth-trips.com` | MX/TXT는 DNS only | 권장 |
| DMARC | `_dmarc.earth-trips.com` | TXT는 DNS only | 권장 |

중요한 원칙은 다음 세 가지다.

1. `earth-trips.com`과 `api.earth-trips.com`의 기존 A/AAAA/CNAME 레코드는 삭제하거나
   프록시 상태를 바꾸지 않는다.
2. DKIM, SPF, DMARC, MX 등 **메일용 DNS 레코드는 모두 DNS only**다. Cloudflare 주황 구름을
   켜지 않는다. MX/TXT에는 프록시 선택 자체가 없고, DKIM CNAME에는 회색 구름을 선택한다.
3. 루트 `earth-trips.com`의 MX는 SES 발신에 필요하지 않다.

Cloudflare는 SMTP를 프록시하지 않으며 SES DKIM CNAME도 프록시하면 안 된다. 공식 근거는
[Cloudflare proxy 상태](https://developers.cloudflare.com/dns/proxy-status/)와
[Cloudflare DNS 레코드 관리](https://developers.cloudflare.com/dns/manage-dns-records/how-to/create-dns-records/)를
참조한다.

## 1. 2026-08-04 현재 외부에서 확인된 상태

이 항목은 문서 작성 시점의 읽기 전용 DNS/HTTPS 확인 결과다. AWS 계정 내부 설정을 확인한 것은
아니므로 SES 설정 완료 증거로 사용하면 안 된다.

- 권한 서버는 Cloudflare의 `raphaela.ns.cloudflare.com`, `jasper.ns.cloudflare.com`이다.
- `earth-trips.com`과 `api.earth-trips.com`은 Cloudflare 프록시 주소로 응답한다.
- `https://earth-trips.com/`은 HTTP 200으로 응답한다.
- `https://api.earth-trips.com/actuator/health/readiness`는 HTTP 200, `UP`으로 응답한다.
- 루트 MX와 `_dmarc` TXT는 아직 공개 DNS에서 확인되지 않는다.
- 기존 루트 TXT에는 Google site verification 값이 있으므로 삭제하면 안 된다.

작업 시작 전에 Cloudflare의 **DNS → Records**에서 현재 레코드를 CSV로 내보내거나 화면을
캡처해 둔다. SES 레코드를 추가하는 작업은 기존 웹 레코드 교체가 아니라 **추가** 작업이다.

## 2. 전체 작업 순서

1. AWS 콘솔 리전을 서울로 고정한다.
2. SES에 `earth-trips.com` domain identity를 만든다.
3. SES가 생성한 DKIM CNAME 3개를 Cloudflare에 추가한다.
4. custom MAIL FROM용 MX/TXT를 추가한다.
5. DMARC TXT를 추가한다.
6. SES sandbox 해제를 요청한다.
7. suppression list, configuration set, SNS 알림을 만든다.
8. Earth Trip 전용 IAM 자격증명을 만든다.
9. OCI 서버 환경변수를 설정하고 애플리케이션을 재시작한다.
10. SES simulator와 실제 앱 네 가지 흐름을 테스트한다.

모든 SES identity, sandbox 상태, quota, configuration set은 **리전별**이다. 작업 중
AWS 콘솔 오른쪽 위 리전이 서울이 아닌 것을 발견하면 그 화면에서 중단하고 서울 리전으로 돌아간다.

## 3. AWS SES에서 발신 domain identity 만들기

### 3.1 콘솔에서 identity 생성

1. [AWS Console](https://console.aws.amazon.com/)에 로그인한다.
2. 오른쪽 위 리전 선택에서 **Asia Pacific (Seoul) / 아시아 태평양(서울) /
   `ap-northeast-2`**를 선택한다.
3. 상단 검색창에 `SES`를 입력하고 **Amazon Simple Email Service**를 연다.
4. 왼쪽 메뉴 **Configuration → Identities**를 연다. UI에 따라 **Verified identities**로
   표시될 수 있다.
5. **Create identity**를 누른다.
6. Identity type은 **Domain**을 선택한다.
7. Domain에는 `earth-trips.com`을 입력한다. `https://`, `www`, `@`는 입력하지 않는다.
8. **Assign a default configuration set**은 아직 끈다. configuration set을 만든 뒤 백엔드의
   `SES_CONFIGURATION_SET`으로 명시할 예정이다.
9. DKIM 설정은 다음과 같이 둔다.
   - DKIM configuration: **Easy DKIM**
   - DKIM signing key length: **RSA_2048_BIT**
   - DKIM signatures: **Enabled**
10. Custom MAIL FROM은 이 단계에서 입력해도 되지만, 실수를 줄이기 위해 identity 검증 후
    별도 단계에서 설정하는 것을 권장한다.
11. **Create identity**를 누른다.

SES domain identity를 검증하면 그 아래의 `no-reply@earth-trips.com` 같은 주소는 별도 메일함이나
메일 인증 링크 없이 발신에 사용할 수 있다. AWS의 identity와 DKIM 절차는
[SES Easy DKIM 공식 문서](https://docs.aws.amazon.com/ses/latest/dg/send-email-authentication-dkim-easy.html)를
따른다.

### 3.2 SES가 보여 주는 DKIM 값을 기록

identity 상세 화면의 **Authentication → DomainKeys Identified Mail (DKIM) → Publish DNS
records**에서 CNAME 세 개를 복사한다. 값은 AWS가 identity마다 생성하므로 아래 예시의
`TOKEN1` 등을 실제로 입력하면 안 된다.

| Type | Name 예시 | Target 예시 |
| --- | --- | --- |
| CNAME | `TOKEN1._domainkey.earth-trips.com` | `TOKEN1.dkim.amazonses.com` |
| CNAME | `TOKEN2._domainkey.earth-trips.com` | `TOKEN2.dkim.amazonses.com` |
| CNAME | `TOKEN3._domainkey.earth-trips.com` | `TOKEN3.dkim.amazonses.com` |

브라우저 탭을 닫아도 identity 상세 화면에서 다시 볼 수 있다. 세 Name과 Target을 메모장에
옮길 때 공백, 따옴표, 줄바꿈을 포함하지 않는다.

## 4. Cloudflare에 DKIM CNAME 3개 추가

각 레코드마다 다음 절차를 반복한다.

1. [Cloudflare Dashboard](https://dash.cloudflare.com/)에 로그인한다.
2. 계정을 선택하고 `earth-trips.com` zone을 연다.
3. 왼쪽 메뉴 **DNS → Records**로 이동한다.
4. **Add record**를 누른다.
5. Type은 **CNAME**을 선택한다.
6. Name에는 SES가 준 첫 번째 Name을 입력한다.
   - Cloudflare 화면이 zone suffix를 자동 표시하면 `TOKEN1._domainkey`까지만 입력해도 된다.
   - 저장 전 화면에 완성된 이름이
     `TOKEN1._domainkey.earth-trips.com`인지 확인한다.
   - `...earth-trips.com.earth-trips.com`으로 두 번 붙으면 잘못된 것이다.
7. Target에는 SES가 준 `TOKEN1.dkim.amazonses.com` 값을 그대로 입력한다.
8. Proxy status는 **DNS only**로 바꾼다. 아이콘이 회색 구름이어야 한다.
9. TTL은 **Auto**로 둔다.
10. Comment가 지원되면 `AWS SES Easy DKIM - Seoul`을 적는다.
11. **Save**를 누른다.
12. 나머지 두 CNAME도 같은 방식으로 추가한다.

Cloudflare **DNS → Settings → CNAME Flattening**에서 `Flatten all CNAMEs`가 켜져 있다면 끄고,
기본값인 apex/root만 flatten하는 설정을 사용한다. DKIM 검증 CNAME은 flatten되면 안 된다.
Cloudflare도 [DKIM CNAME은 DNS only이며 flatten하지 말아야 한다](https://developers.cloudflare.com/dns/manage-dns-records/troubleshooting/cname-domain-verification/)고
안내한다.

### 4.1 DKIM 검증 확인

DNS 저장 후 로컬에서 각 토큰을 확인한다.

```bash
dig +short CNAME TOKEN1._domainkey.earth-trips.com
dig +short CNAME TOKEN2._domainkey.earth-trips.com
dig +short CNAME TOKEN3._domainkey.earth-trips.com
```

각 결과가 SES가 준 `TOKEN*.dkim.amazonses.com.`과 일치해야 한다. 그 다음 AWS SES →
Configuration → Identities → `earth-trips.com`에서 다음을 확인한다.

- Identity status: **Verified**
- DKIM status: **Successful**
- DKIM signatures: **Enabled**

대개 수분 내 처리되지만 DNS 전파/SES 감지는 더 오래 걸릴 수 있다. 72시간이 지나도 Pending이면
16장의 문제 해결 항목부터 확인한다.

## 5. custom MAIL FROM 설정

이 설정은 수신자에게 보이는 `From:` 주소를 바꾸는 기능이 아니다. 반송 처리에 쓰는 envelope
sender/Return-Path를 SES 기본 `amazonses.com` 대신 `mail.earth-trips.com`으로 맞추어 SPF 및
DMARC 정렬을 개선한다. `mail.earth-trips.com`은 발신 또는 일반 수신 주소로 함께 사용하지 않는다.

### 5.1 SES 콘솔

1. AWS SES → **Configuration → Identities → earth-trips.com**을 연다.
2. **Authentication** 탭을 선택한다.
3. **Custom MAIL FROM domain** 영역의 **Edit**를 누른다.
4. **Use a custom MAIL FROM domain**을 체크한다.
5. MAIL FROM domain에 `mail.earth-trips.com`을 입력한다.
6. Behavior on MX failure는 초기 전환 시 **Use default MAIL FROM domain**을 권장한다.
   - DNS 실수 중에도 SES 기본 MAIL FROM으로 발송된다.
   - DNS가 안정된 뒤 엄격히 실패시키려면 `Reject message`로 바꿀 수 있다.
7. **Save changes**를 누른다.
8. 화면에 표시된 MX와 SPF(TXT) 값을 복사한다.

서울 리전의 예상값은 다음과 같지만, **SES 화면의 Publish DNS records 값을 우선**한다.

| Cloudflare Type | Name | Content / Mail server | Priority | Proxy | TTL |
| --- | --- | --- | --- | --- | --- |
| MX | `mail` | `feedback-smtp.ap-northeast-2.amazonses.com` | `10` | DNS only | Auto |
| TXT | `mail` | `v=spf1 include:amazonses.com ~all` | 해당 없음 | DNS only | Auto |

### 5.2 Cloudflare 등록

1. Cloudflare → `earth-trips.com` → **DNS → Records → Add record**를 연다.
2. Type **MX**, Name `mail`, Mail server에 SES가 준 값을 입력하고 Priority `10`, TTL `Auto`로
   저장한다.
3. 다시 **Add record**를 눌러 Type **TXT**, Name `mail`, Content에 SES가 준 SPF 문자열을
   입력하고 저장한다.
4. `mail.earth-trips.com`에 다른 MX가 있으면 제거 전 용도를 확인한다. SES custom MAIL FROM은
   그 이름에 **정확히 MX 한 개**가 있어야 한다.
5. `mail.earth-trips.com`을 가리키는 A/AAAA/CNAME을 새로 만들지 않는다.

확인 명령:

```bash
dig +short MX mail.earth-trips.com
dig +short TXT mail.earth-trips.com
```

SES identity 상세에서 MAIL FROM status가 **Successful**인지 확인한다. AWS는 이 레코드를 최대
72시간 감지할 수 있다. 정확한 요구사항은
[SES custom MAIL FROM 공식 문서](https://docs.aws.amazon.com/ses/latest/dg/mail-from.html)에 있다.

주의: SPF TXT는 `mail.earth-trips.com`에 두 개 이상 만들면 안 된다. 현재 루트
`earth-trips.com`에 다른 SPF가 생기더라도 MAIL FROM subdomain의 SPF와는 이름이 다르므로
무조건 합칠 필요는 없다.

## 6. DMARC 설정

DKIM과 custom MAIL FROM이 성공한 뒤 DMARC를 관찰 모드로 시작한다.

### 6.1 보고서를 받을 메일함이 없는 경우

Cloudflare → **DNS → Records → Add record**에서 다음을 만든다.

| Type | Name | Content | TTL |
| --- | --- | --- | --- |
| TXT | `_dmarc` | `v=DMARC1; p=none; adkim=r; aspf=r; pct=100` | Auto |

### 6.2 보고서를 받을 주소가 있는 경우

먼저 `dmarc-reports@earth-trips.com`이 실제로 메일을 받을 수 있게 만든 뒤 다음 값을 쓴다.

```text
v=DMARC1; p=none; rua=mailto:dmarc-reports@earth-trips.com; adkim=r; aspf=r; pct=100
```

Cloudflare에서 일반 메일 전달을 쓰려면 현재 UI 기준 **Compute → Email Service → Email Routing**에
들어가 Destination Address를 등록·검증한 뒤 `dmarc-reports@earth-trips.com` routing rule을
만들 수 있다. 이 기능을 활성화하면 Cloudflare가 루트 MX 레코드를 추가하므로, 이미 Google
Workspace·Microsoft 365 등 다른 메일 수신 서비스를 쓰고 있다면 활성화하지 않는다. 자세한
메뉴는 [Cloudflare Email Routing 공식 문서](https://developers.cloudflare.com/email-service/configuration/email-routing-addresses/)를
참조한다.

확인:

```bash
dig +short TXT _dmarc.earth-trips.com
```

규칙:

- `_dmarc.earth-trips.com`에는 DMARC TXT를 정확히 하나만 둔다.
- 처음부터 `p=reject`로 시작하지 않는다.
- 2~4주 동안 정상 메일이 DKIM/SPF align되는지 본 다음 `p=quarantine; pct=25`, `pct=100`,
  최종 `p=reject` 순으로 강화한다.
- `no-reply@earth-trips.com`은 표시용 발신 주소이므로 실제 받은편지함이 없어도 된다. 다만
  사용자가 회신할 가능성이 있으면 별도 `support@earth-trips.com`과 Reply-To 정책을 설계한다.

## 7. SES sandbox 해제

새 SES 계정의 sandbox와 quota는 리전별이다. 서울 리전 sandbox에서는 검증된 수신자 또는 SES
mailbox simulator로만 보낼 수 있고, 24시간 200건·초당 1건 제한이 있다. AWS의 현재 절차는
[SES production access 공식 문서](https://docs.aws.amazon.com/ses/latest/dg/request-production-access.html)에
있다.

### 7.1 요청 전 체크

- AWS 콘솔 리전이 서울인지 확인한다.
- `earth-trips.com` identity가 Verified인지 확인한다.
- DKIM이 Successful인지 확인한다.
- 웹사이트 `https://earth-trips.com`이 정상 열리는지 확인한다.
- 반송/불만을 받을 운영 연락처를 정한다.

### 7.2 콘솔 요청

1. SES 왼쪽 메뉴 **Account dashboard**를 연다.
2. 상단 sandbox 경고 상자의 **View Get set up page**를 누른다.
3. **Request production access**를 누른다.
4. Mail type은 **Transactional**을 선택한다.
5. Website URL은 `https://earth-trips.com`을 입력한다.
6. Additional contacts에는 AWS의 발송 평판/제한 알림을 실제로 확인할 운영자 주소를 넣는다.
   최대 4개까지 쉼표로 구분할 수 있다.
7. Preferred contact language는 **English**를 선택한다.
8. 요청한 사용자에게만 메일을 보내며 bounce/complaint 처리 절차가 있다는 Acknowledgement를
   체크한다.
9. **Submit request**를 누른다.

추가 설명을 요청받으면 다음 내용을 Earth Trip의 실제 예상 발송량에 맞게 수정해 답한다.

```text
Earth Trip sends only one-to-one transactional email triggered by user actions:
account email verification, password reset, email address change confirmation,
and invitations to trips created by users. We do not use purchased, rented, or
scraped mailing lists and do not send marketing campaigns through this stream.
Recipients explicitly provide their address or are invited by an Earth Trip user.
We monitor bounces and complaints through Amazon SES event publishing and SNS,
enable the SES account-level suppression list for BOUNCE and COMPLAINT, and stop
sending to suppressed recipients. Initial volume: [replace with expected daily volume].
Website: https://earth-trips.com
```

AWS 문서상 최초 답변 목표는 24시간 이내지만 추가 질문이 있으면 더 걸릴 수 있다. 제출 후 검토가
끝날 때까지 해당 요청 내용을 수정할 수 없다.

확인 명령:

```bash
aws sesv2 get-account --region ap-northeast-2
aws sesv2 get-email-identity \
  --region ap-northeast-2 \
  --email-identity earth-trips.com
```

`ProductionAccessEnabled`가 `true`인지, `VerifiedForSendingStatus`가 `true`인지 확인한다.

## 8. 반송·불만·지연 알림 설정

코드가 메일을 보냈다는 응답만으로 실제 배달을 증명할 수 없다. account suppression과 event
destination을 같이 설정한다.

### 8.1 account-level suppression

1. SES → **Configuration → Suppression list**를 연다.
2. **Account-level settings**의 **Edit**를 누른다.
3. Suppression list의 **Enabled**를 체크한다.
4. Suppression reasons에서 **BOUNCE**, **COMPLAINT**를 모두 선택한다.
5. **Save changes**를 누른다.

이 설정도 서울 리전에만 적용된다. 공식 메뉴와 동작은
[SES account-level suppression 공식 문서](https://docs.aws.amazon.com/ses/latest/dg/sending-email-suppression-list.html)를
참조한다.

### 8.2 configuration set 생성

1. SES → **Configuration → Configuration sets**를 연다.
2. **Create set**을 누른다.
3. Configuration set name은 `earth-trip-transactional`을 입력한다.
4. Sending IP pool은 shared/default 상태를 유지한다. 전용 IP는 초기 소량 발송에 필요하지 않다.
5. Tracking options의 custom redirect domain은 끈다. 현재 인증/초대 메일에는 open/click 추적이
   필요하지 않다.
6. Advanced delivery options는 기본값을 유지한다. OTP 만료보다 늦게 배달되는 것을 막는 별도
   정책이 확정된 경우에만 Maximum delivery duration을 줄인다.
7. Reputation metrics는 CloudWatch 추가 비용을 인지하고 선택한다. 초기는 활성화를 권장하지만,
   SNS 이벤트만으로 운영할 경우 꺼도 발송 자체에는 영향이 없다.
8. Suppression list options의 **Override account level settings**는 체크하지 않는다. 그러면 앞에서
   만든 account-level BOUNCE/COMPLAINT suppression을 그대로 사용한다.
9. **Create set**을 누른다.

상세 옵션은 [SES configuration set 생성 문서](https://docs.aws.amazon.com/ses/latest/dg/creating-configuration-sets.html)를
참조한다.

### 8.3 SNS topic과 운영자 email 구독

1. AWS 콘솔 상단 검색에서 `SNS`를 열고 리전이 서울인지 확인한다.
2. **Topics → Create topic**을 누른다.
3. Type은 반드시 **Standard**를 선택한다. FIFO는 SES event destination에 사용할 수 없다.
4. Name은 `earth-trip-ses-events`로 입력하고 **Create topic**을 누른다.
5. 생성된 topic 상세에서 **Create subscription**을 누른다.
6. Protocol은 **Email**, Endpoint에는 운영자 메일 주소를 입력한다.
7. **Create subscription** 후 해당 받은편지함의 AWS 확인 메일에서 **Confirm subscription**을
   누른다.
8. SNS Subscriptions 화면에서 Status가 `Confirmed`인지 확인한다. `PendingConfirmation`이면
   알림이 전달되지 않는다.

### 8.4 SES event destination 연결

1. SES → **Configuration → Configuration sets → earth-trip-transactional**을 연다.
2. **Event destinations** 탭에서 **Add destination**을 누른다.
3. Event types에서 최소 다음을 체크한다.
   - `Hard bounces` 또는 `Bounce`
   - `Complaints`
   - `Rejects`
   - `Delivery delays`
   - 운영 확인이 필요하면 `Deliveries`
4. **Next**를 누른다.
5. Destination type은 **Amazon SNS**를 선택한다.
6. Name은 `transactional-ops`로 입력한다.
7. Event publishing은 **Enabled**로 둔다.
8. SNS topic은 `earth-trip-ses-events`를 선택한다.
9. **Next → Add destination**을 누른다.
10. 콘솔이 SNS topic access policy를 자동으로 구성하지 못하면 SNS topic → **Edit → Access
    policy**에서 SES configuration set ARN이 `sns:Publish`할 수 있도록 설정한다. AWS가 화면에
    제시하는 policy를 우선 사용한다.

SES event destination의 현재 콘솔 절차와 Standard topic 요구사항은
[SES SNS event destination 문서](https://docs.aws.amazon.com/ses/latest/dg/event-publishing-add-event-destination-sns.html)에
있다.

## 9. Earth Trip 전용 IAM 자격증명

현재 백엔드는 AWS 밖의 OCI VM에서 동작하므로 EC2 instance role을 바로 사용할 수 없다. 가장
단순한 전환 방법은 콘솔 로그인 권한이 없는 전용 IAM user와 최소권한 access key다. 장기적으로는
AWS IAM Roles Anywhere 같은 임시 자격증명 방식이 더 안전하다.

절대로 다음을 하지 않는다.

- AWS root 계정 access key 사용
- 사람의 관리자 IAM access key 재사용
- 키를 Git, `.env.example`, CI log, 메신저에 붙여 넣기
- SES SMTP credential 생성: 현재 코드는 SMTP를 사용하지 않는다.

### 9.1 account ID 확인

AWS Console 오른쪽 위 계정 메뉴에서 12자리 Account ID를 확인하거나 CloudShell에서 실행한다.

```bash
aws sts get-caller-identity
```

### 9.2 최소권한 policy 생성

1. AWS 검색에서 **IAM**을 연다. IAM은 global service다.
2. **Policies → Create policy**를 누른다.
3. **JSON** 탭을 선택한다.
4. 이 저장소의 `deploy/aws/ses/outbound-send-policy.example.json`을 복사한다.
5. `111122223333`을 실제 12자리 AWS Account ID로 바꾼다.
6. Region, identity, configuration set, From 주소가 각각 `ap-northeast-2`,
   `earth-trips.com`, `earth-trip-transactional`, `no-reply@earth-trips.com`인지 확인한다.
7. **Next**를 누르고 policy name을 `EarthTripSesSendPolicy`로 입력해 생성한다.

이 policy는 `ses:SendEmail`만 허용하고, 발신 identity와 백엔드가 요청에 포함하는 configuration
set 두 ARN을 허용한다. SES v2 `SendEmail`에서 configuration set을 지정하면 해당 리소스도 IAM
허용 범위에 들어가야 한다. 템플릿을 바꾸어 `ses:*` 또는 `Resource: "*"`로 넓히지 않는다.

### 9.3 전용 IAM user와 access key

1. IAM → **Users → Create user**를 누른다.
2. User name은 `earth-trip-ses-sender`로 입력한다.
3. AWS Management Console access는 제공하지 않는다.
4. Permissions에서 **Attach policies directly**를 선택하고
   `EarthTripSesSendPolicy`만 체크한다.
5. user를 생성한다.
6. user 상세 → **Security credentials** 탭 → **Access keys → Create access key**를 누른다.
7. Use case는 **Application running outside AWS**를 선택한다.
8. 설명 태그에 `earth-trip production OCI SES sender`를 적고 생성한다.
9. Access key ID와 Secret access key를 비밀관리 도구에 즉시 저장한다. Secret은 이 화면을
   닫은 뒤 다시 볼 수 없다.

AWS도 장기 키를 코드 저장소에 넣지 말고 외부 secret 관리 또는 임시 자격증명을 권장한다.
[AWS programmatic credential 지침](https://docs.aws.amazon.com/IAM/latest/UserGuide/security-creds-programmatic-access.html)을
참조한다.

## 10. OCI 운영 서버 적용과 발신 테스트

### 10.1 환경변수

운영 서버 `/etc/earth-trip/earth-trip.env`에서 다음 값을 설정한다.

```properties
PUBLIC_BASE_URL=https://earth-trips.com
BACKEND_PUBLIC_BASE_URL=https://api.earth-trips.com
REALTIME_ALLOWED_ORIGINS=https://earth-trips.com

AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=replace-with-earth-trip-iam-access-key
AWS_SECRET_ACCESS_KEY=replace-with-earth-trip-iam-secret-key
SES_FROM_EMAIL=no-reply@earth-trips.com
SES_CONFIGURATION_SET=earth-trip-transactional
```

파일은 `root:earthtrip`, mode `0640`을 유지한다.

```bash
sudo chown root:earthtrip /etc/earth-trip/earth-trip.env
sudo chmod 0640 /etc/earth-trip/earth-trip.env
sudo systemctl restart earth-trip
sudo systemctl status earth-trip --no-pager
sudo journalctl -u earth-trip -n 200 --no-pager
curl --fail https://api.earth-trips.com/actuator/health/readiness
```

임시 credential을 쓸 때만 `AWS_SESSION_TOKEN`도 넣는다. AWS SDK default credential provider
chain을 사용하므로 코드에 key 값을 추가하지 않는다.

### 10.2 AWS 자격증명과 직접 발송 확인

운영 서버에 AWS CLI가 이미 있고 보안 정책상 실행 가능할 때만 아래를 쓴다. 명령 history에 secret을
직접 입력하지 않는다.

```bash
aws sts get-caller-identity
aws sesv2 get-account --region ap-northeast-2
aws sesv2 get-email-identity \
  --region ap-northeast-2 \
  --email-identity earth-trips.com
aws sesv2 list-configuration-sets --region ap-northeast-2
```

먼저 SES mailbox simulator의 정상 배달 주소로 보낸다. simulator는 sandbox에서도 recipient
identity 검증 없이 사용할 수 있다.

```bash
aws sesv2 send-email \
  --region ap-northeast-2 \
  --from-email-address no-reply@earth-trips.com \
  --destination 'ToAddresses=success@simulator.amazonses.com' \
  --content 'Simple={Subject={Data=Earth Trip SES test,Charset=UTF-8},Body={Text={Data=SES setup works,Charset=UTF-8}}}' \
  --configuration-set-name earth-trip-transactional
```

그 다음 SNS 이벤트 확인용으로 한 번씩 보낸다.

- 정상: `success@simulator.amazonses.com`
- hard bounce: `bounce@simulator.amazonses.com`
- complaint: `complaint@simulator.amazonses.com`
- suppression: `suppressionlist@simulator.amazonses.com`

Simulator 메시지는 평판 지표에 영향을 주지 않는다. 주소별 의미는
[SES mailbox simulator 공식 문서](https://docs.aws.amazon.com/ses/latest/dg/send-an-email-from-console.html)에
있다.

### 10.3 앱의 필수 네 가지 흐름

Production access 승인 후 실제 staging 이메일로 아래를 각각 한 번 실행한다.

1. 회원가입 이메일 인증
2. 비밀번호 재설정
3. 이메일 주소 변경 확인
4. 여행 초대

각 테스트에서 다음을 한 줄씩 기록한다.

- API 요청 시각과 성공 여부
- 애플리케이션 log의 SES message ID 또는 오류
- SNS의 Delivery/Bounce/Complaint event
- 실제 받은편지함 도착 여부와 spam 여부
- 헤더의 `DKIM=pass`, `SPF=pass`, `DMARC=pass`
- 메일 본문의 링크가 `https://earth-trips.com/...`을 가리키는지

여기까지 성공하면 **발신 전환은 완료**다.


## 11. 최종 end-to-end 검증

### 11.1 발신 체크리스트

- [ ] AWS 콘솔 리전이 서울이다.
- [ ] `earth-trips.com` identity가 Verified다.
- [ ] DKIM이 Successful이고 CNAME 세 개가 DNS only다.
- [ ] custom MAIL FROM이 Successful이다.
- [ ] `mail.earth-trips.com` MX는 정확히 한 개다.
- [ ] `_dmarc.earth-trips.com` TXT는 정확히 한 개다.
- [ ] `ProductionAccessEnabled=true`다.
- [ ] account suppression에 BOUNCE와 COMPLAINT가 켜져 있다.
- [ ] `earth-trip-transactional` configuration set이 있다.
- [ ] SNS email subscription이 Confirmed다.
- [ ] simulator delivery/bounce/complaint event가 예상대로 도착한다.
- [ ] 실제 앱 네 가지 transactional flow가 모두 성공한다.


### 11.2 DNS 일괄 확인

```bash
dig +short NS earth-trips.com
dig +short MX earth-trips.com
dig +short TXT earth-trips.com
dig +short TXT _dmarc.earth-trips.com
dig +short MX mail.earth-trips.com
dig +short TXT mail.earth-trips.com
```

루트 MX가 비어 있는 것은 SES 발신 구성에서는 정상이다. 일반 회사 메일을 운영하기
위해 Cloudflare Email Routing 또는 Google Workspace 등을 별도로 활성화한 경우에만 루트 MX가
있어야 한다.

## 12. 자주 발생하는 문제

### DKIM이 Pending/Failed

- Cloudflare CNAME이 주황 구름인지 확인하고 **DNS only**로 바꾼다.
- Name이 `...earth-trips.com.earth-trips.com`으로 중복되지 않았는지 확인한다.
- Target token이 다른 AWS 리전/identity의 값인지 확인한다.
- CNAME flatten all을 끈다.
- `dig CNAME` 결과가 SES 화면의 Target과 정확히 같은지 확인한다.
- 변경 후 즉시 identity를 지웠다 다시 만들지 말고 최대 72시간 감지 시간을 고려한다.

### custom MAIL FROM이 Pending/Failed

- MX Name이 `@`가 아니라 `mail`인지 확인한다.
- MX server가 `feedback-smtp.ap-northeast-2.amazonses.com`인지 SES 화면과 대조한다.
- priority가 10인지 확인한다.
- `mail.earth-trips.com` MX가 두 개 이상인지 확인한다.
- SPF TXT가 `mail` 이름에 있고 중복 SPF가 없는지 확인한다.

### `MessageRejected: Email address is not verified`

- AWS 리전이 서울인지 확인한다. identity 검증은 리전별이다.
- From이 정확히 `no-reply@earth-trips.com`인지 확인한다.
- sandbox라면 실제 recipient도 검증하거나 simulator를 사용한다.
- production access 승인 여부를 확인한다.

### `Configuration set does not exist`

- 서울 리전에 `earth-trip-transactional`이 있는지 확인한다.
- 이름의 대소문자/하이픈을 환경변수와 대조한다.
- 긴급 복구 시 `SES_CONFIGURATION_SET`을 빈 값으로 두면 코드가 configuration set 없이 보내지만,
  SNS event publishing을 잃으므로 임시 조치로만 사용한다.

### `AccessDenied` 또는 credential 오류

- `aws sts get-caller-identity`로 실제 principal을 확인한다.
- IAM policy의 account ID, region, identity ARN을 확인한다.
- `SES_CONFIGURATION_SET`을 사용한다면 IAM Resource 배열에
  `arn:aws:ses:ap-northeast-2:<ACCOUNT_ID>:configuration-set/earth-trip-transactional`이 있는지
  확인한다.
- `ses:FromAddress` 조건이 실제 From과 같은지 확인한다.
- secret key 앞뒤 공백/따옴표와 systemd 환경 파일 읽기 권한을 확인한다.
- 키를 잃어버렸다면 조회하려 하지 말고 새 키 생성 → 서버 교체 → 구 키 비활성화 → 사용 여부
  확인 → 삭제 순으로 회전한다.


## 13. 전환·관찰·되돌리기

1. SES domain/DKIM/MAIL FROM/production access/IAM/configuration set을 모두 완료한다.
2. simulator와 staging에서 네 가지 발신 흐름을 통과시킨다.
3. 운영 환경변수를 SES 값으로 바꾸고 backend를 재시작한다.
4. 24~48시간 동안 SES reputation, SNS bounce/complaint, 인증 완료율을 관찰한다.
5. Postmark token과 sender 설정은 SES 운영 확인 직후 바로 삭제하지 않는다. 짧은 관찰
   기간 동안 발송 경로에서는 사용하지 않는 비활성 상태로 보관하고, rollback 필요가 없음을 확인한
   뒤 회수한다.

되돌릴 때는 코드를 임의로 혼합하지 말고 배포 단위를 기준으로 한다. 발신 장애라면 먼저 SES
identity/region/IAM/configuration set 오류를 확인한다.

코드와 로컬 테스트 통과는 AWS 계정의 identity 검증, sandbox 해제, DNS 전파,
실제 메일 배달을 증명하지 않는다. 위 체크리스트의 simulator와 실제 메일 검증까지 끝나야 운영
전환이 완료된다.
