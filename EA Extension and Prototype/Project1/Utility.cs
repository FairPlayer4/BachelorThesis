using System;
using System.Globalization;
using System.IO;
using System.Net;
using System.Linq;
using System.Net.Sockets;
using System.Text;
using System.Xml;
using System.Text.RegularExpressions;

namespace EA_Neo4j_SafetyAnalysis_AddIn
{
    internal static class Utility
    {
        private static bool developerMode = false;

        internal static bool DeveloperMode
        {
            get { return developerMode; }
            set
            {
                developerMode = value;
                if (Main.connection != null)
                {
                    Main.connection.setDeveloperMode(developerMode);
                    Main.showSocketLogWindow(value);
                }
            }
        }

        private static string location = Directory.GetCurrentDirectory() + @"\";

        internal static string Location
        {
            get { return location; }
            private set { location = value; }
        }

        private static bool ready = false;

        internal static bool Ready
        {
            get { return ready; }
            private set { ready = value; }
        }

        internal static bool setGlobalVars()
        {
            string regsettings = "SOFTWARE\\Sparx Systems\\EAAddins\\EA_Neo4j_SafetyAnalysis_AddIn\\Settings\\";
            using (Microsoft.Win32.RegistryKey rk = Microsoft.Win32.Registry.CurrentUser.OpenSubKey(regsettings))
            {
                if (rk != null)
                {
                    object locationvalue = rk.GetValue("Path");
                    if (locationvalue != null)
                    {
                        Location = locationvalue.ToString();
                    }
                    else
                    {
                        ErrorWindow error = new ErrorWindow("Error: Registry Entry Path not found!", "RegistryError");
                        error.ShowDialog();
                        return false;
                    }
                    Ready = true;
                    return true;
                }
                else
                {
                    ErrorWindow error = new ErrorWindow("Error: No Registry Entries found!", "RegistryError");
                    error.ShowDialog();
                    return false;
                }
            }
        }

        internal static void Logger(string lines)
        {
            if (Ready)
            {
                Console.WriteLine(lines);
                if (!Location.Equals(""))
                {
                    System.IO.StreamWriter file2 = new System.IO.StreamWriter(Location + "Logger.txt", true);
                    file2.WriteLine(getCurrentDateTime() + " - " + lines);
                    file2.Close();
                }
            }
        }

        internal static void ErrorLogger(Exception e)
        {
            if (Ready)
            {
                Console.WriteLine(e.Message);
                Console.WriteLine(e.StackTrace);
                if (!Location.Equals(""))
                {
                    System.IO.StreamWriter file2 = new System.IO.StreamWriter(Location + "ErrorLogger.txt", true);
                    file2.WriteLine(getCurrentDateTime() + " - " + e.Message + "\n" + "\t" + e.StackTrace.Replace("\n", "\n\t"));
                    file2.Close();
                }
            }
        }

        internal static string getJavaInstallationPath()
        {
            if (Ready)
            {
                if (Environment.Is64BitOperatingSystem)
                {
                    string javaKey = "SOFTWARE\\WOW6432Node\\JavaSoft\\Java Runtime Environment\\";
                    using (Microsoft.Win32.RegistryKey rk = Microsoft.Win32.Registry.LocalMachine.OpenSubKey(javaKey))
                    {
                        string currentVersion = rk.GetValue("CurrentVersion").ToString();
                        if (currentVersion != null)
                        {
                            using (Microsoft.Win32.RegistryKey key = rk.OpenSubKey(currentVersion))
                            {
                                return key.GetValue("JavaHome").ToString();
                            }
                        }
                    }
                }
                else
                {
                    string javaKey = "SOFTWARE\\JavaSoft\\Java Runtime Environment\\";
                    using (Microsoft.Win32.RegistryKey rk = Microsoft.Win32.Registry.LocalMachine.OpenSubKey(javaKey))
                    {
                        string currentVersion = rk.GetValue("CurrentVersion").ToString();
                        if (currentVersion != null)
                        {
                            using (Microsoft.Win32.RegistryKey key = rk.OpenSubKey(currentVersion))
                            {
                                return key.GetValue("JavaHome").ToString();
                            }
                        }
                    }
                }
            }
            else
            {
                ErrorWindow error = new ErrorWindow("Error: Fatal Error! Addin is not initialized correctly!", "UtilityError");
                error.ShowDialog();
                return null;
            }
            ErrorWindow error2 = new ErrorWindow("Error: No Java Installation Path found!", "JavaPathError");
            error2.ShowDialog();
            return null;
        }

