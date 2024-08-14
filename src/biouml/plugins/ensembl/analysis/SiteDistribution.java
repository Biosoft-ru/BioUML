package biouml.plugins.ensembl.analysis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.NavigableSet;
import java.util.TreeSet;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.FolderCollection;
import ru.biosoft.access.ImageDataElement;
import ru.biosoft.access.sql.SqlConnectionPool;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.graphics.chart.Chart;
import ru.biosoft.graphics.chart.ChartOptions;
import ru.biosoft.graphics.chart.ChartSeries;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.bean.BeanInfoEx2;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.plugins.ensembl.access.EnsemblSequenceTransformer;
import biouml.standard.type.Species;

public class SiteDistribution extends AnalysisMethodSupport<SiteDistribution.Parameters>
{
    public SiteDistribution(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        DataCollectionUtils.createSubCollection( parameters.getOutput() );

        final Histogram tssHist = new Histogram( "Transcription start site" );
        final Histogram tesHist = new Histogram( "Transcription end site" );

        DataElementPath ensemblPath = TrackUtils.getEnsemblPath( parameters.getSpecies(), parameters.getOutput() );
        final DataElementPath sequencesPath = TrackUtils.getPrimarySequencesPath(ensemblPath);

        final Track inputTrack = parameters.getInputTrack().getDataElement(Track.class);

        jobControl.forCollection( sequencesPath.getChildren(), chromosomePath -> {
            try
            {

                NavigableSet<GenomicAnchor> tssAnchors = loadTSS( chromosomePath );
                NavigableSet<GenomicAnchor> tesAnchors = loadTES( chromosomePath );

                Sequence sequence = chromosomePath.getDataElement(AnnotatedSequence.class).getSequence();
                DataCollection<Site> sites = inputTrack.getSites( chromosomePath.toString(), sequence.getStart(), sequence.getStart()
                        + sequence.getLength() );

                for( Site s : sites )
                {
                    GenomicAnchor nearest = GenomicAnchor.getNearest( tssAnchors, s );
                    if( nearest != null )
                        tssHist.add( nearest.getDistance( s ) );

                    nearest = GenomicAnchor.getNearest( tesAnchors, s );
                    if( nearest != null )
                        tesHist.add( nearest.getDistance( s ) );
                }
            }
            catch( Exception e )
            {
                log.log(Level.SEVERE,  "Can not process " + chromosomePath, e );
                return false;
            }
            return true;
        } );


        return new Object[] {tssHist.createTable(), tssHist.createPlot(), tesHist.createTable(), tesHist.createPlot()};
    }


    private NavigableSet<GenomicAnchor> loadTSS(DataElementPath chromosomePath) throws SQLException
    {
        return loadTranscriptsBoundary( chromosomePath, true );
    }

    private NavigableSet<GenomicAnchor> loadTES(DataElementPath chromosomePath) throws SQLException
    {
        return loadTranscriptsBoundary( chromosomePath, false );
    }

    private NavigableSet<GenomicAnchor> loadTranscriptsBoundary(DataElementPath chromosomePath, boolean startSite) throws SQLException
    {
        NavigableSet<GenomicAnchor> anchors = new TreeSet<>();

        DataCollection<?> chromosomeCollection = chromosomePath.getParentCollection();
        String coordSystemConstraints = EnsemblSequenceTransformer.getCoordSystemConstraints(chromosomeCollection);
        Connection con = SqlConnectionPool.getConnection( chromosomeCollection );

        try (Statement st = con.createStatement();
                ResultSet rs = st.executeQuery( "SELECT seq_region_start, seq_region_end, seq_region_strand"
                        + " FROM transcript JOIN seq_region on(transcript.seq_region_id=seq_region.seq_region_id)" + " WHERE name='"
                        + chromosomePath.getName() + "' AND " + coordSystemConstraints ))
        {
            while( rs.next() )
            {
                int start = rs.getInt( 1 );
                int end = rs.getInt( 2 );
                int strand = rs.getInt( 3 );
                boolean reverse = startSite ? strand == -1 : strand == 1;
                anchors.add( new GenomicAnchor( reverse ? end : start, reverse ) );
            }
        }

        return anchors;
    }

    private class Histogram
    {
        private final int[] counts = new int[parameters.getWindowWidth()];
        private final String name;

        public Histogram(String name)
        {
            this.name = name;
        }

        public void add(int distance)
        {
            int offset = distance + parameters.getWindowWidth() / 2;
            if( offset < 0 || offset >= parameters.getWindowWidth() )
                return;
            counts[offset]++;
        }

