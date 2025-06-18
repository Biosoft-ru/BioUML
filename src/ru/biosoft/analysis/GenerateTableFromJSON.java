package ru.biosoft.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.json.JSONObject;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.TextDataElement;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;

public class GenerateTableFromJSON extends AnalysisMethodSupport<GenerateTableFromJSONParameters>
{
    public GenerateTableFromJSON(DataCollection origin, String name)
    {
        super( origin, name, new GenerateTableFromJSONParameters() );
    }

    @Override
    public Object justAnalyzeAndPut()
    {
        try
        {
            TableDataCollection result = TableDataCollectionUtils.createTableDataCollection( parameters.getOutput() );
            TextDataElement tde = parameters.getInput().getDataElement( TextDataElement.class );
            String str = tde.getContent();
            JSONObject object = new JSONObject(str);
            JSONObject processes = object.getJSONObject(  "processes" );
            JSONObject points = object.getJSONObject(  "points" );
            
            String[] processNames = JSONObject.getNames( processes );
            String[] pointNames = JSONObject.getNames( points );
            
            //TODO: generalize method
            List<String> columnNames = new ArrayList<>();
            columnNames.add( "process" );
            columnNames.add( "point" );
            //first read all possible properties from all elements
            Map<String, Integer> properties = new HashMap<>();   
//            properties.put( "process", 0 );
//            properties.put( "point", 1 );
            int index = 2;
            for( String processName : processNames )
            {
                JSONObject process = processes.getJSONObject( processName );
                for( String name : JSONObject.getNames( process ) )
                {
                    if( !properties.containsKey( name ) )
                    {
                        properties.put( name, index++ );
                        columnNames.add( name );
                    }
                }
            }

            for( String pointName : pointNames )
            {
                JSONObject point = points.getJSONObject( pointName );
                for( String name : JSONObject.getNames( point ) )
                {
                    if( !properties.containsKey( name ) )
                    {
                        properties.put( name, index++ );
                        columnNames.add( name );
                    }
                }
            }
            
            for (String property: columnNames)
                result.getColumnModel().addColumn( property, DataType.Text );                
            
            int autoIndex = 0;
            for( String processName : processNames )
            {
                for( String pointName : pointNames )
                {
                    Object[] values = new Object[properties.size() + 2];
                    values[0] = processName;
                    JSONObject process = processes.getJSONObject( processName );
                    for( String name : JSONObject.getNames( process ) )
                    {
                        int i = properties.get( name );
                        values[i] = process.get( name );
                    }

                    values[1] = pointName;
                    String rowname = String.valueOf( autoIndex++ );
                    JSONObject point = points.getJSONObject( pointName );

                    for( String name : JSONObject.getNames( point ) )
                    {
                        int i = properties.get( name );
                        values[i] = point.get( name );
                    }
                    TableDataCollectionUtils.addRow( result, rowname, values );
                }
            }

            return result;
        }
        catch( Exception ex )
        {
            log.log( java.util.logging.Level.SEVERE, "Error during tabe generation: " + ex.getMessage(), ex );
            return null;
        }

    }
}