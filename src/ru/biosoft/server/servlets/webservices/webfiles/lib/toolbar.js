/* $Id: toolbar.js,v 1.18 2013/12/09 11:26:04 lan Exp $ */
/*
 * Toolbars-related functions 
 */
var toolbarActions = [];
var dynamicToolbarActions = {};
var userToolbarActions = [];
var journalBox = null;

/*
 * Init toolbar
 */
function initToolbar(actionsInfo)
{
    var actions = actionsInfo.toolbar;
    for (i = 0; i < actions.length; i++) 
    {
        var actionProperties = new Action();
        actionProperties.parse(actions[i]);
        toolbarActions[i] = actionProperties;
    }
    initDynamicActions(actionsInfo.dynamic);
    initUserToolbarActions();
    updateToolbar();
    createTreeItemDroppable($('#mainToolbar'), null, function(path)
	{
        for(var i in userToolbarActions)
    	{
    		if(userToolbarActions[i].path === path) return;
    	}
    	userToolbarActions.push(createUserToolbarAction(path));
        var userToolbarPreference = "";
        $.each(userToolbarActions, function(index, value){userToolbarPreference += value.path + ";";});
        setPreference("UserToolbar", userToolbarPreference); 
    	updateToolbar();
	});
	$(document.body).delegate(".fg-button:not(.ui-state-disabled)", "mouseover", function()
			{
				$(this).addClass("ui-state-hover");
			}).delegate(".fg-button:not(.ui-state-disabled)", "mouseout", function()
			{
				$(this).removeClass("ui-state-hover");
			}).delegate(".fg-button:not(.ui-state-disabled)", "mousedown", function()
		    {
		        $(this).parents('.fg-buttonset-single:first').find(".fg-button.ui-state-active").removeClass("ui-state-active");
		        if ($(this).is('.ui-state-active.fg-button-toggleable, .fg-buttonset-multi .ui-state-active')) 
		        {
		            $(this).removeClass("ui-state-active");
		        }
		        else 
		        {
		            $(this).addClass("ui-state-active");
		        }
		    }).delegate(".fg-button:not(.ui-state-disabled)", "mousedown", function()
		    {
		        if (!$(this).is('.fg-button-toggleable, .fg-buttonset-single .fg-button,  .fg-buttonset-multi .fg-button')) 
		        {
		            $(this).removeClass("ui-state-active");
		        }
		    });
}

function initUserToolbarActions()
{
    userToolbarActions = [];
    var toolbarPrefValue = getPreference("UserToolbar");
    if(toolbarPrefValue != null)
    {
        var toolbarActions = toolbarPrefValue.split(";");
        var addedActions = [];
        var callback = _.after(toolbarActions.length, function()
        {
        	_.each(addedActions, function(action)
        	{
        		if(action) userToolbarActions.push(action);
        	});
        	updateToolbar();
        });
        _.each(toolbarActions, function(action, idx)
        {
            if(action == "") callback();
            else
            {
                getDataCollection(getElementPath(action)).getElementInfo(getElementName(action), function(info)
                {
                    if (info != null) 
                    	addedActions[idx] = createUserToolbarAction(action);
                    callback();
                });
            }
        });
    } else updateToolbar();
}

