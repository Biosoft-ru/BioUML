var documentTabs;
var activeDocumentId = null;
var opennedDocuments = {};

var lastDocumentId = 1;
var documentIdMap = {};
var activeDocumentListeners = new Array();
function allocateDocumentId(completeName)
{
    if(documentIdMap[completeName] != undefined)
        return documentIdMap[completeName];
    var id = "document_"+(lastDocumentId++);
    documentIdMap[completeName] = id;
    return id;
}

/**
 * Iterates over given array calling the specified function which will call the callback upon completion
 * @param array - array to iterate over
 * @param func - function taking three parameters: array index, array element and callback which must be called upon its completion. If not called, then iteration will be stopped.
 * @param end (optional) - function which will be called after the last iteration
 * @TODO: replace with _.each if possible
 */
var iterateWithCallback = function(array, func, end)
{
    var keys = _.keys(array);
    var iteration = function(idx)
    {
        if(idx == keys.length)
        {
            if(end) end();
            return;
        }
        func(keys[idx], array[keys[idx]], function() {iteration(idx+1);});
    };
    iteration(0);
}


function removeTab(tabElement)
{
    if(tabElement.index() < 0)
        return;
    var tab = tabElement.remove();
    var panelId = tab.attr( "aria-controls" );
    // Remove the panel
    $( "#" + panelId ).remove();
    el.documentTabs.tabs('refresh');
}

function addTab(tabId, tabName)
{
    documentTabs.children(".ui-tabs-nav" ).append($( "<li><a href='#"+tabId+"'>" + tabName +"</a></li>" ));
    documentTabs.tabs('refresh');
    
    var tabElement = el.documentTabs.find('a[href="#'+tabId+'"]').parent();
    if(tabElement.index() < 0)
    {
        console.log("Tab " + tabName + " was not added");
        return;
    }
    // append close thingy
    tabElement.append($('<em class="ui-tabs-close" title="'+resources.commonTooltipCloseDocument+'">x</em>').click(function()
    {
        if (tabId != null && opennedDocuments[tabId]) 
        {
            closeDocument(tabId);
        }
        else
            removeTab($(this).parent());
    }));
    tabElement.addClass("tab-with-context-menu");
    
    // skip pagination buttons with href='#' to select only tabs with documents 
    var list = el.documentTabs.find( "> li:has(a[href!='#'])" );
    // select just added tab
    el.documentTabs.tabs('option', 'active', tabElement.index(list));

    activeDocumentId = tabId;
    var activeDocument = opennedDocuments[activeDocumentId];
    if(activeDocument && activeDocument.completeName)
    {
        var path = activeDocument.completeName;
        var a = tabElement.children("a");
        var dc = getDataCollection(getElementPath(path));
        if(dc)
        {
            var info = dc.getElementInfo(getElementName(path));
            if(info.title) a.text(info.title);
            tabElement.attr("title", path).attr("data-path", path);
            a.css("padding-left", "20px").css("background-position", "2px 5px").css("background-repeat", "no-repeat")
                .css("background-size", "16px")
                .css("background-image", getNodeIcon(getDataCollection(getElementPath(path)), getElementName(path)));
            fitElement(a, a.text(), true, 150);
            createTreeItemDraggable(tabElement);
        }
    }
    return tabElement;
}

function getContextTabId (opt)
{
    if(opt && opt.$trigger)
    {
        let tabIdStr = opt.$trigger.children('a').attr('href');
        if(tabIdStr != undefined)
            return tabIdStr.substring(1);
    }
    return null;
    
}
/*
 * Init document tabs
 */
