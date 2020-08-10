using System;
using System.Collections.Concurrent;
using System.Collections.Generic;
using System.Diagnostics;
using System.IO;
using System.Linq;
using System.Net;
using System.Net.Sockets;
using System.Text;
using System.Threading;
using EA;
using System.Windows.Forms;

namespace EA_Neo4j_SafetyAnalysis_AddIn
{
    internal class SocketConnection
    {
        private TcpClient clientSocket;

        private NetworkStream ns;

        private bool received = false;

        private string currentneo4jpath;

        private Thread runningSocketConnection = new Thread(t => { });

        private SetQueue nextNeo4jUpdate = new SetQueue();

        private static readonly HashSet<string> fmstereotypeset = new HashSet<string>() { "FT", "FTInstance", "CFT", "CFTInstance", "IESELogicalComponent", "IESELogicalComponentInstance", "IESELogicalInport", "IESELogicalInportInstance", "IESELogicalOutport", "IESELogicalOutportInstance", "FTAND", "FTOR", "FTM/N", "FTXOR", "FTBasicEvent", "FTNOT", "InputFailureMode", "OutputFailureMode" };
        private static readonly HashSet<string> fmtaggedvaluestereotypes = new HashSet<string>() { "FTM/N", "FTBasicEvent", "InputFailureMode" };
        private static readonly HashSet<string> fmconnectorstereotypeset = new HashSet<string>() { "ComponentFailureModelTrace", "FailurePropagation", "PortFailureModeTrace", "Logical Information Flow" };
        private static readonly string mainSeparator = ",M,";
        private static readonly string singleSeparator = ",S,";
        private static readonly string partSeparator = ",P,";
        private static readonly string tgSeparator = ",T,";
        private static readonly int maxStringLength = 4096;

        private readonly object masterLock = new object();

        internal SocketConnection(bool fullupdate)
        {
            currentneo4jpath = Main.settings.Neo4jPath;
            new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            startConnection(fullupdate);
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
        }

