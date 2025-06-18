package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.NavigableMap;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;

import one.util.streamex.DoubleStreamEx;
import one.util.streamex.IntStreamEx;
import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.exception.ExceptionRegistry;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.access.sql.SqlConnectionHolder;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.util.TextUtil2;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.standard.type.Species;

public class GeneFeatures extends AnalysisMethodSupport<GeneFeatures.Parameters>
{
    public GeneFeatures(DataCollection<?> origin, String name)
    {
        super(origin, name, new Parameters());
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        final TableDataCollection result = TableDataCollectionUtils.createTableDataCollection(parameters.getOutputPath());

        ColumnModel columnModel = result.getColumnModel();
        columnModel.addColumn("Gene", String.class);
        columnModel.addColumn("Transcription factor", String.class);
        columnModel.addColumn("Distance", String.class);
        for( String scoreProperty : TextUtil2.split( parameters.getScoreProperty(), ',' ) )
            columnModel.addColumn(scoreProperty, Double.class);

        Collection<ChIPseqExperiment> experiments = findChipSeqExperiments();
        log.info("Found " + experiments.size() + " experiments");

        final Map<String, NavigableMap<Integer, String>> tssSet = getTSS();

        if( parameters.getOtherSites() != null )
        {
            Track otherSites = parameters.getOtherSites().getDataElement(Track.class);
            processTrack(otherSites, tssSet, result, null);
        }

        jobControl.forCollection(experiments, e -> {
            try
            {
                Track peaks = e.getPeak().getDataElement(Track.class);
                processTrack(peaks, tssSet, result, e.getTfUniprotId());
            }
            catch( Exception ex )
            {
                throw ExceptionRegistry.translateException(ex);
            }
            return true;
        });

        parameters.getOutputPath().save(result);

        TableDataCollection geneCentricTable;
        try
        {
            geneCentricTable = createGeneCentricTable(result);
        }
        catch( Exception e )
        {
            log.log(Level.SEVERE, "createGeneCentricTable", e);
            throw e;
        }
        return new Object[] {result, geneCentricTable};
    }

    private TableDataCollection createGeneCentricTable(TableDataCollection source) throws Exception
    {

        Interval[] intervals = parameters.getIntervalArray();
        String scoreProperty = TextUtil2.split( parameters.getScoreProperty(), ',' )[0];

        Map<String, GeneInfo> genes = new HashMap<>();
        Set<String> factorNames = new HashSet<>();
        for( RowDataElement row : source )
        {
            String gene = row.getValueAsString("Gene");
            String factorName = row.getValueAsString("Transcription factor");
            factorNames.add(factorName);
            int distance = Integer.parseInt(row.getValueAsString("Distance"));
            Number score = (Number)row.getValue(scoreProperty);
            genes.computeIfAbsent(gene, k -> new GeneInfo(gene, intervals)).addFeature(factorName, distance, score.doubleValue());
        }

        TableDataCollection result = TableDataCollectionUtils.createTableDataCollection(parameters.getGeneCentricOutputPath());

        ColumnModel columnModel = result.getColumnModel();
        for( String factorName : factorNames )
            for( Interval interval : intervals )
                columnModel.addColumn(factorName + interval, Double.class);


        for( GeneInfo geneInfo : genes.values() )
        {
            Object[] values = StreamEx.of( factorNames )
                    .flatMap( factorName -> IntStreamEx.ofIndices( intervals ).mapToObj( j -> geneInfo.getFeature( factorName, j ) ) )
                    .toArray();
            TableDataCollectionUtils.addRow(result, geneInfo.gene, values, true);
        }

        result.finalizeAddition();

        parameters.getGeneCentricOutputPath().save(result);
        return result;
    }

