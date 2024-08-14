package biouml.plugins.ensembl.analysis;

import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.Track;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.OptionEx;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.plugins.ensembl.analysis.SiteAggregator.SiteAggregatorSelector;
import biouml.plugins.ensembl.tabletype.EnsemblGeneTableType;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class TrackToGeneSetParametersBeanInfo extends BeanInfoEx
{
    protected TrackToGeneSetParametersBeanInfo(Class<? extends TrackToGeneSetParameters> clazz)
    {
        super(clazz, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }
    
    public TrackToGeneSetParametersBeanInfo()
    {
        super(TrackToGeneSetParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        add(DataElementPathEditor.registerInputMulti("sourcePaths", beanClass, Track.class),
                getResourceString("PN_GENESET_SOURCE_TRACK"), getResourceString("PD_GENESET_SOURCE_TRACK"));
        add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH), getResourceString("PN_SPECIES"), getResourceString("PD_SPECIES"));
        add(new PropertyDescriptorEx("from", beanClass), getResourceString("PN_GENESET_FROM"),
                getResourceString("PD_GENESET_FROM"));
        add(new PropertyDescriptorEx("to", beanClass), getResourceString("PN_GENESET_TO"),
                getResourceString("PD_GENESET_TO"));
        PropertyDescriptorEx pde = new PropertyDescriptorEx("resultTypes", beanClass);
        pde.setSimple(true);
        pde.setHideChildren(true);
        pde.setPropertyEditorClass(SiteAggregatorSelector.class);
        add(pde, getResourceString("PN_GENESET_RESULT_TYPE"), getResourceString("PD_GENESET_RESULT_TYPE"));
        
        add("allGenes");
        
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("destPath", beanClass, TableDataCollection.class, EnsemblGeneTableType.class), "$sourcePaths/path$/Track genes"), getResourceString("PN_GENESET_OUTPUTNAME"),
                getResourceString("PD_GENESET_OUTPUTNAME"));
    }
}
