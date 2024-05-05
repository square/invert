//package com.github.pgreze.kowners
//
//import com.github.ajalt.clikt.core.CliktCommand
//import com.github.ajalt.clikt.core.CliktError
//import com.github.ajalt.clikt.core.NoOpCliktCommand
//import com.github.ajalt.clikt.core.subcommands
//import com.github.ajalt.clikt.parameters.arguments.argument
//import com.github.ajalt.clikt.parameters.arguments.default
//import com.github.ajalt.clikt.parameters.options.default
//import com.github.ajalt.clikt.parameters.options.flag
//import com.github.ajalt.clikt.parameters.options.multiple
//import com.github.ajalt.clikt.parameters.options.option
//import com.github.ajalt.clikt.parameters.types.choice
//import com.github.ajalt.clikt.parameters.types.file
//import java.io.File
//import java.util.Locale
//import kotlin.math.roundToInt
//
//fun main(args: Array<String>) =
//    Kowners().main(args)
//
//// https://ajalt.github.io/clikt/
//class Kowners : NoOpCliktCommand() {
//    init {
//        subcommands(Blame(), Coverage(), Query())
//    }
//}
//
//abstract class BaseCommand(name: String, help: String) : CliktCommand(name = name, help = help) {
//    val verbose: Boolean by option("-v", "--verbose")
//        .flag(default = false)
//
//    // Notice: ~/ notation is not possible with `gw run --args "..."` or IntelliJ runners
//    val target: File by argument(help = "Target directory (default: working directory)")
//        .file(mustExist = true)
//        .default(File(System.getProperty("user.dir")))
//
//    val gitRootPath: File? by lazy { target.findGitRootPath() }
//    private val codeOwnersFile: File by lazy {
//        (gitRootPath ?: target).findCodeOwnerLocations().firstOrNull()
//            ?: cliError("CODEOWNERS file not found in ${(gitRootPath ?: target).absolutePath}")
//    }
//    val resolver by lazy { OwnersResolver(codeOwnersFile.readLines().parseCodeOwners()) }
//
//    val lsFiles: List<String> by lazy {
//        gitRootPath?.let {
//            val relativeTarget = target.relativeTo(it)
//            if (verbose) echo("${it.absoluteFile} $ Git ls-files $relativeTarget", err = true)
//            it.lsFiles(relativeTarget)
//        } ?: run {
//            echo("Target is not a git tracked folder, fallback to a recursive file listing", err = true)
//            target.listFilesRecursively().map { it.path }
//        }.takeIf { it.isNotEmpty() } ?: cliError("Couldn't resolve tracked files for path ${target.absolutePath}")
//    }
//}
//
//class Blame : BaseCommand(
//    name = "blame",
//    help = "display how many files are covered by each ownership rules"
//) {
//    enum class Display { LIST, COUNT, PERCENT }
//
//    val displayMode: Display by option("-d", "--display", help = "Choose how to display result")
//        .choice(Display.values().map { it.name.toLowerCase(Locale.ROOT) to it }.toMap())
//        .default(Display.LIST)
//
//    override fun run() {
//        val lineToFiles = resolveLineToFiles()
//        display(lineToFiles)
//    }
//
//    private fun resolveLineToFiles(): MutableMap<Int, MutableSet<String>> {
//        val foundFiles = mutableSetOf<String>()
//        val lineToFiles = mutableMapOf<Int, MutableSet<String>>()
//        resolver.ownerships.withIndex().reversed().forEach { (index, line) ->
//            lsFiles.forEach { file ->
//                if (file !in foundFiles && line.pattern.matches(file)) {
//                    lineToFiles.getOrPut(index, ::mutableSetOf).add(file)
//                    foundFiles.add(file)
//                }
//            }
//        }
//        return lineToFiles
//    }
//
//    private fun display(lineToFiles: MutableMap<Int, MutableSet<String>>) {
//        val countMaxLength by lazy {
//            (lineToFiles.map { it.value.size }.maxOrNull() ?: 0).toString().length
//        }
//        resolver.ownerships.withIndex().forEach { (index, line) ->
//            when (displayMode) {
//                Display.LIST ->
//                    line.origin + lineToFiles[index]?.let { "\n    " + it.joinToString("\n    ") }
//                Display.COUNT -> (
//                    lineToFiles[index]?.size?.toString()?.padEnd(countMaxLength, ' ')
//                        ?: "0${" ".repeat(countMaxLength - 1)}"
//                    ).let { "$it ${line.origin}" }
//                Display.PERCENT ->
//                    (lineToFiles[index]?.size?.percentOf(lsFiles.size)?.toString() ?: "0")
//                        .let { "$it%".padEnd(PERCENT_SIZE, ' ') + line.origin }
//            }.let(::echo)
//        }
//    }
//
//    private val CodeOwnership.origin: String
//        get() = "${pattern.pattern} ${owners.joinToString(" ")}"
//
//    companion object {
//        const val PERCENT_SIZE = 4 // Size of 'XX% ' pattern.
//    }
//}
//
//class Coverage : BaseCommand(
//    name = "coverage",
//    help = "display the percentage of files covered by each ownership rules"
//) {
//    override fun run() {
//        val ownerToFiles = mutableMapOf<String?, MutableSet<String>>()
//
//        lsFiles.forEach { file ->
//            when (val owners = resolver.resolveOwnership(file)) {
//                null -> ownerToFiles.addForKey(null, file)
//                else -> owners.forEach { owner -> ownerToFiles.addForKey(owner, file) }
//            }
//        }
//
//        ownerToFiles
//            .map { (owner, files) -> owner to files.size.percentOf(lsFiles.size) }
//            .sortedByDescending { it.second }
//            .forEach { (owner, percent) ->
//                echo("$percent% ${owner ?: "??"}")
//            }
//    }
//
//    private fun MutableMap<String?, MutableSet<String>>.addForKey(key: String?, value: String) =
//        getOrPut(key, ::mutableSetOf).add(value)
//}
//
//private fun Int.percentOf(total: Int) =
//    (toDouble() / total * 100).roundToInt()
//
//class Query : BaseCommand(
//    name = "query",
//    help = "display the potential owner and sub-hierarchy owners for each versioned file"
//) {
//    val owner: List<String> by option(help = "Filter by owner(s)")
//        .multiple()
//    val relative: Boolean by option(help = "Display paths related to target path")
//        .flag("--absolute")
//
//    override fun run() {
//        lsFiles.map { it to resolver.resolveOwnership(it) }.forEach { (path, owners) ->
//            if (owner.isEmpty() || owners?.any { owner.contains(it) } == true) {
//                val pathDisplay = path
//                    .takeUnless { relative }
//                    ?: File(gitRootPath, path).relativeTo(target)
//                echo("$pathDisplay ${owners.ownersToString()}")
//            }
//        }
//    }
//}
//
//private fun cliError(message: String): Nothing =
//    throw CliktError(message)
//
//private fun List<String>?.ownersToString() =
//    this?.joinToString(separator = " ") ?: "??"
