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

package jetbrains.buildServer.nuget.server.feed.server.index.impl.latest;

import jetbrains.buildServer.nuget.server.feed.server.index.impl.NuGetPackageBuilder;
import jetbrains.buildServer.nuget.server.feed.server.index.impl.SemanticVersionsComparators;
import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

/**
 * Created 19.03.13 14:31
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public abstract class BaseLatestCalculator implements LatestCalculator {
  private final Comparator<String> myVersionsComparator = SemanticVersionsComparators.getSemanticVersionsComparator();
  private final Map<String, NuGetPackageBuilder> myLatestPackages = new HashMap<String, NuGetPackageBuilder>();

  public void updatePackage(@NotNull NuGetPackageBuilder newLatest) {
    final NuGetPackageBuilder currentLatest = myLatestPackages.get(newLatest.getPackageName());

    if (currentLatest == null || myVersionsComparator.compare(currentLatest.getVersion(), newLatest.getVersion()) < 0) {
      myLatestPackages.put(newLatest.getPackageName(), newLatest);
    }
  }

  public void updateSelectedPackages() {
    for (NuGetPackageBuilder builder : myLatestPackages.values()) {
      updatePackageVersion(builder);
    }
    myLatestPackages.clear();
  }

  protected abstract void updatePackageVersion(@NotNull NuGetPackageBuilder builder);
}
