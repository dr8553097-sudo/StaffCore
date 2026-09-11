package com.tuservidor.staffcore.util;

import org.bukkit.command.CommandSender;

public final class Permissions {

    private Permissions() {
    }

    public static boolean has(CommandSender sender, String permission) {
        return sender.hasPermission("staffcore.admin") || sender.hasPermission(permission);
    }

    public static boolean hasAny(CommandSender sender, String... permissions) {
        if (sender.hasPermission("staffcore.admin")) {
            return true;
        }
        for (String permission : permissions) {
            if (sender.hasPermission(permission)) {
                return true;
            }
        }
        return false;
    }
}
