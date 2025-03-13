/* $Id: tree.js,v 1.42 2013/08/16 10:22:50 lan Exp $ */
/*
 * Repository-tree related functions.
 */
var treeMap = {};
var treeActions;
var treeInProcess = false;
var maxItemsToLoad = 150;
var tabChangeListeners = new Array();

//JSUPDATE
//Commented, context problem does not seem to exist 
//$(function() {$(document).bind("mousedown", function(e) {
//	if(!$(e.target).closest(".tree-context").length)
//	{
//		for(var root in treeMap)
//			treeMap[root].hide_context();
//	}
//});});

/*
 * Init repository tabs
 */
function initRepositoryTabs()
{
	el.repositoryTabs.tabs({
	    beforeActivate: function(event, ui)
		{
			var tabId = ui.newPanel.attr("data-id");
            if(!treeMap[tabId]) return;
            if (treeMap[tabId].selected) 
            {
                var path = getTreeNodePath(treeMap[tabId].selected[0]);
                storeLastOpenPath(path);
            }
            activeDC = rootMap[tabId];
            fireTabChanged();
		}
	});
	
	updateRepositoryTabs();
}

function updateRepositoryTabs()
{
	for(var root in treeMap)
	{
		treeMap[root].jstree('destroy');
		delete rootMap[root].treeObj;
	}
	$(el.repositoryTabs).find( ".ui-tabs-nav li" ).each(function(){
	    var tab = $( this ).remove();
	    var panelId = tab.attr( "aria-controls" );
	    // Remove the panel
	    $( "#" + panelId ).remove();
	});
	el.repositoryTabs.tabs( "refresh" );
	
	treeMap = {};
	var treeNavBar = el.repositoryTabs.children(".ui-tabs-nav" );
	//add repository tabs
	//JSUPDATE
	for(var i in perspective.repository)
	{
		var root = perspective.repository[i].path;
		if(!rootMap[root]) 
		    rootMap[root] = new DataCollection(root);
		//TODO: check if "#rt_'+root.escapeHTML()+'" should be used for <li>
		el.repositoryTabs.append('<div id="'+root+'_tree_container" class="tree_container" data-id="'+root+'">' +
		        '<div id="'+root+'_tree" data-id="'+root+'"></div></div>');      
		//add tab
		//TODO: check if getJQueryIdSelector(root+'_tree_container') is required in href
                var repTitle = perspective.repository[i].title;
                repTitle = resources.repositoryNames[ repTitle ] || repTitle; 
		$("<li><a href='#" + root + "_tree_container'>" + repTitle + "</a></li>" ).appendTo( treeNavBar );
		
		//TODO: check if getJQueryIdSelector(root+'_tree') is required
		treeMap[root] = createTreeObject(root, '#'+root+'_tree');

		rootMap[root].treeObj = treeMap[root];
		if( !activeDC )
			activeDC = rootMap[root];
	}
	
	el.repositoryTabs.tabs( "refresh" );
	el.leftTopPane.trigger("resize");
}

