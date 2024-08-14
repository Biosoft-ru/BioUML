/**
 * Code necessary to startup the BioUML application and maintain basic UI framework 
 */

(function startFramework() {
    
})();
/*
 * Onload actions(executes when DOM loaded but not multimedia yet)
 */
$(document).ready(function()
{
    if(lockInit)
        return;
        
    defaultTitle = appInfo.name+" "+appInfo.version+" "+appInfo.edition;
    document.title = defaultTitle;
    window.onhashchange = function()
    {
        if (currentHashStr != document.location.hash) 
        {
            currentHashStr = document.location.hash;
            if(!currentUserPreferences) document.location.reload();
            else processURL(parseURL(currentHashStr));
        }
    };
    var ids = [ "mainPane", "rightBottomPane", "viewPartTabs",
            "infoTabs",
            "leftBottomPane", "bottomPane", "documentTabs",
            "rightTopPane", "repositoryTabs", "leftTopPane", "topPane",
            "mainToolbar" ];
    for(var i=0; i<ids.length; i++)
    {
        el[ids[i]] = $(document.getElementById(ids[i]));
    }
    // init splitter
    initSplitPanes();
    
    defaultHash = _.clone(paramHash);
    _.extend(paramHash, parseURL(document.location.hash));
    
    var user = new User();
    if( paramHash.user )
    {
        user.login( paramHash.user, paramHash.pass, true );
    } 
    else if( paramHash.sessionId )
    {
        user.login( "", "", true );
    } 
    else if( paramHash.anonymous && (!appInfo.disableAnonymous || appInfo.enableAnonymousHash) )
    {
        user.login("", "", false);
    } 
    else
    {
        if(appInfo.loginURL)
            window.location = appInfo.loginURL;
        else
            user.loginWithCookie(appInfo.loginMessage);
    }

    BioUML.disablePerspectiveSelector = paramHash['disablePerspectiveSelector'] !== undefined;
    BioUML.disableProjectSelector = paramHash['disableProjectSelector'] !== undefined;

    $(window).on("blur focus", updateOnRefocus);
});


var contextMenuInitialized = {};
/*
 * Main function for load application. Called after login.
 */
function loadApplication(startupInfo)
{
    showWaitDialog(resources.commonLoading);
    // init perspective
    setPerspective(startupInfo.perspective);

    // init user preferences
    currentUserPreferences = convertJSONToDPS(startupInfo.preferences);
    
    //init possibility to use experimental products
    if(startupInfo.experimental)
        canUseExperimental = true;
    
    // load common classes
    instanceOf.parseClasses($.evalJSON(startupInfo.classes));
    
    initScripting(startupInfo.scriptTypes);
    
    // init roots
    for(var root in startupInfo.roots)
    {
        getDataCollection(root).fillElementInfoByResult(startupInfo.roots[root]);
    }
    
    initUIMode(getPreference('fullUI'));
    initRepositoryVisibility();
    
    // init toolbar
    createJournalBox(startupInfo.journals);
    initToolbar(startupInfo.actions);
    initTreeActions(startupInfo.actions.tree);
    // init actions for context panel
    initContextToolbar(startupInfo.actions.tree);
    
    CodeMirror.globalContext = startupInfo.context; 

    var startupHashStr = document.location.hash;
    
    // init ajax loader indicator
    initAjaxLoader();
    
    // init repository tabs
    initRepositoryTabs();
    
    //init perspective virtual folders, call server to get target paths for items
    initVirtualCollections();
    
    //init analysis search panel
    initAnalysisSearchToolbar();
    // init document tabs
    initDocumentTabs();
    
    //init intro pages
    initIntro();

    // init view part tabs
    initViewParts();
    // set resize event
    $(window).resize(event,
        function(event2){
            if(event2.target != window)
                return;
            resizeWindow();
    });
    // apply initial size
    resizeWindow();
    
    initCurrentProject();

    initTableSelector();
    // check #anchor URL parameters and process them if necessary
    currentHashStr = startupHashStr;
    _.extend(defaultHash, parseURL(startupHashStr));
    delete defaultHash.user;
    delete defaultHash.pass;
    delete defaultHash.perspective;
    if (_.isEmpty(defaultHash)) 
    {
        openDefaultPath( function(path) { openBranchOrDefault(path, false); });
    }
    else
    {
        processURL(defaultHash);
    }
    
    //init chat session if necessary
    if(rootMap['users'])
    {
        startChatSession();
    }
    
    window.onbeforeunload = function (evt) {
        if(invalidSession) return;
        var unsaved = getNotSavedDocumentNames();
        if(unsaved.length == 0) return;
        var message = "The following documents are unsaved:\n"+unsaved.join("\n")+"\n\nIf you leave now, these changes will be discarded. Proceed?";
        if (typeof evt == "undefined") {
            evt = window.event;
        }
        if (evt) {
            evt.returnValue = message;
        }
        return message;
    };  

    ping();
    ServerMessages.start();
}

