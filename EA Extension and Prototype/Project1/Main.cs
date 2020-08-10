using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Runtime.InteropServices;
using System.Text;
using System.Threading;
using System.Windows.Forms;

namespace EA_Neo4j_SafetyAnalysis_AddIn
{
    [ComVisible(true)]
    public class Main
    {

        internal static Settings settings { get; private set; }

        internal static EA.Repository repository { get; private set; }

        internal static SocketConnection connection { get; private set; }

        private static SocketLogWindow socketWindow;

        private bool addinActive = false;

        private EA.Element lastSelectedElement;

        private EA.Connector lastSelectedConnector;

        internal static StringBuilder consoleOutput = new StringBuilder();

        internal static TextWriter mainTextWriter = new ControlWriterMain();

        private void updateLastSelected()
        {
            if (lastSelectedElement != null)
            {
                connection.updateElement(lastSelectedElement);
                lastSelectedElement = null;
            }
            if (lastSelectedConnector != null)
            {
                connection.updateConnector(lastSelectedConnector);
                lastSelectedConnector = null;
            }
        }

        private class ControlWriterMain : TextWriter
        {
            internal ControlWriterMain()
            {
            }

            public override void Write(char value)
            {
                Main.consoleOutput.Append(value);
            }

            public override void Write(string value)
            {
                Main.consoleOutput.Append(value);
            }

            public override Encoding Encoding
            {
                get { return Encoding.ASCII; }
            }
        }

        internal static void initializeSettings()
        {
            if (repository != null)
            {
                settings = Utility.getSettings(repository.ProjectGUID);
            }
        }

        internal static void showSocketLogWindow(bool value)
        {
            if (Utility.DeveloperMode)
            {
                socketWindow = new SocketLogWindow();
                new Thread(
                    t =>
                    {
                        socketWindow.ShowDialog();
                    }).Start();
            }
            else
            {
                if (socketWindow != null)
                {
                    SocketLogWindow.StayVisible = false;
                }
            }
        }

        internal static void startAddinConnection()
        {
            if (repository != null)
            {
                Console.SetOut(mainTextWriter);
                new Thread(t =>
                {
                    while (true)
                    {
                        Stopwatch timer = new Stopwatch();
                        timer.Start();
                        while (timer.ElapsedMilliseconds < 1000)
                        {
                            Thread.Sleep(500);
                        }
                        timer.Reset();
                        if (SocketLogWindow.StayVisible && socketWindow != null)
                        {
                            socketWindow.writeToTextBox();
                        }
                    }
                }).Start();
                if (settings.LastUpdate.Equals("never"))
                {
                    DialogResult result1 = MessageBox.Show("This project is not stored in a Neo4j Database for Safety Analysis. Do you want to store this project in a Neo4j Database? (You can also store it later)", "Neo4j Safety Analysis Update", MessageBoxButtons.YesNo);
                    if (result1 == DialogResult.Yes)
                    {
                        connection = new SocketConnection(true);
                    }
                    else
                    {
                        connection = new SocketConnection(false);
                    }
                }
                else
                {
                    connection = new SocketConnection(false);
                }
            }
        }

        public String EA_Connect(EA.Repository Repository)
        {
            addinActive = Utility.setGlobalVars();
            return "a string";
        }

        public void EA_Disconnect()
        {
            if (Utility.DeveloperMode)
            {
                SocketLogWindow.StayVisible = false;
            }
            GC.Collect();
            GC.WaitForPendingFinalizers();
        }

        public void EA_FileOpen(EA.Repository Repository)
        {
            repository = Repository;
            initializeSettings();
            if (addinActive)
            {
                DialogResult result1 = MessageBox.Show("Do you want to use the Neo4j Safety Analysis Addin for this Project?", "Neo4j Safety Analysis", MessageBoxButtons.YesNo);
                addinActive = (result1 == DialogResult.Yes);
            }
            if (addinActive)
            {
                startAddinConnection();
            }
            else
            {
                settings.LastUpdate = "never";
            }
        }

