import com.android.build.api.variant.FilterConfiguration
import java.io.File
import java.io.StringReader
import java.util.Properties
import java.util.zip.ZipFile

plugins {
    id("vazie.android.application")
    id("vazie.hilt")
    // JUnit, Robolectric and the Compose test rule for the JVM tests that compose real UI.
    id("vazie.compose.test")
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
}

private val localProperties: Properties =
    providers.fileContents(rootProject.layout.projectDirectory.file("local.properties")).asText.orNull
        ?.let { text -> Properties().apply { load(StringReader(text)) } }
        ?: Properties()

/** A build setting: the environment (CI secrets) wins, then -P or ~/.gradle/gradle.properties, then local.properties. */
fun vazieSetting(property: String, environment: String): String? =
    providers.environmentVariable(environment).orNull?.trim()?.takeIf { it.isNotEmpty() }
        ?: providers.gradleProperty(property).orNull?.trim()?.takeIf { it.isNotEmpty() }
        ?: localProperties.getProperty(property)?.trim()?.takeIf { it.isNotEmpty() }

/** A public endpoint the app talks to: required, https, no trailing slash. */
fun vazieEndpoint(property: String, environment: String): String {
    val url = vazieSetting(property, environment)
        ?: error("$property is not set: add it to local.properties, or set $environment (CI: a GitHub secret)")
    require(Regex("https://[A-Za-z0-9.-]+(:\\d+)?(/[A-Za-z0-9._~/-]*)?").matches(url) && !url.endsWith("/")) {
        "$property must be an https URL without a trailing slash: $url"
    }
    return url
}

val sharedApiUrl = vazieEndpoint("vazie.api.url", "VAZIE_API_URL")
val vpnApiUrl = vazieEndpoint("vazie.vpnApi.url", "VAZIE_VPN_API_URL")
val siteUrl = vazieEndpoint("vazie.site.url", "VAZIE_SITE_URL")

/** The AppMetrica API key: a secret, so never in gradle.properties; the environment or local.properties. Blank = off. */
val appMetricaApiKey: String =
    vazieSetting("appmetrica.apiKey", "APPMETRICA_API_KEY")
        ?.takeIf { it.matches(Regex("[0-9a-fA-F-]{16,64}")) }
        ?: ""

val signKeystoreDir: Directory = rootProject.layout.projectDirectory.dir("signKeystore")

private val environmentInputNames =
    listOf("VAZIE_KEYSTORE_PATH", "VAZIE_KEYSTORE_PASSWORD", "VAZIE_KEY_ALIAS", "VAZIE_KEY_PASSWORD")
private val propertyInputNames =
    listOf("RELEASE_STORE_FILE", "RELEASE_STORE_PASS", "RELEASE_ALIAS", "RELEASE_KEY_PASS")

// Read through a value source rather than `File.readText()`, so the file is an input Gradle knows
// about rather than a fact this script happened to observe once.
private val keyPropertyValues: Map<String, String> =
    providers.fileContents(signKeystoreDir.file("key.properties")).asText.orNull
        ?.let { text -> Properties().apply { load(StringReader(text)) } }
        ?.entries
        ?.mapNotNull { (key, value) ->
            val trimmed = value.toString().trim()
            // A blank line is an absent value, not an empty password.
            if (trimmed.isEmpty()) null else key.toString() to trimmed
        }
        ?.toMap()
        .orEmpty()
        .filterKeys { it in propertyInputNames }

private val environmentInputValues: Map<String, String> = environmentInputNames
    .mapNotNull { name ->
        providers.environmentVariable(name).orNull?.trim()?.takeIf { it.isNotEmpty() }?.let { name to it }
    }
    .toMap()

val signingFromEnvironment: Boolean = environmentInputValues.size == environmentInputNames.size
val signingFromProperties: Boolean = keyPropertyValues.size == propertyInputNames.size
val signingIsComplete: Boolean = signingFromEnvironment || signingFromProperties
val signingInputs: Map<String, String> =
    if (signingFromEnvironment) environmentInputValues else keyPropertyValues

/** Where the keystore actually is, once `RELEASE_STORE_FILE` has been resolved. */
val signKeystoreFile: File? = keyPropertyValues["RELEASE_STORE_FILE"]?.let { declared ->
    val named = File(declared)
    if (named.isAbsolute) named else signKeystoreDir.file(declared).asFile
}

if (signingFromEnvironment && signingFromProperties) {
    // Names, never values. Silence here would let somebody edit `key.properties` and watch it have
    // no effect for an hour.
    logger.warn(
        "signing: both sources are complete; using the environment. " +
            "signKeystore/key.properties is being ignored for this build.",
    )
}

