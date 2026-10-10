package xyz.alexcrea.cuanvil.update;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class MinecraftVersionTests {
    @Test void nativeLeafVersionWinsOverBuildIdentifier() {
        assertEquals(new Version(26, 2), UpdateUtils.resolveMinecraftVersion(
                "26.2", "26.2.build.124-alpha", "Leaf build 124"));
    }

    @Test void legacySpigotVersionsRemainSupported() {
        assertEquals(new Version(1, 21, 11), UpdateUtils.resolveMinecraftVersion(
                null, "1.21.11-R0.1-SNAPSHOT", "Spigot"));
        assertEquals(new Version(26, 2), UpdateUtils.resolveMinecraftVersion(
                null, "26.2-R0.1-SNAPSHOT", "Spigot"));
    }

    @Test void bannerFallbackDoesNotParseBuildNumber() {
        assertEquals(new Version(26, 2), UpdateUtils.resolveMinecraftVersion(
                null, "build-121-stable", "Leaf version 26.2-build-121 (MC: 26.2)"));
    }

    @Test void suffixesAreIgnoredAndUnknownMetadataFailsClearly() {
        assertEquals(new Version(26, 2), UpdateUtils.resolveMinecraftVersion(
                "26.2-rc1", "build-121-stable", "Leaf"));
        assertThrows(IllegalStateException.class, () ->
                UpdateUtils.resolveMinecraftVersion(null, "build-121-stable", "Leaf"));
        assertThrows(IllegalStateException.class, () ->
                UpdateUtils.resolveMinecraftVersion("unknown", "build-121-stable", "Leaf"));
    }
}