function createTreeObject(root, treeContainerId)
{
    var treeInstance = $(treeContainerId).jstree({
        'core' : 
        {
          'check_callback' : function(o, n, p, i, m) {
                          if(o === "move_node" || o === "copy_node") {
                              return false;
                          }
                          return true;
          },
          'data' : function (obj, callback) {
              let _this = this;
              let _root = root;
              getTreeNodes(obj, root, function(newNodes){
                  callback.call(_this, newNodes);
              });
          },
          'multiple' : true,
          'animation' : 0,
          'themes' : {
              'variant' : 'small'
          },
          'dblclick_toggle' : false,
          },
          'types' : {
              'default' : { 'icon' : 'folder' },
          },
          
          'plugins' : ['contextmenu', 'changed'],
          'contextmenu' : {
              'items' : function(node) {
                  return getContextMenu(node);
              }
          }       
    });

    treeInstance.on('changed.jstree', function (e, data) {
        if(data.action && data.action=='delete_node')
            return;
        if(!data.node)
            return;
        var _treeInProcess = treeInProcess;
        if (data.node.id.indexOf("#rt_") == 0) 
        {
            var path = getTreeNodePath(data.node);
            if(activeDC)
                activeDC.selectedElement = path;
            if(!_treeInProcess)
            {
                storeLastOpenPath(path);
                path = getTargetPath(path); 
                if(data && data.selected && data.selected.length) {
                    if(data.selected.length == 1)
                    {
                        showElementInfo(path);
                        loadElementDescription(path);
                    }
                    else
                        updateContextToolbar(null, null, data.selected.map(function(el){return el.substring(4)}));
                }
            }
        }
        else if (data.node.id.indexOf("#et_") == 0) 
        {
            data.instance.set_icon(data.node, "./lib/jstree/themes/default/throbber.gif");
            //Timeout used for "Load next" nodes  with id like #et_ since dc.getElementInfoRange blocks interface for some time
            setTimeout(function()
            {
                var path = data.node.id.substring(4);
                var dc = getTargetCollection(path);
                var parentId = data.instance.get_parent(data.node);
                var parentNode = data.instance.get_node(parentId);
                //TODO: not a good way to take totalSize
                var totalSize = data.node.original.totalSize;
                var size = parentNode.children.length - 1;
                
                //TODO: prev node before "Load next" was selected in older version, is it fine?
                dc.getElementInfoRange(size, size + maxItemsToLoad, function(nameList){
                    data.instance.delete_node(data.node);
                    new AddNameListCallback(data.instance, parentNode, dc, totalSize).func(nameList);
                });
            }, 100);
        }
        
    }).on('close_node.jstree', function (e, data) {
        if(data.instance.creatingItem) return;
        data.node.state.loaded = false;
        var path = getTreeNodePath(data.node);
        getTargetCollection(path).invalidateCollection();
    }).on('dblclick.jstree', function(e){
        //jstree does not support doubleclick propagation to nodes, so here it's just jquery event without tree node passed
        if(e.target && e.target.id)
        {
            let nodeId = $(e.target).closest('.jstree-node')[0].id;
            if(nodeId.indexOf('#rt_') == 0)
            {
                var path = nodeId.substring(4);
                var treeObject = getTreeObject(path);
                var opened = openDocument(path, true);
                if(!opened)
                    treeObject.jstree('toggle_node', nodeId);
            }
        }
    }).on('load_node.jstree', function(e, data){
        //use timeout to wait until node is loaded and rendered, otherwise get_children_dom is not working
        setTimeout(function()
        {
            addDraggable(data.instance, data.node, false);
        }, 300);
    });
    
    return treeInstance;
}


function getContextMenu(node)
{
    //var path = getTreeNodePath(node);
    var treeObject = getTreeObject(getTreeNodePath(node));
    var selectedNodes = treeObject.jstree("get_selected", true);
    var paths = selectedNodes.map(function(selNode){return getTreeNodePath(selNode);});
    var visibleActions = {};
    _.each(treeActions, function(action){
        if(isActionAvailable(action.id))
        {
            if(!action.multi && paths.length > 1 )
                return -1;
            var allVisible = paths.every(function(path){
                return action.isVisible(action.useOriginalPath?path:getTargetPath(path)) === true;
            });
            if(allVisible)
                visibleActions[action.id] = action;
        }
//        if(isActionAvailable(action.id) &&
//                action.isVisible(action.useOriginalPath?path:getTargetPath(path)) === true)
//            visibleActions[action.id] = action;
    });
    return visibleActions;
}

