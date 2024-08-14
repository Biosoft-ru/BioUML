/*
 * get cookie by name
 */
function getCookie(name)
{
    var cookie = " " + document.cookie;
    var search = " " + name + "=";
    var setStr = null;
    var offset = 0;
    var end = 0;
    if (cookie.length > 0)
    {
        offset = cookie.indexOf(search);
        if (offset != -1)
        {
            offset += search.length;
            end = cookie.indexOf(";", offset)
            if (end == -1)
            {
                end = cookie.length;
            }
            setStr = unescape(cookie.substring(offset, end));
        }
    }
    return(setStr);
}

/*
 * Check if 'sid' cookie is set.
 * If so, transfer it to BioUML server, so it can log in to ExPlain as current user
 * This works with BKL and PORTAL authentications, doesn't work with HTTP authentication
 * Sending synchronous ajax query to ensure it will be complete before any other call
 */
function authExPlain()
{
    var sid = getCookie('sid');
    if(sid)
    {
        var req = $.ajax();
        req.open("POST", "../biouml/javascript/executeScript", false);
        var params = "preprocessor=JavaScript&script=explain.auth('sid:"+sid+"')";
        req.setRequestHeader("Content-type", "application/x-www-form-urlencoded");
        req.setRequestHeader("Content-length", params.length);
        req.send(params);
    }
}
// Call it automatically
authExPlain();
