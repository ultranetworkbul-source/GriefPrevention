package com.griefprevention.platform;

import org.jetbrains.annotations.NotNull;

/**
 * Central utility for detecting server platform at runtime.
 * <p>
 * Detection results are cached to avoid repeated reflection calls.
 *
 * @see PlatformListener for adding platform-specific listeners
 */
public final class PlatformDetection
{

    /**
     * Server platforms that the plugin can detect and adapt to.
     */
    public enum Platform
    {
        /** Paper and Paper forks (Purpur, Pufferfish, etc.) */
        PAPER,
        /** Spigot and Spigot-based servers without Paper API */
        SPIGOT
    }

    private static Platform detectedPlatform = null;

    private PlatformDetection()
    {
    }

    /**
     * Detects and returns the server platform.
     * Result is cached after first detection.
     *
     * @return the detected platform
     */
    public static @NotNull Platform getPlatform()
    {
        if (detectedPlatform == null)
        {
            detectedPlatform = detectPlatform();
        }
        return detectedPlatform;
    }

    private static @NotNull Platform detectPlatform()
    {
        if (classExists("com.destroystokyo.paper.PaperConfig")
                || classExists("io.papermc.paper.configuration.Configuration"))
        {
            return Platform.PAPER;
        }
        return Platform.SPIGOT;
    }

    /**
     * Checks if a class exists on the classpath.
     * <p>
     * Useful for checking if platform-specific APIs are available
     * before attempting to use them.
     *
     * @param className the fully qualified class name
     * @return true if the class exists
     */
    public static boolean classExists(String className)
    {
        try
        {
            Class.forName(className);
            return true;
        }
        catch (ClassNotFoundException e)
        {
            return false;
        }
    }

    /**
     * Checks if a Tag field exists on the Tag interface.
     * <p>
     * Useful for checking if version-specific Tags are available
     * before attempting to use them (e.g., COPPER_GOLEM_STATUES in 1.22+).
     *
     * @param tagFieldName the field name on org.bukkit.Tag (e.g., "COPPER_GOLEM_STATUES")
     * @return true if the tag exists
     */
    public static boolean tagExists(String tagFieldName)
    {
        try
        {
            org.bukkit.Tag.class.getField(tagFieldName);
            return true;
        }
        catch (NoSuchFieldException e)
        {
            return false;
        }
    }

}