package dev.mihile.skillpresets;

import java.util.*;
import java.util.function.Consumer;

final class PresetSwitch {
    private PresetSwitch() { }

    static boolean run(PresetBook book, Map<String, Set<String>> live, int target,
                       Consumer<Map<String, Set<String>>> validate, Consumer<Map<String, Set<String>>> apply,
                       Runnable rollback) {
        PresetBook.checkSlot(target);
        book.capture(live);
        if (book.active() == target) return false;
        Map<String, Set<String>> desired = book.allocation(target);
        validate.accept(desired);
        try { apply.accept(desired); }
        catch (RuntimeException failure) {
            try { rollback.run(); }
            catch (RuntimeException rollbackFailure) { failure.addSuppressed(rollbackFailure); }
            throw failure;
        }
        book.select(target);
        return true;
    }
}
