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

package jetbrains.buildServer.nuget.server.feed.server.tab;

import jetbrains.buildServer.nuget.server.settings.SettingsSection;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 26.10.11 19:21
 */
public class FeedServerSettingsSection implements SettingsSection {
  private final String myPath;
  private final String mySettingsPath;
  private final String myJSPath;
  private final String myCssPath;

  public FeedServerSettingsSection(@NotNull PluginDescriptor descriptor) {
    myPath = descriptor.getPluginResourcesPath("feed/status.html");
    mySettingsPath = descriptor.getPluginResourcesPath("feed/settings.html");
    myJSPath = descriptor.getPluginResourcesPath("server/feedServer.js");
    myCssPath = descriptor.getPluginResourcesPath("server/feedServer.css");
  }

  @NotNull
  public String getSectionId() {
    return "feed-server";
  }

  @NotNull
  public String getSectionName() {
    return "NuGet Server";
  }

  @NotNull
  public String getIncludePath() {
    return myPath;
  }

  @NotNull
  public String getSettingsPath() {
    return mySettingsPath;
  }

  @NotNull
  public Collection<String> getCssFiles() {
    return Collections.singleton(myCssPath);
  }

  @NotNull
  public Collection<String> getJsFiles() {
    return Collections.singleton(myJSPath);
  }
}
