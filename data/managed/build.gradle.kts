plugins {
    id("vazie.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vazie.vpn.data.managed"
}

// The only place the Vazie wire format is known; only `:app` sees it (R12). Implements `:managed:api`;
// `:vpn:api` provides the profile model the mapper produces.
dependencies {
    implementation(project(":managed:api"))
    implementation(project(":account:api"))
    implementation(project(":vpn:api"))
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    // Ktor engine types on the test classpath only, to answer requests with exact bodies.
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.mock)
}
