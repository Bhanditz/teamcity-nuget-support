package jetbrains.buildServer.nuget.agent.index

import jetbrains.buildServer.ExtensionHolder
import jetbrains.buildServer.agent.impl.artifacts.ArchivePreprocessor

class NuGetPackagePathProvider(private val extensions: ExtensionHolder) {
    fun getArtifactPath(path: String, fileName: String): String? {
        val targetPath = path.trimEnd('\\', '/')
        if (targetPath.isEmpty()) {
            return fileName
        }

        // We should not index packages withing archives
        if (targetPath.contains(ARCHIVE_PATH_SEPARATOR) ||
            extensions.getExtensions(ArchivePreprocessor::class.java).any { it.shouldProcess(targetPath) }) {
            return null
        }

        return "$targetPath/$fileName"
    }

    companion object {
        private const val ARCHIVE_PATH_SEPARATOR = "!/"
    }
}
