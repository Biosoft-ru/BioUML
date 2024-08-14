package biouml.plugins.ensembl.analysis.mutationeffect;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import com.developmentontheedge.beans.DynamicProperty;

import biouml.plugins.ensembl.access.EnsemblDatabase;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.ClassIcon;
import ru.biosoft.analysiscore.AnalysisMethodSupport;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.VariationElement;
import ru.biosoft.jobcontrol.Iteration;
import ru.biosoft.table.RowDataElement;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;

/**
 * Analysis process table with SNP identifiers and Risk column with variation sequence (might be either Reference allele, Alternative allele or both). 
 * Compares sites found for Risk allele vs. Other allele
 * @author anna
 *
 */
@ClassIcon ( "resources/mutation-effect.gif" )
public class MutationEffectOnSitesBySNP extends AnalysisMethodSupport<ParametersBySNP>
{
    public MutationEffectOnSitesBySNP(DataCollection<?> origin, String name)
    {
        this( origin, name, new ParametersBySNP() );
    }

    protected MutationEffectOnSitesBySNP(DataCollection<?> origin, String name, ParametersBySNP parameters)
    {
        super( origin, name, parameters );
    }

    @Override
    public Object justAnalyzeAndPut() throws Exception
    {
        EnsemblDatabase ensembl = new EnsemblDatabase( parameters.getEnsemblPath() );
        final DataCollection<VariationElement> variationCollection = ensembl.getVariationCollection();
        TableDataCollection snpTable = parameters.getSnpTable().getDataElement( TableDataCollection.class );
        final int columnIndex = ColumnNameSelector.NONE_COLUMN.equals( parameters.getRiskColumn() ) ? -1
                : snpTable.getColumnModel().optColumnIndex( parameters.getRiskColumn() );
        final Map<String, List<Site>> sitesByChr = new HashMap<>();
        final int[] statuses = new int[4];
        jobControl.pushProgress( 0, 30 );
        jobControl.forCollection( snpTable.getNameList(), new Iteration<String>()
        {
            @Override
            public boolean run(String snpName)
            {
                try
                {
                    VariationElement variation = variationCollection.get( snpName );
                    if( variation == null )
                    {
                        statuses[0]++;
                    }
                    else
                    {
                        Site s = variation.getSite();
                        if( s.getOriginalSequence() == null )
                        {
                            statuses[0]++;
                        }
                        else
                        {
                            String chrName = s.getOriginalSequence().getName();
                            s = VariationElement.extendInsertionSite( s );
                            if( columnIndex != -1 )
                            {
                                RowDataElement rde = snpTable.get( snpName );
                                String riskAllele = rde.getValues()[columnIndex].toString();
                                s.getProperties().add( new DynamicProperty( "RiskAllele", String.class, riskAllele ) );
                            }
                            sitesByChr.computeIfAbsent( chrName, k -> new ArrayList<>() ).add( s );
                        }
                    }
                }
                catch( Exception e )
                {
                    statuses[2]++;
                    log.log( Level.SEVERE, e.getMessage(), e );
                }
                return true;
            }
        } );
        if( jobControl.isStopped() )
        {
            return null;
        }
        jobControl.popProgress();
        for( String chr : sitesByChr.keySet() )
        {
            sitesByChr.get( chr ).sort( Comparator.comparing( Site::getFrom ) );
        }
        jobControl.pushProgress( 30, 100 );
        WholeGenomeTaskBySNP task = createTask( sitesByChr );
        return task.run();
    }

    private WholeGenomeTaskBySNP createTask(Map<String, List<Site>> sitesByChr)
    {
        return new WholeGenomeTaskBySNP( parameters, log, jobControl, sitesByChr );
    }
}
