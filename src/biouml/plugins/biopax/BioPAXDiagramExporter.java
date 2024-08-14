package biouml.plugins.biopax;

import java.io.File;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.model.DiagramExporter;
import biouml.plugins.biopax.writer.BioPAXWriterFactory;
import biouml.plugins.biopax.writer.BioPAXWriter;


public class BioPAXDiagramExporter extends DiagramExporter
{
    protected BioPAXWriter writer;
    protected Diagram exportDiagram;

    @Override
    public boolean accept(Diagram diagram)
    {
        try
        {
            return (diagram.getType() instanceof BioPAXDiagramType);
        }
        catch (Throwable t) {}

        return false;
    }

    @Override
    public void doExport(@Nonnull Diagram diagram, final @Nonnull File file) throws Exception
    {
        this.exportDiagram = diagram;
        writer = BioPAXWriterFactory.getWriter(BioPAXSupport.BIOPAX_LEVEL_2);
        (new Thread()
        {
            @Override
            public void run()
            {
                writer.writeDiagram(exportDiagram, file);
            }
        }).start();
    }

    @Override
    public boolean init(String format, String suffix)
    {
        return true;
    }
}