        internal static void updateSettings(Settings settings)
        {
            if (Ready)
            {
                XmlDocument doc = new XmlDocument();
                doc.PreserveWhitespace = true;
                try { doc.Load(Location + "Settings.xml"); }
                catch (System.IO.FileNotFoundException)
                {
                    // Error
                    ErrorWindow error = new ErrorWindow("Error: Settings can't be found!", "SettingsError");
                    error.ShowDialog();
                    //Continue
                }
                XmlNode root = doc.DocumentElement;
                XmlNode setting = null;
                if (root.HasChildNodes)
                {
                    for (int i = 0; i < root.ChildNodes.Count; i++)
                    {
                        if (root.ChildNodes[i].Name.Equals("setting"))
                        {
                            if (root.ChildNodes[i].Attributes[0].Value.Equals(settings.ProjectGUID))
                            {
                                setting = root.ChildNodes[i];
                            }
                        }

                    }
                }
                if (setting == null)
                {
                    // Error
                    ErrorWindow error = new ErrorWindow("Error: Settings are invalid!", "SettingsError");
                    error.ShowDialog();
                    //Continue
                }
                else
                {
                    foreach (XmlNode xn in setting.ChildNodes)
                    {
                        switch (xn.Name)
                        {
                            case "neo4jdbfolderpath":
                                xn.InnerText = settings.Neo4jPath;
                                break;
                            case "lastupdate":
                                xn.InnerText = settings.LastUpdate;
                                break;
                            case "updateconstantly":
                                xn.InnerText = settings.ContinuousUpdate.ToString();
                                break;
                            case "storeandreuseanalysisresults":
                                xn.InnerText = settings.ContinuousAnalysis.ToString();
                                break;
                        }
                    }
                    if (!Directory.Exists(settings.Neo4jPath))
                    {
                        Directory.CreateDirectory(settings.Neo4jPath);
                    }
                    doc.Save(Location + "Settings.xml");
                }
            }
            else
            {
                ErrorWindow error = new ErrorWindow("Error: Fatal Error! Addin is not initialized correctly!", "UtilityError");
                error.ShowDialog();
            }
        }