function getTreeNodes(parentNode, root, callback)
{
    //root elements are pre-defined in "data-id" attribute of holding container
    //they will be first loaded as opened branches 
    if(parentNode.id=="#")
    {
        //TODO: check if .escapeHTML() is required and will not change paths behaviour
        var rootNode = {
                id : "#rt_" + root.escapeHTML(),
                parent : parentNode.id,
                text : root,
                state : {
                    opened    : true, 
                    disabled  : false, 
                    selected  : false
                },
                'children' : true
            };
        
        callback(rootNode);
    }
    else
    {
        var parentPath = getTreeNodePath(parentNode);
        var dc = getDataCollection(parentPath);
        if(dc.treeLoading) return;
        dc.treeLoading = true;
        //dc.getSize( new LoadNameListCallback(parentNode, dc, null, callback).func );
        
        var func = new LoadNameListCallback(parentNode, dc, null, callback).func;
        var parameters = { dc: dc.completePath };
        dc.getSizeWithCallbacks(
            function(dcSize) {
                func(dcSize);
            }, function(){
                func(-1);
        });
    }
}

//Repository tab change listeners
function addTabChangeListener(listener)
{
    var alreadyAdded = false;
    for (li = 0; li < this.tabChangeListeners.length; li++)
    {
        if (this.tabChangeListeners[li] == listener)
        {
            alreadyAdded = true;
            break;
        }
    }
    if (!alreadyAdded)
    {
        this.tabChangeListeners.push(listener);
    }
};

function fireTabChanged()
{
    for (li = 0; li < this.tabChangeListeners.length; li++)
    {
        this.tabChangeListeners[li].tabChanged();
    }
};

function initTreeActions(actions)
{
	treeActions = [];
    for (i = 0; i < actions.length; i++) 
    {
        var action = new Action();
        action.parse(actions[i]);
		treeActions.push(action);
    }
}

function performTreeAction(path, actionId)
{
	var hash = paramHash;
    var action = _.find(treeActions, function(action) {return action.id === actionId && action.isVisible(action.useOriginalPath?path:getTargetPath(path)) === true;});
    if(action)
    {
    	paramHash = hash;
    	action.doAction(action.useOriginalPath?path:getTargetPath(path));
    }
}
/**
 * Returns DOM tree object by given element path
 * 
 * @param {String}
 *            element path like "databases/Biopath" or "data/microarray"
 * @return corresponding tree object
 * to call jstree methods on returned .jstree(...) should be called
 */
function getTreeObject(element)
{
	if(!element) return;
	var departs = getPathComponents(element);
	return treeMap[departs[0]] || treeMap["databases"];
}

/**
 * Returns DOM-element of the tree by given repository path If element is not
 * exists or not loaded, it will return undefined without trying to load it
 * 
 * @param String
 *            path - path to the element
 * @return DOMElement or undefined
 * @see getTreeNodePath
 */
function getTreeNode(path)
{
	return document.getElementById("#rt_"+path);
}

/**
 * Returns repository path associated with given tree node Returns undefined if
 * specified DOM-element is not tree node
 * 
 * @param DOMElement
 *            node
 * @return String containing repository path, undefined if error
 * @see getTreeNode
 */
function getTreeNodePath(node)
{
	if(node != undefined && node.id && (node.id.indexOf('#rt_') == 0))
	{
		return node.id.substring(4);
	}
	return undefined;
}

/**
 * Update content of given tree branch (all elements will be removed and loaded
 * again)
 * 
 * @param element -
 *            complete DataCollection path (like "data/SQLTracks") to refresh.
 *            Use activeDC.selectedElement for selected element
 * @param useParent - if true and tree element is not found, try to refresh parent's branch first
 * @param noBackLinks - if false, will try to refresh all the known collections linking to supplied as well
 */
