package me.supcheg.runtoki;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.jetbrains.annotations.NotNull;

public class RunTokiPlugin implements Plugin<Project> {
    @Override
    public void apply(@NotNull Project target) {
        target.getTasks().register("runToki", RunToki.class);
    }
}
