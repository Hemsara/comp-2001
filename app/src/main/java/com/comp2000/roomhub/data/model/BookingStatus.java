package com.comp2000.roomhub.data.model;

public enum BookingStatus {
    CONFIRMED("Confirmed"),
    PENDING("Pending approval"),
    CANCEL_REQUESTED("Cancellation requested"),
    CANCELLED("Cancelled"),
    DECLINED("Declined");

    public final String label;

    BookingStatus(String label) {
        this.label = label;
    }

    /** Whether the booking currently holds the room (used for overlap checks). */
    public boolean occupiesRoom() {
        return this == CONFIRMED || this == CANCEL_REQUESTED;
    }
}
