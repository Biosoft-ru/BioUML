package biouml.plugins.ensembl.type;

import ru.biosoft.access.core.DataCollection;
import ru.biosoft.bsa.Site;
import ru.biosoft.bsa.WithSite;

public class Transcript extends biouml.standard.type.Transcript implements WithSite
{
    private Site site;

    public Transcript(DataCollection origin, String name, Site site)
    {
        super( origin, name );
        this.site = site;
    }

    @Override
    public Site getSite()
    {
        return site;
    }

}
