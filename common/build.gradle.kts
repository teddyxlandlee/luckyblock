import groovy.lang.Closure
import java.text.SimpleDateFormat
import java.util.TimeZone
import java.util.Date

plugins {
    kotlin("multiplatform")
    id("com.palantir.git-version")
}

repositories {
    mavenCentral()
}

kotlin {
    jvm {
        compilations.all {
            kotlinOptions.jvmTarget = "1.8"
        }
    }

    js {
        browser {
            useCommonJs()

        }
    }

    sourceSets {
        val commonMain by getting

        val jvmMain by getting
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test-common"))
                implementation(kotlin("test-annotations-common"))
                // todo: separate common and JVM tests when mockk adds multiplatform support
                // implementation("io.mockk:mockk-common:1.+")
                implementation(kotlin("test-junit"))
                implementation("io.mockk:mockk:1.+")
            }
        }

        val jsMain by getting
    }
}

val forgeModVersion: String by project
val forgeMinForgeVersion: String by project

val fabricModVersion: String by project

val bedrockModVersion: String by project
val bedrockTemplateAddonVersion: String by project
val minBedrockAddonMCVersion: String by project
val minBedrockMCVersion: String by project

typealias TemplateAddonVersions = Map<String, List<Map<String, String>>>

@Suppress("UNCHECKED_CAST")
val javaTemplateAddonVersions = groovy.json.JsonSlurper().parseText(
    file("src/jvmMain/resources/template-addons/versions.json").readText()
) as TemplateAddonVersions

fun copyJvmRuntimeResources(projectName: String, taskName: String, loaderName: String, modVersion: String, dir: String) {
    val latestTemplateAddon = javaTemplateAddonVersions["versions"]!![0]["folder"]
    project(":$projectName").tasks.register<Copy>(taskName) {
        into(dir)
        into("config/lucky/$modVersion-$loaderName") {
            from("$rootDir/common/build/processedResources/jvm/main/lucky-config")
        }
        into("addons/lucky/lucky-block-custom") {
            from("$rootDir/common/src/jvmMain/resources/template-addons/${latestTemplateAddon}")
        }
        dependsOn(tasks.getByName("jvmProcessResources"))
    }
}
copyJvmRuntimeResources(projectName="fabric", taskName="copyRunResources", loaderName="fabric", modVersion=fabricModVersion, dir="$rootDir/run")
//copyRuntimeResources(projectName="forge", taskName="copyRunResources", loaderName="forge", modVersion=forgeModVersion, dir="$rootDir/run")

tasks.register<Copy>("jvmTestCopyRunResources") {
    into("build/test-run")
    into("config/lucky/0.0.0-0-test") {
        from("$rootDir/common/build/processedResources/jvm/main/lucky-config")
    }
    into("addons/lucky") {
        from("$rootDir/common/build/processedResources/jvm/main/template-addons")
    }
    dependsOn(tasks.getByName("jvmProcessResources"))
}
tasks.getByName("jvmTest").dependsOn(tasks.getByName("jvmTestCopyRunResources"))

tasks.register<Zip>("jvmConfigDist") {
    archiveFileName.set("lucky-config.zip")
    destinationDirectory.set(file("$rootDir/common/build/tmp"))
    from("build/processedResources/jvm/main/lucky-config")
    dependsOn(tasks.getByName("jvmProcessResources"))
}

fun getModVersionNumber(modVersion: String): Int {
    val splitVersion = modVersion.split('-')
    val mcVersion = splitVersion[0].split('.')
    return (mcVersion[0].toInt()) * 1000000 +
        (mcVersion[1].toInt()) * 10000 +
        (mcVersion[2].toInt()) * 100 +
        splitVersion[1].toInt()
}

