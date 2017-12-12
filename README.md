# TeamCity NuGet support [![official JetBrains project](http://jb.gg/badges/official-plastic.svg)](https://confluence.jetbrains.com/display/ALL/JetBrains+on+GitHub) [![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

This plugin provides NuGet support features for TeamCity. For more details, please see [NuGet support description](https://confluence.jetbrains.com/display/TCDL/NuGet).

# Download

The plugin is bundled from TeamCity 7.0. If you need the latest build, download and install it as an [additional TeamCity plugin](https://confluence.jetbrains.com/display/TCDL/Installing+Additional+Plugins).

| Plugin | Status | Download | TeamCity |
|--------|--------|----------|----------|
| master | <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_NuGet_NuGetSupportTrunkFor20172x&guest=1"><img src="https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:TeamCityPluginsByJetBrains_NuGet_NuGetSupportTrunkFor20172x)/statusIcon.svg" alt=""/></a> | [Download](https://teamcity.jetbrains.com/repository/download/TeamCityPluginsByJetBrains_NuGet_NuGetSupportTrunkFor20172x/.lastSuccessful/dotNetPackagesSupport.zip?guest=1)| 2017.2.x |
| 0.15 | <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_NuGet_NuGetSupportV015for10x&guest=1"><img src="https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:TeamCityPluginsByJetBrains_NuGet_NuGetSupportV015for10x)/statusIcon.svg" alt=""/></a> | [Download](https://teamcity.jetbrains.com/repository/download/TeamCityPluginsByJetBrains_NuGet_NuGetSupportV015for10x/.lastSuccessful/dotNetPackagesSupport.zip?guest=1)| 2017.1.x |
| 0.14 | <a href="https://teamcity.jetbrains.com/viewType.html?buildTypeId=TeamCityPluginsByJetBrains_NuGet_NuGetSupportV014for100x&guest=1"><img src="https://teamcity.jetbrains.com/app/rest/builds/buildType:(id:TeamCityPluginsByJetBrains_NuGet_NuGetSupportV014for100x)/statusIcon.svg" alt=""/></a> | [Download](https://teamcity.jetbrains.com/repository/download/TeamCityPluginsByJetBrains_NuGet_NuGetSupportV014for100x/.lastSuccessful/dotNetPackagesSupport.zip?guest=1)| 10.0.x |

# Building the plugin
This project uses gradle as a build system. To resolve non-public libraries, you need to execute `:nuget-server:installTeamCity` gradle task or have a local TeamCity installation and define `teamcityDir` in the [gradle properties](https://docs.gradle.org/current/userguide/build_environment.html). After that you can open it in [IntelliJ IDEA](https://www.jetbrains.com/idea/help/importing-project-from-gradle-model.html) or [Eclipse](http://gradle.org/eclipse/).

## Gradle tasks
* `:nuget-extensions:msbuild` - build .net nuget extensions.
* `:nuget-extensions:nunit` - run .net nuget extensions tests.
* `:nuget-server:installTeamCity` - downloads TeamCity distribution.
* `:nuget-server:assemble` - assemble nuget support plugin.
* `:nuget-server:build` - build & test nuget support plugin.

## Requirements

On Windows to build nuget extensions should be installed Microsoft Build Tools.
On Linux for that should be installed 'mono-devel' package.

# Contributions
We appreciate all kinds of feedback, so please feel free to send a PR or write [an issue](https://github.com/JetBrains/teamcity-nuget-support/issues).