        public TableDataCollection createTable() throws Exception
        {
            DataElementPath tablePath = parameters.getOutput().getChildPath( name );
            TableDataCollection table = TableDataCollectionUtils.createTableDataCollection( tablePath );
            table.getColumnModel().addColumn( "Distance", Integer.class );
            table.getColumnModel().addColumn( "Count", Integer.class );
            for( int i = 0; i < counts.length; i++ )
                TableDataCollectionUtils.addRow( table, String.valueOf( i ), new Object[] {i - parameters.getWindowWidth() / 2, counts[i]},
                        true );
            table.finalizeAddition();
            tablePath.save( table );
            return table;
        }

        public ImageDataElement createPlot() throws Exception
        {
            double[] x = new double[parameters.getWindowWidth()];
            double[] y = new double[parameters.getWindowWidth()];
            for( int i = 0; i < parameters.getWindowWidth(); i++ )
            {
                x[i] = i - parameters.getWindowWidth() / 2;
                y[i] = counts[i];
            }
            ChartSeries series = new ChartSeries( x, y );
            series.setLabel( "Distribution of distances from site center to nearest " + name.toLowerCase() );

            ChartOptions options = new ChartOptions();
            options.getXAxis().setLabel( "Distance (bp)" );
            options.getXAxis().setMin( (double) -parameters.getWindowWidth() / 2 );
            options.getXAxis().setMax( (double)parameters.getWindowWidth() / 2 );
            options.getYAxis().setLabel( "Count" );

            Chart chart = new Chart();
            chart.addSeries( series );
            chart.setOptions( options );

            DataElementPath imagePath = parameters.getOutput().getChildPath( name + " chart" );
            ImageDataElement image = new ImageDataElement( imagePath.getName(), imagePath.optParentCollection(), chart.getImage( 800, 400 ) );
            imagePath.save( image );
            return image;
        }
    }

    private static class GenomicAnchor implements Comparable<GenomicAnchor>
    {
        int position;
        boolean reverse;

        public GenomicAnchor(int position)
        {
            this( position, false );
        }

        public GenomicAnchor(int position, boolean reverse)
        {
            this.position = position;
            this.reverse = reverse;
        }

        public int getDistance(Site site)
        {
            int center = site.getInterval().getCenter();
            int distance = center - position;
            return reverse ? -distance : distance;
        }

        public static GenomicAnchor getNearest(NavigableSet<GenomicAnchor> anchors, Site site)
        {
            GenomicAnchor center = new GenomicAnchor( ( site.getFrom() + site.getTo() ) / 2 );

            GenomicAnchor left = anchors.floor( center );
            GenomicAnchor right = anchors.ceiling( center );

            if( left == null )
                return right;
            else if( right == null )
                return left;
            return center.position - left.position < right.position - center.position ? left : right;
        }

        @Override
        public int compareTo(GenomicAnchor that)
        {
            return this.position - that.position;
        }
    }


    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath inputTrack;
        private int windowWidth = 1000;
        private DataElementPath output;
        private Species species = Species.getDefaultSpecies( null );

        @PropertyName ( "Input track" )
        @PropertyDescription ( "Input track" )
        public DataElementPath getInputTrack()
        {
            return inputTrack;
        }
        public void setInputTrack(DataElementPath inputTrack)
        {
            Object oldValue = this.inputTrack;
            this.inputTrack = inputTrack;
            firePropertyChange( "inputTrack", oldValue, inputTrack );
        }


        @PropertyName ( "Window width" )
        @PropertyDescription ( "The width of window around anchor point (transcription start site, transcription end site)" )
        public int getWindowWidth()
        {
            return windowWidth;
        }
        public void setWindowWidth(int windowWidth)
        {
            Object oldValue = this.windowWidth;
            this.windowWidth = windowWidth;
            firePropertyChange( "windowWidth", oldValue, windowWidth );
        }

        @PropertyName ( "Specie" )
        @PropertyDescription ( "Specie" )
        public Species getSpecies()
        {
            return species;
        }
        public void setSpecies(Species species)
        {
            Object oldValue = this.species;
            this.species = species;
            firePropertyChange( "species", oldValue, species );
        }
        @PropertyName ( "Output folder" )
        @PropertyDescription ( "Output folder" )
        public DataElementPath getOutput()
        {
            return output;
        }
        public void setOutput(DataElementPath output)
        {
            Object oldValue = this.output;
            this.output = output;
            firePropertyChange( "output", oldValue, output );
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
            property( "inputTrack" ).inputElement( Track.class ).add();
            add( DataElementComboBoxSelector.registerSelector( "species", beanClass, Species.SPECIES_PATH) );
            add( "windowWidth" );
            property( "output" ).outputElement( FolderCollection.class ).auto( "$inputTrack$ site distribution" ).add();
        }
    }
}
