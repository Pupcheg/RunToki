package me.supcheg.runtoki;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class UrlUtil {
    @NotNull
    public static InputStream openStream(@NotNull String url, @NotNull Object @NotNull ... args) throws IOException {
        return new URL(url.formatted(args)).openStream();
    }
}
