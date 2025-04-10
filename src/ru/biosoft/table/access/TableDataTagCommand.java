package ru.biosoft.table.access;

import java.util.logging.Level;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.StringTokenizer;

import one.util.streamex.StreamEx;

import java.util.logging.Logger;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.support.DividedLineTagCommand;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.Sample;
import ru.biosoft.table.SampleGroup;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.TextUtil2;

import com.developmentontheedge.beans.DynamicProperty;

public class TableDataTagCommand extends DividedLineTagCommand<TableDataCollection>
{
    protected Logger log = Logger.getLogger(TableDataTagCommand.class.getName());

    public static final String ID_TAG = "ID";
    public static final String ORGANISM_SPECIES_TAG = "OS";
    public static final String PLATFORM_TAG = "PLATFORM";
    public static final String EXPERIMENT_TITLE_TAG = "TITLE";
    public static final String DESCRIPTION_TAG = "DESCRIPTION";
    public static final String INFO_TAG = "INFO";
    public static final String SAMPLES_TAG = "SAMPLES";
    public static final String COLUMN_EXPRESSIONS_TAG = "COLUMN_EXPRESSIONS";
    public static final String PROPERTIES_TAG = "PROPERTIES";
    public static final String GROUPS_TAG = "GROUPS";
    public static final String DATA_TAG = "DATA";

    private StringBuffer mtcLine = null;

    @Override
    public String getTaggedValue()
    {
        StringBuffer value = new StringBuffer(tag);
        TableDataCollection me = transformer.getProcessedObject();

        String appendix = null;
        boolean tagFound = true;

        if( ID_TAG.equals(tag) )
            appendix = me.getName();
        else if( ORGANISM_SPECIES_TAG.equals(tag) )
        {
        }
        //            appendix = me.getSpecies();
        else if( PLATFORM_TAG.equals(tag) )
        {
        }
        //            appendix = me.getPlatform();
        else if( EXPERIMENT_TITLE_TAG.equals(tag) )
        {
            //            appendix = me.getTitle();

            // Not obligatory element. We won't serialize it if it isn't
            // present.
            //if( null == appendix )
                return null;
        }
        else if( DESCRIPTION_TAG.equals(tag) )
        {
            appendix = me.getDescription();

            // Not obligatory element. We won't serialize it if it isn't
            // present.
            if( null == appendix )
                return null;
        }
        else
            tagFound = false;

        if( tagFound )
        {
            // Obligatory elements. We will serialize empty strings if they're
            // not present.
            if( null == appendix )
                appendix = "";
            value.append('\t');
            value.append(appendix);

            return value.toString();
        }

        if( SAMPLES_TAG.equals(tag) )
        {
            for( int i = 0; i < me.getColumnModel().getColumnCount(); i++ )
            {
                TableColumn col = me.getColumnModel().getColumn(i);
                value.append("\n#");
                value.append(i + 1);
                value.append('\t');
                value.append(col.getName());
                value.append("; ");
                value.append(col.getType().toString());
                value.append("; ");
                value.append(col.getNature().toString());
                value.append("; ");
                if( null != col.getShortDescription() )
                {
                    value.append(' ');
                    value.append(col.getShortDescription());
                }
            }
        }
        else if( COLUMN_EXPRESSIONS_TAG.equals(tag) )
        {
            for( TableColumn col : me.getColumnModel() )
            {
                if( !col.isExpressionEmpty() )
                {
                    value.append("\n");
                    value.append(col.getName());
                    value.append(':');
                    String expr = col.getExpression();
                    value.append(expr == null ? "" : expr);
                }
            }
        }
        else if( PROPERTIES_TAG.equals(tag) )
        {
            List<String> properties = getPropertyList();
            DataCollection<Sample> samples = me.getSamples();
            for( String propertyName : properties )
            {
                value.append('\n');
                value.append(propertyName);
                value.append('=');
                Iterator<Sample> it = samples.iterator();
                while(it.hasNext())
                {
                    Sample ms = it.next();
                    Object samplePropertyValue = ms.getAttributes().getValue(propertyName);
                    String toAppend = "";
                    if( null != samplePropertyValue )
                        toAppend = String.valueOf(samplePropertyValue);
                    value.append(toAppend);
                    if( it.hasNext() )
                        value.append(';');
                }
            }
        }
        else if( GROUPS_TAG.equals(tag) )
        {
            for( SampleGroup group : me.getGroups() )
            {
                value.append('\n');
                value.append(group.getName());
                value.append('\t');
                if( null == group.getDescription() )
                    group.setDescription("");
                value.append(group.getDescription().replace("=", ""));
                value.append('=');
                if( null == group.getPattern() )
                    group.setPattern("");
                value.append(group.getPattern());
            }
        }
        else if( INFO_TAG.equals(tag) )
        {
            Properties properties = me.getInfo().getProperties();
            for( Entry<Object, Object> entry : properties.entrySet() )
            {
                value.append('\n');
                value.append(entry.getKey());
                value.append('=');
                value.append(entry.getValue());
            }
        }
        else if( DATA_TAG.equals(tag) )
        {
            for( RowDataElement rowDataElement: me )
            {
                value.append('\n');
                writeDataLine(rowDataElement, value);
            }
        }

        return value.toString();
    }

