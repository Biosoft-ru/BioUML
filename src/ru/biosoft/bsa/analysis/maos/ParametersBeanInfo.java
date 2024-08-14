package ru.biosoft.bsa.analysis.maos;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.SqlTrack;
import ru.biosoft.bsa.VCFSqlTrack;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.bean.BeanInfoEx2;

public class ParametersBeanInfo extends BeanInfoEx2<Parameters>
{
    public ParametersBeanInfo()
    {
        this( Parameters.class );
    }
    
    protected ParametersBeanInfo(Class<? extends Parameters> beanClass)
    {
        super( beanClass );
    }

    @Override
    protected void initProperties() throws Exception
    {
        add( DataElementPathEditor.registerInput( "vcfTrack", beanClass, VCFSqlTrack.class ) );
        add( "genome" );
        add( DataElementPathEditor.registerInput( "siteModels", beanClass, SiteModelCollection.class ) );
        property("scoreType").tags( Parameters.SITE_MODEL_SCORE_TYPE, Parameters.PVALUE_SCORE_TYPE ).hidden( "isScoreTypeHidden" ).add();
        add( "scoreDiff" );
        add( "independentVariations" );
        add( "targetGeneTSSUpstream" );
        add( "targetGeneTSSDownstream" );
        add( "oneNearestTargetGene" );

        property("outputTable").outputElement( TableDataCollection.class ).auto( "$vcfTrack$_mutation_effects" ).add();
        property("siteGainTrack").outputElement( SqlTrack.class ).auto( "$vcfTrack$_site_gain" ).add();
        property("siteLossTrack").outputElement( SqlTrack.class ).auto( "$vcfTrack$_site_loss" ).add();
        property("importantMutationsTrack").outputElement( SqlTrack.class ).auto( "$vcfTrack$_important_mutations" ).add();
        property("summaryTable").outputElement( TableDataCollection.class ).auto( "$vcfTrack$_summary" ).add();
    }
}