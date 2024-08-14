package ru.biosoft.analysis;

import java.util.LinkedHashMap;
import java.util.Map;

import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;

@SuppressWarnings ( "serial" )
public class SignTestParameters extends AbstractAnalysisParameters
{
    private DataElementPath inputTablePath, resultTablePath;
    private String sampleCol, adjMethod;
    private final static Map<String, String> adjMap;
    static
    {
        adjMap = new LinkedHashMap<>();
        adjMap.put( "none", "none" );
        adjMap.put( "Bonferroni 1936", "bonferroni" );
        adjMap.put( "Holm 1979", "holm" );
        adjMap.put( "Hochberg 1988", "hochberg" );
        adjMap.put( "Hommel 1988", "hommel" );
        adjMap.put( "Benjamini et Hochberg 1995", "BH" );
        adjMap.put( "Benjamini et Yekutieli 2001", "BY" );
    }
    public static String[] getAvailableMethods()
    {
        return adjMap.keySet().toArray( new String[0] );
    }


    @PropertyName ( "Input table" )
    @PropertyDescription ( "Table with sample under study and reference cohort" )
    public DataElementPath getInputTablePath()
    {
        return inputTablePath;
    }

    public void setInputTablePath(DataElementPath inputTablePath)
    {
        Object oldValue = this.inputTablePath;
        this.inputTablePath = inputTablePath;
        firePropertyChange( "inputTable", oldValue, inputTablePath );
    }

    @PropertyName ( "Result table" )
    @PropertyDescription ( "Results of the sign test" )
    public DataElementPath getResultTablePath()
    {
        return resultTablePath;
    }

    public void setResultTablePath(DataElementPath resultTablePath)
    {
        Object oldValue = this.resultTablePath;
        this.resultTablePath = resultTablePath;
        firePropertyChange( "resultTablePath", oldValue, resultTablePath );
    }


    @PropertyName ( "Sample column" )
    @PropertyDescription ( "Values to be compared against the cohort" )
    public String getSampleCol()
    {
        return sampleCol;
    }

    public void setSampleCol(String sampleCol)
    {
        Object oldValue = this.sampleCol;
        this.sampleCol = sampleCol;
        firePropertyChange( "sampleCol", oldValue, sampleCol );
    }
    
    @PropertyName ( "p-value adjustment" )
    @PropertyDescription ( "Method to adjust p-value" )
    public String getAdjMethod()
    {
        return adjMethod;
    }

    public void setAdjMethod(String adjMethod)
    {
        Object oldValue = this.adjMethod;
        this.adjMethod = adjMethod;
        firePropertyChange( "adjMethod", oldValue, adjMethod );
    }

    public String getAdjArg(String adjMethod)
    {
        return adjMap.get( adjMethod );
    }
}
