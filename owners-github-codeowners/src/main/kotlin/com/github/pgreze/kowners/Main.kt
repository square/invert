package com.github.pgreze.kowners

import java.io.File


fun main() {
    val file = "build"
    val resolver by lazy {
        OwnersResolver(
            """
# Example CODEOWNERS content
*.kt                @kotlin-dev
build/              @build-team
docs/**             @documentation-team
config/*.json       @config-team
**/logs/            @logging-team
test/**/*           @api-team
**/*.css            @frontend-team
assets/images/*     @design-team
**/api/*            @api-team
scripts/**/*.js     @script-team
""".trimIndent().lines().parseCodeOwners()
        )
    }
//val resolver by lazy { OwnersResolver(codeOwnersFile.readLines().parseCodeOwners()) }


    val filePaths = listOf(
        "build", "build/index.html", "docs/index.md", "docs/project/outline.md",
        "config/settings.json", "config/backup/config.old.json", "application/logs/error.log",
        "test/api/testcase.json", "styles/app.css", "assets/images/logo.png",
        "assets/images/deep/nested/picture.jpg", "src/api/user/get_user_details.api",
        "scripts/ui/load.js", "scripts/deep/nested/script.js", "root.log",
        "logs/server.log", "deep/nested/structure/styles/page.css"
    ).forEach { filePath ->
        val owners = resolver.resolveOwnership(filePath)
//        val owners = findOwnersForFile(filePath, entries)
        println("Owners for $filePath: $owners")
    }

//    val owners = 
//    
//    println(owners)
}