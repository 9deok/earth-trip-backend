# Secret inventory and rotation runbook

실제 값은 이 문서나 Git에 기록하지 않는다. `/etc/earth-trip/earth-trip.env`는 root 소유,
group `earthtrip`, mode `0640`으로 관리한다.

| 논리 이름 | 저장 위치 | 회전 전 확인 | 회전 방식 |
| --- | --- | --- | --- |
| DB app/backup password | env, backup cnf | backup과 staging restore | 새 계정/암호 검증 후 구값 폐기 |
| wallet encryption key ring | env | 구 key 보존·복호화 검증 | 새 primary 추가, 재암호화 후 구 key 제거 |
| integration encryption key ring | env | Calendar refresh 검증 | 새 primary 추가, token 재암호화 |
| push token encryption key | env | staging push 등록·해제 | maintenance에서 token 재등록 계획과 함께 회전 |
| Firebase service account | 별도 JSON | IAM 최소 권한 | 새 key 배포·검증 후 구 key 폐기 |
| AWS SES IAM key/Amadeus/Maps/MOFA | env | provider staging 호출 | 제공자 콘솔에서 새 값 발급·최소 권한·제한 확인 |
| internal/webhook secret | env | 상대 시스템 동시 변경 창 | 양쪽을 같은 maintenance 창에 교체 |
| deploy SSH key | GitHub environment | known_hosts·sudo 범위 | 새 public key 추가, workflow 검증 후 구 key 제거 |

## 공통 절차

1. 회전 담당자, 대상, 영향 endpoint, rollback 값을 비밀 관리 시스템에 기록한다.
2. 새 값을 staging에서 먼저 검증한다.
3. 운영 환경 파일을 원자적으로 교체하고 `systemctl restart earth-trip`을 실행한다.
4. readiness만이 아니라 해당 provider의 실제 진단 endpoint를 확인한다.
5. 실패하면 구 값을 복구하고 원인을 기록한다.
6. 성공 증거를 남긴 뒤 제공자 콘솔의 구 credential을 폐기한다.

키 ring 값은 `new-primary:...,old-primary:...`처럼 구 key를 함께 두는 기간이 필요하다.
DB backup과 암호화된 파일은 사용 당시 key 없이는 복구할 수 있으므로 retention보다 먼저
구 key를 없애지 않는다.
