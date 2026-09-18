pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}
rootProject.name = "RelationshipRadar"
include(":app")
include(":whispercpp")
project(":whispercpp").projectDir = file("third_party/whisper.cpp/examples/whisper.android/lib")