    public static void writeDataLine(RowDataElement rde, StringBuffer buffer)
    {
        buffer.append( StreamEx.of( rde.getValues() ).prepend( rde.getName() ).joining( "\t" ) );
    }

    private List<String> getPropertyList()
    {
        TableDataCollection me = transformer.getProcessedObject();
        List<String> result = new ArrayList<>();
        for(Sample sample : me.getSamples())
        {
            Iterator<String> iter2 = sample.getAttributes().nameIterator();
            while( iter2.hasNext() )
            {
                result.add(iter2.next());
            }
            break;
        }
        return result;
    }

    @Override
    public String getTaggedValue(String value)
    {
        String ret = getTaggedValue();
        if( !ret.endsWith("\n") )
            ret += "\n";
        return ret;
    }

    public TableDataTagCommand(String tag, TableDataEntryTransformer transformer)
    {
        super(tag, transformer);
    }

    @Override
    public void addValue(String value)
    {
        if( DATA_TAG.equals(tag) )
        {
            //process DATA directly
            TableDataCollection me = transformer.getProcessedObject();
            parseParameterLine(value, me);
        }
        else
        {
            super.addValue(value);
        }
    }

    @Override
    public void addLine(String line)
    {
        if( null == mtcLine )
        {
            mtcLine = new StringBuffer();
        }
        else
        {
            mtcLine.append('\n');
        }

        mtcLine.append(line);
    }

    @Override
    public void endTag(String tag)
    {
        if( null == mtcLine )
            return;

        String lineString = mtcLine.toString();

        if( 0 == lineString.length() )
            return;

        TableDataCollection me = transformer.getProcessedObject();

        if( ID_TAG.equals(tag) )
        {
            // me.setName(lineString);
        }
        else if( ORGANISM_SPECIES_TAG.equals(tag) )
        {
            //            me.setSpecies(lineString);
        }
        else if( PLATFORM_TAG.equals(tag) )
        {
            //            me.setPlatform(lineString);
        }
        else if( EXPERIMENT_TITLE_TAG.equals(tag) )
        {
            //            me.setTitle(lineString);
        }
        else if( DESCRIPTION_TAG.equals(tag) )
        {
            me.getInfo().setDescription(lineString);
        }
        else if( SAMPLES_TAG.equals(tag) )
        {
            parseSamplesLine(lineString, me);
        }
        else if( COLUMN_EXPRESSIONS_TAG.equals(tag) )
        {
            parseColumnExpressionsLine(lineString, me);
        }
        else if( PROPERTIES_TAG.equals(tag) )
        {
            parsePropertiesLine(lineString, me);
        }
        else if( GROUPS_TAG.equals(tag) )
        {
            parseGroupsLine(lineString, me);
        }
        else if( INFO_TAG.equals(tag) )
        {
            parseInfoLine(lineString, me);
        }
    }

    private void parseGroupsLine(String line, TableDataCollection me)
    {
        if( me != null )
        {
            String paramsArray[] = line.split("\\n");
            for( String param : paramsArray )
            {
                String preGroup[] = TextUtil2.split( param, '=' );
                if( preGroup.length > 1 )
                {
                    int index = preGroup[0].indexOf(' ');
                    if( 0 >= index )
                        index = preGroup[0].indexOf('\t');
                    String name = null;
                    String description = null;

                    if( 0 >= index )
                    {
                        name = preGroup[0];
                    }
                    else
                    {
                        name = preGroup[0].substring(0, index);
                        description = preGroup[0].substring(index + 1);
                    }

                    SampleGroup sg = new SampleGroup(me.getGroups(), name);
                    sg.setDescription(description);
                    sg.setPattern(preGroup[1]);
                    try
                    {
                        me.getGroups().put(sg);
                    }
                    catch( Exception e )
                    {
                        log.log(Level.SEVERE, "Can not add group", e);
                    }
                }
            }
        }
    }

