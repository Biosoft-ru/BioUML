package ru.biosoft.plugins.docker;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementSupport;

import ru.biosoft.analysiscore.AnalysisMethod;
import ru.biosoft.analysiscore.AnalysisMethodInfo;

import com.developmentontheedge.beans.DynamicProperty;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.annot.PropertyName;

import ru.biosoft.access.core.ClassIcon;

@ClassIcon( "resources/logo-cwl.png" )
@PropertyName ( "CWL Dockered method" )
public class CwlScriptDataElement extends AnalysisMethodInfo
{
    private File cwlFile;
    protected String title;

    private DynamicPropertySet parameterSet;
    private DynamicPropertySet outputSet;
    
    protected String descriptionHTML = null;

    public CwlScriptDataElement(String name, String title, String description, DataCollection parent, File file)
    {
        this(name, title, description, parent);
        this.cwlFile = file;
    }
    
    public CwlScriptDataElement(String name, String title, String description, DataCollection parent)
    {
        super(name, description, parent, CWLDockeredAnalysis.class);
        this.title = title;
        this.parameterSet = new DynamicPropertySetAsMap();
        this.outputSet = new DynamicPropertySetAsMap();
    }

    @Override
    public AnalysisMethod createAnalysisMethod()
    {
        CWLDockeredAnalysis method;
        if( cwlFile != null )
            method = new CWLDockeredAnalysis(getOrigin(), this.getName());
        else
            method = new CWLDockeredAnalysis(getOrigin(), this.getName());
        method.setDescription(title);
        method.getParameters().setCwlPath(getCompletePath());
        /*
        method.getParameters().setParameters(parameterSet);
        method.getParameters().setOutputs(outputSet);
        */    
        return method;
    }

    @Override
    public String getDisplayName()
    {
        return title;
    }

    @Override
    public String getDescriptionHTML()
    {
        return null;
    }

    @Override
    public String getBaseId()
    {
        return ClassLoading.getPluginForClass(getClass());
    }

    @Override
    public URL getBase()
    {
        return null;
    }
    
    public DynamicPropertySet getParameters()
    {
        return parameterSet;
    }
    
    public DynamicPropertySet getOutputs()
    {
        return outputSet;
    }

    public void addOutput(String name, String type)
    {
        Class clazz = type2Class.get(type);
        Object value = type2Object.get(type);
        DynamicProperty dp = new DynamicProperty(name, clazz, value);        
        dp.setCanBeNull(false);
        outputSet.add(dp);
    }
    
    public void addParameter(String name, String type, boolean optional, Object defaultValue)
    {
        Class clazz = type2Class.get(type);
        Object value = defaultValue;
        if( value == null )
            value = type2Object.get(type);
        DynamicProperty dp = new DynamicProperty(name, clazz, value);
        dp.setCanBeNull(false);
        parameterSet.add(dp);
    }

    private static Map<String, Class> type2Class = new HashMap()
    {
        {
            put("boolean", Boolean.class);
            put("string", String.class);
            put("File", DataElementPath.class);
            put("Directory", DataElementPath.class);
            put("int", Integer.class);
            put("double", Double.class);
        }
    };
    
    private static Map<String, Object> type2Object = new HashMap()
    {
        {
            put("boolean", false);
            put("string", "");
            put("File", null);
            put("Directory", null);
            put("int", 0);
            put("double", 0.0);
        }
    };
}
/*
public class CwlScriptDataElement extends DataElementSupport
{
    public CwlScriptDataElement(String name, DataCollection<?> origin)
    {
        super(name, origin);
    }
}
*/
