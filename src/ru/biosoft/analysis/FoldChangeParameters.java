package ru.biosoft.analysis;

import java.util.Map;

import org.apache.commons.lang.ArrayUtils;

import ru.biosoft.access.core.DataElementPath;

public class FoldChangeParameters extends UpDownIdentificationParameters
{
    private String type;

    private String outputLogarithmBase;
    
    private DataElementPath histogramOutput;

    public final static int AVERAGE_NONE = 0;
    public final static int AVERAGE_CONTROL = 1;
    public final static int AVERAGE_EXPERIMENT = 2;
    public final static int AVERAGE_ALL = 3;
    public final static int ONE_TO_ONE = 4;

    static Map<String, Integer> typeToCode = ArrayUtils.toMap(new Object[][] { {"Average all", AVERAGE_ALL},
            {"Average Control", AVERAGE_CONTROL}, {"Average Experiment", AVERAGE_EXPERIMENT},
            {"Each experiment to each control", AVERAGE_NONE}, {"One experiment to one control", ONE_TO_ONE}});

    public FoldChangeParameters()
    {
        setType( "Average all" );
        setInputLogarithmBase( "log2" );
        setOutputLogarithmBase( "log2" );
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        String oldValue = this.type;
        this.type = type;
        firePropertyChange("type", oldValue, type);
        firePropertyChange("*", null, null);
    }

    public int getTypeCode()
    {
        return typeToCode.get( type );
    }

    public String getOutputLogarithmBase()
    {
        return outputLogarithmBase;
    }

    public void setOutputLogarithmBase(String type)
    {
        String oldValue = this.outputLogarithmBase;
        this.outputLogarithmBase = type;
        firePropertyChange("outputLogarithmType", oldValue, outputLogarithmBase);
    }

    public Integer getOutputLogarithmBaseCode()
    {
        return Util.logBaseNameToCode.get( outputLogarithmBase );
    }

    public DataElementPath getHistogramOutput()
    {
        return histogramOutput;
    }

    public void setHistogramOutput(DataElementPath histogramOutput)
    {
        Object oldValue = this.histogramOutput;
        this.histogramOutput = histogramOutput;
        firePropertyChange("histogramOutput", oldValue, histogramOutput);
    }
    
    public boolean isHistogramHidden()
    {
        return getTypeCode() != AVERAGE_ALL;
    }

    public static String[] getTypeNames()
    {
        return typeToCode.keySet().toArray( new String[typeToCode.size()] );
    }
}