fun writeMeta(distDir: String, version: String, versionNumber: Int, minMinecraftVersion: String, extraInfo: Map<String, String> = emptyMap()) {
    val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
    dateFormat.timeZone = TimeZone.getTimeZone("UTC")
    val gitDetails = (project.ext["versionDetails"] as Closure<*>)() as com.palantir.gradle.gitversion.VersionDetails

    file(distDir).mkdirs()
    val meta = mapOf(
        "version" to version,
        "version_number" to versionNumber,
        "min_minecraft_version" to minMinecraftVersion,
        "revision" to gitDetails.gitHash,
        "datetime" to dateFormat.format(Date())
    ) + extraInfo
    file("$distDir/meta.yaml").writeText(
        meta.entries.joinToString("") { (k, v) -> "$k: $v\n" }
    )
}

fun writeTemplateAddonMeta(versionsJson: TemplateAddonVersions) {
    val latest = versionsJson["versions"]!![0]
    writeMeta(
        distDir = "$rootDir/dist/${latest["folder"]}",
        version = latest["version"]!!,
        versionNumber = latest["version"]!!.toInt(),
        minMinecraftVersion = latest["min_minecraft_version"]!!,
        extraInfo = mapOf("min_mod_version" to latest["min_mod_version"]!!)
    )
}

tasks.register<Zip>("jvmTemplateAddonDist") {
    val distName = javaTemplateAddonVersions["versions"]!![0]["folder"]
    doFirst { writeTemplateAddonMeta(javaTemplateAddonVersions) }

    archiveFileName.set("$distName.zip")
    destinationDirectory.set(file("$rootDir/dist/$distName"))
    from("build/processedResources/jvm/main/template-addons/$distName")
    from("dist/$distName/meta.yaml")
    dependsOn(tasks.getByName("jvmProcessResources"))
}

project(":bedrock").tasks.register<Zip>("templateAddonDist") {
    // TODO
}

fun jvmJarDist(projectName: String, modVersion: String) {
    project(":$projectName").tasks.register<Zip>("dist") {
        val distName = "${rootProject.name}-$modVersion-$projectName"
        destinationDirectory.set(file("$rootDir/dist/$distName"))
        archiveFileName.set("$distName.jar")

        doFirst {
            writeMeta(
                distDir = "$rootDir/dist/$distName",
                version = modVersion,
                versionNumber = getModVersionNumber(modVersion),
                minMinecraftVersion = modVersion.split('-')[0],
                extraInfo = if (projectName == "forge") mapOf("min_forge_version" to forgeMinForgeVersion) else emptyMap()
            )
        }

        from(zipTree("$rootDir/$projectName/build/libs/${rootProject.name}-$modVersion.jar"))
        from("$rootDir/common/build/tmp/lucky-config.zip") { into("mod/lucky/java") }
        from("$rootDir/dist/$distName/meta.yaml")

        dependsOn(tasks.getByName("jvmConfigDist"))
        dependsOn(tasks.getByName("jvmProcessResources"))
    }
}
jvmJarDist("fabric", fabricModVersion)
//jvmJarDist("forge", forgeModVersion)

project(":bedrock").tasks.register<Zip>("dist") {
    val distName = "${rootProject.name}-$bedrockModVersion-bedrock"
    destinationDirectory.set(file("$rootDir/dist/$distName"))
    archiveFileName.set("$distName.mcpack")

    doFirst {
        writeMeta(
            distDir = "$rootDir/dist/$distName",
            version = bedrockModVersion,
            versionNumber = getModVersionNumber(bedrockModVersion),
            minMinecraftVersion = bedrockModVersion.split('-')[0]
        )
    }

    from("$rootDir/bedrock/build/mcpack/$distName")
    //dependsOn(tasks.getByName("processResources")) // TODO
}

tasks.register<Delete>("jvmCleanDist") {
    delete("$rootDir/dist")
}

tasks.getByName("jvmJar") { dependsOn(tasks.getByName("jvmTemplateAddonDist")) }
tasks.clean { dependsOn(tasks.getByName("jvmCleanDist")) }

