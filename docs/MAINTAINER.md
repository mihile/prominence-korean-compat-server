# 서버 패치 빌드와 배포

2026-10-26 유지보수 지침: 검증된 변경이 서버 통합 JAR 하나의 교체로 끝나면 최신 누적본에 통합해 이 저장소의 서명된 정식 릴리스/피드로 공개합니다. 과거 서버 준비본만 유지 조건은 폐기됐으며 게시 승인을 다시 요청하지 않습니다. 기존 updater가 다음 정상 시작 직전에 적용합니다. 실행 중 설치본 직접 교체·강제 종료·재시작이나 설정/월드/시작 훅/다른 모드 변경으로 확대하지 않습니다. JAR 단독 변경마다 별도 수동 ZIP을 만들지 않으며 비JAR 배포가 필요하면 그 범위를 명시합니다.

클라이언트 저장소와 별도 작업입니다. 파일명 `prominence-korean-compat-server-1.0.0.jar`, mod ID `prominence_korean_compat`, mod version `1.0.0`을 유지하고 **매 배포마다 내부 build를 증가**시킵니다. 공개한 빌드의 JAR/서명을 바꾸어 재사용하지 않습니다.

최신 누적 서버 JAR을 base로 사용합니다. 클라이언트 JAR, 예전 운영 JAR 또는 예전 ZIP으로 바꾸면 안 됩니다. 첫 기준은 Testosterone 수정이 포함된 `base/server-compat-before-updater.jar`입니다. 이후에는 직전 서버 릴리스 JAR에 새 서버 수정부터 통합한 결과를 base로 사용합니다. 새 업데이트 모듈 이외의 기존 클래스·가드·믹신·진입점을 보존합니다. 클라이언트 전용 클래스/의존성을 통합하지 않습니다.

## 로컬 준비

Python 3, JDK 17+, Git/GitHub CLI가 필요합니다. `classpath.json`은 현재 서버의 Minecraft intermediary, Fabric Loader와 Fabric API lifecycle 및 필요한 라이브러리 경로의 JSON 배열입니다. 개인 절대 경로이므로 커밋하지 않습니다.

```text
python tools/build.py --base base/server-compat-before-updater.jar --classpath classpath.json --java-home JDK_HOME --build 2026102501 --output build
```

JDK17 대상으로 컴파일합니다. 모듈 라이선스 전문·원저자·내장 소스·self guard 지문을 보존하고 검사합니다. 누적 모듈의 버전 가드를 무작정 넓히지 않습니다.

Just Hammers 서버 인챈트 모듈은 [별도 빌드·검증 절차](HAMMER_ENCHANTMENTS.md)를 따릅니다. `src/compat/java`의 모듈을 최신 공개 누적 JAR에 먼저 통합한 결과를 updater 빌드의 base로 사용합니다. 이후 릴리스도 이 추가 모듈을 보존합니다.

정교한 배낭의 패널 내 재단조는 [별도 절차](BACKPACK_INLINE_REFORGE.md)를 따릅니다. `src/compat/backpack-inline`은 서버 공통 코드만 담으며, 최신 서버 누적 JAR에 추가한 결과를 updater 빌드의 base로 사용합니다. 클라이언트 전용 화면 코드를 포함하지 않습니다.

```text
python tools/verify.py --build-dir build --classpath classpath.json --java-home JDK_HOME
```

검증 도구는 임시 파일/짧은 Java 프로세스만 사용합니다. Minecraft·서버·월드는 실행하지 않습니다. 실제 운영 서버 로드/월드 동작은 따로 기록합니다.

## 서명 및 공개

서버용 Ed25519 개인키는 저장소 밖의 안전한 위치에서 유지합니다. 최초 작업 PC의 경로는 사용자 `.codex/keys/prominence-korean-compat-server/update-ed25519.pk8`입니다. 클라이언트 키와 별개입니다. 재생성/덮어쓰기하지 않습니다. `public-key.der`만 배포합니다. 개인키·토큰·개인 설정·월드·로그·classpath를 업로드하지 않습니다.

```text
python tools/publish.py --build-dir build --signing-key PRIVATE_KEY_OUTSIDE_REPO --java-home JDK_HOME --tag build-20261025.1 --notes-file releases/build-20261025.1.md --prepare-only
```

검토 후 소스를 커밋·푸시하고 `--prepare-only`를 빼고 실행합니다. 릴리스 target은 전체 40자리 source commit SHA입니다. `server-update.properties`는 서버 채널·팩·Minecraft·파일명·build·SHA256·크기·고정 저장소 URL을 서명합니다. 일반 정식 릴리스 `latest`만 운영 피드입니다. draft/prerelease를 자동 업데이트 대상으로 쓰지 않습니다.

배포 후 공개 피드·JAR·SHA256SUMS를 다시 받아 `VerifyRelease`로 서명과 파일을 확인합니다. 첫 배포에 이어 실제 공개 구버전→신버전 교체와 실패 복구 실험은 **작은 임시 fixture**에서 실행하고 게임 서버를 띄우지 않습니다.

새 `server-updater-bootstrap.zip`은 신규 설치용입니다. 이미 설치된 서버는 서명된 JAR 한 개만 업데이트합니다. 시작 훅/프로토콜 자체에 변경이 필요한 릴리스는 자동 JAR 업데이트로 충분한지 검토하고, 필요하면 서버/기존 시작 스크립트 종료 후 부트스트랩 수동 업그레이드를 안내합니다. 구조용 bootstrap SHA는 훅과 함께 업데이트해야 합니다.

## 복구 규칙과 범위

서버를 켜기 전에 관리자가 미리 적용할 때는 서버와 시작 스크립트를 모두 종료한 상태에서 검증한 새 JAR을 **mods 밖의 임시 runner 경로**에 두고 `java -cp RUNNER_JAR local.prominence.serverupdate.UpdateEngine --prepare SERVER_ROOT`를 사용합니다. `--prepare`는 적용·백업 후 실제 기동 시도를 기록하지 않습니다. 기본 모드(서버 root 인자만 지정)는 바로 다음 Minecraft 실행을 전제로 시도를 기록하므로 적용만 해두는 작업에 쓰면 안 됩니다. 첫 실제 시작 훅이 대기 준비본을 유지하고 시도를 기록합니다. 정상 확인 영수증을 관리자가 대신 작성하지 않습니다.

트랜잭션은 기존 JAR 백업·fsync·해시 검증 → 복구 기록을 먼저 기록 → 새 canonical 이름으로 교체 → 옛 이름 제거 순서입니다. 무작위 실행 토큰과 build/해시가 맞고 `SERVER_STARTED` 후 60초 이상 틱이 진행돼야 정상 확인합니다. 실패 시 이전 JAR을 복구하고 같은 build를 차단합니다. 더 높은 build로만 자동 진행합니다.

서명 오류/연결 실패는 재부팅을 유발하지 않습니다. 기존 JAR로 시작하며 확인 실패 시 5분 쿨다운합니다. 판단 불가 상태에서는 launcher 전체를 종료 코드20으로 중단합니다. 설정·월드·다른 JAR까지 자동 롤백하거나, 오래된 사용자 삭제 모드를 되살리는 작업을 포함하지 않습니다.
