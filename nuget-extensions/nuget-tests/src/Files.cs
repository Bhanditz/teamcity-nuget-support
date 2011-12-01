using System;
using System.IO;
using System.Net;
using NUnit.Framework;

namespace JetBrains.TeamCity.NuGet.Tests
{
  public static class Files
  {
    private static readonly Lazy<string> ourCachedNuGetExe_1_4 = PathSearcher.SearchFile("nuget.exe", "lib/nuget/1.4/nuget.exe");
    private static readonly Lazy<string> ourCachedNuGetExe_1_5 = PathSearcher.SearchFile("nuget.exe", "lib/nuget/1.5/nuget.exe");
    private static readonly Lazy<string> ourCachedNuGetRunnerPath = PathSearcher.SearchFile("JetBrains.TeamCity.NuGetRunner.exe", "bin/JetBrains.TeamCity.NuGetRunner.exe");
    private static readonly Lazy<string> ourLocalFeed = PathSearcher.SearchDirectory("nuget-tests/testData/localFeed");
    private static readonly Lazy<string> ourCachedNuGet_CI_Last = new Lazy<string>(() => FetchLatestNuGetPackage("bt4")); 
    private static readonly Lazy<string> ourCachedNuGet_CommandLinePackage_Last = new Lazy<string>(FetchLatestNuGetCommandline); 
    
    public static string NuGetExe_1_4 { get { return ourCachedNuGetExe_1_4.Value; } }
    public static string NuGetExe_1_5 { get { return ourCachedNuGetExe_1_5.Value; } }
    public static string NuGetRunnerExe { get { return ourCachedNuGetRunnerPath.Value; } }
    public static string LocalFeed { get { return ourLocalFeed.Value; } }

    public static string GetNuGetExe(NuGetVersion version)
    {
      switch (version)
      {
        case NuGetVersion.NuGet_1_4:
          return NuGetExe_1_4;
        case NuGetVersion.NuGet_1_5:
          return NuGetExe_1_5;
        case NuGetVersion.NuGet_Latest_CI:
          return ourCachedNuGet_CI_Last.Value;
        case NuGetVersion.NuGet_CommandLine_Package_Latest:
          return ourCachedNuGet_CommandLinePackage_Last.Value;
        default:
          throw new Exception("Unsupported nuget version: " + version);
      }
    }

    private static string FetchLatestNuGetPackage(string bt)
    {
      var homePath = CreateTempPath();
      //TODO: better is to download entire nuget.commandline package
      string url = "http://ci.nuget.org:8080/guestAuth/repository/download/" + bt + "/.lastSuccessful/Console/NuGet.exe";
      var nugetPath = Path.Combine(homePath, "NuGet.exe");
      var cli = new WebClient();
      cli.DownloadFile(url, nugetPath);
      return nugetPath;
    }

    private static string CreateTempPath()
    {
      var homePath = Path.GetTempFileName();
      File.Delete(homePath);
      Directory.CreateDirectory(homePath);
      return homePath;
    }

    private static string FetchLatestNuGetCommandline()
    {
      var temp = CreateTempPath();
      ProcessExecutor.ExecuteProcess(Files.NuGetExe_1_5, "install", "NuGet.commandline", "-ExcludeVersion", "-OutputDirectory", temp).Dump().AssertNoErrorOutput().AssertExitedSuccessfully();
      string nugetPath = Path.Combine(temp, "NuGet.CommandLine/tools/NuGet.Exe");
      Assert.IsTrue(File.Exists(nugetPath));
      return nugetPath;
    }
  }

  public enum NuGetVersion
  {
    NuGet_1_4,
    NuGet_1_5,

    NuGet_Latest_CI,
    NuGet_CommandLine_Package_Latest
  }
}