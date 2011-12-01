/*
 * Copyright 2000-2011 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.tests.agent;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.nuget.agent.runner.install.impl.RepositoryPathResolver;
import jetbrains.buildServer.nuget.agent.runner.install.impl.RepositoryPathResolverImpl;
import jetbrains.buildServer.nuget.tests.integration.Paths;
import jetbrains.buildServer.util.FileUtil;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;

/**
 * @author Eugene Petrenko (eugene.petrenko@gmail.com)
 *         Date: 01.12.11 18:32
 */
public class RepositoryPathResolverTest extends BaseTestCase {
  private RepositoryPathResolver myResolver;
  private File myHome;
  private File mySln;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    myHome = createTempDir();
    mySln = new File(myHome, "project.sln");

    FileUtil.writeFile(mySln, "fake sln file content");
    myResolver = new RepositoryPathResolverImpl();
  }

  @Test
  public void testDefaultPath() {
    doResolveTest("packages");
  }

  @Test
  public void testResolveWithConfig_01() throws IOException {
    FileUtil.copy(Paths.getTestDataPath("config/NuGet-01.config"), new File(myHome, "NuGet.config"));
    doResolveTest("../lib");
  }

  @Test
  public void testResolveWithConfig_02() throws IOException {
    FileUtil.copy(Paths.getTestDataPath("config/NuGet-02.Config"), new File(myHome, "nuget.config"));
    doResolveTest("packages");
  }

  @Test
  public void testResolveWithConfig_03() throws IOException {
    FileUtil.copy(Paths.getTestDataPath("config/NuGet-03.Config"), new File(myHome, "nuget.config"));
    doResolveTest("../lib");
  }

  private void doResolveTest(String packages) {
    final File actual = myResolver.resolvePath(mySln);
    Assert.assertEquals(actual, FileUtil.getCanonicalFile(actual), "should return absolute canonical path");
    Assert.assertEquals(actual, new File(myHome, packages));
  }
}