function initScripting(allowedScriptTypes)
{
    scriptTypes = {
        js: {
            title: "JavaScript",
            mode: "javascript",
            allowed: false
        },
        R: {
            title: "R script",
            mode: "r",
            allowed: false
        },
        math: {
            title: "Math",
            mode: "txt",
            allowed: false
        },
         antimony: {
            title: "Antimony",
            mode: "antimony",
            allowed: false
        }
    }
    for(var key in allowedScriptTypes) {
        if(scriptTypes[key] !== undefined) {
            scriptTypes[key].allowed = true;
        }
    }
    CodeMirror.commands.javascript_autocomplete = function(cm) {
        CodeMirror.showHint(cm, CodeMirror.hint.javascript);
    };
    CodeMirror.commands.r_autocomplete = function(cm) {};
    
}

function initAjaxLoader()
{
    $(".ajaxLoaderHidden").ajaxStart(function() 
    {
        $(".ajaxLoaderHidden").removeClass("ajaxLoaderHidden").addClass("ajaxLoader");
    }).ajaxStop(function() 
    {
        $(".ajaxLoader").removeClass("ajaxLoader").addClass("ajaxLoaderHidden");
    });
}

function toggleUI(desiredMode)
{
    var newMode;
    if(desiredMode !== undefined)
    {
        newMode = desiredMode ? 'true' : 'false';
        if(getPreference('fullUI') === newMode)
            return;
    }
    else
        newMode = getPreference('fullUI') === 'true' ? 'false' : 'true';
    setPreference('fullUI', newMode);
    initUIMode(newMode);
}

function initUIMode(fullUI) 
{
    if(!fullUI && appInfo.fullUI != undefined)
    {
        fullUI = appInfo.fullUI.toString();
        setPreference('fullUI', fullUI);
    }
    if(!fullUI || fullUI === 'false')
    {
        BioUML.fullUI = false;
        el.topPane.height("100%");
        el.bottomPane.height(0).hide();
        el.mainPane.children('.hsplitter').hide();
        el.mainPane.trigger('resize');
        resizeWindow();
        el.mainSplitter.position("100%");
    } else
    {
        BioUML.fullUI = true;
        el.topPane.height("70%");
        el.bottomPane.height("30%").show();
        el.mainPane.children('.hsplitter').show();
        el.mainPane.trigger('resize');
        resizeWindow();
        el.mainSplitter.position("70%");
    }
}

function toggleRepositoryVisibility(desiredMode)
{
    var newMode;
    if(desiredMode !== undefined)
    {
        newMode = desiredMode ? 'true' : 'false';
        if(getPreference('showRepository') === newMode)
            return;
    }
    else
        newMode = getPreference('showRepository') === null || getPreference('showRepository') === 'true' ? 'false' : 'true';
    
    setPreference('showRepository', newMode);
    if(newMode === 'true')
        showRepository();
    else
        hideRepository();
}
function initRepositoryVisibility()
{
    var show = getPreference('showRepository') === null || getPreference('showRepository') === 'true';
    if( show )
      showRepository();
    else
      hideRepository();
}

function showRepository()
{
    el.rightTopPane.width("80%");
    el.leftTopPane.width('20%').show();
    el.topPane.children('.vsplitter').show();
    el.mainPane.trigger('resize');
    resizeWindow();
    el.leftTopSplitter.position("20%");
    
    if(el.repositoryTabs.data("ui-tabs") && !el.repositoryTabs.tabs('option', 'active'))
    {
        var doc = getActiveDocument();
        if(doc)
        {
            doc.visibleActions = null;
            openBranch(doc.completeName, true);
        }
        else
        {
          el.repositoryTabs.tabs('option', 'active', 0);
        }
    }
}
function hideRepository()
{
    el.leftTopPane.hide();
    el.rightTopPane.width('100%');
    el.topPane.children('.vsplitter').hide();
    el.mainPane.trigger('resize');
    resizeWindow();
    el.leftTopSplitter.position(0);
}

/*
 * Initialize intro page
 */
function initIntro()
{
    //return;
    var name = "default";
    var introPage = "intro";
    if(perspective)
    {
      if(perspective.name)
        name = perspective.name;
      if(perspective.intro)
        introPage = perspective.intro;
    }
    var parentSelector = "#intro>[title='" + name + "']";
    $("#intro").children().hide();
    if($(parentSelector).length == 0)
    {
      $("<div></div>").appendTo("#intro")
        .attr("title", name)
        .attr("class", "documentTab")
        .css({
          "position":"relative",
          "height":"100%",
          "width":"100%",
          "border":"none"
        });
      $(parentSelector).load(introPage+".html?"+appInfo.build, function(response, status, xhr)
      {
          initWorkflowsPage();
          if (status != "error")
          {
              resizeDocumentsTabs();
              introPageOpenned(name);
          }
      });
    }
    else
    {
      $(parentSelector).show();
      introPageOpenned(name);
    }
}

var introPageOpenCallbacks = {};
function onIntroPageOpen(perspectiveName, callback) {
  if(!introPageOpenCallbacks[perspectiveName])
    introPageOpenCallbacks[perspectiveName]=[];
  introPageOpenCallbacks[perspectiveName].push(callback);
  //Execute callback right now if currently selected perspective == perspectiveName
  if(perspective && perspective.name == perspectiveName)
    callback();
}
function introPageOpenned(perspectiveName)
{
  var callbacks = introPageOpenCallbacks[perspectiveName];
  if(callbacks)
  {
    for(var i = 0; i < callbacks.length; i++)
      callbacks[i]();
  }
}