function refreshTreeBranch(element, useParent, noBackLinks)
{
    if(!element) return;
    if(!noBackLinks)
    {
        var backLinkList = getBackPaths(element);
        for(var i=0; i<backLinkList.length; i++)
            refreshTreeBranch(backLinkList[i], useParent, true);
        return;
    }
    var dc = getDataCollection(element);
    dc.invalidateCollection();
    var elementObj = getTreeNode(element);
    if (!elementObj && useParent) 
    {
        refreshTreeBranch(getElementPath(element), useParent);
        elementObj = getTreeNode(element);
    }
    if(!elementObj) return;
    treeObj = getTreeObject(element);
    $(elementObj).children("ul").detach();
    dc.invalidateCollection();
    dc.getSize(function(dcsize){
        elementObj.curSize = dcsize;
        treeObj.jstree('close_node', elementObj);
        treeObj.jstree('open_node', elementObj);
        //treeObj.jstree('refresh_node', elementObj);
        treeObj.jstree('deselect_all', true);
        treeObj.jstree('select_node', elementObj);
    });
}

/**
 * close tree branch
 * 
 * @param element -
 *          complete DataCollection path (like "data/SQLTracks") to refresh.
 *          Use activeDC.selectedElement for selected element
 */

function closeTreeBranch(element)
{
	var elementObj = document.getElementById("#rt_" + element);
	if(!elementObj) return;
	treeObj = getTreeObject(element);
	treeObj.jstree("close_node", "#rt_" + element);
}

var topElement = null;
var topElementArray = Array();
function createTreeItemDroppable(element, type, f)
{
	$(element).droppable({
		scope: "treeItem",
		tolerance: "pointer",
		greedy: "true",
		accept: function(draggable)
		{
			var path = getTargetPath(getTreeNodePath(draggable.parent().get(0)));
			if(!path) path = draggable.attr("data-path");
			if(!path) return false;
			if(type && !instanceOf(getElementClass(path), type)) return false;
			return true;
		},
		over: function(event, ui) {
		    topElementArray.push(topElement);
		    topElement = this;
		},
		out: function(){
            var lastElem = topElementArray.pop();
            topElement = lastElem;
        },
		drop: function(event, ui)
		{
		    if(topElement == this)
	        {
    			var path = getTargetPath(getTreeNodePath(ui.draggable.parent().get(0)));
    			if(!path) path = ui.draggable.attr("data-path");
                event.pageX = ui.offset.left;
                event.pageY = ui.offset.top;
    			f.call(event.target, path, event);
	        }
		}
	});
};

function createTreeItemDraggable(element, customOptions)
{
	var options = {
			appendTo: document.body,
			zIndex: 2000,
			distance: 1,
			delay: 50,
			revert: "invalid",
			scope: "treeItem",
			iframeFix: true,
			containment: "window",
			scroll: false,
			helper: function()
			{
				var path = $(this).attr("data-path");
				var name = getElementName(path);
				var shortName = name;
				if( name.length > 45 )
					shortName = name.substring(0,45) + "...";
				return $("<div/>").addClass("draghelper").css("background-image", getNodeIcon(getDataCollection(getElementPath(path)),name)).text(shortName);
			}
	};
	if(customOptions)
	{
		_.extend(options, customOptions);
	}
	$(element).mousedown(function(e) {e.preventDefault();});
	$(element).draggable(options);
}

