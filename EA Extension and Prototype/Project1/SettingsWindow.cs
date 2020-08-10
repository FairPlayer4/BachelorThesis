using System;
using System.Windows.Forms;

namespace EA_Neo4j_SafetyAnalysis_AddIn
{
    internal partial class SettingsWindow : Form
    {

        internal SettingsWindow(SocketConnection connection)
        {
            InitializeComponent();
            neo4jpathTextBox.Text = Main.settings.Neo4jPath;
            if (Main.settings.ContinuousUpdate)
            {
                contupdateCheckBox.Checked = true;
            }
            else
            {
                contupdateCheckBox.Checked = false;
            }
            if (Main.settings.ContinuousAnalysis)
            {
                contanalysisCheckBox.Checked = true;
            }
            else
            {
                contanalysisCheckBox.Checked = false;
            }
        }

        private void contupdateCheckBox_CheckedChanged(object sender, EventArgs e)
        {
            if (contupdateCheckBox.Checked == true)
            {
                if (!Main.settings.ContinuousUpdate)
                {
                    Main.settings.ContinuousUpdate = true;
                    Main.connection.outsideUpdate();
                }
            }
            else
            {
                if (Main.settings.ContinuousUpdate)
                {
                    Main.settings.ContinuousUpdate = false;
                }
            }
        }

        private void contanalysisCheckBox_CheckedChanged(object sender, EventArgs e)
        {
            if (contupdateCheckBox.Checked == true)
            {
                if (!Main.settings.ContinuousAnalysis)
                {
                    Main.settings.ContinuousAnalysis = true;
                    Main.connection.performFullAnalysis();
                }
            }
            else
            {
                if (Main.settings.ContinuousAnalysis)
                {
                    Main.settings.ContinuousAnalysis = false;
                }
            }
        }

        private void changeneo4jpathButton_Click(object sender, EventArgs e)
        {
            using (var fbd = new FolderBrowserDialog())
            {
                DialogResult result = fbd.ShowDialog();

                if (result == DialogResult.OK && !string.IsNullOrWhiteSpace(fbd.SelectedPath))
                {
                    Main.settings.Neo4jPath = fbd.SelectedPath + @"\" + Main.settings.ProjectGUID;
                    Main.connection.changeNeo4jPath();
                    neo4jpathTextBox.Text = Main.settings.Neo4jPath;
                }
            }
        }

        private void devCheckBox_CheckedChanged(object sender, EventArgs e)
        {
            if (devCheckBox.Checked == true)
            {
                if (!Utility.DeveloperMode)
                {
                    Utility.DeveloperMode = true;
                }
            }
            else
            {
                if (Utility.DeveloperMode)
                {
                    Utility.DeveloperMode = false;
                }
            }
        }

        private void clearDB_Click(object sender, EventArgs e)
        {
            Main.settings.LastUpdate = "never";
            Main.connection.outsideUpdate();
        }
    }
}
