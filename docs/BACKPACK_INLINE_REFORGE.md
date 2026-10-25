# 배낭 모루 패널 안에서 재단조

정교한 배낭 모루 탭의 재단조 버튼을 누르면 같은 오른쪽 패널에서 장비·재료·첨가제를 넣고 바로 재단조할 수 있습니다. 별도 전체 화면으로 이동하지 않습니다. 일반 모루 입력과 재단조 입력을 따로 저장하며 원래 Tierify의 조건·확률·소모·소리를 사용합니다.

멀티플레이에서는 클라이언트 build2026102516 이상(현재 공개 클라이언트 build2026102601 포함)과 서버 build2026102601 이상이 함께 필요합니다. 구형 상대와는 기존 슬롯 구성을 유지하며 버튼을 비활성화합니다. 지원 확인 직전에 배낭을 열었다면 다시 열어주세요. 일반 모루와 높은 레벨 비용을 지불하는 기존 모루 호환은 유지합니다.

서버 build2026102602 / 클라이언트 build2026102603은 탭을 바꾼 뒤에도 서버에 남는 `UpgradeContainerBase.isOpen` 값 대신 현재 배낭의 `getOpenContainer()`와 업그레이드 객체를 검사합니다. 이전 구현은 배낭을 연 뒤 모루 탭을 선택하면 재단조 전환을 거절할 수 있었습니다. 전환·단조·준비 여부 갱신·숨긴 슬롯 판정도 같은 현재 선택 기준으로 수정했습니다. 서버 수정이 필요하며 클라이언트만 갱신하면 기존 서버의 전환 거절은 고쳐지지 않습니다.

`tools/verify_inline_selection.py`는 실제 Core의 선택/플래그 여섯 메서드와 production receiver/state를 데이터 전용 환경에서 실행합니다. `--baseline`에서 이전 공개 JAR의 거절을 재현하고 수정본33검사에서 전환/회신/재료 슬롯/단조/동일 틱 제한/닫힌 탭/이전 배낭/접근/지원 여부를 검증합니다. 실제 서버·월드·GUI는 실행하지 않습니다.

## 유지보수와 검증

- `src/compat/backpack-inline/java`: 서버/공통 13개 소스, 14개 클래스. 클라이언트 UI 코드는 포함하지 않습니다.
- Minecraft1.20.1 / Sophisticated Backpacks `1.20.1-3.23.4.5.110` / Core `1.20.1-1.2.7.15.166` / Tierify1.2.0 / Porting Lib Transfer2.3.13 / MixinExtras0.5.4의 버전·원본 클래스 지문 가드를 유지합니다.
- HELLO/ACK와 화면 열기 순서를 서버 스레드에서 처리하고 서버가 현재 배낭/플레이어/거리/패널/sync ID/재료 조건을 검증합니다. 숨긴 슬롯의 직접 클릭·빠른 이동·드래그·모두 줍기와 같은 틱 중복 재단조를 막습니다.
- `tools/integrate_backpack_inline.py`는 최신 공개 서버 JAR에 common 모듈만 추가하고 모든 이전 비메타데이터 엔트리를 보존합니다. 이미 이 모듈이 있는 base라면 자동 덮어쓰지 않고 명시적인 교체 검토를 요구합니다.
- 이번 두 클래스 교체에는 `--replace-tab-selection`을 명시합니다. 기존14클래스와 리소스/소스 집합을 확인하고 InlineReforge/InlineState 두 클래스와 해당 내장 소스만 달라질 수 있도록 검사합니다.
- 빌드용 classpath에는 기존 서버 intermediary/Loader/Fabric/Porting 의존성과 설치된 Backpacks/Core/Tierify JAR이 필요합니다. 개인 절대 경로 목록은 커밋하지 않습니다.

```text
python tools/integrate_backpack_inline.py --base LATEST_SERVER_JAR --classpath BACKPACK_CLASSPATH_JSON --java-home JDK_HOME --output build/backpack-release-base.jar
python tools/build.py --base build/backpack-release-base.jar --classpath BACKPACK_CLASSPATH_JSON --java-home JDK_HOME --build NEW_BUILD --preserve-updater --output build/backpack-release
python tools/verify.py --build-dir build/backpack-release --classpath BACKPACK_CLASSPATH_JSON --java-home JDK_HOME
python tools/verify_inline_native.py --jar build/backpack-release/prominence-korean-compat-server-1.0.0.jar --classpath BACKPACK_CLASSPATH_JSON --java-home JDK_HOME --output build/backpack-release/native
```

서버 기준 native 6대상398메서드의 실제 Mixin 변환·ASM 검증, handshake/slot 접근과 8개 호출 위치를 확인하고 Tierify 원래 계산·소모·닫기5개 메서드 호출열을 보존합니다. updater118검사도 통과했습니다. 기존 클라이언트 준비 당시에는 native7대상414메서드와 원래 Porting/Fabric 데이터 저장 fixture도 통과했습니다. 오프라인 검증은 실제 멀티플레이 GUI 조작을 대신하지 않습니다.

2026-10-26 사용자 요청으로 서버 준비본만 유지 조건은 폐기됐습니다. 검증된 서버 JAR은 서명된 정식 GitHub 릴리스로 공개하며 기존 updater가 다음 서버 시작 직전에 적용합니다. 실행 중 서버 파일을 직접 교체하거나 서버를 강제로 재시작하지 않습니다.
