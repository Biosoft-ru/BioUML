package biouml.plugins.antimony;

import java.io.File;

import javax.annotation.Nonnull;

import java.util.logging.Level;
import java.util.logging.Logger;

import biouml.model.Diagram;
import biouml.model.DiagramExporter;

import com.developmentontheedge.application.ApplicationUtils;

/**
 * Exporter to Antimony format
 */
public class AntimonyExporter extends DiagramExporter
{
    protected static final Logger log = Logger.getLogger(AntimonyExporter.class.getName());


    @Override
    public boolean accept(Diagram diagram)
    {
        //AntimonyExporter can export SBML diagram
        return AntimonyUtility.checkDiagramType(diagram);
    }

    @Override
    public void doExport(@Nonnull Diagram diagram, @Nonnull File file) throws Exception
    {
        try
        {
            Antimony antimony = new Antimony(diagram);
            antimony.createAst();
            String text = antimony.generateText();
            ApplicationUtils.writeString( file, text );
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  "ERROR_DIAGRAM_WRITING: " + diagram.getName(), e );
        }
    }

    @Override
    public boolean init(String format, String suffix)
    {
        return true;
    }

}
