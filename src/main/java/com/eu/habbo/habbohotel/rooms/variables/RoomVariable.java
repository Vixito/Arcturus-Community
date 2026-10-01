package com.eu.habbo.habbohotel.rooms.variables;

public class RoomVariable {
    private final String name;
    private int value;
    private boolean hasValue;
    private boolean isPermanent;
    private boolean isReadOnly;
    private long createdAt;
    private long updatedAt;

    public RoomVariable(String name, int value, boolean hasValue, boolean isPermanent) {
        this.name = name.toLowerCase().trim();
        this.value = value;
        this.hasValue = hasValue;
        this.isPermanent = isPermanent;
        this.isReadOnly = false;
        long now = System.currentTimeMillis();
        this.createdAt = now;
        this.updatedAt = now;
    }

    public RoomVariable(String name, int value, boolean hasValue, boolean isPermanent, long createdAt, long updatedAt) {
        this.name = name.toLowerCase().trim();
        this.value = value;
        this.hasValue = hasValue;
        this.isPermanent = isPermanent;
        this.isReadOnly = false;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getName() {
        return this.name;
    }

    public int getValue() {
        return this.value;
    }

    public void setValue(int value) {
        this.value = value;
        this.hasValue = true;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean hasValue() {
        return this.hasValue;
    }

    public void setHasValue(boolean hasValue) {
        this.hasValue = hasValue;
        this.updatedAt = System.currentTimeMillis();
    }

    public boolean isPermanent() {
        return this.isPermanent;
    }

    public void setPermanent(boolean permanent) {
        this.isPermanent = permanent;
    }

    public boolean isReadOnly() {
        return this.isReadOnly;
    }

    public void setReadOnly(boolean readOnly) {
        this.isReadOnly = readOnly;
    }

    public long getCreatedAt() {
        return this.createdAt;
    }

    public long getUpdatedAt() {
        return this.updatedAt;
    }

    public long getAgeSeconds() {
        return Math.max(0, (System.currentTimeMillis() - this.createdAt) / 1000L);
    }

    public long getUpdateAgeSeconds() {
        return Math.max(0, (System.currentTimeMillis() - this.updatedAt) / 1000L);
    }
}
