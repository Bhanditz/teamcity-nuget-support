/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.server.trigger.impl.checker;

import com.intellij.openapi.diagnostic.Logger;
import jetbrains.buildServer.nuget.common.FeedConstants;
import jetbrains.buildServer.nuget.server.exec.SourcePackageInfo;
import jetbrains.buildServer.nuget.server.exec.SourcePackageReference;
import jetbrains.buildServer.nuget.server.feed.reader.FeedPackage;
import jetbrains.buildServer.nuget.server.feed.reader.NuGetFeedReader;
import jetbrains.buildServer.nuget.server.trigger.impl.CheckResult;
import jetbrains.buildServer.nuget.server.trigger.impl.CheckablePackage;
import jetbrains.buildServer.nuget.server.trigger.impl.PackageCheckRequest;
import jetbrains.buildServer.nuget.server.trigger.impl.mode.CheckRequestModeTeamCity;
import jetbrains.buildServer.util.ExceptionUtil;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.ExecutorService;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 30.09.11 16:43
 */
public class PackageCheckerTeamCity implements PackageChecker {
  private static final Logger LOG = Logger.getInstance(PackageCheckerTeamCity.class.getName());

  private final NuGetFeedReader myReader;

  public PackageCheckerTeamCity(@NotNull NuGetFeedReader reader) {
    myReader = reader;
  }

  public boolean accept(@NotNull PackageCheckRequest request) {
    return request.getMode() instanceof CheckRequestModeTeamCity;
  }

  public void update(@NotNull ExecutorService executor, @NotNull Collection<CheckablePackage> entries) {
    for (final CheckablePackage entry : entries) {
      entry.setExecuting();
      executor.submit(ExceptionUtil.catchAll("Check update of NuGet package " + entry.getPackage().getPackageId(), new Runnable() {
        private boolean isNetworkSource(@NotNull String uri) {
          uri = uri.toLowerCase().trim();
          return uri.startsWith("http://") || uri.startsWith("https://");
        }

        @NotNull
        private String getUri(@NotNull final SourcePackageReference request) {
          String uri = request.getSource();
          if (uri == null) uri = FeedConstants.MS_REF_FEED_V2;
          return uri;
        }

        public void run() {
          final String packageId = entry.getPackage().getPackageId();
          final String uri = getUri(entry.getPackage());

          if (!isNetworkSource(uri)) {
            entry.setResult(CheckResult.failed("Current environment does not allow to start NuGet.exe processes, " +
                    "TeamCity provided emulation supports only HTTP or HTTPS NuGet package feed URLs, " +
                    "but was: " + uri));
            return;
          }

          try {
            final Collection<FeedPackage> packages = myReader.queryPackageVersions(uri, entry.getPackage().getUsername(), entry.getPackage().getPassword(), packageId);
            final Collection<SourcePackageInfo> infos = new ArrayList<SourcePackageInfo>();
            for (FeedPackage aPackage : packages) {
              infos.add(new SourcePackageInfo(entry.getPackage().getSource(), packageId, aPackage.getInfo().getVersion()));
            }

            entry.setResult(CheckResult.fromResult(infos));
          } catch (Throwable e) {
            final String msg = "Failed to check changes of " + packageId + ". " + e.getMessage();
            LOG.warn(msg, e);
            entry.setResult(CheckResult.failed(msg));
          }
        }
      }));
    }
  }
}
