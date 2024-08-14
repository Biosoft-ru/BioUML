package biouml.plugins.chemoinformatics;

import java.awt.Dimension;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import com.developmentontheedge.application.ApplicationUtils;
import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.standard.type.CDKRenderer;
import biouml.standard.type.Structure;
import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementImporter;
import ru.biosoft.access.exception.BiosoftParseException;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;

public class SDFImporter implements DataElementImporter
{
    public static final String STRUCTURE_COLUMN = "Molecule structure";

    public static final String SUMMARY_TABLE_NAME = "summary";

    private static final Pattern PROPERTY_NAME_PATTERN = Pattern.compile( ">\\s*<(\\S*)>.*" );

    protected static final Logger log = Logger.getLogger(SDFImporter.class.getName());

    protected ImportProperties properties;

    @Override
    public int accept(DataCollection<?> parent, File file)
    {
        if( parent.isAcceptable( Structure.class ) )
        {
            return isSdfFile( file ) ? ACCEPT_HIGH_PRIORITY : ACCEPT_UNSUPPORTED;
        }
        return ACCEPT_UNSUPPORTED;
    }

    private boolean isSdfFile(File file)
    {
        if (file == null || file.getName().toLowerCase().endsWith(".sdf"))
            return true;
        try(BufferedReader br = ApplicationUtils.asciiReader( file ))
        {
            return br.lines().skip( 3 ).limit( 1 ).anyMatch( line -> line.trim().endsWith( "V2000" ) );
        }
        catch(IOException ex)
        {
            return false;
        }
    }

    @Override
    public DataElement doImport(@Nonnull DataCollection<?> parent, @Nonnull File file, String elementName, FunctionJobControl jobControl, Logger log) throws Exception
    {
        if( jobControl != null )
        {
            jobControl.functionStarted();
        }
        if( parent.contains(elementName) )
        {
            parent.remove(elementName);
        }

        DataCollection<?> structures = DataCollectionUtils.createSubCollection( parent.getCompletePath().getChildPath( elementName ) );
        importSDF(structures, file);
        if( jobControl != null )
        {
            jobControl.functionFinished();
        }
        return structures;
    }

    @Override
    public boolean init(Properties properties)
    {
        return true;
    }

    @Override
    public Object getProperties(DataCollection<?> parent, File file, String elementName)
    {
        try
        {
            properties = new ImportProperties(file);
            return properties;
        }
        catch( Exception ex )
        {
            log.log(Level.SEVERE, "Error while attempting to create Import properties: " + ex.getMessage());
            return null;
        }
    }