    private void processTrack(Track track, Map<String, NavigableMap<Integer, String>> tssSet, TableDataCollection result, String name)
            throws Exception
    {
        int resultSize = result.getSize();
        for( Site site : track.getAllSites() )
        {
            String chr = site.getSequence().getName();
            int center = site.getInterval().getCenter();
            if( parameters.isUseSummit() )
            {
                try
                {
                    center = site.getFrom() + (Integer)site.getProperties().getValue("summit");
                }
                catch( Exception e )
                {

                }
            }

            Integer left = tssSet.get(chr).floorKey(center);
            Integer right = tssSet.get(chr).higherKey(center);

            if( left == null && right == null )
                continue;

            int leftDistance = left == null ? Integer.MAX_VALUE : center - left;
            int rightDistance = right == null ? Integer.MAX_VALUE : center - right;


            int distance;
            String gene;
            if( Math.abs(leftDistance) < Math.abs(rightDistance) )
            {
                distance = leftDistance;
                gene = tssSet.get(chr).get(left);
            }
            else
            {
                distance = rightDistance;
                gene = tssSet.get(chr).get(right);
            }

            if( Math.abs(distance) <= parameters.getMaxTSSDistance() )
            {
                String[] scoreProperties = TextUtil2.split( parameters.getScoreProperty(), ',' );

                Object[] values = new Object[3 + scoreProperties.length];
                values[0] = gene;
                values[1] = name == null ? site.getProperties().getValueAsString("name") : name;
                values[2] = distance;
                for( int i = 0; i < scoreProperties.length; i++ )
                    try
                    {
                        values[i + 3] = site.getProperties().getValue(scoreProperties[i]);
                    }
                    catch( Exception e )
                    {
                    }

                TableDataCollectionUtils.addRow(result, String.valueOf(resultSize++), values, true);
            }

        }
        result.finalizeAddition();
    }


    private Collection<ChIPseqExperiment> findChipSeqExperiments()
    {
        List<ChIPseqExperiment> result = new ArrayList<>();
        DataCollection<ChIPseqExperiment> experimentCollection = parameters.getExperimentsPath().getDataCollection(ChIPseqExperiment.class);
        for( ChIPseqExperiment e : experimentCollection )
        {
            if( !e.isControlExperiment() && e.getSpecie().equals(parameters.getSpecie())
                    && e.getCell().getTitle().equalsIgnoreCase(parameters.getCellLine()) && e.getPeak().exists()
                    && ( e.getTreatment() == null || e.getTreatment().isEmpty() || e.getTreatment().equalsIgnoreCase("None") ) )
            {

                result.add(e);
            }
        }
        return result;
    }

    private Map<String, NavigableMap<Integer, String>> getTSS() throws SQLException
    {
        Map<String, NavigableMap<Integer, String>> result = new HashMap<>();

        String ensembl = parameters.getSpecie().getAttributes().getValue("ensemblPath").toString();

        Connection connection = ( (SqlConnectionHolder)DataElementPath.create(ensembl).getDataElement() ).getConnection();

        Statement st = null;
        ResultSet rs = null;
        try
        {
            st = connection.createStatement();
            rs = st.executeQuery("select seq_region.name, if(seq_region_strand=1,seq_region_start,seq_region_end), gene_stable_id.stable_id "
                    + "from transcript join seq_region using(seq_region_id) join gene_stable_id using(gene_id)");
            while( rs.next() )
            {
                String chr = rs.getString(1);
                int pos = rs.getInt(2);
                String gene = rs.getString(3);

                result.computeIfAbsent(chr, k -> new TreeMap<>()).put(pos, gene);
            }
        }
        finally
        {
            SqlUtil.close(st, rs);
        }

        return result;
    }

    public static class Parameters extends AbstractAnalysisParameters
    {
        private String cellLine;
        private Species specie = Species.getDefaultSpecies(null);
        private int maxTSSDistance = 10000;
        private DataElementPath experimentsPath = DataElementPath.create("data/Collaboration/GTRD/Data/experiments");
        private DataElementPath otherSites;
        private DataElementPath outputPath;
        private DataElementPath geneCentricOutputPath;
        private String scoreProperty = Site.SCORE_PROPERTY;
        private boolean useSummit = false;
        private String intervals = "(-10000,-5000);(-4999,-1000);(-999,-500);(-499,-200);(-199,-100);(-99,0);(1,100);(101,200);(201,500);(501,1000);(1001,5000);(5000,10000)";

        public String getCellLine()
        {
            return cellLine;
        }
        public void setCellLine(String cellLine)
        {
            Object oldValue = this.cellLine;
            this.cellLine = cellLine;
            firePropertyChange("cellLine", oldValue, cellLine);
        }

        public DataElementPath getOtherSites()
        {
            return otherSites;
        }
        public void setOtherSites(DataElementPath otherSites)
        {
            Object oldValue = this.otherSites;
            this.otherSites = otherSites;
            firePropertyChange("otherSites", oldValue, otherSites);
        }
        public Species getSpecie()
        {
            return specie;
        }
        public void setSpecie(Species specie)
        {
            Object oldValue = this.specie;
            this.specie = specie;
            firePropertyChange("specie", oldValue, specie);
        }

