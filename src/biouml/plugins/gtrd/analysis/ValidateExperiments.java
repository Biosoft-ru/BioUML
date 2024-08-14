package biouml.plugins.gtrd.analysis;

import java.sql.Connection;
import java.util.logging.Level;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.apache.commons.lang.StringUtils;

import com.developmentontheedge.beans.BeanInfoEx;

import biouml.plugins.gtrd.ChIPseqExperiment;
import biouml.plugins.gtrd.ChIPseqExperimentSQLTransformer;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.exception.BiosoftSQLException;
import ru.biosoft.access.sql.SqlUtil;
import ru.biosoft.analysiscore.AbstractAnalysisParameters;
import ru.biosoft.analysiscore.AnalysisMethodSupport;

public class ValidateExperiments extends AnalysisMethodSupport<ValidateExperiments.Parameters>
{
    private static final Pattern EXP_PATTERN = Pattern.compile( "EXP[0-9]{6}" );
    private static final Pattern READS_PATTERN = Pattern.compile( "READS[0-9]{6}" );
    private static final Pattern ALIGNS_PATTERN = Pattern.compile( "ALIGNS[0-9]{6}" );
    private static final Pattern PEAKS_PATTERN = Pattern.compile( "PEAKS[0-9]{6}" );

    public ValidateExperiments(DataCollection<?> origin, String name)
    {
        super( origin, name, new Parameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        List<ChIPseqExperiment> expCollection = new ArrayList<>(DataCollectionUtils.asCollection(parameters.getExperimentsCollection(),
                ChIPseqExperiment.class));

        List<String> errors = new ArrayList<>();
        for( ChIPseqExperiment e : expCollection )
            validateExperiment( e, errors );
        noUnusedControls( expCollection, errors );
        eachExperimentHasOwnElements( expCollection, errors );
        noDuplicateDataSources( expCollection, errors );
        allTablesLinkedToChipExperiments(
                DataCollectionUtils.getSqlConnection( parameters.getExperimentsCollection().getDataCollection() ), errors );

        for( String err : errors )
            log.log(Level.SEVERE,  err );

        return new Object[0];
    }

    private void allTablesLinkedToChipExperiments(Connection con, List<String> errors) throws BiosoftSQLException
    {
        if( SqlUtil.queryInt( con, "SELECT COUNT(*) FROM chip_experiments RIGHT JOIN hub on(id = output) WHERE output_type='ExperimentGTRDType' AND isNULL(id)" ) != 0 )
            errors.add( "Not linked hub" );
        if( SqlUtil.queryInt( con,
                "SELECT COUNT(*) FROM chip_experiments RIGHT JOIN external_refs using(id) WHERE isNULL(chip_experiments.id)" ) != 0 )
            errors.add( "Not linked external_refs" );
        if( SqlUtil.queryInt( con, "SELECT COUNT(*) FROM hub RIGHT JOIN properties on(input=id) WHERE isNULL(hub.input)" ) != 0 )
            errors.add( "Not linked properties" );
    }
    private void noDuplicateDataSources(Iterable<ChIPseqExperiment> expCollection, List<String> errors)
    {
        Set<Set<String>> sources = new HashSet<>();
        for( ChIPseqExperiment e : expCollection )
        {
            Set<String> source = new HashSet<>( e.getReadsURLs() );
            if( sources.contains( source ) )
                errors.add( "Duplicate source (" + e.getName() + ") " + StringUtils.join( source, ";" ) );
            sources.add( source );
        }
    }

    private void eachExperimentHasOwnElements(Iterable<ChIPseqExperiment> expCollection, List<String> errors)
    {
        Set<String> used = new HashSet<>();
        for( ChIPseqExperiment e : expCollection )
        {
            for( DataElementPath path : e.getReads() )
            {
                String readId = path.getName().replaceAll( "[.].*", "" );
                if( used.contains( readId ) )
                    errors.add( "Duplicate ID " + readId );
                used.add( readId );
            }
            
            String id = e.getAlignment().getName();
            if( used.contains( id ) )
                errors.add( "Duplicate ID " + id );
            used.add( id );
            
            if( e.getPeak() != null )
            {
                id = e.getPeak().getName();
                if( used.contains( id ) )
                    errors.add( "Duplicate ID " + id );
                used.add( id );
            }
        }
    }

    private void noUnusedControls(Iterable<ChIPseqExperiment> expCollection, List<String> errors)
    {
        Set<String> controlIDs = new HashSet<>();
        Set<String> usedControlIDs = new HashSet<>();
        for( ChIPseqExperiment e : expCollection )
            if( e.isControlExperiment() )
                controlIDs.add( e.getName() );
            else if( e.getControlId() != null )
                usedControlIDs.add( e.getControlId() );
        controlIDs.removeAll( usedControlIDs );
        if( !controlIDs.isEmpty() )
            errors.add( "Unused controls " + StringUtils.join( controlIDs, ";" ) );
    }


    private void validateExperiment(ChIPseqExperiment e, List<String> errors)
    {
        if( !EXP_PATTERN.matcher( e.getName() ).matches() )
            errors.add( "Wrong experiment ID (" + e.getName() + ")" );

        if( e.getReads() == null || e.getReads().isEmpty() )
            errors.add( "No linked reads for " + e.getName() );

        if( e.getAlignment() == null )
            errors.add( "No linked alignments for " + e.getName() );
        if( !ALIGNS_PATTERN.matcher( e.getAlignment().getName() ).matches() )
            errors.add( "Wrong alignemnts ID (" + e.getAlignment().getName() + ")" );

        if( e.isControlExperiment() )
        {
            if( e.getControl() != null )
                errors.add( "Control set for control experiment " + e.getName() );
            if( e.getTfClassId() != null )
                errors.add( "TF class (" + e.getTfClassId() + ") set for control experiment " + e.getName() );
            if( e.getTfUniprotId() != null )
                errors.add( "Uniprot id (" + e.getTfUniprotId() + ") set for control experiment " + e.getName() );

            if( e.getPeak() != null )
                errors.add( "Peaks set for control experiment " + e.getName() );
        }
        else
        {
            if( e.getControl() != null && !e.getControl().exists() )
                errors.add( "No control for " + e.getName() );

            if( e.getTfUniprotId() == null || e.getTfUniprotId().isEmpty() )
                errors.add( "No uniprot id for non control experiment " + e.getName() );

            if( e.getPeak() == null )
                errors.add( "No peaks set for non control experiment " + e.getName() );

            if( !PEAKS_PATTERN.matcher( e.getPeak().getName() ).matches() )
                errors.add( "Wrong peaks ID (" + e.getPeak().getName() + ")" );
        }

        Set<String> urls = new HashSet<>();
        for( DataElementPath path : e.getReads() )
        {
            String id = path.getName().replaceAll( "[.].*", "" );
            if( !READS_PATTERN.matcher( id ).matches() )
                errors.add( "Wrong reads id (" + id + ")" );
            String sourceURL = e.getElementProperties( id ).get( "url" );
            if( sourceURL == null || sourceURL.isEmpty() )
                errors.add( "No source URL for " + id );
            urls.add( sourceURL );
        }

        if( !urls.equals( new HashSet<>( e.getReadsURLs() ) ) )
        {
            errors.add( "URL mismatch" );
        }
    }
    public static class Parameters extends AbstractAnalysisParameters
    {
        private DataElementPath experimentsCollection = DataElementPath.create( ChIPseqExperimentSQLTransformer.DEFAULT_GTRD_ROOT + "/experiments" );

        public DataElementPath getExperimentsCollection()
        {
            return experimentsCollection;
        }

        public void setExperimentsCollection(DataElementPath experimentsCollection)
        {
            Object oldValue = this.experimentsCollection;
            this.experimentsCollection = experimentsCollection;
            firePropertyChange( "experimentsCollection", oldValue, experimentsCollection );
        }

    }

    public static class ParametersBeanInfo extends BeanInfoEx
    {
        public ParametersBeanInfo()
        {
            super( Parameters.class, true );
        }

        @Override
        protected void initProperties() throws Exception
        {
            add( "experimentsCollection" );
        }
    }
}