function initDocumentTabs()
{
    documentTabs = el.documentTabs.tabs(
    {
        beforeActivate: function(event, ui)
        {
            activeDocumentId = $(ui.newPanel).attr("id");
            updateToolbar();
            updateViewParts();
            updateURL();
            markActiveTreeNode();
            
            var activeDocument = opennedDocuments[activeDocumentId];
            if (activeDocument) 
            {
                if(activeDocument.completeName)
                {
                    var path = activeDocument.completeName;
                    showElementInfo(path);
                    loadElementDescription(path);
                }
                fireActiveDocumentChanged(activeDocument);
            }
        },
        activate: function(event, ui)
        {
            activeDocumentId = $(ui.newPanel).attr('id');
            updateURL();
            if(opennedDocuments[activeDocumentId] && opennedDocuments[activeDocumentId].activate)
                opennedDocuments[activeDocumentId].activate();
            if($(getJQueryIdSelector(activeDocumentId+"_container"))) {
                $(getJQueryIdSelector(activeDocumentId+"_container")).resize();
            }
        }
    });
    documentTabs.tabs('paging', { cycle: true, followOnActive: true, activeOnAdd: true });
    $("#intro_tab").find('span').text(appInfo.startPageTitle);
    
    if(!contextMenuInitialized['document']) 
    {
    //init context menu
    $.contextMenu( {
        selector : '#documentTabs > ul> li.tab-with-context-menu',
        className: 'document-context-menu',
        zIndex: 80,
        items : {
            'show_in_tree': { 
                name: resources.menuShowInTree,
                icon: 'show',
                visible: function(itemKey, opt) {
                    let tabId = getContextTabId(opt);
                    return (tabId != null && opennedDocuments[tabId] && opennedDocuments[tabId].completeName) 
                },
                callback: function(itemKey, opt, originalEvent)
                {
                    let tabId = getContextTabId(opt);
                    if (tabId != null && opennedDocuments[tabId] && opennedDocuments[tabId].completeName) {
                        openBranch(opennedDocuments[tabId].completeName, true);
                    }
                }
            },
            'close_tab': {
                name : resources.menuCloseTab,
                icon: 'close',
                callback: function(itemKey, opt, originalEvent)
                {
                    let tabId = getContextTabId(opt);
                    if (tabId != null && opennedDocuments[tabId]) 
                        closeDocument(tabId);
                    else
                        removeTab(el.documentTabs.find('a[href="#'+tabId+'"]').parent());
                },
            },
            'close_other_tabs': {
                name : resources.menuCloseOtherTabs,
                icon: 'close',
                callback: function(itemKey, opt, originalEvent)
                {
                    let tabId = getContextTabId(opt);
                    iterateWithCallback(opennedDocuments, function(id2, doc, callback)
                    {
                        if(doc && tabId != id2)
                            closeDocument(id2, callback);
                        else
                            callback();
                    }, function()
                    {
                    if(opt.$trigger)
                        opt.$trigger.siblings('li').each(function()
                        {
                            if($(this).children('.ui-tabs-close').length == 0) return;
                            var id2 = $(this).children('a').attr("href").substring(1);
                            if(id2 != tabId)
                                removeTab(el.documentTabs.find('a[href="#'+id2+'"]').parent());
                            });
                        });
                },
            },
            'close_all_tabs': {
                name : resources.menuCloseAllTabs,
                icon: 'close',
                callback: function()
                {
                    closeAllTabs();
                },
            },
        }
    });
        contextMenuInitialized['document'] = true;
    }
}

/*
 * Resize documents tabs
 */
function resizeDocumentsTabs()
{
    if (documentTabs) 
    {
        var tabs = documentTabs.children("ul");
        documentTabs.tabs("refreshOnly");
        var baseHeight = documentTabs.height() - tabs.outerHeight() - 3;
        documentTabs.find('.documentTab, .documentTabWithToolbar')
            .filter(function() {
                return !$(this).parents().is('.documentTab, .documentTabWithToolbar');
            })
            .css('width', documentTabs.width())
            .css('height', function() {
                var toolbar = $(this).siblings(".fg-toolbar");
                return toolbar.length==0?baseHeight:baseHeight-toolbar.outerHeight()-2;})
            .each(function() {$(this).triggerHandler("resize");});
    }
}

