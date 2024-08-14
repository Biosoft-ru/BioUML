package ru.biosoft.bsa.analysis;

import java.beans.PropertyDescriptor;

import ru.biosoft.access.biohub.ReferenceTypeRegistry;
import ru.biosoft.access.repository.DataElementPathEditor;
import ru.biosoft.bsa.SiteModelCollection;
import ru.biosoft.bsa.gui.MessageBundle;
import ru.biosoft.table.TableDataCollection;
import ru.biosoft.util.OptionEx;
import ru.biosoft.workbench.editors.DataElementComboBoxSelector;
import biouml.standard.type.Species;

import com.developmentontheedge.beans.BeanInfoEx;
import com.developmentontheedge.beans.PropertyDescriptorEx;

public class GeneSetSiteSearchParametersBeanInfo extends BeanInfoEx
{
    public GeneSetSiteSearchParametersBeanInfo()
    {
        super(GeneSetSiteSearchParameters.class, MessageBundle.class.getName());
        beanDescriptor.setDisplayName(getResourceString("CN_CLASS"));
        beanDescriptor.setShortDescription(getResourceString("CD_CLASS"));
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        // Note: indirect link to EnsemblGeneTableType, because we cannot refer here to this class directly
        // as it will imply dependency to biouml.plugins.ensembl
        // TODO: extract site search analyzes to separate plugin and make proper dependencies
        add(DataElementPathEditor.registerInput("yesSetPath", beanClass, TableDataCollection.class,
                ReferenceTypeRegistry.getReferenceType("Genes: Ensembl").getClass()), getResourceString("PN_GENESET_YESSET"),
                getResourceString("PD_GENESET_YESSET"));
        add(DataElementPathEditor.registerInput("noSetPath", beanClass, TableDataCollection.class,
                ReferenceTypeRegistry.getReferenceType("Genes: Ensembl").getClass(), true), getResourceString("PN_GENESET_NOSET"),
                getResourceString("PD_GENESET_NOSET"));
        add(DataElementComboBoxSelector.registerSelector("species", beanClass, Species.SPECIES_PATH),
                getResourceString("PN_SPECIES"), getResourceString("PD_SPECIES"));
        add(new PropertyDescriptorEx("from", beanClass), getResourceString("PN_GENESET_FROM"),
                getResourceString("PD_GENESET_FROM"));
        add(new PropertyDescriptorEx("to", beanClass), getResourceString("PN_GENESET_TO"),
                getResourceString("PD_GENESET_TO"));
        addHidden(new PropertyDescriptor("defaultProfile", beanClass, "getDefaultProfile", null));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerInput("profilePath", beanClass, SiteModelCollection.class),
                "$defaultProfile$"), getResourceString("PN_SITESEARCH_PROFILE"), getResourceString("PD_SITESEARCH_PROFILE"));
        PropertyDescriptorEx pde = new PropertyDescriptorEx("optimizeCutoff", beanClass);
        add(pde, getResourceString("PN_GENESET_OPTIMIZE_CUTOFF"), getResourceString("PD_GENESET_OPTIMIZE_CUTOFF"));
        pde = new PropertyDescriptorEx("optimizeWindow", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_GENESET_OPTIMIZE_WINDOW"), getResourceString("PD_GENESET_OPTIMIZE_WINDOW"));
        pde = new PropertyDescriptorEx("pvalueCutoff", beanClass);
        pde.setExpert(true);
        pde.setHidden(beanClass.getMethod("isOptimizationOptionsHidden"));
        add(pde, getResourceString("PN_SITESEARCH_PVALUE_CUTOFF"), getResourceString("PD_GENESET_PVALUE_CUTOFF"));
        pde = new PropertyDescriptorEx("deleteNonOptimized", beanClass);
        pde.setExpert(true);
        pde.setHidden(beanClass.getMethod("isOptimizationOptionsHidden"));
        add(pde, getResourceString("PN_GENESET_DELETE_NON_OPTIMIZED"), getResourceString("PD_GENESET_DELETE_NON_OPTIMIZED"));
        pde = new PropertyDescriptorEx("overrepresentedOnly", beanClass);
        pde.setExpert(true);
        add(pde, getResourceString("PN_SUMMARY_OVERREPRESENTED_ONLY"), getResourceString("PD_SUMMARY_OVERREPRESENTED_ONLY"));
        add(OptionEx.makeAutoProperty(DataElementPathEditor.registerOutput("outputPath", beanClass, SiteSearchResult.class),
                "$yesSetPath$ sites $from$..$to$"), getResourceString("PN_GENESET_OUTPUTPATH"),
                getResourceString("PD_GENESET_OUTPUTPATH"));
    }
}
