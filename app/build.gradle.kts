@file:Suppress("UnstableApiUsage")

import org.eclipse.jgit.api.ArchiveCommand
import org.eclipse.jgit.api.Git
import org.eclipse.jgit.archive.TarFormat
import org.eclipse.jgit.lib.ObjectId
import org.jetbrains.kotlin.backend.common.pop

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

buildscript {
    dependencies {
        classpath(libs.jgit)
        classpath(libs.jgit.archive)
    }
}

typealias VersionTriple = Triple<String?, Int, ObjectId>

fun describeVersion(git: Git): VersionTriple {
    // jgit doesn't provide a nice way to get strongly-typed objects from its `describe` command
    val describeStr = git.describe().setLong(true).call()

    return if (describeStr != null) {
        val pieces = describeStr.split('-').toMutableList()
        val commit = git.repository.resolve(pieces.pop().substring(1))
        val count = pieces.pop().toInt()
        val tag = pieces.joinToString("-")

        Triple(tag, count, commit)
    } else {
        val log = git.log().call().iterator()
        val head = log.next()
        var count = 1

        while (log.hasNext()) {
            log.next()
            ++count
        }

        Triple(null, count, head.id)
    }
}

fun getVersionCode(triple: VersionTriple): Int {
    val tag = triple.first
    val (major, minor) = if (tag != null) {
        if (!tag.startsWith('v')) {
            throw IllegalArgumentException("Tag does not begin with 'v': $tag")
        }

        val pieces = tag.substring(1).split('.')
        if (pieces.size != 2) {
            throw IllegalArgumentException("Tag is not in the form 'v<major>.<minor>': $tag")
        }

        Pair(pieces[0].toInt(), pieces[1].toInt())
    } else {
        Pair(0, 0)
    }

    // 8 bits for major version, 8 bits for minor version, and 8 bits for git commit count
    assert(major in 0 until 1.shl(8))
    assert(minor in 0 until 1.shl(8))
    assert(triple.second in 0 until 1.shl(8))

    return major.shl(16) or minor.shl(8) or triple.second
}

fun getVersionName(git: Git, triple: VersionTriple): String {
    val tag = triple.first?.replace(Regex("^v"), "") ?: "NONE"

    return buildString {
        append(tag)

        if (triple.second > 0) {
            append(".r")
            append(triple.second)

            append(".g")
            git.repository.newObjectReader().use {
                append(it.abbreviate(triple.third).name())
            }
        }
    }
}

val git = Git.open(File(rootDir, ".git"))!!
val gitVersionTriple = describeVersion(git)
val gitVersionCode = getVersionCode(gitVersionTriple)
val gitVersionName = getVersionName(git, gitVersionTriple)

val projectUrl = "https://github.com/chenxiaolong/RSAF"
val releaseMetadataBranch = "master"

val extraDir = layout.buildDirectory.map { it.dir("extra") }
val archiveDir = extraDir.map { it.dir("archive") }
val rcbridgeDir = extraDir.map { it.dir("rcbridge") }
val rcbridgeAar = rcbridgeDir.map { it.file("rcbridge.aar") }

