using System;
using System.Collections.Generic;
using System.ComponentModel;
using System.Data;
using System.Drawing;
using System.IO;
using System.Linq;
using System.Text;
using System.Windows.Forms;

namespace EA_Neo4j_SafetyAnalysis_AddIn
{
    internal partial class SocketLogWindow : Form
    {
        internal static bool StayVisible { get; set; } = false;

        private Timer closeWindowTimer;

        private StringBuilder nextTextToWrite = new StringBuilder();

        internal SocketLogWindow()
        {
            StayVisible = true;
            closeWindowTimer = new Timer();
            closeWindowTimer.Enabled = true;
            closeWindowTimer.Interval = 100;
            closeWindowTimer.Tick += new System.EventHandler(this.closeWindowTimer_Tick);
            InitializeComponent();
            socketTextBox.Text = Main.consoleOutput.ToString();
            Console.SetOut(new TextBoxWriter(socketTextBox, this));
        }

        private void closeWindowTimer_Tick(object sender, EventArgs e)
        {
            if (!StayVisible)
            {
                Console.SetOut(Main.mainTextWriter);
                this.Close();
            }
        }

        private class TextBoxWriter : TextWriter
        {
            private Control textBox;
            private SocketLogWindow socketLogWindow;
            internal TextBoxWriter(Control _socketTextBox, SocketLogWindow _socketLogWindow)
            {
                textBox = _socketTextBox;
                socketLogWindow = _socketLogWindow;
            }

            public override void Write(char value)
            {
                socketLogWindow.nextTextToWrite.Append(value);
                Main.consoleOutput.Append(value);
            }

            public override void Write(string value)
            {
                socketLogWindow.nextTextToWrite.Append(value);
                Main.consoleOutput.Append(value);
            }

            public override Encoding Encoding
            {
                get { return Encoding.ASCII; }
            }
        }

        internal void writeToTextBox()
        {
            if (socketTextBox.InvokeRequired)
            {
                Invoke((MethodInvoker)(() => socketTextBox.Text += nextTextToWrite.ToString()));
            }
            else
            {
                socketTextBox.Text += nextTextToWrite.ToString();
            }
            nextTextToWrite.Clear();
        }

        private void CloseWindow(object sender, FormClosingEventArgs e)
        {
            Console.SetOut(Main.mainTextWriter);
            this.Close();
        }
    }
}
