/*
 * Copyright 2000-2015 JetBrains s.r.o.
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

package jetbrains.buildServer.nuget.tests.server.tools;

import com.google.common.collect.Sets;
import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.nuget.common.NuGetToolReferenceUtils;
import jetbrains.buildServer.nuget.server.settings.impl.NuGetSettingsManagerImpl;
import jetbrains.buildServer.nuget.server.toolRegistry.*;
import jetbrains.buildServer.nuget.server.toolRegistry.impl.*;
import jetbrains.buildServer.nuget.server.toolRegistry.impl.impl.DownloadableNuGetTool;
import jetbrains.buildServer.nuget.server.toolRegistry.impl.impl.NuGetToolManagerImpl;
import org.jetbrains.annotations.NotNull;
import org.jmock.Expectations;
import org.jmock.Mockery;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;

/**
 * Created 27.12.12 18:34
 *
 * @author Eugene Petrenko (eugene.petrenko@jetbrains.com)
 */
public class NuGetToolManagerTest extends BaseTestCase {
  private Mockery m;
  private NuGetToolsInstaller myInstaller;
  private ToolsRegistry myToolsRegistry;
  private NuGetToolManagerImpl myToolManager;
  private AvailableToolsState myAvailables;

  @BeforeMethod
  @Override
  protected void setUp() throws Exception {
    super.setUp();
    m = new Mockery();
    myAvailables = m.mock(AvailableToolsState.class);
    myInstaller = m.mock(NuGetToolsInstaller.class);
    myToolsRegistry = m.mock(ToolsRegistry.class);
    myToolManager = new NuGetToolManagerImpl(
            myAvailables,
            myInstaller,
            myToolsRegistry,
            new NuGetToolsSettings(new NuGetSettingsManagerImpl()));
  }

  @Test
  public void test_install_tool() throws ToolException {
    final InstalledTool it = m.mock(InstalledTool.class);

    m.checking(new Expectations(){{
      oneOf(myInstaller).installNuGet("filename", new File("foo"));
      oneOf(myToolsRegistry).findTool("id"); will(returnValue(it));
    }});

    assertSame(it, myToolManager.installTool("id", "filename", new File("foo")));
  }

  @Test
  public void test_default_tool_null() {
    m.checking(new Expectations(){{
      allowing(myToolsRegistry).findTool(null); will(returnValue(null));
    }});
    assertNull(myToolManager.getDefaultTool());
  }

  @Test
  public void test_default_tool_2() {
    myToolManager.setDefaultTool("aaa");
    final InstalledTool it = m.mock(InstalledTool.class);
    m.checking(new Expectations(){{
      allowing(it).getId(); will(returnValue("aaa"));
      allowing(it).getNuGetExePath(); will(returnValue(new File("some-path")));
      allowing(it).getVersion(); will(returnValue("42.333.667"));
      allowing(myToolsRegistry).findTool("aaa"); will(returnValue(it));
    }});

    NuGetInstalledTool ret = myToolManager.getDefaultTool();
    assertNotNull(ret);
    assertTrue(ret.isDefaultTool());
    assertEquals(it.getId(), ret.getId());
    assertEquals(it.getNuGetExePath(), ret.getNuGetExePath());
    assertEquals(it.getVersion(), ret.getVersion());
  }

  @Test
  public void test_default_tool_3() {
    myToolManager.setDefaultTool("aaa");

    m.checking(new Expectations(){{
      allowing(myToolsRegistry).findTool("aaa"); will(returnValue(null));
    }});

    NuGetInstalledTool ret = myToolManager.getDefaultTool();
    assertNull(ret);
  }

  @Test
  public void test_set_default() {
    myToolManager.setDefaultTool("some_tool");
    assertEquals("some_tool", myToolManager.getDefaultToolId());
  }

  @Test
  public void test_nuget_path_custom() throws IOException {
    File file = createTempFile();
    assertEquals(file.getPath(), myToolManager.getNuGetPath(file.getPath()));
  }