function createUserToolbarAction(element)
{
	return {
	    id: "action_"+element,
	    path: element,
	    label: getElementName(element),
	    icon: getNodeIcon(getDataCollection(getElementPath(element)), getElementName(element)).replace(/^url\(\'/, "").replace(/\'\)/,""),
	    visible: function() { return true; },
	    action: function() { openDocument(element); },
	    doAction: function() { openDocument(element); }
	};
}

function createJournalBox(data)
{
    journalBox = $('<select id="journalComboBox" class="ui-state-default" style="font-size: 10pt;float:right; max-width: 300px"></select>');
	journalBox.children('option').remove();
	if(!appInfo.userProjectRequired)
	    journalBox.append('<option id="-">-</option>');
    var current = "-";
    if(data.current)
        current = data.current;
    setCurrentProjectPath(journalNameToProject(current));
    var names = data.names;
    for (i = 0; i < names.length; i++) 
    {
    	if (names[i].length > 0) 
        {
            journalBox.append('<option id="' + names[i] + '">' + names[i] + '</option>');
        }
    }
    journalBox.val(current);
    $('#mainToolbar').on("change", "#journalComboBox", function()
    {
        setCurrentProject(journalNameToProject(journalBox.val()));
    });
}

function initJournals(callback)
{
	queryBioUML("web/journal/init", {}, function(data){
	    createJournalBox(data.values);
        updateToolbar();
        if(callback)
            callback();
    });
}

function isActionAvailable(actionId)
{
    function isRulePassed(actionId, rule)
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
        return actionId.match(rule.pattern);
    }
    var result = true;
    for(var i in perspective.actions)
    {
        var rule = perspective.actions[i];
        if(isRulePassed(actionId, rule)) 
            result = rule.type == "allow";
    }
    return result;
}

var perspectiveSelector;
function setPerspectiveName(name)
{
	resetPerspectiveName(name, false);
}
function tryResetPerspectiveName()
{
	if( perspective && perspective.name )
        resetPerspectiveName(perspective.name, true);
}
/**
 * calls set perspective name if perspective needs to be changed
 */
function resetPerspectiveName(name, ignoreIfSame)
{
    if(!perspectiveSelector)
        return;
	if(perspectiveSelector.val() != name)
	    perspectiveSelector.val(name);
	if( !ignoreIfSame )
		ignoreIfSame = false;
	queryBioUML("web/perspective", {name: name, ignoreIfSame: ignoreIfSame}, function(data)
	{
	    if( ignoreIfSame === true && data.values.ignore === true )
			  return;
	  	setPerspective(data.values);
	  	updateRepositoryTabs();
	  	updateViewParts();
	  	initVirtualCollections();
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
	  	updateToolbar();
	  	initIntro();
	});
}

/*
 * Update main toolbar actions
 */
function updateToolbar()
{
    var toolbar = $('#mainToolbar');
    toolbar.children('.fg-buttonset').remove();
    toolbar.children('.fg-rightcomponent').remove();
    
    var block = $('<div class="fg-buttonset ui-helper-clearfix"></div>');
    var lastSeparator = true;
    for (var i = 0; i < toolbarActions.length; i++) 
    {
        if (toolbarActions[i].id) 
        {
            if(!isActionAvailable(toolbarActions[i].id)) continue;
            var doc = getActiveDocument();
            var visible = toolbarActions[i].isVisible(doc);
            if (visible !== -1) 
            {
                var action = createToolbarButton(toolbarActions[i].label, toolbarActions[i].icon, toolbarActions[i].doAction);
                setToolbarButtonEnabled(action, visible);
                block.append(action);
                lastSeparator = false;
            }
        }
        else 
        {
        	if(!lastSeparator)
        	{
        		block.append($('<span class="fg-separator"/>'));
        		lastSeparator = true;
        	}
        }
    }
    if(!lastSeparator)
        block.append($('<span class="fg-separator"/>'));
    toolbar.append(block);
    
    block = $('<div class="fg-buttonset ui-helper-clearfix" id="dynamicToolbarBlock"></div>');
    toolbar.append(block);
    
    updateDynamicToolbar();
    
    var block2 = $('<div class="fg-rightcomponent ui-helper-clearfix"></div>');
    
    // Perspective selector
    if(/*BioUML.perspectiveNames.length > 1 &&*/ !BioUML.disablePerspectiveSelector && !appInfo.disablePerspectiveSelector)
    {
	    perspectiveSelector = $('<select class="ui-state-default" style="font-size: 10pt;float:right; max-width:300px;"/>');
	    for(var i=0; i<BioUML.perspectiveNames.length; i++)
		{
	    	perspectiveSelector.append($('<option/>').text(BioUML.perspectiveNames[i]));
		}
	    perspectiveSelector.val(perspective.name);
	    perspectiveSelector.change(function() { 
          setPerspectiveName(perspectiveSelector.val());
        });
	    block2.append(perspectiveSelector);
    }
    
    if(journalBox != null && !BioUML.disableProjectSelector && !Boolean(appInfo.disableProjectSelector) && ( perspective && perspective.projectSelector))
    {
    	block2.append(journalBox);
    }
    for (var i = 0; i < userToolbarActions.length; i++) 
    {
        var action = $('<span class="fg-button ui-state-default fg-button-icon-solo  ui-corner-all"><img class="fg-button-icon-span" src="' + userToolbarActions[i].icon + '"/></span>');
        action.click(userToolbarActions[i].doAction);
        var path = userToolbarActions[i].path;
        action.attr("data-path", path).attr("title", path);
        createTreeItemDraggable(action);
        addTreeItemContextMenu(action, 'tree-menu-toolbar', {
        	'remove_toolbar_item': {label: resources.menuRemoveFromUserToolbar, icon: "icons/remove.gif", action: function(itemKey, options, originalEvent)
    		{
    			var path = options.$trigger.attr("data-path");
	        	for(var i in userToolbarActions)
	        	{
	        		if(userToolbarActions[i].path === path)
	        		{
	        			userToolbarActions.splice(i, 1);
                        var userToolbarPreference = "";
                        $.each(userToolbarActions, function(index, value){userToolbarPreference += value.path + ";";});
                        setPreference("UserToolbar", userToolbarPreference); 
	        			updateToolbar();
	        			return;
	        		}
	        	}
    		}}
        });
        block2.append(action);
    }

    toolbar.append(block2);
}

/**
 * Init dynamic actions
 */
function initDynamicActions(actions)
{
    for (i = 0; i < actions.length; i++) 
    {
        var actionProperties = new DynamicAction();
        actionProperties.parse(actions[i]);
        dynamicToolbarActions[actionProperties.id] = actionProperties;
    }
}

/**
 * Update toolbar for dynamic actions
 */
function updateDynamicToolbar(block)
{
    var doc = getActiveDocument();
    if( doc == null )
        return;
    if(dynamicToolbarActions.length == 0)
        return;
    var block = $('#dynamicToolbarBlock');
    if(!block)
        return;
    var doUpdateToolbar = function(visibles)
    {
        for (var i = 0; i < visibles.length; i++) 
        {
            if( dynamicToolbarActions[visibles[i]] != undefined )
            {
                var curAction = dynamicToolbarActions[visibles[i]];
                if( curAction.numSelected != curAction.selConstants.SELECTED_UNDEFINED && !doc.getSelection)
                    continue;
                var action = createToolbarButton(curAction.label, curAction.icon, curAction.doAction);
                if(doc.readOnly && !curAction.acceptReadOnly) setToolbarButtonEnabled(action, false);
                block.append(action);
            } else
            {
            	block.append("<span class='fg-separator'/>");
            }
        }
    }; 
    if(doc.visibleActions == null)
    {
        queryBioUMLWatched(doc.completeName + "_dynamicActions", "web/action/visibleall", 
	    {
	        type: "dynamic",
	        de: doc.completeName
	    }, function(data)
	    {
	        var visibles = data.values;
	        var filtered = [];
	        var converted = {};
	        var re = /^(.+)\.(\d+)$/;
	        for (var i=0; i<visibles.length; i++)
	    	{
	        	var name = visibles[i];
	        	if(name == "")
	        	{
	        		filtered.push(name);
	        		continue;
	        	}
	        	var baseName = name;
	        	var index = 0;
	        	var m = baseName.match(re);
	        	if(m)
	    		{
	        		baseName = m[1];
	        		index = m[2];
	    		}
	        	if(converted[baseName] == undefined || index > converted[baseName][0])
	        	{
	        		if(!converted[baseName])
	        			filtered.push(name);
	        		converted[baseName] = [index, name];
	        	}
	    	}
	        doc.visibleActions = filtered;
	        doUpdateToolbar(filtered);
	    }, function(){});
    } else
    	doUpdateToolbar(doc.visibleActions);
}

/*
 * Action class
 */
function Action()
{
	var _this = this;
    this.id = null;
    this.label = null;
	this.visible = function(node) {
		if(!node || node.length === 0) return -1;
		if(!_this.multi && node.length > 1) return -1;
		if(node.length === 1) node = [node];
		var result = true;
		for(var i=0;i<node.length;i++)
		{
			var path = getTreeNodePath(node[i][0]);
			if(!_this.useOriginalPath) path = getTargetPath(path);
			var visible = _this.isVisible(path);
			if(visible === -1) return -1;
			if(visible === false) result = false;
		}
		return result;
	};
	this.action = function(data) {
		if(!data || data.length === 0) return -1;
		var treeObject = $.jstree.reference(data.reference);
		var selectedNodes = treeObject.get_selected(true);
        if(!_this.multi && selectedNodes.length > 1) return -1;
		if(_this.multiAction)
		{
			var paths = [];
			for(var i=0;i<selectedNodes.length;i++)
			{
				var path = getTreeNodePath(selectedNodes[i]);
				if(!_this.useOriginalPath) path = getTargetPath(path);
				paths.push(path);
			}
			_this.multiAction(paths);
		} else
		{
		    for(var i=0;i<selectedNodes.length;i++)
            {
		        var path = getTreeNodePath(selectedNodes[i]);
    			if(!_this.useOriginalPath) path = getTargetPath(path);
    			_this.doAction(path);
            }
		}
	};
    this.icon = null;
    
    /**
     * eval function represented as string
     */
    function evalFunction(func, defaultFunc)
    {
        var param = func.toString().match(/\((.*?)\)/)[1].split(/,\s*/);
        var start = func.indexOf('{');
        var end = func.lastIndexOf('}');
        if (start != -1 && end != -1 && end > start) 
        {
            return new Function(param.join(","), func.substring(start + 1, end));
        }
        return defaultFunc;
    }

    this.parse = function(action)
    {
        if (action.id) 
        {
            this.id = action.id;
            this.label = action.label;
            this.icon = action.icon;
            this.multi = action.multi;
            this.useOriginalPath = action.useOriginalPath;
            this.isVisible = evalFunction(action.visible, function(){return -1;});
            if(action.multiAction)
            {
            	this.multiAction = evalFunction(action.multiAction);
            	this.doAction = function(path)
            	{
					_this.multiAction([ path ]);
				};
            }
            if(action.action)
            	this.doAction = evalFunction(action.action);
        }
    };
}

/*
 * DynamicAction class
 */
function DynamicAction()
{
    this.selConstants = {
        SELECTED_ZERO: 0,
        SELECTED_ONE: 1,
        SELECTED_ANY: 2,
        SELECTED_ZERO_OR_ANY: 3,
        SELECTED_UNDEFINED: -1
    };
    
    this.id = null;
    this.label = null;
    this.icon = null;
    this.parse = function(action)
    {
        if (action.id) 
        {
            this.id = action.id;
            this.label = action.label;
            this.icon = action.icon;
            this.acceptReadOnly = action.acceptReadOnly;
            this.numSelected = action.numSelected || this.selConstants.SELECTED_UNDEFINED;
        }
    };
    
    this.visible = function(node) {
        if(!node || node.length != 1) return -1;
        return _this.isVisible(node);
    };
    this.action = function(node) {
        if(!node || node.length != 1) return -1;
        return _this.doAction(node);
    };
    
    this.isVisible = function(node, callback)
    {
        if(!node)
            return false;
        var path = node.completeName;
        if(!path)
            return false;
        var params = {
            type: "dynamic",
            name: this.id,
            de: path,
            selectionBase: node.selectionBase
        };
        var _this = this;
        queryBioUML("web/action/visible", params, function(data)
        {
            callback(true, _this);
        }, function(data)
        {
            callback(false);
        });
    };
    var _thisAction = this; 
    this.doAction = function(node)
    {
        var activeDocument = getActiveDocument();
        if(!activeDocument)
            return;
        var path = activeDocument.completeName;
        if(!path)
            return;
        if( !activeDocument.getSelection && _thisAction.numSelected != _thisAction.selConstants.SELECTED_UNDEFINED)
            return;
        var selectedRows = activeDocument.getSelection ? activeDocument.getSelection() : [];
        if(!_thisAction.checkNumSelected(selectedRows.length))
        {
            logger.error(_thisAction.getNumSelectedMessage());
            return;
        }
        var params = {
                type: "dynamic",
                name: _thisAction.id,
                selectionBase: activeDocument.selectionBase,
                de: path,
                jsonrows: $.toJSON(selectedRows)
            };
        queryBioUML("web/action/validate", params, function(data)
        {
            if(data.values)
            {
                if (data.values.confirm) 
                {
                    createConfirmDialog(data.values.confirm, function()
                    {
                        _thisAction.performAction(params["jsonrows"]);
                    });
                }
                else 
                {
                	if(data.values.length == 1 && data.values[0].name == "target")
                	{
                		createSelectElementDialog(data.values[0].displayName, 
                				data.values[0].value,
                				{dataElementType: data.values[0].elementClass,
                				promptOverwrite: data.values[0].promptOverwrite,
                				elementMustExist: data.values[0].elementMustExist},
                				function(path) {
                					_thisAction.performAction(params["jsonrows"], $.toJSON([{name: "target", value: path}]));
                				});
                	} else
                	{
                    	var propertyPane = new JSPropertyInspector();
                        var parentID = "dynamic_action_pi_cont_" + rnd();

                        function syncronizeData(control)
                		{
                            queryBioUML("web/action/validate", 
                        		_.extend({properties: convertDPSToJSON(propertyPane.getModel(), control)}, params),
    	                        function(data)
    	                        {
    	                    	    $(getJQueryIdSelector(parentID)).empty();
    	                    		var beanDPS = convertJSONToDPS(data.values);
    	                    		propertyPane = new JSPropertyInspector();
    	                    	    propertyPane.setParentNodeId(parentID);
    	                    	    propertyPane.setModel(beanDPS);
    	                    	    propertyPane.generate();
    	                    		propertyPane.addChangeListener(function(control, oldValue, newValue) {
    	                    			syncronizeData(control);
    	                    		});
    	                        });
                		}
                		var beanDPS = convertJSONToDPS(data.values);
                	    var dialogDiv = $('<div title="'+resources.commonDynamicActionPropertiesTitle+'"></div>');
                	    dialogDiv.append('<div id="' + parentID + '"></div>');
                	    dialogDiv.dialog(
                	    {
                	        autoOpen: false,
                	        modal: true,
                	        width: 500,
                	        height: 500,
                	        buttons: 
                	        {
                	            "Cancel": function()
                	            {
                	                $(this).dialog("close");
                	                $(this).remove();
                	            },
                	            "Ok": function()
                	            {
                                    var dps = propertyPane.getModel();
                                    var json = convertDPSToJSON(dps);
                                    _thisAction.performAction(params["jsonrows"], json);
                                    $(this).dialog("close");
                                    $(this).remove();
                	            }
                	        }
                	    });
                	    addDialogKeys(dialogDiv);
                	    sortButtons(dialogDiv);
                	    dialogDiv.dialog("open");
                	    
                	    propertyPane.setParentNodeId(parentID);
                	    propertyPane.setModel(beanDPS);
                	    propertyPane.generate();
            			propertyPane.addChangeListener(function(control, oldValue, newValue) {
            				syncronizeData(control);
            			});
                	}
                }
            }
            else
            {
                _thisAction.performAction(params["jsonrows"]);
            }
        });
    };
    
    this.checkNumSelected = function(selected)
    {
        switch(_thisAction.numSelected)
        {
            case _thisAction.selConstants.SELECTED_ZERO:
                return (selected == 0);
            case _thisAction.selConstants.SELECTED_ONE:
                return (selected == 1);
            case _thisAction.selConstants.SELECTED_ANY:
                return (selected >= 1);
            case _thisAction.selConstants.SELECTED_ZERO_OR_ANY:
            	return true;
        }
        return true;
    };
    this.getNumSelectedMessage = function()
    {
        switch(_thisAction.numSelected)
        {
            case _thisAction.selConstants.SELECTED_ZERO:
                return resources.commonErrorActionNoSelectionRequired;
            case _thisAction.selConstants.SELECTED_ONE:
                return resources.commonErrorActionOneSelectedRowRequired;
            case _thisAction.selConstants.SELECTED_ANY:
                return resources.commonErrorActionSelectedRowsRequired;
            case _thisAction.selConstants.SELECTED_UNDEFINED:
                return resources.commonErrorActionUnknown;
            default:
                return "";
        }
    };
    
    this.performAction = function(rows, properties)
    {
        var activeDocument = getActiveDocument();
        if(!activeDocument)
            return;
        var path = activeDocument.completeName;
        if(!path)
            return;
        var jobID = rnd();
        var params = {
            type: "dynamic",
            name: _thisAction.id,
            de: path,
            selectionBase: activeDocument.selectionBase,
            jobID: jobID
        };
        if(properties)
            params["properties"] = properties;
        if(rows)
            params["jsonrows"] = rows;
        queryBioUML("web/action/run", params
        , function(data)
        {
            if(data.values == "action finished")
        	{
        		// TODO: change this to something more appropriate like opening result
        		logger.message(_thisAction.label + " performed successfully");
        	} else
        		showProgressDialog(jobID, _thisAction.label, function(message, results) {
        			if(results != undefined)
        			{
        				for(var i=0; i<results.length; i++)
        				{
                            var id = allocateDocumentId(results[i]);
                            if( opennedDocuments[id] != undefined && opennedDocuments[id].refresh != undefined )
                                opennedDocuments[id].refresh();
                            else
                            {
            					refreshTreeBranch(getElementPath(results[i]));
        					    openDocument(results[i]);
                            }
        				}
        			}
        		});
        });
    };
}
