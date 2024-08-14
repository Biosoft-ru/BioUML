package biouml.model;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.DataElementExporter;
import ru.biosoft.access.DataElementExporterRegistry;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

/**
 * General interface to export diagram in the specified format,
 * for example in SBML format or as PNG image.
 */
public abstract class DiagramExporter implements DataElementExporter
{
    public static final String DIAGRAM_TYPE = "diagramType";
    protected String diagramType;

    /**
     * new paths for all subdiagrams 
     */
    protected Map<String, String> newPaths = new HashMap<>();
    
    public void setNewPaths(Map<String, String> newPaths)
    {
        this.newPaths = newPaths;
    }
    
    /** Returns true if the specified diagram can be exported in this format and false otherwise. */
    public abstract boolean accept(Diagram diagram);

    /**
     * Exports the specified diagram into the specified file or directory.
     * @param diagram - diagram to be exported
     * @param file - name of file or directory where the export results will be stored.
     */
    public abstract void doExport(@Nonnull Diagram diagram, @Nonnull File file) throws Exception;

    public abstract boolean init(String format, String suffix);

    @Override
    public int accept(DataElement de)
    {
        if( ! ( de instanceof Diagram ) )
            return DataElementExporter.ACCEPT_UNSUPPORTED;
        if( !diagramType.equals("*") && !diagramType.equals( ( (Diagram)de ).getType().getClass().getName()) )
            return DataElementExporter.ACCEPT_UNSUPPORTED;
        if( accept((Diagram)de) )
        {
            return DataElementExporter.ACCEPT_HIGH_PRIORITY;
        }
        return DataElementExporter.ACCEPT_UNSUPPORTED;
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file) throws Exception
    {
        doExport((Diagram)de, file);
    }

    @Override
    public void doExport(@Nonnull ru.biosoft.access.core.DataElement de, @Nonnull File file, FunctionJobControl jobControl) throws Exception
    {
        if( jobControl == null )
        {
            doExport(de, file);
            return;
        }
        jobControl.functionStarted();
        doExport((Diagram)de, file);
        if( jobControl.getStatus() != JobControl.TERMINATED_BY_REQUEST && jobControl.getStatus() != JobControl.TERMINATED_BY_ERROR )
        {
            jobControl.setPreparedness(100);
            jobControl.functionFinished();
        }
    }

    @Override
    public boolean init(Properties properties)
    {
        diagramType = properties.getProperty(DIAGRAM_TYPE);
        if( diagramType == null || diagramType.equals("") )
            diagramType = "*";
        return init(properties.getProperty(DataElementExporterRegistry.FORMAT), properties.getProperty(DataElementExporterRegistry.SUFFIX));
    }

    @Override
    public List<Class<? extends DataElement>> getSupportedTypes()
    {
        return Collections.singletonList( Diagram.class );
    }
}