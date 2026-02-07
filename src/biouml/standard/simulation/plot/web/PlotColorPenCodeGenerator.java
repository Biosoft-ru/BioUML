package biouml.standard.simulation.plot.web;

import static ru.biosoft.util.j2html.TagCreator.div;
import static ru.biosoft.util.j2html.TagCreator.input;
import static ru.biosoft.util.j2html.TagCreator.p;
import static ru.biosoft.util.j2html.TagCreator.span;

import java.awt.Color;
import java.util.Properties;

import com.developmentontheedge.beans.Option;

import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.Experiment;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.standard.simulation.plot.Series;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.util.ControlCodeGenerator;
import ru.biosoft.util.Util;
import ru.biosoft.util.j2html.tags.Tag;

public class PlotColorPenCodeGenerator implements ControlCodeGenerator
{

    @Override
    public Tag<?> getControlCode(Object value) throws Exception
    {
        Pen pen = (Pen) value;
        Color color = pen.getColor();
        long uid = Util.getUniqueId();
        String viewerId = "viewer_" + uid;
        return div()
                .attr( "style", "width:100px; height:12px; border:1px solid black; background-color:rgb(" + color.getRed() + "," + color.getGreen() + "," + color.getBlue() + ");" )
                .withId( viewerId );
    }

    @Override
    public Class<?> getSupportedItemType()
    {
        return Pen.class;
    }

    @Override
    public Tag<?> getControlCode(Object value, Properties properties) throws Exception
    {
        Object rowBean = properties.getOrDefault( "parentBean", null );
        String plotPath = properties.getProperty( "parentPath" );
        String name = ((Series) rowBean).getName();
        Pen pen = (Pen) value;
        Color color = pen.getColor();
        long uid = Util.getUniqueId();
        String viewerId = "viewer_" + uid;
        
        if(Series.class.isAssignableFrom( rowBean.getClass() ))
        {
            String parentPath = "plotseriespen/" + plotPath + ";" + name;
            return p().withClass( "cellControl" ).attr( "style", "white-space:nowrap;" )
                    //.with( input().withType( "image" ).withSrc( "icons/edit.gif" ).withValue( "Edit" ).attr( "onclick", "editColorPen('" + parentPath + "', '" + viewerId + "')" ) )
                    .with( span().attr( "style", "width:100px; height:12px; border:1px solid black; float:left; background-color:rgb(" + color.getRed() + "," + color.getGreen()
                            + "," + color.getBlue() + ");" ).withId( viewerId ).attr( "onclick", "editColorPen('" + parentPath + "', '" + viewerId + "')" ) );
        }
        else if(Curve.class.isAssignableFrom( rowBean.getClass() ) || Experiment.class.isAssignableFrom( rowBean.getClass() ))
        {
            Option plotParent = ( (Option)rowBean ).getParent();
            if( plotParent != null && plotParent instanceof PlotInfo )
            {
                String plotName = ( (PlotInfo)plotParent ).getTitle();
                String varType = Curve.class.isAssignableFrom( rowBean.getClass() ) ? "curve" : "experiment";
                String parentPath = "plotseriespen/" + plotPath + ";" + plotName + ";" + name + ";" + varType;
                return p().withClass( "cellControl" ).attr( "style", "white-space:nowrap;" )
                        //.with( input().withType( "image" ).withSrc( "icons/edit.gif" ).withValue( "Edit" ).attr( "onclick", "editColorPen('" + parentPath + "', '" + viewerId + "')" ) )
                        .with( span().attr( "style", "width:100px; height:12px; border:1px solid black; float:left; background-color:rgb(" + color.getRed() + "," + color.getGreen()
                                + "," + color.getBlue() + ");" ).withId( viewerId ).attr( "onclick", "editColorPen('" + parentPath + "', '" + viewerId + "')" ) );
            }
        }
        return null;
    }

    @Override
    public boolean isApplicable(Properties properties)
    {
        Object editorClass = properties.getOrDefault( "editorClass", null );
        Object rowBean = properties.getOrDefault( "parentBean", null );
        return editorClass != null && PenEditor.class.isAssignableFrom( (Class<?>) editorClass ) && rowBean != null && (Series.class.isAssignableFrom( rowBean.getClass() )
                || Curve.class.isAssignableFrom( rowBean.getClass() ) || Experiment.class.isAssignableFrom( rowBean.getClass() ));
    }

}
