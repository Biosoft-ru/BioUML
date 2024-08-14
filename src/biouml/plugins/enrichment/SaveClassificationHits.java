package biouml.plugins.enrichment;

import java.security.InvalidParameterException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.DataCollectionUtils;
import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.analysiscore.AnalysisParameters;
import ru.biosoft.analysiscore.AnalysisParametersFactory;
import ru.biosoft.table.ColumnModel;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.StringSet;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.TableDataCollectionUtils;
import ru.biosoft.table.access.TableRowsExporter;
import ru.biosoft.table.columnbeans.ColumnNameSelector;

@ClassIcon ( "resources/save-hits.png" )
public class SaveClassificationHits extends AnalysisMethodSupport<SaveClassificationHitsParameters>
{
    public SaveClassificationHits(DataCollection<?> origin, String name)
    {
        super( origin, name, new SaveClassificationHitsParameters() );
    }

    @Override
    public void validateParameters() throws IllegalArgumentException
    {
        super.validateParameters();
        SaveClassificationHitsParameters params = getParameters();
        DataElementPath inputPath = params.getSourcePath();

        DataElement functionalClassification = inputPath.getDataElement();
        if( ! ( functionalClassification instanceof TableDataCollection ) )
            throw new InvalidParameterException( "This element is not a table" );

        AnalysisParameters parameters = AnalysisParametersFactory.read( functionalClassification );
        if( parameters == null || ! ( parameters instanceof FunctionalClassificationParameters ) )
            throw new InvalidParameterException( "This table is not a result of Functional classification" );

        checkNotEmptyCollection( "sourcePath" );

        if( ColumnNameSelector.NONE_COLUMN.equals( params.getPValueColumn() ) )
            throw new InvalidParameterException( "P-value column must be specified." );

        int hitsColIndex = ( (TableDataCollection)functionalClassification ).getColumnModel()
                .optColumnIndex( FunctionalClassification.HITS_COLUMN );
        if( hitsColIndex == -1 )
            throw new InvalidParameterException(
                    "'" + FunctionalClassification.HITS_COLUMN + "' column should be present in the result of Functional classification" );

    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        log.info( "Filtering categories..." );
        TableDataCollection input = getParameters().getSourcePath().getDataElement( TableDataCollection.class );

        DataElementPath resultPath = getParameters().getOutputPath();

        if( resultPath.exists() )
            resultPath.remove();
        DataCollection<DataElement> resultsFolder = DataCollectionUtils.createSubCollection( resultPath );

        String column = getParameters().getPValueColumn();
        ColumnModel model = input.getColumnModel();
        int colIndex = model.optColumnIndex( column );
        double threshold = getParameters().getPValueThreshold();
        int gsColIndex = model.optColumnIndex( FunctionalClassification.GROUP_SIZE_COLUMN );
        int maxSize = getParameters().getMaxGroupSize();
        int numHitsColIndex = model.optColumnIndex( FunctionalClassification.NUMBER_OF_HITS_COLUMN );
        int minHits = getParameters().getMinHits();
        Set<String> obligate = StreamEx.of( Arrays.asList( getParameters().getObligateCategories() ) ).filter( input::contains ).toSet();
        List<String> goTermsAll = input.stream().filter( rde -> {
            if( obligate.contains( rde.getName() ) )
                return false;
            Object[] values = rde.getValues();
            return ( gsColIndex == -1 || Integer.parseInt( values[gsColIndex].toString() ) < maxSize )
                    && ( numHitsColIndex == -1 || Integer.parseInt( values[numHitsColIndex].toString() ) > minHits );
        } ).sorted( Comparator.comparingDouble( rde -> Double.parseDouble( rde.getValues()[colIndex].toString() ) ) )
                .map( rde -> rde.getName() ).collect( Collectors.toList() );

        jobControl.setPreparedness( 10 );
        if( jobControl.isStopped() )
            return null;

        int minCategories = getParameters().getMinCategories() - obligate.size();

        if( ( minCategories > 0 && goTermsAll.isEmpty() ) || goTermsAll.size() < minCategories )
        {
            log.info( "Not enought categories passed the filters, empty result folder is created..." );
            return null;
        }
        double minCatCutoff = Double.parseDouble( ( input.get( goTermsAll.get( minCategories - 1 ) ).getValues()[colIndex] ).toString() );
        List<String> goTerms = null;
        int maxCategories = getParameters().getMaxCategories() - obligate.size();
        if( minCatCutoff < threshold )
        {
            goTerms = StreamEx.of( goTermsAll ).filter( name -> {
                try
                {
                    return Double.parseDouble( input.get( name ).getValues()[colIndex].toString() ) < threshold;
                }
                catch( Exception e )
                {
                    return false;
                }
            } ).limit( maxCategories ).toList();
        }
        else
        {
            int min = Math.min( 50 - obligate.size(), goTermsAll.size() );
            log.info( "Not enought categories passed the threshold filter, taking first " + min + " categories from the list..." );
            goTerms = goTermsAll.subList( 0, min );
        }
        goTerms.addAll( obligate );


        log.info( goTerms.size() + " categories passed the filter..." );
        log.info( "Generating gene lists..." );
        jobControl.pushProgress( 10, 90 );

        AnalysisParameters fcParameters = AnalysisParametersFactory.read( input );
        TableDataCollection source = ( (FunctionalClassificationParameters)fcParameters ).getSource();
        List<TableDataCollection> results = new ArrayList<>();

        jobControl.forCollection( goTerms, term -> {
            Object hits = null;
            try
            {
                hits = input.get( term ).getValue( FunctionalClassification.HITS_COLUMN );
            }
            catch( Exception e1 )
            {
            }
            DataElementPath destination = resultPath.getChildPath( term + " hits" );
            TableDataCollection result = null;
            if( hits != null && hits instanceof StringSet )
            {

                if( source == null || source.isEmpty() )
                {
                    result = TableDataCollectionUtils.createTableDataCollection( destination );
                    Object[] values = new Object[0];
                    for( String hit : ( (StringSet)hits ) )
                    {
                        TableDataCollectionUtils.addRow( result, hit, values );
                    }
                }
                else
                {
                    List<RowDataElement> hitsDE = new ArrayList<>();
                for( String hit : ( (StringSet)hits ) )
                {
                    try
                    {
                        RowDataElement row = source.get( hit );
                        if( row != null )
                            hitsDE.add( row );
                    }
                    catch( Exception e )
                    {
                    }
                }
                    TableRowsExporter.exportTable( destination, source, hitsDE, jobControl );
                    result = destination.getDataElement( TableDataCollection.class );
                    ReferenceTypeRegistry.copyCollectionReferenceType( result, source );

                }
            }
            else
                result = TableDataCollectionUtils.createTableDataCollection( destination );
            if( result != null )
            {
                destination.save( result );
                results.add( result );
            }
            return true;
        } );
        return new Object[0];
    }

}
