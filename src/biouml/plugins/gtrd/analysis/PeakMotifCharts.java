package biouml.plugins.gtrd.analysis;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.ImageElement;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.IntervalMap;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.plugins.jsreport.JavaScriptReport;
import ru.biosoft.plugins.jsreport.JavaScriptReport.Report;
import ru.biosoft.util.bean.BeanInfoEx2;

public class PeakMotifCharts extends AnalysisMethodSupport<PeakMotifCharts.Parameters>
{

    public PeakMotifCharts(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        List<PeakAndMotif> data = loadData();
        
        DataCollection<DataElement> result = DataCollectionUtils.createSubCollection(getParameters().getOutputPath());
        
        Report report = new JavaScriptReport().create("Statistics report "+result.getName());
        report.addHeader("Peaks and motifs");
        
        DataElementPath imagePath = DataElementPath.create( result, "Distance between peak and motif centers" );
        ImageElement image = makePeakMotifDistance(imagePath, data);
        addImage( report, image);        
        
        imagePath = DataElementPath.create( result, "Motif and peaks score" );
        image = makeMotifAndPeakScore(imagePath, data);
        addImage( report, image);
        
        imagePath = DataElementPath.create( result, "Motif and peak rank" );
        image = makeMotifAndPeakRank(imagePath, data);
        addImage( report, image);        

        
        DataElementPath reportPath = DataElementPath.create( result, "report" );
        report.store( reportPath.toString() );
        
        writeProperties( result );
        return reportPath.optDataElement();
    }
    
    private ImageElement makeMotifAndPeakRank(DataElementPath imagePath, List<PeakAndMotif> data)
    {
        
        PeakAndMotif[] sorted = data.toArray( new PeakAndMotif[0] );
        Arrays.sort( sorted, (a,b)->Double.compare( b.score, a.score ) );
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries( "" );
        int withMotif = 0;
        for(int i = 0; i < sorted.length; i++)
        {
            if(!sorted[i].motifs.isEmpty())
                withMotif++;
            series.add( i+1, (double)withMotif / (i+1) );
        }
        dataset.addSeries( series );
        
        JFreeChart chart = ChartFactory.createXYLineChart(
                "",         // chart title
                "Peak rank",
                "Fraction with motifs and <= rank",
                dataset,                  // data
                PlotOrientation.VERTICAL, // orientation
                false,                     // include legend
                false,                     // tooltips?
                false                     // URLs?
            );
        
        BufferedImage img = chart.createBufferedImage( 500, 500 );
        ImageDataElement res = new ImageDataElement( imagePath.getName(), imagePath.getParentCollection(), img );
        imagePath.save( res );
        return res;
    }

    private ImageElement makePeakMotifDistance(DataElementPath imagePath, List<PeakAndMotif> data)
    {
        final int BREAK_BY = 10;
        int[] bins = new int[parameters.getMaxDistance()/BREAK_BY];
        int other = 0;
        for(PeakAndMotif p : data)
        {
            if(p.motifs.isEmpty())
            {
                other++;
                continue;
            }
            int minDist = p.motifs.stream()
                    .mapToInt( m->Math.abs( m.region.getCenter() - p.center ) )
                    .min().getAsInt();
            if(minDist >= parameters.getMaxDistance())
                other++;
            else
                bins[minDist/BREAK_BY]++;
                
        }
        
        DefaultCategoryDataset dataSet = new DefaultCategoryDataset();
        for(int i = 0; i < bins.length; i++)
            dataSet.addValue( bins[i], "Count", "["+(i*BREAK_BY) + "-" + (i*BREAK_BY + BREAK_BY) + ")" );
        dataSet.addValue( other, "Count", "Other" );
        
        JFreeChart chart = ChartFactory.createBarChart(
                "",         // chart title
                "Distance [nt]",
                "Count",
                dataSet,                  // data
                PlotOrientation.VERTICAL, // orientation
                false,                     // include legend
                false,                     // tooltips?
                false                     // URLs?
            );
        
        chart.getCategoryPlot().getDomainAxis().setCategoryLabelPositions( CategoryLabelPositions.UP_90  );

        BufferedImage img = chart.createBufferedImage( 500, 500 );
        ImageDataElement res = new ImageDataElement( imagePath.getName(), imagePath.getParentCollection(), img );
        imagePath.save( res );
        return res;
    }
    
