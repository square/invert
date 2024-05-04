#!/usr/bin/env kotlin

import Github_pages_main.TerminalExecRunnerResult
import java.io.File
import java.io.InputStream
import java.util.concurrent.Executors

val PROJECT_ROOT_DIR = File(".")
val INVERT_VERSION = File(PROJECT_ROOT_DIR, "gradle.properties").readText()
    .lines()
    .first { it.startsWith("version=") }
    .substringAfter("version=")
val INVERT_INIT_SCRIPT = File(PROJECT_ROOT_DIR, "invert.gradle")

/**
 * Data bundle that includes standard output, error output, and exit code of a process.
 */
data class TerminalExecRunnerResult(
    val stdOut: String,
    val stdErr: String,
    val exitCode: Int
)

fun readStream(inputStream: InputStream): String {
    return inputStream.bufferedReader().use { reader ->
        val output = StringBuilder()
        var line: String?
        while (reader.readLine().also { line = it } != null) {
            println(line) // This prints each line in real-time.
            output.append(line).append("\n")
        }
        output.toString().trim()
    }
}

/**
 * Runs a terminal command that returns [TerminalExecRunnerResult] when the process finishes.
 *
 * @param [command] shell command split into a list that includes the program and it's arguments.
 */
fun exec(command: List<String>, workingDir: File): TerminalExecRunnerResult {
    println("*****\nBEGIN ${command.joinToString(" ")}")
    println("CWD ${workingDir.path}")
    val process = ProcessBuilder(command)
        .directory(workingDir)
        .start()

    val executor = Executors.newFixedThreadPool(2)

    // Start reading stdout and stderr in separate threads
    val stdoutFuture = executor.submit<String> { readStream(process.inputStream) }
    val stderrFuture = executor.submit<String> { readStream(process.errorStream) }

    // Wait for the process to terminate
    val exitCode = process.waitFor()
    executor.shutdown()

    // Get the outputs from futures
    val stdout = stdoutFuture.get()
    val stderr = stderrFuture.get()

    println("END (Exit Code $exitCode) ${command.joinToString(" ")}\n-----")

    return TerminalExecRunnerResult(stdout, stderr, exitCode)
}

/**
 * Runs a terminal command that returns [TerminalExecRunnerResult] when the process finishes.
 *
 * Note: This method should only be used for simple commands, as the command is manually split by
 * a space character. This becomes a problem for commands with more complex arguments that include
 * spaces. We don't split spaces inside of double quotes, but more advanced scenarios might fail, so
 * use at your own risk.
 *
 * @param [command] shell command as you would enter it in the terminal.
 */
fun executeCmd(command: String, workingDir: File): TerminalExecRunnerResult {
    return exec(
        // https://stackoverflow.com/a/51356605
        // This regex splits strings only outside of double quotes.
        command.split(" (?=(?:[^\"]*\"[^\"]*\")*[^\"]*\$)".toRegex())
            .map {
                // Strip surrounding double quotes.
                it.trim('"')
            },
        workingDir
    )
}

executeCmd("./scripts/publish-to-maven-local.sh", PROJECT_ROOT_DIR)

val DEFAULT_INIT_SCRIPT_LINE = "./gradlew --init-script ${INVERT_INIT_SCRIPT.canonicalPath} :invert --no-daemon"

data class TargetRepo(
    val org: String,
    val project: String,
    val enabled: Boolean = true,
    val buildDirPath: String = "build",
    val invertGradleCmd: () -> String = { DEFAULT_INIT_SCRIPT_LINE },
    val postCheckout: (File) -> Unit = { projectCloneDir -> },
    val runInvert: (File) -> Unit = { projectCloneDir ->
        executeCmd(
            invertGradleCmd(),
            projectCloneDir
        )
    }
) {

    val url = "https://github.com/$org/$project"
}