        public void EA_FileClose(EA.Repository Repository)
        {
            if (connection != null)
            {
                connection.closeJava();
                connection = null;
            }
            if (Utility.DeveloperMode)
            {
                SocketLogWindow.StayVisible = false;
            }
            repository = null;
        }

        public void EA_OnContextItemChanged(EA.Repository Repository, string GUID, EA.ObjectType ot)
        {
            if (connection != null && repository != null && addinActive)
            {
                if (!settings.LastUpdate.Equals("never"))
                {
                    Repository.SaveAllDiagrams();
                    updateLastSelected();
                    if (ot.Equals(EA.ObjectType.otConnector))
                    {
                        lastSelectedConnector = Repository.GetConnectorByGuid(GUID);
                    }
                    else
                    {
                        if (ot.Equals(EA.ObjectType.otElement))
                        {
                            lastSelectedElement = Repository.GetElementByGuid(GUID);
                        }
                    }
                }
            }
        }

        public bool EA_OnPostNewElement(EA.Repository Repository, EA.EventProperties Info)
        {
            if (connection != null && repository != null && addinActive)
            {
                if (!settings.LastUpdate.Equals("never"))
                {
                    EventPropertiesHandler eventhelper = new EventPropertiesHandler(Repository, Info);
                    EA.Element element;
                    eventhelper.GetElement(out element);
                    if (element != null)
                    {
                        connection.addElement(element);
                        return true;
                    }
                    else
                    {
                        ErrorWindow error = new ErrorWindow("Error: EA New Element Event failed!", "EAError");
                        error.ShowDialog();
                        return false;
                    }
                }
            }
            return false;
        }

        public bool EA_OnPreDeleteElement(EA.Repository Repository, EA.EventProperties Info)
        {
            if (connection != null && repository != null && addinActive)
            {
                if (!settings.LastUpdate.Equals("never"))
                {
                    EventPropertiesHandler eventhelper = new EventPropertiesHandler(Repository, Info);
                    EA.Element element;
                    eventhelper.GetElement(out element);
                    if (element != null)
                    {
                        connection.deleteElement(element);
                        return true;
                    }
                    else
                    {
                        ErrorWindow error = new ErrorWindow("Error: EA Delete Element Event failed!", "EAError");
                        error.ShowDialog();
                        return false;
                    }
                }
            }
            return true;
        }

        public bool EA_OnPostNewDiagramObject(EA.Repository Repository, EA.EventProperties Info)
        {
            if (connection != null && repository != null && addinActive)
            {
                if (!settings.LastUpdate.Equals("never"))
                {
                    EventPropertiesHandler eventhelper = new EventPropertiesHandler(Repository, Info);
                    EA.Element diagramobject;
                    eventhelper.GetDiagramObject(out diagramobject);
                    if (diagramobject != null)
                    {
                        connection.addElement(diagramobject);
                        return true;
                    }
                    else
                    {
                        ErrorWindow error = new ErrorWindow("Error: EA New DiagramObject Event failed!", "EAError");
                        error.ShowDialog();
                        return false;
                    }
                }
            }
            return false;
        }

        public bool EA_OnPreDeleteDiagramObject(EA.Repository Repository, EA.EventProperties Info)
        {
            if (connection != null && repository != null && addinActive)
            {
                if (!settings.LastUpdate.Equals("never"))
                {
                    EventPropertiesHandler eventhelper = new EventPropertiesHandler(Repository, Info);
                    EA.Element diagramobject;
                    eventhelper.GetDiagramObject(out diagramobject);
                    if (diagramobject != null)
                    {
                        connection.deleteElement(diagramobject);
                        return true;
                    }
                    else
                    {
                        ErrorWindow error = new ErrorWindow("Error: EA Delete DiagramObject Event failed!", "EAError");
                        error.ShowDialog();
                        return false;
                    }
                }
            }
            return true;
        }

