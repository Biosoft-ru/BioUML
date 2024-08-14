/**
 * Core functions for BioUML web
 */
var logger = new BioUMLLogger();

var BioUML = {
    selection : {
        listeners : [],
        lastSelected : null,
        addListener : function(f) {
            BioUML.selection.listeners.push(f);
        },
        changed : function(elementPath) {
            if(BioUML.selection.lastSelected === elementPath) {
                return;
            }
            BioUML.selection.lastSelected = elementPath;
            for(var i=0; i<BioUML.selection.listeners.length; i++) {
                BioUML.selection.listeners[i](elementPath);
            }
        }
    },
    perspectiveNames: []
};

var rootMap = {};

var activeDC;
var currentUser = null;
var currentUserPreferences = null;
var defaultTitle;
var scriptTypes = {};
var invalidSession = false;
var lockInit = false;
var perspective;
var el = {};
var canUseExperimental = false;

function getWindowSize()
{
    var f;
    //IE
    if (!window.innerWidth) 
    {
        if (!(document.documentElement.clientWidth == 0)) 
        {
            // strict mode
            f = function() {
                return [document.documentElement.clientWidth, document.documentElement.clientHeight];
            };
        } else 
        {
            // quirks mode
            f = function() {
                return [document.body.clientWidth, document.body.clientHeight];
            };
        }
    } else 
    {
        // w3c
        f = function() {
            return [window.innerWidth, window.innerHeight];
        };
    }
    getWindowSize = f;
    return getWindowSize();
}

/**
 * Returns element path without name (everything before last slash)
 * 
 * @param {String}
 *            path - path to the element
 */
function getElementPath(path)
{
    if (path == undefined || path == null)     
        return path;
    var ind = path.lastIndexOf('/');
    if (ind == -1)
        return "";
    return path.substring(0, ind);
}

function isPathExists(path)
{
    logger.setQuietMode(true);
    var result = getDataCollection(getElementPath(path)).getElementInfo(getElementName(path)) != undefined;
    logger.setQuietMode(false);
    return result;
}

/*
 * Get DataCollection element by full path
 */
function getDataCollection(path)
{
    var dc;
    if(path == "") return null;
    var splitPath = getPathComponents(path);
    if(rootMap[splitPath[0]])
        dc = rootMap[splitPath[0]];
    else
    {
        dc = new DataCollection(splitPath[0]);
        rootMap[splitPath[0]] = dc;
    }
    for (var i = 1; i < splitPath.length; i++) 
    {
        dc = dc.get(splitPath[i]);
    }
    return dc;
}

/**
 * Returns real collection (following symlinks if any)
 * Warning: may block if some collections on the way weren't loaded before
 * @param path
 */
function getTargetCollection(path)
{
    if (path == undefined || path == null)     
        return null;
    var dc;
    var splitPath = getPathComponents(path);
    if(rootMap[splitPath[0]])
        dc = rootMap[splitPath[0]];
    else 
        dc = new DataCollection(splitPath[0]);
    if(dc.getLinkTarget())
        dc = getTargetCollection(dc.getLinkTarget());
    for (var i = 1; i < splitPath.length; i++) 
    {
        dc = dc.get(splitPath[i]);
        if(dc.getLinkTarget())
            dc = getTargetCollection(dc.getLinkTarget());
    }
    return dc;
}

/**
 * Returns real path (following symlinks if any)
 * Warning: may block if some collections on the way weren't loaded before
 * @param path
 */
function getTargetPath(path)
{
    var dc = getTargetCollection(path);
    return dc == null ? null : dc.completeName;
}

/**
 * Returns list of known paths which link to the path specified (including path itself)
 * @param path
 */
function getBackPaths(path)
{
    if (path == undefined || path == null)     
        return [];
    var splitPath = getPathComponents(path);
    var result = [""];
    for (var i = 0; i < splitPath.length; i++) 
    {
        var newLinks = [];
        for(var j=0; j<result.length; j++)
        {
            result[j] = result[j]?result[j]+"/"+splitPath[i]:splitPath[i];
            var dc = getDataCollection(result[j]);
            var backLinks = dc.getBackLinks();
            if(backLinks)
            {
                newLinks = newLinks.concat(backLinks);
            }
        }
        result = result.concat(newLinks);
    }
    return result;
}

/**
 * Returns element name without path (everything after last slash)
 * 
 * @param {String}
 *            path - path to the element
 */
