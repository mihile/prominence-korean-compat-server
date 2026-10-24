package dev.mihile.tridentback;

import dev.mihile.tridentback.mixin.ArrowPickupAccess;
import net.fabricmc.fabric.api.dimension.v1.FabricDimensions;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.portal.PortalInfo;
import net.minecraft.world.phys.Vec3;

public final class RescueController {
    private static final ClassValue<WeaponFamily> FAMILIES = new ClassValue<>() {
        @Override protected WeaponFamily computeValue(Class<?> type) { return WeaponFamily.fromModHierarchy(type); }
    };
    private static long rescued;
    public static long rescuedCount() { return rescued; }
    public static void resetStatistics() { rescued = 0; }

    public static WeaponFamily family(AbstractArrow arrow) {
        return arrow instanceof ThrownTrident ? WeaponFamily.TRIDENT : FAMILIES.get(arrow.getClass());
    }

    /** Returns true to skip the entire original entity tick, including mod return damage and item drops. */
    public static boolean handle(ServerLevel level, Entity entity) {
        if (!(entity instanceof AbstractArrow arrow) || entity.isRemoved()) return false;
        WeaponFamily family = family(arrow);
        if (family == WeaponFamily.NONE) return false;
        RescueData data = ((RescueState)arrow).gmtb$data();
        data.reconcileOwner(((RescueState)arrow).gmtb$actualOwner());
        RescueConfig config = GiveMyTridentBack.config();

        // Record player ownership while it can be proven. Never guess the owner from proximity.
        if (data.playerOwner == null && arrow.getOwner() instanceof ServerPlayer player) {
            boolean creative = player.getAbilities().instabuild || (family != WeaponFamily.SOULS_WEAPONRY
                    && arrow.pickup == AbstractArrow.Pickup.CREATIVE_ONLY);
            data.recordPlayer(player.getUUID(), creative);
        }
        if (!data.rescuing) {
            if (!VoidPolicy.belowThreshold(arrow.getY(), level.getMinBuildHeight(), config.voidDepth)) return false;
            boolean realWeapon = !data.synthetic && (family != WeaponFamily.SIMPLY_SWORDS
                    || (arrow instanceof SimplyRealWeapon real && real.gmtb$isRealWeapon()));
            ItemStack item = ((ArrowPickupAccess)arrow).gmtb$pickupItem();
            if (!VoidPolicy.mayStart(config.enabled, family, realWeapon, data.playerOwner != null, item.getCount(),
                    arrow.pickup != AbstractArrow.Pickup.DISALLOWED, config.requireLoyalty,
                    EnchantmentHelper.getLoyalty(item))) return false;
            data.rescuing = true;
            rescued++;
        }

        // Creative throws do not remove the source item. Retire only the projectile,
        // even if the owner changed game mode before the rescue.
        if (data.creativeThrow) { arrow.discard(); return true; }

        // Keep the original projectile as the only item authority while offline/dead/full.
        arrow.setDeltaMovement(Vec3.ZERO);
        arrow.setNoPhysics(true);
        arrow.shakeTime = 0;
        ServerPlayer player = data.playerOwner == null ? null : level.getServer().getPlayerList().getPlayer(data.playerOwner);
        if (player == null || !VoidPolicy.safeOwner(true, player.isAlive(), player.isSpectator(),
                player.getY(), player.serverLevel().getMinBuildHeight())) return true;

        if (arrow.level() != player.level()) {
            // Fabric's explicit target bypasses portal creation and End spawn platform placement.
            AbstractArrow moved = FabricDimensions.teleport(arrow, player.serverLevel(),
                    new PortalInfo(player.position().add(0, .5, 0), Vec3.ZERO, arrow.getYRot(), arrow.getXRot()));
            if (moved == null) return true;
            arrow = moved;
        } else arrow.setPos(player.getX(), player.getY() + .5, player.getZ());

        arrow.setOwner(player);
        arrow.pickup = AbstractArrow.Pickup.ALLOWED;
        arrow.setNoPhysics(true);
        arrow.shakeTime = 0;
        arrow.setDeltaMovement(Vec3.ZERO);
        // This calls the weapon's own pickup implementation: enchantments/NBT/offhand and
        // creative-only semantics are preserved. A full inventory leaves it alive for retry.
        arrow.playerTouch(player);
        return true;
    }
}
