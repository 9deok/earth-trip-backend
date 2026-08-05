# OCI Rocky Linux deployment

이 디렉터리는 기존 OCI Rocky Linux VM에서 Earth Trip을 Java 21 단일 process로 실행하기 위한
검토 가능한 운영 원본이다. placeholder를 실제 값으로 바꾸기 전에는 그대로 설치하지 않는다.

AWS SES 발송 설정은
[`aws/ses/README.md`](aws/ses/README.md)를 따른다.

## 1. 변경 전 확인

```bash
cat /etc/rocky-release
java -version
mariadb --version
sudo systemctl status mariadb nginx --no-pager
sudo nginx -T
sudo ss -lntp
getenforce
sudo firewall-cmd --list-all
df -h
```

기존 Nginx virtual host, MariaDB database, 80/443 listener와 인증서를 먼저 기록한다. 8080과
3306은 OCI NSG/Security List 또는 public firewalld에 열지 않는다.

## 2. 계정과 경로

```bash
sudo useradd --system --home-dir /opt/earth-trip --shell /sbin/nologin earthtrip
sudo install -d -m 0755 -o root -g root /opt/earth-trip/releases
sudo install -d -m 0750 -o earthtrip -g earthtrip /opt/earth-trip/logs
sudo install -d -m 0755 -o root -g root /opt/earth-trip/website/releases
sudo install -d -m 0750 -o earthtrip -g earthtrip /var/lib/earth-trip/objects
sudo install -d -m 0750 -o earthtrip -g earthtrip /var/lib/earth-trip/exports
sudo install -d -m 0750 -o root -g earthtrip /etc/earth-trip
sudo install -d -m 0700 -o root -g root /var/backups/earth-trip/mariadb
```

이미 존재하는 계정·경로라면 소유권을 바꾸기 전에 현재 용도를 확인한다.

## 3. 환경과 DB client 파일

`config/earth-trip.env.example`을 서버의 `/etc/earth-trip/earth-trip.env`로 복사하고 실제
credential을 서버에서만 입력한다. mode는 `0640`, owner는 `root:earthtrip`으로 둔다.
Backend 로그는 JAR 위치의 `logs/service.log`에 기록된다. 배포 release가 바뀌어도
`/opt/earth-trip/logs`를 공유하며, Logback이 일 단위 압축 파일을 7일까지만 보관한다.
Calendar callback 값은 코드 endpoint를 추측하지 말고 Google에 등록한 redirect URI와 Flutter
`GOOGLE_CALENDAR_REDIRECT_URI`에 사용한 정확한 문자열을 넣는다.

backup용 MariaDB account에는 대상 schema를 dump하는 데 필요한 최소 권한만 주고,
`config/mariadb-backup.cnf.example` 형식의 `/etc/earth-trip/mariadb-backup.cnf`를 root mode
`0600`으로 만든다. password를 shell 인자나 systemd unit에 쓰지 않는다.

## 4. 운영 파일 설치

```bash
sudo install -m 0644 deploy/systemd/earth-trip.service /etc/systemd/system/
sudo install -m 0644 deploy/systemd/earth-trip-backup.service /etc/systemd/system/
sudo install -m 0644 deploy/systemd/earth-trip-backup.timer /etc/systemd/system/
sudo install -m 0755 deploy/scripts/deploy-earth-trip /usr/local/sbin/
sudo install -m 0755 deploy/scripts/backup-earth-trip-mariadb /usr/local/sbin/
sudo install -m 0755 deploy/scripts/restore-earth-trip-mariadb /usr/local/sbin/
sudo systemd-analyze verify /etc/systemd/system/earth-trip.service
sudo systemd-analyze verify /etc/systemd/system/earth-trip-backup.service
sudo systemctl daemon-reload
sudo systemctl enable --now earth-trip-backup.timer
sudo systemctl list-timers earth-trip-backup.timer
```

첫 backup을 수동 실행하고 `.sql.gz`, `.sha256`, `gzip --test`, off-server 복사를 확인한다.

## 5. Nginx와 HTTPS

`nginx/earth-trip.conf.example`을 별도 파일로 복사하고 `server_name`과 website root를 실제
값으로 바꾼다. 기존 설정을 덮어쓰지 않는다.

```bash
sudo install -m 0644 deploy/nginx/earth-trip.conf.example /etc/nginx/conf.d/earth-trip.conf
sudo nginx -t
sudo systemctl reload nginx
```

DNS A record가 OCI reserved public IP를 가리키고 OCI/firewalld의 80·443이 열린 뒤 Certbot을
실행한다. 설치 방식은 서버의 기존 Certbot 방식을 따른다.

