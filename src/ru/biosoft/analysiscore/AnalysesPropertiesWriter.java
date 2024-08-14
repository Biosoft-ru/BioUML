package ru.biosoft.analysiscore;

import static ru.biosoft.util.j2html.TagCreator.*;

import java.awt.Color;
import java.util.stream.Collectors;

import one.util.streamex.Joining;
import one.util.streamex.StreamEx;
import ru.biosoft.access.BeanRegistry;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.util.BeanUtil;
import ru.biosoft.util.ColorUtils;
import ru.biosoft.util.TextUtil;
import ru.biosoft.util.j2html.tags.ContainerTag;
import ru.biosoft.util.j2html.tags.DomContent;
import ru.biosoft.util.j2html.tags.Tag;
import ru.biosoft.util.j2html.tags.Text;
import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.model.ArrayProperty;
import com.developmentontheedge.beans.model.ComponentFactory;
import com.developmentontheedge.beans.model.ComponentFactory.Policy;
import com.developmentontheedge.beans.model.CompositeProperty;
import com.developmentontheedge.beans.model.Property;

public class AnalysesPropertiesWriter
{
    public String getWorkflowDescription(DataCollection<?> dc)
    {
        if( dc == null ) return "";
        String workflowPath = dc.getInfo().getProperty("workflow_path");
        if(workflowPath == null)
            return "";
        ContainerTag result = div().with(
            b().withText( "Result of workflow" ),
            new Text(": "),
            a().withHref( "de:"+TextUtil.encodeURL( workflowPath ) + "&fromDE=" + TextUtil.encodeURL( dc.getCompletePath().toString() ) )
                .withTitle(workflowPath).withText( DataElementPath.create( workflowPath ).getName() ),
            br()
        );
        Object bean = BeanRegistry.getBean("workflow/relaunch/"+dc.getCompletePath(), null);
        if(bean instanceof DynamicPropertySet)
        {
            result = result.with( b().withText( "Parameters:" ), br(),
                    ul().withType( "circle" )
                        .with( StreamEx.of(((DynamicPropertySet)bean).iterator())
                                .map( property -> li().with(
                                        b().withText( property.getName() ).withText( ": " ),
                                        getPropertyValue(property.getValue()) ) ).toList() )
                    );
        }
        return result.render();
    }
    
    public String getParametersDescription(DataCollection<?> dc)
    {
        if( dc == null )
            return "";
        String analysisName = dc.getInfo().getProperty( AnalysisParametersFactory.ANALYSIS_NAME_PROPERTY );
        if( analysisName == null )
            return "";

        String analysisParametersDescription = dc.getInfo().getProperty( AnalysisParametersFactory.PARAMETERS_DESCRIPTION_PROPERTY );
        if( analysisParametersDescription == null )
        {
            AnalysisParameters parameters = AnalysisParametersFactory.read( dc );
            if( parameters == null )
                return "";
            analysisParametersDescription = getParametersHTMLDescription( parameters );
        }
        AnalysisMethodInfo methodInfo = AnalysisMethodRegistry.getMethodInfo( analysisName );
        if( methodInfo == null )
            return "";
        DataElementPath analysisPath = methodInfo.getCompletePath();
        ContainerTag tag = div();
        tag = tag.with( b().withText( "Result of analysis" ) ).withText( ": " );
        if( analysisPath == null )
        {
            tag = tag.withText( analysisName );
        }
        else
        {
            tag = tag.with( a()
                    .withHref( "de:" + TextUtil.encodeURL( analysisPath.toString() ) + "&fromDE="
                            + TextUtil.encodeURL( dc.getCompletePath().toString() ) )
                    .withTitle( String.valueOf( analysisPath ) ).withText( analysisName ) );
        }
        return tag.render().concat( analysisParametersDescription );
    }

    public static String getParametersHTMLDescription(AnalysisParameters parameters)
    {
        Property prop = ComponentFactory.getModel( parameters, Policy.DEFAULT, true );
        ContainerTag tag = div();
        ContainerTag properties = getRecursivePropertyDescription( prop );
        if( properties != null )
            tag = tag.with( b().withText( "Parameters:" ), br(), properties.withType( "circle" ) );
        return tag.render();
    }

    private static ContainerTag getRecursivePropertyDescription(Property model)
    {
        ContainerTag tag = null;
        for( Property property : BeanUtil.properties( model ).filter( property -> property.isVisible( Property.SHOW_EXPERT ) ) )
        {
            ContainerTag li = li().with( b().withText( property.getDisplayName() ) )
                .condWith( property.getDescriptor().isExpert(), new Text(" (expert)") );
            if( property instanceof ArrayProperty
                    || ( property instanceof CompositeProperty && !property.isHideChildren() && !property.getValueClass().getPackage()
                            .getName().startsWith( "java." ) ) )
            {
                ContainerTag subProperty = getRecursivePropertyDescription(property);
                if(subProperty == null)
                    continue;
                li = li.with( subProperty );
            }
            else
            {
                li = li.withText( ": " ).with( getPropertyValue( property.getValue() ) );
            }
            tag = (tag == null ? ul() : tag).with( li );
        }
        return tag;
    }

    /**
     * Returns HTML-formatted value (some types are handled specially for user-friendly display)
     * @param value
     * @return
     */
    private static DomContent getPropertyValue(Object value)
    {
        if(value == null || value.equals(""))
        {
            return new Text("(none)");
        }
        if(value.getClass().isArray())
        {
            return new Text(StreamEx.of( (Object[])value ).map( String::valueOf ).collect( Joining.with( ", " ).maxGraphemes( 100 ) ));
        }
        if(value instanceof DataElementPath)
        {
            DataElementPath path = (DataElementPath)value;
            return getPathHTML(path);
        }
        if(value instanceof DataElementPathSet)
        {
            String paths = ( (DataElementPathSet)value ).stream().map( path -> getPathHTML( path ).toString() )
                    .collect( Collectors.joining( ", " ) );
            return unsafeHtml("["+((DataElementPathSet)value).size()+"] "+paths);
        }
        if(value instanceof Color)
        {
            return div().attr( "style", "display: inline-block; width: 50px; height: 10px; background-color: "+ColorUtils.colorToString( (Color)value));
        }
        return new Text(value.toString());
    }
    
    protected static Tag<?> getPathHTML(DataElementPath path)
    {
        if( path.exists() )
        {
            return a().withHref( "de:" + TextUtil.encodeURL( path.toString() ) ).withTitle( path.toString() ).withText( path.getName() );
        }
        return span().withTitle( path.toString() ).withText( path.getName() );
    }
}
