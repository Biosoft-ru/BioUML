package ru.biosoft.bsa.filter;

import ru.biosoft.access.core.DataElement;
import ru.biosoft.access.core.filter.PatternFilter;

@SuppressWarnings ( "serial" )
public class NameFilter extends PatternFilter<DataElement>
{
    @Override
    public String getCheckedProperty(DataElement de)
    {
        return de.getName();
    }
}
