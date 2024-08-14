package ru.biosoft.access;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import one.util.streamex.StreamEx;
import ru.biosoft.access.core.CollectionFactory;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.access.core.DataElementPathSet;

import ru.biosoft.util.TextUtil;

@SuppressWarnings ( "serial" )
public class WildcardPathSet extends DataElementPathSet
{
    public WildcardPathSet(String globPaths)
    {
        if(globPaths.isEmpty())
            return;
        List<ru.biosoft.access.core.DataElementPath> result = new ArrayList<>();
        
        for(String globPath : TextUtil.split( globPaths, ';' ))
        {
            if(!globPath.contains( "/" ))
            {
                globPath = getPath().getChildPath( globPath ).toString();
            }
            String[] pathComponents = TextUtil.split( globPath, '/' );
            Pattern pattern = Pattern.compile(TextUtil.wildcardToRegex(pathComponents[0]));

            for( String root : CollectionFactory.getRootNames() )
                if(pattern.matcher(root).matches())
                    result.add(DataElementPath.create(root));
            
            for(int i = 1; i < pathComponents.length && !result.isEmpty(); i++)
            {
                Pattern subPattern = Pattern.compile(TextUtil.wildcardToRegex(pathComponents[i]));
                result = StreamEx.of(result).<ru.biosoft.access.core.DataElementPath>flatMap(
                            path -> StreamEx.of(path).map( DataElementPath::optDataCollection ).nonNull()
                                .flatMap(dc -> dc.names())
                                .filter( name -> subPattern.matcher( name ).matches() )
                                .map(name -> path.getChildPath( name ))).toList();
            }
          
            addAll(result);
        }
    }
    
}