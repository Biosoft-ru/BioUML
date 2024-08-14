package biouml.plugins.gxl;

import java.io.File;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.model.DiagramExporter;


public class GxlExporter extends DiagramExporter
{
    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram != null;
    }

    @Override
    public void doExport(@Nonnull Diagram diagram, @Nonnull File file) throws Exception
    {
        new GxlWriter().writeDiagram(file, diagram);
    }

    @Override
    public boolean init(String format, String suffix)
    {
        return true;
    }
}
