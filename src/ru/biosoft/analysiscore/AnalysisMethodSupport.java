package ru.biosoft.analysiscore;

import java.beans.PropertyChangeListener;
import java.io.StringWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Logger;

import org.apache.commons.text.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.mozilla.javascript.Scriptable;

import com.developmentontheedge.beans.BeanInfoConstants;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.ComponentModel;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;

import one.util.streamex.StreamEx;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.biohub.ReferenceType;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.access.exception.ParameterInvalidTargetException;
import ru.biosoft.access.exception.ParameterNotAcceptableException;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.security.CodePrivilege;
import ru.biosoft.access.security.CodePrivilegeType;
import ru.biosoft.access.security.SecurityManager;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.exception.InternalException;
import ru.biosoft.exception.MissingParameterException;
import ru.biosoft.plugins.javascript.HostObjectInfo;
import ru.biosoft.plugins.javascript.JSAnalysis;
import ru.biosoft.plugins.javascript.JSProperty;
import ru.biosoft.plugins.javascript.JavaScriptHostObjectBase;
import ru.biosoft.plugins.jsconsistent.JavaScriptConsistent;
import ru.biosoft.util.BeanAsMapUtil;
import ru.biosoft.util.TextUtil2;

@CodePrivilege({CodePrivilegeType.REPOSITORY, CodePrivilegeType.THREAD, CodePrivilegeType.TEMP_RESOURCES_ACCESS, CodePrivilegeType.LAUNCH, CodePrivilegeType.REFLECTION})
public abstract class AnalysisMethodSupport<T extends AnalysisParameters> implements AnalysisMethod
{
    private String description;
    private final String name;
    private final DataCollection<?> origin;
    private Class<? extends JavaScriptHostObjectBase> jsClass;
    private Method jsMethod;
    private String hostObjectName;
    private String funcName;
    protected T parameters;
    protected Logger log = Logger.getLogger( AnalysisMethodSupport.class.getName() );
    protected AnalysisJobControl jobControl;

    public AnalysisMethodSupport(DataCollection<?> origin, String name, T parameters)
    {
        this.name = name;
        this.origin = origin;
        this.jobControl = createJobControl();
        this.parameters = parameters;
    }

    public AnalysisMethodSupport(DataCollection<?> origin, String name, Class<? extends JavaScriptHostObjectBase> jsClass, T parameters)
    {
        this(origin, name, parameters);
        this.jsClass = jsClass;

        int paramLength = -1;
        for(Method method: jsClass.getMethods())
        {
            JSAnalysis descriptor = method.getAnnotation(JSAnalysis.class);
            if(descriptor == null || !descriptor.value().isAssignableFrom(getClass())) continue;
            if(method.getParameterTypes().length > paramLength)
            {
                paramLength = method.getParameterTypes().length;
                jsMethod = method;
            }
        }
        try
        {
            DataElementPath.create("analyses/JavaScript/Host objects").getDataCollection()
                .stream( HostObjectInfo.class )
                    .filter( ho -> ho.getObjectClass().equals( jsClass ) ).findFirst()
                .ifPresent( ho -> hostObjectName = ho.getName() );
        }
        catch( RepositoryException e )
        {
            // ignore
        }
    }

    /**
     * Must be overridden to create custom JobControl object
     */
    protected AnalysisJobControl createJobControl()
    {
        return new AnalysisJobControl(this);
    }

    /**
     * Override this to perform actual analysis
     * @return analysis result item if applicable
     * @throws Exception
     */
    public Object justAnalyzeAndPut() throws Exception
    {
        return null;
    }

    /**
     * Recover previously terminated analysis.
     * This method can inspect existing outputs and reuse them to get final results.
     */
    public Object recover() throws Exception
    {
        if(isAllOutputsReady())
        {
            log.info( "Skip completed " + getName() );
            return new Object[] {};//We don't want to write analysis.* properties to results again
        }
        return justAnalyzeAndPut();
    }

