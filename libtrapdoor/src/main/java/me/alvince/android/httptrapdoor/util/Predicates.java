package me.alvince.android.httptrapdoor.util;

public class Predicates {

    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

}