    protected void importSDF(@Nonnull DataCollection<?> parent, File sdfFile) throws Exception
    {
        StringBuilder buffer = new StringBuilder();
        String data = null;
        boolean generatedKeys = properties.getKey().equals(GENERATE_UNIQUE_ID);
        String format = "";
        List<Structure> structures = new ArrayList<>();
        Map<String, Class<?>> columns = new TreeMap<>();
        if(generatedKeys)
        {
            format = getIdFormat(parent.getName(), sdfFile);
        }
        int n = 1;
        try(BufferedReader reader = ApplicationUtils.asciiReader( sdfFile ))
        {
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                if( line.startsWith("$$$$") )
                {
                    Map<String, Object> attrs;
                    try
                    {
                        attrs = getProperties(buffer.toString());
                    }
                    catch( Exception e )
                    {
                        throw new BiosoftParseException( e, sdfFile.getName() );
                    }
                    String key = null;
                    if( generatedKeys )
                    {
                        key = String.format(format, n);
                    }
                    else
                    {
                        key = normalizeKey(attrs.get(properties.getKey()).toString());
                    }
                    Structure structure = new Structure(parent, key);
                    structures.add( structure );
                    structure.setFormat("MOL");
                    structure.setData(data);

                    for( Map.Entry<String, Object> entry : attrs.entrySet() )
                    {
                        columns.put( entry.getKey(), entry.getValue().getClass() );
                        writeProperty(structure.getAttributes(), entry.getKey(), entry.getValue());
                    }
                    CollectionFactoryUtils.save( structure );
                    buffer = new StringBuilder();
                    n++;
                }
                else
                {
                    buffer.append(line);
                    buffer.append('\n');

                    if( line.startsWith("M  END") )
                    {
                        data = buffer.toString();
                        buffer = new StringBuilder();
                    }
                }
            }
        }
        if(properties.isCreateTable())
        {
            createTable( parent, structures, columns );
        }
    }

    private void createTable(@Nonnull DataCollection<?> parent, List<Structure> structures, Map<String, Class<?>> columns) throws Exception
    {
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection( parent, SUMMARY_TABLE_NAME );
        table.getInfo().getProperties().setProperty( DataCollectionConfigConstants.URL_TEMPLATE, "de:"+parent.getCompletePath()+"/$id$" );
        table.getColumnModel().addColumn( STRUCTURE_COLUMN, CompositeView.class );
        for(Entry<String, Class<?>> entry : columns.entrySet())
        {
            table.getColumnModel().addColumn( entry.getKey(), entry.getValue() );
        }
        for(Structure structure : structures)
        {
            RowDataElement rde = new RowDataElement( structure.getName(), table );
            rde.setValues( new Object[table.getColumnModel().getColumnCount()] );
            try
            {
                CompositeView view = CDKRenderer.createStructureView( structure, new Dimension(200,150), ApplicationUtils.getGraphics() );
                rde.setValue( STRUCTURE_COLUMN, view );
            }
            catch( Exception e )
            {
                ExceptionRegistry.log( e );
            }
            for(DynamicProperty property : structure.getAttributes())
            {
                rde.setValue( property.getName(), property.getValue() );
            }
            table.put( rde );
        }
        CollectionFactoryUtils.save( table );
    }

    private String getIdFormat(String defName, File sdfFile) throws IOException
    {
        String format;
        int count = 0;
        try(BufferedReader reader = ApplicationUtils.asciiReader( sdfFile ))
        {
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                if( line.startsWith("$$$$") )
                    count++;
            }
        }
        if(count == 1)
        {
            return "Structure of "+defName;
        }
        int nDigits = 1;
        while(count >= 10)
        {
            count/=10;
            nDigits++;
        }
        format = "Structure %0"+nDigits+"d";
        return format;
    }

    private String normalizeKey(String key)
    {
        return key.replaceAll( "[\n\r\t\\?\\*\\:\"\']", " " );
    }

    protected static String extractPropertyName(String line)
    {
        Matcher matcher = PROPERTY_NAME_PATTERN.matcher( line );
        if(matcher.matches())
        {
            return matcher.group( 1 );
        }
        return null;
    }

    protected Map<String, Object> getProperties(String props)
    {
        Map<String, Object> result = new HashMap<>();
        String pName = null;
        StringBuilder pValue = null;
        StringTokenizer st = new StringTokenizer(props, "\n");
        while( st.hasMoreTokens() )
        {
            String line = st.nextToken();
            String newPropertyName = extractPropertyName( line );
            if( newPropertyName != null )
            {
                if( pName != null )
                {
                    if( pName.equals("PASS_ACTIVITY_SPECTRUM") )
                    {
                        //import activities as StringSet
                        Set<String> values = new TreeSet<>();
                        for( String str : pValue.toString().split("\n") )
                        {
                            values.add(str);
                        }
                        result.put( pName, new StringSet( values ) );
                    }
                    else
                    {
                        result.put(pName, pValue.toString().trim());
                    }
                }
                pValue = new StringBuilder();
                pName = newPropertyName;
            }
            else
            {
                if(pValue != null)
                {
                    pValue.append(line);
                    pValue.append('\n');
                } else
                {
                    throw new IllegalStateException( "Expected property name, got: "+line );
                }
            }
        }
        if( pName != null )
        {
            result.put(pName, pValue.toString());
        }
        return result;
    }

    protected void writeProperty(DynamicPropertySet dps, String name, Object value)
    {
        try
        {
            Class<?> clazz = null;
            if( value instanceof StringSet )
            {
                clazz = StringSet.class;
            }
            else
            {
                clazz = String.class;
            }
            dps.add(new DynamicProperty(name, clazz, value));
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not add structure properties", e);
        }
    }

    private static List<String> getPossibleKeys(File file) throws IOException
    {
        Set<String> possibleKeys = null;
        List<String> currentKeys = new ArrayList<>();
        try(BufferedReader reader = ApplicationUtils.asciiReader( file ))
        {
            String line;
            while( ( line = reader.readLine() ) != null )
            {
                if( line.startsWith("$$$$") )
                {
                    if(possibleKeys == null)
                    {
                        possibleKeys = new TreeSet<>(currentKeys);
                    } else
                    {
                        possibleKeys.retainAll( currentKeys );
                    }
                    currentKeys = new ArrayList<>();
                }
                String propertyName = extractPropertyName( line );
                if( propertyName != null )
                {
                    currentKeys.add(propertyName);
                }
            }
        }
        List<String> result = new ArrayList<>();
        result.add( GENERATE_UNIQUE_ID );
        result.addAll( possibleKeys );
        return result;
    }

    private final static String GENERATE_UNIQUE_ID = "Generate unique ID";

    @SuppressWarnings ( "serial" )
    @PropertyName("SDF import properties")
    public static class ImportProperties extends Option
    {
        private final List<String> possibleKeys;
        private String key;

        private boolean createTable = true;

        public ImportProperties(File file) throws IOException
        {
            possibleKeys = SDFImporter.getPossibleKeys(file);
            key = GENERATE_UNIQUE_ID;
        }

        public StreamEx<String> getPossibleKeys()
        {
            return StreamEx.of(possibleKeys);
        }

        @PropertyName("Names for the structures")
        public String getKey()
        {
            return key;
        }

        public void setKey(String key)
        {
            Object oldValue = this.key;
            this.key = key;
            firePropertyChange( "key", oldValue, this.key );
        }

        @PropertyName("Generate summary table")
        public boolean isCreateTable()
        {
            return createTable;
        }

        public void setCreateTable(boolean createTable)
        {
            Object oldValue = this.createTable;
            this.createTable = createTable;
            firePropertyChange( "createTable", oldValue, this.createTable );
        }
    }

    public static class ImportPropertiesBeanInfo extends BeanInfoEx2<ImportProperties>
    {
        public ImportPropertiesBeanInfo()
        {
            super(ImportProperties.class);
        }
        @Override
        public void initProperties() throws Exception
        {
            addWithTags("key", ImportProperties::getPossibleKeys);
            add("createTable");
        }
    }

    @Override
    public Class<? extends DataElement> getResultType()
    {
        return Structure.class;
    }
}
