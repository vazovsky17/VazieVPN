plugins {
    id("vazie.android.feature")
    // Compose tests for the inventory rows.
    id("vazie.compose.test")
}

android {
    namespace = "app.vazie.vpn.feature.connections"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
    // The list is stored profiles now. `:vpn:api` is the only VPN module a feature may see, and
    // what arrives through it are summaries with no field capable of holding a secret (R2, R12).
    implementation(project(":vpn:api"))
    // `ManagedServer` — the countries Vazie plans to run servers in. `:app` provisions them and
    // this screen lists them; `:core:model` is the only module both are allowed to see.
    implementation(project(":core:model"))
    implementation(libs.androidx.hilt.navigation.compose)
}