        private void startJava(IPAddress ip, int port)
        {
            string javapath = Utility.getJavaInstallationPath();
            if (javapath != null)
            {
                Process cmd = new Process();
                cmd.StartInfo.FileName = "cmd.exe";
                cmd.StartInfo.RedirectStandardInput = true;
                cmd.StartInfo.RedirectStandardOutput = true;
                cmd.StartInfo.CreateNoWindow = true;
                cmd.StartInfo.UseShellExecute = false;
                cmd.Start();
                cmd.StandardInput.WriteLine("cd " + javapath + @"\bin\");
                Utility.Logger(@"java -jar " + @"""" + Utility.Location + "SocketNeo4jPrototype.jar" + @"""" + " " + ip.ToString() + " " + port.ToString() + " " + @"""" + Utility.Location.Replace(@"\", "/") + @"""");
                cmd.StandardInput.WriteLine(@"java -jar " + @"""" + Utility.Location + "SocketNeo4jPrototype.jar" + @"""" + " " + ip.ToString() + " " + port.ToString() + " " + @"""" + Utility.Location.Replace(@"\", "/") + @"""");
                cmd.StandardInput.Flush();
                cmd.StandardInput.Close();
            }
        }

        private void startConnection(bool fullupdate)
        {
            IPHostEntry hostEntry;
            hostEntry = Dns.GetHostEntry(Dns.GetHostName());
            if (hostEntry.AddressList.Length > 0)
            {
                var ip = hostEntry.AddressList[0];
                int port = Utility.getAvailableTcpPort();
                TcpListener server = new TcpListener(ip, port);
                server.Start();
                startJava(ip, port);
                Stopwatch timer = new Stopwatch();
                timer.Start();
                while (timer.ElapsedMilliseconds < 10000 && clientSocket == null)
                {
                    if (server.Pending())
                    {
                        clientSocket = server.AcceptTcpClient();
                        ns = clientSocket.GetStream();
                    }
                    else
                    {
                        Thread.Sleep(10);
                    }
                }
                timer.Reset();
                if (clientSocket == null)
                {
                    ErrorWindow error = new ErrorWindow("Error: Connection timed out!", "ConnectionError");
                    error.ShowDialog();
                }
                else
                {
                    string msg = startCommand("start neo4j path") + Main.settings.Neo4jPath.Replace(@"\", "/");
                    sendData(msg);
                    waitForACK();
                    if (fullupdate)
                    {
                        startFullUpdate();
                    }
                }
            }
            else
            {
                ErrorWindow error = new ErrorWindow("Error: No IP Address found!", "ConnectionError");
                error.ShowDialog();
            }
        }

        internal void setDeveloperMode(bool value)
        {  
            new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            sendData(startCommand("set developer mode") + value.ToString());
                            waitForACK();
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
        }

        internal void changeNeo4jPath()
        {
            new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            update();
                            string msg = startCommand("change neo4j path");
                            sendData(msg);
                            waitForACK();
                            new Microsoft.VisualBasic.Devices.Computer().FileSystem.CopyDirectory(currentneo4jpath, Main.settings.Neo4jPath);
                            string msg2 = startCommand("start neo4j path") + Main.settings.Neo4jPath.Replace(@"\", "/");
                            sendData(msg2);
                            waitForACK();
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
        }

        private void startReader()
        {
            try
            {
                if (ns.DataAvailable)
                {
                    byte[] buff = new byte[128];
                    ns.Read(buff, 0, 128);
                    String responseString = System.Text.Encoding.UTF8.GetString(buff);
                    if (responseString.Contains("received"))
                    {
                        received = true;
                    }
                }
            }
            catch (IOException e)
            {
                ErrorWindow error = new ErrorWindow("Error: Connection Lost (IOException)!", "ConnectionError", e);
                error.ShowDialog();
            }
            catch (SocketException se)
            {
                ErrorWindow error = new ErrorWindow("Error: Connection Lost (SocketException)!", "ConnectionError", se);
                error.ShowDialog();
            }
        }

        private void waitForACK()
        {
            while (!received)
            {
                startReader();
                Thread.Sleep(10);
            }
            received = false;
        }

        private void sendData(string data)
        {
            try
            {
                byte[] senddata = System.Text.Encoding.UTF8.GetBytes(data);
                ns.Write(senddata, 0, senddata.Length);
            }
            catch (IOException e)
            {
                ErrorWindow error = new ErrorWindow("Error: Connection Lost (IOException)!", "ConnectionError", e);
                error.ShowDialog();
            }
            catch (SocketException se)
            {
                ErrorWindow error = new ErrorWindow("Error: Connection Lost (SocketException)!", "ConnectionError", se);
                error.ShowDialog();
            }
        }

        internal void closeJava()
        {
            new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            update();
                            sendData(startCommand("exit"));
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
        }

        internal void performFullAnalysis()
        {
            new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            sendData(startCommand("analyze and store results"));
                            waitForACK();
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
        }

        internal void openAnalysisWindow()
        {
            new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            update();
                            sendData(startCommand("open analysis window"));
                            waitForACK();
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
        }

        internal void outsideUpdate()
        {
            new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            update();
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
        }

        private void update()
        {
            if (Main.settings.LastUpdate.Equals("never"))
            {
                startFullUpdate();
                nextNeo4jUpdate.clear();
            }
            while (!nextNeo4jUpdate.isEmpty())
            {
                string start = startCommand("update");
                StringBuilder elem = new StringBuilder();
                int number = 0;
                int nextlength = 0;
                while ((elem.Length + nextlength + mainSeparator.Length) < (maxStringLength - start.Length - 1) && !nextNeo4jUpdate.isEmpty())
                {
                    elem.Append(mainSeparator + nextNeo4jUpdate.dequeue());
                    if (!nextNeo4jUpdate.isEmpty())
                    {
                        nextlength = nextNeo4jUpdate.peek().Length;
                    }
                    number++;
                }
                string message = start + number + elem.ToString();
                if (message.Length < maxStringLength)
                {
                    sendData(message);
                    waitForACK();
                }
                else
                {
                    ErrorWindow error = new ErrorWindow("Error: Message is too long!", "UpdateError");
                    error.ShowDialog();
                }
            }
            Main.settings.LastUpdate = Utility.getCurrentDateTime();
            if (Main.settings.ContinuousAnalysis)
            {
                performFullAnalysis();
            }
            if (!Main.settings.ContinuousUpdate)
            {
                DialogResult result1 = MessageBox.Show("Update Complete!", "Neo4j Safety Analysis", MessageBoxButtons.OK);
            }
        }

        private void startFullUpdate()
        {
            Queue<string> connectorstoadd = new Queue<string>();
            Queue<string> elementstoadd = new Queue<string>(); ;
            Thread prepareUpdate = new Thread(
                    t =>
                    {
                        EA.Collection elementlist = Main.repository.GetElementSet("SELECT el.Object_ID FROM t_object AS el WHERE el.Stereotype IN (\"FT\", \"FTInstance\", \"CFT\", \"CFTInstance\", \"IESELogicalComponent\", \"IESELogicalComponentInstance\", \"IESELogicalInport\", \"IESELogicalInportInstance\", \"IESELogicalOutport\", \"IESELogicalOutportInstance\", \"FTAND\", \"FTOR\", \"FTM/N\", \"FTXOR\", \"FTBasicEvent\", \"FTNOT\", \"InputFailureMode\", \"OutputFailureMode\")", 2);
                        var fmelements = elementlist;
                        foreach (EA.Element fmelement in fmelements)
                        {
                            foreach (EA.Connector con in fmelement.Connectors)
                            {
                                if (fmconnectorstereotypeset.Contains(con.Stereotype) && con.ClientID.Equals(fmelement.ElementID))
                                {
                                    connectorstoadd.Enqueue(getConnectorString(con));
                                }
                            }
                            if (fmelement.ParentID != 0)
                            {
                                connectorstoadd.Enqueue(getChildConnectorString(fmelement));
                            }
                            if (fmelement.ClassifierID != 0)
                            {
                                connectorstoadd.Enqueue(getInstanceConnectorString(fmelement));
                            }
                            elementstoadd.Enqueue(getElementString(fmelement));
                        }
                    });
            prepareUpdate.Start();
            sendData(startCommand("start full update"));
            waitForACK();
            while (prepareUpdate.IsAlive)
            {
                Thread.Sleep(10);
            }
            string start = startCommand("add elements");
            while (elementstoadd.Count > 0)
            {
                StringBuilder elem = new StringBuilder();
                int nextlength = 0;
                while ((elem.Length + nextlength + mainSeparator.Length) < (maxStringLength - start.Length) && elementstoadd.Count > 0)
                {
                    elem.Append(mainSeparator + elementstoadd.Dequeue());
                    if (elementstoadd.Count > 0)
                    {
                        nextlength = elementstoadd.Peek().Length;
                    }
                }
                string message = start + elem.ToString();
                if (message.Length < maxStringLength)
                {
                    sendData(message);
                    waitForACK();
                }
                else
                {
                    ErrorWindow error = new ErrorWindow("Error: Message is too long!", "UpdateError");
                    error.ShowDialog();
                }
            }
            string startcon = startCommand("add connectors");
            while (connectorstoadd.Count > 0)
            {
                StringBuilder cons = new StringBuilder();
                int nextlength = 0;
                while ((cons.Length + nextlength + mainSeparator.Length) < (maxStringLength - startcon.Length) && connectorstoadd.Count > 0)
                {
                    cons.Append(mainSeparator + connectorstoadd.Dequeue());
                    if (connectorstoadd.Count > 0)
                    {
                        nextlength = connectorstoadd.Peek().Length;
                    }
                }
                string message = startcon + cons.ToString();
                if (message.Length < maxStringLength)
                {
                    sendData(message);
                    waitForACK();
                }
                else
                {
                    ErrorWindow error = new ErrorWindow("Error: Message is too long!", "UpdateError");
                    error.ShowDialog();
                }
            }
            sendData(startCommand("end full update"));
            waitForACK();
            Main.settings.LastUpdate = Utility.getCurrentDateTime();
            if (Main.settings.ContinuousAnalysis)
            {
                performFullAnalysis();
            }
        }

        internal void addElement(EA.Element element)
        {
            if (fmstereotypeset.Contains(element.Stereotype))
            {
                new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            nextNeo4jUpdate.enqueue(startSingleCommand("add single element") + getElementString(element));
                            foreach (EA.Connector con in element.Connectors)
                            {
                                ErrorWindow error = new ErrorWindow("Error: Connectors were added with a new Element!", "EAError");
                                error.ShowDialog();
                                if (fmconnectorstereotypeset.Contains(con.Stereotype) && con.ClientID.Equals(element.ElementID))
                                {
                                    nextNeo4jUpdate.enqueue(startSingleCommand("add single connector") + getConnectorString(con));
                                }
                            }
                            if (element.ParentID != 0)
                            {
                                nextNeo4jUpdate.enqueue(startSingleCommand("add single connector") + getChildConnectorString(element));
                            }
                            if (element.ClassifierID != 0)
                            {
                                nextNeo4jUpdate.enqueue(startSingleCommand("add single connector") + getInstanceConnectorString(element));
                            }
                            if (Main.settings.ContinuousUpdate)
                            {
                                update();
                            }
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
            }
        }

        internal void deleteElement(Element element)
        {
            if (fmstereotypeset.Contains(element.Stereotype))
            {
                new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            nextNeo4jUpdate.enqueue(startSingleCommand("delete single element") + getElementString(element));
                            if (Main.settings.ContinuousUpdate)
                            {
                                update();
                            }
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
            }
        }

        internal void updateElement(Element element)
        {
            if (fmstereotypeset.Contains(element.Stereotype))
            {
                new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            nextNeo4jUpdate.enqueue(startSingleCommand("update single element") + getElementString(element));
                            if (Main.settings.ContinuousUpdate)
                            {
                                update();
                            }
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
            }
        }

        internal void addConnector(Connector connector)
        {
            if (fmconnectorstereotypeset.Contains(connector.Stereotype))
            {
                new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            nextNeo4jUpdate.enqueue(startSingleCommand("add single connector") + getConnectorString(connector));
                            if (Main.settings.ContinuousUpdate)
                            {
                                update();
                            }
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
            }
        }

        internal void deleteConnector(Connector connector)
        {
            if (fmconnectorstereotypeset.Contains(connector.Stereotype))
            {
                new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            nextNeo4jUpdate.enqueue(startSingleCommand("delete single connector") + getConnectorString(connector));
                            if (Main.settings.ContinuousUpdate)
                            {
                                update();
                            }
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
            }
        }

        internal void updateConnector(Connector connector)
        {
            if (fmconnectorstereotypeset.Contains(connector.Stereotype))
            {
                new Thread(
                t =>
                {
                    while (runningSocketConnection.IsAlive)
                    {
                        Thread.Sleep(10);
                    }
                    lock (masterLock)
                    {
                        runningSocketConnection = new Thread(
                        rt =>
                        {
                            nextNeo4jUpdate.enqueue(startSingleCommand("update single connector") + getConnectorString(connector));
                            if (Main.settings.ContinuousUpdate)
                            {
                                update();
                            }
                        });
                        runningSocketConnection.Start();
                    }
                }).Start();
            }
        }

        private string getElementString(EA.Element element)
        {
            StringBuilder elementString = new StringBuilder();
            elementString.Append(element.ElementID + partSeparator + element.Name + partSeparator + element.Stereotype + partSeparator + element.ClassifierID);
            if (fmtaggedvaluestereotypes.Contains(element.Stereotype))
            {
                EA.TaggedValue tg = element.TaggedValues.GetByName("cValue");
                if (tg == null)
                {
                    tg = element.TaggedValues.GetByName("m");
                    if (tg != null)
                    {
                        elementString.Append(partSeparator + "MOONNumber" + tgSeparator + tg.Value);
                    }
                }
                else
                {
                    elementString.Append(partSeparator + "Basic Failure Probability" + tgSeparator + tg.Value);
                }
            }
            return elementString.ToString();
        }

        private string getConnectorString(EA.Connector connector)
        {
            return connector.ConnectorID + partSeparator + connector.Stereotype + partSeparator + connector.ClientID + partSeparator + connector.SupplierID;
        }

        private string getChildConnectorString(EA.Element element)
        {
            return element.ElementID + partSeparator + "Is_Child_Of" + partSeparator + element.ElementID + partSeparator + element.ParentID;
        }

        private string getInstanceConnectorString(EA.Element element)
        {
            return element.ElementID + partSeparator + "Is_Instance_Of" + partSeparator + element.ElementID + partSeparator + element.ClassifierID;
        }

        private string startCommand(string command)
        {
            return command + mainSeparator;
        }
        private string startSingleCommand(string command)
        {
            return command + singleSeparator;
        }
    }
}
