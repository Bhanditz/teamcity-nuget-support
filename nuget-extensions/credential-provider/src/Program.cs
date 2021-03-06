﻿using System;
using System.IO;
using JetBrains.TeamCity.NuGet.ExtendedCommands;
using JetBrains.TeamCity.NuGet.ExtendedCommands.Data;
using PowerArgs;

namespace JetBrains.TeamCity.NuGet.CredentialProvider
{
  public class Program
  {
    private static readonly string TEAMCITY_NUGET_FEEDS = "TEAMCITY_NUGET_FEEDS";
    
    public static int Main(string[] args)
    {
      try
      {
        var request = Args.Parse<CredentialProviderArgs>(args);
        if (request == null)
        {
          Console.Error.WriteLine("Invalid request arguments");
          return (int) ExitCode.UnknownError;
        }

        var response = new CredentialProviderResponse();
        ExitCode exitCode;

        var path = Environment.GetEnvironmentVariable(TEAMCITY_NUGET_FEEDS);
        if (string.IsNullOrWhiteSpace(path))
        {
          response.Message = "Failed to load NuGet feed credentials file on path " + path;
          Console.Error.WriteLine("Failed to find {0} var in environment", TEAMCITY_NUGET_FEEDS);
          exitCode = ExitCode.ProviderNotApplicable;
        }
        else if (!File.Exists(path))
        {
          response.Message = "Failed to load NuGet feed credentials file on path " + path;
          exitCode = ExitCode.Failure;
        }
        else
        {
          INuGetSources nuGetSources = XmlSerializerHelper.Load<NuGetSources>(path);
          
          var targetSource = nuGetSources.FindSource(request.Uri);
          if (targetSource == null)
          {
            response.Message = "NuGet feed credentials file contains no credentials for URL " + request.Uri;
            exitCode = ExitCode.ProviderNotApplicable;
          }
          else
          {
            response.Username = targetSource.Username;
            response.Password = targetSource.Password;
            response.Message = "Success";
            exitCode = ExitCode.Success;
          }
        }

        Console.Out.WriteLine(response.ToString());
        return (int) exitCode;
      }
      catch (Exception ex)
      {
        Console.Error.WriteLine(ex.Message);
        return (int) ExitCode.UnknownError;
      }
    }
  }
}
