namespace EA_Neo4j_SafetyAnalysis_AddIn
{
    partial class SettingsWindow
    {
        /// <summary>
        /// Required designer variable.
        /// </summary>
        private System.ComponentModel.IContainer components = null;

        /// <summary>
        /// Clean up any resources being used.
        /// </summary>
        /// <param name="disposing">true if managed resources should be disposed; otherwise, false.</param>
        protected override void Dispose(bool disposing)
        {
            if (disposing && (components != null))
            {
                components.Dispose();
            }
            base.Dispose(disposing);
        }

        #region Windows Form Designer generated code

        /// <summary>
        /// Required method for Designer support - do not modify
        /// the contents of this method with the code editor.
        /// </summary>
        private void InitializeComponent()
        {
            this.neo4jpathTextBox = new System.Windows.Forms.TextBox();
            this.changeneo4jpathButton = new System.Windows.Forms.Button();
            this.contanalysisCheckBox = new System.Windows.Forms.CheckBox();
            this.contupdateCheckBox = new System.Windows.Forms.CheckBox();
            this.label1 = new System.Windows.Forms.Label();
            this.devCheckBox = new System.Windows.Forms.CheckBox();
            this.button1 = new System.Windows.Forms.Button();
            this.SuspendLayout();
            // 
            // neo4jpathTextBox
            // 
            this.neo4jpathTextBox.Location = new System.Drawing.Point(45, 54);
            this.neo4jpathTextBox.Name = "neo4jpathTextBox";
            this.neo4jpathTextBox.Size = new System.Drawing.Size(500, 26);
            this.neo4jpathTextBox.TabIndex = 0;
            // 
            // changeneo4jpathButton
            // 
            this.changeneo4jpathButton.Location = new System.Drawing.Point(45, 100);
            this.changeneo4jpathButton.Name = "changeneo4jpathButton";
            this.changeneo4jpathButton.Size = new System.Drawing.Size(500, 30);
            this.changeneo4jpathButton.TabIndex = 1;
            this.changeneo4jpathButton.Text = "Change Neo4j Directory Path";
            this.changeneo4jpathButton.UseVisualStyleBackColor = true;
            this.changeneo4jpathButton.Click += new System.EventHandler(this.changeneo4jpathButton_Click);
            // 
            // contanalysisCheckBox
            // 
            this.contanalysisCheckBox.AutoSize = true;
            this.contanalysisCheckBox.Location = new System.Drawing.Point(45, 163);
            this.contanalysisCheckBox.Name = "contanalysisCheckBox";
            this.contanalysisCheckBox.Size = new System.Drawing.Size(178, 24);
            this.contanalysisCheckBox.TabIndex = 2;
            this.contanalysisCheckBox.Text = "Continuous Analysis";
            this.contanalysisCheckBox.UseVisualStyleBackColor = true;
            this.contanalysisCheckBox.CheckedChanged += new System.EventHandler(this.contanalysisCheckBox_CheckedChanged);
            // 
            // contupdateCheckBox
            // 
            this.contupdateCheckBox.AutoSize = true;
            this.contupdateCheckBox.Checked = true;
            this.contupdateCheckBox.CheckState = System.Windows.Forms.CheckState.Checked;
            this.contupdateCheckBox.Location = new System.Drawing.Point(45, 208);
            this.contupdateCheckBox.Name = "contupdateCheckBox";
            this.contupdateCheckBox.Size = new System.Drawing.Size(173, 24);
            this.contupdateCheckBox.TabIndex = 3;
            this.contupdateCheckBox.Text = "Continuous Update";
            this.contupdateCheckBox.UseVisualStyleBackColor = true;
            this.contupdateCheckBox.CheckedChanged += new System.EventHandler(this.contupdateCheckBox_CheckedChanged);
            // 
            // label1
            // 
            this.label1.AutoSize = true;
            this.label1.Location = new System.Drawing.Point(211, 21);
            this.label1.Name = "label1";
            this.label1.Size = new System.Drawing.Size(209, 20);
            this.label1.TabIndex = 4;
            this.label1.Text = "Directory of Neo4j Database";
            this.label1.TextAlign = System.Drawing.ContentAlignment.MiddleCenter;
            // 
            // devCheckBox
            // 
            this.devCheckBox.AutoSize = true;
            this.devCheckBox.Location = new System.Drawing.Point(45, 252);
            this.devCheckBox.Name = "devCheckBox";
            this.devCheckBox.Size = new System.Drawing.Size(151, 24);
            this.devCheckBox.TabIndex = 5;
            this.devCheckBox.Text = "Developer Mode";
            this.devCheckBox.UseVisualStyleBackColor = true;
            this.devCheckBox.CheckedChanged += new System.EventHandler(this.devCheckBox_CheckedChanged);
            // 
            // button1
            // 
            this.button1.Location = new System.Drawing.Point(427, 234);
            this.button1.Name = "button1";
            this.button1.Size = new System.Drawing.Size(205, 59);
            this.button1.TabIndex = 6;
            this.button1.Text = "Clear and Re-Update Neo4j Database";
            this.button1.UseVisualStyleBackColor = true;
            this.button1.Click += new System.EventHandler(this.clearDB_Click);
            // 
            // SettingsWindow
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(9F, 20F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(656, 317);
            this.Controls.Add(this.button1);
            this.Controls.Add(this.devCheckBox);
            this.Controls.Add(this.label1);
            this.Controls.Add(this.contupdateCheckBox);
            this.Controls.Add(this.contanalysisCheckBox);
            this.Controls.Add(this.changeneo4jpathButton);
            this.Controls.Add(this.neo4jpathTextBox);
            this.Name = "SettingsWindow";
            this.Text = "Settings";
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.TextBox neo4jpathTextBox;
        private System.Windows.Forms.Button changeneo4jpathButton;
        private System.Windows.Forms.CheckBox contanalysisCheckBox;
        private System.Windows.Forms.CheckBox contupdateCheckBox;
        private System.Windows.Forms.Label label1;
        private System.Windows.Forms.CheckBox devCheckBox;
        private System.Windows.Forms.Button button1;
    }
}