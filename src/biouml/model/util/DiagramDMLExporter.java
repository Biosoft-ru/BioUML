package biouml.model.util;

import java.io.File;
import java.io.FileOutputStream;

import javax.annotation.Nonnull;

import biouml.model.Diagram;
import biouml.model.DiagramExporter;

/**
 * Exports diagram into BioUML format (.dml)
 */
public class DiagramDMLExporter extends DiagramExporter
{
    /** Accepts any diagram. */
    @Override
    public boolean accept(Diagram diagram)
    {
        return diagram != null;
    }

    @Override
    public boolean init(String format, String suffix)
    {
        return true;
    }

    @Override
    public void doExport(@Nonnull Diagram diagram, @Nonnull File file) throws Exception
    {
        try (FileOutputStream fos = new FileOutputStream( file ))
        {
            DiagramXmlWriter writer = new DiagramXmlWriter( fos );
            writer.setReplacements( newPaths );
            writer.write( diagram );
        }
    }
}
