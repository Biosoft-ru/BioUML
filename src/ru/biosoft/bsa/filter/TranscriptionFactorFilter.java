package ru.biosoft.bsa.filter;

import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.MutableFilter;
import ru.biosoft.access.core.filter.PatternFilter;
import ru.biosoft.bsa.Const;
import ru.biosoft.bsa.TransfacTranscriptionFactor;
import ru.biosoft.bsa.classification.ClassificationUnit;

import com.developmentontheedge.beans.ChoicePropertyDescriptorEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import com.developmentontheedge.beans.editors.StringTagEditor;
import com.developmentontheedge.beans.editors.TagEditorSupport;
import ru.biosoft.jobcontrol.FunctionJobControl;


@SuppressWarnings ( "serial" )
public class TranscriptionFactorFilter extends MutableFilter<TransfacTranscriptionFactor>
{
    private TaxonFilter          taxonFilter          = null;//new TaxonFilter();
    private SpeciesFilter        speciesFilter        = null;//new SpeciesFilter();
    private DomainFilter         domainFilter         = null;//new DomainFilter();
    private ClassificationFilter classificationFilter = null;//new ClassificationFilter();
    private FactorNameFilter     nameFilter           = null;//new FactorNameFilter();
    private PositiveTissueSpecificityFilter positiveTissueSpecificityFilter = null;//new PositiveTissueSpecificityFilter();
    private NegativeTissueSpecificityFilter negativeTissueSpecificityFilter = null;//new NegativeTissueSpecificityFilter();
    protected static final Logger cat = Logger.getLogger(TranscriptionFactorFilter.class.getName());

    public TranscriptionFactorFilter( FunctionJobControl jobControl, DataElementPath classificationsRoot )
    {
        this(null,jobControl, classificationsRoot);
    }

    public TranscriptionFactorFilter(Option parent,FunctionJobControl jobControl, DataElementPath classificationsRoot )
    {
        super(parent);

        setProgress( jobControl,0.3 );
        taxonFilter = new TaxonFilter(classificationsRoot);

        setProgress( jobControl,0.1 );
        speciesFilter = new SpeciesFilter(classificationsRoot);
        setProgress( jobControl,0.7 );
        classificationFilter = new ClassificationFilter(classificationsRoot);
        setProgress( jobControl,0.2 );
        domainFilter = new DomainFilter(classificationsRoot);
        setProgress( jobControl,0.2 );
        nameFilter = new FactorNameFilter();
        setProgress( jobControl,0.2 );
        positiveTissueSpecificityFilter = new PositiveTissueSpecificityFilter();
        setProgress( jobControl,0.2 );
        negativeTissueSpecificityFilter = new NegativeTissueSpecificityFilter();
        setProgress( jobControl,1 );
    }

    public Filter[] getFilter()
    {
        return new Filter[] {taxonFilter, speciesFilter, classificationFilter, domainFilter, nameFilter, positiveTissueSpecificityFilter,
                negativeTissueSpecificityFilter};
    }

    private void setProgress( FunctionJobControl jobControl, double part )
    {
        if( jobControl!=null )
        {
            int curr = jobControl.getPreparedness();
            int last = 100 - curr;
            int next = curr + (int)(last*part);
            jobControl.setPreparedness( next );
        }
    }

    /**
     * To satisfy this filter <code>ru.biosoft.access.core.DataElement</code> should satisfy <b>all</b>
     * all subfilters.
     */
    @Override
    public boolean isAcceptable(TransfacTranscriptionFactor de)
    {
        if( taxonFilter.isEnabled() )
        {
            ClassificationUnit dataElement = de.getTaxon();
            if( dataElement==null || !taxonFilter.isAcceptable(dataElement) )
                return false;
        }
        if( speciesFilter.isEnabled() )
        {
            ClassificationUnit dataElement = de.getTaxon();
            if( dataElement==null || !speciesFilter.isAcceptable(dataElement) )
                return false;
        }
        if( domainFilter.isEnabled() )
        {
            ClassificationUnit dataElement = de.getDNABindingDomain();
            if( dataElement == null || !domainFilter.isAcceptable(dataElement) )
                return false;
        }
        if( classificationFilter.isEnabled() )
        {
            ClassificationUnit dataElement = de.getGeneralClass();
            if( dataElement == null || !classificationFilter.isAcceptable(dataElement) )
                return false;
        }
        if(nameFilter.isEnabled() && (!nameFilter.isAcceptable(de)))
        {
            return false;
        }
        if(positiveTissueSpecificityFilter.isEnabled() && (!positiveTissueSpecificityFilter.isAcceptable(de)))
        {
            return false;
        }
        if(negativeTissueSpecificityFilter.isEnabled() && (!negativeTissueSpecificityFilter.isAcceptable(de)))
        {
            return false;
        }
        return true;
    }

