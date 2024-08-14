package ru.biosoft.bsa.filter;

import ru.biosoft.access.core.filter.CompositeFilter;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.filter.MutableFilter;
import ru.biosoft.access.core.filter.PatternFilter;
import ru.biosoft.bsa.Basis;
import ru.biosoft.bsa.BasisEditor;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.StrandEditor;
import ru.biosoft.bsa.StrandType;

import com.developmentontheedge.beans.ChoicePropertyDescriptorEx;
import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;
import ru.biosoft.jobcontrol.FunctionJobControl;

@SuppressWarnings ( "serial" )
public class SiteFilter extends CompositeFilter<Site>
{
    protected NameFilter nameFilter;
    protected TypeFilter typeFilter;
    protected BasisFilter basisFilter;
    protected StrandFilter strandFilter;
    //protected MatrixFilter matrixFilter;
    protected CutOffFilter cutOffFilter;
    protected CoreCutOffFilter coreCutOffFilter;

    public SiteFilter(Option parent, FunctionJobControl jobControl )
    {
        super(parent);

        setJobProgress( jobControl,2 );
        add(nameFilter       = new SiteIDFilter());
        setJobProgress( jobControl,4 );
        add(typeFilter       = new TypeFilter());
        setJobProgress( jobControl,6 );
        add(basisFilter      = new BasisFilter());
        setJobProgress( jobControl,8 );
        add(strandFilter     = new StrandFilter());
        setJobProgress( jobControl,10 );
        add(cutOffFilter     = new CutOffFilter());
        setJobProgress( jobControl,12 );
        add(coreCutOffFilter = new CoreCutOffFilter());
        setJobProgress( jobControl,16 );
        //add(matrixFilter     = new MatrixFilter(jobControl));
    }

    public SiteFilter(Option parent, int basis, FunctionJobControl jobControl )
    {
        this(parent,jobControl);

        basisFilter.setBasis(basis);
        basisFilter.setEnabled(true);
    }

    //for serialization
    public SiteFilter(String siteSetName, int basis)
    {
        this(null, DataElementPath.create(siteSetName).getDataCollection(Site.class), basis, null);
    }

    public SiteFilter(Option parent, DataCollection<Site> siteSet, int basis, FunctionJobControl jobControl )
    {
        this(parent,jobControl);
        this.siteSet = siteSet;

        basisFilter.setBasis(basis);
        basisFilter.setEnabled(true);
    }

    //for serialization
    private DataCollection<Site> siteSet;
    //for serialization
    public String getSiteSetName()
    {
        String name = null;
        if (siteSet!=null)
        {
            name = siteSet.getCompletePath().toString();
        }
        return name;
    }

    //for serialization
    public int getBasis()
    {
        return basisFilter.getBasis();
    }

    private void setJobProgress( FunctionJobControl jobControl, int progress )
    {
        if( jobControl!=null )
            jobControl.setPreparedness( progress );
    }


    /** @todo implement */
    @Override
    public boolean isAcceptable(Site de)
    {
        if (nameFilter.isEnabled() && !nameFilter.isAcceptable(de))
            return false;

        if (typeFilter.isEnabled() && !typeFilter.isAcceptable(de))
            return false;

        if (basisFilter.isEnabled() && !basisFilter.isAcceptable(de))
            return false;

        if (strandFilter.isEnabled() && !strandFilter.isAcceptable(de))
            return false;

        if (de.getBasis()!=Basis.BASIS_ANNOTATED &&
            cutOffFilter.isEnabled() && !cutOffFilter.isAcceptable(de))
            return false;

        if (de.getBasis()!=Basis.BASIS_ANNOTATED &&
            coreCutOffFilter.isEnabled() && !coreCutOffFilter.isAcceptable(de))
            return false;

        //if(matrixFilter.isEnabled())
        //{
        //    return false;
        //}
        return true;
    }

    ////////////////////////////////////////
    // Subfilter classes
    //

    //----- Type Filter ----------------------------------------------/
    public static class TypeFilter extends PatternFilter<Site>
    {
        @Override
        public String getCheckedProperty(Site de)
        {
            return de.getType();
        }
    }

