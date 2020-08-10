namespace EA_Neo4j_SafetyAnalysis_AddIn
{
    partial class SocketLogWindow
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
            this.socketTextBox = new System.Windows.Forms.TextBox();
            this.SuspendLayout();
            // 
            // socketTextBox
            // 
            this.socketTextBox.Location = new System.Drawing.Point(12, 12);
            this.socketTextBox.Multiline = true;
            this.socketTextBox.Name = "socketTextBox";
            this.socketTextBox.ScrollBars = System.Windows.Forms.ScrollBars.Both;
            this.socketTextBox.Size = new System.Drawing.Size(678, 870);
            this.socketTextBox.TabIndex = 0;
            // 
            // SocketLogWindow
            // 
            this.AutoScaleDimensions = new System.Drawing.SizeF(9F, 20F);
            this.AutoScaleMode = System.Windows.Forms.AutoScaleMode.Font;
            this.ClientSize = new System.Drawing.Size(702, 894);
            this.Controls.Add(this.socketTextBox);
            this.Name = "SocketLogWindow";
            this.Text = "SocketLogWindow";
            this.FormClosing += new System.Windows.Forms.FormClosingEventHandler(this.CloseWindow);
            this.ResumeLayout(false);
            this.PerformLayout();

        }

        #endregion

        private System.Windows.Forms.TextBox socketTextBox;
    }
}