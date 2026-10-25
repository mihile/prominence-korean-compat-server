# Just Hammers 채굴 인챈트 호환

대상은 Minecraft 1.20.1 / Just Hammers 20.1.5+mc1.20.1입니다. HammerItem과 원본 Fabric 플레이어 채굴 Mixin의 정확한 SHA-256 두 개를 확인한 뒤 서버에서만 활성화합니다. 원본 모드 JAR은 수정하지 않습니다.

## 원인과 처리

기존 HammerItem.findAndBreakNearBlocks는 주변 블록을 직접 드롭·경험치 처리한 뒤 공기로 바꿉니다. Architectury BREAK만 수동 호출하며 Fabric PlayerBlockBreakEvents.AFTER는 호출하지 않습니다. Vein Mining 1.5.0의 VeinMiningEvents와 Zenith Renewed 2.0.2의 EarthsBoonEnchant는 이 AFTER 이벤트를 사용합니다.

기존 causeAoe 호출 시 중심 위치와 타격 면만 기록하고, 중심의 tryBreakBlock이 성공한 뒤 범위 채굴을 수행합니다. 주변 블록마다 ServerPlayerInteractionManager.tryBreakBlock을 호출하여 실제 설치된 Fabric/보호 모드의 일반 채굴 경로를 통과합니다. 자체 드롭이나 인챈트 확률을 복제하지 않습니다. 주변 블록 드롭 위치는 일반 채굴 방식을 따릅니다.

- 중심 실패/취소 시 주변에 손대지 않습니다. 중심 좌표는 값으로 비교하여 재채굴하지 않습니다.
- 범위는 원본 getAreaOfEffect를 사용합니다. 다른 등급/크기의 해머도 그 범위를 따릅니다.
- 주변 채굴 도중 해머 AOE 재진입을 억제하고 finally에서 해제합니다. 베인 마이닝의 자체 재진입 방지도 그대로 사용합니다.
- 원본 canDestroy와 실제 도구 등급 검사를 유지합니다. 블록 엔티티/금지 태그/파괴 불가 블록을 추가로 허용하지 않습니다.
- 기존 내구도 예비량·웅크리기·크리에이티브·경고 문구를 유지합니다. 내구도는 일반 채굴 중 즉시 소비되어 연쇄 채굴 뒤 잔량도 다음 주변 블록에 반영됩니다.
- 손의 도구 교체/빈 스택/차원 이동 시 중단합니다. 주변 청크는 create=false로 이미 로드된 경우만 조회합니다.

## 달라지지 않는 발동 조건

베인 마이닝은 기존 활성화 키/웅크리기 설정과 광석·원목 허용 목록을 따릅니다. 일반 돌 전체를 무제한으로 연결 채굴하도록 바꾸지 않습니다. 기존 최대 개수/내구도/허기 설정도 유지합니다. 주변 블록의 정상 채굴 이벤트 역시 해당 조건을 검사합니다.

대지의 축복은 원래 돌 태그와 레벨당 1% 확률을 유지합니다. III는 해당 블록 하나마다 3%입니다. 드롭이 매 타격에 보장되는 인챈트가 아닙니다. 클라이언트 JAR 업데이트는 이 서버 채굴 수정에 필요하지 않습니다.

## 빌드와 검증

이전 공개 서버 JAR을 `BASE`로 사용합니다. 개인 classpath는 저장소 밖/무시된 파일로 유지합니다.

```text
python tools/integrate_hammers.py --base BASE --output build/hammer-base.jar --classpath classpath.json --java-home JDK_HOME --hammers ORIGINAL_HAMMERS_JAR
python tools/build.py --base build/hammer-base.jar --classpath classpath.json --java-home JDK_HOME --build 2026102504 --output build/hammer-release
python tools/verify.py --build-dir build/hammer-release --classpath classpath.json --java-home JDK_HOME
python tools/verify_hammer_transform.py --jar build/hammer-release/prominence-korean-compat-server-1.0.0.jar --classpath classpath.json --java-home JDK_HOME --hammers ORIGINAL_HAMMERS_JAR --output build/hammer-release-transform
python tools/verify_hammer_behavior.py --jar build/hammer-release/prominence-korean-compat-server-1.0.0.jar --java-home JDK_HOME --output build/hammer-release-behavior
```

원본 Just Hammers Mixin과 새 Mixin 세 개를 실제 Mixin 엔진에서 함께 변환하여 38개 메서드를 ASM 검증했습니다. 가드 8개 경우, production helper를 사용하는 독립 API fixture의 20개 경우, 기존 updater 118개 검사를 통과했습니다. fixture는 실제 보호 모드/인챈트 실행이나 운영 월드 시험을 대신하지 않습니다. 실제 게임/서버를 실행하지 않았으며 운영 채굴은 다음 재시작 후 확인해야 합니다.

서버 시작 로그: `Just Hammers enchantment compatibility: enabled`.

다음 서버 시작에서 서명된 build2026102504를 자동 적용합니다. 실행 중 서버 JAR과 클라이언트·월드·다른 모드·설정·서버 ZIP은 변경하지 않았습니다. 기존 Testosterone 등 이전 누적 모듈의 바이트를 보존했습니다.
