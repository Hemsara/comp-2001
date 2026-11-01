package com.comp2000.roomhub.data.model;

public class User {
    public final String id;
    public final String name;
    public final String email;
    public final String department;
    public final Role role;

    public User(String id, String name, String email, String department, Role role) {
        this.id = id;
        this.name = name;
        this.email = email;
        this.department = department;
        this.role = role;
    }

    public String initials() {
        String[] parts = name.split(" ");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty() && sb.length() < 2) sb.append(p.charAt(0));
        }
        return sb.toString().toUpperCase();
    }

    public String firstName() {
        return name.split(" ")[0];
    }

    /** Label used by dropdowns (ArrayAdapter calls toString()). */
    @Override
    public String toString() {
        return name + " · " + department;
    }
}