    private boolean isAllOutputsReady()
    {
        DataElementPath[] existingResults = getParameters().getExistingOutputNames();
        AnalysisParameters parameters = getParameters();
        ComponentModel model = ComponentFactory.getModel( parameters, Policy.DEFAULT, true );
        String[] outputNames = getParameters().getOutputNames();
        Set<String> obligateOutputs = new HashSet<>();
        for( String name : outputNames )
        {
            Property property = model.findProperty( name );
            Object value = property.getValue();
            if( value != null || ! ( property.getBooleanAttribute( BeanInfoConstants.CAN_BE_NULL ) ) )
            {
                obligateOutputs.add( name );
            }
        }
        if( obligateOutputs.size() != existingResults.length )
            return false;
        for(DataElementPath path : existingResults)
        {
            DataCollection<?> resultDC = path.optDataCollection();
            if(resultDC == null)
                continue;
            Class<? extends AnalysisParameters> analysisClass = AnalysisParametersFactory.getAnalysisClass( resultDC );
            if(analysisClass == null)
                return false;
        }
        return true;
    }

    // Validators

    protected void checkInputs() throws IllegalArgumentException
    {
        checkPaths(getParameters().getInputNames(), null);
    }

    protected void checkOutputs() throws IllegalArgumentException
    {
        checkPaths(null, getParameters().getOutputNames());
    }

    protected void checkPaths() throws IllegalArgumentException
    {
        checkPaths(getParameters().getInputNames(), getParameters().getOutputNames());
    }

    protected void checkRange(String name, int from, int to) throws IllegalArgumentException
    {
        AnalysisParameters parameters = getParameters();
        ComponentModel model = ComponentFactory.getModel(parameters);
        Property property = model.findProperty(name);
        Object value = property.getValue();
        if( value == null || !(value instanceof Number) )
            throw new MissingParameterException( property.getDisplayName() );
        int valueNum = ((Number)value).intValue();
        if( valueNum < from || valueNum > to )
        {
            if(from == Integer.MIN_VALUE)
                throw new IllegalArgumentException(property.getDisplayName()+" should not exceed "+to);
            if(to == Integer.MAX_VALUE)
            {
                if(from == 0)
                    throw new IllegalArgumentException(property.getDisplayName()+" should be non-negative");
                throw new IllegalArgumentException(property.getDisplayName()+" should be no less than "+from);
            }
            throw new IllegalArgumentException(property.getDisplayName()+" should be from "+from+" to "+to);
        }
    }

    protected void checkGreater(String name, int from) throws IllegalArgumentException
    {
        checkRange(name, from, Integer.MAX_VALUE);
    }

    protected void checkLesser(String name, int to) throws IllegalArgumentException
    {
        checkRange(name, Integer.MIN_VALUE, to);
    }

    protected void checkRange(String name, double from, double to) throws IllegalArgumentException
    {
        AnalysisParameters parameters = getParameters();
        ComponentModel model = ComponentFactory.getModel(parameters);
        Property property = model.findProperty(name);
        Object value = property.getValue();
        if( value == null || !(value instanceof Number) )
            throw new MissingParameterException( property.getDisplayName() );
        double valueNum = ((Number)value).doubleValue();
        if( valueNum < from || valueNum > to )
        {
            if(from == Double.POSITIVE_INFINITY)
                throw new IllegalArgumentException(property.getDisplayName()+" should be less than "+to);
            if(to == Double.NEGATIVE_INFINITY)
                throw new IllegalArgumentException(property.getDisplayName()+" should be greater than "+from);
            throw new IllegalArgumentException(property.getDisplayName()+" should be from "+from+" to "+to);
        }
    }

    protected void checkGreater(String name, double from) throws IllegalArgumentException
    {
        checkRange(name, from, Double.POSITIVE_INFINITY);
    }

    protected void checkLesser(String name, double to) throws IllegalArgumentException
    {
        checkRange(name, Double.NEGATIVE_INFINITY, to);
    }

