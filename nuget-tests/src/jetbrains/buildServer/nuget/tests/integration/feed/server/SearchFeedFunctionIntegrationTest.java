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

package jetbrains.buildServer.nuget.tests.integration.feed.server;

import jetbrains.buildServer.nuget.server.feed.server.PackageAttributes;
import jetbrains.buildServer.nuget.server.feed.server.index.NuGetIndexEntry;
import jetbrains.buildServer.nuget.server.feed.server.javaFeed.NuGetAPIVersion;
import jetbrains.buildServer.util.CollectionsUtil;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * @author Evgeniy.Koshkin
 */
public class SearchFeedFunctionIntegrationTest extends NuGetJavaFeedIntegrationTestBase {

  @Override
  @BeforeMethod
  public void setUp() throws Exception {
    super.setUp();
    System.setProperty(NuGetAPIVersion.TEAMCITY_NUGET_API_VERSION_PROP_NAME, NuGetAPIVersion.V2);
  }

  @Override
  @AfterMethod
  public void tearDown() throws Exception {
    System.getProperties().remove(NuGetAPIVersion.TEAMCITY_NUGET_API_VERSION_PROP_NAME);
    super.tearDown();
  }

  @Test
  public void testBadRequest() throws Exception {
    addMockPackage("foo", "1.0.0.0");
    assert400("Search()").run();
    assert400("Search()?&searchTerm='foo'&includePrerelease=true").run();
    assert400("Search()?&targetFramework='net45'&includePrerelease=true").run();
    assert400("Search()?&searchTerm='foo'&targetFramework='net45'").run();
  }

  @Test
  public void testNoPackagesFound() throws Exception {
    addMockPackage("foo", "1.0.0.0");
    assert404("Search()?&searchTerm='noSuchPackage'&targetFramework='net45'&includePrerelease=true").run();
  }

  @Test
  public void testEmptySearchTerm() throws Exception {
    addMockPackage("foo", "1.0.0.0");
    final String response = openRequest("Search()?&searchTerm=''&targetFramework='net45'&includePrerelease=true");
    assertContainsPackageVersion(response, "1.0.0.0");
  }

  @Test
  public void testVSRequests() throws Exception {
    addMockPackage("foo", "1.0.0.0");
    final String[] reqs = {
            "Search()/$count?$filter=IsAbsoluteLatestVersion&searchTerm='foo'&targetFramework='net45%7Cnet45%7Cnet45%7Cnet45%7Cnet45%7Cnet45%7Cnet45'&includePrerelease=true",
            "Search()?$filter=IsAbsoluteLatestVersion&$skip=0&$top=30&searchTerm='foo'&targetFramework='net45%7Cnet45%7Cnet45%7Cnet45%7Cnet45%7Cnet45%7Cnet45'&includePrerelease=true",
            "Search()/$count?$filter=IsAbsoluteLatestVersion&searchTerm=''&targetFramework='net45%7Cnet45%7Cnet45%7Cnet45%7Cnet45%7Cnet45%7Cnet45'&includePrerelease=true",
            "Search()?$filter=IsAbsoluteLatestVersion&$orderby=DownloadCount%20desc,Id&$skip=0&$top=30&searchTerm=''&targetFramework='net45%7Cnet45%7Cnet45%7Cnet45%7Cnet45%7Cnet45%7Cnet45'&includePrerelease=true"
    };
    for (String req : reqs) {
      assert200(req).run();
    }
  }

  @Test
  public void testTargetFramework() throws Exception {
    fail("TW-38385");
  }