function addTreeItemContextMenu(element, className, customActions)
{
    if(className == undefined)
        className = 'tree-menu-item';
    if(!contextMenuInitialized[className])
    {
        var cmf = function ($triggerElement, event, custAct){
            var path = $triggerElement.attr("data-path");
            var curitems = {};
            if(custAct == undefined)
                custAct = {};
            _.each(treeActions, function(action) {
                if(!(action.id in customActions) && action.id.match(/^(open|run)/) && 
                        isActionAvailable(action.id) &&
                        action.isVisible(action.useOriginalPath?path:getTargetPath(path)) === true)
                {
                    curitems[action.id] = { 
                            name: action.label,
                            icon: function(opt, $itemElement, itemKey, item){
                                $itemElement.css("background-image", "url('"+action.icon+"')");
                                return 'context-menu-icon-updated';
                            },
                            callback: function(){
                                action.doAction(action.useOriginalPath?path:getTargetPath(path));
                        }};
                }
            });
            var show = getPreference('showRepository') === null || getPreference('showRepository') === 'true';
            if(show)
                curitems['show_in_tree'] = { 
                    name: resources.menuShowInTree,
                    icon: 'expand',
                    callback: function(){
                        openBranch(path, true);
                }};
            for(var key in custAct)
            {
                if(custAct[key])
                {
                    curitems[key] = { 
                        name: custAct[key].label,
                        icon: function(opt, $itemElement, itemKey, item){
                            $itemElement.css("background-image", "url('"+custAct[key].icon+"')");
                            return 'context-menu-icon-updated';
                        },
                        callback: custAct[key].action
                    };
                }
            }
            
            return {
                items : curitems
            }
        }
        if(customActions == undefined)
            customActions = {};
        var cmfunction = _.partial(cmf, _, _, customActions);
        
        $.contextMenu(
        {
            selector: '.'+className,
            zIndex: 93,
            build: cmfunction
        });
        contextMenuInitialized[className] = true;
    }
    element.addClass(className);
}

/**
 * Internal function for opening branch. Don't call it directly, use openBranch
 * instead
 * 
 * @see openBranch
 */
function doOpenBranch(treeObj, branch, pos, tries, size)
{
    if(!treeObj)
        return;
	treeInProcess = true;
	if(tries <= 0)
	{
		treeInProcess = false;
		return;
	}
	var departs = getPathComponents(branch);
	if(departs.length <= pos)
	{
		treeInProcess = false;
		return;
	}
	var elementID = createPathFromComponents(departs.slice(0,pos+1));
	var element = document.getElementById("#rt_" + elementID.escapeHTML());
	if (element) 
	{
		if(pos+1 == departs.length)
		{
			treeInProcess = false;
			let elId = element.id;
			treeObj.jstree('open_node', elId, function(){
			    treeObj.jstree('deselect_all', true);
	            	treeObj.jstree('select_node', elId);
			});
			// finished
			element = $(element);
			var tree = element.closest(".tree_container");
			tree.scrollTop(Math.max(element.offset().top-tree.children().offset().top-tree.height()/2,0));
		} else
		{
			treeObj.jstree('open_node', element.id);
			doOpenBranch(treeObj, branch, pos + 1, 50, size==-1?-1:0);
		}
	} else
	{
		var parentID = createPathFromComponents(departs.slice(0,pos));
		var parent = document.getElementById("#rt_"+parentID);
		var addMoreElements = document.getElementById("#et_"+parentID);
		var parentNode = treeObj.jstree('get_node', "#rt_"+parentID);
		
		if(size != -1 && addMoreElements && parent && parentNode && parentNode.children.length > size)
		{
			treeObj.jstree().select_node(addMoreElements);
			setTimeout(function() {doOpenBranch(treeObj, branch, pos, 50, parentNode.children.length - 1);}, 100);
		} else
			setTimeout(function() {doOpenBranch(treeObj, branch, pos, tries-1, size);}, 100);
	}
	treeInProcess = false;
}

/**
 * Try to open given tree branch and select element (without clicking on it)
 * 
 * @param {String}
 *            branch - complete path to the branch
 * @param {Boolean}
 *            expand - whether try to expand long branches Note that it may give
 *            up in several cases: item not found; item is too far from
 *            collection start and expand set to false; loosy network connection
 *            and so on
 * @param {Boolean}
 *            noBackLinks - if false or undefined, will try to open all the known collections 
 *            linking to supplied as well
 */