android {
    namespace = "com.chiller3.rsaf"

    compileSdk = 34
    buildToolsVersion = "34.0.0"
    ndkVersion = "26.0.10792818"

    defaultConfig {
        applicationId = "com.chiller3.rsaf"
        minSdk = 28
        targetSdk = 34
        versionCode = gitVersionCode
        versionName = gitVersionName

        base.archivesName.set("RSAF-$versionName")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "PROJECT_URL_AT_COMMIT",
            "\"${projectUrl}/tree/${gitVersionTriple.third.name}\"")

        buildConfigField("String", "DOCUMENTS_AUTHORITY",
            "APPLICATION_ID + \".documents\"")

        resValue("string", "app_name", "@string/app_name_release")
    }
    sourceSets {
        getByName("main") {
            assets {
                srcDir(archiveDir)
            }
        }
    }
    signingConfigs {
        create("release") {
            val keystore = System.getenv("RELEASE_KEYSTORE")
            storeFile = if (keystore != null) { File(keystore) } else { null }
            storePassword = System.getenv("RELEASE_KEYSTORE_PASSPHRASE")
            keyAlias = System.getenv("RELEASE_KEY_ALIAS")
            keyPassword = System.getenv("RELEASE_KEY_PASSPHRASE")
        }
    }
    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"

            resValue("string", "app_name", "@string/app_name_debug")
        }

        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")

            signingConfig = signingConfigs.getByName("release")
        }
    }
    applicationVariants.all {
        // This is set here so that applicationIdSuffix will be respected
        resValue("string", "documents_authority", "$applicationId.documents")
    }
    compileOptions {
        sourceCompatibility(JavaVersion.VERSION_17)
        targetCompatibility(JavaVersion.VERSION_17)
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
    splits {
        // Split by ABI because compiled golang code is huge and a universal APK is nearly 200 MiB
        abi {
            isEnable = true
            isUniversalApk = false
        }
    }
}

dependencies {
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.biometric)
    implementation(libs.core.ktx)
    implementation(libs.fragment.ktx)
    implementation(libs.preference.ktx)
    implementation(libs.security.crypto)
    implementation(libs.material)
    implementation(files(rcbridgeAar))

    // Included only to work around R8 complaining about missing annotation classes referenced by
    // the Tink transitive dependency
    implementation(libs.jsr305)

    androidTestImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
}

val archive = tasks.register("archive") {
    inputs.property("gitVersionTriple.third", gitVersionTriple.third)

    val outputFile = archiveDir.map { it.file("archive.tar") }
    outputs.file(outputFile)

    doLast {
        val format = "tar_for_task_$name"

        ArchiveCommand.registerFormat(format, TarFormat())
        try {
            outputFile.get().asFile.outputStream().use {
                git.archive()
                    .setTree(git.repository.resolve(gitVersionTriple.third.name))
                    .setFormat(format)
                    .setOutputStream(it)
                    .call()
            }
        } finally {
            ArchiveCommand.unregisterFormat(format)
        }
    }
}

val rcbridge = tasks.register<Exec>("rcbridge") {
    val rcbridgeSrcDir = File(rootDir, "rcbridge")
    val tempDir = rcbridgeDir.map { it.dir("temp") }

    inputs.files(
        File(rcbridgeSrcDir, "go.mod"),
        File(rcbridgeSrcDir, "go.sum"),
        File(rcbridgeSrcDir, "rcbridge.go"),
        File(File(rcbridgeSrcDir, "envhack"), "envhack.go"),
    )
    inputs.properties(
        "android.defaultConfig.minSdk" to android.defaultConfig.minSdk,
        "android.namespace" to android.namespace,
        "android.ndkDirectory" to android.ndkDirectory,
    )
    outputs.files(
        rcbridgeDir.map { it.file("rcbridge.aar") },
        rcbridgeDir.map { it.file("rcbridge-sources.jar") },
    )

    executable = "gomobile"
    args = listOf(
        "bind",
        "-v",
        "-o", rcbridgeAar.get().asFile.absolutePath,
        "-target=android",
        "-androidapi=${android.defaultConfig.minSdk}",
        "-javapkg=${android.namespace}.binding",
        ".",
    )
    environment(
        "ANDROID_HOME" to android.sdkDirectory,
        "ANDROID_NDK_HOME" to android.ndkDirectory,
        "TMPDIR" to tempDir.get().asFile.absolutePath,
    )

    if (!environment.containsKey("GOPROXY")) {
        environment("GOPROXY", "https://proxy.golang.org,direct")
    }

    workingDir(rcbridgeSrcDir)

    doFirst {
        tempDir.get().asFile.mkdirs()
    }

    // gomobile fails to clean up its temp directories after it switched to using go modules. These
    // directories are never reused, so delete them.
    doLast {
        val subDirs = tempDir.get().asFile.listFiles { _, name: String ->
            name.startsWith("gomobile-work-")
        }
        if (subDirs != null) {
            for (subDir in subDirs) {
                println("Cleaning up gomobile leftovers: $subDir")

                exec {
                    executable("go")
                    args = listOf("clean", "-modcache")
                    environment("GOPATH", subDir.absolutePath)
                }

                File(subDir, "pkg").delete()
                subDir.delete()
            }
        }
    }
}

