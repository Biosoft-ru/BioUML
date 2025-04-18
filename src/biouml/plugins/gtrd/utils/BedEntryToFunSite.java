package biouml.plugins.gtrd.utils;

import java.beans.PropertyDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import biouml.plugins.machinelearning.utils.DataMatrix;
import ru.biosoft.bigbed.AutoSql;
import ru.biosoft.bigbed.BedEntry;
import ru.biosoft.bigbed.ChromInfo;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.track.big.BedEntryConverter;
import ru.biosoft.bsa.track.big.BigBedTrack;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.StaticDescriptor;

public class BedEntryToFunSite implements BedEntryConverter<FunSite>
{
    private BigBedTrack<?> origin;
    
    public BedEntryToFunSite(BigBedTrack<?> origin, Properties props) 
    {
        this.origin = origin;
    }
    
    static class Column
    {
        PropertyDescriptor pd;
        Class<?> type;
    }
    private List<Column> columns;
    private void initColumns()
    {
        if(columns != null)
            return;
        AutoSql autoSql = origin.getBBFile().getAutoSql();
        if(autoSql != null)
        {
            columns = new ArrayList<>();
            for(int i = 3; i < autoSql.columns.size(); i++)
            {
                AutoSql.Column autoSqlColumn = autoSql.columns.get( i );
                Column col = new Column();
                col.pd = StaticDescriptor.create( autoSqlColumn.name, null, autoSqlColumn.description, null, false, true );
                col.type = convertAutoSqlTypeToJava( autoSqlColumn.type );
                columns.add( col );
            }
        }
    }
    
    private Class<?> convertAutoSqlTypeToJava(String type)
    {
        if(type.equals("float") || type.equals( "int" ) || type.equals( "uint" ))
            return Double.class;
        if(type.equals("string") || type.equals("lstring") || type.equals( "char" ) || type.matches( "char\\[[0-9]+\\]" ))
            return String.class;
        return String.class;
    }
    
    @Override
    public FunSite fromBedEntry(BedEntry e)
    {
    	initColumns(); //late init, so that BigBedTrack will be open
    	
        String[] parts = TextUtil2.split( e.getRest(), '\t' );

        Interval coordinates = new Interval(e.start, e.end);
        ChromInfo chrInfo = origin.getChromInfo(e.chrId);
        String chrName = origin.internalToExternal(chrInfo.name);
        Sequence seq = origin.getChromosomeSequence( chrName );
        
        double[] propertyValues = new double[parts.length];
        String[] propertyNames = new String[parts.length];
        if(columns == null)
        {
        	propertyValues = new double[parts.length];
            propertyNames = new String[parts.length];
        	for(int i = 0; i < parts.length; i++)
            {
        		propertyNames[i] = "prop" + (i+1);
            }
        }
        else
        {
        	propertyValues = new double[columns.size()];
            propertyNames = new String[columns.size()];
            
        	for(int i = 0; i < columns.size(); i++)
            {
                Column col = columns.get( i );
                Object value = parts[i];
                if(col.type == Double.class)
                    value = Double.parseDouble( value.toString() );
                else if(col.type == String.class)
                    {
                    //TODO: support String properties in FunSites	
                    }
                propertyNames[i] = col.pd.getName();
                propertyValues[i] = (double) value;
            }
        	
            if(columns.size() >= 3 && columns.get( 2 ).pd.getName().equals( "strand" ))
            {
                //TODO: add strand
            }
        }
        
        DataMatrix dataMatrix = new DataMatrix(null, propertyNames, new double[][]{propertyValues});
        FunSite fs = new FunSite(chrName, coordinates, 0, dataMatrix, seq);

        return fs;
    }

    @Override
    public BedEntry toBedEntry(FunSite fs)
    {
        List<String> propValues = new ArrayList<>();
        Object[] values = fs.getObjects();
        if( values != null )
        {
        	for( int i = 0; i < values.length; i++ )
        	{
        		Object val = values[i];
                String propValue = val == null ? "" : val.toString();
                propValues.add( propValue );
        	}
        }

        String rest = String.join( "\t", propValues );
        
        ChromInfo chrInfo = origin.getChromInfo(fs.getChromosomeName());
        BedEntry e = new BedEntry(chrInfo.id, fs.getStartPosition()-1, fs.getFinishPosition());
        e.data = rest.getBytes(StandardCharsets.UTF_8);
        return e;
        
    }

}
