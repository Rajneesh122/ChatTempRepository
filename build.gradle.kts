plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.skie) apply false
}

tasks.register("buildIosSimulator") {
    dependsOn(":shared:linkDebugFrameworkIosSimulatorArm64")
    group = "ios"
    description = "Build Shared.framework for iOS Simulator"
}

tasks.register("buildIosDevice") {
    dependsOn(":shared:linkDebugFrameworkIosArm64")
    group = "ios"
    description = "Build Shared.framework for iOS device"
}
