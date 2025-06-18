/**
 * Set of document view parts and view parts utility functions
 * 
 */
var viewParts = new Array();
var opennedViewPartId = null;
var viewPartsInitialized = {};

function getViewPartId(name)
{
    return name;//name.replace(/\./g, "_");
}

function getViewPartSelector(name)
{
    return getJQueryIdSelector(getViewPartId(name));
}

function createViewPartContainer(name)
{
    return $("<div/>").attr("id", getViewPartId(name)).attr("data-id", name);
}

/*
 * Create view part objects
 */
var pagingInitialized = false;
function initViewParts()
{
    initGeneralViewParts();
    initWDLViewParts();

    //var viewPartTabs = el.viewPartTabs;

    var tabs = el.viewPartTabs.tabs(
    {
        activate: function(event, ui)
        {
            if (opennedViewPartId != null) 
            {
                var vp = lookForViewPart(opennedViewPartId);
                if (vp != null) 
                {
                    vp.save();
                }
            }
            opennedViewPartId = $(ui.newPanel).attr('data-id');
            var vp = lookForViewPart(opennedViewPartId);
            if (vp != null) 
            {
                updateViewPartsToolbar(vp);
                if(vp.show)
                    vp.show(opennedDocuments[activeDocumentId]);
            }
            el.viewPartTabs.triggerHandler("resize");
        }
    });
    this.viewPartToolbar = $('<div class="ui-corner-all ui-helper-clearfix tabs-shifter" style="min-width: 115px; left: 2px; top: 2px; position:absolute; z-index:1;"></div>');
    tabs.find('.ui-tabs-nav').append(this.viewPartToolbar);
    
    initSettings(tabs);
    
    updateViewParts();
    
    el.viewPartTabs.bind("resize", function(event) {
        if(el.viewPartTabs.has(event.target).length) 
            return;
        if(pagingInitialized)
            el.viewPartTabs.tabs('refreshOnly');
        el.viewPartTabs.children('div')
            .width(el.viewPartTabs.width()-30)
            .height(el.viewPartTabs.height()-el.viewPartTabs.children('ul').outerHeight()-23);
    });
}

function initViewpartsPaging()
{
    if(pagingInitialized)
        return;
    if($(el.viewPartTabs).find( ".ui-tabs-nav li" ).length > 0){
        el.viewPartTabs.tabs('paging', { cycle: true, followOnActive: true, activeOnAdd: false });
        pagingInitialized = true;
    }
}

function refreshViewParts(vpt)
{
    //viewPartTabs.tabs('resizeInit');
    if(pagingInitialized)
        vpt.tabs('refreshOnly');
    
    vpt.children('div')
        .width(vpt.width()-30)
        .height(vpt.height()-vpt.children('ul').outerHeight()-23);
}

function initSettings(tabs)
{
    var viewPartSettings = $('<div class="ui-corner-all ui-helper-clearfix tabs-shifter" style="right: 0px; top: 2px; position:absolute; z-index:2;"></div>');
    tabs.find('.ui-tabs-nav').append(viewPartSettings);
    var block = $('<div class="fg-buttonset ui-helper-clearfix" style="margin: 3px 5px 0 0;"></div>');
    viewPartSettings.append(block);
    var settingsAction = createToolbarButton(resources.vpSettings, "settings.png", editViewpartSettings);
    block.append(settingsAction);
}

