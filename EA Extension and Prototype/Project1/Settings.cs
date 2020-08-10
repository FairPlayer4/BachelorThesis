namespace EA_Neo4j_SafetyAnalysis_AddIn
{
    internal class Settings
    {

        private string projectguid;

        internal string ProjectGUID
        {
            get
            {
                return projectguid;
            }
            set
            {
                projectguid = value;
                Utility.updateSettings(this);
            }
        }

        private string neo4jpath;

        internal string Neo4jPath
        {
            get
            {
                return neo4jpath;
            }
            set
            {
                neo4jpath = value;
                Utility.updateSettings(this);
            }
        }

        private string lastupdate;

        internal string LastUpdate
        {
            get
            {
                return lastupdate;
            }
            set
            {
                lastupdate = value;
                Utility.updateSettings(this);
            }
        }

        internal void resetLastUpdate()
        {
            lastupdate = "never";
        }

        private bool contupdate;

        internal bool ContinuousUpdate
        {
            get
            {
                return contupdate;
            }
            set
            {
                contupdate = value;
                Utility.updateSettings(this);
            }
        }

        private bool contanalysis;

        internal bool ContinuousAnalysis
        {
            get
            {
                return contanalysis;
            }
            set
            {
                contanalysis = value;
                Utility.updateSettings(this);
            }
        }

        internal Settings(string projectguid, string neo4jpath, string lastupdate, bool contupdate, bool contanalysis)
        {
            this.projectguid = projectguid;
            this.neo4jpath = neo4jpath;
            this.lastupdate = lastupdate;
            this.contupdate = contupdate;
            this.contanalysis = contanalysis;
        }

        internal void changeSettings(string projectguid, string neo4jpath, string lastupdate, bool contupdate, bool contanalysis)
        {
            this.projectguid = projectguid;
            this.neo4jpath = neo4jpath;
            this.lastupdate = lastupdate;
            this.contupdate = contupdate;
            this.contanalysis = contanalysis;
            Utility.updateSettings(this);
        }

    }
}
