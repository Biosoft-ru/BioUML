package biouml.plugins.microarray;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import ru.biosoft.access.support.DividedLineTagCommand;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableColumn;
import ru.biosoft.table.TableDataCollectionUtils;
import biouml.standard.type.TableExperiment;

public class MicroarrayTagCommand extends DividedLineTagCommand<TableExperiment>
{
    public static final String ID_TAG = "ID";
    public static final String DESCRIPTION_TAG = "DE";
    public static final String PARAMETERS_DESCRIPTION_TAG = "PD";
    public static final String PARAMETERS_TAG = "PR";

    private StringBuffer mtcLine = null;

    @Override
    public String getTaggedValue()
    {
        StringBuffer value = new StringBuffer(tag);
        value.append('\t');
        TableExperiment me = transformer.getProcessedObject();

        if( ID_TAG.equals(tag) )
        {
            String title = me.getTitle();
            if( null == title )
                title = "";
            value.append(title);
        }
        else if( DESCRIPTION_TAG.equals(tag) )
        {
            String description = me.getDescription();
            if( null == description )
                description = "";
            value.append(description);
        }
        else if( PARAMETERS_DESCRIPTION_TAG.equals(tag) )
        {
            for (TableColumn col : me.getTableData().getColumnModel())
            {
                value.append(col.getName());
                value.append(' ');
            }
        }
        else if( PARAMETERS_TAG.equals(tag) )
        {
            int j = 0;
            
            for (Iterator<RowDataElement> iterator = me.getTableData().iterator(); iterator.hasNext();)
            {
                RowDataElement rowDataElement = iterator.next();
                Object attributes[] = rowDataElement.getValues();
                if( 0 == attributes.length )
                    continue;
                
                String gene = rowDataElement.getName();

                value.append(gene);
                value.append(' ');
                value.append(attributes[0]);
                for( int i = 1; i < attributes.length; i++ )
                {
                    value.append(' ');
                    value.append(attributes[i]);
                }

                j++;
                if( me.getTableData().getSize() > j )
                {
                    value.append('\n');
                    value.append(tag);
                    value.append('\t');
                }
            }
        }

        return value.toString();
    }

    @Override
    public String getTaggedValue(String value)
    {
        String ret = getTaggedValue();
        if( !ret.endsWith("\n") )
            ret += "\n";
        return ret;
    }

    public MicroarrayTagCommand(String tag, MicroarrayEntryTransformer transformer)
    {
        super(tag, transformer);
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

        TableExperiment me = transformer.getProcessedObject();

        if( ID_TAG.equals(tag) )
        {
            me.setTitle(lineString);
        }
        else if( DESCRIPTION_TAG.equals(tag) )
        {
            me.setDescription(lineString);
        }
        else if( PARAMETERS_DESCRIPTION_TAG.equals(tag) )
        {
            try
            {
                parseDescriptionLine(lineString, me);
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
        }
        else if( PARAMETERS_TAG.equals(tag) )
        {
            try
            {
                String linesArray[] = lineString.split("\\n");
                for( String oneOfTheLines : linesArray )
                {
                    parseParameterLine(oneOfTheLines, me);
                }
            }
            catch( Exception e )
            {
                e.printStackTrace();
            }
        }
    }

    @Override
    public void startTag(String tag)
    {
        mtcLine = null;
    }

    protected void parseParameterLine(String line, TableExperiment me)
    {
        if( me != null )
        {
            String paramsArray[] = line.split("\\s");
            if( paramsArray.length > 1 )
            {
                String key = paramsArray[0];
                List<Object> values = new ArrayList<>();
                for( int i = 1; i < paramsArray.length; i++ )
                {
                    try
                    {
                        values.add(Double.parseDouble(paramsArray[i]));
                    }
                    catch( Exception e )
                    {
                        values.add(paramsArray[i]);
                    }
                }
                TableDataCollectionUtils.addRow(me.getTableData(), key, values.toArray(new Object[values.size()]));
            }
        }
    }

    protected void parseDescriptionLine(String line, TableExperiment me) throws Exception
    {
        if( me != null )
        {
            for( String param : line.split("\\s") )
            {
                me.getTableData().getColumnModel().addColumn(param, String.class);
            }
        }
    }
}
