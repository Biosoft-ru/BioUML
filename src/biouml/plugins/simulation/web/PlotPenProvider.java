package biouml.plugins.simulation.web;

import biouml.model.Diagram;
import biouml.model.dynamics.plot.PlotsInfo;
import biouml.model.dynamics.plot.Curve;
import biouml.model.dynamics.plot.Experiment;
import biouml.model.dynamics.plot.PlotInfo;
import biouml.standard.simulation.plot.Plot;
import biouml.standard.simulation.plot.Series;
import one.util.streamex.StreamEx;
import ru.biosoft.access.BeanProvider;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.server.servlets.webservices.providers.WebBeanProvider;
import ru.biosoft.util.TextUtil;

public class PlotPenProvider implements BeanProvider
{
    @Override
    public Object getBean(String path)
    {
        String[] params = TextUtil.split( path, ';' );
        
        if( params.length >= 2 )
        {
            DataElementPath fullPath = DataElementPath.create( params[0] );
            DataElementPath dePath = fullPath.getParentPath();
            Object de = WebBeanProvider.getBean( dePath.toString() );
            //fullPath = plotPath/source/stringNumber
            if( de == null )
            {
                dePath = dePath.getParentPath();
                de = WebBeanProvider.getBean( dePath.toString() );
            }
            if( de != null && de instanceof Plot )
            {
                Series series = ( (Plot)de ).getSeries().stream().filter( s -> s.getName().equals( params[1] ) ).findFirst().orElse( null );
                if( series != null )
                    return series.getSpec();

            }
            else
            {
                de = WebBeanProvider.getBean( dePath.getParentPath().toString() );
                if( de != null && de instanceof Diagram )
                {
                    Object plotObject = ( (Diagram)de ).getAttributes().getValue( "Plots" );
                    if( plotObject instanceof PlotsInfo )
                    {
                        PlotInfo plot = StreamEx.of( ( (PlotsInfo)plotObject ).getPlots() )
                                .findFirst( pi -> pi.getTitle().equals( params[1] ) ).orElse( null );
                        if( plot != null )
                        {
                            if( params[3].equals( "curve" ) )
                            {
                                for( Curve curve : plot.getYVariables() )
                                {
                                    if( curve.getName().equals( params[2] ) )
                                        return curve.getPen();
                                }
                            }
                            else if( params[3].equals( "experiment" ) && plot.getExperiments() != null )
                            {
                                for( Experiment exp : plot.getExperiments() )
                                {
                                    if( exp.getName().equals( params[2] ) )
                                        return exp.getPen();
                                }
                            }
                        }

                    }
                }

            }
            
        }
        return null;
    }

}
