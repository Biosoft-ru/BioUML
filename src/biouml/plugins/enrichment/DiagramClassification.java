package biouml.plugins.enrichment;

import java.util.Map;
import java.util.function.Function;

import biouml.standard.type.Base;
import ru.biosoft.access.core.DataCollection;
import ru.biosoft.access.core.DataElementPath;
import ru.biosoft.util.Cache;

public class DiagramClassification extends FunctionalClassification
{
    public DiagramClassification(DataCollection<?> origin, String name)
    {
        super( origin, name, new DiagramClassificationParameters() );
    }

    @Override
    protected Function<String, String> getNameFunction()
    {
        Map<String, DataElementPath> map = ((DiagramHub)getParameters().getFunctionalHub()).getKernelMap();
        return Cache.hard( hit -> {
            DataElementPath path = map.get( hit );
            if(path != null) {
                Base base = path.optDataElement( Base.class );
                if(base != null)
                    return base.getTitle();
            }
            return hit;
        });
    }


}
