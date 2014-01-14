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

/****
****
**** THIS CODE IS GENERATED BY jetbrains.buildServer.nuget.tests.server.entity.EntityGenerator$EntityInterfaceGenerator
**** DO NOT CHANGE!
**** Generated with class jetbrains.buildServer.nuget.tests.server.entity.EntityGenerator
**** 
*****/
package jetbrains.buildServer.nuget.server.feed.server.javaFeed.entity;

import org.jetbrains.annotations.NotNull;

public interface PackageEntityV4 {

  @NotNull
  String getId();

  @NotNull
  String getVersion();

  @NotNull
  String getNormalizedVersion();

  @NotNull
  String getAuthors();

  @NotNull
  String getCopyright();

  @NotNull
  org.joda.time.LocalDateTime getCreated();

  @NotNull
  String getDependencies();

  @NotNull
  String getDescription();

  @NotNull
  Integer getDownloadCount();

  @NotNull
  String getGalleryDetailsUrl();

  @NotNull
  String getIconUrl();

  @NotNull
  Boolean getIsLatestVersion();

  @NotNull
  Boolean getIsAbsoluteLatestVersion();

  @NotNull
  Boolean getIsPrerelease();

  @NotNull
  String getLanguage();

  @NotNull
  org.joda.time.LocalDateTime getLastUpdated();

  @NotNull
  org.joda.time.LocalDateTime getPublished();

  @NotNull
  String getPackageHash();

  @NotNull
  String getPackageHashAlgorithm();

  @NotNull
  Long getPackageSize();

  @NotNull
  String getProjectUrl();

  @NotNull
  String getReportAbuseUrl();

  @NotNull
  String getReleaseNotes();

  @NotNull
  Boolean getRequireLicenseAcceptance();

  @NotNull
  String getSummary();

  @NotNull
  String getTags();

  @NotNull
  String getTitle();

  @NotNull
  Integer getVersionDownloadCount();

  @NotNull
  String getMinClientVersion();

  @NotNull
  org.joda.time.LocalDateTime getLastEdited();

  @NotNull
  String getLicenseUrl();

  @NotNull
  String getLicenseNames();

  @NotNull
  String getLicenseReportUrl();


  String[] KeyPropertyNames = new String[] {
    "Id", 
    "Version", 
  };


}

