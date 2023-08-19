package me.supcheg.runtoki;

import com.google.gson.JsonArray;
import com.google.gson.JsonParser;
import lombok.AllArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;

@AllArgsConstructor
public class PaperDownloader {
    private static final String BUILDS_URL = "https://api.papermc.io/v2/projects/paper/versions/%s";
    private static final String BUILD_URL = "https://api.papermc.io/v2/projects/paper/versions/%s" +
            "/builds/%s/downloads/paper-%s-%s.jar";

    private final Path cacheDir;
    private final String minecraftVersion;

    @NotNull
    public Path downloadLatest() {
        return downloadBuild(resolveLatestBuild());
    }

    @SneakyThrows
    @NotNull
    private Path downloadBuild(int build) {
        Path outputPath = cacheDir.resolve("paper-" + minecraftVersion + "-" + build + ".jar");
        if (Files.notExists(outputPath)) {
            try (InputStream in = UrlUtil.openStream(BUILD_URL, minecraftVersion, build, minecraftVersion, build)) {
                Files.copy(in, outputPath);
            }
        }
        return outputPath;
    }

    @SneakyThrows
    private int resolveLatestBuild() {
        try (InputStream in = UrlUtil.openStream(BUILDS_URL, minecraftVersion)) {
            JsonArray builds = JsonParser.parseReader(new InputStreamReader(in)).getAsJsonObject()
                    .getAsJsonArray("builds");
            return builds.get(builds.size() - 1).getAsInt();
        }
    }
}
