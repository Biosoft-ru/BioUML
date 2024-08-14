package biouml.plugins.gtrd.master.sites.bsaconv;

import ru.biosoft.bsa.Site;

@FunctionalInterface
public interface SiteConverter<T>
{
    T createFromSite(Site s);
}