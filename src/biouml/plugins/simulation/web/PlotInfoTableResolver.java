package biouml.plugins.simulation.web;

import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.Diagram;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.Experiment;
import biouml.model.dynamics.plot.PlotInfo;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.VectorDataCollection;
import ru.biosoft.graphics.Pen;
import ru.biosoft.graphics.PenEditor;
import ru.biosoft.server.servlets.webservices.BiosoftWebRequest;
import ru.biosoft.server.servlets.webservices.WebException;
import ru.biosoft.table.access.TableResolver;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PlotInfoTableResolver extends TableResolver
{
    public static final String TABLE_TYPE_PARAMETER = "tabletype";
    public static final String PLOT_NAME_PARAMETER = "plotname";

    private String type = "curves";
    private String plotName = null;

    public PlotInfoTableResolver(BiosoftWebRequest arguments) throws WebException
    {
        this.type = arguments.getString( TABLE_TYPE_PARAMETER );
        if( type.equals( "curves" ) || type.equals( "experiments" ) )
        {
            plotName = arguments.getString( PLOT_NAME_PARAMETER );
        }
    }

    @Override
    public DataCollection<?> getTable(DataElement de) throws Exception
    {
        Diagram diagram = (Diagram)de;
        Object plotObject = diagram.getAttributes().getValue( "Plots" );
        PlotsInfo plotsInfo = null;
        if( plotObject instanceof PlotsInfo )
        {
            plotsInfo = (PlotsInfo)plotObject;
        }
        PlotInfo plotInfo = StreamEx.of( plotsInfo.getPlots() ).findFirst( pi -> pi.getTitle().equals( plotName ) ).orElse( null );
        if( plotInfo != null )
        {
            if( type.equals( "curves" ) )
            {
                VectorDataCollection<CurveWrapper> vdc = new VectorDataCollection<>( "Curves", CurveWrapper.class, null );
                for( Curve curve : plotInfo.getYVariables() )
                    vdc.put( new CurveWrapper( curve ) );
                return vdc;
            }
            else if( type.equals( "experiments" ) )
            {
                VectorDataCollection<Experiment> vdc = new VectorDataCollection<>( "Experiments", Experiment.class, null );
                if( plotInfo.getExperiments() != null )
                    for( Experiment v : plotInfo.getExperiments() )
                        vdc.put( v );
                return vdc;
            }
        }
        else
            throw new WebException( "EX_QUERY_NO_TABLE", "plots" );
        return null;
    }

    public class CurveWrapper implements DataElement
    {
        private Curve curve;
        private String curveName;

        public CurveWrapper(Curve curve)
        {
            this.curve = curve;
            this.curveName = curve.getName();
        }


        public String getName()
        {
            return curve.getCompleteName();
        }

        public void setName(String name)
        {
            curve.setName( name );
        }

        @Override
        public DataCollection<?> getOrigin()
        {
            return null;
        }

        public Curve getCurve()
        {
            return curve;
        }

        @PropertyName ( "Path" )
        public String getPath()
        {
            return curve.getPath();
        }
        public void setPath(String path)
        {
            curve.setPath( path );
        }

        @PropertyName ( "Title" )
        public String getTitle()
        {
            return curve.getTitle();
        }
        public void setTitle(String title)
        {
            curve.setTitle( title );
        }

        @PropertyName ( "Line spec" )
        public Pen getPen()
        {
            return curve.getPen();
        }
        public void setPen(Pen pen)
        {
            curve.setPen( pen );
        }

        @PropertyName ( "Name" )
        public String getCurveName()
        {
            return curveName;
        }

        public void setCurveName(String curveName)
        {
            this.curveName = curveName;
        }
    }
    public static class CurveWrapperBeanInfo extends BeanInfoEx2<CurveWrapper>
    {
        public CurveWrapperBeanInfo()
        {
            super( CurveWrapper.class );
        }

        @Override
        public void initProperties() throws Exception
        {
            add( "path" );
            add( "curveName" );
            add( "title" );
            add( "pen", PenEditor.class );
        }
    }

}
