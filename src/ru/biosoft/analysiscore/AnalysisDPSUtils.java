package ru.biosoft.analysiscore;

import java.util.logging.Level;
import java.util.logging.Logger;

import com.developmentontheedge.beans.DPSProperties;

import ru.biosoft.util.DPSUtils;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;

/**
 * Utility functions for DPS parameters support
 */
public class AnalysisDPSUtils
{
    protected static final Logger log = Logger.getLogger( AnalysisDPSUtils.class.getName() );

    public static final String PARAMETER_ANALYSIS_FULLNAME = "analysisName";

    /**
     * Write {@link AnalysisParameters} as {@link DynamicPropertySet}.
     */
    public static void writeParametersToNodeAttributes(String analysisName, AnalysisParameters parameters, DynamicPropertySet attributes)
    {
        try
        {
            if( analysisName != null )
                attributes.add(new DynamicProperty(PARAMETER_ANALYSIS_FULLNAME, String.class, analysisName));
            parameters.write(new DPSProperties(attributes), DPSUtils.PARAMETER_ANALYSIS_PARAMETER + ".");
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not write paramenets", e);
        }
    }

    /**
     * Get {@link AnalysisParameters} as {@link DynamicPropertySet}
     */
    public static DynamicPropertySet getParametersAsDynamicPropertySet(AnalysisParameters parameters)
    {
        try
        {
            DynamicPropertySet result = new DynamicPropertySetAsMap();
            parameters.write(new DPSProperties(result), "");
            return result;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "Can not write paramenets", e);
        }
        return null;
    }

    /**
     * Restore {@link AnalysisParameters} from {@link DynamicPropertySet}.
     */
    public static AnalysisParameters readParametersFromAttributes(DynamicPropertySet attributes)
    {
        AnalysisMethod analysisMethod = getAnalysisMethodByNode(attributes);
        if( analysisMethod != null )
        {
            AnalysisParameters result = analysisMethod.getParameters();
            fillAnalysisParameters(result, attributes);
            return result;
        }
        return null;
    }

    /**
     * Fill {@link AnalysisParameters} with values from {@link DynamicPropertySet}
     */
    public static void fillAnalysisParameters(AnalysisParameters parameters, DynamicPropertySet attributes)
    {
        parameters.read(new DPSProperties(attributes), DPSUtils.PARAMETER_ANALYSIS_PARAMETER + ".");
    }

    /**
     * Get analysis method by {@link DynamicPropertySet}
     */
    public static AnalysisMethod getAnalysisMethodByNode(DynamicPropertySet attributes)
    {
        AnalysisMethod result = null;
        Object analysisName = attributes.getValue(PARAMETER_ANALYSIS_FULLNAME);
        if( analysisName != null )
        {
            result = AnalysisMethodRegistry.getAnalysisMethod(analysisName.toString());
        }
        return result;
    }
}