function openBranch(branch, expand, noBackLinks)
{
    if(!noBackLinks)
    {
        var backLinkList = getBackPaths(branch);
        for(var i=0; i<backLinkList.length; i++)
            openBranch(backLinkList[i], expand, true);
        return;
    }
    var departs = getPathComponents(branch);
	var treeObj = treeMap[departs[0]] || treeMap["databases"];
	for(var i=0; i<perspective.repository.length; i++)
	{
		if(perspective.repository[i].path == departs[0])
		{
			el.repositoryTabs.tabs("option", "active", i);
			break;
		}
	}
	doOpenBranch(treeObj, branch, 1, 50, expand?0:-1);
}

function openBranchOrDefault(path, expand, noBackLinks)
{
    if(isRepositoryExists(path)){
        openBranch(path, expand, noBackLinks);
    }
    else if(el.repositoryTabs)
        el.repositoryTabs.tabs('option', 'active', 0);
}

function isRepositoryExists (path, noBackLinks)
{
    if(!noBackLinks)
    {
        var backLinkList = getBackPaths(path);
        for(var i=0; i<backLinkList.length; i++)
            if(isRepositoryExists(backLinkList[i], true))
                return true;
    }
    var departs = getPathComponents(path);
    var treeObj = treeMap[departs[0]] || treeMap["databases"];
    for(var i=0; i<perspective.repository.length; i++)
    {
        if(perspective.repository[i].path == departs[0])
            return true;
    }
}


function markActiveTreeNode()
{
    el.repositoryTabs.find(".active_node").removeClass("active_node");
    if (opennedDocuments[activeDocumentId] && opennedDocuments[activeDocumentId].completeName) 
    {
        var node = getTreeNode(findVisiblePath(opennedDocuments[activeDocumentId].completeName));
        if(node)
            $(node).addClass("active_node");
    }
}

function markActiveProject()
{
    el.repositoryTabs.find(".active_project").removeClass("active_project");
    var node = getTreeNode(findVisiblePath(currentProjectPath));
    if(node)
        $(node).addClass("active_project");
}

function storeLastOpenPath(path)
{
    var key = currentUser + '_last_selected';
    Cookies.setItem(key, path);
}

function openDefaultPath( callback )
{
    var key = currentUser + '_last_selected';
    var result = Cookies.getItem(key);
    if(result)
        callback (result);
    else
    {
        var projectPath = getDefaultProjectDataPath();
        if( projectPath )
            callback(projectPath);
        else
        {
            var defaultPath = appInfo.userProjectsPath;
            getDataCollection(defaultPath).getElementInfoAt(0, function(info)
        	{
        		var path = info ? createPath(defaultPath, info.name) + "/Data" : defaultPath;
                callback(path);
        	});
        }
    }
}

/*
 * Load name list callback class
 */
function LoadNameListCallback(parentNode, dc, itemsToLoad, callback)
{
    var parentPath = getTreeNodePath(parentNode);
    
    if(!itemsToLoad) 
        itemsToLoad = parentNode.curSize != undefined ? parentNode.curSize : maxItemsToLoad; 
        
    this.func = function(size)
    {
        if(size < 0)
        {
            dc.treeLoading = false;
            callback([]);
            return;
        }
        var treeObject = $.jstree.reference(parentNode.id);
        treeObject.creatingItem = true;
        if (size > maxItemsToLoad*2 && size > itemsToLoad) 
        {        
            dc.getElementInfoRange(0, itemsToLoad, function(nameList)
            {
                var elements = createTreeElements(parentPath, nameList, dc);
                if (nameList != null && nameList.length>0) 
                {
                    var lastElement =
                    {
                        id : "#et_" + parentPath.escapeHTML(),
                        parent : parent.id,
                        text : size-itemsToLoad<=itemsToLoad?resources.treeLoadLastItems.replace("{itemsToLoad}", size-itemsToLoad):
                            resources.treeLoadNextItems.replace("{itemsToLoad}", itemsToLoad).replace("{itemsNotLoaded}", size-itemsToLoad),
                        state : {
                            opened    : false, 
                            disabled  : false, 
                            selected  : false
                        },
                        totalSize: size
                    };
                    elements.push(lastElement);
                    //
                    dc.treeLoading = false;
                    callback(elements);
                }
                else
                {
                    //TODO: what to do? Is this situation possible?
                    console.log("empty name list for size>100, some error happened");
                }
            });
        } 
        else
        {
            dc.getNameList(function(nameList)
            {
                var elements = createTreeElements(parentPath, nameList, dc);
                if (nameList != null && nameList.length>0) 
                {
                } 
                else
                {
                    //TODO: do we need this closing for empty element?
                    treeObject.close_node(parentNode);
                }
                dc.treeLoading = false;
                callback(elements);
            }, function(){
                dc.treeLoading = false;
                callback([]);
            });
         }
        treeObject.creatingItem = false;
    };
}

