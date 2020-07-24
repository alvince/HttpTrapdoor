package me.alvince.android.httptrapdoor.util;

public class Preconditions {

    public static <T> T requireNonNull(T obj) {
        if (obj == null)
            throw new NullPointerException();
        return obj;
    }

}
