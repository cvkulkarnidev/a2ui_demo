package dev.cvkulkarnidev.a2ui

/**
 * Describes the components supported by an Android renderer catalog.
 *
 * This mirrors the catalog boundary used by the maintained A2UI renderers:
 * the surface identifies a catalog, and component resolution is constrained
 * to the implementations registered by that catalog.
 */
data class A2UICatalog(
    val id: String,
    val displayName: String,
    val supportedComponents: Set<String>
) {
    fun supports(componentType: String): Boolean = componentType in supportedComponents
}

val BASIC_ANDROID_CATALOG = A2UICatalog(
    id = "basic",
    displayName = "Android Basic Catalog",
    supportedComponents = setOf(
        "Text",
        "Column",
        "Row",
        "Card",
        "Button",
        "TextField",
        "CheckBox",
        "Slider",
        "Divider",
        "Image",
        "Icon"
    )
)

val A2UI_CATALOGS: Map<String, A2UICatalog> = mapOf(
    BASIC_ANDROID_CATALOG.id to BASIC_ANDROID_CATALOG
)
