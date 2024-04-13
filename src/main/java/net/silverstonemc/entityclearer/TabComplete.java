package net.silverstonemc.entityclearer;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class TabComplete implements TabCompleter {
    final List<String> arguments = new ArrayList<>();

    public List<String> onTabComplete(CommandSender sender, @NotNull Command cmd, @NotNull String label, @NotNull String[] args) {
        if (sender.hasPermission("entityclearer.reload")) if (!arguments.contains("reload")) arguments.add(
            "reload");
        if (sender.hasPermission("entityclearer.debug")) if (!arguments.contains("debug")) arguments.add(
            "debug");
        if (sender.hasPermission("entityclearer.clear")) if (!arguments.contains("clearnow")) arguments.add(
            "clearnow");

        List<String> result = new ArrayList<>();
        if (args.length == 1) {
            for (String a : arguments)
                if (a.toLowerCase().startsWith(args[0].toLowerCase())) result.add(a);
            return result;
        }
        return null;
    }
}