package me.supcheg.runtoki;

import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.Properties;

@AllArgsConstructor
public class TokiPatcher {
    private static final String LATEST_TOKI = "https://github.com/TetraTau/Toki/releases/download/" +
            "v0.1.3-1.19.4/toki-v0.1.3-1.19.4.jar";
    private static final List<String> TRANSFERRING = List.of(
            "patches.list", "libraries.list", "versions.list",
            "main-class", "download-context",
            "versions", "libraries"
    );

    private final Path cacheDir;
    private final String minecraftVersion;

    @SneakyThrows
    public Path patchJar(@NotNull Path jarPath) {
        Path targetTokiJar = jarPath.getParent().resolve("patched-" + jarPath.getFileName());
        if (Files.notExists(targetTokiJar)) {
            Files.copy(jarPath, targetTokiJar);
            transferPatchInfo(jarPath, downloadTokiJar());
        }
        return targetTokiJar;
    }

    @SneakyThrows
    @NotNull
    private Path downloadTokiJar() {
        Path tokiPath = cacheDir.resolve("toki-v0.1.3.jar");
        if (Files.notExists(tokiPath)) {
            try (InputStream in = UrlUtil.openStream(LATEST_TOKI)) {
                Files.copy(in, tokiPath);
            }
        }
        return tokiPath;
    }

    @SneakyThrows
    private void transferPatchInfo(Path paperPath, Path tokiPath) {
        try (var tokiFileSystem = FileSystems.newFileSystem(tokiPath.toAbsolutePath())) {
            try (var paperFileSystem = FileSystems.newFileSystem(paperPath.toAbsolutePath())) {
                for (String fileName : TRANSFERRING) {
                    Path sourcePath = paperFileSystem.getPath("META-INF", fileName);
                    Path destPath = tokiFileSystem.getPath("META-INF", fileName);

                    Files.walkFileTree(sourcePath, new CopyFileVisitor(sourcePath, destPath));
                }
            }

            Path tokiInstallProperties = tokiFileSystem.getPath("toki-install.properties");

            var install = new Properties();
            install.load(new StringReader(Files.readString(tokiInstallProperties)));
            install.put("gameVersion", minecraftVersion);

            var stringWriter = new StringWriter();
            install.list(new PrintWriter(stringWriter));
            Files.writeString(tokiInstallProperties, stringWriter.toString());
        }
    }

    @RequiredArgsConstructor
    private static class CopyFileVisitor extends SimpleFileVisitor<Path> {
        private final Path targetPath;
        private final Path sourcePath;
        private boolean firstDirectory = true;

        @Override
        public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
            if (firstDirectory) {
                firstDirectory = false;
            } else {
                Files.createDirectories(targetPath.resolve(sourcePath.relativize(dir)));
            }
            return FileVisitResult.CONTINUE;
        }

        @Override
        public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
            Files.copy(file, targetPath.resolve(sourcePath.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
            return FileVisitResult.CONTINUE;
        }
    }
}
