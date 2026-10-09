plugins {
    id("vazie.jvm.library")
}

// Pure Kotlin: what managed access is, not how it is fetched. `:vpn:api` is `api` because `VpnProfile` is on
// the surface; R15 stops `:vpn:*` from depending back.
dependencies {
    api(project(":vpn:api"))
}
