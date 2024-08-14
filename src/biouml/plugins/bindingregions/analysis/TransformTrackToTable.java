package biouml.plugins.bindingregions.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang.ArrayUtils;

import com.developmentontheedge.beans.DynamicPropertySet;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.annot.PropertyDescription;
import com.developmentontheedge.beans.annot.PropertyName;
import com.developmentontheedge.beans.editors.StringTagEditor;

import biouml.plugins.bindingregions.resources.MessageBundle;
import biouml.plugins.bindingregions.utils.ChipSeqPeak;
import biouml.plugins.bindingregions.utils.SequenceSampleUtils;
import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisJobControl;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.AnnotatedSequence;
import ru.biosoft.bsa.BasicGenomeSelector;
import ru.biosoft.bsa.Interval;
import ru.biosoft.bsa.LinearSequence;
import ru.biosoft.bsa.Sequence;
import ru.biosoft.bsa.SequenceRegion;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.datatype.DataType;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.GenericMultiSelectEditor;

/**
 * @author yura
 * 
 * Transform track to table: Create data matrix with sequence sample and/or variables values.
 * Variables are stored as dynamic properties of sites in track.
 * Particular example of track is GTRD track with ChIP-Seq peaks.
 * 
 * Under construction
 * TODO: Currently sequences are unaltered; to extend to general 'sequenceSampleType'

 */
public class TransformTrackToTable extends AnalysisMethodSupport<TransformTrackToTable.TransformTrackToTableParameters>
{
    private static final String SEQUENCE_SAMPLE = "Sequence sample";
    private static final String DATA_IN_TRACK = "Values of variables in track (variables are stored as dynamic properties of sites in track)";
    
    public TransformTrackToTable(DataCollection<?> origin, String name)
    {
        super(origin, name, new TransformTrackToTableParameters());
    }
    
    @Override
    public TableDataCollection justAnalyzeAndPut() throws Exception
    {
        log.info("Transform track to table: Create data matrix with sequence sample and/or variables values");
        log.info("Variables are dynamic properties of sites in track");
        log.info("Particular example of track is GTRD track with ChIP-Seq peaks");
        
        DataElementPath pathToTrack = parameters.getPathToTrack();
        String[] tableContent = parameters.getTableContent();
        String[] variableNames = parameters.getVariableNames();
        DataElementPath pathToSequences = parameters.getDbSelector().getSequenceCollectionPath();
        String sequenceSampleType = parameters.getSequenceSampleType();
        int specifiedLengthOfSequence = parameters.getSpecifiedLengthOfSequence();
        DataElementPath pathToOutputTable = parameters.getPathToOutputTable();
        
        // 1. Input parameter correction on hiddenness
        if( parameters.isSequenceSampleHidden() )
        {
            pathToSequences = null;
            sequenceSampleType = null;
        }
        if( parameters.isVariableNamesHidden() )
            variableNames = null;
        if( parameters.isSpecifiedLengthOfSequenceHidden() )
            specifiedLengthOfSequence = 0;
        
        // 2. Implementation
        Track track = pathToTrack.getDataElement(Track.class);
        return writeSitePropertiesAndSequenceSampleIntoTable(track, variableNames, sequenceSampleType, specifiedLengthOfSequence, pathToSequences, pathToOutputTable, jobControl, 0, 100);
    }

