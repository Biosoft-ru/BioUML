package ru.biosoft.galaxy.filters;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import one.util.streamex.StreamEx;
import ru.biosoft.galaxy.GalaxyDataCollection;
import ru.biosoft.util.XmlUtil;

/**
 * @author lan
 *
 */
public class DataTablesPool
{
    private static Logger log = Logger.getLogger(DataTablesPool.class.getName());
    private static Map<String, List<DataTable>> tables = new HashMap<>();
    private static Map<String, DataTable> mergedTables = new HashMap<>();
    
    /**
     * Get predefined DataTable (from "tool_data_table_conf.xml") by its name
     * @param name
     * @return
     */
    public static DataTable getDataTable(String name)
    {
        initDataTables();
        return mergedTables.computeIfAbsent( name, key -> {
            List<DataTable> tableList = tables.get( key );
            if( tableList == null || tableList.isEmpty() )
                return null;
            DataTable firstTable = tableList.get( 0 );
            if( tableList.size() == 1 )
                return firstTable;
            List<String> columns = firstTable.getColumns();
            List<List<String>> content = StreamEx.of( firstTable.getContent() ).map( Arrays::asList ).toList();

            for( DataTable t : tableList )
                if( !t.getColumns().equals( columns ) )
                {
                    log.warning( "Galaxy data tables for " + name + " has distinct columns" );
                }
                else
                {
                    content.addAll( StreamEx.of( t.getContent() ).map( Arrays::asList ).toList() );
                }
            List<String[]> distinctContent = StreamEx.of( content ).distinct().map( l -> l.toArray( new String[0] ) ).toList();
            return new DataTable( key, columns, distinctContent );
        } );
    }

    private static boolean initialized = false;
    private synchronized static void initDataTables()
    {
        if(initialized) return;
        initialized = true;
        for( File file : GalaxyDataCollection.getGalaxyDistFiles().getToolDataConfFiles() )
        {
            try
            {
                DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                DocumentBuilder builder = factory.newDocumentBuilder();
                Document doc = builder.parse( file );
                NodeList tableList = doc.getDocumentElement().getElementsByTagName( "table" );
                for( Element child : XmlUtil.elements( tableList ) )
                {
                    DataTable table = new DataTable( child );
                    tables.computeIfAbsent( table.getName(), x -> new ArrayList<>() ).add( table );
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "Unable to read " + file.getAbsolutePath(), e );
            }
        }
    }
    
    public static void reInit()
    {
        tables.clear();
        mergedTables.clear();
        initialized = false;
        initDataTables();
    }
}