val ALL_REPOS = listOf<TargetRepo>(
    TargetRepo(
        org = "JetBrains",
        project = "kotlin",
        invertGradleCmd = {
            "./gradlew :invert"
        },
        postCheckout = { clonedProjectDir ->
            // Remove Dependency Verification
            File(clonedProjectDir, "gradle/verification-metadata.xml").apply {
                if (exists()) delete()
            }

            // Add Maven Local Repo
            File(clonedProjectDir, "settings.gradle").apply {
                readText().also { text ->
                    val toFind = "gradlePluginPortal()"
                    val toReplace = "$toFind\nmavenLocal()"
                    if (!text.contains(toReplace)) {
                        writeText(text.replace(toFind, toReplace))
                    }
                }
            }

            // Add Invert Plugin
            File(clonedProjectDir, "build.gradle.kts").apply {
                readText().also { text ->
                    val toFind = "plugins {"
                    val toReplace = "$toFind\nid(\"com.squareup.invert\") version \"$INVERT_VERSION\""
                    if (!text.contains(toReplace)) {
                        writeText(text.replace(toFind, toReplace))
                    }
                }
            }
        }
    ),
    TargetRepo(
        org = "apereo",
        project = "cas",
        enabled = false,
    ),
    TargetRepo(
        org = "android",
        project = "nowinandroid"
    ),
    TargetRepo(
        org = "duckduckgo",
        project = "Android"
    ),
    TargetRepo(
        org = "square",
        project = "anvil",
        buildDirPath = "build/root-build"
    ),
    TargetRepo(
        org = "square",
        project = "okhttp"
    ),
    TargetRepo(
        org = "chrisbanes",
        project = "tivi"
    ),
    TargetRepo(
        org = "gradle",
        project = "gradle",
        invertGradleCmd = {
            // ignoreBuildJavaVersionCheck=true is needed for https://github.com/gradle/gradle for the Java 11/Java 17 mix
            "$DEFAULT_INIT_SCRIPT_LINE -Dorg.gradle.ignoreBuildJavaVersionCheck=true"
        },
    ),
    TargetRepo(
        org = "spring-projects",
        project = "spring-boot"
    ),
).filter { it.enabled }

val CLONES_DIR = File("build/clones").apply {
    if (!exists()) {
        mkdirs()
    }
}

val STATIC_SITE_FOLDER = File("build/static").apply {
    if (exists()) {
        deleteRecursively()
    }
    mkdirs()
}

ALL_REPOS.forEach { targetRepo ->

    val PROJECT_CLONE_DIR = File(CLONES_DIR, "${targetRepo.org}/${targetRepo.project}")

    if (!PROJECT_CLONE_DIR.exists()) {
        executeCmd(
            "git clone --depth=1 ${targetRepo.url}",
            PROJECT_CLONE_DIR.parentFile.apply { if (!exists()) mkdirs() }
        )
    } else {
        executeCmd("git pull", PROJECT_CLONE_DIR)
    }

    targetRepo.postCheckout(PROJECT_CLONE_DIR)
    targetRepo.runInvert(PROJECT_CLONE_DIR)

    val INVERT_REPORT_DIR = File(PROJECT_CLONE_DIR, "${targetRepo.buildDirPath}/reports/invert")

    val OUTPUT_FOLDER = File(STATIC_SITE_FOLDER, "${targetRepo.org}-${targetRepo.project}").apply {
        if (exists()) {
            deleteRecursively()
        }
    }
    INVERT_REPORT_DIR
        .walkTopDown()
        .forEach {
            val relativePath = it.canonicalPath.replace(INVERT_REPORT_DIR.canonicalPath, "").drop(1)
            val OUTPUT_FILE = File(OUTPUT_FOLDER, relativePath)
            it.copyTo(
                OUTPUT_FILE,
                overwrite = true
            )
        }
}

val html = buildString {
    appendLine("<html>")
    appendLine("<head><link rel='stylesheet' href='https://cdn.jsdelivr.net/npm/bootstrap@3.4.1/dist/css/bootstrap.min.css' crossorigin='anonymous'></head>")
    appendLine("<body><h1>Invert results for GitHub Projects</h1><br/><ul>")
    STATIC_SITE_FOLDER.listFiles()!!
        .mapNotNull { file ->
            ALL_REPOS.firstOrNull { repo -> file.name == "${repo.org}-${repo.project}" }
        }
        .forEach {
            println("Found $it")
            appendLine("<li><a href='${it.org}-${it.project}/index.html'>${it.org}/${it.project}</a> (${it.url})</li>")
        }
    appendLine("</ul></body></html>")
}
val HTML_FILE = File(STATIC_SITE_FOLDER, "index.html").apply {
    writeText(html)
}