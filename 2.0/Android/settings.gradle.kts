pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // 本地 TRTC SDK aar（仓库根目录 libs/）
        flatDir {
            dirs(File(settingsDir, "libs").absolutePath)
        }
    }
}

rootProject.name = "AIConversationDemo"
include(":app")
include(":aiconversation")
