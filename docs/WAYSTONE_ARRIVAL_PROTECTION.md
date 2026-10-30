# 웜홀·웨이스톤 도착 보호

서버 build2026103001은 Fabric Waystones3.3.3의 실제 이동 직전부터 피해를 막고, 갱신된 클라이언트의 도착 화면 준비 신호 뒤60ticks(정상20TPS에서3초)까지 보호한다. 일반 웨이스톤과 포켓 웜홀의 공통 경로에 적용한다. 다른 모드·명령어 이동이나 첫 로그인 전체에 무조건 적용하지 않는다.

클라이언트 nightly2026103005가 도착 차원·청크·로딩 화면 종료와 정상 렌더링3프레임을 확인한다. 현재 token과 다른 완료/중복 신호는 시간을 연장하지 않는다. 응답이 없으면60초 대기와3초 유예로 끝나며, 구클라이언트는 도착부터10초 고정 보호다. 구서버에는 이 서버 모듈이 없어 새 클라이언트만으로 피해를 막을 수 없다.

몹 공격·투사체·마법·화염·낙하 등 native player damage를 막는다. out_of_world와 generic_kill은 보존한다. 이동 실패·리스폰·disconnect·서버 종료와 만료를 정리하며 저장 NBT·abilities·전역 gamerule을 변경하지 않는다. weak player 상태만 보관한다. 새 thread나 청크 로딩은 추가하지 않는다.

소스는 `src/compat/arrival-protection`이다. `tools/integrate_arrival.py`로 최신 누적 서버 JAR에 먼저 병합하고 기존 `tools/build.py --preserve-updater`로 내부 build만 올린다. 파일명과 mod version1.0.0은 유지한다. 클라이언트 소스 저장소와 공통11클래스가 byte-identical이며 서버 JAR에는 클라이언트 렌더 코드가 없다.

검증: dedicated SERVER와 CLIENT 실제 native2타깃211메서드 Mixin/ASM, 정책3,027개·guard6개, 기존182서버클래스 보존, updater118개 검사. 실제 운영 전투/텔레포트는 다음 정상 시작 후 검수 대상이다. 기존 updater가 다음 정상 시작 전에 받을 수 있으며 운영 JAR 직접 교체나 서버 강제 종료·재시작은 하지 않는다.

[클라이언트 상세 구현](https://github.com/mihile/prominence-korean-compat/blob/nightly/docs/WAYSTONE_ARRIVAL_PROTECTION.md)
