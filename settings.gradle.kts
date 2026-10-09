pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // The verified libXray AAR as module "vendored:libXray": AGP will not bundle a raw local .aar, and
        // FAIL_ON_PROJECT_REPOS puts the repository here. See `fetchAndVerifyLibXray()`.
        flatDir { dirs(File(rootDir, "vpn/engine/xray/build/libxray-repo")) }
    }
}

rootProject.name = "Vazie"

// Modules are created when they get real content, never as placeholders.
include(":app")
include(":catalog")
include(":core:model")
include(":core:analytics")
include(":core:designsystem")
include(":core:navigation")
// The backend client, and Vazie servers for VPN Plus accounts (never a stored profile).
include(":core:network")
// Encryption at rest, shared by the profile store and the session token store.
include(":core:crypto")
// The VPN contracts and the link parser.
include(":vpn:api")
include(":vpn:config")
// The tunnel and VpnService, and the Xray engine binding (visible to `:app` only, R3).
include(":vpn:runtime")
include(":vpn:engine:xray")
include(":data:profiles")
include(":managed:api")
include(":data:managed")
include(":feature:home")
include(":feature:connections")
include(":feature:config")
include(":feature:settings")
include(":feature:onboarding")
// Vazie Account and VPN Plus: email sign-in, the signed-in account, sign-out, and paying for VPN
// Plus. Bound into every build type, release included.
include(":account:api")
include(":data:account")
include(":feature:account")
// Which build is current and which must update, from the backend's public version endpoint. Apart from the
// account on purpose: an update check needs no session, and never stands in the way of connecting.
include(":update:api")
include(":data:update")
// The About screen's links and the words under them, from the backend's public links endpoint, cached on disk.
include(":data:links")