    protected void checkNotEmpty(String name) throws IllegalArgumentException
    {
        AnalysisParameters parameters = getParameters();
        ComponentModel model = ComponentFactory.getModel(parameters);
        Property property = model.findProperty(name);
        Object value = property.getValue();
        //boolean plural = property.getValueClass().isArray() || DataElementPathSet.class.isAssignableFrom(property.getValueClass());
        if( value == null || ( value instanceof DataElementPathSet && ( (DataElementPathSet)value ).isEmpty() )
                || ( value.toString().isEmpty() ) || ( value.getClass().isArray() && Array.getLength(value) == 0))
            throw new MissingParameterException( property.getDisplayName() );
    }

    protected void checkNotEmptyCollection(String name) throws IllegalArgumentException
    {
        AnalysisParameters parameters = getParameters();
        ComponentModel model = ComponentFactory.getModel(parameters);
        Property property = model.findProperty(name);
        Object value = property.getValue();
        if( !(value instanceof DataElementPath) )
            throw new MissingParameterException( property.getDisplayName() );

        DataCollection<? extends DataElement> dc;
        try
        {
            dc = ((DataElementPath)value).getDataElement(DataCollection.class);
        }
        catch( RepositoryException e )
        {
            throw new ParameterNotAcceptableException(e, property);
        }
        if(dc.isEmpty())
            throw new IllegalArgumentException(property.getDisplayName()+" is empty, analysis cannot start.");
    }

    protected void checkPaths(String[] inputNames, String[] outputNames)
    {
        AnalysisParameters parameters = getParameters();
        ComponentModel model = ComponentFactory.getModel(parameters, Policy.DEFAULT, true);
        if(inputNames == null) inputNames = new String[0];
        if(outputNames == null) outputNames = new String[0];
        Map<ru.biosoft.access.core.DataElementPath, String> inputPaths = new HashMap<>();
        for(String name: inputNames)
        {
            Property property = model.findProperty(name);
            if( !ru.biosoft.access.core.DataElementPath.class.isAssignableFrom(property.getValueClass()) || !property.isVisible(Property.SHOW_EXPERT) )
                continue;
            Object value = property.getValue();
            if(value == null)
            {
                if(property.getBooleanAttribute(BeanInfoConstants.CAN_BE_NULL)) continue;
                throw new MissingParameterException( property.getDisplayName() );
            }
            if(!(value instanceof DataElementPath)) continue;
            DataElementPath path = (DataElementPath)value;
            if( path.isEmpty() && property.getBooleanAttribute(BeanInfoConstants.CAN_BE_NULL) )
                continue;
            Object elementClassObj = property.getDescriptor().getValue(DataElementPathEditor.ELEMENT_CLASS);
            Class<? extends DataElement> elementClass = null;
            if( elementClassObj instanceof Class<?> )
                elementClass = ((Class<?>)elementClassObj).asSubclass( ru.biosoft.access.core.DataElement.class );
            Object childClassObj = property.getDescriptor().getValue(DataElementPathEditor.CHILD_CLASS);
            Class<? extends DataElement> childClass = null;
            if( childClassObj instanceof Class<?> )
                childClass = ((Class<?>)childClassObj).asSubclass( ru.biosoft.access.core.DataElement.class );
            Object referenceTypeObj = property.getDescriptor().getValue(DataElementPathEditor.REFERENCE_TYPE);
            Class<? extends ReferenceType> referenceType = null;
            if( referenceTypeObj instanceof Class<?> )
                referenceType = ((Class<?>)referenceTypeObj).asSubclass( ReferenceType.class );
            try
            {
                path.getDataElement();
            }
            catch( RepositoryException e )
            {
                throw new ParameterNotAcceptableException(e, property);
            }
            if(!DataCollectionUtils.isAcceptable(path, childClass, elementClass, referenceType))
            {
                throw new ParameterNotAcceptableException(property);
            }
            inputPaths.put(path, property.getDisplayName());
        }
        for(String name: outputNames)
        {
            Property property = model.findProperty(name);
            if( !ru.biosoft.access.core.DataElementPath.class.isAssignableFrom(property.getValueClass()) || !property.isVisible(Property.SHOW_EXPERT) )
                continue;
            Object value = property.getValue();
            if(value == null)
            {
                if(property.getBooleanAttribute(BeanInfoConstants.CAN_BE_NULL)) continue;
                throw new MissingParameterException( property.getDisplayName() );
            }
            if(!(value instanceof DataElementPath)) continue;
            DataElementPath path = (DataElementPath)value;
            DataElementPath parentPath = path.getParentPath().getTargetPath();
            try
            {
                DataCollectionUtils.checkQuota(parentPath);
                DataCollectionUtils.createFoldersForPath(path);
            }
            catch( Exception e1 )
            {
                throw new ParameterInvalidTargetException(e1, property);
            }
            if(path.getName().equals(""))
            {
                throw new MissingParameterException( property.getDisplayName() );
            }
            if(inputPaths.containsKey(path))
            {
                throw new IllegalArgumentException(property.getDisplayName()+" is the same as "+TextUtil2.toLower(inputPaths.get(path))+": "+path+". Please specify different path.");
            }
            if( !SecurityManager.getPermissions(parentPath).isWriteAllowed() )
                throw new IllegalArgumentException(property.getDisplayName() + ": specified collection is write protected: "
                        + path.getParentPath() + ".\nPlease specify different path.");
            Object elementClassObj = property.getDescriptor().getValue(DataElementPathEditor.ELEMENT_CLASS);
            Class<? extends DataElement> elementClass = null;
            if( elementClassObj instanceof Class<?> )
                elementClass = ((Class<?>)elementClassObj).asSubclass( ru.biosoft.access.core.DataElement.class );
            if(!DataCollectionUtils.isAcceptable(parentPath, elementClass, null))
            {
                throw new ParameterInvalidTargetException(null, property);
            }
        }
    }