android.applicationVariants.all {
    preBuildProvider.configure {
        dependsOn(archive)
        dependsOn(rcbridge)
    }
}

data class LinkRef(val type: String, val number: Int) : Comparable<LinkRef> {
    override fun compareTo(other: LinkRef): Int = compareValuesBy(
        this,
        other,
        { it.type },
        { it.number },
    )

    override fun toString(): String = "[$type #$number]"
}

fun checkBrackets(line: String) {
    var expectOpening = true

    for (c in line) {
        if (c == '[' || c == ']') {
            if (c == '[' != expectOpening) {
                throw IllegalArgumentException("Mismatched brackets: $line")
            }

            expectOpening = !expectOpening
        }
    }

    if (!expectOpening) {
        throw IllegalArgumentException("Missing closing bracket: $line")
    }
}

fun updateChangelogLinks(baseUrl: String) {
    val file = File(rootDir, "CHANGELOG.md")
    val regexStandaloneLink = Regex("\\[([^\\]]+)\\](?![\\(\\[])")
    val regexAutoLink = Regex("(Issue|PR) #(\\d+)")
    val links = hashMapOf<LinkRef, String>()
    var skipRemaining = false
    val changelog = mutableListOf<String>()

    file.useLines { lines ->
        for (rawLine in lines) {
            val line = rawLine.trimEnd()

            if (!skipRemaining) {
                checkBrackets(line)
                val matches = regexStandaloneLink.findAll(line)

                for (linkMatch in matches) {
                    val linkText = linkMatch.groupValues[1]
                    val match = regexAutoLink.matchEntire(linkText)
                    require(match != null) { "Invalid link format: $linkText" }

                    val type = match.groupValues[1]
                    val number = match.groupValues[2].toInt()

                    val link = when (type) {
                        "Issue" -> "$baseUrl/issues/$number"
                        "PR" -> "$baseUrl/pull/$number"
                        else -> throw IllegalArgumentException("Unknown link type: $type")
                    }

                    // #0 is used for examples only
                    if (number != 0) {
                        links[LinkRef(type, number)] = link
                    }
                }

                if ("Do not manually edit the lines below" in line) {
                    skipRemaining = true
                }

                changelog.add(line)
            }
        }
    }

    for ((ref, link) in links.entries.sortedBy { it.key }) {
        changelog.add("$ref: $link")
    }

    changelog.add("")

    file.writeText(changelog.joinToString("\n"))
}

fun updateChangelog(version: String?, replaceFirst: Boolean) {
    val file = File(rootDir, "CHANGELOG.md")
    val expected = if (version != null) { "### Version $version" } else { "### Unreleased" }

    val changelog = mutableListOf<String>().apply {
        // This preserves a trailing newline, unlike File.readLines()
        addAll(file.readText().lineSequence())
    }

    val index = changelog.indexOfFirst { it.startsWith("### ") }
    if (index == -1) {
        changelog.addAll(0, listOf(expected, ""))
    } else if (changelog[index] != expected) {
        if (replaceFirst) {
            changelog[index] = expected
        } else {
            changelog.addAll(index, listOf(expected, ""))
        }
    }

    file.writeText(changelog.joinToString("\n"))
}

tasks.register("changelogUpdateLinks") {
    doLast {
        updateChangelogLinks(projectUrl)
    }
}

tasks.register("changelogPreRelease") {
    doLast {
        val version = project.property("releaseVersion")

        updateChangelog(version.toString(), true)
    }
}

tasks.register("changelogPostRelease") {
    doLast {
        updateChangelog(null, false)
    }
}

tasks.register("preRelease") {
    dependsOn("changelogUpdateLinks")
    dependsOn("changelogPreRelease")
}

tasks.register("postRelease") {
    dependsOn("changelogPostRelease")
}