        internal static Settings getSettings(string projectguid)
        {
            if (Ready)
            {
                XmlDocument doc = new XmlDocument();
                doc.PreserveWhitespace = true;
                try { doc.Load(Location + "Settings.xml"); }
                catch (System.IO.FileNotFoundException)
                {
                    doc.LoadXml("<?xml version=\"1.0\" encoding=\"utf-8\"?><settings></settings>");
                    doc.Save(Location + "Settings.xml");
                }
                XmlNode root = doc.DocumentElement;
                XmlNode setting = null;
                if (root.HasChildNodes)
                {
                    for (int i = 0; i < root.ChildNodes.Count; i++)
                    {
                        if (root.ChildNodes[i].Name.Equals("setting"))
                        {
                            if (root.ChildNodes[i].Attributes[0].Value.Equals(projectguid))
                            {
                                setting = root.ChildNodes[i];
                            }
                        }
                    }
                }
                if (setting == null)
                {
                    XmlElement res = doc.CreateElement("setting");
                    res.SetAttribute("name", projectguid);
                    XmlNode res2 = root.AppendChild(res);
                    XmlElement res3 = doc.CreateElement("neo4jdbfolderpath");
                    res3.InnerText = Location + projectguid;
                    res2.AppendChild(res3);
                    XmlElement res4 = doc.CreateElement("lastupdate");
                    res4.InnerText = "never";
                    res2.AppendChild(res4);
                    XmlElement res5 = doc.CreateElement("updateconstantly");
                    res5.InnerText = "True";
                    res2.AppendChild(res5);
                    XmlElement res6 = doc.CreateElement("storeandreuseanalysisresults");
                    res6.InnerText = "False";
                    res2.AppendChild(res6);
                    string nicexml = formatXML(doc);
                    doc.LoadXml(nicexml);
                    doc.Save(Location + "Settings.xml");
                    if (!Directory.Exists(Location + projectguid))
                    {
                        Directory.CreateDirectory(Location + projectguid);
                    }
                    return new Settings(projectguid, Location + projectguid, "never", true, false);
                }
                else
                {
                    string neo4jpath = "";
                    string lastupdate = "";
                    bool contupdate = true;
                    bool contanalysis = false;
                    foreach (XmlNode xn in setting.ChildNodes)
                    {
                        switch (xn.Name)
                        {
                            case "neo4jdbfolderpath":
                                neo4jpath = xn.InnerText;
                                break;
                            case "lastupdate":
                                lastupdate = xn.InnerText;
                                break;
                            case "updateconstantly":
                                contupdate = Boolean.Parse(xn.InnerText);
                                break;
                            case "storeandreuseanalysisresults":
                                contanalysis = Boolean.Parse(xn.InnerText);
                                break;
                        }
                    }
                    if (!neo4jpath.Equals("") && !lastupdate.Equals(""))
                    {
                        Settings settings = new Settings(projectguid, neo4jpath, lastupdate, contupdate, contanalysis);
                        if (!Directory.Exists(neo4jpath))
                        {
                            Directory.CreateDirectory(neo4jpath);
                        }
                        return settings;
                    }
                    else
                    {
                        doc.LoadXml("<?xml version=\"1.0\" encoding=\"utf-8\"?><settings></settings>");
                        doc.Save(Location + "Settings.xml");
                        XmlElement res = doc.CreateElement("setting");
                        res.SetAttribute("name", projectguid);
                        XmlNode res2 = root.AppendChild(res);
                        XmlElement res3 = doc.CreateElement("neo4jdbfolderpath");
                        res3.InnerText = Location + projectguid;
                        res2.AppendChild(res3);
                        XmlElement res4 = doc.CreateElement("lastupdate");
                        res4.InnerText = "never";
                        res2.AppendChild(res4);
                        XmlElement res5 = doc.CreateElement("updateconstantly");
                        res5.InnerText = "True";
                        res2.AppendChild(res5);
                        XmlElement res6 = doc.CreateElement("storeandreuseanalysisresults");
                        res6.InnerText = "False";
                        res2.AppendChild(res6);
                        string nicexml = formatXML(doc);
                        doc.LoadXml(nicexml);
                        doc.Save(Location + "Settings.xml");
                        return new Settings(projectguid, Location + projectguid, "never", true, false);
                    }
                }
            }
            ErrorWindow error = new ErrorWindow("Error: Fatal Error! Addin is not initialized correctly!", "UtilityError");
            error.ShowDialog();
            return new Settings(projectguid, "Error", "never", false, false);
        }

        internal static string formatXML(XmlDocument doc)
        {
            StringBuilder sb = new StringBuilder();
            XmlWriterSettings settings = new XmlWriterSettings
            {
                Indent = true,
                IndentChars = "  ",
                NewLineChars = "\r\n",
                NewLineHandling = NewLineHandling.Replace
            };
            using (XmlWriter writer = XmlWriter.Create(sb, settings))
            {
                doc.Save(writer);
            }
            return sb.ToString();
        }

        internal static string getCurrentDateTime()
        {
            DateTime localDate = DateTime.Now;
            var culture = new CultureInfo("de-DE");
            return localDate.ToString(culture);
        }

        internal static int getAvailableTcpPort()
        {
            TcpListener l = new TcpListener(IPAddress.Loopback, 0);
            l.Start();
            int port = ((IPEndPoint)l.LocalEndpoint).Port;
            l.Stop();
            return port;
        }
    }
}
