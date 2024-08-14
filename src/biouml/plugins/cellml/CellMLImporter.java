package biouml.plugins.cellml;

import java.io.File;
import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import biouml.model.Diagram;
import biouml.model.DiagramImporter;
import biouml.model.Module;

public class CellMLImporter extends DiagramImporter
{
    @Override
    public int accept(File file)
    {
        try
        {
            String header = ApplicationUtils.readAsString(file, 2000);

            int iXml = header.indexOf("<?xml");

            if ( iXml < 0 || ! header.substring(iXml, iXml + 100 > header.length() ? header.length() : iXml + 100).matches (
                "(\\s)*<\\?xml(\\s)*version(\\s)*=(.|\\s)*"))
            {
                return ACCEPT_UNSUPPORTED;
            }

            // these two things, "<model" and "name" are comprised in SBML models too
            // Exclude SBML here
            if (header.indexOf("<sbml") != -1)
                return ACCEPT_UNSUPPORTED;

            int index = header.indexOf("<model");
            if (index == -1)
                return ACCEPT_UNSUPPORTED;

            int indexName = header.indexOf("name", index + 6);
            if (indexName == -1)
                return ACCEPT_UNSUPPORTED;

            return ACCEPT_HIGH_PRIORITY;
        }
        catch (Throwable t)
        {
//            t.printStackTrace();
//            log.log(Level.SEVERE, "accept error :", t);
        }

        return ACCEPT_UNSUPPORTED;
    }

    @Override
    public DataElement doImport(Module module, File file, String diagramName) throws Exception
    {
        DataCollection<Diagram> origin = module.getDiagrams();
        Diagram diagram = new CellMLModelReader(file).read(origin);

        if( diagramName != null && diagramName.trim().length() > 0 && !diagramName.equals(diagram.getName()) )
            diagram = diagram.clone(origin, diagramName);

        origin.put(diagram);
        return diagram;
    }
}