    private void parseInfoLine(String line, TableDataCollection me)
    {
        if( me != null )
        {
            Properties properties = me.getInfo().getProperties();
            String paramsArray[] = line.split("\\n");
            for( String param : paramsArray )
            {
                String preGroup[] = TextUtil2.split( param, '=' );
                if( preGroup.length > 1 )
                {
                    String value = preGroup[1].replaceAll("\\\\t", "\t");
                    properties.put(preGroup[0], value);
                }
            }
        }
    }

    private void parsePropertiesLine(String line, TableDataCollection me)
    {
        if( me != null )
        {
            DataCollection<Sample> samples = me.getSamples();
            String paramsArray[] = line.split("\\n");
            List<String> list = samples.getNameList();
            for( String param : paramsArray )
            {
                String preProp[] = TextUtil2.split( param, '=' );
                if( preProp.length == 2 )
                {
                    String name = preProp[0];
                    String values[] = TextUtil2.split( preProp[1], ';' );
                    for( int j = 0; j < values.length; j++ )
                    {
                        Sample ms = null;
                        try
                        {
                            ms = samples.get(list.get(j));
                            ms.getAttributes().add(new DynamicProperty(name, String.class, values[j]));
                        }
                        catch( Exception e )
                        {
                            log.log(Level.SEVERE, "Can not add attribute to sample", e);
                        }
                    }
                }
            }
        }
    }

    @Override
    public void startTag(String tag)
    {
        mtcLine = null;
    }

    protected void parseParameterLine(String line, TableDataCollection me)
    {
        if( me != null )
        {
            int size = me.getColumnModel().getColumnCount();
            Object[] values = new Object[size];
            StringTokenizer st = new StringTokenizer(line, " \t");
            int i = 0;
            if( st.hasMoreTokens() )
            {
                String key = st.nextToken();

                while( ( st.hasMoreTokens() ) && ( i < size ) )
                {
                    String token = st.nextToken();
                    try
                    {
                        values[i] = me.getColumnModel().getColumn(i).getType().convertValue(token);
                    }
                    catch( Exception e )
                    {
                        values[i] = token; // just store it to give user possibility to fix it later
                    }
                    i++;
                }
                TableDataCollectionUtils.addRow(me, key, values);
            }
        }
    }

    protected void parseSamplesLine(String line, TableDataCollection me)
    {
        if( me != null )
        {
            String paramsArray[] = line.split("\\n");
            for( String param : paramsArray )
            {
                String preSample = param.split("\\t")[1];
                String sample[] = preSample.split("; ");
                if( sample.length > 3 )
                {
                    DataType type = DataType.fromString(sample[1]);
                    TableColumn.Nature nature = TableColumn.Nature.valueOf(sample[2]);

                    Sample value = null;
                    if( nature == TableColumn.Nature.SAMPLE )
                    {
                        value = new Sample(null, sample[0]);
                    }

                    TableColumn col;
                    if( !sample[3].trim().isEmpty() )
                        col = me.getColumnModel().addColumn(sample[0], sample[0], sample[3].trim(), type.getType(), "");
                    else
                        col = me.getColumnModel().addColumn(sample[0], type.getType());

                    // modify last added column
                    col.setNature(nature);
                    col.setSample(value);
                }
            }
            me.getSamples();
        }
    }

    protected void parseColumnExpressionsLine(String line, TableDataCollection me)
    {
        if( me != null )
        {
            String expressionLinesArray[] = line.split("\\n");
            for( String exprLine : expressionLinesArray )
            {
                int colonIdx = exprLine.indexOf(':');
                if( colonIdx < 0 )
                {
                    log.log(Level.SEVERE, "Incorrect expression section");
                    return;
                }

                String columnName = exprLine.substring(0, colonIdx);
                String expression = exprLine.substring(colonIdx + 1);

                me.getColumnModel().getColumn(columnName).setExpression(expression);
            }
        }
    }

    protected TableColumn findByName(List<TableColumn> columnInfos, String columnName)
    {
        for( TableColumn dp : columnInfos )
        {
            if( dp.getName().equals(columnName) )
            {
                return dp;
            }
        }

        return null;
    }
}