function editViewpartSettings()
{
    var dialogDiv = $('<div title="'+resources.vpSettingsDialogTitle+'"></div>');
    
    var table = $('<table class="display"><tr><th>'+resources.vpSettingsTitle+'</th><th>'+resources.vpSettingsChecked+'</th></tr></table>').css("width","300px").css("margin", "0 auto");
    var vpHidePref = getPreference("viewpartsHidden");
    var vpHide = [];
    if(vpHidePref != null)
        vpHide = vpHidePref.split(";");
    var i = 1;
    for (var vpi = 0; vpi < viewParts.length; vpi++) 
    {
        var viewPart = viewParts[vpi];
        var tr = $('<tr><td></td><td></td></tr>').addClass(i++%2?"odd":"even");
        tr.children().eq(0).text(viewPart.tabName);
        var ch = $('<input type="checkbox" class="vpHide"/>').attr("vp-id", viewPart.tabId);
        if(!vpHide.includes(viewPart.tabId))
            ch.attr("checked", "checked");
        tr.children().eq(1).append(ch);
        table.append(tr);
    }
    
    dialogDiv.append(table);
    
    var dialogButtons = {};
    dialogButtons[ resources.dlgButtonSave ] = function()
            {
                var _thisDialog = $(this);
                //Some viewparts are inititalized only when specific elements like Diagram, Table are opened. 
                //Keep old hidden viewparts ids even if they are not in the list now.
                var currentlyAvailable = {};
                var toHide = [];
                _thisDialog.find(".vpHide:checkbox:not(:checked)").each(function() {
                    toHide.push($(this).attr("vp-id"));
                });
                
                _thisDialog.find(".vpHide:checkbox").each(function() {
                    currentlyAvailable[$(this).attr("vp-id")] = 1;
                });
                for(var i=0; i < vpHide.length; i++)
                {
                    if(currentlyAvailable[vpHide[i]] == undefined)
                        toHide.push(vpHide[i]);
                }
                setPreference("viewpartsHidden", toHide.join(";"), updateViewParts)
                $(this).dialog("close");
                $(this).remove();
            };
    dialogButtons[ resources.dlgButtonCancel ] = function()
            {
                $(this).dialog("close");
                $(this).remove();
            };
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 350,
        buttons: dialogButtons
    });
    dialogDiv.dialog("open");  
};


/**
 * Called by viewPart constructor to perform common viewPart initialization steps
 * @param viewPart - viewPart object itself
 * @param id - viewPart id
 * @param name - viewPart title displayed to user
 */
function createViewPart(viewPart, id, name)
{
    viewPart.tabId = id;
    viewPart.tabName = name;
    viewPart.tabDiv = createViewPartContainer(id);
    viewPart.containerDiv = $('<div class="viewPartTab"/>').attr("id", id + '_container');
    viewPart.tabDiv.append(viewPart.containerDiv);
}

function getActiveViewPart()
{
    return lookForViewPart(opennedViewPartId);
}

function isRulePassed(viewPartId, rule)
{
    if(!rule.pattern)
    {
        var pattern = "";
        for(var i=0; i<rule.template.length; i++)
        {
            var char = rule.template.charAt(i);
            if( char == '*' )
                pattern+=".*";
            else if( char == '?' )
                pattern+=".";
            else if( "+()^$.{}[]|\\".indexOf(char) != -1 )
                pattern+='\\'+char; // prefix all metacharacters with backslash
            else
                pattern+=char;
        }
        rule.pattern = new RegExp(pattern, "i");
    }
    return viewPartId.match(rule.pattern);
}

function isViewPartAvailable(viewPartId)
{
    var result = true;
    for(var i in perspective.viewparts)
    {
        var rule = perspective.viewparts[i];
        if(isRulePassed(viewPartId, rule)) result = rule.type == "allow";
    }
    return result;
}

function updateViewPartsToolbar(viewPart)
{
    this.viewPartToolbar.empty();
    var block = $('<div class="fg-buttonset ui-helper-clearfix" style="margin: 3px 0 0 2px;"></div>');
    
    if (viewPart && viewPart.initActions) 
    {
        viewPart.initActions(block);
    }
    
    this.viewPartToolbar.append(block);
    var leftToolbarShift = Math.max(block.children().length*22 + 5, 117)  ;
    $('#viewPartTabs > ul').css("padding-left", leftToolbarShift + "px");
}

/*
 * Show view parts for opened object. Use "null" for global view parts.
 */
