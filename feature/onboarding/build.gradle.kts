plugins {
    id("vazie.android.feature")
}

android {
    namespace = "app.vazie.vpn.feature.onboarding"
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:navigation"))
}
