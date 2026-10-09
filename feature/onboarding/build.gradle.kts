plugins {
    id("vazie.android.feature")
    id("vazie.compose.test")
}

android {
    namespace = "app.vazie.vpn.feature.onboarding"
}

// The last page sells VPN Plus at the backend's prices: `:account:api` is what they are read through (never
// `:data:account`, which R12 keeps for `:app`).
dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    implementation(project(":account:api"))
    implementation(libs.androidx.hilt.navigation.compose)
}
