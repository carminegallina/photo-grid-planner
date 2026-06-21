# Project-specific rules for release builds.
# R8 already understands AndroidX, Jetpack Compose and Kotlin metadata well; keep rules here intentionally narrow.

# These enum names are persisted in local DataStore JSON. Keep only them stable so saved local projects
# remain readable after an app update; the rest of the app remains available for R8 obfuscation/shrinking.
-keep enum com.niwlayr.app.data.PostKind { *; }
-keep enum com.niwlayr.app.data.PlaceholderType { *; }
-keep enum com.niwlayr.app.data.PreviewMode { *; }

# Do not log noisy warnings for optional debug/tooling annotations referenced by dependencies.
-dontwarn org.jetbrains.annotations.**
