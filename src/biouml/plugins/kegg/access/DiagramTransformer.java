
package biouml.plugins.kegg.access;

import com.developmentontheedge.application.ApplicationUtils;

import ru.biosoft.access.FileDataElement;
import biouml.model.Diagram;
import biouml.model.Module;
import biouml.model.util.DiagramXmlTransformer;

public class DiagramTransformer extends DiagramXmlTransformer
{
    /**
     * Reads diagram in DML or KGML format.
     *
     * It automatically detects format. For this purpose it reads
     * the beginning of file, if it contains "<dml" element, then this is DML format
     * and KGML otherwise.
     */
    @Override
    public Diagram transformInput(FileDataElement fde) throws Exception
    {
        Module module = Module.getModule(fde);
        Diagram diagram = null;

        // define the format
        String str = ApplicationUtils.readAsString(fde.getFile(), 100);
        int offset = str.indexOf("<dml");

        if( offset > 1 )
        { // DML format
            DiagramReader reader = new DiagramReader(fde.getFile());
            diagram = reader.read(getTransformedCollection(), fde.getName(), module);
        }
        else
        { // KGML format
            KgmlDiagramReader reader = new KgmlDiagramReader(fde.getFile());
            diagram = reader.read(module.getDiagrams(), fde.getName(), module);
        }

        return diagram;
    }

    /**
     * Stub to disable diagram saving
     */
    @Override
    public FileDataElement transformOutput(Diagram output) throws Exception
    {
        return null;
    }
}