android {
    namespace = "app.vazie.vpn"

    // English and Russian only, so library translations stay out of resources.arsc.
    androidResources {
        localeFilters += listOf("en", "ru")
    }

    // The NDK is declared only so `stripReleaseDebugSymbols` can strip libgojni.so (~15 MB of symbols).
    ndkVersion = "28.2.13676358"

    defaultConfig {
        // The published identity; it can never change after the first upload.
        applicationId = "app.vazie.vpn"
        // Hilt's runner, not the default one.
        testInstrumentationRunner = "app.vazie.vpn.VazieTestRunner"

        // The AppMetrica key: `appmetrica.apiKey` in local.properties, or APPMETRICA_API_KEY in CI.
        // A build without one reports nothing at all.
        buildConfigField("String", "APPMETRICA_API_KEY", "\"$appMetricaApiKey\"")

        // Endpoints from local.properties or the environment; see `vazieEndpoint`.
        buildConfigField("String", "SHARED_API_URL", "\"$sharedApiUrl\"")
        buildConfigField("String", "VPN_API_URL", "\"$vpnApiUrl\"")
        buildConfigField("String", "SITE_URL", "\"$siteUrl\"")
    }

    // Robolectric needs merged resources; `AppearanceLauncherArtTest` inflates the real launcher drawables.
    testOptions.unitTests.isIncludeAndroidResources = true

    // Release signing: all four values from the environment or signKeystore/key.properties, never mixed;
    // none means an unsigned release, a partial set fails `verifyReleaseSigning`.
    if (signingIsComplete) {
        signingConfigs.create("release") {
            if (signingFromEnvironment) {
                storeFile = file(signingInputs.getValue("VAZIE_KEYSTORE_PATH"))
                storePassword = signingInputs.getValue("VAZIE_KEYSTORE_PASSWORD")
                keyAlias = signingInputs.getValue("VAZIE_KEY_ALIAS")
                keyPassword = signingInputs.getValue("VAZIE_KEY_PASSWORD")
            } else {
                storeFile = signKeystoreFile
                storePassword = signingInputs.getValue("RELEASE_STORE_PASS")
                keyAlias = signingInputs.getValue("RELEASE_ALIAS")
                keyPassword = signingInputs.getValue("RELEASE_KEY_PASS")
            }
        }
    }

    buildTypes {
        debug {
            applicationIdSuffix = ".debug"
            isMinifyEnabled = false
            buildConfigField("boolean", "SYSTEM_INTEGRATIONS_ENABLED", "true")
        }
        release {
            isMinifyEnabled = true
            // The symbols the strip takes out of the APK, kept where a crash report can still reach them.
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
            isShrinkResources = true
            // Turns on the tile and the widgets; with it false the receivers exist but never publish.
            buildConfigField("boolean", "SYSTEM_INTEGRATIONS_ENABLED", "true")
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Present only when all four inputs were. An unsigned release is an artifact nobody can
            // install by accident; a debug-signed one is an artifact somebody can.
            if (signingIsComplete) signingConfig = signingConfigs.getByName("release")
        }
        // Release-shaped build for testers; readable stack traces, debug-signed for now.
        create("qa") {
            initWith(getByName("release"))
            applicationIdSuffix = ".qa"
            matchingFallbacks += "release"
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    // Single dimension. Flavor source sets carry DI bindings only - never screens.
    flavorDimensions += "distribution"
    productFlavors {
        create("play") { dimension = "distribution" }
        create("huawei") { dimension = "distribution" }
        create("samsung") { dimension = "distribution" }
        create("direct") { dimension = "distribution" }
    }

    // `qa` uses the release source set too, so debug-only code needs one no-op counterpart.
    sourceSets.getByName("qa").apply {
        kotlin.directories.add("src/release/kotlin")
        res.directories.add("src/release/res")
        manifest.srcFile("src/release/AndroidManifest.xml")
    }

    // One APK per ABI (the engine is ~50 MB per ABI), plus a universal APK for sideloading.
    splits {
        abi {
            // Off while an App Bundle is being built, on otherwise.
            val buildingBundle = gradle.startParameter.taskNames.any { it.contains("undle") }
            isEnable = !buildingBundle
            reset()
            // Phones only. An x86 Android phone has not shipped in a decade, and each x86 ABI cost ~14 MB of
            // the universal APK. The universal APK drops them too — see `androidComponents` below.
            include("arm64-v8a", "armeabi-v7a")
            isUniversalApk = true
        }
    }

    buildFeatures { compose = true }
}

// Direct-download files are named `VazieVPN-<abi>.apk` and `VazieVPN-universal.apk`.
androidComponents {
    // Drops x86 and x86_64 from release-shaped APKs outside Play; debug keeps them for emulators.
    onVariants { variant ->
        if (variant.buildType == "debug" || variant.flavorName == "play") return@onVariants
        variant.packaging.jniLibs.excludes.addAll("lib/x86/**", "lib/x86_64/**")
    }

    onVariants(selector().withFlavor("distribution" to "direct").withBuildType("release")) { variant ->
            // Compressed native libraries, for the direct download and for nothing else.
            variant.packaging.jniLibs.useLegacyPackaging.set(true)

        variant.outputs.forEach { output ->
            val abi = output.filters
                .firstOrNull { it.filterType == FilterConfiguration.FilterType.ABI }
                ?.identifier
            output.outputFileName.set(
                if (abi == null) "VazieVPN-universal.apk" else "VazieVPN-$abi.apk"
            )
        }
    }
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:model"))
    implementation(project(":core:navigation"))
    // The composition root binds `:data:profiles` (R12) and constructs the parser registry.
    implementation(project(":vpn:api"))
    implementation(project(":vpn:config"))
    implementation(project(":data:profiles"))
    // VPN Plus servers, bound in `VazieServersModule`.
    implementation(project(":managed:api"))
    implementation(project(":data:managed"))
    // Crash and payment reports, only with the person's consent (`AppMetricaDiagnostics`).
    implementation(project(":core:analytics"))
    implementation(libs.appmetrica.analytics)
    // The tunnel and the engine; only `:app` may see an engine (R3), bound in `VpnModule`.
    implementation(project(":vpn:runtime"))
    implementation(project(":vpn:engine:xray"))
    implementation(project(":feature:home"))
    implementation(project(":feature:connections"))
    implementation(project(":feature:config"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:onboarding"))
    // Vazie Account and VPN Plus: email sign-in and payment against the production backend, in every
    // build type. `AccountModule` and `AccountEntry` in `src/main` bind them.
    implementation(project(":feature:account"))
    implementation(project(":data:account"))

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    // `:app` is not a feature, so it declares the lifecycle helpers itself.
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.kotlinx.coroutines.test)

    // ImageIO cannot read WebP; `LauncherIconGeometryTest` needs it to measure the launcher rasters.
    testImplementation(libs.imageio.webp)
    // The activity `createComposeRule` launches is declared in the consumer's own debug manifest,
    // so this one cannot arrive transitively.
    debugImplementation(libs.androidx.compose.ui.test.manifest)

    // Instrumentation needs concurrent-futures 1.2.0, while Glance's WorkManager resolves 1.1.0.
    constraints {
        implementation("androidx.concurrent:concurrent-futures-ktx:1.2.0")
    }

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.hilt.android.testing)
    kspAndroidTest(libs.hilt.compiler)

    // Glance comes through `:core:designsystem`; only the preview tooling is declared here.
    implementation(libs.androidx.glance.preview)
    debugImplementation(libs.androidx.glance.appwidget.preview)
}

// Release verification as build tasks: the resolved dependency graph and the built artifact.

/** The modules a public release must not link against, by Gradle path. A module parked here still builds and
 * runs its tests; it only may not appear on this application's runtime classpath. */
val parkedModules = listOf<String>(
    // Nothing is parked any more: managed access ships with VPN Plus. The task stays, so the next
    // module parked here is checked the day it is added.
)

val checkReleaseGraph by tasks.registering {
    group = "verification"
    description = "Fails if a parked backend module reaches the release runtime classpath."

    // Resolved at execution time and reported as project paths, so the failure names the module
    // rather than a jar somewhere in a Gradle cache.
    val resolved = configurations.named("directReleaseRuntimeClasspath").map { configuration ->
        configuration.incoming.resolutionResult.allComponents
            .mapNotNull { (it.id as? org.gradle.api.artifacts.component.ProjectComponentIdentifier)?.projectPath }
            .toSet()
    }
    val forbidden = parkedModules

    doLast {
        val offenders = resolved.get().filter { it in forbidden }
        check(offenders.isEmpty()) {
            "the release runtime classpath contains parked backend modules: $offenders\n" +
                "See settings.gradle.kts for why they are built but not bound."
        }
    }
}

/** What must not be found in a release artifact, asked of the artifact itself. */
val checkReleaseArtifact by tasks.registering {
    group = "verification"
    description = "Scans the assembled release APKs for the old package, Vazie endpoints, " +
        "credential shapes, debug tooling and protocols this build cannot run."
    dependsOn("assembleDirectRelease")

    val apkDirectory = layout.buildDirectory.dir("outputs/apk/direct/release")
    // Hosts a release must never name (retired backends): kept out of the repository, so they come from
    // local.properties or the environment, comma-separated.
    val forbiddenHosts = vazieSetting("vazie.release.forbiddenHosts", "VAZIE_RELEASE_FORBIDDEN_HOSTS")
        .orEmpty().split(',').map { it.trim() }.filter { it.isNotEmpty() }
    doLast {
        val retired = forbiddenHosts.map { "a retired Vazie host" to Regex(Regex.escape(it)) }
        val rules: List<Pair<String, Regex>> = retired + listOf(
            "the retired package name" to Regex("""app\.vazovsky\.vazie"""),
            // Managed access ships with VPN Plus, so its paths are allowed; an internal one is not.
            "a Vazie internal API path" to Regex("""/internal/v\d+/"""),
            "an embedded VLESS credential" to Regex(
                """vless://(?!00000000-0000-0000-0000-000000000001)[0-9a-fA-F]{8}-[0-9a-fA-F]{4}"""
            ),
            "a session-token-shaped literal" to Regex("""VAZIE_DEV_SESSION_TOKEN|DEV_SESSION_TOKEN"""),
            "a network debug inspector" to Regex("""chucker|Chucker"""),
            "a protocol this build cannot run" to Regex("""(?i)(wireguard|shadowsocks|vmess|trojan://)"""),
        )

        val apks = apkDirectory.get().asFile.listFiles().orEmpty().filter { it.extension == "apk" }
        check(apks.isNotEmpty()) { "no release APK to scan in ${apkDirectory.get().asFile}" }

        val offences = mutableListOf<String>()
        apks.forEach { apk ->
            ZipFile(apk).use { zip ->
                zip.entries().asSequence()
                    .filter { it.name.endsWith(".dex") || it.name == "resources.arsc" }
                    .forEach { entry ->
                        // ISO-8859-1 keeps every byte a distinct character, so a match cannot be
                        // invented or lost by a decoder guessing at UTF-8 in compiled bytecode.
                        val text = zip.getInputStream(entry).readBytes().toString(Charsets.ISO_8859_1)
                        rules.forEach { (name, pattern) ->
                            if (pattern.containsMatchIn(text)) {
                                offences += "${apk.name} :: ${entry.name} :: $name"
                            }
                        }
                    }
            }
        }
        check(offences.isEmpty()) {
            "the release artifact contains what a public build must not:\n" +
                offences.joinToString("\n") { "  - $it" }
        }
        logger.lifecycle("Release artifact scan OK - ${apks.size} APKs, ${rules.size} rules, no findings")
    }
}

/** Refuses a release build that was handed an incomplete set of signing inputs. */
val verifyReleaseSigning by tasks.registering {
    group = "verification"
    description = "Fails when a signing source is half-filled, or names a keystore that is not there."

    // Captured as plain strings at configuration time: a task that closes over the project cannot
    // be cached, and there is nothing here worth keeping alive but four names and a path.
    val environmentPresent = environmentInputValues.keys.sorted()
    val environmentMissing = environmentInputNames.filterNot { it in environmentInputValues }
    val propertiesPresent = keyPropertyValues.keys.sorted()
    val propertiesMissing = propertyInputNames.filterNot { it in keyPropertyValues }
    val keystorePath = signKeystoreFile?.takeIf { !signingFromEnvironment }?.absolutePath
    val propertiesComplete = signingFromProperties

    doLast {
        check(environmentPresent.isEmpty() || environmentMissing.isEmpty()) {
            "signing is half-configured in the environment: $environmentPresent set, " +
                "$environmentMissing missing. A release build would fall back to the debug key and " +
                "produce something installable under a key anyone can make."
        }
        check(propertiesPresent.isEmpty() || propertiesMissing.isEmpty()) {
            "signing is half-configured in signKeystore/key.properties: $propertiesPresent set, " +
                "$propertiesMissing missing or blank. Fill them in, or delete the file to build " +
                "unsigned — a release must not fall back to the debug key."
        }
        if (propertiesComplete && keystorePath != null) {
            check(File(keystorePath).isFile) {
                "signKeystore/key.properties names a keystore that is not there: $keystorePath. " +
                    "RELEASE_STORE_FILE is resolved inside signKeystore/ unless it is absolute."
            }
        }
    }
}

tasks.matching { it.name.startsWith("assemble") && it.name.contains("Release") }
    .configureEach { dependsOn(verifyReleaseSigning) }
tasks.matching { it.name.startsWith("bundle") && it.name.contains("Release") }
    .configureEach { dependsOn(verifyReleaseSigning) }
