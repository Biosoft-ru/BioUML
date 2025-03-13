var bioumlLocale = Cookies.getItem( "bioumlLocale" ) ||
                   navigator.language || navigator.userLanguage;

var resources = resourcesEN;

if( bioumlLocale == 'ru' && hasBioumlLocale( 'ru' ) )
{
    resources = resourcesRU;
}

function setBioumlLocale( locale )
{
    Cookies.setItem( "bioumlLocale", locale );
    window.location.reload();
}

function hasBioumlLocale( locale )
{
    if( locale == 'en' )
    {
        return true;
    }

    if( locale == 'ru' && appInfo.enableLocales && appInfo.enableLocales.indexOf( 'ru' ) >= 0 )
    {
        return typeof resourcesRU != 'undefined'
    }

    if( locale == 'de' && appInfo.enableLocales && appInfo.enableLocales.indexOf( 'de' ) >= 0 )
    {
        return typeof resourcesDE != 'undefined'
    }

    return false;
}