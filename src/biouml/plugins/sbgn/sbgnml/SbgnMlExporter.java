package biouml.plugins.sbgn.sbgnml;

import java.io.File;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.model.DiagramExporter;
import biouml.plugins.sbgn.SbgnDiagramType;

public class SbgnMlExporter extends DiagramExporter
{
    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram.getType() instanceof SbgnDiagramType;
    }

    @Override
    public void doExport(@Nonnull Diagram diagram, @Nonnull File file) throws Exception
    {
        SbgnMlWriter writer = new SbgnMlWriter();
        writer.write( diagram, file );
    }

    @Override
    public boolean init(String format, String suffix)
    {
        return true;
    }

}