    ////////////////////////////////////////
    // Subfilter classes
    //


    //----- Taxon Filter ---------------------------------------------/

    public static class TaxonFilter extends MutableFilter<ClassificationUnit>
    {
        private final DataElementPath classificationsRoot;

        public TaxonFilter(DataElementPath classificationsRoot)
        {
            taxonFilterFullNames = ( String [] )(new MessageBundle()).getObject( "TAXON_FILTER_TYPES_FULL_NAMES" );
            this.classificationsRoot = classificationsRoot;
            setTaxonIndex( 0 );
            setEnabled( false );
        }

        private String[] taxonFilterFullNames = null;
        private ClassificationUnit taxon;

        @Override
        public boolean isAcceptable(ClassificationUnit cu)
        {
            if(taxon == null)
            {
                return true;
            }
            return taxon.isAncestor(cu);
        }

        private int taxonIndex = 0;
        public int getTaxonIndex()
        {
            return taxonIndex;
        }

        public void setTaxonIndex(int taxonIndex)
        {
            int oldValue = this.taxonIndex;
            this.taxonIndex = taxonIndex;

            if(classificationsRoot == null)
            {
                taxon = null;
                return;
            }
            taxon = classificationsRoot.getRelativePath(taxonFilterFullNames[taxonIndex]).getDataElement(ClassificationUnit.class);
            if( cat.isLoggable( Level.FINE ) )
                cat.log( Level.FINE, "taxonFilterFullNames[" + taxonIndex + "] = " + taxonFilterFullNames[taxonIndex] + ", dc = " + taxon );
            firePropertyChange( "taxonIndex", oldValue, taxonIndex );
        }

    }

    public static class TaxonTagEditor extends TagEditorSupport
    {
        public TaxonTagEditor()
        {
            super( "ru.biosoft.bsa.filter.MessageBundle", TaxonTagEditor.class, "TAXON_FILTER_TYPES", 0);
        }
    }

    public static class TaxonFilterBeanInfo extends FilterBeanInfo
    {
        public TaxonFilterBeanInfo()
        {
            super(TaxonFilter.class, "TAXON_FILTER", "taxonIndex");
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            addHidden(new ChoicePropertyDescriptorEx("taxonIndex", beanClass, TaxonTagEditor.class ));
        }
    }

    //----- Species Filter ---------------------------------------------/

    public static class SpeciesFilter extends MutableFilter<ClassificationUnit>
    {
        private final DataElementPath classificationsRoot;

        public SpeciesFilter(DataElementPath classificationsRoot)
        {
            setEnabled( false );
            this.classificationsRoot = classificationsRoot;
        }

        @Override
        public boolean isAcceptable(ClassificationUnit cu)
        {
            return cu.getName().equals(species);
        }

        private String species = "human, Homo sapiens";

        public String getSpecies()
        {
            return species;
        }

        public void setSpecies(String species)
        {
            String oldValue = this.species;
            this.species = species;
            firePropertyChange("species", oldValue, species);
        }
    }

    public static class SpeciesFilterTagEditor extends StringTagEditor
    {
        static String[] tags;
        static DataElementPath tagsPath;

        private static void updateSpeciesList(DataElementPath newTagsPath)
        {
            if(tagsPath == null || !tagsPath.equals(newTagsPath))
            {
                tagsPath = newTagsPath;
                ClassificationUnit taxonClassification = tagsPath == null ? null : tagsPath.getRelativePath(Const.TAXON_CLASSIFICATION)
                        .getDataElement(ClassificationUnit.class);
                if(taxonClassification != null)
                {
                    Set<String> names = findSpecies(taxonClassification);
                    tags = names.toArray(new String[names.size()]);
                }
                else
                {
                    tags = new String[]{""};
                }
            }
        }

