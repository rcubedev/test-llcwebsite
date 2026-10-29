package org.limelightcreatives.site.web;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * URLs for files under {@code static/} with a hash of their content added
 *
 * <p>Templates use it as {@code ${assets.url('/css/app.css')}}. Anything that isn't a local
 * path (a YouTube thumbnail, say) is returned unchanged. A path with no file behind it fails
 * the render
 */
@Component
public class Assets {

    private record Version(long lastModified, String hash) {}

    private final Map<String, Version> versions = new ConcurrentHashMap<>();

    public String url(String path) {
        if (path == null || !path.startsWith("/") || path.startsWith("//")) return path;
        return path + "?v=" + hash(path);
    }

    private String hash(String path) {
        // Paths in content can be URL-encoded ("Olivia%20Li.webp"); the file name isn't.
        var file = new ClassPathResource("static" + UriUtils.decode(path, StandardCharsets.UTF_8));
        if (!file.exists()) throw new IllegalArgumentException("No file in static/ for " + path);
        try {
            long lastModified = file.lastModified(); // so bootRun picks up edited files
            Version known = versions.get(path);
            if (known != null && known.lastModified() == lastModified) return known.hash();
            String hash = sha256(file).substring(0, 10);
            versions.put(path, new Version(lastModified, hash));
            return hash;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static String sha256(ClassPathResource file) throws IOException {
        try (InputStream in = file.getInputStream()) {
            var digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[64 * 1024];
            for (int read; (read = in.read(buffer)) > 0; ) digest.update(buffer, 0, read);
            return HexFormat.of().formatHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e); // every JVM has SHA-256
        }
    }
}