/**
 * Open document by given complete repository path This will try to activate
 * first action associated with given document Usually it's not necessary to
 * have the same tree branch opened also
 * 
 * @param {String}
 *            path
 */
function openDocument(path, noOpenBranch)
{
    var hash = paramHash;
    if(path.substring(0, 7) == "images/")
    {
         showImage(path.substring(7));
         return true;
    }
    var action = _.find(treeActions, function(action) {return action.isVisible(action.useOriginalPath?path:getTargetPath(path)) === true;});
    if(action && action.id.match(/^(open|run)/))
    {
        paramHash = hash;
        action.doAction(action.useOriginalPath?path:getTargetPath(path));
        return true;
    }
    else
    {
         if(!noOpenBranch)
             openBranch(path, false);
         showElementInfo(path);
         return false;
    }
}

/**
 * Close document if it was opened, then open it again
 * @param {String} path
 */
function reopenDocument(path)
{
	closeDocumentByPath(path);
    openDocument(path);
}

function closeDocumentByPath(path)
{
  for(var id in opennedDocuments)
  {
    if(opennedDocuments[id].completeName == path || getElementPath(opennedDocuments[id].completeName) == path)
      closeDocument(id);
  }
}

/**
 * Returns current opened document, for composite diagram returns current
 * selected diagram
 */
function getActiveDocument()
{
    var doc = null;
    if (activeDocumentId != null) 
    {
        doc = opennedDocuments[activeDocumentId];
        if( doc instanceof CompositeDiagram )
        {
            doc = doc.getDiagram();
        }
    }
    return doc;
}

function saveDocument(documentId, callback)
{
    var saved = false;
    if(!documentId) documentId = activeDocumentId; 
    if(documentId)
    {
        var activeDocument = opennedDocuments[documentId];
        //do not save documents to temporary folder, they will be lost
        if(isTemporaryPath(activeDocument.completeName))
        {
            saveDocumentAs(documentId, callback);
            return;
        }
        
        if(activeDocument.save)
        {
            activeDocument.save(function(data)
            {
                if(data && (data.type == 0 || data.type == 'ok'))
                {
                    if(callback) callback();
                    else logger.message(resources.commonSaved);
                } else
                {
                    createConfirmDialog(
                            data.message?resources.commonNotSavedMessage.replace("{message}", data.message):resources.commonNotSaved, function()
                    {
                        saveDocumentAs(documentId, callback);
                    });
                }
            });
            saved = true;
        }
    }
    if(!saved)
    {
        logger.error(resources.commonErrorNoSave);
    }
}

function saveDocumentAs(documentId, callback)
{
    if(!documentId) documentId = activeDocumentId; 
    if(documentId)
    {
        var activeDocument = opennedDocuments[documentId];
        if(activeDocument.saveAs)
        {
            var path = createSaveElementPath(activeDocument.completeName);
            createSaveElementDialog(resources.dlgSaveAsTitle, 
                getElementClass(activeDocument.completeName),
                path,
                function(completePath)
                {
                    activeDocument.saveAs(completePath, function(data)
                    {
                        if(data && (data.type == 0 || data.type == 'ok'))
                        {
                            refreshTreeBranch(getElementPath(completePath));
                            if(callback) callback(completePath);
                            else logger.message(resources.commonSaved);
                        } else
                        {
                            logger.message(resources.commonErrorNotSavedAs.replace("{path}", completePath).replace("{message}", data.message));
                        }
                    });
                });
        }
        else
        {
            logger.error(resources.commonErrorNoSaveACopy);
        }
    }
    else
    {
        logger.error(resources.commonErrorNoSaveACopy);
    }
}

function revertDocument(documentObj)
{
    if(!documentObj) return;
    var path = documentObj.completeName;
    if(path == null) return;
    createConfirmDialog(resources.commonConfirmDocumentRevert, function()
    {
        queryBioUML("web/revert", {de: path}, function()
        {
            if(documentObj.setChanged)
                documentObj.setChanged(false);
            closeDocument(documentObj.tabId);
            if(documentObj instanceof DiagramSupport)
            {
                openDiagram(path);
            } else
                openDocument(path);
        });
    });
}

