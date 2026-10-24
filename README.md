# Prominence II 한국어 호환 패치 · 서버

Prominence II **4.1.3 · Minecraft 1.20.1 · Fabric** 전용 서버용 누적 호환 패치입니다.

**처음 한 번 설치하면, 다음 업데이트부터 서버를 켤 때 자동으로 확인하고 적용합니다.** 서버 파일명은 `prominence-korean-compat-server-1.0.0.jar`, 모드 버전은 `1.0.0`을 유지하며 빌드 번호로 업데이트를 구분합니다.

[최신 다운로드](https://github.com/mihile/prominence-korean-compat-server/releases/latest) · [업데이트 내역](https://github.com/mihile/prominence-korean-compat-server/releases) · [문제 제보](https://github.com/mihile/prominence-korean-compat-server/issues)

## 설치와 업데이트

1. 서버를 종료하고 **기존 자동 재시작 창도 완전히 닫습니다.**
2. 릴리스의 **`server-updater-bootstrap.zip`**을 서버 폴더에 풉니다.
3. 서버 폴더에서 `Install-ServerUpdater.ps1`을 실행한 뒤 기존 `start.bat`으로 시작합니다. 명령과 자세한 절차는 [설치 안내](docs/INSTALL.md)를 확인하세요.

첫 시작 전에 이전 `prominence-korean-compat-1.0.0.jar`을 백업하고 서버용 이름으로 이전합니다. 이후에는 **서버를 켤 때마다 새 빌드를 확인하고, Minecraft가 시작되기 전에 교체합니다.** 기존 stop·crash 후 자동 재시작에서도 같은 검사를 거칩니다. 업데이트를 위한 추가 재시작은 필요하지 않습니다.

인터넷 연결이나 다운로드에 문제가 생기면 기존 패치로 시작합니다. 새 버전의 기동에 실패하면 다음 시작 전에 이전 JAR로 복구하고, 같은 실패 빌드는 다시 설치하지 않습니다.

새 버전은 **기동 완료 후 60초 이상 서버 틱이 진행되어야 정상으로 확정**됩니다. 그 전에 정상 stop하거나 강제 종료해도 다음 시작에서 이전 버전으로 복구합니다.

자동 업데이트 범위는 **서버 호환 JAR 한 개**입니다. 서버를 이용하는 플레이어에게는 별도의 [클라이언트 패치](https://github.com/mihile/prominence-korean-compat)가 필요할 수 있습니다. 리소스팩·퀘스트·다른 모드는 별도 배포를 사용합니다. 두 용도의 호환 JAR을 같은 `mods` 폴더에 함께 넣지 마세요.

## 포함된 기능

<details>
<summary>장비와 제작 호환</summary>

화살통과 화살 스킬의 탄약 소모, 대장장이 장비 정보 보존, 정교한 배낭 작업대·모루·재단조 연결, 전기톱과 TreeChop의 서버 판정 호환을 보완합니다. 대상 모드 버전과 기존 호환 조건을 유지합니다.

</details>

<details>
<summary>플레이와 서버 실행 보완</summary>

스킬 프리셋, 삼지창 반환, 장신구 이력, BetterEnd 높이 캐시와 구조물 메모리 보완을 포함합니다. Testosterone의 불필요한 액체 조회가 청크를 계속 유지하는 문제도 보완합니다. Voxy/VoxyServer는 별도 모드이며 이 JAR의 자동 업데이트 대상에 포함되지 않습니다.

</details>

<details>
<summary>자동 업데이트의 검증과 백업</summary>

서버 전용 Ed25519 서명과 SHA-256·크기·팩·빌드를 확인합니다. 클라이언트와 저장소·서명키·업데이트 피드를 분리하며, 기존 모드 ID와 통신 식별자는 유지합니다.

백업과 적용 기록은 서버 폴더의 `.prominence-server-update`에 남습니다. 교체가 중단되면 기록과 백업으로 복구합니다. 아직 실행하지 않은 준비본을 기동 실패로 오인하지 않으며, 실제 기동에 실패한 빌드는 재설치를 차단합니다.

수동으로 변경된 파일, 손상된 백업, 중복 설치처럼 안전하게 판단할 수 없는 상태에서는 시작 루프를 멈춥니다. Java/JVM 옵션·다른 모드·설정·월드는 변경하지 않습니다. [운영과 복구 안내](docs/INSTALL.md)에서 확인할 수 있습니다.

</details>

## 개발과 배포

이 저장소의 `src`는 서버 자동 업데이트 모듈입니다. 누적 서버 호환 JAR에 기존 기능을 보존한 채 통합하며, 기존 소스·라이선스·출처도 유지합니다. Testosterone 액체 검사 모듈은 GPL-3.0-only이며 관련 전문은 [라이선스 파일](LICENSES/GPL-3.0-only.txt)에 있습니다.

[관리자 빌드·배포 절차](docs/MAINTAINER.md) · [검증 범위](docs/VALIDATION.md)