    @Override
    public String getName()
    {
        return name;
    }

    @Override
    public DataCollection<?> getOrigin()
    {
        return origin;
    }

    @Override
    public String getDescription()
    {
        return description;
    }

    @Override
    public void setDescription(String description)
    {
        this.description = description;
    }

    @Override
    public T getParameters()
    {
        return parameters;
    }

    @SuppressWarnings ( "unchecked" )
    @Override
    public void setParameters(AnalysisParameters parameters)
    {
        if(this.parameters != null && !this.parameters.getClass().isInstance( parameters ))
        {
            throw new InternalException( "Attempt to set wrong parameter class: " + parameters.getClass() + "; expected: "
                    + this.parameters.getClass() );
        }
        this.parameters = (T)parameters;
    }

    @SuppressWarnings ( "unchecked" )
    protected T getDefaultParameters() throws Exception
    {
        return (T)getClass().getConstructor(DataCollection.class, String.class).newInstance(null, "").getParameters();
    }


    public String generateJavaScript(Object parameters)
    {
        if(funcName != null) return generateJavaScriptForHost(parameters);
        if(jsClass == null) return "/* no JavaScript handler class specified */";
        if(jsMethod == null) return "/* no method found to handle this analysis */";
        if(hostObjectName == null) return "/* Unable to find host object for specified JS class "+jsClass.getName()+" */";
        Annotation[][] annotations = jsMethod.getParameterAnnotations();
        Class<?>[] paramTypes = jsMethod.getParameterTypes();
        ComponentModel model = ComponentFactory.getModel(parameters);
        String[] params = new String[paramTypes.length];
        Set<String> usedProperties = new HashSet<>();
        for(int i=0; i<annotations.length; i++)
        {
            params[i] = "null";
            for(Annotation annotation: annotations[i])
            {
                if(!(annotation instanceof JSProperty)) continue;
                String propertyName = ((JSProperty)annotation).value();
                usedProperties.add(propertyName);
                Property property = model.findProperty(propertyName);
                if(property == null || property.getValue() == null)
                    break;
                Object value = property.getValue();
                if(value instanceof Number || value instanceof Boolean)
                    params[i] = value.toString();
                else if(value instanceof DataElementPath)
                {
                    if(DataElement.class.isAssignableFrom(paramTypes[i]))
                        params[i] = "data.get('"+StringEscapeUtils.escapeJava(value.toString())+"')";
                    else
                        params[i] = "'"+StringEscapeUtils.escapeJava(value.toString())+"'";
                }
                else if( value instanceof JavaScriptConsistent )
                {
                    params[i] = ( (JavaScriptConsistent)value ).toJaveScriptString();
                }
                else if(value.getClass().isArray())
                {
                    JSONArray array = new JSONArray();
                    for(Object element: (Object[])value)
                        array.put(element);
                    params[i] = array.toString();
                }
                else params[i] = "'"+StringEscapeUtils.escapeJava(property.getValue().toString())+"'";
                break;
            }
        }
        for(int i=0; i<paramTypes.length; i++)
        {
            if(!paramTypes[i].equals(Scriptable.class)) continue;
            try
            {
                AnalysisParameters defParameters = getClass().getConstructor(DataCollection.class, String.class).newInstance(null, "").getParameters();
                ComponentModel defModel = ComponentFactory.getModel(defParameters);
                JSONObject opt = new JSONObject();
                for(int j=0; j<defModel.getPropertyCount(); j++)
                {
                    Property defProperty = defModel.getPropertyAt(j);
                    if(usedProperties.contains(defProperty.getName())) continue;
                    try
                    {
                        Object defValue = defProperty.getValue();
                        Object value = model.findProperty(defProperty.getName()).getValue();
                        if( !Objects.equals( defValue, value ) )
                            opt.put(defProperty.getName(), value);
                    }
                    catch( Exception e )
                    {
                    }
                }
                params[i] = opt.toString();
            }
            catch( Exception e )
            {
            }
            break;
        }
        return hostObjectName+"."+jsMethod.getName()+"("+String.join(", ", params)+");";
    }

