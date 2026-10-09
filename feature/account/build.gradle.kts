plugins {
    id("vazie.android.feature")
    id("vazie.compose.test")
}

android {
    namespace = "app.vazie.vpn.feature.account"
}

// Sign-in screens and the account block in Settings. Sees `:account:api` only: the backend, the
// token and its file are `:data:account`'s, which rule R12 keeps from every feature.
dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":account:api"))
    implementation(project(":core:analytics"))
    implementation(libs.androidx.hilt.navigation.compose)
}
