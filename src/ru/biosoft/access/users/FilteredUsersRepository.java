package ru.biosoft.access.users;

import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import ru.biosoft.jobcontrol.FunctionJobControl;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.filter.Filter;
import ru.biosoft.access.core.filter.FilteredDataCollection;

/**
 *  {@link UsersRepository} with automatically group filtering
 */
public class FilteredUsersRepository extends FilteredDataCollection<UserGroup>
{
    public FilteredUsersRepository(DataCollection<?> parent, Properties properties) throws Exception
    {
        super(parent, properties);
    }
    
    @Override
    public UserGroup get(String name) throws Exception
    {
        if( !contains(name) )
            return null;
        return super.get(name);
    }

    @Override
    protected List<String> getFilteredNames()
    {
        Filter<? super UserGroup> filter = getFilter();
        return primaryCollection.stream().filter( filter::isAcceptable ).map( UserGroup::getName ).collect( Collectors.toList() );
    }

    @Override
    protected void initNames(FunctionJobControl jobControl)
    {
    }
}
