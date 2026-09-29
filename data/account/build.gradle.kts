plugins {
    id("vazie.android.library")
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "app.vazie.vpn.data.account"
}

// The Vazie Account on the wire and on disk; implements `:account:api`. Only `:app` sees it (R12).
dependencies {
    api(project(":account:api"))
    implementation(project(":core:model"))
    api(project(":core:network"))
    implementation(project(":core:crypto"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.core)
    testImplementation(libs.ktor.client.mock)
}
