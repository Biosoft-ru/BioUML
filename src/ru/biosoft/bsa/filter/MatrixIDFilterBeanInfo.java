package ru.biosoft.bsa.filter;

//import ru.biosoft.access.CollectionFactory;
//import ru.biosoft.access.core.filter.CompositeFilter;
//import ru.biosoft.access.core.DataCollection;
//import ru.biosoft.access.core.DataElement;
//import ru.biosoft.access.core.filter.MutableFilter;

//import com.developmentontheedge.beans.Option;
import com.developmentontheedge.beans.PropertyDescriptorEx;

//import ru.biosoft.bsa.TranscriptionFactor;
//import ru.biosoft.bsa.classification.ClassificationUnit;
//import ru.biosoft.bsa.filter.TranscriptionFactorFilter;

//import de.biobase.matchpro.Const;
//import mgl3.util.List;
//import java.util.Set;
//import java.util.TreeSet;
//import java.util.Iterator;
//import java.util.TreeMap;
//import java.util.Map;
//import java.awt.LayoutManager;
//import java.awt.Component;
//import java.awt.Container;
//import java.awt.Dimension;
//import java.awt.Insets;

//import org.apache.oro.text.perl.Perl5Util;

public class MatrixIDFilterBeanInfo extends FilterBeanInfo
{
    public MatrixIDFilterBeanInfo()
    {
        super(MatrixIDFilter.class, "MATRIXID_FILTER", "pattern");
    }

    @Override
    public void initProperties() throws Exception
    {
        super.initProperties();
        addHidden(new PropertyDescriptorEx("pattern", beanClass));
    }
}
