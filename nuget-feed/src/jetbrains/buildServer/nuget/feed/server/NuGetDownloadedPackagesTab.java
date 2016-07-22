/*
 * Copyright 2000-2016 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.feed.server;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.nuget.common.NuGetPackageInfo;
import jetbrains.buildServer.nuget.common.PackageDependencies;
import jetbrains.buildServer.nuget.common.PackageDependenciesStore;
import jetbrains.buildServer.nuget.common.SourcePackageInfo;
import jetbrains.buildServer.nuget.feed.server.index.NuGetIndexEntry;
import jetbrains.buildServer.nuget.feed.server.index.PackagesIndex;
import jetbrains.buildServer.serverSide.SBuild;
import jetbrains.buildServer.serverSide.SBuildServer;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifact;
import jetbrains.buildServer.serverSide.artifacts.BuildArtifactsViewMode;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import jetbrains.buildServer.web.openapi.ViewLogTab;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import static jetbrains.buildServer.nuget.common.PackagesConstants.NUGET_USED_PACKAGES_DIR;
import static jetbrains.buildServer.nuget.common.PackagesConstants.NUGET_USED_PACKAGES_FILE;

/**
 * Created Eugene Petrenko (eugene.petrenko@gmail.com)
 * date: 28.04.11
 */
public class NuGetDownloadedPackagesTab extends ViewLogTab {
  private static final String DEPS_FILE = NUGET_USED_PACKAGES_DIR + "/" + NUGET_USED_PACKAGES_FILE;
  private static final Logger LOG = Logger.getInstance(NuGetDownloadedPackagesTab.class.getName());

  private final PackageDependenciesStore myStore;
  private final PackagesIndex myPackageFeed;

  public NuGetDownloadedPackagesTab(@NotNull final PagePlaces pagePlaces,
                                    @NotNull final SBuildServer server,
                                    @NotNull final PluginDescriptor descriptor,
                                    @NotNull final PackageDependenciesStore store,
                                    @NotNull final PackagesIndex packageFeed) {
    super("NuGet Packages", "nugetPackagesBuildTab", pagePlaces, server);
    myStore = store;
    myPackageFeed = packageFeed;
    setIncludeUrl(descriptor.getPluginResourcesPath("showPackages.jsp"));
    register();
  }

  @Override
  protected boolean isAvailable(@NotNull HttpServletRequest request, @NotNull SBuild build) {
    return super.isAvailable(request, build) && getPackagesFile(build) != null;
  }

  @Nullable
  private BuildArtifact getPackagesFile(@Nullable final SBuild build) {
    if (build == null) return null;
    BuildArtifact file = build.getArtifacts(BuildArtifactsViewMode.VIEW_ALL).getArtifact(DEPS_FILE);
    if (file == null) return null;
    if (file.isDirectory()) return null;
    return file;
  }


  @Override
  protected void fillModel(@NotNull final Map<String, Object> model,
                           @NotNull final HttpServletRequest request,
                           @Nullable final SBuild build) {
    if (build == null) return;
    model.put("packages", loadDependencies(build));

    Set<NuGetPackageInfo> infos = new TreeSet<NuGetPackageInfo>();
    for(Iterator<NuGetIndexEntry> entries = myPackageFeed.getNuGetEntries(build.getBuildId()); entries.hasNext();) {
      infos.add(entries.next().getPackageInfo());
    }

    model.put("feedPackages", infos);
  }

  @NotNull
  private PackageDependencies loadDependencies(@NotNull final SBuild build) {
    final BuildArtifact file = getPackagesFile(build);
    if (file != null) {
      InputStream inputStream = null;
      try {
        inputStream = file.getInputStream();
        return myStore.load(inputStream);
      } catch (IOException e) {
        LOG.warn("Failed to read used packages build artifacts of build id=" + build.getBuildId());
      } finally {
        FileUtil.close(inputStream);
      }
    }
    return new PackageDependencies(
            Collections.<NuGetPackageInfo>emptyList(),
            Collections.<NuGetPackageInfo>emptyList(),
            Collections.<SourcePackageInfo>emptyList());
  }
}
