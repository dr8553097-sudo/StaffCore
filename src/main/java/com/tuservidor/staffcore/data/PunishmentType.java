package com.tuservidor.staffcore.data;

import java.util.Locale;

public enum PunishmentType {
    WARN,
    MUTE,
    KICK,
    BAN,
    TEMPBAN,
    BAN_IP,
    TEMPBAN_IP;

    public String translationKey() {
        return "punishment-type-" + name().toLowerCase(Locale.ROOT);
    }
}