/*
 * Extend name list callback class
 */
function AddNameListCallback(treeObject, parentNode, dc, totalSize)
{
    this.treeObject = treeObject;
    this.parentNode = parentNode;
    this.totalSize = totalSize;
    this.dc = dc;
    
    var _this = this;
    var lastScrollId = null;
    this.func = function(nameList)
    {
        var parentPath = getTreeNodePath(_this.parentNode);
        var itemsToLoad = maxItemsToLoad; 
        _this.treeObject.creatingItem = true;
        if (nameList != null)
        {
            var elements = createTreeElements(parentPath, nameList, _this.dc);
            for(var i = 0; i < elements.length; i++)
            {
                var nodeId = _this.treeObject.create_node(parentNode, elements[i]);
                if(i==elements.length-1)
                    lastScrollId = nodeId;
            }
            //addDraggable(treeObject, parentNode, addedNodes);
        }
        var curSize = parentNode.children.length;
        if (curSize && totalSize && totalSize - curSize > 0) 
        {
            var lastElement =
            {
                id : "#et_" + parentPath.escapeHTML(),
                parent : _this.parentNode.id,
                text: totalSize - curSize <= itemsToLoad ? 
                        resources.treeLoadLastItems.replace("{itemsToLoad}", totalSize - curSize) :
                        resources.treeLoadNextItems.replace("{itemsToLoad}", itemsToLoad).replace("{itemsNotLoaded}", totalSize - curSize),
                state : {
                    opened    : false, 
                    disabled  : false, 
                    selected  : false
                },
                totalSize: totalSize
            };
            _this.treeObject.create_node(parentNode, lastElement, "last");
        }
        _this.treeObject.creatingItem = false;
        addDraggable(_this.treeObject, _this.parentNode);
        if(lastScrollId != null)
        {
            var scrollElement = $(document.getElementById(lastScrollId));
            if(scrollElement)
            var tree = scrollElement.closest(".tree_container");
            tree.scrollTop(Math.max(scrollElement.offset().top-tree.children().offset().top-tree.height()/2,0));
        }
    };
}

function createTreeElements(parentPath, nameList, dc)
{
    var elements = [];
    for(var i = 0; i < nameList.length; i++)
    {
        var title = nameList[i].name;
        if(nameList[i].title)
            title = nameList[i].title;
        var icon = getNodeIcon(dc, nameList[i].name);
        var icon2 = icon.substring(5, icon.length - 2);

	//call escapeHTML only on name, double call on parentPath results in wrong ampersand escaping  
        var nameEscaped = nameList[i].name.escapeHTML();
	var element =
        {
            id : "#rt_"+createPath(parentPath, nameEscaped),
            parent : parent.id,
            text : title,
            icon : icon2,
            state : {
                opened    : false, 
                disabled  : false, 
                selected  : false
            },
            a_attr : {"class":"ui-draggable"},
            'children' : nameList[i].hasChildren
        }; 
        
        if( activeDocumentId && opennedDocuments[activeDocumentId] && opennedDocuments[activeDocumentId].completeName 
                && opennedDocuments[activeDocumentId].completeName == createPath(parentPath, nameList[i].name) )
        {
            element.li_attr = {"class": "active_node"};
        }
        
        if(getTargetPath(parentPath) == appInfo.userProjectsPath && currentProjectPath && getElementName(currentProjectPath) == nameList[i].name)
        {
            if(!element.li_attr)
                element.li_attr = {}; 
            element.li_attr["class"] = element.li_attr["class"] ? element.li_attr["class"] + ", active_project" : "active_project";
        }
        elements.push(element);
    }
    return elements;
}