        // recuirsive find species in taxons tree
        static private Set<String> findSpecies(ClassificationUnit dc)
        {
            Set<String> names = new TreeSet<>();
            if(dc.isEmpty())
            {
                names.add(dc.getName());
                return names;
            }
            for(ClassificationUnit child: dc)
            {
                names.addAll(findSpecies(child));
            }
            return names;
        }

        @Override
        public String[] getTags()
        {
            updateSpeciesList(((SpeciesFilter)getBean()).classificationsRoot);
            return tags.clone();
        }
    }

    public static class SpeciesFilterBeanInfo extends FilterBeanInfo
    {
        public SpeciesFilterBeanInfo()
        {
            super(SpeciesFilter.class, "SPECIES_FILTER", "species");
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            addHidden(new PropertyDescriptorEx("species", beanClass), SpeciesFilterTagEditor.class);
        }
    }

    //----- Classification Filter ---------------------------------------------/

    public static class ClassificationFilter extends MutableFilter<ClassificationUnit>
    {
        private final DataElementPath classificationsRoot;

        public ClassificationFilter(DataElementPath classificationsRoot)
        {
            this.classificationsRoot = classificationsRoot;
            setEnabled( false );
            setClassificationIndex(ClassificationFilterTagEditor.getDefaultValue(classificationsRoot));
        }

        private DataElementPath factorClassName;
        @Override
        public boolean isAcceptable(ClassificationUnit de)
        {
            if(factorClassName == null)
                return true;

            return factorClassName.isAncestorOf(DataElementPath.create(de));
        }

        private String classificationIndex = "";
        public String getClassificationIndex()
        {
            return classificationIndex;
        }

        public void setClassificationIndex(String classificationIndex)
        {
            String oldValue = this.classificationIndex;
            this.classificationIndex = classificationIndex;
            factorClassName = ClassificationFilterTagEditor.getClassificationPathByTag(classificationIndex);
            firePropertyChange( "classificationIndex", oldValue, classificationIndex );
        }
    }

    public static class ClassificationFilterTagEditor extends StringTagEditor
    {
        static SortedMap<String, DataElementPath> tags = new TreeMap<>();
        static DataElementPath tagsPath;

        private static void updateClassifications(DataElementPath newTagsPath)
        {
            if(tagsPath == null || !tagsPath.equals(newTagsPath))
            {
                tagsPath = newTagsPath;
                ClassificationUnit tfClassification = tagsPath == null ? null : tagsPath.getRelativePath(
                        Const.TRANSCRIPTION_FACTOR_CLASSIFICATION).getDataElement(ClassificationUnit.class);
                tags.clear();
                if(tfClassification != null)
                {
                    flatClassification(tfClassification, false);
                }
            }
        }

        public static String getDefaultValue(DataElementPath newTagsPath)
        {
            updateClassifications(newTagsPath);
            return tags.size()>0?tags.keySet().iterator().next():"";
        }

        public static DataElementPath getClassificationPathByTag(String tag)
        {
            return tags.get(tag);
        }

        // Recursive find classifications
        static private void flatClassification(ClassificationUnit cu, boolean addItself)
        {
            if( addItself )
            {
                String tag = "";
                if( cu.getClassNumber() != null )
                    tag = cu.getClassNumber() + ". ";
                tags.put(tag + cu.getClassName(), DataElementPath.create(cu));
            }

            for(int i=0; i<cu.getSize(); i++)
                flatClassification(cu.getChild(i), true);
        }

        @Override
        public String[] getTags()
        {
            updateClassifications(((ClassificationFilter)getBean()).classificationsRoot);
            return tags.keySet().toArray(new String[tags.size()]);
        }
    }

    public static class ClassificationFilterBeanInfo extends FilterBeanInfo
    {
        public ClassificationFilterBeanInfo()
        {
            super(ClassificationFilter.class, "CLASSIFICATION_FILTER", "classificationIndex");
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            addHidden(new ChoicePropertyDescriptorEx("classificationIndex", beanClass, ClassificationFilterTagEditor.class ));
        }
    }

    //----- Domain Filter ---------------------------------------------/
    public static class DomainFilter extends MutableFilter<ClassificationUnit>
    {
        private final DataElementPath classificationsRoot;

