/*
 * Copyright 2000-2014 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package jetbrains.buildServer.nuget.agent.runner.install.impl;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.agent.BuildProgressLogger;
import jetbrains.buildServer.util.FileUtil;
import org.jdom.Attribute;
import org.jdom.Element;
import org.jdom.Text;
import org.jdom.xpath.XPath;
import org.jetbrains.annotations.NotNull;

import java.io.File;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 01.12.11 18:16
 */
public class RepositoryPathResolverImpl implements RepositoryPathResolver {
  private static final Logger LOG = Logger.getInstance(RepositoryPathResolverImpl.class.getName());
  private static final String NUGET_CONFIG = "nuget.config";
  private static final String PACKAGES = "packages";

  @NotNull
  public File resolveRepositoryPath(@NotNull final BuildProgressLogger logger,
                                    @NotNull final File solutionFile) {

    final File repositoryPath = resolveRepositoryPathImpl(logger, solutionFile);
    //noinspection ResultOfMethodCallIgnored
    repositoryPath.mkdirs();
    if (!repositoryPath.isDirectory()) {
      logger.warning("Failed to create packages directory: " + repositoryPath);
    }
    return repositoryPath;
  }

  private File resolveRepositoryPathImpl(@NotNull final BuildProgressLogger logger,
                                         @NotNull final File solutionFile) {
    final File home = solutionFile.getParentFile();
    final File configFilePath = new File(home, NUGET_CONFIG);

    final File defaultRepositoryPath = new File(home, PACKAGES);
    if (!configFilePath.isFile()) {
      LOG.warn("Failed to find NuGet.config file at " + configFilePath + ". Packages will be downloaded into default path: " + defaultRepositoryPath + ".");
      return defaultRepositoryPath;
    }

    LOG.debug("Found NuGet.config file: " + configFilePath);
    try {
      final Element element = FileUtil.parseDocument(configFilePath);
      {
        final Attribute pathAttribute = (Attribute) XPath.newInstance("/configuration/config/add[@key='repositoryPath']/@value").selectSingleNode(element);
        if (pathAttribute != null) {
          String text = pathAttribute.getValue().trim();
          LOG.info("Found packages path: " + text);
          return FileUtil.resolvePath(home, text);
        }
      }

      {
        final Text pathText = (Text) XPath.newInstance("/configuration/repositoryPath/text()").selectSingleNode(element);
        if (pathText != null) {
          final String text = pathText.getTextTrim();
          LOG.info("Found packages path: " + text);
          return FileUtil.resolvePath(home, text);
        }
      }

      {
        final Text pathText = (Text) XPath.newInstance("/settings/repositoryPath/text()").selectSingleNode(element);
        if (pathText != null) {
          final String text = pathText.getTextTrim();
          LOG.info("Found packages path: " + text);
          return FileUtil.resolvePath(home, text);
        }
      }
    } catch (final Exception e) {
      final String message = "Failed to parse NuGet.config file at " + configFilePath + ". Packages will be downloaded into default path: " + defaultRepositoryPath + ". " + e.getMessage();
      logger.warning(message);
      LOG.warn(message, e);
      return defaultRepositoryPath;
    }

    return defaultRepositoryPath;
  }
}
