package org.limelightcreatives.site.web;

import org.junit.jupiter.api.Test;

import java.security.MessageDigest;
import java.util.HexFormat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class AssetsTest {

    private final Assets assets = new Assets();

    /** The hash Assets should add for a file under static/ (read from the classpath, which includes the built CSS). */
    public static String hashOf(String path) throws Exception {
        byte[] bytes;
        try (var in = AssetsTest.class.getResourceAsStream("/static" + path)) {
            if (in == null) throw new java.io.FileNotFoundException("static" + path);
            bytes = in.readAllBytes();
        }
        return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)).substring(0, 10);
    }

    @Test
    void addsTheContentHash() throws Exception {
        assertThat(assets.url("/js/site.js")).isEqualTo("/js/site.js?v=" + hashOf("/js/site.js"));
    }

    @Test
    void handlesEncodedFileNames() throws Exception {
        assertThat(assets.url("/team/headshots/Olivia%20Li.webp"))
                .isEqualTo("/team/headshots/Olivia%20Li.webp?v=" + hashOf("/team/headshots/Olivia Li.webp"));
    }

    @Test
    void differentFilesGetDifferentHashes() {
        assertThat(assets.url("/assets/cards/event.webp").replaceFirst(".*\\?v=", ""))
                .isNotEqualTo(assets.url("/assets/cards/bg2.webp").replaceFirst(".*\\?v=", ""));
    }

    @Test
    void leavesOtherUrlsAlone() {
        assertThat(assets.url("https://i.ytimg.com/vi/x/hqdefault.jpg")).isEqualTo("https://i.ytimg.com/vi/x/hqdefault.jpg");
        assertThat(assets.url("//cdn.example.com/a.png")).isEqualTo("//cdn.example.com/a.png");
        assertThat(assets.url(null)).isNull();
    }

    @Test
    void aMissingFileFailsLoudly() {
        assertThatThrownBy(() -> assets.url("/assets/nope.webp"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("/assets/nope.webp");
    }
}