    private TableDataCollection writeSitePropertiesAndSequenceSampleIntoTable(Track track, String[] propertiesNames, String sequenceSampleType, int specifiedLengthOfSequence, DataElementPath pathToSequences, DataElementPath pathToOutputTable, AnalysisJobControl jobControl, int from, int to)
    {
        // 1. Table initialization
        int iJobControl = 0, difference = to - from, index = 0;
        TableDataCollection table = TableDataCollectionUtils.createTableDataCollection(pathToOutputTable);
        table.getColumnModel().addColumn("chromosome", String.class);
        table.getColumnModel().addColumn("position 'from'", Integer.class);
        table.getColumnModel().addColumn("position 'to'", Integer.class);
        table.getColumnModel().addColumn("length", Integer.class);
        if( propertiesNames != null )
            for( String s : propertiesNames )
                table.getColumnModel().addColumn(s, Double.class);
        if( sequenceSampleType != null )
            table.getColumnModel().addColumn(sequenceSampleType, String.class);
        
        // 2. Table creation
        DataCollection<Site> sites = track.getAllSites();
        int n = sites.getSize();
        for( Site site : sites )
        {
            // 2a. Table creation : add values
            if( jobControl != null )
                jobControl.setPreparedness(from + ++iJobControl * difference / n);
            String chromosome = site.getSequence().getName();
            if( chromosome.equals("MT") || chromosome.equals("M") || chromosome.equals("EBV") || chromosome.contains("_") || site.getInterval().getLength() <= 0 || site.getStart() < 1 ) continue;
            Interval siteInterval = site.getInterval();
            if( sequenceSampleType != null )
            {
                int summit = sequenceSampleType.equals( SequenceSampleUtils.SEQUENCE_TYPE_AROUND_SUMMIT_AND_GIVEN_LENGTH )
                        ? Integer.parseInt( (String)site.getProperties().getValue( ChipSeqPeak.SUMMIT_PROPERTY ) ) : 0;
                siteInterval = SequenceSampleUtils.changeIntervalAppropriately(siteInterval, sequenceSampleType, specifiedLengthOfSequence, summit);
            }
            List<Object> rowElements = new ArrayList<>(Arrays.asList(chromosome, siteInterval.getFrom(), siteInterval.getTo(), siteInterval.getLength()));
            if( propertiesNames != null )
            {
                DynamicPropertySet properties = site.getProperties();
                for( String propertyName : propertiesNames )
                {
//                  Number number = (Number)properties.getValue(propertyName);
//                  Number number = Double.valueOf((String)properties.getValue(propertyName));
//                  rowElements.add(number == null ? Double.NaN : number.doubleValue());
                    rowElements.add( DataType.Float.convertValue( properties.getValue( propertyName ) ) );
                }
            }
            
            // 2b. Table creation : add sequences
            if( sequenceSampleType != null )
            {
                Sequence fullChromosome = pathToSequences.getChildPath(chromosome).getDataElement(AnnotatedSequence.class).getSequence();
                if( siteInterval.getFrom() < 1 || siteInterval.getTo() > fullChromosome.getLength() ) continue;
                String seq = new LinearSequence(new SequenceRegion(fullChromosome, siteInterval, false, false)).toString();
                rowElements.add(seq);
            }
            TableDataCollectionUtils.addRow(table, "site_" + Integer.toString(index++), rowElements.toArray());
        }
        table.finalizeAddition();
        CollectionFactoryUtils.save(table);
        return table;
    }
    
    public static class TransformTrackToTableParameters extends AbstractAnalysisParameters
    {
        private BasicGenomeSelector dbSelector;
        private DataElementPath pathToTrack;
        private String[] tableContent;
        private String[] variableNames;
        private String sequenceSampleType;
        private int specifiedLengthOfSequence;
        private DataElementPath pathToOutputTable;
        
        public TransformTrackToTableParameters()
        {
            setDbSelector(new BasicGenomeSelector());
        }
        
        @PropertyName(MessageBundle.PN_DB_SELECTOR)
        @PropertyDescription(MessageBundle.PD_DB_SELECTOR)
        public BasicGenomeSelector getDbSelector()
        {
            return dbSelector;
        }
        public void setDbSelector(BasicGenomeSelector dbSelector)
        {
            Object oldValue = this.dbSelector;
            this.dbSelector = dbSelector;
            dbSelector.setParent(this);
            firePropertyChange("dbSelector", oldValue, dbSelector);
        }

        @PropertyName(MessageBundle.PN_TRACK_PATH)
        @PropertyDescription(MessageBundle.PD_TRACK_PATH_PEAK_FINDER)
        public DataElementPath getPathToTrack()
        {
            return pathToTrack;
        }
        public void setPathToTrack(DataElementPath pathToTrack)
        {
            Object oldValue = this.pathToTrack;
            this.pathToTrack = pathToTrack;
//          firePropertyChange("pathToTrack", oldValue, pathToTrack);
            firePropertyChange("*", oldValue, pathToTrack);
        }
        
        @PropertyName(MessageBundle.PN_TABLE_CONTENT)
        @PropertyDescription(MessageBundle.PD_TABLE_CONTENT)
        public String[] getTableContent()
        {
            return tableContent;
        }
        public void setTableContent(String[] tableContent)
        {
            Object oldValue = this.tableContent;
            this.tableContent = tableContent;
//          firePropertyChange("tableContent", oldValue, tableContent);
            firePropertyChange("*", oldValue, tableContent);
        }
        
        @PropertyName(MessageBundle.PN_VARIABLE_NAMES)
        @PropertyDescription(MessageBundle.PD_VARIABLE_NAMES)
        public String[] getVariableNames()
        {
            return variableNames;
        }
        public void setVariableNames(String[] variableNames)
        {
            Object oldValue = this.variableNames;
            this.variableNames = variableNames;
            firePropertyChange("variableNames", oldValue, variableNames);
        }
        
        @PropertyName(MessageBundle.PN_SEQUENCE_SAMPLE_TYPE)
        @PropertyDescription(MessageBundle.PD_SEQUENCE_SAMPLE_TYPE)
        public String getSequenceSampleType()
        {
            return sequenceSampleType;
        }
        public void setSequenceSampleType(String sequenceSampleType)
        {
            Object oldValue = this.sequenceSampleType;
            this.sequenceSampleType = sequenceSampleType;
//          firePropertyChange("sequenceSampleType", oldValue, sequenceSampleType);
            firePropertyChange("*", oldValue, sequenceSampleType);
        }

