plugins {
    id("vazie.jvm.library")
}

// Zero project dependencies, by rule R4: whatever reaches diagnostics has to be expressible in the
// types declared here, so a profile, a secret or an account cannot be passed in by accident.
dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
}
