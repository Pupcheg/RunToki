package me.supcheg.runtoki;

import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.JavaExec;
import org.gradle.api.tasks.TaskAction;
import org.jetbrains.annotations.NotNull;

import java.io.Reader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Properties;

public class RunToki extends JavaExec {
    private static final List<String> AIKAR_FLAGS = List.of(
            "-XX:+UseG1GC", "-XX:+ParallelRefProcEnabled", "-XX:MaxGCPauseMillis=200",
            "-XX:+UnlockExperimentalVMOptions", "-XX:+DisableExplicitGC", "-XX:+AlwaysPreTouch",
            "-XX:G1NewSizePercent=30", "-XX:G1MaxNewSizePercent=40", "-XX:G1HeapRegionSize=8M",
            "-XX:G1ReservePercent=20", "-XX:G1HeapWastePercent=5", "-XX:G1MixedGCCountTarget=4",
            "-XX:InitiatingHeapOccupancyPercent=15", "-XX:G1MixedGCLiveThresholdPercent=90",
            "-XX:G1RSetUpdatingPauseTimePercent=5", "-XX:SurvivorRatio=32", "-XX:+PerfDisableSharedMem",
            "-XX:MaxTenuringThreshold=1", "-Dusing.aikars.flags=https://mcflags.emc.gs",
            "-Daikars.new.flags=true"
    );

    private final Project project;
    private final Task buildTask;

    @Getter
    @Setter
    @Input
    private boolean useAikarFlags = true;
    @Getter
    @Setter
    @Input
    private String minecraftVersion;

    public RunToki(@NotNull Project project) {
        this.project = project;
        this.buildTask = project.getTasksByName("build", false).iterator().next();
        dependsOn(buildTask);
    }

    @SneakyThrows
    @TaskAction
    @Override
    public void exec() {
        Objects.requireNonNull(minecraftVersion);
        if (useAikarFlags) {
            args(AIKAR_FLAGS);
        }
        args("nogui", "-add-plugin=" + getProjectOutputFilePath());

        setStandardInput(System.in);

        Path workingDir = project.file("run").toPath();
        Files.createDirectories(workingDir);
        setWorkingDir(workingDir.toFile());

        Path cacheDir = workingDir.resolve("cache");
        Files.createDirectories(cacheDir);

        Path latestPaper = new PaperDownloader(cacheDir, minecraftVersion)
                .downloadLatest();
        Path patchedJarPath = new TokiPatcher(cacheDir, minecraftVersion)
                .patchJar(latestPaper);

        setClasspath(project.files(patchedJarPath.toAbsolutePath().toString()));
        getMainClass().set(readMainClassName(patchedJarPath));

        super.exec();
    }

    @SneakyThrows
    @NotNull
    private static String readMainClassName(@NotNull Path path) {
        Properties manifest = new Properties();
        try (FileSystem fs = FileSystems.newFileSystem(path)) {
            Path manifestPath = fs.getPath("META-INF", "MANIFEST.MF");
            try (Reader reader = Files.newBufferedReader(manifestPath)) {
                manifest.load(reader);
            }
        }
        return manifest.get("Main-Class").toString();
    }

    @NotNull
    private String getProjectOutputFilePath() {
        return buildTask.getOutputs().getFiles().getFiles().iterator().next().getPath();
    }
}