    public static class TypeFilterBeanInfo extends PatternFilterBeanInfo
    {
        public TypeFilterBeanInfo()
        {
            super(TypeFilter.class, "TYPE_FILTER");
        }
    }

    //----- Basis Filter ---------------------------------------------/

    public static class BasisFilter extends MutableFilter<Site>
    {
        public BasisFilter()
        {
            this.basis = Basis.BASIS_PREDICTED;
//            this.basis = Basis.BASIS_BOTH;
            setEnabled(false);
        }

        protected int basis;
        public int getBasis()
        {
            return basis;
        }

        public void setBasis(int basis)
        {
            int oldValue = this.basis;
            this.basis = basis;
            firePropertyChange("basis", oldValue, basis);
        }

        @Override
        public boolean isAcceptable(Site de)
        {
            int siteBasis = de.getBasis();
            if (basis == Basis.BASIS_USER_ANNOTATED)
                return siteBasis == Basis.BASIS_USER || siteBasis == Basis.BASIS_ANNOTATED;
            return siteBasis == basis;
        }
    }

    public static class BasisFilterBeanInfo extends FilterBeanInfo
    {
        public BasisFilterBeanInfo()
        {
            super(BasisFilter.class, "BASIS_FILTER", "basis");
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            addHidden(new ChoicePropertyDescriptorEx("basis", beanClass, BasisEditor.class));
        }
    }

    //----- Strand Filter ---------------------------------------------/

    public static class StrandFilter extends MutableFilter<Site>
    {
        public StrandFilter()
        {
            this.strand = StrandType.STRAND_PLUS;
            setEnabled(false);
        }

        protected int strand;
        public int getStrand()
        {
            return strand;
        }

        public void setStrand(int strand)
        {
            int oldValue = this.strand;
            this.strand = strand;
            firePropertyChange("strand", oldValue, strand);
        }

        @Override
        public boolean isAcceptable(Site de)
        {
            return strand == de.getStrand();
        }
    }

    public static class StrandFilterBeanInfo extends FilterBeanInfo
    {
        public StrandFilterBeanInfo()
        {
            super(StrandFilter.class, "STRAND_FILTER", "strand");
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            addHidden(new ChoicePropertyDescriptorEx("strand", beanClass, StrandEditor.class));
        }
    }

    //----- CutOff Filter --------------------------------------------/

    /**
     * @pending move to de.biobase.matchpro.filter
     * TODO: implement or remove
     */
    public static class CutOffFilter extends MutableFilter<Site>
    {
        public CutOffFilter()
        {
            setEnabled(false);
        }

        @Override
        public boolean isAcceptable( Site de )
        {
            return false;
        }

        private double cutOff;
        public double getCutOff()
        {
            return cutOff;
        }

        public void setCutOff( double cutOff )
        {
            double oldValue = this.cutOff;
            this.cutOff = cutOff;
            firePropertyChange( "cutOff", oldValue, cutOff );
        }
    }

    public static class CutOffFilterBeanInfo extends FilterBeanInfo
    {
        public CutOffFilterBeanInfo()
        {
            super(CutOffFilter.class, "CUTOFF_FILTER", "cutOff");
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            addHidden(new PropertyDescriptorEx("cutOff", beanClass));
        }
    }
    //----- CoreCutOff Filter ----------------------------------------/

    /**
     * @pending move to de.biobase.matchpro.filter
     */
    public static class CoreCutOffFilter extends CutOffFilter
    {
        //////////
        //TODO: Use de.biobase.matchpro.analysis.MatchMethodInfo after it will be refactored.
        //protected double getWeight(de.biobase.matchpro.analysis.MatchMethodInfo matchMethodInfo)
        //{
        //    return matchMethodInfo.getCoreWeight();
        //}
        //////////
    }

    public static class CoreCutOffFilterBeanInfo extends FilterBeanInfo
    {
        public CoreCutOffFilterBeanInfo()
        {
            super(CoreCutOffFilter.class, "CORE_CUTOFF_FILTER", "cutOff");
        }

        @Override
        public void initProperties() throws Exception
        {
            super.initProperties();
            addHidden(new PropertyDescriptorEx("cutOff", beanClass));
        }
    }
}
