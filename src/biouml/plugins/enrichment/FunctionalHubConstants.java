package biouml.plugins.enrichment;

import java.beans.PropertyDescriptor;

import ru.biosoft.util.bean.StaticDescriptor;

/**
 * @author lan
 *
 */
public interface FunctionalHubConstants
{

    /**
     * Property for Element to specify total number of genes in all categories
     */
    public static final String TOTAL_GENES_PROPERTY = "Total genes";
    public static final PropertyDescriptor TOTAL_GENES_DESCRIPTOR = StaticDescriptor.create(TOTAL_GENES_PROPERTY);
    /**
     * Property for Element to specify number of input genes which match to any category (may be less than total number of input genes)
     */
    public static final String INPUT_GENES_PROPERTY = "Input genes";
    public static final PropertyDescriptor INPUT_GENES_DESCRIPTOR = StaticDescriptor.create(INPUT_GENES_PROPERTY);
    /**
     * Element property describing size (number of genes) in current category
     */
    public static final String GROUP_SIZE_PROPERTY = "Group size";
    public static final PropertyDescriptor GROUP_SIZE_DESCRIPTOR = StaticDescriptor.create(GROUP_SIZE_PROPERTY);
    /**
     * ClassificationRecord entry to mark this hub as valid for FC analysis
     */
    public static final String FUNCTIONAL_CLASSIFICATION_RECORD = "FunctionalClassification";
    /**
     * ClassificationRecord entry to mark this hub as valid for FC analysis and hits statistics is requested
     */
    public static final String FUNCTIONAL_CLASSIFICATION_HITS_RECORD = "FunctionalClassificationHits";

}