function isActiveDocument(document)
{
    if(document.tabId === activeDocumentId) return true;
    if(!document.parentDocument) return false;
    return isActiveDocument(document.parentDocument);
}

/**
 * The same as openDocument, but also loads element info 
 */
function openDocumentExt(path)
{
    showElementInfo(path);
    openDocument(path);
}

function openDocumentTab(document)
{
    if(document.isValid && !document.isValid())
    {
        delete opennedDocuments[document.tabId];
        return;
    }
    if ($('#documentTabs ' + getJQueryIdSelector(document.tabId)).length > 0) 
    {
        el.documentTabs.tabs('option', 'active', getTabIndex(getJQueryIdSelector(document.tabId)));
    }
    else 
    {
        document.open(el.documentTabs);
        //el.documentTabs.tabs('add', getJQuerySelector(document.tabId), document.name);
        addTab(getJQuerySelector(document.tabId), document.name);
    }
}

function getTabIndex(tabIdSelector)
{
    // find index w/o prev,next buttons considered
    var tabElement = el.documentTabs.find('a[href="'+tabIdSelector+'"]').parent();
    var list = tabElement.parent().find( "> li:has(a[href!='#'])" );
    return list.index(tabElement);
    //return el.documentTabs.find('a[href="'+tabIdSelector+'"]').parent().index();
}

/*
 * Open diagram tab and prepare diagram link at the name
 */
function openDiagramTab(diagram)
{
    var tabElement = addTab(getJQuerySelector(diagram.tabId), diagram.name);
}

function openDiagram(name, callback)
{
    getDataCollection(name).getDiagramTypeInfo(function(typeInfo)
    {
        var diagramOpenHandler = function(diagram)
        {
            if ($('#documentTabs ' + getJQueryIdSelector(diagram.tabId)).length > 0) 
            {
                el.documentTabs.tabs('option', 'active', getTabIndex(getJQueryIdSelector(diagram.tabId)));
                if(callback)
            	{
                	var doc = getActiveDocument();
                	if(doc instanceof Diagram)
                		callback(doc);
                }
            }
            else 
            {
                diagram.open(el.documentTabs);
                openDiagramTab(diagram);
                resizeDocumentsTabs();
                var diagramType = typeInfo.type;
                if(diagramType.match(/WorkflowDiagramType$/) || diagramType.match(/SedMlDiagramType$/))
                    selectViewPart('diagram.workflow.main');
                else
                    selectViewPart('diagram.overview');
                if(callback)
                        callback(diagram);
            }
        };
        if (typeInfo.composite) 
        {
            CreateCompositeDiagramDocument ( name, diagramOpenHandler);
        }
        else
        {
            CreateDiagramDocument(name, diagramOpenHandler);
        }
    });
}

function openNonTreeDocument(id, containerId, name, viewpartId)
{
    if ($(getJQueryIdSelector(containerId)).length > 0) 
    {
        documentTabs.tabs('option', 'active', getTabIndex(getJQueryIdSelector(containerId)));
        if (viewpartId != undefined) 
        {
            selectViewPart(viewpartId);
        }
    }
    else 
    {
        var tabs = documentTabs.children("ul");
        var baseHeight = documentTabs.height() - tabs.outerHeight() - 3;
        
        var containerDiv = $('<div id="' + containerId + '" class="documentTab"></div>').css('padding', 0).css('position', 'relative').css('width', documentTabs.width()).css('height', baseHeight);
        
        var mainDiv = $('<div id="' + id + '"></div>');
        containerDiv.append(mainDiv);
        documentTabs.append(containerDiv);
        addTab(containerId, name);
        if (viewpartId != undefined) 
        {
            selectViewPart(viewpartId);
        }
    }
}

