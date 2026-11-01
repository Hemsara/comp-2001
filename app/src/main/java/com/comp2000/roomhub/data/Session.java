package com.comp2000.roomhub.data;

import com.comp2000.roomhub.data.model.User;

/** Holds the signed-in user for the lifetime of the process. */
public final class Session {
    private static User current;

    private Session() {
    }

    public static User user() {
        return current;
    }

    public static void signIn(User user) {
        current = user;
    }

    public static void signOut() {
        current = null;
    }
}
