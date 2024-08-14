function getPreference(name)
{
    var parentPrefs = currentUserPreferences;
    var ind = name.indexOf('/');
    while(ind != -1)
    {
        var parentName = name.substring(0, ind );
        parentPrefs = parentPrefs.getValue(parentName);
        if( parentPrefs == null )
            return null;
        name = name.substring(ind+1);
        ind = name.indexOf('/');
    }
    return parentPrefs.getValue(name);
}

function setPreference(name, value, callback)
{
    var requestParameters =  {
            name: name,
            value: value
        };
        queryBioUML("web/preferences/add", requestParameters, function(data){
            currentUserPreferences = convertJSONToDPS(data.values);
            if(callback)
                callback();
        });
}

function getPathFromPreferences(childClass, elementClass)
{
    var key = getPathPreferencesKey(childClass, elementClass);
    var path = getPreference("DataCollectionPaths/" + key);
    if (path != null) 
        return path;
    else
        return appInfo.userProjectsPath;
}

function storePathToPreferences(childClass, elementClass, value)
{
    var key = getPathPreferencesKey(childClass, elementClass);
    var pathCollection = currentUserPreferences.getValue("DataCollectionPaths");
    var pathValue = getElementPath(value);
    var pathProperty = new DynamicProperty(key, "code-string", pathValue);
    if (pathCollection && pathCollection.getValue(key) && pathCollection.getValue(key) == pathValue) 
        return;
    setPreference("DataCollectionPaths/" + key, pathValue);
}

function getPathPreferencesKey(childClass, elementClass)
{
    var key = null;
    if( childClass )
    {
        key = getSimpleClassName(childClass);
    }
    else if( elementClass )
    {
        key = getSimpleClassName(elementClass);
    }
    else
    {
        key = "(default)";
    }
    return key;
}

