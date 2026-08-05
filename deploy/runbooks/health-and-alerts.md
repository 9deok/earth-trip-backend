# Health, disk, certificate, backup runbook

## 정상 판정

다음 네 신호를 서로 대체하지 않고 모두 확인한다.

1. `systemctl is-active earth-trip`이 `active`
2. `curl --fail http://127.0.0.1:8080/actuator/health/readiness` 성공
3. 공개 HTTPS의 `/actuator/health/readiness` 성공
4. 인증된 staging 기기에서 REST 호출과 WebSocket 재연결 성공

```bash
sudo systemctl status earth-trip --no-pager
sudo journalctl -u earth-trip -n 200 --no-pager
curl --fail --silent --show-error http://127.0.0.1:8080/actuator/health/readiness
curl --fail --silent --show-error https://earth-trip.example.com/actuator/health/readiness
sudo nginx -t
sudo ss -lntp
```

## 경보 기준

- readiness 2회 연속 실패 또는 5분 이상 실패
- systemd 10분 내 재시작 3회 이상
- `/var/lib/earth-trip`, MariaDB data, backup volume 중 사용률 80% 경고·90% 긴급
- 인증서 만료 30일 경고·14일 긴급
- 마지막 성공 backup 30시간 초과 또는 checksum/off-server 복사 실패
- push delivery outbox의 장기 실패·invalid token 비율 급증

## 순서

1. 사용자 영향과 시작 시각을 기록한다.
2. readiness, process, MariaDB, Nginx 순서로 경계를 좁힌다.
3. 최근 release 직후라면 배포 디렉터리의 `DEPLOYMENT`와 journal을 확인한다.
4. 데이터 손상 근거가 없으면 DB restore부터 실행하지 않는다.
5. 해결 후 공개 health, 로그인, 여행 bootstrap, 변경 피드, WebSocket을 재검증한다.

로그와 화면에 access/refresh token, 예약번호, 이메일 원문, 환경변수 본문을 남기지 않는다.
