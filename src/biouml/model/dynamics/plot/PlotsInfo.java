package biouml.model.dynamics.plot;

import java.awt.BasicStroke;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

import org.jfree.chart.ChartColor;

import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.annot.PropertyName;

import biouml.model.dynamics.EModel;
import one.util.streamex.StreamEx;
import ru.biosoft.graphics.Pen;

@PropertyName ( "Simulation Plots" )
public class PlotsInfo extends Option
{
    public static final Color[] POSSIBLE_COLORS = new Color[] {new Color(0xFF, 0x55, 0x55), new Color(0x55, 0x55, 0xFF),
            new Color(0x00, 0xFF, 0x00), Color.pink, ChartColor.DARK_RED, ChartColor.DARK_BLUE, ChartColor.DARK_GREEN,
            ChartColor.DARK_MAGENTA, ChartColor.DARK_CYAN, Color.darkGray, ChartColor.VERY_DARK_RED, ChartColor.VERY_DARK_BLUE,
            ChartColor.VERY_DARK_GREEN, ChartColor.VERY_DARK_YELLOW, ChartColor.VERY_DARK_MAGENTA, ChartColor.VERY_DARK_CYAN};

    private PlotInfo[] plots;
    private EModel emodel;

    public PlotsInfo(EModel emodel, PlotInfo[] plots)
    {
        this.emodel = emodel;
        this.plots = plots;
    }

    public PlotsInfo(EModel emodel)
    {
        this(emodel, new PlotInfo[] {new PlotInfo(emodel)});
    }

    public PlotsInfo clone(EModel emodel)
    {
        return new PlotsInfo(emodel, StreamEx.of(getPlots()).map(p -> p.clone(emodel)).toArray(PlotInfo[]::new));
    }
    
    public void setEModel(EModel emodel)
    {
        this.emodel = emodel;
        for (PlotInfo plotInfo: plots)
            plotInfo.setEModel( emodel );
    }

    @PropertyName ( "Plots" )
    public PlotInfo[] getPlots()
    {
        return plots;
    }
    public void setPlots(PlotInfo[] plots)
    {
        PlotInfo[] oldValue = this.plots;
        this.plots = plots;

        for( PlotInfo plot : plots )
        {
            if( !emodel.equals(plot.getEModel()) )
                plot.setEModel(emodel);
        }
        this.firePropertyChange( "plots", oldValue, plots );
    }

    public PlotInfo[] getActivePlots()
    {
        return StreamEx.of(plots).filter(p -> p.isActive).toArray(PlotInfo[]::new);
    }

    public static class AutoPenSelector
    {
        private static final BasicStroke DEFAULT_STROKE = new BasicStroke( 1.0f );
        private static final Pen[] POSSIBLE_PENS;
        private static final int INNER_SIZE;
        static
        {
            String[] strokesStrs = Pen.getAvailableStrokes();
            BasicStroke[] strokes;
            if( strokesStrs == null || strokesStrs.length == 0 )
                strokes = new BasicStroke[] {DEFAULT_STROKE};
            else
                strokes = Stream.of( strokesStrs ).map( s -> Pen.getStrokeByName( DEFAULT_STROKE, s ) ).toArray( BasicStroke[]::new );
            List<Pen> pens = new ArrayList<>();
            for( BasicStroke stroke : strokes )
                for( Color color : PlotsInfo.POSSIBLE_COLORS )
                    pens.add( new Pen( stroke, color ) );
            POSSIBLE_PENS = pens.toArray( new Pen[0] );
            INNER_SIZE = pens.size();
        }

        private int index = 0;
        public int getNumber()
        {
            return index;
        }
        public void updateNumber(int newCount)
        {
            index = Math.abs( newCount % INNER_SIZE );
        }

        public Pen getNextPen()
        {
            if( index >= INNER_SIZE )
                index = index % INNER_SIZE;
            return POSSIBLE_PENS[index++].clone();
        }
    }
}