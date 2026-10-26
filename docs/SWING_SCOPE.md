# Server weapon animation scope

Build 2026102603 adds the guarded common swing-scope module. The installed Simply Swords calls weapon swing powers for ordinary player arm animations as well as authoritative Better Combat attacks. This can fire Livyatan waves and other shared swing powers while interacting or mining.

The module marks LivingEntity swing execution with a nested, finally-cleared thread-local scope and suppresses only server-player animation power dispatch when the verified Better Combat attack hook exists. Actual attacks, NPC swings, cooldowns and damage are preserved. Version/hash mismatch disables the mixins; missing transformed attack hook preserves original behavior. Client and server common module classes must match byte-for-byte. This needs no client protocol support for the server behavior correction.

Source and signed release publication are authorized. The live server is not changed or restarted; its existing updater selects this cumulative release before the next normal start. All 126 previous server classes are retained. CPU-only native Mixin and data fixtures do not constitute live gameplay validation.
