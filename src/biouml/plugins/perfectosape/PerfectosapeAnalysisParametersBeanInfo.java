package biouml.plugins.perfectosape;

import biouml.standard.type.Species;

import com.developmentontheedge.beans.PropertyDescriptorEx;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.analysis.WeightMatrixCollection;
import ru.biosoft.bsa.snp.SNPTableType;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.OptionEx;
import ru.biosoft.util.bean.BeanInfoEx2;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;

/**
 * @author lan
 *
 */
public class PerfectosapeAnalysisParametersBeanInfo extends BeanInfoEx2<PerfectosapeAnalysisParameters>
{
    public PerfectosapeAnalysisParametersBeanInfo()
    {
        super(PerfectosapeAnalysisParameters.class);
    }

    @Override
    protected void initProperties() throws Exception
    {
        addWithTags("mode", PerfectosapeAnalysisParameters.SEQUENCES_MODE, PerfectosapeAnalysisParameters.SNP_MODE);
        addHidden(DataElementPathEditor.registerInput("seqTable", beanClass, TableDataCollection.class), "isSequencesModeHidden");
        addHidden(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH), "isSNPModeHidden");
        addHidden(DataElementPathEditor.registerInput("snpTable", beanClass, TableDataCollection.class, SNPTableType.class),
                "isSNPModeHidden");
        add(DataElementPathEditor.registerInput("matrixLib", beanClass, WeightMatrixCollection.class));
        addHidden(new PropertyDescriptorEx("table", beanClass, "getTable", null));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outTable", beanClass, TableDataCollection.class),
                "$table$ out"));
    }
}
