package biouml.plugins.bionetgen.diagram;

import java.io.File;
import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.model.DiagramExporter;

/**
 * Export diagram to BioNetGen Language file format (.bngl)
 */
public class BionetgenExporter extends DiagramExporter
{
    @Override
    public boolean accept(Diagram diagram)
    {
        return BionetgenUtils.checkDiagramType(diagram);
    }

    @Override
    public void doExport(@Nonnull Diagram diagram, @Nonnull File file) throws Exception
    {
        Bionetgen bionetgen = new Bionetgen(diagram);
        ApplicationUtils.writeString( file, bionetgen.generateText() );
    }

    @Override
    public boolean init(String format, String suffix)
    {
        return true;
    }

}