        @PropertyName(MessageBundle.PN_SPECIFIED_LENGTH_OF_SEQUENCE)
        @PropertyDescription(MessageBundle.PD_SPECIFIED_LENGTH_OF_SEQUENCE)
        public int getSpecifiedLengthOfSequence()
        {
            return specifiedLengthOfSequence;
        }
        public void setSpecifiedLengthOfSequence(int specifiedLengthOfSequence)
        {
            Object oldValue = this.specifiedLengthOfSequence;
            this.specifiedLengthOfSequence = specifiedLengthOfSequence;
            firePropertyChange("specifiedLengthOfSequence", oldValue, specifiedLengthOfSequence);
        }
        
        @PropertyName(MessageBundle.PN_OUTPUT_TABLE_PATH)
        @PropertyDescription(MessageBundle.PD_OUTPUT_TABLE_PATH)
        public DataElementPath getPathToOutputTable()
        {
            return pathToOutputTable;
        }
        public void setPathToOutputTable(DataElementPath pathToOutputTable)
        {
            Object oldValue = this.pathToOutputTable;
            this.pathToOutputTable = pathToOutputTable;
            firePropertyChange("pathToOutputTable", oldValue, pathToOutputTable);
        }
        
        public boolean isSequenceSampleHidden()
        {
            return ! ArrayUtils.contains(getTableContent(), SEQUENCE_SAMPLE);
        }
        
        public boolean isVariableNamesHidden()
        {
            return ! ArrayUtils.contains(getTableContent(), DATA_IN_TRACK);
        }
        
        public boolean isSpecifiedLengthOfSequenceHidden()
        {
            if( ! ArrayUtils.contains(getTableContent(), SEQUENCE_SAMPLE) ) return true;
            String sequenceSampleType = getSequenceSampleType();
//          return ! sequenceSampleType.equals(SequenceSampleUtils.SEQUENCE_TYPE_UNALTERED_CENTERS_AND_GIVEN_LENGTH) && ! sequenceSampleType.equals(SequenceSampleUtils.SEQUENCE_TYPE_SHORT_PROLONGATED) && ! sequenceSampleType.equals(SequenceSampleUtils.SEQUENCE_TYPE_AROUND_SUMMIT_AND_GIVEN_LENGTH);
            return sequenceSampleType == null || sequenceSampleType.equals(SequenceSampleUtils.SEQUENCE_TYPE_UNALTERED);
        }
    }
    
    public static class SequenceSampleTypeSelector extends StringTagEditor
    {
        @Override
        public String[] getTags()
        {
//          return SequenceSampleUtils.getAvailableSequenceSampleTypes();
            String[] sequenceSampleTypes = SequenceSampleUtils.getAvailableSequenceSampleTypes();
            Track track = ((TransformTrackToTableParameters)getBean()).getPathToTrack().getDataElement(Track.class);
            String[] variableNames = SequenceSampleUtils.getAvailablePropertiesNames(track);
            if( ! ArrayUtils.contains(variableNames, ChipSeqPeak.SUMMIT_PROPERTY) )
                sequenceSampleTypes = (String[])ArrayUtils.removeElement(sequenceSampleTypes, SequenceSampleUtils.SEQUENCE_TYPE_AROUND_SUMMIT_AND_GIVEN_LENGTH);
            return sequenceSampleTypes;
        }
    }
    
    public static class TableContentSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            return new String[]{SEQUENCE_SAMPLE, DATA_IN_TRACK};
        }
    }

    public static class VariableNamesSelector extends GenericMultiSelectEditor
    {
        @Override
        protected String[] getAvailableValues()
        {
            try
            {
                Track track = ((TransformTrackToTableParameters)getBean()).getPathToTrack().getDataElement(Track.class);
                return SequenceSampleUtils.getAvailablePropertiesNames(track);
            }
            catch( Exception e )
            {
                return new String[]{"(please select variable names)"};
            }
        }
    }

    public static class TransformTrackToTableParametersBeanInfo extends BeanInfoEx2<TransformTrackToTableParameters>
    {
        public TransformTrackToTableParametersBeanInfo()
        {
            super(TransformTrackToTableParameters.class);
        }
        
        @Override
        protected void initProperties() throws Exception
        {
            add(DataElementPathEditor.registerInput("pathToTrack", beanClass, Track.class, false));
            add(new PropertyDescriptorEx("tableContent", beanClass), TableContentSelector.class);
            addHidden(new PropertyDescriptorEx("variableNames", beanClass), VariableNamesSelector.class, "isVariableNamesHidden");
            addHidden("dbSelector", "isSequenceSampleHidden");
            addHidden(new PropertyDescriptorEx("sequenceSampleType", beanClass), SequenceSampleTypeSelector.class, "isSequenceSampleHidden");
            addHidden("specifiedLengthOfSequence", "isSpecifiedLengthOfSequenceHidden");
            add(DataElementPathEditor.registerOutput("pathToOutputTable", beanClass, TableDataCollection.class, false));
        }
    }
}

