import local.prominence.cinder.hourglass.HourglassSafeDelivery;
import java.lang.reflect.Method;

public final class HourglassGuardTests {
    static int checks;
    static void check(boolean b, String msg) {
        checks++;
        if (!b) throw new AssertionError(msg);
    }

    public static void main(String[] args) throws Exception {
        ClassLoader cl = HourglassGuardTests.class.getClassLoader();

        // 1. Verify HourglassSafeDelivery exists and has deliver method with DeliveryType
        Method deliverMethod = null;
        for (Method m : HourglassSafeDelivery.class.getDeclaredMethods()) {
            if (m.getName().equals("deliver")) {
                deliverMethod = m;
                break;
            }
        }
        check(deliverMethod != null, "HourglassSafeDelivery.deliver method missing");
        check(deliverMethod.getParameterCount() == 5, "HourglassSafeDelivery.deliver parameter count mismatch");
        check(deliverMethod.getParameterTypes()[4] == HourglassSafeDelivery.DeliveryType.class, "Last param must be DeliveryType");

        // 2. Verify DeliveryType values
        check(HourglassSafeDelivery.DeliveryType.values().length == 4, "DeliveryType values count mismatch");

        // 3. Verify mixin classes exist in classpath (without initializing static blocks)
        Class<?> acc = Class.forName("local.prominence.cinder.mixin.MythicChallengeAccessor", false, cl);
        check(acc != null && acc.isInterface(), "MythicChallengeAccessor interface missing");

        Class<?> cm = Class.forName("local.prominence.cinder.mixin.HourglassChallengeMixin", false, cl);
        check(cm != null, "HourglassChallengeMixin missing");

        Class<?> pm = Class.forName("local.prominence.cinder.mixin.HourglassPortalMixin", false, cl);
        check(pm != null, "HourglassPortalMixin missing");

        Class<?> im = Class.forName("local.prominence.cinder.mixin.HourglassItemMixin", false, cl);
        check(im != null, "HourglassItemMixin missing");

        // 4. Verify prominentbase target classes exist in classpath
        Class<?> mc = Class.forName("elocindev.prominentbase.mythicchallenge.MythicChallenge", false, cl);
        check(mc != null, "MythicChallenge class missing");

        Class<?> pe = Class.forName("elocindev.prominentbase.entity.nonliving.ChallengePortalEntity", false, cl);
        check(pe != null, "ChallengePortalEntity class missing");

        Class<?> hi = Class.forName("elocindev.prominentbase.item.HourglassItem", false, cl);
        check(hi != null, "HourglassItem class missing");

        System.out.println("PASS: HourglassGuardTests passed " + checks + " checks.");
    }
}
