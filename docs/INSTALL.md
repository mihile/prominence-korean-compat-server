# Windows 서버 설치와 운영

## 최초 설치

기존 `start.ps1`의 `while ($true)` / `RunJavaCommand` 기반 ServerPackCreator 자동 재시작 구조를 지원합니다. PowerShell 5.1+, Java 17+가 필요합니다. GitHub CLI나 토큰은 운영 서버에서 필요하지 않습니다.

1. 서버에 `stop`을 보내고, 현재 시작 창에서 **Ctrl+C로 자동 재시작 스크립트도 종료**합니다. 이미 실행 중인 PowerShell은 디스크의 새 스크립트를 다시 읽지 않습니다.
2. 최신 `server-updater-bootstrap.zip`을 서버 루트에 풀어줍니다. 아직 `mods`는 바뀌지 않습니다.
3. 서버 폴더에서 다음 명령을 실행합니다.

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\Install-ServerUpdater.ps1
```

4. 기존 `start.bat`을 실행합니다. 첫 기동 전 번들에 든 서명된 JAR로 이전 이름을 서버 이름으로 바꿉니다. 오프라인이어도 첫 번들 설치가 가능하며, 온라인이면 더 최신의 검증된 서버 빌드를 먼저 사용할 수 있습니다.
5. `Server compatibility build ... startup confirmed` 로그를 확인합니다. 새 버전은 `SERVER_STARTED` 후 60초 이상 실제 서버 틱이 진행되어야 정상 확인됩니다.

시작 스크립트는 백업 후 두 부분만 추가합니다. Java 경로·힙·GC·GUI·기존 5초 재시작 설정은 유지합니다. 기존 두 호환 JAR이 모두 있으면 자동으로 어느 하나를 삭제하지 않고 멈춥니다. 다른 이름으로 수동 복사한 같은 mod ID JAR도 함께 설치하지 마세요.

## 운영과 실패 복구

- `mods/prominence-korean-compat-server-1.0.0.jar`: 현재 서버 패치. 수동 교체는 서버와 시작 스크립트를 모두 종료한 상태에서만 합니다.
- `.prominence-server-update/backups/<SHA256>.jar`: 이름 변경 전/업데이트 전 원본.
- `.prominence-server-update/pending.properties`: 교체·최초 기동 확인 대기 기록. **문제 해결 목적으로 임의 삭제하지 마세요.**
- 준비가 완료됐지만 아직 서버를 켜지 않았다면 `ready=true`만 기록됩니다. 실제 다음 시작 직전에 `attempted=true`가 기록되며, 그 뒤 정상 확인 없이 종료된 경우에만 기동 실패로 복구합니다.
- `.prominence-server-update/healthy.properties`: 해당 실행의 무작위 토큰·빌드·해시를 확인한 기동 영수증. 다음 시작 전 반영·정리됩니다.
- `.prominence-server-update/ledger.properties`: 정상 확인한 최대 빌드와 실패해 차단한 빌드.
- `.prominence-server-update/updater.log`: 업데이트·복구·안전 중단 로그. 1MiB가 넘으면 이전 로그 하나를 남깁니다.
- `.prominence-server-update/bootstrap.jar`: 첫 설치 및 설치 JAR 손상 시 사용할 구조용 업데이트 엔진. 시작 훅의 고정 SHA256으로 검사합니다. 일반 업데이트 때 삭제하지 않습니다.

정상 기동 후 60초 전에 stop하거나 강제 종료하면 다음 시작에 이전 JAR로 돌아가고 해당 빌드를 차단합니다. 이는 불확실한 기동을 정상으로 인정하지 않는 보수적 규칙입니다. 장애 원인이 별도 설정/의존성이었다면 원인을 해결하고 서버·시작 루프가 종료된 상태에서 검토 후 해당 `blocked.<빌드>` 항목만 해제할 수 있습니다. 자동으로 차단을 해제하지 않습니다.

SHA가 맞지 않는 백업, 복구 중 수동 JAR 교체, 중복 JAR, 링크/정션 경로, 이미 사용 중인 서버 포트, 다른 시작 스크립트 점유는 안전 중단합니다. 종료 코드 20이면 콘솔과 `updater.log`를 확인하세요. 월드/플레이어/퀘스트 데이터까지 롤백하는 기능은 아닙니다. 업데이트 JAR은 월드 형식을 변경하는 작업을 자동 배포에 포함하면 안 됩니다.

자동 온라인 확인만 끄려면 `.prominence-server-update/config.properties`에 다음 한 줄을 넣습니다. 미설정 기본값은 `true`입니다. 진행 중인 복구 기록 처리는 끄지 않습니다. 첫 이름 이전용 로컬 서명 번들도 복구/설치 안전을 위해 계속 사용합니다.

```properties
enabled=false
```

다시 켜려면 `enabled=true`로 바꿉니다. 네트워크 실패 뒤 즉시 재확인이 꼭 필요하면 서버와 시작 루프를 종료하고 `network.properties`의 `retryAfter`를 `0`으로 바꿀 수 있습니다. 운영 서버는 공개 HTTPS 다운로드만 사용하며 GitHub 토큰이나 개인 서명키를 저장하지 않습니다.

## 수동 훅 통합

설치 도구가 알 수 없는 시작 스크립트라고 중단하면 현재 스크립트 구조를 검토해 다음을 연결합니다. 시작 시 한 번:

```powershell
. (Join-Path $BaseDir 'maintenance/ServerUpdate.ps1')
Initialize-ProminenceServerUpdate $BaseDir
```

매 Minecraft JVM 실행 직전 `Invoke-ProminenceServerUpdate $Java`를 호출합니다. 이 함수가 예외를 던지면 **다음 JVM을 실행하지 않고 시작 스크립트 전체를 종료**해야 합니다. Java/JVM 옵션 및 서버 실행 명령 자체는 바꾸지 않습니다. Unix `start.sh`는 이 릴리스의 자동 훅 대상이 아닙니다.