        public DomainFilter(DataElementPath classificationsRoot)
        {
            this.classificationsRoot = classificationsRoot;
            setDomainIndex(DomainFilterTagEditor.getDefaultValue(classificationsRoot));
            setEnabled( false );
        }

        @Override
        public boolean isAcceptable(ClassificationUnit cu)
        {
            if(domainIndex == null)
                return true;
            return domainIndex.equals( cu.getClassName() );
        }

        private String domainIndex;
        public String getDomainIndex()
        {
            return domainIndex;
        }

        public void setDomainIndex(String domainIndex)
        {
            String oldValue = this.domainIndex;
            this.domainIndex = domainIndex;
            firePropertyChange("domainIndex", oldValue, domainIndex);
        }
    }

    public static class DomainFilterTagEditor extends StringTagEditor
    {
        static SortedSet<String> tags = new TreeSet<>();
        static DataElementPath tagsPath;

        private static void updateDomains(DataElementPath newTagsPath)
        {
            if(tagsPath == null || !tagsPath.equals(newTagsPath))
            {
                tagsPath = newTagsPath;
                ClassificationUnit domainClassification = tagsPath == null ? null : tagsPath.getRelativePath(
                        Const.DNA_BINDING_DOMAIN_CLASSIFICATION).getDataElement(ClassificationUnit.class);
                tags.clear();
                if( domainClassification != null )
                {
                    for( int i = 0; i < domainClassification.getSize(); i++ )
                    {
                        ClassificationUnit domain = domainClassification.getChild(i);
                        tags.add(domain.getClassName());
                    }
                }
            }
        }

        public static String getDefaultValue(DataElementPath newTagsPath)
        {
            updateDomains(newTagsPath);
            return tags.size()>0?tags.iterator().next():"";
        }

        @Override
        public String[] getTags()
        {
            updateDomains(((DomainFilter)getBean()).classificationsRoot);
            return tags.toArray(new String[tags.size()]);
        }
    }

    public static class DomainFilterBeanInfo extends FilterBeanInfo
    {
        public DomainFilterBeanInfo()
        {
            super(DomainFilter.class, "DOMAIN_FILTER", "domainIndex");
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            addHidden(new ChoicePropertyDescriptorEx("domainIndex", beanClass, DomainFilterTagEditor.class ));
        }
    }

    //----- Name Filter ---------------------------------------------/

    public static class FactorNameFilter extends PatternFilter<TransfacTranscriptionFactor>
    {
        FactorNameFilter()
        {
            setPattern(pattern);
        }

        @Override
        public String getCheckedProperty(TransfacTranscriptionFactor de)
        {
            return de.getDisplayName();
        }
    }


    public static class FactorNameFilterBeanInfo extends PatternFilterBeanInfo
    {
        public FactorNameFilterBeanInfo()
        {
            super(FactorNameFilter.class, "FACTOR_NAME_FILTER");
        }
    }

    //----- PositiveTissueSpecificity Filter ---------------------------------------------/

    public static class PositiveTissueSpecificityFilter extends PatternFilter<TransfacTranscriptionFactor>
    {
        @Override
        public String getCheckedProperty(TransfacTranscriptionFactor de)
        {
            return de.getPositiveTissueSpecificity();
        }
    }

    public static class PositiveTissueSpecificityFilterBeanInfo extends PatternFilterBeanInfo
    {
        public PositiveTissueSpecificityFilterBeanInfo()
        {
            super(PositiveTissueSpecificityFilter.class, "POSITIVE_TISSUE_SPECIFICITY_FILTER");
        }
    }

    //----- NegativeTissueSpecificity Filter ---------------------------------------------/

    public static class NegativeTissueSpecificityFilter extends PatternFilter<TransfacTranscriptionFactor>
    {
        @Override
        public String getCheckedProperty(TransfacTranscriptionFactor de)
        {
            return de.getNegativeTissueSpecificity();
        }
    }

    public static class NegativeTissueSpecificityFilterBeanInfo extends PatternFilterBeanInfo
    {
        public NegativeTissueSpecificityFilterBeanInfo()
        {
            super(NegativeTissueSpecificityFilter.class, "NEGATIVE_TISSUE_SPECIFICITY_FILTER");
        }
    }
}