    private String generateJavaScriptForHost(Object parameters)
    {
        try
        {
            AnalysisParameters defParameters = getDefaultParameters();
            StringWriter writer = new StringWriter();
            JSONWriter jsonWriter = new JSONWriter(writer);
            //JSONObject opt = new JSONObject();
            CompositeProperty model = ComponentFactory.getModel(parameters, Policy.DEFAULT, true);
            CompositeProperty defModel = ComponentFactory.getModel(defParameters, Policy.DEFAULT, true);
            jsonWriter.object();
            Map<String, String> parametersMap = new LinkedHashMap<>();
            putNonDefaultParameters(parametersMap, model, defModel, "");
            for(Entry<String, String> entry: parametersMap.entrySet())
            {
                jsonWriter.key(entry.getKey()).value(entry.getValue());
            }
            jsonWriter.endObject();
            return funcName+"("+writer.toString()+");";
        }
        catch( Exception e )
        {
            return "/* unable to construct parameters string: "+ExceptionRegistry.log(e)+" */";
        }
    }

    private String generateRScript(AnalysisParameters parameters)
    {
        try
        {
            AnalysisParameters defParameters = getDefaultParameters();
            
            Map<String, Object> nonDefault = BeanAsMapUtil.getNonDefault( parameters, defParameters );
            Map<String, Object> flatNonDefault = BeanAsMapUtil.flattenMap( nonDefault );
            flatNonDefault = BeanAsMapUtil.makeOneBased(flatNonDefault);
            
            boolean firstParameter = true;
            int curLength = 0;
            StringBuilder sb = new StringBuilder();
            sb.append("library(rbiouml)\nbiouml.analysis(\""+StringEscapeUtils.escapeJava(getName())+"\", list(");
            for(Entry<String, Object> entry: flatNonDefault.entrySet())
            {
                String parameter;
                if(entry.getKey().contains("/"))
                    parameter = '"'+entry.getKey()+'"';
                else
                    parameter = entry.getKey();
                Object value = entry.getValue();
                if(value instanceof Map && ( (Map<?,?>)value ).isEmpty())
                    parameter += "=list()";//empty map means leave all defaults
                else if (value instanceof List && ((List<?>)value).isEmpty())
                    parameter += "=list()";//empty list means empty ArrayProperty
                else
                    parameter+="=\""+value.toString().replace("\\", "\\\\").replace("\"", "\\\"")+'"';
                if(!firstParameter)
                {
                    sb.append(',');
                    int addLength = parameter.length();
                    if(addLength+curLength > 40)
                    {
                        sb.append("\n  ");
                        curLength = addLength;
                    } else
                    {
                        sb.append(' ');
                        curLength += addLength + 2;
                    }
                } else
                {
                    sb.append("\n  ");
                    curLength = parameter.length();
                }
                sb.append(parameter);
                firstParameter = false;
            }
            if(!firstParameter)
                sb.append('\n');
            sb.append("))");
            return sb.toString();
        }
        catch( Exception e )
        {
            return "# Unable to construct parameters string:\n# "+ExceptionRegistry.log(e).replace("\n", "\n# ");
        }
    }


