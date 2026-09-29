import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.api.tasks.wrapper.Wrapper
import org.gradle.language.jvm.tasks.ProcessResources
import java.nio.charset.StandardCharsets
import java.util.Base64

plugins {
    `java-library`
    `maven-publish`
    id("net.neoforged.gradle.userdev") version "7.1.38"
}

fun prop(name: String): String = providers.gradleProperty(name).get()
fun decodeArgs(name: String): List<String> = providers.gradleProperty(name).orNull
    ?.takeIf { it.isNotEmpty() }
    ?.split('.')
    ?.map { token -> if (token == "_") "" else String(Base64.getDecoder().decode(token), StandardCharsets.UTF_8) }
    ?: emptyList()

val modVersion = prop("mod_version")
val modId = prop("mod_id")
val modGroupId = prop("mod_group_id")
val minecraftVersion = prop("minecraft_version")
val minecraftVersionRange = prop("minecraft_version_range")
val neoVersion = prop("neo_version")
val loaderVersionRange = prop("loader_version_range")
val modName = prop("mod_name")
val modAuthors = prop("mod_authors")
val modDescription = prop("mod_description")
val modLicense = prop("mod_license")
val create_version: String by project
val minecraft_version: String by project
val ponder_version: String by project
val flywheel_version: String by project
val registrate_version: String by project
val junit_version: String by project

version = modVersion
group = modGroupId

tasks.named<Wrapper>("wrapper") {
    distributionType = Wrapper.DistributionType.BIN
}

sourceSets.named("main") {
    resources.srcDir("src/generated/resources")
    resources.exclude("**/*.bbmodel")
    resources.exclude("src/generated/**/.cache")
}

base {
    archivesName.set(modId)
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val devUsername = providers.gradleProperty("cptUsername").orElse("Dev").get()
val safeDevUsername = devUsername.replace(Regex("[^A-Za-z0-9._-]"), "_")
val pycodersUsername = providers.gradleProperty("pycodersUsername").orElse(devUsername).get()
val pycodersGameArgs = decodeArgs("pycodersGameArgsB64")
val pycodersJavaArgs = decodeArgs("pycodersJavaArgsB64")
val cptLegacyRunRoot = layout.projectDirectory.dir("../../runtime/legacy-import/CreateProbabilityTuning/run")
val cptRunDir = file(
    providers.gradleProperty("pycodersRuntimeDir")
        .orElse("../../runtime/legacy-import/CreateProbabilityTuning/run")
        .get()
)
val jeiJar = cptLegacyRunRoot.file("client-Tester2/mods/jei-1.21.1-neoforge-19.44.0.403.jar")

runs {
    configureEach {
        systemProperty("forge.logging.markers", "REGISTRIES")
        systemProperty("forge.logging.console.level", "debug")
        workingDirectory = cptRunDir
        modSource(sourceSets.main.get())
        jvmArguments.addAll(pycodersJavaArgs)
    }

    val client by creating {
        systemProperty("neoforge.enabledGameTestNamespaces", modId)
        arguments.addAll(listOf("--username", pycodersUsername) + pycodersGameArgs)
    }

    val server by creating {
        systemProperty("neoforge.enabledGameTestNamespaces", modId)
        arguments.addAll(listOf("--nogui") + pycodersGameArgs)
    }

    val gameTestServer by creating {
        systemProperty("neoforge.enabledGameTestNamespaces", modId)
    }

    val data by creating {
        listOf(
            "--mod", modId,
            "--all",
            "--output", file("src/generated/resources/").absolutePath,
            "--existing", file("src/main/resources/").absolutePath,
        ).forEach(arguments::add)
    }
}

configurations.named("runtimeClasspath") {
    extendsFrom(configurations.named("localRuntime").get())
}

repositories {
    maven("https://maven.createmod.net")
    maven("https://maven.ithundxr.dev/snapshots")
    mavenCentral()
}

dependencies {
    implementation("net.neoforged:neoforge:$neoVersion")
    implementation("com.simibubi.create:create-$minecraft_version:$create_version:slim") {
        isTransitive = false
    }
    implementation("net.createmod.ponder:ponder-neoforge:$ponder_version+mc$minecraft_version")
    compileOnly("dev.engine-room.flywheel:flywheel-neoforge-api-$minecraft_version:$flywheel_version")
    compileOnly(files(jeiJar))
    runtimeOnly("dev.engine-room.flywheel:flywheel-neoforge-$minecraft_version:$flywheel_version")
    implementation("com.tterrag.registrate:Registrate:$registrate_version")
    testImplementation(platform("org.junit:junit-bom:$junit_version"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

tasks.withType<ProcessResources>().configureEach {
    val replaceProperties = mapOf(
        "minecraft_version" to minecraftVersion,
        "minecraft_version_range" to minecraftVersionRange,
        "neo_version" to neoVersion,
        "loader_version_range" to loaderVersionRange,
        "mod_id" to modId,
        "mod_name" to modName,
        "mod_version" to modVersion,
        "mod_authors" to modAuthors,
        "mod_description" to modDescription,
        "mod_license" to modLicense,
    )
    inputs.properties(replaceProperties)
    filesMatching(listOf("META-INF/neoforge.mods.toml", "pack.mcmeta")) {
        expand(replaceProperties)
    }
}

publishing {
    publications {
        register<MavenPublication>("mavenJava") {
            from(components["java"])
        }
    }
    repositories {
        maven("file://${project.projectDir}/repo")
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = true
    }
}
