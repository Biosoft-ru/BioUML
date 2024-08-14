package ru.biosoft.table;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataCollectionConfigConstants;
import ru.biosoft.access.biohub.BioHubSupport;
import ru.biosoft.access.biohub.Element;
import ru.biosoft.access.biohub.TargetOptions;

import ru.biosoft.jobcontrol.FunctionJobControl;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Hub which matches table row IDs to the data in given column (comma-separated)
 * @author lan
 */
public class TableColumnMatchingHub extends BioHubSupport
{
    private final TableDataCollection data;
    private final String columnName;

    public TableColumnMatchingHub(TableDataCollection data, String columnName)
    {
        super(createProperties(data, columnName));
        this.data = data;
        this.columnName = columnName;
    }

    private static Properties createProperties(TableDataCollection data, String columnName)
    {
        Properties properties = new Properties();
        properties.put(DataCollectionConfigConstants.NAME_PROPERTY, data.getName()+":"+columnName);
        return properties;
    }

    @Override
    public int getPriority(TargetOptions dbOptions)
    {
        return 0;
    }

    @Override
    public Element[] getReference(Element startElement, TargetOptions dbOptions, String[] relationTypes, int maxLength, int direction)
    {
        return null;
    }

    @Override
    public Map<String, String[]> getReferences(String[] inputList, Properties input, Properties output, FunctionJobControl jobControl)
    {
        int idx = data.getColumnModel().optColumnIndex(columnName);
        if(idx == -1) return null;
        Map<String, String[]> result = new HashMap<>();
        for(int i=0; i<inputList.length; i++)
        {
            try
            {
                RowDataElement row = data.get(inputList[i]);
                if(row == null) continue;
                Object val = row.getValues()[idx];
                String[] outputList;
                if(val instanceof StringSet)
                    outputList = ((StringSet)val).toStringArray();
                else
                    outputList = TableDataCollectionUtils.splitIds(val.toString());
                result.put(inputList[i], outputList);
                jobControl.setPreparedness(i*100/inputList.length);
                if(jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST) return null;
            }
            catch( Exception e )
            {
            }
        }
        return result;
    }
}