        public bool EA_OnPostNewConnector(EA.Repository Repository, EA.EventProperties Info)
        {
            if (connection != null && repository != null && addinActive)
            {
                if (!settings.LastUpdate.Equals("never"))
                {
                    EventPropertiesHandler eventhelper = new EventPropertiesHandler(Repository, Info);
                    EA.Connector connector;
                    eventhelper.GetConnector(out connector);
                    if (connector != null)
                    {
                        connection.addConnector(connector);
                        return true;
                    }
                    else
                    {
                        ErrorWindow error = new ErrorWindow("Error: EA New Connector Event failed!", "EAError");
                        error.ShowDialog();
                        return false;
                    }
                }
            }
            return false;
        }

        public bool EA_OnPreDeleteConnector(EA.Repository Repository, EA.EventProperties Info)
        {
            if (connection != null && repository != null && addinActive)
            {
                if (!settings.LastUpdate.Equals("never"))
                {
                    EventPropertiesHandler eventhelper = new EventPropertiesHandler(Repository, Info);
                    EA.Connector connector;
                    eventhelper.GetConnector(out connector);
                    if (connector != null)
                    {
                        connection.deleteConnector(connector);
                        return true;
                    }
                    else
                    {
                        ErrorWindow error = new ErrorWindow("Error: EA Delete Connector Event failed!", "EAError");
                        error.ShowDialog();
                        return false;
                    }
                }
            }
            return true;
        }

        //Called when user Click Add-Ins Menu item from within EA.
        //Populates the Menu with our desired selections.
        public object EA_GetMenuItems(EA.Repository Repository, string Location, string MenuName)
        {
            EA.Package aPackage = Repository.GetTreeSelectedPackage();
            if (addinActive)
            {
                switch (MenuName)
                {
                    case "":
                        return "-&Neo4J Safety Analysis";
                    case "-&Neo4J Safety Analysis":
                        string[] ar = { "&Update Neo4j Database", "&Open Analysis Window", "&Options", "&Deactivate Neo4j Safety Analysis AddIn" };
                        return ar;
                }
            }
            else
            {
                switch (MenuName)
                {
                    case "":
                        return "-&Neo4J Safety Analysis";
                    case "-&Neo4J Safety Analysis":
                        string[] ar = { "&Activate Neo4j Safety Analysis AddIn" };
                        return ar;
                }
            }
            return "";
        }

        //Sets the state of the menu depending if there is an active project or not
        public bool IsProjectOpen(EA.Repository Repository)
        {
            try
            {
                EA.Collection c = Repository.Models;
                return true;
            }
            catch
            {
                return false;
            }
        }

        //Called once Menu has been opened to see what menu items are active.
        public void EA_GetMenuState(EA.Repository Repository, string Location, string MenuName, string ItemName, ref bool IsEnabled, ref bool IsChecked)
        {
            if (!IsProjectOpen(Repository))

                // If no open project, disable all menu options
                IsEnabled = false;
        }

        //Called when user makes a selection in the menu.
        //This is your main exit point to the rest of your Add-in
        public void EA_MenuClick(EA.Repository Repository, string Location, string MenuName, string ItemName)
        {
            if (addinActive)
            {
                switch (ItemName)
                {
                    case "&Update Neo4j Database":
                            connection.outsideUpdate();
                        break;
                    case "&Open Analysis Window":
                            connection.openAnalysisWindow();
                        break;
                    case "&Options":
                        SettingsWindow settingsWindow = new SettingsWindow(connection);
                        settingsWindow.ShowDialog();
                        break;
                    case "&Deactivate Neo4j Safety Analysis AddIn":
                        addinActive = false;
                        if (connection != null)
                        {
                            connection.closeJava();
                            connection = null;
                        }
                        break;
                }
            } 
            else
            {
                switch (ItemName)
                {
                    case "&Activate Neo4j Safety Analysis AddIn":
                        if (Utility.Ready)
                        {
                            addinActive = true;
                            startAddinConnection();
                        }
                        break;
                }
            }
        }
    }
}




