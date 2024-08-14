package ru.biosoft.bsa.track.big;

import java.beans.PropertyDescriptor;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.jetbrains.bio.big.AutoSql;
import org.jetbrains.bio.big.BedEntry;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SiteImpl;
import ru.biosoft.bsa.StrandType;
import ru.biosoft.util.TextUtil;
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
            for(int i = 3; i < autoSql.getColumns().size(); i++)
            {
                org.jetbrains.bio.big.Column autoSqlColumn = autoSql.getColumns().get( i );
                autoSqlColumn.getType();
                autoSqlColumn.getName();
                autoSqlColumn.getDescription();
                Column col = new Column();
                col.pd = StaticDescriptor.create( autoSqlColumn.getName(), null, autoSqlColumn.getDescription(), null, false, true );
                col.type = convertAutoSqlTypeToJava(autoSqlColumn.getType());
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
        
        Sequence seq = origin.getChromosomeSequence( e.getChrom() );
        
        String[] parts = TextUtil.split( e.getRest(), '\t' );
        String id;
        if(idColumn != -1)
        {
            id = parts[idColumn];
        }else
            id = e.getChrom() + ":" + (e.getStart()+1) + "-" + e.getEnd(); 
        
        
        SiteImpl s = new SiteImpl( null, id, e.getStart() + 1, e.getEnd() - e.getStart(), StrandType.STRAND_NOT_KNOWN, seq );
        
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
        String rest = String.join( "\t", propValues );
        return new BedEntry( s.getOriginalSequence().getName(), s.getFrom()-1, s.getTo(), rest );
    }
    
   

}
