# 화살 틱 오류·Neruina UUID·로그 반복 제어

## 화살 틱 오류

운영 로그에서 Simply Bows 0.1.4의 EchoArrowEntity → Spell Engine 0.15.12의 PersistentProjectileEntityMixin.performImpacts → SpellHelper.projectileImpact → AreaImpact.combinedRadius 호출 중 `power` null 예외를 확인했습니다. 2026-10-26 00:58/01:31 등과 20:24/20:41/20:46/20:53에 같은 경로가 기록되어 있습니다. Neruina는 그 예외를 잡아 화살을 격리한 쪽입니다.

Spell Engine의 화살 Mixin은 위치만 지정한 빈 ImpactContext를 넘깁니다. 개별 피해 계산에서는 위력을 보완하지만 광역 반경 계산은 같은 null을 그대로 사용합니다. 서버의 물리 화살/PersistentProjectileEntity이며 광역 효과가 있고 위력만 빠졌을 때 원래 SpellPower.getSpellPower(spell.school, caster)로 채웁니다. 이미 위력이 있는 문맥, 비화살 투사체, 비광역 주문, 클라이언트는 원래 경로를 유지합니다. 위치·채널·거리 계수·타깃 모드 및 원래 치명타 계산을 보존합니다.

원본 Spell Engine 네 클래스와 Spell Power 한 클래스의 정확한 버전/지문에만 적용합니다. 원본 JAR은 변경하지 않습니다. Simply Bows를 제거하거나 화살 주문/광역 피해/보호 모드를 끄지 않습니다. 동반된 Projectile hit capture 오류는 숨기지 않습니다.

## Neruina 저장 기록

Neruina 2.3.1-beta.1의 TickingEntry.writeNbt는 `uuid`를 문자열로 쓰지만 fromNbt는 INT[] UUID로 읽습니다. 저장한 예외 기록이 있는 서버에서 재시작 시 Expected UUID-Tag ... INT[], ... STRING 오류가 생기는 원본 결함입니다.

정상 UUID 배열은 그대로 쓰고, 유효한 표준 UUID 문자열만 읽기용 복사본에서 정규화합니다. 새 저장 결과도 UUID 배열로 보완합니다. uuid/entityUuid 외의 필드와 실제 예외를 보존하고 잘못된 UUID를 임의 생성하지 않습니다. 기록을 삭제하거나 격리된 엔티티를 자동 재개/제거하지 않습니다. 운영 world/data/neruina.dat를 도구가 수정하지 않습니다. 조사 당시 현재 파일은 이미 tickingEntries가 빈 목록이었으며, 이전 읽기 실패 뒤 사라진 과거 기록을 복원했다고 주장하지 않습니다.

정확한 원본 Neruina 버전과 TickingEntry 지문에서만 적용합니다.

## 반복 로그

서버 전용 preLaunch가 Log4j 루트에 필터를 추가합니다. 운영 log4j 설정 파일이나 Voxy/Lithium 동작·최적화·캐시 설정은 변경하지 않습니다.

- `Lithium Class Analysis`의 정확한 NoClassDefFoundError 경고 중, Minecraft 클래스이고 원래 코드가 fallback을 사용한다고 명시한 메시지만 같은 문구를 5분 동안 중복 제거합니다. 최초 경고, 다른 클래스/원인 및 5분 뒤의 진단은 표시합니다. 이전 실행 로그에서 BaseBlock 520회, BaseLeavesBlock 36회 등 반복을 확인했습니다. Lithium의 fallback 반환 자체는 변경하지 않습니다.
- `Voxy` INFO의 정확한 `VoxyInstance` 월드 엔진 생성/유휴 종료 메시지 두 가지는 종류별 최초 및 5분 간격으로 표시합니다. 원본 코드상 정상 수명 관리 안내이며 오류 분기는 별도 ERROR입니다. 반복 열기/종료 비용이 개선됐다는 뜻은 아니며 실제 월드 작업/캐시는 그대로입니다.
- Throwable을 동반한 이벤트, ERROR, Voxy WARN, Neruina 예외, 알 수 없는 메시지, 다른 로거, 누락 attribute/구조물 pool/믹신 충돌은 그대로 표시합니다. 실제 오류를 통째로 숨기지 않습니다.

메시지 키는 문자열 최대 256개로 제한하며 엔티티/월드 객체를 보관하지 않습니다. 필터를 끄려면 사용자가 다음 실행의 JVM에 `-Dprominence.serverLogNoise.disabled=true`를 선택할 수 있습니다. 도구가 JVM 인자를 변경하지는 않습니다.

## 검증과 적용

- 원본 두 대상에 실제 Mixin 변환: 88개 메서드 ASM 검사, 원본/변경/누락 지문 15개 검사.
- 원본 AreaImpact의 null 예외를 재현하고 실제 SpellPower.getSpellPower/Result/ImpactContext/AreaImpact와 production helper를 실행: 16개 검사. 월드/속성 공급 부분은 작은 API fixture입니다.
- 실제 Minecraft NBT의 UUID 오류를 재현하여 교정·원본 보존·유효/무효 형식을 검사하고 실제 Log4j 루트/하위 로거 이벤트 보존까지 34개 검사.
- 기존 updater 118개 검사. 이전 누적 서버 클래스 131개와 모든 비메타데이터 엔트리를 보존했습니다. updater 바이트도 그대로입니다.

공개 빌드 2026102604는 기존 서명 updater가 다음 정상 서버 시작 직전에 적용합니다. 실행 중 JAR·월드·설정·다른 모드·기동 훅·클라이언트·배포 ZIP은 교체하지 않습니다. 실제 운영 전투와 다음 기동 로그는 적용 후 검수 대상입니다.

```text
python tools/integrate_arrow_tick.py --base LATEST_SERVER_JAR --output build/arrow-base.jar --classpath classpath.json --java-home JDK_HOME --mods SERVER_MODS
python tools/build.py --base build/arrow-base.jar --classpath classpath.json --java-home JDK_HOME --build 2026102604 --output build/arrow-release --preserve-updater
python tools/verify.py --build-dir build/arrow-release --classpath classpath.json --java-home JDK_HOME
python tools/verify_arrow_native.py --jar build/arrow-release/prominence-korean-compat-server-1.0.0.jar --classpath classpath.json --java-home JDK_HOME --output build/arrow-release-native
python tools/verify_arrow_power.py --jar build/arrow-release/prominence-korean-compat-server-1.0.0.jar --classpath classpath.json --java-home JDK_HOME --mods SERVER_MODS --output build/arrow-release-power
python tools/verify_arrow_transform.py --jar build/arrow-release/prominence-korean-compat-server-1.0.0.jar --classpath classpath.json --java-home JDK_HOME --mods SERVER_MODS --output build/arrow-release-transform
```
