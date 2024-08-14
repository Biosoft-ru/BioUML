package ru.biosoft.analysis;

import java.beans.PropertyChangeEvent;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnGroup;
import ru.biosoft.workbench.editors.GenericComboBoxItem;
import ru.biosoft.workbench.editors.GenericEditorData;

@SuppressWarnings ( "serial" )
public class CorrelationAnalysisParameters extends MicroarrayAnalysisParameters
{

    public final static int COLUMNWISE = 0;
    public final static int ROWWISE = 1;

    public final static int COLUMNS = 0;
    public final static int MATRIX_CORRELATION = 1;
    public final static int MATRIX_PVALUE = 2;

    public final static int PEARSON = 0;
    public final static int SPEARMAN = 1;

    private GenericComboBoxItem dataSource;// = ROWWISE;
    private GenericComboBoxItem resultType;// = COLUMNS;
    private GenericComboBoxItem correlationType;// = PEARSON;

    static Map<String, Integer> dataSourceMap = ArrayUtils.toMap(new Object[][] { {"Columns", COLUMNWISE}, {"Rows", ROWWISE}});
    static Map<String, Integer> resultTypeMap = ArrayUtils.toMap(new Object[][] {{"Columns", COLUMNS}, {"Correlation matrix", MATRIX_CORRELATION}, {"P-value matrix", MATRIX_PVALUE} });
    static Map<String, Integer> correlationTypeMap = ArrayUtils.toMap(new Object[][] { {"Pearson", PEARSON},
            {"Spearman", SPEARMAN}});

    public CorrelationAnalysisParameters()
    {
        GenericEditorData.registerValues("dataSource", dataSourceMap.keySet().toArray(new String[dataSourceMap.size()]));
        GenericEditorData.registerValues("resultType", resultTypeMap.keySet().toArray(new String[resultTypeMap.size()]));
        GenericEditorData.registerValues("correlationType", correlationTypeMap.keySet().toArray(new String[correlationTypeMap.size()]));
        setCorrelationTypeName("Pearson correlation");
        setDataSourceName("Rows");
        setResultTypeName("Columns");
        getExperimentData().setNumerical(true);
        controlData.setNumerical(true);
    }

    private ColumnGroup controlData = new ColumnGroup(this);

    //Right group of columns from microarray
    public ColumnGroup getControlData()
    {
        return controlData;
    }
    public void setControlData(ColumnGroup columns)
    {
        ColumnGroup oldValue = controlData;
        if( columns != null )
            controlData = columns;
        else
            controlData = new ColumnGroup(this);
        controlData.setParent(this);
        firePropertyChange("controlData", oldValue, controlData);
    }

    public GenericComboBoxItem getDataSource()
    {
        return dataSource;
    }
    public void setDataSource(GenericComboBoxItem type)
    {
        GenericComboBoxItem oldValue = dataSource;
        dataSource = type;
        firePropertyChange("dataSource", oldValue, type);
    }
    public void setDataSourceName(String type)
    {
        setDataSource(new GenericComboBoxItem("dataSource", type));
    }
    public Integer getDataSourceCode()
    {
        return dataSourceMap.get(getDataSource().getValue().toString());
    }

    public GenericComboBoxItem getResultType()
    {
        return resultType;
    }
    public void setResultType(GenericComboBoxItem type)
    {
        GenericComboBoxItem oldValue = resultType;
        resultType = type;
        firePropertyChange("resultType", oldValue, type);
    }

    public void setResultTypeName(String type)
    {
        setResultType(new GenericComboBoxItem("resultType", type));
    }
    public Integer getResultTypeCode()
    {
        return resultTypeMap.get(getResultType().getValue().toString());
    }

    public GenericComboBoxItem getCorrelationType()
    {
        return correlationType;
    }
    public void setCorrelationType(GenericComboBoxItem type)
    {
        GenericComboBoxItem oldValue = correlationType;
        correlationType = type;
        firePropertyChange("correlationType", oldValue, type);
    }

    public void setCorrelationTypeName(String type)
    {
        setCorrelationType(new GenericComboBoxItem("correlationType", type));
    }
    public Integer getCorrelationTypeCode()
    {
        return correlationTypeMap.get(getCorrelationType().getValue().toString());
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[] {"experimentData/tablePath", "controlData/tablePath"};
    }


    public TableDataCollection getControl()
    {
        return getControlData().getTable();
    }

    public void setControl(TableDataCollection table)
    {
        getControlData().setTable(table);
    }

    @Override
    public void read(Properties properties, String prefix)
    {
        super.read(properties, prefix);
        String rightGroupStr = properties.getProperty(prefix + "controlData");
        if( rightGroupStr != null )
        {
            controlData = ColumnGroup.readObject(this, rightGroupStr);
        }
    }
    
    @Override
    public void propertyChange(PropertyChangeEvent event)
    {
        //when set table for experiment it will be automatically set as control
        if( event.getSource() == getExperimentData() && event.getPropertyName().equals("table") )
        {
            this.setControl(getExperimentData().getTable());
        }
    }

}
