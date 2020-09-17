package util;

import java.util.Collection;
import java.util.function.Predicate;

public class CollectionFinderByIf {
    public static <T> T findByIf(Collection<T> collection, Predicate<T> predicate) {
        for (var e : collection) {
            if (predicate.test(e)) {
                return e;
            }
        }

        return null;
    }
}
