package kor.toxicity.cutscenemaker.util.conditions;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Predicate;

public interface ActionPredicate<T> extends Predicate<T> {

    default ActionPredicate<T> castInstead(Consumer<T> action) {
        Objects.requireNonNull(action);
        return t -> {
            action.accept(t);
            return !test(t);
        };
    }
    default ActionPredicate<T> cast(Consumer<T> action) {
        Objects.requireNonNull(action);
        return t -> {
            action.accept(t);
            return !test(t);
        };
    }

    default ActionPredicate<T> addAnd(Predicate<T> predicate) {
        Objects.requireNonNull(predicate);
        return t -> test(t) && predicate.test(t);
    }
    default ActionPredicate<T> addOr(Predicate<T> predicate) {
        Objects.requireNonNull(predicate);
        return t -> test(t) || predicate.test(t);
    }
}
