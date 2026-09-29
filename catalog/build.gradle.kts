plugins {
    id("vazie.android.application")
    alias(libs.plugins.kotlin.compose)
}

// :catalog is a standalone internal application - the Vazie UI Kit showcase. Rule R11: nothing may
// depend on :catalog, and no release job builds it.
android {
    namespace = "app.vazie.vpn.catalog"

    defaultConfig {
        applicationId = "app.vazie.vpn.catalog"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    buildFeatures { compose = true }
}

dependencies {
    implementation(project(":core:designsystem"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
