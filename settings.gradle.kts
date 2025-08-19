pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://s01.oss.sonatype.org/content/repositories/releases")
        maven(url = "https://maven.rikka.dev")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        maven(url = "https://s01.oss.sonatype.org/content/repositories/releases")
        maven(url = "https://maven.rikka.dev")
    }
}

rootProject.name = "ShizukuSmartDnsVPN"
include(":app")
