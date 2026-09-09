package com.griefprevention.platform;

import org.bukkit.Material;
import org.bukkit.Tag;
import org.bukkit.entity.Entity;

/**
 * Utility class for handling version-specific features.
 * <p>
 * Provides safe access to features that may not exist on all Minecraft versions,
 * such as CopperGolem (1.22+) and related tags.
 */
public final class VersionCompatibility
{
    private static final Class<?> COPPER_GOLEM_CLASS;
    private static final Tag<Material> COPPER_GOLEM_STATUES_TAG;

    static
    {
        // Try to load CopperGolem class (1.22+)
        Class<?> copperGolemClass = null;
        try
        {
            copperGolemClass = Class.forName("org.bukkit.entity.CopperGolem");
        }
        catch (ClassNotFoundException ignored)
        {
            // Not available on this version
        }
        COPPER_GOLEM_CLASS = copperGolemClass;

        // Try to load COPPER_GOLEM_STATUES tag (1.22+)
        Tag<Material> copperGolemStatuesTag = null;
        try
        {
            @SuppressWarnings("unchecked")
            Tag<Material> tag = (Tag<Material>) Tag.class.getField("COPPER_GOLEM_STATUES").get(null);
            copperGolemStatuesTag = tag;
        }
        catch (NoSuchFieldException | IllegalAccessException | ClassCastException ignored)
        {
            // Not available on this version
        }
        COPPER_GOLEM_STATUES_TAG = copperGolemStatuesTag;
    }

    private VersionCompatibility()
    {
    }

    /**
     * Checks if CopperGolem entity type is available on this server version.
     *
     * @return true if CopperGolem is available (1.22+)
     */
    public static boolean hasCopperGolem()
    {
        return COPPER_GOLEM_CLASS != null;
    }

    /**
     * Checks if the entity is a CopperGolem.
     * Returns false on versions where CopperGolem doesn't exist.
     *
     * @param entity the entity to check
     * @return true if the entity is a CopperGolem
     */
    public static boolean isCopperGolem(Entity entity)
    {
        return COPPER_GOLEM_CLASS != null && COPPER_GOLEM_CLASS.isInstance(entity);
    }

    /**
     * Checks if COPPER_GOLEM_STATUES tag is available on this server version.
     *
     * @return true if the tag is available (1.22+)
     */
    public static boolean hasCopperGolemStatuesTag()
    {
        return COPPER_GOLEM_STATUES_TAG != null;
    }

    /**
     * Checks if the material is tagged as a copper golem statue.
     * Returns false on versions where the tag doesn't exist.
     *
     * @param material the material to check
     * @return true if the material is a copper golem statue
     */
    public static boolean isCopperGolemStatue(Material material)
    {
        return COPPER_GOLEM_STATUES_TAG != null && COPPER_GOLEM_STATUES_TAG.isTagged(material);
    }
}
