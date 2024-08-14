package biouml.plugins.ensembl.analysis.mutationeffect;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.analysis.maos.Parameters;
import ru.biosoft.bsa.analysis.maos.ParametersBeanInfo;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;

public class ParametersBySNPBeanInfo extends ParametersBeanInfo
{
    public ParametersBySNPBeanInfo()
    {
        super( ParametersBySNP.class );
    }


    @Override
    protected void initProperties() throws Exception
    {
        add( DataElementPathEditor.registerInput( "snpTable", beanClass, TableDataCollection.class ) );
        add( ColumnNameSelector.registerTextOrSetSelector( "riskColumn", beanClass, "snpTable", true, false ) );
        add( "genome" );
        add( DataElementPathEditor.registerInput( "siteModels", beanClass, SiteModelCollection.class ) );
        property( "scoreType" ).tags( Parameters.SITE_MODEL_SCORE_TYPE, Parameters.PVALUE_SCORE_TYPE ).hidden( "isScoreTypeHidden" ).add();
        add( "scoreDiff" );
        add( "independentVariations" );
        add( "targetGeneTSSUpstream" );
        add( "targetGeneTSSDownstream" );
        add( "oneNearestTargetGene" );

        property( "outputTable" ).outputElement( TableDataCollection.class ).auto( "$snpTable$_mutation_effects" ).add();
        property( "siteGainTrack" ).outputElement( SqlTrack.class ).auto( "$snpTable$_site_gain" ).add();
        property( "siteLossTrack" ).outputElement( SqlTrack.class ).auto( "$snpTable$_site_loss" ).add();
        property( "importantMutationsTrack" ).outputElement( SqlTrack.class ).auto( "$snpTable$_important_mutations" ).add();
        property( "summaryTable" ).outputElement( TableDataCollection.class ).auto( "$snpTable$_summary" ).add();
    }
}
