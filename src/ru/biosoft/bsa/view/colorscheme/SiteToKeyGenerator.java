package ru.biosoft.bsa.view.colorscheme;

import ru.biosoft.bsa.Site;

/**
* Interface for site-to-key generator.
* If this one implementation of this cannot generate key
* for the site, then isSuitable returns false.
*/
abstract public class SiteToKeyGenerator
{
    /**
    * Is the <CODE>site</CODE> suitable for this generator
    * @return true if a site is suitable for this generator
    */
    abstract public boolean isSuitable(Site site);
    /**
    * Get key for site
    * @param site a site
    * @return key
    */
    abstract public String getKey(Site site);
}