function getElementName(path)
{
    if(path === undefined || path === null) return path;
    var ind = path.lastIndexOf('/');
    if(ind === -1) return path;
    return unescapeElementName(path.substring(ind + 1));
}

function getElementTitle(path)
{
    var name = getElementName(path);
    var title = getDataCollection(getElementPath(path)).getElementInfo(name).title;
    if(title != undefined) return title;
    return name;
};

function escapeElementName(name)
{
    return name.replace(/\\/g, "\\\\").replace(/\//g, "\\s");
}

function unescapeElementName(name)
{
    return name.replace(/\\s/g, "/").replace(/\\\\/g, "\\");
}

function createPath(path, name)
{
    return path+"/"+escapeElementName(name);
}

function createPathFromComponents(components)
{
    var result = escapeElementName(components[0]);
    for(var i=1; i<components.length; i++)
    {
        result = result+"/"+escapeElementName(components[i]);
    }
    return result;
}

function getPathComponents(path)
{
    var components = path.split("/");
    for(var i=0; i<components.length; i++) components[i] = unescapeElementName(components[i]);
    return components;
}

function getElementClass(path)
{
    if(path == undefined || path == null) return "ru.biosoft.access.core.DataElement";
    var parent = getElementPath(path);
    if(parent == "") return "ru.biosoft.access.core.DataElement";
    return getDataCollection(parent).getChildClass(getElementName(path));
}

// ///////////////////////////
// Utility functions
// //////////////////////////

/*
 * Random key generation
 */
var rndAdd = 1;
function rnd()
{
    return parseInt(String((new Date()).getTime()).replace(/\D/gi, ''))*100+(rndAdd++);
}

/**
 * Converts hash like {a: "foo", b: "bar"} to URI string like "a=foo&b=bar"
 * encoding it properly
 */
function toURI(h)
{
    if(!h) return "";
    var res = [];
    for(k in h)
        res.push(encodeURIComponent(k)+"="+encodeURIComponent(h[k]));
    return res.join("&");
}

/**
 * Returns POST-form which sends specified query
 * 
 * @param {String}
 *            action - URL pointing to form target
 * @param {Object}
 *            h - parameters hash
 * @return form jQuery object
 */
function toForm(action, h)
{
    var form = $('<form method="POST"/>').attr("action", action);
    for(k in h)
        form.append($('<input type="hidden"/>').attr("name", k).val(h[k]));
    return form;
}

function showElementInfo(completeName)
{
    BioUML.selection.changed(completeName);
}


function loadElementDescription (path)
{
    var descriptionViewPart = lookForViewPart('common.description');
    if(descriptionViewPart != null)
        descriptionViewPart.setElementDescription(path);
}

/**
 * Returns true if childClass Java class is an instance of parentClass
 * 
 * @param {String}
 *            childClass fully qualified Java child class name
 * @param {String}
 *            parentClass fully qualified Java parent class name
 * @param {Function}
 *            callback if present, will asynchronously call it after execution.
 *            If not, will return result
 */
function instanceOf(childClass, parentClass, callback)
{
    if(childClass == parentClass) return true;
    if(!childClass || !parentClass) return false;
    if(!instanceOf.classHierarchy)
    {
        instanceOf.classHierarchy = {};
    }
    function loadClassHierarchy(className, callback)
    {
        return queryBioUMLWithCallback('web/data',
        {
            service: "access.service",
            "class": className,
            command: 32
        }, function(data)
        {
            var result = $.evalJSON(data.values);
            instanceOf.parseClasses(result);
        }, function(data)
        {
        }, callback);
    }
    var checkInstanceOf = function()
    {
        var toCheck = [childClass];
        var checked = {};
        while(toCheck.length > 0)
        {
            var newToCheck = [];
            for(var i=0; i<toCheck.length; i++)
            {
                className = toCheck[i];
                if(instanceOf.classHierarchy[className] && instanceOf.classHierarchy[className].length)
                {
                    for(var j=0; j<instanceOf.classHierarchy[className].length; j++)
                    {
                        var parentClassName = instanceOf.classHierarchy[className][j];
                        if(parentClassName==parentClass) return true;
                        if(checked[parentClassName]) continue;
                        newToCheck.push(parentClassName);
                    }
                }
                checked[className] = 1;
            }
            toCheck = newToCheck;
        }
        return false;
    };
    if(!instanceOf.classHierarchy[childClass]) 
    {
        if(callback)
        {
            loadClassHierarchy(childClass, function() {callback(checkInstanceOf());});
            return;
        }
        loadClassHierarchy(childClass);
    }
    if(callback) callback(checkInstanceOf());
    else return checkInstanceOf();
}

instanceOf.parseClasses = function(result)
{
    if(!instanceOf.classHierarchy)
    {
        instanceOf.classHierarchy = {};
    }
    for(var fullClassName in result)
    {
        className = fullClassName.replace(/^\w+\s+/, "");
        var parents = result[fullClassName];
        for(var i=0;i<parents.length; i++)
        {
            parents[i] = parents[i].replace(/^\w+\s+/, "");
        }
        instanceOf.classHierarchy[className] = parents;
    }
};

function getNodeIcon(dc, name)
{
    var result = "url('icons/folder.png')";
    if (dc == undefined || dc == null)
    {
        return rootMap[name]?result:"url('icons/leaf.gif')";
    }
    var icon = dc.getNodeIcon(name);
    if(icon != undefined)
    {
        return "url('"+appInfo.serverPath+"web/img?id="+encodeURIComponent(icon)+"')";
    }
    var protection = dc.getProtectionStatus(name);
    var perm = dc.getPermission(name);
    // Protected collection: return protection status icon
    switch(protection)
    {
    case 0:
        return "url('icons/remoteNotProtectedDatabaseIcon.png')";
    case 1:
        return "url('icons/remotePublicReadDatabaseIcon.png')";
    case 2:
        if(perm & 0x04) // write allowed
            return "url('icons/remotePublicDatabaseIcon2.png')";
        else
            return "url('icons/remotePublicDatabaseIcon.png')";
    case 3:
        if (perm & 0x02) // read allowed
            return "url('icons/remoteProtectedReadDatabaseIcon2.png')";
        else
            return "url('icons/remoteProtectedReadDatabaseIcon.png')";
    case 4:
        if (perm & 0x02) // read allowed
            return "url('icons/remoteProtectedDatabaseIcon2.png')";
        else
            return "url('icons/remoteProtectedDatabaseIcon.png')";
    }
    var type = dc.getChildClass(name);
    if (!instanceOf(type,"ru.biosoft.access.core.DataCollection")) 
    {
        result = "url('icons/leaf.gif')";
    }
    return result;
}

/**
 * Sets perspective by perspective provider response
 * @param data
 */
function setPerspective(values)
{
    perspective = values.perspective;
    BioUML.perspectiveNames = values.names;
}

/**
 * Init perspective virtual folders, call server to get target paths for items
 * Should be called at startup to create correct links between real collections and symlinks 
 */
function initVirtualCollections()
{
    for(var i in perspective.repository)
    {
        if(perspective.repository[i].virtual)
        {
            var root = perspective.repository[i].path;
            getDataCollection(root).getNameList( function(nameList)
            {
                for (var j = 0; j < nameList.length; j++)
                {
                    getDataCollection(createPath(root, nameList[j].name)).initLinkTargets();
                }
            });
        }
    }
}

/*
 * BioUMLLogger class, renamed from Logger
 */
function BioUMLLogger()
{
    this.quietMode = false;
    
    var activeMessages = {};
    
    var _this = this;
    var dialog = function(message, title)
    {
        if( _this.quietMode || activeMessages[message] )
            return;
        activeMessages[message] = true;
        var dialogDiv = $('<div title="'+title+'"></div>');
        dialogDiv.html("<p>" + message.replace(/\n/g, "<br>") + "</p>");
        dialogDiv.dialog(
        {
            autoOpen: false,
            width: 500,
            modal: true,
            buttons: 
            {
                "Ok": function()
                {
                    delete activeMessages[message];
                    $(this).dialog("close");
                    $(this).remove();
                }
            },
            close: function()
            {
                delete activeMessages[message];
            }
        });
        addDialogKeys(dialogDiv);
        dialogDiv.dialog("open");
        dialogDiv.parent().find(":button:contains('Ok')").focus();
    };
    
    this.error = function(message)
    {
        dialog(message, resources.commonErrorBoxTitle);
    };
    
    this.message = function(message)
    {
        dialog(message, resources.commonMessageBoxTitle);
    };
    
    this.setQuietMode = function(mode)
    {
        this.quietMode = mode;    
    };
    
    this.debug = function(message)
    {
        dialog(message, "Debug");
    };
}

var JobControl = {
        /** Instance has been created but not yet executed. */
        CREATED: 0,

        /** The analysis instance is run. */
        RUNNING: 1,

        /** The analysis instance is paused. */
        PAUSED: 2,

        /** The instance has completed execution. */
        COMPLETED: 3,

        /** The instance was terminated by user request. */
        TERMINATED_BY_REQUEST: 4,

        /** The instance terminated due to an error. */
        TERMINATED_BY_ERROR: 5
    };

/**
 * Cancel job by given jobID
 */
function cancelJob(jobID)
{
    queryBioUML("web/jobcontrol/cancel",
    {
        jobID: jobID
    }, function() {}, function() {});
    if(helperFrame)
    {
        helperFrame.attr("src", "javascript:false");
    }
}

function getJQueryIdSelector(id)
{
    return "#"+getJQuerySelector(id);
}

function getJQuerySelector(str)
{
    //Special symbols should be escaped in selectors:
    // http://api.jquery.com/category/selectors/
    return str.replace(/([\#\;\&\,\.\+\*\~\'\:\"\!\^\$\[\]\(\)\=\>\<\|\/\ ])/g, "\\$1");
}

function processQuotedHref(quotedHref)
{
    var href = quotedHref.replace(/\\([\#\;\&\,\.\+\*\~\\"'\:\!\^\$\[\]\(\)\=\>\<\|\/\ ])/g, "$1");
    return href;
}

/////////////////////////////////////////
// Some useful string enhancements
/**
 * Escapes HTML special chars
 */
String.prototype.escapeHTML = function ()
{                                       
    return(                                                                 
        this.replace(/&/g,'&amp;').
            replace(/>/g,'&gt;').                                           
            replace(/</g,'&lt;').                                           
            replace(/"/g,'&quot;').
            replace(/'/g,'&#x27;')
    );                                                                     
};

/**
 * Make first symbol of supplied string upper case
 */
String.prototype.ucfirst = function()
{
    return this.charAt(0).toUpperCase()+this.substr(1);
};

function elementFromHTML(html)
{
    html = html.replace(/<(\/?)script[^>]*>/ig, "<$1noscript>");
    var element = $("<div/>").html(html);
    element.find("noscript").remove();
    element.find("head").remove();
    element.find("title").remove();
    element.find("meta").remove();
    element.find("link").remove();
    var children = element.children();
    if(children.filter('a[href="#skip-navbar_bottom"] + table,a[href="#skip-navbar_top"] + table').remove())
    element.find("a").each(function()
    {
        var href = $(this).attr("href");
        if(!href) return;
        if(href.match(/^#/))
        {
            if(href.match(/^#de=/))
                $(this).removeAttr("target");
            else
                $(this).click(function() {scrollDescriptionToAnchor(this); return false;});
        } else if(href.match(/^\.\.\//))
        {
            $(this).removeAttr("href");
        } else
        {
            $(this).attr("target", "_blank");
        }
    });
    element.find("img").each(function()
    {
        var src = $(this).attr("src");
        var m = src.match(/^(\w+)\:\/\//);
        if(m && m[1] != "http" && m[1] != "https")
        {
            $(this).attr("src", appInfo.serverPath+"web/img?id="+encodeURIComponent(src));
        }
    });
    return element;
}

function scrollDescriptionToAnchor(element)
{
    element = $(element);
    var anchor = element.attr("href").substring(1);
    var target = element.closest(".elementDescription").find("a[name="+anchor+"]");
    if(target.length == 0) return;
    var pos = target.get(0).offsetTop;
    element.closest(".elementDescriptionScrollPane").scrollTop(pos);
};

function getSimpleClassName(fullClassName)
{
    if (fullClassName == undefined || fullClassName == null)     
        return fullClassName;
    var ind = fullClassName.lastIndexOf('.');
    if (ind == -1)     
        return "";
    return fullClassName.substring(ind+1);
}

var browserApp = {};

function checkBrowserSupport()
{
    if (navigator.userAgent.match(/msie/i))
        return resources.commonErrorIncompatibleBrowser.replace("{version}", detectBrowserVersion()); 
    var canvas = document.createElement('canvas');
    var ctx = canvas.getContext('2d');
    var isSupported = true;
    if(ctx.fillText == undefined)
    {
        isSupported = false;
    }
    if(!isSupported)
        return resources.commonErrorIncompatibleBrowserVersion.replace("{version}", detectBrowserVersion());
    return "";
}

/*
 * Get good browser name with full version number. jQuery.browser return incorrect version in some cases.
 * jQuery.browser is removed since 1.9 
 */
function detectBrowserVersion(){
    
    var fullVersionName = navigator.appName + " " + navigator.appVersion;
    var userAgent = navigator.userAgent.toLowerCase();
    var version = navigator.appVersion;
    
    browserApp.chrome = navigator.userAgent.match(/chrome/i);
    browserApp.msie = navigator.userAgent.match(/msie/i) || navigator.userAgent.match(/trident/i);
    browserApp.mozilla = navigator.userAgent.match(/firefox/i);
    browserApp.safari = navigator.userAgent.match(/safari/i);
    browserApp.opera = navigator.userAgent.match(/opera/i) || navigator.userAgent.match(/opr/i);
    // Is this a version of IE?
    if(browserApp.msie)
    {
        fullVersionName = "Internet Explorer " + version;
    }
    
    // Is this a version of Chrome?
    if (browserApp.chrome) 
    {
        var rchrome = /(chrome)[ \/]([\w.]+)/;
        var match = rchrome.exec(userAgent);
        if (match) 
        {
            version = match[2];
        }
        fullVersionName = "Google Chrome " + version;
        // If it is chrome then jQuery thinks it's safari so we have to tell it it isn't
        browserApp.safari = false;
    }
    
    // Is this a version of Safari?
    if(browserApp.safari)
    {
        fullVersionName = "Safari " + version;
    }
    
    // Is this a version of Mozilla?
    if(browserApp.mozilla)
    {
        var rfirefox = /(firefox)[ \/]([\w.]+)/;
        var match = rfirefox.exec(userAgent);
        //Is it Firefox?
        if(match){
            var version = userAgent.substring(userAgent.indexOf('firefox/') +8);
            version = version.substring(0, version.indexOf('.'));
            fullVersionName = "Firefox " + match[2];
        }
        // If not then it must be another Mozilla
        else{
            fullVersionName = "Mozilla " + version;
        }
    }
    
    // Is this a version of Opera?
    if(browserApp.opera)
    {
        fullVersionName = "Opera " + version;
    }
    
    //TODO: remove
    console.log("Browser version " + fullVersionName);
    return fullVersionName;
}

/**
 * Suggest correct non-temporary path visible in tree 
 * Use in SaveAs dialog as first user prompted path 
 */
function createSaveElementPath(path)
{
    var dc = getDataCollection(getElementPath(path));
    var title = dc.getElementInfo(getElementName(path)).title;
    if(title == undefined)
        title = getElementName(path);
    
    var parentPath = getElementPath(path);
    if(isTemporaryPath(parentPath))
        parentPath = getDefaultProjectDataPath();
    var visibleParentPath = findVisiblePath(parentPath);
    if(visibleParentPath != null)
        return createPath(visibleParentPath, title);
    else
        return createPath(parentPath, title);
}

/**
 * Return path visible in current tree configuration and null if path was not found
 * Find first visible parent and construct path started from visible parent
 * Note: symlinks do not exist for data, only for subitems. For example, PathFinder/Projects -> data/Projects
 */
function findVisiblePath(path)
{
    var c1 = getPathComponents(path);
    var c2 = [];
    var visiblePath;
    while(c1.length > 0)
    {
        var curVisiblePath = undefined;
        var curPath = createPathFromComponents(c1);
        var paths = getBackPaths(curPath);
        for(var i=0; i<paths.length; i++)
        {
            var node = getTreeNode(paths[i]);
            if(node)
            {
                curVisiblePath = paths[i];
                break;
            }
        }
        if(curVisiblePath != undefined)
            return createPathFromComponents(getPathComponents(curVisiblePath).concat(c2));
        
        var c = c1.pop();
        c2.unshift(c);
    }
    return null;
}


/**
 * Check if path is in project's temporary folder like data/Collaboration/MyProject/tmp
 */
function isTemporaryPath(path)
{
    var realPath = getTargetPath(path);
    var components = getPathComponents(path);
    return realPath.startsWith(appInfo.userProjectsPath) && (components[3]=="tmp");
}

/**
 * Parse text properties serialized to string
 * @returns properties object
 */
function parseProperties(propString)
{
    var propLines = propString.split('\n');
    var properties = {}
    for (i = 0; i < propLines.length; i++) 
    {
        var line = propLines[i].split('=');
        if (line.length == 2) 
        {
            properties[line[0]] = line[1].replace("\r", "").replace(/\\(.?)/, "$1");
        }
    }
    return properties;
}