function addDraggable(treeObject, parentNode, nodes)
{
    var children = treeObject.get_children_dom (parentNode);
    if(children)
    {
        _.each(children, function(childNode){
            if(!nodes || nodes[childNode.id])
                createTreeItemDraggable($(childNode).children("a").eq(0), {
                    helper: function(event) {
                      var clone = $(this).clone().css({
                          "background-repeat": "no-repeat", 
                          "padding-left": "20px",
                          "font-family": "Verdana, Arial, sans-serif", 
                          "font-size": "11px", 
                          "text-decoration": "none", 
                          "height": "16px"
                      });
                      clone.children("i").css({"width":"18px",
                          "height":"18px",
                          "background-repeat": "no-repeat"
                          });
                      var text = clone.text();
                      if( text && text.length > 45 )
                      {
                          clone.text(text.substring(0,45) + "...");
                      }
                      return clone;
                }});
        });
    }
    if(!nodes)
    {
        createTreeItemDraggable(treeObject.get_node(parentNode, true).children("a").eq(0), {
            helper: function(event) {
              var clone = $(this).clone().css({
                  "background-repeat": "no-repeat", 
                  "padding-left": "20px",
                  "font-family": "Verdana, Arial, sans-serif", 
                  "font-size": "11px", 
                  "text-decoration": "none", 
                  "height": "16px"
              });
              clone.children("i").css({"width":"18px",
                  "height":"18px",
                  "background-repeat": "no-repeat"
                  });
              var text = clone.text();
              if( text && text.length > 45 )
              {
                  clone.text(text.substring(0,45) + "...");
              }
              return clone;
        }});
    }
}

/*
 * Protection status callback class
 */
function ProtectionStatusCallback(node)
{
    this.node = node;
    this.func = function(type, dc)
    {
        if (type == 0) 
        {
            node.children("a").get(0).style.backgroundImage = "url('icons/remoteNotProtectedDatabaseIcon.png')";
        }
        else 
            if (type == 1) 
            {
                node.children("a").get(0).style.backgroundImage = "url('icons/remotePublicReadDatabaseIcon.png')";
            }
            else if (type == 2 || type == 3 || type == 4) 
            { 
                var _type = type;
                var _dc = dc;
                dc.getPermission( function(perm, dc2)
                {
                    if (_type == 2) 
                    {
                        if(perm == 0)
                            node.children("a").get(0).style.backgroundImage = "url('icons/remotePublicDatabaseIcon2.png')";
                        else
                            node.children("a").get(0).style.backgroundImage = "url('icons/remotePublicDatabaseIcon.png')";
                    }
                    else 
                        if (_type == 3) 
                        {
                            if (perm == 0) 
                                node.children("a").get(0).style.backgroundImage = "url('icons/remoteProtectedReadDatabaseIcon2.png')";
                            else
                                node.children("a").get(0).style.backgroundImage = "url('icons/remoteProtectedReadDatabaseIcon.png')";
                        }
                        else 
                            if (_type == 4) 
                            {
                                if(perm == 0)
                                    node.children("a").get(0).style.backgroundImage = "url('icons/remoteProtectedDatabaseIcon2.png')";
                                else
                                    node.children("a").get(0).style.backgroundImage = "url('icons/remoteProtectedDatabaseIcon.png')";
                            }
                });
            }
    };
}
