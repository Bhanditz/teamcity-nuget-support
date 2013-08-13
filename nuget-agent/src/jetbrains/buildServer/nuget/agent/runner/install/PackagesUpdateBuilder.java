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

package jetbrains.buildServer.nuget.agent.runner.install;

import jetbrains.buildServer.RunBuildException;
import jetbrains.buildServer.agent.BuildRunnerContext;
import jetbrains.buildServer.nuget.agent.commands.NuGetActionFactory;
import jetbrains.buildServer.nuget.agent.parameters.PackagesUpdateParameters;
import jetbrains.buildServer.nuget.agent.runner.install.impl.locate.PackagesInstallerAdapter;
import jetbrains.buildServer.nuget.agent.util.BuildProcessContinuation;
import jetbrains.buildServer.nuget.common.PackagesUpdateMode;
import org.jetbrains.annotations.NotNull;

import java.io.File;

import static jetbrains.buildServer.nuget.common.PackagesUpdateMode.FOR_EACH_PACKAGES_CONFIG;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 18.06.12 12:10
 */
public class PackagesUpdateBuilder extends PackagesInstallerAdapter {
  private final NuGetActionFactory myActionFactory;
  private final BuildProcessContinuation myUpdateStages;
  private final BuildRunnerContext myContext;
  private final PackagesUpdateParameters myUpdateParameters;

  public PackagesUpdateBuilder(@NotNull final NuGetActionFactory actionFactory,
                               @NotNull final BuildProcessContinuation updateStages,
                               @NotNull final BuildRunnerContext context,
                               @NotNull final PackagesUpdateParameters updateParameters) {
    myContext = context;
    myUpdateStages = updateStages;
    myUpdateParameters = updateParameters;
    myActionFactory = actionFactory;
  }

  public void onSolutionFileFound(@NotNull File sln, @NotNull File targetFolder) throws RunBuildException {
    super.onSolutionFileFound(sln, targetFolder);

    if (myUpdateParameters.getUpdateMode() != PackagesUpdateMode.FOR_SLN) return;

    myUpdateStages.pushBuildProcess(
            myActionFactory.createUpdate(
                    myContext,
                    myUpdateParameters,
                    sln,
                    targetFolder
            )
    );
  }

  public void onPackagesConfigFound(@NotNull final File config, @NotNull final File targetFolder) throws RunBuildException {
    super.onPackagesConfigFound(config, targetFolder);

    if (myUpdateParameters.getUpdateMode() != FOR_EACH_PACKAGES_CONFIG) return;
    myUpdateStages.pushBuildProcess(
            myActionFactory.createUpdate(
                    myContext,
                    myUpdateParameters,
                    config,
                    targetFolder
            )
    );
  }
}
