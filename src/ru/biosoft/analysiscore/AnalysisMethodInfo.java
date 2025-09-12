package ru.biosoft.analysiscore;

import java.lang.reflect.Constructor;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.text.StringEscapeUtils;

import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.ClassLoading;
import ru.biosoft.access.core.CloneableDataElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataElementImporterRegistry;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementSupport;
import ru.biosoft.access.HtmlDescribedElement;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.repository.IconFactory;
import ru.biosoft.util.bean.BeanInfoEx2;

import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.DynamicPropertySetAsMap;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;
import com.developmentontheedge.application.ApplicationUtils;

/**
 * Analysis method info object
 */
@ClassIcon ( "resources/analysis.gif" )
@PropertyName("analysis")
public class AnalysisMethodInfo extends DataElementSupport implements HtmlDescribedElement, CloneableDataElement
{
    private static final Pattern ANALYSIS_CLAUSE_PATTERN = Pattern.compile("%analysis:(.+?)%");

    private static Logger log = Logger.getLogger( AnalysisMethodInfo.class.getName() );
    protected String description;
    protected Class<? extends AnalysisMethod> clazz;
    protected DynamicPropertySet attributes;
    private String js;

    public AnalysisMethodInfo(String name, String description, DataCollection<?> parent, Class<? extends AnalysisMethod> clazz)
    {
        super(name, parent);
        this.description = description;
        this.clazz = clazz;
    }

    public AnalysisMethodInfo(String name, String description, DataCollection<?> parent, Class<? extends AnalysisMethod> clazz, String js)
    {
        this(name, description, parent, clazz);
        this.js = js;
    }

    public DynamicPropertySet getAttributes()
    {
        if( attributes == null )
            attributes = new DynamicPropertySetAsMap();

        return attributes;
    }

    public String getDisplayName()
    {
        return getName();
    }

    public AnalysisMethod createAnalysisMethod()
    {
        try
        {
            Constructor<? extends AnalysisMethod> constructor = clazz.getConstructor(DataCollection.class, String.class);
            AnalysisMethod analysisMethod = constructor.newInstance(getOrigin(), getName());
            analysisMethod.setDescription(getDescription());
            if(js != null && (analysisMethod instanceof AnalysisMethodSupport))
            {
                ((AnalysisMethodSupport<?>)analysisMethod).setJavaScriptFunction(js);
            }
            return analysisMethod;
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE,  ExceptionRegistry.log( e ) );
        }
        return null;
    }

    public Class<? extends AnalysisMethod> getAnalysisClass()
    {
        return clazz;
    }

    public String getDescription()
    {
        return description;
    }

    public void setDescription(String description)
    {
        this.description = description;
    }
    
    private String shortDescription;
    

    public String getShortDescription()
    {
        if(shortDescription == null)
        {
            if(description == null || description.length() > 600 || description.endsWith( ".html" ))
                return "";
            return description;
        }
        return shortDescription;
    }

    public void setShortDescription(String shortDescription)
    {
        this.shortDescription = shortDescription;
    }

    private Class<?> getBaseClass()
    {
        if(getAnalysisClass().equals(ImportAnalysis.class))
        {
            return DataElementImporterRegistry.getImporterInfo(getName()).getImporter().getClass();
        }
        return getAnalysisClass();
    }

    @Override
    public URL getBase()
    {
        String description = getDescription();
        if( description == null )
            return null;
        return ClassLoading.getResourceURL( getBaseClass(), description );
    }

    /**
     * @return ID prefix for images referenced from getDescriptionHTML appropriate to pass into {@link IconFactory}.getIconById
     */
    @Override
    public String getBaseId()
    {
        try
        {
            return ClassLoading.getPluginForClass( getBaseClass() ) + ":" + description.replaceFirst("/[^\\/]+$", "");
        }
        catch( Exception e )
        {
            return null;
        }
    }

    /**
     * @return String containing HTML document describing this analysis
     */
    @Override
    public String getDescriptionHTML()
    {
        String description = getDescription();
        ClassLoader cl = ClassLoading.getClassLoader( getBaseClass() );
        URL url = description.isEmpty() ? null : cl.getResource(description);
        if( url == null )
        {
            return "<h1>"+getName()+"</h1>"+description+getParametersDescription();
        }
        try
        {
            String stringDescription = ApplicationUtils.readAsString(url.openStream());
            if(stringDescription.contains("%parameters%"))
                stringDescription = stringDescription.replace("%parameters%", getParametersDescription());
            stringDescription = stringDescription.replace("%analysisName%", getName());
            Matcher m = ANALYSIS_CLAUSE_PATTERN.matcher(stringDescription);
            int start = 0;
            while(m.find(start))
            {
                String analysisName = m.group(1);
                AnalysisMethodInfo methodInfo = AnalysisMethodRegistry.getMethodInfo(analysisName);
                String replacement;
                if(methodInfo != null)
                {
                    DataElementPath analysisPath = methodInfo.getCompletePath();
                    replacement = "<a href=\"de:"+analysisPath+"\">"+analysisPath.getName()+"</a>";
                } else
                {
                    replacement = analysisName;
                }
                stringDescription = stringDescription.substring(0, m.start())+replacement+stringDescription.substring(m.end());
                start = m.start()+replacement.length();
                m = ANALYSIS_CLAUSE_PATTERN.matcher(stringDescription);
            }
            return stringDescription;
        }
        catch( Exception e )
        {
            return "";
        }
    }

    private String analysisDescription;
    private String getParametersDescription()
    {
        if(analysisDescription == null)
        {
            synchronized(this)
            {
                if(analysisDescription == null)
                {
                    StringBuilder sb = new StringBuilder();
                    sb.append("<br><h3>Parameters:</h3><ul type=\"circle\">")
                            .append(getRecursivePropertyDescription(ComponentFactory.getModel(createAnalysisMethod().getParameters())))
                            .append("</ul>");
                    analysisDescription = sb.toString();
                }
            }
        }
        return analysisDescription;
    }

    private String getRecursivePropertyDescription(Property model)
    {
        StringBuilder sb = new StringBuilder();
        Set<String> names = new HashSet<>();
        for(int i=0; i<model.getPropertyCount(); i++)
        {
            Property property = model.getPropertyAt(i);
            if( property.getDescriptor().isHidden() || names.contains(property.getDisplayName()) ) continue;
            names.add(property.getDisplayName());
            sb.append("<li><b>").append(property.getDisplayName()).append("</b>");
            if(property.getDescriptor().isExpert())
                sb.append(" (").append("expert").append(")");
            Object htmlDescription = property.getDescriptor().getValue(BeanInfoEx2.HTML_DESCRIPTION_PROPERTY);
            sb.append(" &ndash; ");
            if(htmlDescription != null)
                sb.append(htmlDescription.toString().replace("\n", "<br>"));
            else
                sb.append(StringEscapeUtils.escapeHtml4(property.getShortDescription()).replace("\n", "<br>"));
            if(property instanceof CompositeProperty && !property.isHideChildren() && !property.getValueClass().getPackage().getName().startsWith("java."))
            {
                String subProperty = getRecursivePropertyDescription(property);
                if(!subProperty.isEmpty())
                {
                    sb.append("<ul>").append(subProperty).append("</ul>");
                }
            }
        }
        return sb.toString();
    }

    @Override
    public AnalysisMethodInfo clone(DataCollection origin, String name)
    {
        try
        {
            return (AnalysisMethodInfo)super.clone(origin, name);
        }
        catch( CloneNotSupportedException e )
        {
            throw new InternalError();
        }
    }
}