        public int getMaxTSSDistance()
        {
            return maxTSSDistance;
        }
        public void setMaxTSSDistance(int maxTSSDistance)
        {
            Object oldValue = this.maxTSSDistance;
            this.maxTSSDistance = maxTSSDistance;
            firePropertyChange("maxTSSDistance", oldValue, maxTSSDistance);
        }

        public DataElementPath getExperimentsPath()
        {
            return experimentsPath;
        }
        public void setExperimentsPath(DataElementPath experimentsPath)
        {
            Object oldValue = this.experimentsPath;
            this.experimentsPath = experimentsPath;
            firePropertyChange("experimentsPath", oldValue, experimentsPath);
        }

        public DataElementPath getOutputPath()
        {
            return outputPath;
        }
        public void setOutputPath(DataElementPath outputPath)
        {
            Object oldValue = this.outputPath;
            this.outputPath = outputPath;
            firePropertyChange("outputPath", oldValue, outputPath);
        }

        public DataElementPath getGeneCentricOutputPath()
        {
            return geneCentricOutputPath;
        }
        public void setGeneCentricOutputPath(DataElementPath geneCentricOutputPath)
        {
            Object oldValue = this.geneCentricOutputPath;
            this.geneCentricOutputPath = geneCentricOutputPath;
            firePropertyChange("geneCentricOutputPath", oldValue, geneCentricOutputPath);
        }
        public String getScoreProperty()
        {
            return scoreProperty;
        }
        public void setScoreProperty(String scoreProperty)
        {
            Object oldValue = this.scoreProperty;
            this.scoreProperty = scoreProperty;
            firePropertyChange("scoreProperty", oldValue, scoreProperty);
        }

        public boolean isUseSummit()
        {
            return useSummit;
        }
        public void setUseSummit(boolean useSummit)
        {
            Object oldValue = this.useSummit;
            this.useSummit = useSummit;
            firePropertyChange("useSummit", oldValue, useSummit);
        }

        public String getIntervals()
        {
            return intervals;
        }
        public void setIntervals(String intervals)
        {
            Object oldValue = this.intervals;
            this.intervals = intervals;
            firePropertyChange("intervals", oldValue, intervals);
        }

        public Interval[] getIntervalArray()
        {
            return StreamEx.split(intervals, ';').map( Interval::new ).toArray( Interval[]::new );
        }
    }

    public static class ParametersBeanInfo extends BeanInfoEx2<Parameters>
    {
        public ParametersBeanInfo()
        {
            super(Parameters.class);
        }

        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementComboBoxSelector.registerSelector("specie", beanClass, Species.SPECIES_PATH));
            add("cellLine");
            add("maxTSSDistance");
            add("scoreProperty");
            add("useSummit");
            add("intervals");
            add(DataElementPathEditor.registerInputChild("experimentsPath", beanClass, ChIPseqExperiment.class));
            property( "otherSites" ).inputElement( Track.class ).canBeNull().add();
            property( "outputPath" ).outputElement( TableDataCollection.class ).add();
            property( "geneCentricOutputPath" ).outputElement( TableDataCollection.class ).add();
        }
    }

    private static class GeneInfo
    {
        String gene;
        Interval[] intervals;
        Map<String, double[]> features;

        public GeneInfo(String gene, Interval[] intervals)
        {
            this.gene = gene;
            this.intervals = intervals;
            features = new HashMap<>();
        }

        public void addFeature(String factorName, int distance, double score)
        {
            double[] factorFeatures = features.computeIfAbsent( factorName,
                    k -> DoubleStreamEx.constant( Double.NaN, intervals.length ).toArray() );
            for( int i = 0; i < factorFeatures.length; i++ )
            {
                if( intervals[i].inside(distance) )
                {
                    double oldValue = factorFeatures[i];
                    if( Double.isNaN(oldValue) || oldValue < score )
                        factorFeatures[i] = score;
                }
            }
        }

        public double getFeature(String factorName, int intervalId)
        {
            if( !features.containsKey(factorName) )
                return Double.NaN;
            return features.get(factorName)[intervalId];
        }
    }
}