function closeDocument(id, callback)
{
    var doc = opennedDocuments[id];
    if(!doc) return;
    var closeHandler = function()
    {
        doc.close();
        delete opennedDocuments[id];
        removeTab(el.documentTabs.find('a[href="#'+id+'"]').parent());
        //documentTabs.tabs('remove', "#"+id);
        //if(documentTabs.tabs('length') == 0)
        if(el.documentTabs.find('.ui-tabs-nav li').length == 0)
            updateViewParts();
        if(callback) callback();
    };
    //do not call confirm dialog if no completeName specified, special documents should process saving name manually
    if(!doc.completeName) 
    {
        if(doc.isChanged())
            saveDocument(id, closeHandler);
        else
            closeHandler();
    }
    else if(isTemporaryPath(doc.completeName))
    {
        createYesNoConfirmDialog(resources.commonConfirmCloseTemporary.replace("{path}", getElementName(doc.completeName)), function(yes)
        {
            if(yes)
            {
                saveDocument(id, closeHandler);
            } 
            else
            {
                closeHandler();
            }
        });
    }
    else if(doc.isChanged())
    {
        openDocumentTab(doc);
        createYesNoConfirmDialog(resources.commonConfirmSaveOnClose.replace("{path}", getElementName(doc.completeName)), function(yes)
        {
            if(yes)
            {
                saveDocument(id, closeHandler);
            } else
            {
                queryBioUML("web/revert", {de: doc.completeName}, function() {}, function() {});
                closeHandler();
            }
        });
    }
    else 
    {
        queryBioUML("web/revert", {de: doc.completeName}, function() {}, function() {});
        closeHandler();
    }
}

function getNotSavedDocumentNames()
{
    var result = [];
    for(var id in opennedDocuments)
    {
        if(opennedDocuments[id] && opennedDocuments[id].isChanged())
            result.push(opennedDocuments[id].name);
    }
    return result;
}


/**
 * Will try to close all opened document tabs asking to save if necessary
 * @param closeAllCallback callback to be called after successful operation (if operation fails for some reason, it's not called)
 */
function closeAllTabs(closeAllCallback)
{
    iterateWithCallback(opennedDocuments, function(id2, doc, callback)
    {
        if(doc)
            closeDocument(id2, callback);
        else
            callback();
    }, function()
    {
        el.documentTabs.children("ul").children("li").each(function()
        {
            if($(this).children('.ui-tabs-close').length == 0) return;
//            var id2 = $(this).children('a').attr("href").substring(1);
//            documentTabs.tabs('remove', "#"+id2);
            var tab = $( this ).remove();
            var panelId = tab.attr( "aria-controls" );
            $( "#" + panelId ).remove();
        });
        el.documentTabs.tabs('refresh');
        if(closeAllCallback) closeAllCallback();
    });
}

/**
 * Return id of opened document with path specified and null if no document found
 */
function findOpenedDocument(path)
{
    for(var id in opennedDocuments)
    {
        if(opennedDocuments[id].completeName == path || getElementPath(opennedDocuments[id].completeName) == path) 
            return id;
    }
    return null;
}

/**
 * Manage active document changing by firing listeners 
 */
function addActiveDocumentListener(listener)
{
    var alreadyAdded = false;
    for (li = 0; li < activeDocumentListeners.length; li++)
    {
        if (activeDocumentListeners[li] == listener)
        {
            alreadyAdded = true;
            break;
        }
    }
    if (!alreadyAdded)
    {
        activeDocumentListeners.push(listener);
    }
};

function fireActiveDocumentChanged (document)
{
    for (li = 0; li < activeDocumentListeners.length; li++)
    {
        activeDocumentListeners[li].activeDocumentChanged(document);
    }
};

function removeActiveDocumentListener (listener)
{
    var newListeners = new Array();
    for (li = 0; li < activeDocumentListeners.length; li++)
    {
        if (activeDocumentListeners[li] != listener)
        {
            newListeners.push(activeDocumentListeners[li]);
        }
    }
    activeDocumentListeners = newListeners;
};
