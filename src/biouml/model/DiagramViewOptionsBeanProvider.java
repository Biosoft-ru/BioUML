package biouml.model;

import java.util.stream.Stream;

import com.developmentontheedge.beans.annot.PropertyName;

import one.util.streamex.StreamEx;
import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.RepositoryException;
import ru.biosoft.util.bean.BeanInfoEx2;

public class DiagramViewOptionsBeanProvider implements BeanProvider
{
    @Override
    public Object getBean(String path)
    {
        try
        {
            DataElementPath dePath = DataElementPath.create( path );
            DataElement de = dePath.getDataElement();
            if( de instanceof Diagram )
                return ( (Diagram)de ).getViewOptions();
            else if( de instanceof DiagramElement )
            {
                return new DiagramElementViewOptions( (DiagramElement)de );
            }
            else
                return null;
        }
        catch( RepositoryException e )
        {
            return null;
        }
    }

    public static class DiagramElementViewOptions
    {
        DiagramElement de;

        public DiagramElementViewOptions(DiagramElement de)
        {
            this.de = de;
        }

        @PropertyName ( "Predefined style" )
        public String getPredefinedStyle()
        {
            return de.getPredefinedStyle();
        }

        public Stream<String> getAvailableStyles()
        {
            return StreamEx.of( Diagram.getDiagram( de ).getViewOptions().getStyles() ).map( s -> s.getName() )
                    .append( DiagramElementStyle.STYLE_NOT_SELECTED, DiagramElementStyle.STYLE_DEFAULT );
        }

        public void setPredefinedStyle(String predefinedStyle)
        {
            de.setPredefinedStyle( predefinedStyle );
        }

        @PropertyName ( "Custom style" )
        public DiagramElementStyle getCustomStyle()
        {
            return de.getCustomStyle();
        }

        public void setCustomStyle(DiagramElementStyle customStyle)
        {
            de.setCustomStyle( customStyle );
        }

        public boolean isStylePredefined()
        {
            return de.isStylePredefined();
        }
    }

    public static class DiagramElementViewOptionsBeanInfo extends BeanInfoEx2<DiagramElementViewOptions>
    {
        public DiagramElementViewOptionsBeanInfo()
        {
            super( DiagramElementViewOptions.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            addWithTags( "predefinedStyle", bean -> bean.getAvailableStyles() );
            addHidden( "customStyle", "isStylePredefined" );
        }
    }
}