/*
 * Tune workflows page in the intro 
 */
function initWorkflowsPage()
{
    if( currentUser )
    {
        try
        {
            toggleIntroPages();
        }
        catch(e)
        {
            
        }
        if(appInfo.manual)
            $(".workflows_manual_link").attr('href', appInfo.manual);
        else
            $(".workflows_manual_link").hide();    
    } else
    {
        $(".open-workflow").hide();
    }
    
}

/*
 * Initialize splitters
 */
function initSplitPanes()
{
    //JSUPDATE
    //onDrag is called too often, so onDragEnd is used to resize after splitter is moved
    el.mainSplitter = el.mainPane.split(
    {
        orientation: 'horizontal',
        limit: 10,
        position: "80%",
        onDragEnd: function(event) {
            el.rightTopPane.trigger('resize');
            el.leftTopPane.trigger('resize');
            el.leftBottomPane.trigger('resize');
            el.rightBottomPane.trigger('resize');
        }
    });
    
    el.leftTopSplitter = el.topPane.split(
    {
        orientation: "vertical",
        limit: 0,
        position: "20%",
        onDragEnd: function(event) {
            el.rightTopPane.trigger('resize');
            el.leftTopPane.trigger('resize');
        }
    });
    
    el.bottomPane.split(
    {
        orientation: "vertical",
        limit: 10,
        position: "20%",
        onDragEnd: function(event) {
            el.leftBottomPane.trigger('resize');
            el.rightBottomPane.trigger('resize');
        }
    });
    
    el.leftTopPane.resize(function(event)
    {
        var pane = el.leftTopPane;
        if(pane.has(event.target).length) return;
        var tabs = el.repositoryTabs;
        if (tabs.data("ui-tabs")) {
            tabs.tabs('refresh');
        } 
        var decrease = pane.children("#analysisSearchToolbar").is(":hidden") ? 0 : pane.children("#analysisSearchToolbar").outerHeight();
        var contextDecrease = pane.children("#contextToolbar").is(':visible') ? pane.children("#contextToolbar").outerHeight() + 6 : 2;
        tabs.height(pane.height() - decrease - contextDecrease );
        tabs.children(".tree_container").height(tabs.height() - tabs.children("ul").outerHeight());
    });
    
    el.rightTopPane.resize(function(event)
    {
        if(el.rightTopPane.has(event.target).length) return;
        resizeDocumentsTabs();
    });

    el.rightBottomPane.resize(function(event)
    {
        if(el.rightBottomPane.has(event.target).length) return;
        el.viewPartTabs.triggerHandler("resize");
    });
    
    el.leftBottomPane.resize(function(event)
    {
        if(el.leftBottomPane.has(event.target).length) return;
        el.infoTabs.triggerHandler("resize");
    });
    
    el.mainPane.resize(function(event)
    {
        if(event.target === el.mainPane[0])
        {
            //if(!BioUML.fullUI)
                //el.topPane.height("100%");
            el.rightTopPane.trigger('resize');
            el.leftTopPane.trigger('resize');
            el.leftBottomPane.trigger('resize');
            el.rightBottomPane.trigger('resize');
        }
        
    });
    
    $(".hsplitter,.vsplitter").mousedown(function(e) {
        e.preventDefault();
    });
}

/*
 * Resize function
 */
function resizeWindow()
{
    var windowSize = getWindowSize();
    var width = windowSize[0] - 8;
    var dHeight = el.mainToolbar.outerHeight() + 8;
    var height = windowSize[1] - (navigator.userAgent.match(/msie/i)?dHeight+2:dHeight);
    var updated = false;
    if(parseInt(el.mainPane.get(0).style.width) !== parseInt(width))
    {
        el.mainPane.css("width", width);//.trigger("resize");
        updated = true;
    }
    if(parseInt(el.mainPane.get(0).style.height) !== parseInt(height))
    {
        el.mainPane.css("height", height)//.trigger("resize");
        updated = true;
    }
    if(updated)
    {
        el.mainSplitter.refresh();
        if(BioUML.fullUI)
            el.mainSplitter.position("70%");
        else
            el.mainSplitter.position("100%");
        el.mainPane.trigger("resize");
        
        if (el.viewPartTabs.data("ui-tabs") && pagingInitialized) {
            el.viewPartTabs.tabs('refreshOnly');
        }
        
    }
}

/**
 *  Following code have been taken from
 *  https://stackoverflow.com/questions/1760250/how-to-tell-if-browser-tab-is-active
 */
function updateOnRefocus(event) {
	var prevType = $(this).data("prevFocusType");
    if (prevType != event.type) {   //  reduce double fire issues
        switch (event.type) {
            case "blur":
                break;
            case "focus":
            	tryResetPerspectiveName();
                break;
        }
    }
    $(this).data("prevFocusType", event.type);
}
