package ru.biosoft.bsa.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicPropertySet;

import ru.biosoft.access.CollectionFactoryUtils;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.Track;
import ru.biosoft.bsa.TrackUtils;
import ru.biosoft.bsa.importer.VCFTrackImporter;
import ru.biosoft.jobcontrol.JobControl;

/**
 * Filter VCF track by Genotype. Use Format_GT property of track sites. Create separate track for each sample.
 * heterozygote - two different alleles, at least one non-reference (0/1, 1|0, 1/2); homozygote - all alleles the
 * same non-reference one, including haploid calls on chrX/chrY/chrM (1/1, 1|1, 1); both - either of them.
 * Reference-only (0/0, 0), missing (./., 0/.) and other values are ignored.
 * @author anna
 *
 */
@ClassIcon ( "resources/filter-track-vcf.png" )
public class FilterVCFAnalysis extends AnalysisMethodSupport<FilterVCFAnalysisParameters>
{
    public FilterVCFAnalysis(DataCollection<?> origin, String name)
    {
        super( origin, name, new FilterVCFAnalysisParameters() );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        Track input = parameters.getInputTrack().getDataElement( Track.class );

        log.info( "Initializing resulting tracks..." );
        List<String> properties = TrackUtils.getTrackSitesProperties( input );
        Map<String, SqlTrack> results = new HashMap<>();

        DataElementPath outputPath = parameters.getOutputPath();
        DataCollection<DataElement> resultsFolder = DataCollectionUtils.createSubCollection( outputPath );
        String mode = getParameters().getMode();
        for( String propertyName : properties )
        {
            if( propertyName.startsWith( VCFTrackImporter.VCF_FORMAT_PREFIX + "GT_" ) )
            {
                String sampleName = propertyName.substring( 10 );
                DataElementPath trackPath = outputPath.getChildPath( sampleName + "_" + mode );
                SqlTrack result = SqlTrack.createTrack( trackPath, input, input.getClass() );
                results.put( propertyName, result );
            }
        }
        if( results.isEmpty() )
        {
            log.info( "Genotype FORMAT field was not found in track " + input.getName() );
            return null;
        }

        log.info( "Filtering..." );
        jobControl.pushProgress( 5, 95 );
        jobControl.forCollection( DataCollectionUtils.asCollection( input.getAllSites(), Site.class ), element -> {
            try
            {
                DynamicPropertySet dps = element.getProperties();
                for( String propertyName : results.keySet() )
                {
                    if( matches( mode, dps.getValueAsString( propertyName ) ) )
                    {
                        results.get( propertyName ).addSite( element );
                    }
                }
                            }
            catch( Exception e )
            {
                log.log( Level.SEVERE, "Unable to add site: " + e.getMessage() );
            }
            return true;
        } );
        jobControl.popProgress();
        if( jobControl.getStatus() == JobControl.TERMINATED_BY_REQUEST )
        {
            for( SqlTrack result : results.values() )
            {
                result.getOrigin().remove( result.getName() );
            }
            return null;
        }
        for( SqlTrack result : results.values() )
        {
            result.finalizeAddition();
            CollectionFactoryUtils.save( result );
            // the omics type lives in the input's folder, not in the track: carry it over to the new folder
            TrackUtils.copyOmicsType( input, result );
        }
        return resultsFolder;
    }

    /** Genotype classes, see the class comment. */
    public static final int GT_OTHER = 0, GT_HETEROZYGOTE = 1, GT_HOMOZYGOTE = 2;

    public static int classifyGenotype(String gt)
    {
        if( gt == null || gt.isEmpty() )
            return GT_OTHER;
        String[] alleles = gt.split( "[/|]" );
        boolean nonReference = false;
        for( String allele : alleles )
        {
            if( allele.isEmpty() || allele.equals( "." ) )
                return GT_OTHER;
            for( int i = 0; i < allele.length(); i++ )
                if( !Character.isDigit( allele.charAt( i ) ) )
                    return GT_OTHER;
            if( Integer.parseInt( allele ) != 0 )
                nonReference = true;
        }
        if( !nonReference )
            return GT_OTHER;
        for( String allele : alleles )
            if( Integer.parseInt( allele ) != Integer.parseInt( alleles[0] ) )
                return GT_HETEROZYGOTE;
        return GT_HOMOZYGOTE;
    }

    public static boolean matches(String mode, String gt)
    {
        int type = classifyGenotype( gt );
        if( FilterVCFAnalysisParameters.MODE_HETEROZYGOTE.equals( mode ) )
            return type == GT_HETEROZYGOTE;
        if( FilterVCFAnalysisParameters.MODE_HOMOZYGOTE.equals( mode ) )
            return type == GT_HOMOZYGOTE;
        if( FilterVCFAnalysisParameters.MODE_BOTH.equals( mode ) )
            return type != GT_OTHER;
        return false;
    }
}