    private void putNonDefaultParameters(Map<String, String> params, Property model, Property defModel, String prefix)
    {
        for(int j=0; j<defModel.getPropertyCount(); j++)
        {
            try
            {
                Property defProperty = defModel.getPropertyAt(j);
                if(defProperty.isReadOnly()) continue;
                Object defValue = defProperty.getValue();
                Property property = model.findProperty(defProperty.getName());
                Object value = property.getValue();
                if((defValue == null && value == null) || (defValue != null && value != null && defValue.equals(value))) continue;
                if(property instanceof CompositeProperty && !property.isHideChildren() && !property.getValueClass().getPackage().getName().startsWith("java."))
                {
                    putNonDefaultParameters(params, property, defProperty, prefix+property.getName()+"/");
                } else
                {
                    params.put(prefix+property.getName(), TextUtil2.toString(value));
                    try
                    {
                        defProperty.setValue(value);
                    }
                    catch( Exception e )
                    {
                    }
                }
            }
            catch( Exception e )
            {
            }
        }
    }

    @Override
    public AnalysisJobControl getJobControl()
    {
        return jobControl;
    }

    @Override
    public Logger getLogger()
    {
        return log;
    }

    @Override
    public void setLogger(Logger log)
    {
        this.log = log;
    }

    public Object[] getAnalysisResults()
    {
        ru.biosoft.access.core.DataElementPath[] outNames = getParameters().getExistingOutputNames();
        if( outNames == null )
            return null;
        Object[] results = StreamEx.of(outNames).map( DataElementPath::optDataElement ).toArray();
        return results.length == 0 ? null : results;
    }

    protected void writeProperties(DataElement de) throws Exception
    {
        if( de instanceof DataCollection )
        {
            AnalysisParametersFactory.write( de, this );
            CollectionFactoryUtils.save(de);
        }
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        checkPaths();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l)
    {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l)
    {
    }

    public void setJavaScriptFunction(String js)
    {
        this.funcName = js;
    }

    @Override
    public double estimateWeight()
    {
        return 1;
    }

    @Override
    public long estimateMemory()
    {
        return 0;
    }

    @Override
    public Map<String, String> generateScripts(AnalysisParameters parameters)
    {
        String javaScript = generateJavaScript(parameters);
        Map<String, String> result = new HashMap<>();
        if(javaScript != null && !javaScript.isEmpty())
            result.put("js", javaScript);
        result.put("R", generateRScript(parameters));
        return result;
    }
}
