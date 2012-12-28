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

package jetbrains.buildServer.nuget.server.toolRegistry.tab;

import org.jetbrains.annotations.NotNull;

/**
 * Created by Eugene Petrenko (eugene.petrenko@gmail.com)
 * Date: 11.08.11 12:41
 */
public class InstallableTool {
  private String myId;
  private String myVersion;
  private boolean myIsAllreadyInstalled;

  public InstallableTool(String id, String version, boolean isAllreadyInstalled) {
    myId = id;
    myVersion = version;
    myIsAllreadyInstalled = isAllreadyInstalled;
  }

  @NotNull
  public String getId() {
    return myId;
  }

  @NotNull
  public String getVersion() {
    return myVersion;
  }

  public boolean isAllreadyInstalled() {
    return myIsAllreadyInstalled;
  }
}
