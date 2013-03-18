<%--
  ~ Copyright 2000-2012 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%@ taglib prefix="forms" tagdir="/WEB-INF/tags/forms" %>
<%@ taglib prefix="props" tagdir="/WEB-INF/tags/props" %>
<%@ taglib prefix="l" tagdir="/WEB-INF/tags/layout" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<jsp:useBean id="ib" class="jetbrains.buildServer.nuget.server.runner.pack.PackBean" scope="request"/>

<l:settingsGroup title="NuGet settings">
  <tr>
    <th>NuGet.exe<l:star/>:</th>
    <td>
      <jsp:include page="../tool/runnerSettings.html?name=${ib.nuGetPathKey}&class=longField"/>
    </td>
  </tr>
</l:settingsGroup>

<script type="text/javascript">
  appendSpecificationFile = function(specFile) {
    var textarea = $j(BS.Util.escapeId('${ib.packSpecFile}'));
    var val = textarea.text();
    if (val.length > 0) {
      var lines = val.split("\n");
      lines.push(specFile);
      textarea.text(lines.join("\n"));
    } else {
      textarea.text(specFile);
    }
  };
</script>
<l:settingsGroup title="Package parameters">
  <tr>
    <th rowspan="2"><label for="${ib.packSpecFile}">Specification files</label><l:star/>:</th>
    <td>
      <props:multilineProperty name="${ib.packSpecFile}" linkTitle="Specification or project files" cols="60" rows="5" expanded="${true}"/>
      <bs:vcsTree callback="appendSpecificationFile" treeId="${ib.packSpecFile}"/>
      <script type="text/javascript">
        BS.Util.hide($('vcsTreeControl_${ib.packSpecFile}'));
      </script>
      <span class="smallNote">Specify paths to .nuspec files and/or to Visual Studio project files (i.e. .csproj or .vbproj). MSBuild-style wildcards are supported</span>
      <span id="error_${ib.packSpecFile}" class="error"></span>
    </td>
  </tr>
  <tr>
    <td>
      <props:checkboxProperty name="${ib.packPreferProject}"/>
      <label for="${ib.packPreferProject}">Prefer project files to .nuspec</label>
      <span class="smallNote">Use project file (if exists, i.e. .csproj or .vbproj) for every matched .nuspec file</span>
    </td>
  </tr>

  <tr>
    <th><label for="${ib.packVersion}">Version</label>:</th>
    <td>
      <props:textProperty name="${ib.packVersion}" className="longField"/>
      <span class="smallNote">Specify version for package to create (recommended) or leave it blank to let NuGet decide</span>
      <span id="error_${ib.packVersion}" class="error"></span>
    </td>
  </tr>

  <props:selectSectionProperty name="${ib.packBaseDirectoryMode}" title="Base Directory">
    <c:forEach var="it" items="${ib.packBaseDirectoryModes}">
      <props:selectSectionPropertyContent value="${it.value}" caption="${it.description}">
        <c:if test="${it.showBaseDirectorySelector}">
          <tr>
            <th></th>
            <td>
              <props:textProperty name="${ib.packBaseDirectory}" className="longField"/>
              <bs:vcsTree fieldId="${ib.packBaseDirectory}" treeId="${ib.packBaseDirectory}"/>
              <span class="smallNote">Base directory for packing. Leave blank to use build checkout directory</span>
              <span id="error_${ib.packBaseDirectory}" class="error"></span>
            </td>
          </tr>
        </c:if>
      </props:selectSectionPropertyContent>
    </c:forEach>
  </props:selectSectionProperty>

</l:settingsGroup>

<l:settingsGroup title="Output">
  <tr>
    <th rowspan="3"><label for="${ib.packOutputDirectory}">Output Directory</label><l:star/>:</th>
    <td>
      <props:textProperty name="${ib.packOutputDirectory}" className="longField"/>
      <span class="smallNote">Specify path to put generated NuGet package. Specify directory to put generated NuGet packages into. See also <em>NuGet Publish</em> build runner.</span>
      <span id="error_${ib.packOutputDirectory}" class="error"></span>
    </td>
  </tr>
  <tr>
    <td>
      <props:checkboxProperty name="${ib.packOutputClear}"/><label for="${ib.packOutputClear}">Clean output directory</label>
    </td>
  </tr>
  <tr>
    <td>
      <props:checkboxProperty name="${ib.packAsArtifact}"/><label for="${ib.packAsArtifact}">Publish created packages to build artifacts</label>
      <span class="smallNote">Created packages will be published to the root of artifacts</span>
    </td>
  </tr>
</l:settingsGroup>

<l:settingsGroup title="Additionals parameters">

  <tr>
    <th><label for="${ib.packExcludePatterns}">Exclude files: </label></th>
    <td>
      <props:multilineProperty name="${ib.packExcludePatterns}" linkTitle="Exclude files" cols="60" rows="5" />
      <span class="smallNote">Exclude files when creating a package. Equavalent to NuGet.exe -Exclude argument</span>
      <span id="error_${ib.packExcludePatterns}" class="error"></span>
    </td>
  </tr>

  <tr>
    <th><label for="${ib.packProperties}">Properties:</label></th>
    <td>
      <props:multilineProperty name="${ib.packProperties}" linkTitle="Properties" cols="60" rows="5" />
      <span class="smallNote">
        Semicolon or newline separated list of package creation properties (i.e. key=value) to pass to NuGet.exe
      </span>
      <span id="error_${ib.packProperties}" class="error"></span>
    </td>
  </tr>

  <tr>
    <th rowspan="2">Options:</th>
    <td>
      <props:checkboxProperty name="${ib.packAsTool}"/>
      <label for="${ib.packAsTool}">Create <strong>tool</strong> package</label>
    </td>
  </tr>
  <tr>
    <td>
      <props:checkboxProperty name="${ib.packSources}"/>
      <label for="${ib.packSources}">Include sources and symbols</label>
    </td>
  </tr>

  <tr>
    <th><label for="${ib.packCustomCommandline}">Additional commandline arguments:</label></th>
    <td>
      <props:multilineProperty name="${ib.packCustomCommandline}" linkTitle="Commandline" cols="60" rows="5" />
      <span class="smallNote">Additional commandline parameters to add to calling NuGet.exe</span>
      <span id="error_${ib.packCustomCommandline}" class="error"></span>
    </td>
  </tr>

</l:settingsGroup>
<script type="text/javascript">
  $j(document).ready(function() {
    //move vcs-tree icon to the left from textarea, after completion icon
    var img = $('vcsTreeControl_${ib.packSpecFile}');
    if (!img) {
      //there is no icon when no vcs roots configured
      return;
    }
    img.remove();
    img.style.position = 'absolute';

    var textarea = $('${ib.packSpecFile}');
    var dim = textarea.getDimensions(),
        layout = textarea.getLayout();

    var xshift = dim.width + 20; // Put next to the completion icon
    var pos = textarea.positionedOffset();

    textarea.parentNode.appendChild(img);
    var x = pos[0] + xshift + layout.get('margin-left');
    var y = pos[1] + 3 + layout.get('margin-top');
    BS.Util.show(img);
    BS.Util.place(img, x, y);
  });
</script>