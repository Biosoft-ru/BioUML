package ru.biosoft.analysis;

import java.beans.PropertyChangeEvent;
import java.util.Map;
import java.util.Properties;

import javax.annotation.Nonnull;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnGroup;

@SuppressWarnings ( "serial" )
@PropertyName ( "Up and down identification" )
@PropertyDescription ( "Up and down identification" )
public class UpDownIdentificationParameters extends MicroarrayAnalysisParameters
{
    private ColumnGroup controlData = new ColumnGroup(this);
    private String method;
    private String outputType;
    protected String inputLogarithmBase;

    final public static int UP_REGULATED = 1;
    final public static int DOWN_REGULATED = 2;
    final public static int UP_AND_DOWN_REGULATED = UP_REGULATED | DOWN_REGULATED;
    final public static int NON_CHANGED = 0;
    
    public final static int STUDENT = 0;
    public final static int WILCOXON = 1;
    public final static int LEHMAN_ROSENBLATT = 2;
    public final static int KOLMOGOROV_SMIRNOV = 3;

    static Map<String, Integer> outputTypeNameToCode = ArrayUtils.toMap(new Object[][] { {"Up and down regulated", UP_AND_DOWN_REGULATED},
            {"Up regulated", UP_REGULATED}, {"Down regulated", DOWN_REGULATED}, {"Non-changed", NON_CHANGED}});

    //expert available methods
    static Map<String, Integer> methodNameToCode = ArrayUtils.toMap(new Object[][] { {"Student's t-test", STUDENT}, {"Wilcoxon", WILCOXON},
            {"Lehman-Rosenblatt", LEHMAN_ROSENBLATT}, {"Kolmogorov-Smirnov", KOLMOGOROV_SMIRNOV}});

    //simple available methods
    static Map<String, Integer> simpleMethodNameToCode = ArrayUtils.toMap(new Object[][] { {"Student's t-test", STUDENT},
            {"Wilcoxon", WILCOXON}});

    public UpDownIdentificationParameters()
    {
        setMethod( "Student's t-test" );
        setInputLogarithmBase( "non-logarithmic" );
        setOutputType("Up and down regulated");
        getExperimentData().setNumerical(true);
        controlData.setNumerical(true);
    }

    @Override
    public void setExpertMode(boolean flag)
    {
        super.setExpertMode(flag);
        if( !flag )
        {
            if( getMethodCode() == LEHMAN_ROSENBLATT || getMethodCode() == KOLMOGOROV_SMIRNOV )
                setMethod( "Student's t-test" );
        }
    }

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
        controlData.setNumerical(true);
        firePropertyChange("controlData", oldValue, controlData);
    }

    public String getMethod()
    {
        return method;
    }

    public void setMethod(String method)
    {
        String oldValue = this.method;
        this.method = method;
        firePropertyChange("method", oldValue, method);
    }
    public Integer getMethodCode()
    {
        return methodNameToCode.get( method );
    }

    public String getOutputType()
    {
        return outputType;
    }

    public Integer getOutputTypeCode()
    {
        return outputTypeNameToCode.get( outputType );
    }

    public void setOutputType(String outputType)
    {
        String oldValue = this.outputType;
        this.outputType = outputType;
        firePropertyChange( "outputType", oldValue, outputType );
    }

    @Override
    public @Nonnull String[] getInputNames()
    {
        return new String[] {"experimentData/tablePath", "controlData/tablePath"};
    }

    public TableDataCollection getControl()
    {
        return getControlData() == null ? null : getControlData().getTable();
    }

    public void setControl(TableDataCollection table)
    {
        getControlData().setTable(table);
    }

    public void setInputLogarithmBase(String inputLogarithmBase)
    {
        String oldValue = this.inputLogarithmBase;
        this.inputLogarithmBase = inputLogarithmBase;
        firePropertyChange("inputLogarithmType", oldValue, inputLogarithmBase);
    }

    public Integer getInputLogarithmBaseCode()
    {
        return Util.logBaseNameToCode.get( inputLogarithmBase );
    }

    public String getInputLogarithmBase()
    {
        return inputLogarithmBase;
    }

    public String[] getMethodNames()
    {
        if( !isExpertMode() )
            return simpleMethodNameToCode.keySet().toArray( new String[simpleMethodNameToCode.size()] );
        else
            return methodNameToCode.keySet().toArray( new String[methodNameToCode.size()] );
    }

    public static String[] getOutputTypes()
    {
        return outputTypeNameToCode.keySet().toArray( new String[outputTypeNameToCode.size()] );
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
            TableDataCollection table = getExperimentData().getTable();
            if(table != null)
            {
                if( getControl() == null )
                    this.setControl(table);
                AnalysisParameters prevParams = AnalysisParametersFactory.readPersistent(table);
                if( prevParams instanceof NormalizationParameters )
                {
                    setInputLogarithmBase( ( (NormalizationParameters)prevParams ).getOutputLogarithmBase().getValue().toString() );
                }
            }
        }
    }
}
