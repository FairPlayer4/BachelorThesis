using System;
using Microsoft.CSharp.RuntimeBinder;

namespace EA_Neo4j_SafetyAnalysis_AddIn
{
    internal class EventPropertiesHandler
    {
        private EA.Repository repository;
        private EA.EventProperties eventProperties;

        internal EventPropertiesHandler(EA.Repository repository, EA.EventProperties eventProperties)
        {
            this.repository = repository;
            this.eventProperties = eventProperties;
        }

        internal bool GetPackage(out EA.Package package)
        {
            package = null;
            int packageId;
            if (GetEAObjectId("PackageID", out packageId))
            {
                package = repository.GetPackageByID(packageId);
                return package != null;
            }
            return false;
        }

        internal bool GetElement(out EA.Element element)
        {
            element = null;
            int elementId;
            if (GetEAObjectId("ElementID", out elementId))
            {
                element = repository.GetElementByID(elementId);
                return element != null;
            }
            return false;
        }

        internal bool GetConnector(out EA.Connector connector)
        {
            connector = null;
            int connectorId;
            if (GetEAObjectId("ConnectorID", out connectorId))
            {
                connector = repository.GetConnectorByID(connectorId);
                return connector != null;
            }
            return false;
        }

        internal bool GetDiagram(out EA.Diagram diagram)
        {
            diagram = null;
            int diagramId;
            if (GetEAObjectId("DiagramID", out diagramId))
            {
                diagram = repository.GetDiagramByID(diagramId);
                return diagram != null;
            }
            return false;
        }

        internal bool GetDiagramObject(out EA.Element diagramobject)
        {
            diagramobject = null;
            int diagramobjectId;
            if (GetEAObjectId("ID", out diagramobjectId))
            {
                diagramobject = repository.GetElementByID(diagramobjectId);
                return diagramobject != null;
            }
            return false;
        }

        private bool GetEAObjectId(String idKey, out int id)
        {
            id = 0;
            eventProperties.Get(idKey).Value.ToString();
            return int.TryParse(eventProperties.Get(idKey).Value.ToString(), out id);
        }
    }
}