function updateViewParts(skipSelection)
{
    var doc = getActiveDocument();
    var viewPartToSelect = null;
    if(activeDocumentId == 'sql_result')
        viewPartToSelect = "sql.editor";
    else
        viewPartToSelect = opennedViewPartId;
    if(viewPartToSelect == null && appInfo.defaultViewPart)
        viewPartToSelect = appInfo.defaultViewPart;
    var viewPartTabs = el.viewPartTabs;
    var currentTabs = [];
    viewPartTabs.children('div').each(function()
    {
        currentTabs[$(this).attr('id')] = $(this);
    });
    
    viewPartTabs.children('ul').children('li').remove();
    
    var vpHidePreference = getPreference("viewpartsHidden");
    var vpHidden = [];
    if(vpHidePreference != null)
        vpHidden = vpHidePreference.split(";");
    var vpNavBar = viewPartTabs.children(".ui-tabs-nav" );
    var vpVisible = [];
    for (var vpi = 0; vpi < viewParts.length; vpi++) 
    {
        var viewPart = viewParts[vpi];
        if(!isViewPartAvailable(viewPart.tabId)) continue;
        if(vpHidden.includes(viewPart.tabId)) continue;
        
        (function(viewPart) // using closure to isolate viewPart variable
        {
            viewPart.isVisible(doc, function(visible)
            {
                if (visible) 
                {
                    vpVisible.push(viewPart.tabId);
                    if(currentTabs[viewPart.tabId])
                    {
                        currentTabs[viewPart.tabId] = null;
                    }
                    else
                    {
                        viewPartTabs.append(viewPart.tabDiv);
                    }
                    $( "<li><a href='#"+viewPart.tabId+"'>"+viewPart.tabName+"</a></li>" ).appendTo(vpNavBar);
                    viewPart.explore(doc);
                }
            });
        })(viewPart);
    }
    viewPartTabs.tabs( "refresh" );
    for(var i in currentTabs)
    {
        if(currentTabs[i]!=null)
        {
            viewPartTabs.children(getViewPartSelector(i)).remove();
            
        }
    }
    //No viewparts visible for current document (all hidden or disabled)
    if(vpVisible.length == 0)
    {
        opennedViewPartId = null;
        //console.log("No viewparts");
        viewPartToSelect = null;
        updateViewPartsToolbar(null);                                   
    }
    else
    {   
        if (!skipSelection && viewPartToSelect != null)
        {
            selectViewPart(viewPartToSelect);
            viewPartToSelect = null;
        }
        else
            el.viewPartTabs.tabs("option", "active", 0);
        //viewPartTabs.trigger("resize");
        refreshViewParts(viewPartTabs);
        initViewpartsPaging();
    }
}

/*
 * Save changes in all view parts
 */
function saveAllViewParts()
{
    var doc = getActiveDocument();
    if (doc != null) 
    {
        for (i = 0; i < viewParts.length; i++) 
        {
            var viewPart = viewParts[i];
            viewPart.isVisible(doc, function(visible)
            {
                if (visible) 
                {
                    viewPart.save();
                }
            });
        }
    }
}

/*
 * Search view part by ID
 */
function lookForViewPart(id)
{
    for (i = 0; i < viewParts.length; i++) 
    {
        var viewPart = viewParts[i];
        if (viewPart.tabId == id) 
        {
            return viewPart;
        }
    }
    return null;
}

/*
 * Select tab by ID
 */
function selectViewPart(id)
{
    viewPartToSelect = id;
    var index = $('#viewPartTabs a[href="#'+id+'"]').parent().index();
    if(index < 0)
        return;
    var anchors = $('#viewPartTabs > ul > li > a');
    var offset = 2; //offset for toolbar and settings
//    if(anchors.length > 0 && anchors.get(0).getAttribute("href") == "#")
//        offset += 1;
    el.viewPartTabs.tabs("option", "active", index-offset);
}
