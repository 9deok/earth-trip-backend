# Disaster recovery runbook

## 복구 목표와 준비물

실제 RPO/RTO는 운영 전에 확정한다. 복구 세트에는 DB dump, object/export 파일, 적용 당시
wallet/integration/push 암호화 key, 환경변수 inventory, 배포 JAR SHA-256이 함께 있어야 한다.

## 복구 순서

1. 장애 시각, 마지막 정상 시각, 마지막 off-server backup을 기록한다.
2. 원본 VM을 덮어쓰기 전에 새 격리 VM 또는 임시 DB에서 backup checksum을 확인한다.
3. Java 21, MariaDB 호환 version, Nginx와 전용 계정·directory를 준비한다.
4. `earth-trip.service`를 중지한 상태에서만 복구 스크립트를 실행한다.

```bash
sudo systemctl stop earth-trip
sudo EARTH_TRIP_RESTORE_CONFIRM=restore-earth-trip \
  /usr/local/sbin/restore-earth-trip-mariadb \
  /var/backups/earth-trip/mariadb/earth-trip-YYYYMMDDTHHMMSSZ.sql.gz
```

5. `flyway_schema_history`의 success/checksum과 핵심 table row count를 기록한다.
6. object/export 파일을 private 경로에 복구하고 owner/mode를 확인한다.
7. 복구한 JAR의 `SHA256SUMS`를 검증한 뒤 서비스를 시작한다.
8. loopback readiness, 공개 HTTPS, 로그인, 여행 bootstrap, 파일 다운로드, 변경 피드,
   WebSocket, push staging delivery를 확인한다.
9. DNS 또는 OCI reserved IP 전환은 검증이 끝난 뒤 수행한다.

복구 연습은 최소 월 1회 임시 DB에서 실행하고
[`restore-record.example.md`](restore-record.example.md)를 복사해 증거를 남긴다.
