using System;
using System.IO;
using System.Windows.Forms;

namespace EA_Neo4j_SafetyAnalysis_AddIn
{
    internal partial class ErrorWindow : Form
    {

        private bool connectionError = false;
        private bool unfixableError = false;
        private bool settingsError = false;

        internal ErrorWindow(string errormessage, string type, Exception e)
        {
            InitializeComponent();
            Utility.ErrorLogger(e);
            Utility.Logger(errormessage);
            errorTextBox.Text += errormessage;
            switch (type)
            {
                case "ConnectionError":
                    errorTextBox.Text += "\nA connection error occured. You can try to reconnect to solve the issue.";
                    actionButton.Text = "Reconnect";
                    cancelButton.Text = "Cancel";
                    connectionError = true;
                    break;
                default:
                    errorTextBox.Text += "\nAn unknown error occured. Restart Enterprise Architect to solve this issue.";
                    actionButton.Text = "Continue";
                    cancelButton.Text = "Cancel";
                    unfixableError = true;
                    break;
            }
        }

        internal ErrorWindow(string errormessage, string type)
        {
            InitializeComponent();
            Utility.Logger(errormessage);
            errorTextBox.Text += errormessage;
            switch (type)
            {
                case "ConnectionError":
                    errorTextBox.Text += "\nA connection error occured. You can try to reconnect to solve the issue.";
                    actionButton.Text = "Reconnect";
                    cancelButton.Text = "Cancel";
                    connectionError = true;
                    break;
                case "UpdateError":
                    actionButton.Text = "Continue";
                    cancelButton.Text = "Cancel";
                    unfixableError = true;
                    break;
                case "SettingsError":
                    errorTextBox.Text += "\nA settings error occured. You can try to repair the settings.";
                    actionButton.Text = "Repair Settings";
                    cancelButton.Text = "Cancel";
                    settingsError = true;
                    break;
                default:
                    actionButton.Text = "Continue";
                    cancelButton.Text = "Cancel";
                    unfixableError = true;
                    break;
            }
        }

        private void cancelButton_Click(object sender, EventArgs e)
        {
            this.Close();
        }

        private void actionButton_Click(object sender, EventArgs e)
        {
            if (connectionError)
            {
                Main.startAddinConnection();
            }
            else
            {
                if (unfixableError)
                {
                    this.Close();
                }
                else
                {
                    if (settingsError)
                    {
                        Main.initializeSettings();
                    }
                }
            }
        }
    }
}
