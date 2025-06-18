package ru.biosoft.bsa.track.big;

import java.beans.PropertyDescriptor;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.bigbed.AutoSql;
import ru.biosoft.bigbed.BedEntry;
import ru.biosoft.bigbed.ChromInfo;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.StaticDescriptor;

public class BedEntryToSite implements BedEntryConverter<Site>
{
    private BigBedTrack<?> origin;
    private int idColumn = -1;
    
    public BedEntryToSite(BigBedTrack<?> origin, Properties props)
    {
        this.origin = origin;
        String idColumnStr = props.getProperty( BigBedTrack.PROP_UNIQUE_ID_COLUMN );
        if(idColumnStr != null)
            idColumn = Integer.parseInt( idColumnStr );
        
       

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
            	ru.biosoft.bigbed.AutoSql.Column autoSqlColumn = autoSql.columns.get( i );
                Column col = new Column();
                col.pd = StaticDescriptor.create( autoSqlColumn.name, null, autoSqlColumn.description, null, false, true );
                col.type = convertAutoSqlTypeToJava(autoSqlColumn.type);
                columns.add( col );
            }
        }
    }
    
    
    private Class<?> convertAutoSqlTypeToJava(String type)
    {
        if(type.equals( "int" ) || type.equals( "uint" ))
            return Integer.class;
        if(type.equals("float"))
            return Float.class;
        if(type.equals("string") || type.equals("lstring") || type.equals( "char" ) || type.matches( "char\\[[0-9]+\\]" ))
            return String.class;
        return String.class;
    }


    @Override
    public Site fromBedEntry(BedEntry e)
    {
        initColumns();//late init, so that BigBedTrack will be open
        
        ChromInfo chrInfo = origin.getChromInfo(e.chrId);
        String externalChrName = origin.internalToExternal(chrInfo.name);
        Sequence seq = origin.getChromosomeSequence( externalChrName );
        
        String dataStr = new String(e.data, StandardCharsets.UTF_8);
        String[] parts = TextUtil2.split( dataStr, '\t' );
        String id;
        if(idColumn != -1)
        {
            id = parts[idColumn];
        }else
            id = externalChrName + ":" + (e.start+1) + "-" + e.end; 
        
        
        SiteImpl s = new SiteImpl( null, id, e.start + 1, e.end - e.start, StrandType.STRAND_NOT_KNOWN, seq );
        
        DynamicPropertySet dps = s.getProperties();
        
        if(columns == null)
            for(int i = 0; i < parts.length; i++)
            {
                dps.add( new DynamicProperty( "prop" + (i+1), String.class, parts[i] ) );
            }
        else
        {
            for(int i = 0; i < columns.size(); i++)
            {
                Column col = columns.get( i );
                Object value = parts[i];
                if(col.type == Integer.class)
                    value = Integer.parseInt( value.toString() );
                else if(col.type == Float.class)
                    value = Float.parseFloat( value.toString() );
                dps.add( new DynamicProperty( col.pd, col.type, value ) );
            }
            if(columns.size() >= 3 && columns.get( 2 ).pd.getName().equals( "strand" ))
            {
                if(parts[2].equals( "+" ))
                    s.setStrand( StrandType.STRAND_PLUS );
                else if(parts[2].equals("-"))
                    s.setStrand( StrandType.STRAND_MINUS );
            }
        }
        return s;
    }

    @Override
    public BedEntry toBedEntry(Site s)
    {
        List<String> propValues = new ArrayList<>();
        for(DynamicProperty dp : s.getProperties())
        {
            Object val = dp.getValue();
            String propValue = val == null ? "" : val.toString();
            propValues.add( propValue );
        }
        String data = String.join( "\t", propValues );
        
        String externalChrName = s.getOriginalSequence().getName();
        ChromInfo chromInfo = origin.getChromInfo(externalChrName);
        BedEntry res = new BedEntry(chromInfo.id, s.getFrom()-1, s.getTo());
        if(data.length() > 0)
        	res.data = data.getBytes(StandardCharsets.UTF_8);        
        return res;
    }
    
   

}