    private ImageElement makeMotifAndPeakScore(DataElementPath imagePath, List<PeakAndMotif> data)
    {
        double scoreMin = data.stream().mapToDouble( p->p.score ).min().getAsDouble();
        double scoreMax = data.stream().mapToDouble( p->p.score ).max().getAsDouble();
        
        final int COUNT = 1000;
        int[] motifsInBin = new int[COUNT];
        double w = (scoreMax - scoreMin) / COUNT;
        int[] peaksInBin = new int[COUNT];
        for(PeakAndMotif p : data)
        {
            int bin = (int) ( (p.score - scoreMin)*COUNT/(scoreMax - scoreMin) );
            if(bin == COUNT)
                --bin;
            peaksInBin[bin]++;
            if( !p.motifs.isEmpty() )
                motifsInBin[bin]++;
        }
        
        
        int[] cumTotal = new int[COUNT];
        for(int i = COUNT - 1, cur = 0; i >= 0; i--)
        {
            cur += peaksInBin[i];
            cumTotal[i] = cur;
        }
        int[] cumWithMotif = new int[COUNT];
        for(int i = COUNT - 1, cur = 0; i >= 0; i--)
        {
            cur += motifsInBin[i];
            cumWithMotif[i] = cur;
        }

        double[] fraction = new double[COUNT];
        for(int i = 0; i < COUNT; i++)
            fraction[i] = (double)cumWithMotif[i]/cumTotal[i];
        
        
        XYSeriesCollection dataset = new XYSeriesCollection();
        XYSeries series = new XYSeries( "" );
        for(int i = 0; i < COUNT; i++)
            series.add( scoreMin + i*w , fraction[i] );
        dataset.addSeries( series  );
        
        JFreeChart chart = ChartFactory.createXYLineChart(
                "",         // chart title
                "Peak score",
                "Fraction with motifs and >= score",
                dataset,                  // data
                PlotOrientation.VERTICAL, // orientation
                false,                     // include legend
                false,                     // tooltips?
                false                     // URLs?
            );
        
        BufferedImage img = chart.createBufferedImage( 500, 500 );
        ImageDataElement res = new ImageDataElement( imagePath.getName(), imagePath.getParentCollection(), img );
        imagePath.save( res );
        return res;
    }

    private void addImage(Report report, ImageElement image) throws IOException
    {
        report.addSubHeader(image.getName());
        report.addImage( image.getImage( null ), image.getName(), image.getName() );
    }

    @Override
    protected void writeProperties(DataElement de) throws Exception
    {
        if( de instanceof DataCollection )
        {
            AnalysisParametersFactory.write( de, this );
            CollectionFactoryUtils.save(de);
        }
    }

    
    private static class PeakAndMotif
    {
        String chr;
        Interval region;
        int center;
        double score;
        List<Motif> motifs = new ArrayList<>();
        
        PeakAndMotif(String chr, Interval region)
        {
            this.chr = chr;
            this.region = region;
            this.center = region.getCenter();
        }
    }
    private static class Motif
    {
        Interval region;
        double score;
        
        Motif(Interval region, double score)
        {
            this.region = region;
            this.score = score;
        }
    }
    
