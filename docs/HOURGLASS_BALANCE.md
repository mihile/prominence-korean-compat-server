# Hourglass balance

Server compatibility for ProminentBase 4.1.1 and Eldritch End 0.3.4, guarded by native class SHA-256 pins.

- Faceless shadow-surge damage is multiplied by 3/8 (one eighth of player maximum health per hit before downstream damage handling).
- Hourglass boss HP uses the configured level-specific solo target times 0.8 times `1 + 0.8 * (actual players - 1)`, for primary and twin bosses. Original party-dependent damage remains unchanged.
- Newly spawned mobs in the challenge world get a persistent 0.8 maximum-health multiplier. Affix add waves use the boss single-player baseline with the same 0.8 reduction, independently of party size.
- During the Obsidilith fight, all mining fatigue is removed each challenge tick, including pre-existing effects and effects from other sources; Freezing Cold also stops reapplying it. Slowness and other boss fights are unchanged. The native rune block contains no mining-fatigue clearing logic.
- Existing mobs and bosses from challenges already in progress are not retroactively modified. Health attribute caps and effects from other mods can affect observed values.

Offline checks cover native Mixin transformation, class pins, 20 level/party combinations and exception-safe spawn context, plus cumulative updater checks. The outdated portal notice about a five-player health rule is suppressed without changing boss damage scaling. They do not establish in-game behavior. Installed server JAR is not changed while running; signed updater applies this build at a later normal start.
