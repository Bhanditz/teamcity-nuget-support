/*
 * Copyright 2000-2017 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.agent.dependencies;

import jetbrains.buildServer.nuget.common.PackageDependencies;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 18.07.11 22:43
 */
public interface NuGetPackagesCollector {

  /**
   * Adds used package to the list of packages
   * @param packageId package Id
   * @param version version
   * @param allowedVersions version constraint
   */
  void addUsedPackage(@NotNull String packageId,
                      @NotNull String version,
                      @Nullable String allowedVersions);

  /**
   * Adds create package to the list of packages
   * @param packageId package Id
   * @param version version
   */
  void addCreatedPackage(@NotNull String packageId,
                         @NotNull String version);


  /**
   * Adds published package
   * @param packageId package id
   * @param version version
   * @param source source
   */
  void addPublishedPackage(@NotNull String packageId, @NotNull String version, @Nullable String source);

  /**
   * @return sorted list of packages that were registered
   */
  @NotNull
  public PackageDependencies getPackages();
}