    private List<PeakAndMotif> loadData()
    {
        List<PeakAndMotif> result = new ArrayList<>();
        
        Track peaksTrack = parameters.getPeaksTrack().getDataElement( Track.class );
        Track motifsTrack = parameters.getMotifsTrack().getDataElement( Track.class );

        DataElementPathSet chrs = TrackUtils.getTrackSequencesPath( peaksTrack ).getChildren();
        jobControl.forCollection( chrs, chrPath -> {
            Sequence chrSeq = chrPath.getDataElement( AnnotatedSequence.class ).getSequence();
            IntervalMap<PeakAndMotif> peakIndex = new IntervalMap<>();
            
            peaksTrack
            .getSites( chrPath.toString(), chrSeq.getStart(), chrSeq.getStart() + chrSeq.getLength() )
            .stream().map( s->{
                PeakAndMotif p = new PeakAndMotif( chrPath.getName(), s.getInterval() );
                p.score = ((Number)s.getProperties().getValue( parameters.getScoreColumn() )).doubleValue();
                String cCol = parameters.getCenterColumn();
                if(cCol != null && ! cCol.isEmpty())
                {
                    p.center = s.getFrom() + (Integer)s.getProperties().getValue( parameters.getCenterColumn() );
                }
                return p;
            } ).forEach( p->peakIndex.add( p.region.getFrom(), p.region.getTo(), p ));

            motifsTrack
            .getSites( chrPath.toString(), chrSeq.getStart(), chrSeq.getStart() + chrSeq.getLength() )
            .stream()
            .map(s -> {
                double score = ((Number)s.getProperties().getValue( "score" )).doubleValue();
                return new Motif(s.getInterval(), score);
            }).forEach(m->{
                peakIndex
                .getIntervals( m.region.getCenter() - parameters.getMaxDistance(), m.region.getCenter() + parameters.getMaxDistance() )
                .forEach( p -> p.motifs.add( m ) );
            });

            result.addAll( peakIndex.getIntervals( Integer.MIN_VALUE, Integer.MAX_VALUE ) );
            return true;
        } );
        
        return result;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath peaksTrack;
        public DataElementPath getPeaksTrack()
        {
            return peaksTrack;
        }
        public void setPeaksTrack(DataElementPath peaksTrack)
        {
            Object oldValue = this.peaksTrack;
            this.peaksTrack = peaksTrack;
            firePropertyChange( "peaksTrack", oldValue, peaksTrack );
        }
        
        private DataElementPath motifsTrack;
        public DataElementPath getMotifsTrack()
        {
            return motifsTrack;
        }
        public void setMotifsTrack(DataElementPath motifsTrack)
        {
            Object oldValue = this.motifsTrack;
            this.motifsTrack = motifsTrack;
            firePropertyChange( "motifsTrack", oldValue, motifsTrack );
        }
        
        private String scoreColumn;
        public String getScoreColumn()
        {
            return scoreColumn;
        }
        public void setScoreColumn(String scoreColumn)
        {
            Object oldValue = this.scoreColumn;
            this.scoreColumn = scoreColumn;
            firePropertyChange( "scoreColumn", oldValue, scoreColumn );
        }
        
        private String centerColumn;
        public String getCenterColumn()
        {
            return centerColumn;
        }
        public void setCenterColumn(String centerColumn)
        {
            Object oldValue = this.centerColumn;
            this.centerColumn = centerColumn;
            firePropertyChange( "centerColumn", oldValue, centerColumn );
        }
        
        private int maxDistance = 100;
        public int getMaxDistance()
        {
            return maxDistance;
        }
        public void setMaxDistance(int maxDistance)
        {
            int oldValue = this.maxDistance;
            this.maxDistance = maxDistance;
            firePropertyChange( "maxDistance", oldValue, maxDistance );
        }

        private DataElementPath outputPath;
        public DataElementPath getOutputPath()
        {
            return outputPath;
        }
        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange( "outputPath", oldValue, outputPath );
        }
    }
    
    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class );
        }

        @Override
        protected void initProperties() throws Exception
        {
            property("peaksTrack").inputElement( Track.class ).add();
            property("motifsTrack").inputElement( Track.class ).add();
            add("scoreColumn");
            add("centerColumn");
            add("maxDistance");
            property("outputPath").outputElement( FolderCollection.class ).add();
        }
    }
}
