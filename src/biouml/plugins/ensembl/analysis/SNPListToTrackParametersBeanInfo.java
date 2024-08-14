package biouml.plugins.ensembl.analysis;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.analysis.aggregate.NumericAggregatorEditor;
import ru.biosoft.bsa.VCFSqlTrack;
import ru.biosoft.bsa.gui.MessageBundle;
import ru.biosoft.bsa.snp.SNPTableType;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.table.columnbeans.ColumnNameSelector;
import ru.biosoft.util.OptionEx;

public class SNPListToTrackParametersBeanInfo extends BeanInfoEx
{
    public SNPListToTrackParametersBeanInfo()
    {
        super(SNPListToTrackParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add(DataElementPathEditor.registerInput("sourcePath", beanClass, TableDataCollection.class, SNPTableType.class),
                getResourceString("PN_GENESET_SOURCE"), getResourceString("PD_GENESET_SOURCE"));
        add( "ensembl" );
        add(new PropertyDescriptorEx("fivePrimeSize", beanClass), getResourceString("PN_SNP_5PRIME"), getResourceString("PD_SNP_5PRIME"));
        add(new PropertyDescriptorEx("threePrimeSize", beanClass), getResourceString("PN_SNP_3PRIME"), getResourceString("PD_SNP_3PRIME"));
        add(new PropertyDescriptorEx("outputNonMatched", beanClass), getResourceString("PN_SNP_OUTPUT_NON_MATCHED"),
                getResourceString("PD_SNP_OUTPUT_NON_MATCHED"));
        add(ColumnNameSelector.registerSelector("column", beanClass, "sourcePath"), getResourceString("PN_SNP_NUMERIC_COLUMN"), getResourceString("PD_SNP_NUMERIC_COLUMN"));
        property( "ignoreNaNInAggregator" ).titleRaw( "Ignore empty values" ).descriptionRaw( "Ignore empty values during aggregator work" )
                .add();
        PropertyDescriptorEx pde = new PropertyDescriptorEx("aggregator", beanClass);
        pde.setSimple(true);
        pde.setPropertyEditorClass(NumericAggregatorEditor.class);
        add(pde, getResourceString("PN_SNP_AGGREGATION_TYPE"),
                getResourceString("PD_SNP_AGGREGATION_TYPE"));
        add( OptionEx.makeAutoProperty( DataElementPathEditor.registerOutput( "destPath", beanClass, VCFSqlTrack.class ),
                "$sourcePath$ track" ),
                getResourceString("PN_GENESET_OUTPUT_TRACK"), getResourceString("PD_GENESET_OUTPUT_TRACK"));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("annotatedPath", beanClass, TableDataCollection.class, SNPTableType.class),
                "$sourcePath$ annotated"), getResourceString("PN_SNP_OUTPUT_TABLE"), getResourceString("PD_SNP_OUTPUT_TABLE"));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputGenes", beanClass, TableDataCollection.class, ReferenceTypeRegistry.getReferenceType("Genes: Ensembl").getClass()),
                "$sourcePath$ genes"), getResourceString("PN_SNP_OUTPUT_GENES"), getResourceString("PD_SNP_OUTPUT_GENES"));
    }
}