  @Test
  public void test_nuget_path_preset() throws IOException {
    final InstalledTool it = m.mock(InstalledTool.class);
    m.checking(new Expectations(){{
      allowing(it).getNuGetExePath(); will(returnValue(new File("some-path")));
      allowing(myToolsRegistry).findTool("aaa"); will(returnValue(it));
    }});

    assertEquals(myToolManager.getNuGetPath("?aaa"), "some-path");
  }

  @Test
  public void test_nuget_resolves_default_tool() {
    myToolManager.setDefaultTool("aaa");

    final InstalledTool it = m.mock(InstalledTool.class);
    m.checking(new Expectations(){{
      allowing(it).getNuGetExePath(); will(returnValue(new File("some-path")));
      allowing(myToolsRegistry).findTool("aaa"); will(returnValue(it));
    }});

    assertEquals("some-path", myToolManager.getNuGetPath(NuGetToolReferenceUtils.getDefaultToolReference()));
  }

  @Test(expectedExceptions = RuntimeException.class)
  public void test_nuget_resolves_default_tool_not_found() {
    myToolManager.setDefaultTool("bbb");

    m.checking(new Expectations(){{
      allowing(myToolsRegistry).findTool("bbb"); will(returnValue(null));
    }});

    myToolManager.getNuGetPath(NuGetToolReferenceUtils.getDefaultToolReference());
  }

  @Test
  public void test_nuget_manager_should_not_return_default_tool_id() {
    myToolManager.setDefaultTool("aaa");
    final NuGetInstalledTool it = m.mock(NuGetInstalledTool.class);
    m.checking(new Expectations(){{
      allowing(it).getId(); will(returnValue("aaa"));
      allowing(myToolsRegistry).getTools(); will(returnValue(Collections.singletonList(it)));
    }});

    final String defaultId = NuGetToolReferenceUtils.getDefaultToolReference();
    for (NuGetInstalledTool tool : myToolManager.getInstalledTools()) {
      assertFalse(tool.getId().equals(defaultId));
    }
  }

  @Test
  public void test_GetAvailableTools_ShouldNotModifyRecievedAvailableToolsSet() throws Exception {
    final DownloadableNuGetTool dt = m.mock(DownloadableNuGetTool.class);
    final InstalledTool it = m.mock(InstalledTool.class);
    m.checking(new Expectations() {{
      allowing(dt).getVersion(); will(returnValue("v1"));
      allowing(it).getVersion(); will(returnValue("v1"));}
    });
    m.checking(new Expectations(){{
      allowing(myAvailables).getAvailable(ToolsPolicy.FetchNew); will(returnValue(FetchAvailableToolsResult.createSuccessfull(Collections.singleton(dt))));
      allowing(myToolsRegistry).getTools(); will(returnValue(Collections.singletonList(it)));
    }});
    myToolManager.getAvailableTools(ToolsPolicy.FetchNew);
  }

  @Test
  public void test_GetAvailableTools_ShouldSortAvailableToolsSet() throws Exception {
    final InstalledTool it = m.mock(InstalledTool.class);
    m.checking(new Expectations(){{
      allowing(it).getVersion(); will(returnValue("1"));
      allowing(myAvailables).getAvailable(ToolsPolicy.FetchNew); will(returnValue(FetchAvailableToolsResult.createSuccessfull(Sets.newHashSet(toolOfVersion("1"), toolOfVersion("5"), toolOfVersion("3")))));
      allowing(myToolsRegistry).getTools(); will(returnValue(Collections.singletonList(it)));
    }});

    final FetchAvailableToolsResult result = myToolManager.getAvailableTools(ToolsPolicy.FetchNew);
    assertEmpty(result.getErrors());

    Iterator<? extends NuGetTool> tools = result.getFetchedTools().iterator();
    assertEquals("5", tools.next().getVersion());
    assertEquals("3", tools.next().getVersion());
    assertFalse(tools.hasNext());
  }

  private static DownloadableNuGetTool toolOfVersion(final String version){
    return new DownloadableNuGetTool() {
      @NotNull
      public String getDownloadUrl() {
        return "downloadUrl-for-" + getId();
      }

      @NotNull
      public String getDestinationFileName() {
        return "some_path";
      }

      @NotNull
      public String getId() {
        return "nuget-exe-" + version;
      }

      @NotNull
      public String getVersion() {
        return version;
      }
    };
  }
}