  @Test
  public void testIncludePreRelease() throws Exception {
    addMockPackage(new NuGetIndexEntry("pre-release", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.IS_PRERELEASE, Boolean.TRUE.toString(), PackageAttributes.VERSION, "1")));
    addMockPackage(new NuGetIndexEntry("release", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.IS_PRERELEASE, Boolean.FALSE.toString(), PackageAttributes.VERSION, "2")));
    addMockPackage(new NuGetIndexEntry("release", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.VERSION, "3")));

    final String preReleaseIncludedFeedResponse = openRequest("Search()?&searchTerm=''&targetFramework='net45'&includePrerelease=true");
    assertContainsPackageVersion(preReleaseIncludedFeedResponse, "1.0");
    assertContainsPackageVersion(preReleaseIncludedFeedResponse, "2.0");
    assertContainsPackageVersion(preReleaseIncludedFeedResponse, "3.0");

    final String preReleaseNotIncludedFeedResponse = openRequest("Search()?&searchTerm=''&targetFramework='net45'&includePrerelease=false");
    assertNotContainsPackageVersion(preReleaseNotIncludedFeedResponse, "1.0");
    assertContainsPackageVersion(preReleaseNotIncludedFeedResponse, "2.0");
    assertContainsPackageVersion(preReleaseNotIncludedFeedResponse, "3.0");
  }

  @Test
  public void testFilterIsAbsoluteLatestVersion() throws Exception {
    addMockPackage(new NuGetIndexEntry("pre-release-old", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.IS_PRERELEASE, Boolean.TRUE.toString(), PackageAttributes.VERSION, "1")));
    addMockPackage(new NuGetIndexEntry("release-old", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.IS_PRERELEASE, Boolean.FALSE.toString(), PackageAttributes.VERSION, "2")));
    addMockPackage(new NuGetIndexEntry("release-latest", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.VERSION, "3")));
    addMockPackage(new NuGetIndexEntry("pre-release-latest", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.IS_PRERELEASE, Boolean.TRUE.toString(), PackageAttributes.VERSION, "4")));
    addMockPackage(new NuGetIndexEntry("pre-release-old", CollectionsUtil.asMap(PackageAttributes.ID, "boo", PackageAttributes.IS_PRERELEASE, Boolean.TRUE.toString(), PackageAttributes.VERSION, "5")));
    addMockPackage(new NuGetIndexEntry("pre-release-latest", CollectionsUtil.asMap(PackageAttributes.ID, "boo", PackageAttributes.IS_PRERELEASE, Boolean.TRUE.toString(), PackageAttributes.VERSION, "6")));
    addMockPackage(new NuGetIndexEntry("release-old", CollectionsUtil.asMap(PackageAttributes.ID, "boo", PackageAttributes.IS_PRERELEASE, Boolean.FALSE.toString(), PackageAttributes.VERSION, "7")));
    addMockPackage(new NuGetIndexEntry("release-latest", CollectionsUtil.asMap(PackageAttributes.ID, "boo", PackageAttributes.VERSION, "8")));

    final String includedPrereleaseResponse = openRequest("Search()?$filter=IsAbsoluteLatestVersion&searchTerm=''&targetFramework='net45'&includePrerelease=true");

    assertContainsPackageVersion(includedPrereleaseResponse, "4.0");
    assertContainsPackageVersion(includedPrereleaseResponse, "8.0");
    assertNotContainsPackageVersion(includedPrereleaseResponse, "1.0");
    assertNotContainsPackageVersion(includedPrereleaseResponse, "2.0");
    assertNotContainsPackageVersion(includedPrereleaseResponse, "3.0");
    assertNotContainsPackageVersion(includedPrereleaseResponse, "5.0");
    assertNotContainsPackageVersion(includedPrereleaseResponse, "6.0");
    assertNotContainsPackageVersion(includedPrereleaseResponse, "7.0");

    final String stableOnlyResponse = openRequest("Search()?$filter=IsAbsoluteLatestVersion&searchTerm=''&targetFramework='net45'&includePrerelease=false");

    assertContainsPackageVersion(stableOnlyResponse, "8.0");
    assertNotContainsPackageVersion(stableOnlyResponse, "1.0");
    assertNotContainsPackageVersion(stableOnlyResponse, "2.0");
    assertNotContainsPackageVersion(stableOnlyResponse, "3.0");
    assertNotContainsPackageVersion(stableOnlyResponse, "4.0");
    assertNotContainsPackageVersion(stableOnlyResponse, "5.0");
    assertNotContainsPackageVersion(stableOnlyResponse, "6.0");
    assertNotContainsPackageVersion(stableOnlyResponse, "7.0");
  }

  @Test
  public void testSkip() throws Exception {
    addMockPackage(new NuGetIndexEntry("pre-release", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.IS_PRERELEASE, Boolean.TRUE.toString(), PackageAttributes.VERSION, "1")));
    addMockPackage(new NuGetIndexEntry("release", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.IS_PRERELEASE, Boolean.FALSE.toString(), PackageAttributes.VERSION, "2")));
    addMockPackage(new NuGetIndexEntry("release", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.VERSION, "3")));

    final String response = openRequest("Search()?$skip=1&searchTerm=''&targetFramework='net45'&includePrerelease=true");
    assertContainsPackageVersion(response, "1.0");
    assertContainsPackageVersion(response, "2.0");
    assertNotContainsPackageVersion(response, "3.0");

    final String skipZeroResponse = openRequest("Search()?$skip=0&searchTerm=''&targetFramework='net45'&includePrerelease=true");
    assertContainsPackageVersion(skipZeroResponse, "1.0");
    assertContainsPackageVersion(skipZeroResponse, "2.0");
    assertContainsPackageVersion(skipZeroResponse, "3.0");
  }

  @Test
  public void testTop() throws Exception {
    addMockPackage(new NuGetIndexEntry("pre-release", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.IS_PRERELEASE, Boolean.TRUE.toString(), PackageAttributes.VERSION, "1")));
    addMockPackage(new NuGetIndexEntry("release", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.IS_PRERELEASE, Boolean.FALSE.toString(), PackageAttributes.VERSION, "2")));
    addMockPackage(new NuGetIndexEntry("release", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.VERSION, "3")));

    final String response = openRequest("Search()?$top=2&searchTerm=''&targetFramework='net45'&includePrerelease=true");
    assertContainsPackageVersion(response, "3.0");
    assertContainsPackageVersion(response, "2.0");
    assertNotContainsPackageVersion(response, "1.0");

    final String topZeroResponse = openRequest("Search()?$top=0&searchTerm=''&targetFramework='net45'&includePrerelease=true");
    assertNotContainsPackageVersion(topZeroResponse, "1.0");
    assertNotContainsPackageVersion(topZeroResponse, "2.0");
    assertNotContainsPackageVersion(topZeroResponse, "3.0");
  }

  @Test
  public void testOrderBy() throws Exception {
    fail();
  }

  @Test
  public void testCountRequest() throws Exception {
    addMockPackage(new NuGetIndexEntry("id-matches", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.VERSION, "1")));
    addMockPackage(new NuGetIndexEntry("id-not-matches", CollectionsUtil.asMap(PackageAttributes.ID, "boo", PackageAttributes.VERSION, "2")));
    addMockPackage(new NuGetIndexEntry("description-matches", CollectionsUtil.asMap(PackageAttributes.DESCRIPTION, "foo", PackageAttributes.VERSION, "3")));
    addMockPackage(new NuGetIndexEntry("description-not-matches", CollectionsUtil.asMap(PackageAttributes.DESCRIPTION, "boo", PackageAttributes.VERSION, "4")));
    addMockPackage(new NuGetIndexEntry("tags-matches", CollectionsUtil.asMap(PackageAttributes.TAGS, "foo", PackageAttributes.VERSION, "5")));
    addMockPackage(new NuGetIndexEntry("tags-not-matches", CollectionsUtil.asMap(PackageAttributes.TAGS, "boo", PackageAttributes.VERSION, "6")));
    addMockPackage(new NuGetIndexEntry("authors-matches", CollectionsUtil.asMap(PackageAttributes.AUTHORS, "foo", PackageAttributes.VERSION, "7")));
    addMockPackage(new NuGetIndexEntry("authors-not-matches", CollectionsUtil.asMap(PackageAttributes.AUTHORS, "boo", PackageAttributes.VERSION, "8")));

    assertEquals("4", openRequest("Search()/$count?&searchTerm='foo'&targetFramework='net45'&includePrerelease=true"));
    assertEquals("2", openRequest("Search()/$count?$filter=IsAbsoluteLatestVersion&searchTerm='foo'&targetFramework='net45'&includePrerelease=true"));
  }

  @Test
  public void testAllAttributesAreProcessed() throws Exception {
    addMockPackage(new NuGetIndexEntry("id-matches", CollectionsUtil.asMap(PackageAttributes.ID, "foo", PackageAttributes.VERSION, "1")));
    addMockPackage(new NuGetIndexEntry("id-not-matches", CollectionsUtil.asMap(PackageAttributes.ID, "boo", PackageAttributes.VERSION, "2")));
    addMockPackage(new NuGetIndexEntry("description-matches", CollectionsUtil.asMap(PackageAttributes.DESCRIPTION, "foo", PackageAttributes.VERSION, "3")));
    addMockPackage(new NuGetIndexEntry("description-not-matches", CollectionsUtil.asMap(PackageAttributes.DESCRIPTION, "boo", PackageAttributes.VERSION, "4")));
    addMockPackage(new NuGetIndexEntry("tags-matches", CollectionsUtil.asMap(PackageAttributes.TAGS, "foo", PackageAttributes.VERSION, "5")));
    addMockPackage(new NuGetIndexEntry("tags-not-matches", CollectionsUtil.asMap(PackageAttributes.TAGS, "boo", PackageAttributes.VERSION, "6")));
    addMockPackage(new NuGetIndexEntry("authors-matches", CollectionsUtil.asMap(PackageAttributes.AUTHORS, "foo", PackageAttributes.VERSION, "7")));
    addMockPackage(new NuGetIndexEntry("authors-not-matches", CollectionsUtil.asMap(PackageAttributes.AUTHORS, "boo", PackageAttributes.VERSION, "8")));

    final String responseBody = openRequest("Search()?&searchTerm='foo'&targetFramework='net45'&includePrerelease=true");

    assertContainsPackageVersion(responseBody, "1.0");
    assertNotContainsPackageVersion(responseBody, "2.0");
    assertContainsPackageVersion(responseBody, "3.0");
    assertNotContainsPackageVersion(responseBody, "4.0");
    assertContainsPackageVersion(responseBody, "5.0");
    assertNotContainsPackageVersion(responseBody, "6.0");
    assertContainsPackageVersion(responseBody, "7.0");
    assertNotContainsPackageVersion(responseBody, "8.0");
  }

  private void assertContainsPackageVersion(String responseBody, String version){
    assertContains(responseBody, "<d:Version>" + version + "</d:Version>");
  }

  private void assertNotContainsPackageVersion(String responseBody, String version){
    assertNotContains(responseBody, "<d:Version>" + version + "</d:Version>", false);
  }
}