plugins {
    id("vazie.jvm.library")
}

// What a Vazie Account is and what can be asked of it; `:data:account` talks to the backend.
dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
}