```bash
sudo certbot --nginx -d earth-trip.example.com
sudo nginx -t
sudo systemctl reload nginx
sudo certbot renew --dry-run
```

SELinux AVC로 Nginx의 loopback proxy가 거부되는 근거가 있을 때만
`sudo setsebool -P httpd_can_network_connect 1`을 적용한다. SELinux를 끄거나 `chmod 777`을
사용하지 않는다.

## 6. 로컬에서 Backend 배포

현재 운영 JAR과 관련 runtime 파일은 다음 경로에 있다.

```text
/home/rocky/services/earth-trip/earth-trip-backend/
├── earth-trip-backend.jar
├── earth-trip.env
├── firebase-service-account.json
├── exports/
├── logs/
├── objects/
└── tmp-jar/                         # 배포 JAR 임시 업로드 경로
```

같은 `deploy-earth-trip` 파일을 서버의 `/usr/local/sbin/deploy-earth-trip`에 설치한 뒤 개발자
PC에서 다음 한 command로 테스트, 빌드, 전송, 운영 JAR 교체, `earth-trip.service` 재시작,
내부·공개 readiness 확인을 순서대로 실행한다. SSH private key와 host key는 저장소에 넣지 않고
개발자 PC의 `~/.ssh/config`와 `known_hosts`를 사용한다.
서버 deploy 계정은 `/usr/local/sbin/deploy-earth-trip`만 비밀번호 없이 `sudo`할 수 있어야 한다.

```bash
./deploy/scripts/deploy-earth-trip rocky@k8s-worker-02
```

`earth-trip-oci`는 다음처럼 개발자 PC의 `~/.ssh/config`에 등록한 Host 별칭이다. 실제 host와
key 경로는 로컬에만 둔다.

```sshconfig
Host earth-trip-oci
  HostName k8s-worker-02
  User rocky
  IdentityFile /absolute/path/to/earth-trip-deploy-key
  IdentitiesOnly yes
```

별칭 대신 target과 key를 직접 지정할 수도 있다.

```bash
./deploy/scripts/deploy-earth-trip rocky@k8s-worker-02 \
  --identity /absolute/path/to/earth-trip-deploy-key
```

기본값은 미커밋 변경이 있으면 배포를 중단한다. 현재 로컬 상태를 의도적으로 배포해야 할 때만
`--allow-dirty`를 사용하며, 이 경우 release ID에 `dirty`가 기록된다. SSH target과 공개
readiness URL은 환경변수로도 설정할 수 있다.

```bash
export EARTH_TRIP_DEPLOY_TARGET=earth-trip-oci
export EARTH_TRIP_PUBLIC_READINESS_URL=https://api.earth-trips.com/actuator/health/readiness
./deploy/scripts/deploy-earth-trip
```

전체 옵션은 다음 command로 확인한다.

```bash
./deploy/scripts/deploy-earth-trip --help
```

서버 배포기는 업로드된 JAR의 SHA-256을 다시 검증한 뒤 기존
`/home/rocky/services/earth-trip/earth-trip-backend/earth-trip-backend.jar`의 소유권과 mode를
보존해 새 JAR로 원자 교체하고 `systemctl restart earth-trip.service`를 실행한다. 새 process가
60초 안에 내부 readiness를 통과하지 못하면 기존 JAR을 복구하고 서비스를 다시 시작한다.
Flyway가 적용한 DB migration은 JAR rollback으로 되돌아가지 않으므로 migration을 포함한
release는 별도로 검토한다. 업로드 JAR은 같은 directory의 `tmp-jar/`에만 잠시 두고 배포 시도
후 삭제한다.

website는 `npm run lint`, `npm run typecheck`, `npm run build`를 통과한 `out/`만 versioned
directory에 복사하고 `/opt/earth-trip/website/current` symlink를 원자적으로 전환한다. public
viewer의 `/api/` 요청은 같은 Nginx origin에서 backend로 proxy된다.

## 7. 배포 후 검증

```bash
sudo systemctl status earth-trip --no-pager
sudo journalctl -u earth-trip -n 200 --no-pager
curl --fail http://127.0.0.1:8080/actuator/health/readiness
curl --fail https://api.earth-trips.com/actuator/health/readiness
```

그 다음 staging 계정으로 로그인, 여행 bootstrap, 변경 feed cursor, WebSocket CHANGE 수신,
파일 업로드·다운로드, Calendar 진단, push 등록·해제까지 확인한다. 자세한 장애·회전·복구 절차는
[`runbooks`](runbooks)에 있다.
