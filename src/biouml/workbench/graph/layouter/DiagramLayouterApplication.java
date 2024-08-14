package biouml.workbench.graph.layouter;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import com.developmentontheedge.application.ApplicationUtils;

import biouml.model.Diagram;
import biouml.workbench.graph.DiagramToGraphTransformer;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.ExProperties;

/**
 * Diagram layout application.
 * Necessary files in startup folder:
 *   - biouml.lcf - config file for JUL logging
 *   - layouter/layout.config - description of diagram collections and layout properties
 *   - layouter/completeList.txt - list of full names of layouted diagrams. This diagrams will be excluded from diagram list with next running.
 */
public class DiagramLayouterApplication implements IApplication
{
    protected static final Logger log = Logger.getLogger(DiagramLayouterApplication.class.getName());

    protected static final String PROPERTY_BASE_COLLECTION = "collection";
    protected static final String PROPERTY_TARGET_COLLECTION = "result";
    protected static final String PROPERTY_LAYOUTER = "layouter";

    @Override
    public Object start(IApplicationContext arg0)
    {
        try
        {
            File configFile = new File( "biouml.lcf" );
            try( FileInputStream fis = new FileInputStream( configFile ) )
            {
                LogManager.getLogManager().readConfiguration( fis );
            }
            catch( Exception e )
            {
                System.err.println( "Error init logging: " + e );
            }

            Object appArgs = arg0.getArguments().get( "application.args" );
            if( ! ( appArgs instanceof String[] ) )
            {
                if( appArgs == null )
                    appArgs = "null";
                System.out.println( "Can not start: incorrect input application arguments (" + appArgs + ")" );
                return IApplication.EXIT_OK;
            }

            String[] args = (String[])appArgs;
            CollectionFactory.createRepository(args[0]);
            CollectionFactory.createRepository(args[1]);
            System.out.println("Building diagram list...");
            List<ru.biosoft.access.core.DataElementPath> excludeList = readExcludeList(new File("layouter/completeList.txt"));
            List<DiagramLayouterInfo> diagramLayoutInfoList = getDiagramList(new File("layouter/layout.config"), excludeList);
            System.out.println("Layouting...");
            layoutDiagrams(diagramLayoutInfoList, excludeList, new File("layouter/completeList.txt"));
        }
        catch( Throwable t )
        {
            System.err.println("Fatal error in layout application: " + t);
            t.printStackTrace();

            System.out.println("\nPress any key to continue");
            try
            {
                System.in.read();
            }
            catch( Throwable ignore )
            {
            }
        }

        return IApplication.EXIT_OK;
    }

    @Override
    public void stop()
    {
    }

    protected List<ru.biosoft.access.core.DataElementPath> readExcludeList(File excludeListFile) throws Exception
    {
        List<ru.biosoft.access.core.DataElementPath> excludeList = new ArrayList<>();
        try
        {
            for(String line : ApplicationUtils.readAsList( excludeListFile ))
            {
                excludeList.add(DataElementPath.create(line.trim()));
            }
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not read exclude list", e);
        }
        return excludeList;
    }

    protected List<DiagramLayouterInfo> getDiagramList(File propertiesFile, List<ru.biosoft.access.core.DataElementPath> excludeList) throws Exception
    {
        List<DiagramLayouterInfo> result = new ArrayList<>();

        Properties properties = new ExProperties(propertiesFile);

        DataElementPath collectionPath = DataElementPath.create(properties.getProperty(PROPERTY_BASE_COLLECTION));
        String layouterClassName = properties.getProperty(PROPERTY_LAYOUTER);
        Properties layouterProperties = readLayouterProperties(properties);
        DataCollection<?> targetCollection = CollectionFactory.getDataCollection(properties.getProperty(PROPERTY_TARGET_COLLECTION));
        for( DataElementPath path: collectionPath.getChildren() )
        {
            if( !excludeList.contains(path) )
            {
                DiagramLayouterInfo info = new DiagramLayouterInfo(path, layouterClassName, layouterProperties, targetCollection);
                result.add(info);
            }
        }

        return result;
    }

    protected Properties readLayouterProperties(Properties baseProperties)
    {
        Properties result = new Properties();
        Iterator<?> iter = baseProperties.keySet().iterator();
        while( iter.hasNext() )
        {
            String key = (String)iter.next();
            String[] keys = key.split("\\.");
            if( ( keys.length == 2 ) && ( keys[0].equals(PROPERTY_LAYOUTER) ) )
            {
                result.put(keys[1], baseProperties.getProperty(key));
            }
        }
        return result;
    }

    public void layoutDiagrams(List<DiagramLayouterInfo> diagramLayouts, List<ru.biosoft.access.core.DataElementPath> excludeList, File completeFile) throws Exception
    {
        try (PrintWriter completeWriter = new PrintWriter(
                new OutputStreamWriter( new FileOutputStream( completeFile, true ), StandardCharsets.UTF_8 ) ))
        {
            for( DiagramLayouterInfo info : diagramLayouts )
            {
                DataElementPath fullName = info.getDiagramCompleteName();
                Object diagramObj = fullName.optDataElement();
                if( diagramObj instanceof Diagram )
                {
                    Diagram diagram = (Diagram)diagramObj;
                    DiagramToGraphTransformer.layout(diagram, info.getLayouter());
                    info.getTargetCollection().put(diagram);

                    completeWriter.println(fullName);
                    completeWriter.flush();
                    System.out.println(fullName);
                }
            }
        }
    }
}
