# Prominence Korean Compat Server

Prominence II **4.1.3 / Minecraft 1.20.1 / Fabric** 전용 서버 호환 패치입니다.

서버 파일명은 **`prominence-korean-compat-server-1.0.0.jar`**입니다. [클라이언트 패치](https://github.com/mihile/prominence-korean-compat)와 저장소·서명키·업데이트 피드를 분리합니다. 기존 모드 ID `prominence_korean_compat`와 모드 버전 `1.0.0`은 통신과 기존 호환을 위해 유지하고, 내부 빌드 번호로 새 배포를 구분합니다.

[최신 릴리스](https://github.com/mihile/prominence-korean-compat-server/releases/latest) · [설치 안내](docs/INSTALL.md) · [관리자 빌드·배포](docs/MAINTAINER.md) · [검증 범위](docs/VALIDATION.md)

## 설치와 동작

처음에는 **Minecraft와 기존 자동 재시작 스크립트를 모두 종료**하고 릴리스의 `server-updater-bootstrap.zip`을 서버 폴더에 풀어 `Install-ServerUpdater.ps1`을 실행합니다. 이후 기존 `start.bat`으로 시작합니다. 상세 명령과 수동 설치는 [설치 안내](docs/INSTALL.md)를 따릅니다.

서버를 시작할 때, 그리고 기존 자동 재시작 루프가 다음 JVM을 실행하기 직전에 GitHub의 서명된 최신 배포 정보를 확인합니다. 새 빌드가 있으면 검증·백업 후 **서버 JVM 실행 전에** JAR 한 개만 교체합니다. 실행 중 서버를 강제로 종료하거나 업데이트를 위해 추가 재시작하지 않습니다.

- 네트워크·다운로드·서명·크기·SHA-256 오류: 기존 JAR로 시작합니다. 확인 실패 후 5분 동안 네트워크 재시도를 쉬며 전체 통신 기한은 45초입니다.
- 첫 설치 이름 변경: 이전 `prominence-korean-compat-1.0.0.jar`를 백업한 다음 서버 이름으로 이전합니다. 같은 mod ID 두 JAR을 함께 로드하지 않습니다.
- 새 빌드 기동 실패: 다음 시작 전 이전 JAR을 복구하고 해당 빌드를 차단합니다. 더 높은 빌드의 수정판은 받을 수 있습니다.
- 정상 확인: 실제 `SERVER_STARTED` 이후 60초가 지나 서버 틱이 진행되어야 확인됩니다. **그 전에 정상 stop/강제 종료해도 보수적으로 이전 버전으로 복구**합니다.
- 교체 중 전원 차단·프로세스 종료: 먼저 쓴 복구 기록과 해시 검증한 백업으로 다음 시작 전에 복구합니다.
- 백업 손상·외부 수동 교체·중복 설치 등 안전하게 판단할 수 없는 상태: 자동 재시작 루프 전체를 종료 코드 20으로 멈춥니다. 해당 파일을 덮어쓰거나 무한 재기동하지 않습니다.

## 포함 범위

기존 서버 누적 호환 모듈을 보존합니다. 화살통/화살 스킬, 대장장이·배낭 작업대, 스킬 프리셋, 삼지창 반환, BetterEnd 높이 캐시, 구조물 메모리, 장신구 이력, 전기톱/TreeChop, Testosterone 액체 조회의 불필요한 청크 유지 수정을 포함합니다. 모듈별 대상 버전과 가드는 기존대로 유지합니다.

자동 업데이트는 서버 호환 JAR 한 개만 대상으로 합니다. 다른 모드, Voxy/VoxyServer, 리소스팩, 퀘스트, 설정, Java/JVM 옵션과 월드는 이 피드로 바꾸지 않습니다. 서버가 정상 실행되던 기존 구성과 필요한 외부 모드는 별도로 설치되어 있어야 합니다. 클라이언트 JAR과 서버 JAR을 함께 설치하지 마세요.

## 소스와 라이선스

`src`는 새 서버 업데이트 모듈(MIT)입니다. `base/server-compat-before-updater.jar`는 빌드 기준 누적 서버 패치이며 기존 소스·출처·라이선스를 포함합니다. `legacy-sources`에는 그 JAR에 포함되어 있던 소스를 경로 그대로 보존했습니다. Testosterone fluid-check 모듈은 **GPL-3.0-only**이며 원저자 표기를 보존합니다. 관련 전문은 `LICENSES/GPL-3.0-only.txt`에 있습니다. 각 기존 모듈의 라이선스와 출처를 하나의 MIT 표기로 대체하지 않습니다.

이 업데이트 기능의 오프라인 검증은 완료했지만 실제 운영 서버의 첫 로드와 월드 동작은 별도 검수 대상입니다.
