package com.eu.habbo.habbohotel.rooms.variables;

public enum VariableScope {
    ROOM(0, "Global (Sala)"),
    USER(1, "Usuario"),
    FURNI(2, "Furni"),
    CONTEXT(3, "Contexto (Ejecución)"),
    CROSS_ROOM(4, "Desde Otra Sala");

    public final int code;
    public final String label;

    VariableScope(int code, String label) {
        this.code = code;
        this.label = label;
    }

    public static VariableScope fromCode(int code) {
        for (VariableScope s : values()) {
            if (s.code == code) return s;
        }
        return ROOM;
    }
}
