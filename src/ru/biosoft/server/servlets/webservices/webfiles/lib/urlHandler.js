/**
 * Handle hash URLs
 */
var currentHashStr;
var paramHash = {};
var defaultHash;

function parseURL(hash)
{
    if(hash.substring(0,1) == "#") hash = hash.substring(1);
    var params = hash.split("&");
    var paramHash = {};
    function decode(s)
    { 
        return decodeURIComponent( s.replace(/\+/g, '%20') );
    }
    for(var i=0; i<params.length; i++)
    {
        var fields = params[i].split("=");
        if(fields.length == 2)
        {
            paramHash[decode(fields[0])] = decode(fields[1]); 
        }
        else
        {
            paramHash = decode(fields[0]); //special case
        }
    }
    return paramHash;
}

function processURL(hash)
{
    paramHash = hash;
    if (paramHash.de) 
    {
        var des = paramHash.de.split("|");
        var action = paramHash.action;
        for(var i=0; i < des.length; i++)
        {
            var de = des[i];
            if(action)
            {
                performTreeAction(de, action);
            } else
            {
                if(opennedDocuments[activeDocumentId] && getTargetPath(opennedDocuments[activeDocumentId].completeName) == getTargetPath(de))
                {
                    if(opennedDocuments[activeDocumentId].onHashChange)
                    {
                        opennedDocuments[activeDocumentId].onHashChange(paramHash);
                    }
                } else
                {
                    if(!paramHash.docOnly) 
                        openBranchOrDefault(de, false);
                    openDocument(de);
                    
                }
            }
        }
    }
    if(paramHash.view && opennedDocuments[paramHash.view]) documentTabs.tabs('select', getJQueryIdSelector(paramHash.view)); 
}

function updateURL()
{
    var name2hash = "#";
    if (opennedDocuments[activeDocumentId] && opennedDocuments[activeDocumentId].completeName) 
    {
        var name = opennedDocuments[activeDocumentId].completeName;
        if(!getTreeNode(name))
        {
            var backPath = _.find(getBackPaths(name), function(backPath) {return getTreeNode(backPath);});
            if(backPath) name = backPath;
        }
        name2hash = "#de=" + encodeURIComponent(name).replace(/\%2F/g, "/");
        if(opennedDocuments[activeDocumentId].getHashParameters)
        {
            var addParams = toURI(opennedDocuments[activeDocumentId].getHashParameters());
            if(addParams != "")
                name2hash += "&"+addParams.replace(/\%2F/g, "/").replace(/\%3A/g, ":");
        }
        var dc = getDataCollection(getElementPath(name));
        var title = undefined;
        if(dc != null)
        {
            var ei = dc.getElementInfo(getElementName(name));
            if(ei != null)
                title = ei.title;
        }
        if(!title) title = getElementName(name);
        document.title = title+" - "+defaultTitle;
    } else
    {
        document.title = defaultTitle;
    }
    if(document.location.hash.indexOf("dialog=geImport") > -1) name2hash="#geImport";
    currentHashStr = name2hash;
    document.location.hash = name2hash;
}

