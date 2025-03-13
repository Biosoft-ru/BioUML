/**
 * Abstract diagram parent class
 *
 * @author anna
 */

function DiagramSupport(completeName)
{
    
}

/*
 * Constructor where some required variables are initialized
 */
DiagramSupport.prototype.DiagramSupportConstructor = function (completeName)
{
    var _this = this;
    this.completeName = completeName;
    this.name = getElementName(completeName);
    this.drawType = "json";
    this.type == undefined;
    this.changed = false;
    this.autoLayout = false;
    this.resizable={};
    
    queryBioUML("web/diagram/get_auto_layout", {de: _this.completeName}, function(data)
    {
        _this.autoLayout = data.values.autoLayout;
        updateToolbar();
    }, function() {});
    
    var dc = getDataCollection(this.completeName);
    dc.addChangeListener(this);
    dc.addRemoveListener(this);
    
    //interface features
    this.tabId = allocateDocumentId(completeName);
    this.showToolbar = true;
    this.zoom = 1.0;
    
    //listeners
    this.viewAreaListeners = new Array();
    this.selectListeners = new Array();
    this.refreshRequest = null;
    
    //diagram features
    this.modelDetected = false;
    this.isArterialTree = undefined;
    this.isAgentModel = undefined;
    this.isAnnotation = undefined;
    
    this.updateSelection = _.debounce(_.bind(function(nodes)
    {
        queryBioUML("web/diagram/update_selections",
        {
            de: this.completeName,
            selections: $.toJSON(nodes == undefined?[]:nodes)
        }, function() {}, function() {});
    }, this), 100);
};

DiagramSupport.prototype.isAutoLayout = function()
{
    return this.autoLayout;
};

DiagramSupport.prototype.setAutoLayout = function(value)
{
    var _this = this;
    queryBioUML('web/diagram/set_auto_layout', {
        de: _this.completeName,
        value: value
    }, function(data) {
        _this.autoLayout = data.values.autoLayout;
        updateToolbar();
    });
};

DiagramSupport.prototype.editViewOptions = function()
{
    var _this = this;
    createBeanEditorDialog(resources.dlgEditViewOptionsTitle, "properties/diagramViewOptions/"+this.completeName, function() {
        _this.changed = true;
        _this.refresh();
    }, true);
};

DiagramSupport.prototype.editElementViewOptions = function()
{
	var _this = this;
    var selection = this.getSelection();
    if(selection && selection.length > 0)
    {
    	createBeanEditorDialog(resources.dlgEditViewOptionsTitle, "properties/diagramElementViewOptions/"+this.completeName+ "/" + selection[0], function() {
            _this.changed = true;
            _this.refresh();
        }, true);
    }
    else
	{
    	logger.message("Please, select diagram element");
	}
    
};

DiagramSupport.prototype.getSelection = function(value)
{
    var selected = this.selector.nodes;
    if(!selected || selected.length==0)
        selected = this.edgeEditor.getSelection();
    return selected;
};

DiagramSupport.prototype.undo = function(editNum)
{
    var _this = this;
    showWaitDialog("Undo in progress...");
    queryBioUML("web/diagram/undo",
    {
        de: this.completeName,
        undoTo: editNum >= 0 ? editNum : "",
        type: this.drawType
    }, function(data)
    {
    	removeWaitDialog();
        _this.selector.clear();
        _this.update(data, true);
    }, function(){
    	removeWaitDialog();
    });
};

DiagramSupport.prototype.redo = function()
{
    var _this = this;
    showWaitDialog("Redo in progress...");
    queryBioUML("web/diagram/redo",
    {
        de: this.completeName,
        type: this.drawType
    }, function(data)
    {
    	removeWaitDialog();
        _this.selector.clear();
        _this.update(data, true);
    }, function(){
    	removeWaitDialog();
    });
};

/**
 * Return nodeName relative to diagram for complete nodeName
 */
DiagramSupport.prototype.getRelativeNodeName = function(nodeName)
{
    if (nodeName.substr(0, this.completeName.length) == this.completeName)
    {
        nodeName = nodeName.substr(this.completeName.length);
        while(nodeName.substr(0,1) == "/")
            nodeName = nodeName.substr(1);
    }
    return nodeName;
};

/*
 * Create new node action
 */
DiagramSupport.prototype.createNewNode = function(event, dcName, type)
{
    var parentX = this.diagramContainerDiv.offset().left;
    var parentY = this.diagramContainerDiv.offset().top;
    var x = event.originalEvent.clientX - parentX + this.diagramContainerDiv.scrollLeft();
    var y = event.originalEvent.clientY - parentY + this.diagramContainerDiv.scrollTop();
    var nodeDialog = new NewNodeDialog(this, dcName, type, x / this.zoom, y / this.zoom);
    nodeDialog.open();
};

/*
 * Create new edge action
 */
DiagramSupport.prototype.createNewEdge = function(event, dcName, type)
{
    this.selector.clear();
    this.edgeEditor.clear();
    this.edgeCreation = [dcName, type]; 
};

/*
 * Create new port action
 */
DiagramSupport.prototype.createNewPort = function(event, dcName, type)
{
    var parentX = this.diagramContainerDiv.offset().left;
    var parentY = this.diagramContainerDiv.offset().top;
    var x = event.originalEvent.clientX - parentX + this.diagramContainerDiv.scrollLeft();
    var y = event.originalEvent.clientY - parentY + this.diagramContainerDiv.scrollTop();
    var portDialog = new NewPortDialog(this, dcName, type, x / this.zoom, y / this.zoom);
    portDialog.open();
};

/*
 * Create new reaction action
 */
DiagramSupport.prototype.createNewReaction = function(event)
{
    var parentX = this.diagramContainerDiv.offset().left;
    var parentY = this.diagramContainerDiv.offset().top;
    var x = event.originalEvent.clientX - parentX + this.diagramContainerDiv.scrollLeft();
    var y = event.originalEvent.clientY - parentY + this.diagramContainerDiv.scrollTop();
    var reactionDialog = new NewReactionDialog(this, x / this.zoom, y / this.zoom);
    reactionDialog.open();
};


/*
 * Listeners
 */
DiagramSupport.prototype.addSelectListener = function(listener)
{
    var alreadyAdded = false;
    for (li = 0; li < this.selectListeners.length; li++)
    {
        if (this.selectListeners[li] == listener)
        {
            alreadyAdded = true;
            break;
        }
    }
    if (!alreadyAdded)
    {
        this.selectListeners.push(listener);
    }
};
    
DiagramSupport.prototype.removeSelectListener = function(listener)
{
    for (li = 0; li < this.selectListeners.length; li++)
    {
        if (this.selectListeners[li] == listener)
        {
            this.selectListeners.splice(li,1);
            break;
        }
    }
};
    
DiagramSupport.prototype.fireSelectNode = function(node)
{
    for (var i = 0; i < this.selectListeners.length; i++)
    {
        var listener = this.selectListeners[i];
        if(listener.nodeSelected)
            listener.nodeSelected(node);
    }
};

DiagramSupport.prototype.fireSelectionChanged = function(nodes)
{
    for (var i = 0; i < this.selectListeners.length; i++)
    {
        var listener = this.selectListeners[i];
        if(listener.selectionChanged)
            listener.selectionChanged(nodes);
    }
};

/*
 * Export diagram in one of the selected formats
 */
DiagramSupport.prototype.exportElement = function(value)
{
    var _this = this;
    $.chainclude(
        {
            'lib/export.js':function(){
                exportElement(_this.completeName, "Diagram", {scale: _this.zoom});
            }
        }
    );
};

DiagramSupport.prototype.getDiagram = function()
{
    return this;
};

/*
 * Save diagram
 */
DiagramSupport.prototype.save = function(callback)
{
    var _this = this;
    _this.saveAs(_this.completeName, callback);
};

DiagramSupport.prototype.saveAs = function(newPath, callback)
{
    var _this = this;
    var params = { de: _this.completeName, newPath: newPath };
    queryBioUML("web/doc/save", params, function(data)
    {
        if(data.type == QUERY_TYPE_ADDITIONAL)//check if comment is necessary
        {
            createTextAreaDialog(resources.dlgEnterCommentTitle, resources.dlgEnterCommentName, function(comment)
            {
                params.comment = comment;
                queryBioUML("web/doc/save", params, function(data)
                {
                    if(_this.completeName == newPath)
                        _this.setChanged(false);
                    else
                    	_this.dataCollectionChanged();
                    if(callback) callback(data);
                }, function(data)
                {
                    if(callback) callback(data);
                }); 
            });
        } else
        {
            if(_this.completeName == newPath)
                _this.setChanged(false);
            else
            	_this.dataCollectionChanged();
            if(callback) callback(data);
        }
    }, function(data)
    {
        if(callback) callback(data);
    });
};

DiagramSupport.prototype.addDiagramElement = function(path, event)
{
    var _this = this;
    var x = event.pageX - _this.diagramContainerDiv.offset().left + _this.diagramContainerDiv.scrollLeft();  
    var y = event.pageY - _this.diagramContainerDiv.offset().top + _this.diagramContainerDiv.scrollTop();
    
    queryBioUML("web/diagram/add",
    {
        de: _this.completeName,
        e: getElementName(path),
        x: x/ this.zoom,
        y: y/ this.zoom,
        dc: getElementPath(path),
        resptype: _this.drawType
    }, function(data)
    {
        _this.update(data, true);
    });
};

DiagramSupport.prototype.checkToolbar = function()
{
    var dbName = getDataCollection(this.completeName).getDatabaseName();
    var _this = this;
    queryService("access.service", 23, {dc: dbName},
        function(data)
        {
            if((data.values == "1")||(data.values == "3"))
            {
                //hide toolbar for read only databases
                _this.showToolbar = false;
                _this.readOnly = true;
            }
        }, function() {});
};

/*
 *  Code to be called when diagram is loaded
 */
DiagramSupport.prototype.setLoadedListener = function(listener)
{
    this.loadedListener = listener;
};

/*
 * Action on mouse click(replaced by `selectControl` function). Should return `false` to replace default action.
 */
DiagramSupport.prototype.clickAction = function(event)
{
    return true;
};

/*
 * Called when diagram toolbar action selected.
 */
DiagramSupport.prototype.selectControl = function(clickAction)
{
    this.cancelEdgeEditing();
    this.clickAction = clickAction;
};

/*
 * Load toolbar buttons
 */
DiagramSupport.prototype.loadDiagramToolbar = function(toolbar)
{
    queryBioUML("web/action",
    {
        type: "diagram",
        de: this.completeName
    }, function(data)
    {
        var actions = data.values;
        var block = $('<div class="fg-buttonset fg-buttonset-single ui-helper-clearfix"></div>');
        for (i = 0; i < actions.length; i++)
        {
            var actionProperties = new Action();
            actionProperties.parse(actions[i]);
            if (actionProperties.isVisible(null) == true)
            {
                var action = createToolbarButton(actionProperties.label, actionProperties.icon, actionProperties.doAction);
                if(i == 0) action.addClass("ui-state-active");
                block.append(action);
            }
        }
        if(this.searchBox)
        {
            block.append(this.searchBox);
        }
        toolbar.prepend(block);
    }.bind(this), function() {});
};

DiagramSupport.prototype.switchToolbarButton = function(title)
{
    this.toolbar.children(".fg-buttonset").eq(0).children(".fg-button").removeClass("ui-state-active");
    $(".fg-button[title='"+title+"']", this.toolbar).addClass("ui-state-active");
};

DiagramSupport.prototype.cancelEdgeEditing = function()
{
    if(this.edgeCreation)
    {
        this.edgeCreation = undefined;
        this.edgeCreationView = undefined;
        this.selector.restoreSelection();
        this.viewPane.highlightView();
    }
};

DiagramSupport.prototype.setSelectMode = function()
{
    this.cancelEdgeEditing();
    this.switchToolbarButton("Select");
    this.selectControl(function(event)
    {
        return true;
    });
};

/*
 * Diagram model support
 */
DiagramSupport.prototype.getModelObjectName = function(callback)
{
    var hasModel = getDataCollection(this.completeName).diagramHasModel();
    var engine = null;
    if(hasModel)
        engine = "diagram/model/" + this.completeName;
    if (callback) 
    {
        callback(engine);
    }
    else
        return engine;
};

/*
 * Listener support
 */
DiagramSupport.prototype.addViewAreaListener = function(listener)
{
    var alreadyAdded = false;
    for (li = 0; li < this.viewAreaListeners.length; li++)
    {
        if (this.viewAreaListeners[li] == listener)
        {
            alreadyAdded = true;
            break;
        }
    }
    if (!alreadyAdded)
    {
        this.viewAreaListeners.push(listener);
    }
};

DiagramSupport.prototype.removeViewAreaListener = function(listener)
{
    for (li = 0; li < this.viewAreaListeners.length; li++)
    {
        if (this.viewAreaListeners[li] == listener)
        {
            this.viewAreaListeners.splice(li,1);
            break;
        }
    }
};

/**
 * Remove elements from diagram
 * @param {String[]} elementList - either String or Array of Strings containing element names 
 * @param {Function} callback - (optional) callback function to be called upon successful deletion
 */
DiagramSupport.prototype.removeElements = function(elementList, callback)
{
    if(this.readOnly)
    {
        if(callback) callback();
        return;
    }
    if(!(elementList instanceof Array)) elementList = [elementList];
    var _this = this;
    queryBioUML("web/diagram/remove",
    {
        de: this.completeName,
        e: $.toJSON(elementList),
        type: this.drawType
    }, function(data)
    {
        if(callback) callback();
        var dc = getDataCollection(_this.completeName);
        _.each(elementList, function(element)
        {
            var elName = getElementName(element);
            var elParentPath = getElementPath(element);
            if(elParentPath && elParentPath.length > 0)
            {
                var parentDc = getDataCollection(_this.completeName + '/' + elParentPath);
                parentDc.remove(elName);
            }
            else
                dc.remove(element);
        });
        showElementInfo(_this.completeName);
        //_this.update(data, true);
    });
};

/*
 * Get point coordinates on diagram
 */
DiagramSupport.prototype.getEventPoint = function (event)
{
    var parentX = this.diagramContainerDiv.offset().left;
    var parentY = this.diagramContainerDiv.offset().top;
    var x = event.originalEvent.clientX - parentX + this.diagramContainerDiv.scrollLeft();
    var y = event.originalEvent.clientY - parentY + this.diagramContainerDiv.scrollTop();
    var point = new Point( x / this.zoom, y / this.zoom);
    return point;
};

/*
 * Get diagram type name
 */
DiagramSupport.prototype.getDiagramType = function (callback)
{
    callback(getDataCollection(this.completeName).getDiagramType());
};

/*
 * Arterial tree check
 */
DiagramSupport.prototype.checkArterialTree = function (callback)
{
    var _this = this;
    if(_this.isArterialTree != undefined)
    {
        callback(_this.isArterialTree);
    }
    else
    {
        queryService("hemodynamics.service", 601, { de: _this.completeName },
        function(data)
        {
            _this.isArterialTree = true;
            callback(_this.isArterialTree);
        }, function(data)
        {
            _this.isArterialTree = false;
            callback(_this.isArterialTree);
        });
    };
}

DiagramSupport.prototype.checkAnnotation = function (callback)
{
    var _this = this;
    if(_this.isAnnotation != undefined)
    {
        callback(_this.isAnnotation);
    }
    else
    {
        queryService("genomeenhancer.annotation.service", 801, { de: _this.completeName },
        function()
        {
            _this.isAnnotation = true;
            callback(_this.isAnnotation);
        }, function()
        {
            _this.isAnnotation = false;
            callback(_this.isAnnotation);
        });
    };
}

DiagramSupport.prototype.checkPhysicell = function (callback)
{
    var _this = this;
    if(_this.isPhysicell != undefined)
    {
        callback(_this.isPhysicell);
    }
    else
    {
        var info = getDataCollection(this.completeName).getDiagramTypeInfo();
        _this.isPhysicell = instanceOf(info.modelClass,'biouml.plugins.physicell.MulticellEModel') ;
        callback(_this.isPhysicell);
    };
}

DiagramSupport.prototype.checkEModel = function (callback)
{
    var _this = this;
    if(_this.isEModel != undefined)
    {
        callback(_this.isEModel);
    }
    else
    {
        var info = getDataCollection(this.completeName).getDiagramTypeInfo();
        _this.isEModel = instanceOf(info.modelClass,'biouml.model.dynamics.EModel') ;
        callback(_this.isEModel);
    };
}

/*
 * Save active diagram every 20 sec if changed (now used for Annotation diagram)
 */
DiagramSupport.prototype.autoSave = function()
{
	var _this = this;
	clearTimeout(this.autoSaveTimer);
    this.autoSaveTimer = setTimeout(function()
    {
    	if(isActiveDocument(_this))
    	{
	    	if(_this.isChanged())
	    	{
	    		_this.save();
	    	}
	    	_this.autoSave();
    	}
    }, 20000);
};

/*
 * Get list of parameters for port creation
 */
DiagramSupport.prototype.getDiagramPortParameters = function (callback)
{
    var _this = this;
    queryBioUML("web/diagram/get_port_parameters", 
    {
        de: _this.completeName
    }, function(data)
    {
        callback(data.values);
    }, function(data)
    {
        callback("");
    });
};

/*
 * Method called as listener for diagram changes
 */
DiagramSupport.prototype.dataCollectionChanged = function()
{
    var _this = this;
    queryBioUML("web/diagram/refresh",
    {
        de: this.completeName,
        type: this.drawType
    }, function(data)
    {
        _this.update(data, true);
    });
};

DiagramSupport.prototype.refresh = function()
{
    getDataCollection(this.completeName).invalidateCollection();
    this.dataCollectionChanged();
};

/*
 * Method called as listener if diagram is removed from parent collection
 */
DiagramSupport.prototype.dataCollectionRemoved = function()
{
    closeDocument(this.tabId);
};

/*
 * Close handler
 */
DiagramSupport.prototype.close = function(callback)
{
    getDataCollection(this.completeName).removeChangeListener(this);
    getDataCollection(this.completeName).removeRemoveListener(this);
    //clearTimeout(this.autoSaveTimer);
    if(callback) callback();
};

DiagramSupport.prototype.setChanged = function(changed)
{
    if(this.changed != changed)
    {
        this.changed = changed;
    }
};

DiagramSupport.prototype.isChanged = function()
{
    return this.changed;
};

/*
 * Vertex management
 */
DiagramSupport.prototype.editVertex = function(edge, action, vertexNum, point)
{
    var _this = this;
    var params = {
        de: this.completeName,
        edge: edge,
        vertex_action: action,
        vertex_number : vertexNum,
        type: this.drawType
    };
    if(point)
    {
        params.x = Math.floor(point.x/_this.zoom);
        params.y = Math.floor(point.y/_this.zoom);
    }
    queryBioUML("web/diagram/vertex", params, function(data)
    {
        _this.edgeEditor.clear();
        _this.viewPane.highlightView();
        _this.update(data);
        _this.selectEdge(edge);
    });
};

DiagramSupport.prototype.setFixed = function(toFix, toSelect)
{
    var selection = toSelect != undefined ? toSelect : this.getSelection();
    if(selection && selection.length > 0)
    {
        var params = {
            de: this.completeName,
            type: this.drawType,
            e: $.toJSON(selection),
            fixed: toFix
        };
        var _this = this;
        queryBioUML("web/diagram/fix", params, function(data)
            {
                _this.update(data);
            });
    }
};

DiagramSupport.prototype.isResizable = function(nodes)
{
    var _thisDiagram = this;
    var unchecked = nodes.filter(n => _thisDiagram.resizable[getElementName(n)] == undefined);
    if(unchecked.length > 0){
        var params = {
            de: this.completeName,
            e: $.toJSON(unchecked)
        };
        var data = queryBioUML("web/diagram/check_resizable", params);
        for(var i=0; i< data.values.resizable.length; i++)
        {
            var name = data.values.resizable[i];
            _thisDiagram.resizable[name] = true;
        }
        if(data.values.resizable < unchecked.length)
        {
            nodes.forEach(n => { 
                var name = getElementName(n); 
                if( _thisDiagram.resizable[name]== undefined)
                    _thisDiagram.resizable[name] = false;
            });
            return false; //light optimization - find unresizable
        }
    }
    function isNotResizable(element, index, array) {
        return _thisDiagram.resizable[getElementName(element)] == false;
    }
    return !nodes.some(isNotResizable);
};

/*
 * New node dialog
 */
function NewNodeDialog(diagram, dcName, type, posX, posY)
{
    this.diagram = diagram;
    this.dcName = dcName;
    this.type = type;
    this.posX = posX;
    this.posY = posY;

    this.open = function()
    {
        this.dialogDiv = $('<div title="'+resources.dlgNewNodeTitle+'"></div>');
        var _thisDialog = this;

        var selectElement = $('<select></select>');
        this.dialogDiv.html(resources.commonLoading);
        var needDialog = true;
        if (this.dcName && this.dcName.length > 0)
        {
            if (dcName == "not available") 
            {
                var ind = type.lastIndexOf(".");
                var typeName = ind == -1? type: type.substring(ind+1);
                logger.error(resources.commonAddDiagramElementUnavailable.replace("{type}", typeName));
                needDialog = false;
            }
            else 
            {
                getDataCollection(this.dcName).getNameList(function(nameList)
                {
                    if (nameList != null) 
                    {
                        for (nli = 0; nli < nameList.length; nli++) 
                        {
                            selectElement.append($('<option value="' + nameList[nli].name + '">' + nameList[nli].name + '</option>'));
                        }
                        _thisDialog.dialogDiv.html(resources.dlgNewNodeSelectKernel + ": ");
                        _thisDialog.dialogDiv.append(selectElement);
                    }
                });
            }
        }
        else
        {
            var data = queryBioUML('web/diagram/type_structure',
                {
                    de: this.diagram.completeName,
                    x: this.posX,
                    y: this.posY,
                    type: this.type
                });
            var beanDPS = convertJSONToDPS(data.values);
            var properties = beanDPS.getAllProperties();
            var onlyReadOnly = true;
            for (var propName in properties)
            {
                var prop = properties[propName];
                if (!prop.getDescriptor().isReadOnly())
                {
                    onlyReadOnly = false;
                    break;
                }
            }

            if (onlyReadOnly == true)
            {
                needDialog = false;
                _thisDialog.addElementByProperties(beanDPS);
            }
            else
            {
                var parentID = "property_inspector_dialog_" + rnd();
                _thisDialog.dialogDiv.empty();
                _thisDialog.propertyInspector = $('<div id="' + parentID + '"></div>');
                _thisDialog.dialogDiv.append(_thisDialog.propertyInspector);
                $(document.body).append(_thisDialog.dialogDiv);
                _thisDialog.initPropertyInspector(beanDPS);
            }
        }

        if (needDialog == true)
        {
            var dialogButtons = {};
            dialogButtons[ "Ok" ] = function()
                    {
                        if (_thisDialog.dcName && _thisDialog.dcName.length > 0)
                        {
                            var newName = selectElement.children('option:selected').val();
                            _thisDialog.addElement(newName);
                        }
                        else
                        {
                            var dps = _thisDialog.propertyPane.getModel();
                            _thisDialog.addElementByProperties(dps);
                        }

                        $(this).dialog("close");
                        $(this).remove();
                    };
            dialogButtons[ resources.dlgButtonCancel ] = function()
                    {
                        $(this).dialog("close");
                        $(this).remove();
                        _thisDialog.diagram.setSelectMode();
                    };
            this.dialogDiv.dialog(
            {
                autoOpen: false,
                width: 520,
                buttons: dialogButtons
            });
            this.dialogDiv.dialog("open");
            addDialogKeys(this.dialogDiv);
            sortButtons(this.dialogDiv);
        } else
            _thisDialog.diagram.setSelectMode();

        this.diagram.selectControl(function(event)
        {
            return true;
        });
    };

    this.addElement = function(name)
    {
        var _thisDialog = this;
        queryBioUML("web/diagram/add",
        {
            de: this.diagram.completeName,
            e: name,
            x: this.posX,
            y: this.posY,
            type: this.type,
            dc: this.dcName,
            resptype: this.diagram.drawType
        }, function(data)
        {
            var _this = _thisDialog.diagram;
            _this.update(data, true);
            _this.setSelectMode();
        });
    };

    this.addElementByProperties = function(dps)
    {
        var _thisDialog = this;
        queryBioUML("web/diagram/add",
        {
            de: this.diagram.completeName,
            json: convertDPSToJSON(dps),
            x: this.posX,
            y: this.posY,
            type: this.type,
            dc: this.dcName,
            resptype: this.diagram.drawType
        }, function(data)
        {
            var _this = _thisDialog.diagram;
            _this.update(data, true);
            _this.setSelectMode();
        });
    };
    
    this.syncronizeData = function (control)
    {
        var _thisDialog = this;
        this.setProperties(function(data)
        {
            _thisDialog.propertyInspector.empty();
            var beanDPS = convertJSONToDPS(data.values);
            _thisDialog.initPropertyInspector(beanDPS);
        }, 
        control != undefined ? control.getModel().getName(): undefined);
    };
    
    this.setProperties = function(successCallback, control)
    {
        disableDPI(this.propertyInspector);
        this.propertyPane.updateModel();
        var json = convertDPSToJSON(this.propertyPane.getModel(), control);
        var requestParameters = {
                de: this.diagram.completeName,
                x: this.posX,
                y: this.posY,
                type: this.type,
                json: json
            };
        queryBioUML("web/diagram/type_structure", requestParameters, successCallback);    
    };
    
    this.initPropertyInspector = function(beanDPS)
    {
        var _thisDialog = this;
        this.propertyPane = new JSPropertyInspector();
        this.propertyPane.setParentNodeId(this.propertyInspector.attr('id'));
        this.propertyPane.setModel(beanDPS);
        this.propertyPane.generate();
        this.propertyPane.addChangeListener(function(control, oldValue, newValue) {
            _thisDialog.syncronizeData(control);
        });
    };
}

/*
 * New edge dialog
 */
function NewEdgeDialog(diagram, dcName, type)
{
    this.diagram = diagram;
    this.dcName = dcName;
    this.type = type;

    var _thisDialog = this;

    this.addElement = function(input, output, additionalParams)
    {
        var params = {
            de: this.diagram.completeName,
            input: input.replace(/\//g, "."),
            output: output.replace(/\//g, "."),
            type: this.type,
            dc: this.dcName,
            resptype: this.diagram.drawType
        };
        if(additionalParams != undefined)
        {
            params['additional'] = additionalParams;
        }
        queryBioUML("web/diagram/add", params, function(data)
        {
            if (data.type == 0)
            {
                var _this = _thisDialog.diagram;
                _this.update(data, true);
            }
            else
            {
                _thisDialog.openParameterConnection(input, output, data.values['input'], data.values['output'], data.values["connections"])
            }
        });
    };

    this.nodeSelected = function(nodePath)
    {
        if (this.inputPath.val().length == 0)
        {
            this.inputPath.val(nodePath);
        }
        else
        {
            this.outputPath.val(nodePath);
        }
    };
    
    this.openParameterConnection = function(from, to, fromParams, toParams, connections)
    {
        this.dialogDivPC = $('<div title="Establish parameters connections"></div>');
        
        var tableFromTo = $('<table class="clipboard"><tr id="headerRow"><th>From</th><th>To</th></tr></table>');
        tableFromTo.addClass('selectable_table');
        this.dialogDivPC.append(tableFromTo);
        this.dialogDivPC.append('<br/>');
        
        this.dialogDivPC.append($('<input type="button" value="Add"/>').click(function()
            {
                var fromVal = _thisDialog.fromSelect.val();
                var toVal = _thisDialog.toSelect.val();
                if (fromVal != undefined && toVal != undefined) 
                {
                    var tr = $('<tr><td class="from_value">' + fromVal + '</td><td class="to_value">' + toVal + '</td></tr>');
                    tableFromTo.append(tr);
                }
            })
        );
        
        this.dialogDivPC.append($('<input type="button" value="Remove"/>').click(function()
            {
                tableFromTo.find('.row_selected').remove();
            })
        );
        if(connections != undefined)
        {
            for(var i = 0; i < connections.length; i++)
            {
                var tr = $('<tr><td class="from_value">' + connections[i][0] + '</td><td class="to_value">' + connections[i][1] + '</td></tr>');
                tableFromTo.append(tr);
            }
        }
        
        this.dialogDivPC.append('<br/><br/>');
        
        var tableParams = $('<table class="clipboard"></table>');
        
        tableParams.append($('<tr id="headerRow"/>').append($('<th>'+from+' parameters</th>')).append($('<th>'+to+' parameters</th>')));
        
        this.fromSelect = $('<select></select>')
            .attr("id", "from_select")
            .attr("size", "6");
        for(var i = 0; i < fromParams.length; i++)
        {
            this.fromSelect.append($('<option/>').val(fromParams[i]).text(fromParams[i]));
        }
        this.toSelect = $('<select></select>')
            .attr("id", "to_select")
            .attr("size", "6");
        for(var i = 0; i < toParams.length; i++)
        {
            this.toSelect.append($('<option/>').val(toParams[i]).text(toParams[i]));
        }
        
        tableParams.append($('<tr/>')
                    .append($('<td/>').append(this.fromSelect))
                    .append($('<td/>').append(this.toSelect)));
        
        this.dialogDivPC.append(tableParams);
        
        var dialogButtons = {};
        dialogButtons[ "Ok" ] = function()
                 {
                    var connections = new Array();
                    tableFromTo.find('tr:not(#headerRow)').each(function(row)
                    {
                         var con = new Array();
                         con[0] = $(this).find(".from_value").html();
                         con[1] = $(this).find(".to_value").html();
                         connections.push(con);   
                    });
                    _thisDialog.addElement(from, to, $.toJSON(connections));
                    $(this).dialog("close");
                    $(this).remove();
                 };
        dialogButtons[ resources.dlgButtonCancel ] = function()
                {
                    $(this).dialog("close");
                    $(this).remove();
                };

        this.dialogDivPC.dialog(
        {
            autoOpen: false,
            width: 300,
            buttons: dialogButtons
        });
        this.dialogDivPC.dialog("open");
    };
}

/*
 * Port creation dialog
 */
function NewPortDialog(diagram, dcName, type, posX, posY)
{
    this.diagram = diagram;
    this.dcName = dcName;
    this.type = type;
    this.posX = posX;
    this.posY = posY;

    var _thisDialog = this;

    this.open = function()
    {
        this.dialogDiv = $('<div title="'+resources.dlgNewPortTitle+'"></div>');
        
        var selectElement = $('<select></select>');
        this.dialogDiv.html(resources.commonLoading);
        this.diagram.getDiagramPortParameters(function(nameList)
            {
                if (nameList != null)
                {
                    for (nli = 0; nli < nameList.length; nli++)
                    {
                        selectElement.append($('<option value="' + nameList[nli] + '">' + nameList[nli] + '</option>'));
                    }
                    _thisDialog.dialogDiv.html(resources.dlgNewPortPrompt+": ");
                    _thisDialog.dialogDiv.append(selectElement);
                }
            });
            
        var dialogButtons = {};
        dialogButtons[ "Ok" ] = function()
                {
                    var portElement = selectElement.children('option:selected').val();
                    _thisDialog.addPort(portElement);
                    $(this).dialog("close");
                    $(this).remove();
                };
        dialogButtons[ resources.dlgButtonCancel ] = function()
                {
                    $(this).dialog("close");
                    $(this).remove();
                };
        this.dialogDiv.dialog(
        {
            autoOpen: false,
            width: 300,
            buttons: dialogButtons
        });
        this.dialogDiv.dialog("open");
        addDialogKeys(this.dialogDiv);
        sortButtons(this.dialogDiv);
    };
    
    this.addPort = function(name)
    {
        var _thisDialog = this;
        queryBioUML("web/diagram/add",
        {
            de: this.diagram.completeName,
            e: name,
            x: this.posX,
            y: this.posY,
            type: this.type,
            dc: this.dcName,
            resptype: this.diagram.drawType
        }, function(data)
        {
            var _this = _thisDialog.diagram;
            _this.update(data, true);
        });
    };
    
    this.diagram.selectControl(function(event)
    {
        return true;
    });
}



/*
 * New reaction dialog
 */
function NewReactionDialog(diagram, posX, posY)
{
    this.diagram = diagram;
    this.posX = posX;
    this.posY = posY;
    
    this.added = new Array();

    var _thisDialog = this;

    this.open = function()
    {
        this.dialogDiv = $('<div title="New reaction"></div>');
        
        this.reactionName = $('<input type="text" disabled value="...loading..."/>');
        this.dialogDiv.append('<b>Reaction name: </b>');
        this.dialogDiv.append(this.reactionName);
        
        this.reactionTitle = $('<input type="text" size="50"/>');
        this.dialogDiv.append('<br/><b>Reaction title: </b>');
        this.dialogDiv.append(this.reactionTitle);
        
        queryBioUML("web/diagram/new_reaction_name", {de:_thisDialog.diagram.completeName}, 
    		function(data)
		    {
    			_thisDialog.reactionName.val(data.values.name);
    			if(data.values.readOnly)
    				_thisDialog.reactionName.attr('readonly', true);
    			else
    				_thisDialog.reactionName.prop("disabled", false);
		    });
        
        this.inputName = $('<input type="text" disabled/>');
        this.inputVariable = $('<input type="text" disabled/>');
        this.hiddenTitle = $('<input type="hidden"/>');

        this.dialogDiv.append('<br/><br/><b>Add/remove reaction component:</b><br/>Component: ');
        this.dialogDiv.append(this.inputName);
        this.dialogDiv.append("&nbsp; Variable name: ");
        this.dialogDiv.append(this.inputVariable);
        this.roleSelect = $('<select/>')
            .append($('<option/>').text("reactant").val("reactant"))
            .append($('<option/>').text("product").val("product"))
            .append($('<option/>').text("modifier").val("modifier"));
        this.dialogDiv.append('&nbsp; Role: ');            
        this.dialogDiv.append(this.roleSelect);
        
        this.dialogDiv.append(this.hiddenTitle);
        
        this.tableComponents = $('<table class="clipboard selectable_table"></table>');
        
        this.tableComponents.append($('<tr id="headerRow"/>')
            .append($('<th>Identifier</th>'))
            .append($('<th>Variable</th>'))
            .append($('<th>Role</th>'))
            .append($('<th>Stoichiometry</th>'))
            .append($('<th>Modifier action</th>'))
            .append($('<th>Participation</th>'))
            .append($('<th>Title</th>'))
            .append($('<th>Comment</th>')));
        
        this.dialogDiv.append('<br/><br/>');
        this.dialogDiv.append($('<input type="button" value="Add"/>').click(function()
        {
            queryBioUML("web/diagram/accept_for_reaction", 
                    {
                        de:_thisDialog.diagram.completeName,
                        node:getElementName(BioUML.selection.lastSelected) 
                    },
                    function(data)
                    {
                    if(_thisDialog.inputName.val())
                    {
                        var name = _thisDialog.inputName.val();
                        var role = _thisDialog.roleSelect.val();
                        var key = name + role
                        if(_thisDialog.added[key])
                        {
                            logger.error(resources.dlgCreateReactionComponentAlreadyExist.replace("{name}", name).replace("{role}", role));
                            return;
                        }
                        _thisDialog.added[key] = 1;
                        
                        var modifierSelector = $('<select class="component_modifier"/>')
                                .append($('<option/>').text("catalyst").val("catalyst"))
                                .append($('<option/>').text("inhibitor").val("inhibitor"))
                                .append($('<option/>').text("switch on").val("switch on"))
                                .append($('<option/>').text("switch off").val("switch off"));
                        
                        var roleSelect = $('<select class="component_role"/>')
                        .append($('<option/>').text("reactant").val("reactant"))
                        .append($('<option/>').text("product").val("product"))
                        .append($('<option/>').text("modifier").val("modifier"));
                        
                        roleSelect.bind('focusin', function(){
                            //console.log("Saving value " + $(this).val());
                            $(this).data('val', $(this).val());
                        });
                        roleSelect.bind('change', function(){
                        	var prevRole = $(this).data('val');
                        	var newRole = this.value;
                        	var row = $(this).closest("tr");
                        	var titleCtrl = row.find(".component_title");
                        	
                        	var name = titleCtrl.attr("hidden-title");
                        	if(_thisDialog.added[name+newRole])
                    		{
                        		logger.error(resources.dlgCreateReactionComponentAlreadyExist.replace("{name}", name).replace("{role}", newRole));
                        		this.value = prevRole;
                        		return;
                        	}
                        	else
                    		{
                        		delete _thisDialog.added[name + prev];
                        		_thisDialog.added[name+this.value] = 1;
                        	}
                        	titleCtrl.val(_thisDialog.getSpecieReferenceName(name, this.value));
                        	
                        	if(this.value == "modifier")
                        		row.find(".component_modifier_cell").empty().append(modifierSelector);
                        	else
                        		row.find(".component_modifier_cell").empty();
                        	
                        	_thisDialog.reactionTitle.val(_thisDialog.getReactionTitle());
                        });
                        roleSelect.val(_thisDialog.roleSelect.val());
                        
                        var particip = $('<select class="component_particip"/>')
                        .append($('<option/>').text("direct").val("direct"))
                        .append($('<option/>').text("indirect").val("indirect"))
                        .append($('<option/>').text("unknown").val("unknown"));
                        
                        var modifier = "";
                        if(role == "modifier")
                        {
                            modifier = modifierSelector;
                        }
                        var title = _thisDialog.getSpecieReferenceName(_thisDialog.hiddenTitle.val(), role);
                        var tr = $('<tr/>')
                            .append($('<td class="component_id">'+_thisDialog.inputName.val()+'</td>'))
                            .append($('<td class="component_name">'+_thisDialog.inputVariable.val()+'</td>'))
                            .append($('<td/>').append(roleSelect))
                            .append($('<td>').append($('<input type="text" class="component_stoich" size="13"/>').val("1")))
                            .append($('<td class="component_modifier_cell"/>').append(modifier))
                            .append($('<td/>').append(particip))
                            .append($('<td/>').append($('<input type="text" class="component_title"/>').val(title).attr("hidden-title", _thisDialog.hiddenTitle.val())))
                            .append($('<td/>').append($('<input type="text" class="component_comment"/>').val("")));
                        
                        _thisDialog.tableComponents.append(tr);
                        _thisDialog.reactionTitle.val(_thisDialog.getReactionTitle());
                    }
                },
                function (error)
                {
                    logger.error(error.message);
                });
            
            
        }));
        
        this.reactionName.bind('change', function()
    		{
        		var newName = _thisDialog.reactionName.val();
        		_thisDialog.reactionName.prop("disabled", true).val("...loading...");
	        	queryBioUML("web/diagram/validate_reaction_name", {name:newName, de:_thisDialog.diagram.completeName}, 
	    		function(data)
			    {
	        		if(data.values)
	    			{
	        			_thisDialog.reactionName.prop("disabled", false).val(data.values);
	        			_thisDialog.tableComponents.find('tr:not(#headerRow)').each(function(index, row){
	                		var rowObj = $(row);
	            		    var titleCtrl = rowObj.find(".component_title");
	            		    if(titleCtrl.length)
	            		    	titleCtrl.val(_thisDialog.getSpecieReferenceName(titleCtrl.attr("hidden-title"), rowObj.find(".component_role").val()));
	                	});
	    			}
			    });
        });
        
        this.dialogDiv.append($('<input type="button" value="Remove"/>').click(function()
            {
                var row = _thisDialog.tableComponents.find('.row_selected');
                var key = row.find('td.component_id').text() + row.find('td.component_role').text();
                _thisDialog.added[key] = undefined;
                row.remove();
                _thisDialog.reactionTitle.val(_thisDialog.getReactionTitle());
                
            })
        );
        
        this.dialogDiv.append('<br/><br/><b>Reaction components:</b>');
        this.dialogDiv.append(this.tableComponents);
        
        this.dialogDiv.append('<br/><b>Formula:</b><br/>');
        this.formula = $('<input type="text"/>').css('width', '80%');
        this.dialogDiv.append(this.formula);
        
        this.diagram.addSelectListener(this);
        var dialogButtons = {};
        dialogButtons[ "Ok" ] = function()
                {
                    var components = new Array();
                    var addedComponents = {};
                    _thisDialog.tableComponents.find('tr:not(#headerRow)').each(function(row)
                    {
                    	var row = $(this);
                        var component = {};
                        component['id'] = row.find('td.component_id').text();
                        component['role'] = row.find('.component_role').val();
                        var key = component['id']  + component['role']
                        if(addedComponents[key] != undefined)
                        {
                            logger.error("Reaction contains duplicated component "+component['id']+" with role '" + component['role'] + "'. "+
                                            "<br>You can use stoichiometric coefficient to indicate " +
                                            "<br>how much molecules of the same specie is involved in reation.");
                            return;
                        }
                        
                        component['stoichiometry'] = row.find('.component_stoich').val();
                        component['participation'] = row.find('.component_particip').val();
                        component['title'] = row.find('.component_title').val();
                        component['comment'] = row.find('.component_comment').val();
                        var modifier = row.find('.component_modifier');
                        if(modifier)
                            component['modifier'] = modifier.val();
                        components.push(component);
                        addedComponents[key] = 1;
                    });
                    
                    var formula = _thisDialog.formula.val();
                    var reactionName = _thisDialog.reactionName.val();
                    var title = _thisDialog.reactionTitle.val();
                    _thisDialog.addElement(reactionName, $.toJSON(components), formula, title);
                	
                    _thisDialog.diagram.removeSelectListener(_thisDialog);
                    $(this).dialog("close");
                    $(this).remove();
                };
        dialogButtons[ resources.dlgButtonCancel ] = function()
                {
                    _thisDialog.diagram.removeSelectListener(_thisDialog);
                    $(this).dialog("close");
                    $(this).remove();
                };
        this.dialogDiv.dialog(
        {
            autoOpen: false,
            width: 750,
            buttons: dialogButtons
        });
        this.dialogDiv.dialog("open");
        addDialogKeys(this.dialogDiv);
        sortButtons(this.dialogDiv);

        this.diagram.selectControl(function(event)
        {
            return true;
        });
    };
    
    this.getSpecieReferenceName = function (specieName, role)
    {
    	return this.reactionName.val() + "__" + specieName + "_as_" + role;
    };
    
    this.getReactionTitle = function ()
    {
    	var reactants = [];
    	var products = [];
    	var other = [];
    	_thisDialog.tableComponents.find('tr:not(#headerRow)').each(function(index, row){
    		var rowObj = $(row);
    		var role = rowObj.find(".component_role").val();
    		var name = rowObj.find('td.component_id').text();
    		if(role == "reactant")
    			reactants.push(name);
    		else if (role == "product")
    			products.push(name);
    		else
    			other.push(name);
    	});
    	var result = "";
    	if( reactants.length > 0 )
        {
            result += reactants.join("+") + " ";
        }
        if( other.length == 0 )
            result += "->";
        else
        {
            result += "-";
            result += other.join(",");
            result += "->";
        }
        if( products.length > 0 )
        {
            result += " ";
            result+= products.join("+");
        }
        return result;
    };

    this.addElement = function(name, components, formula, title)
    {
        var params = {
            de: this.diagram.completeName,
            x: this.posX,
            y: this.posY,
            name : name,
            components: components,
            title: title,
            resptype: this.diagram.drawType
        };
        
        if(formula)
        {
            params['formula'] = formula;
        }
        
        queryBioUML("web/diagram/add", params, function(data)
        {
            _thisDialog.diagram.update(data, true);
        });
    };

    this.nodeSelected = function(nodePath)
    {
       var nodeDC = new DataCollection(this.diagram.completeName + "/" + nodePath);
       nodeDC.getBeanFields('title;role/name', function(result)
        {
            _thisDialog.inputName.val(nodePath);
            var role = result.getValue('role');
            if (role) 
            {
                _thisDialog.inputVariable.val(role.getValue('name'));
            }
            else
            {
                _thisDialog.inputVariable.val("");
            }
            _thisDialog.hiddenTitle.val(result.getValue('title'));
        });
    };
}

/*
 * Diagram element selector class
 */
function Selector(parent, diagram)
{
    this.diagram = diagram;
    this.selectorDiv = $("<div class='selector node_selector' tabindex='0'></div>").css('position', 'absolute');
    this.selectorDivDotes = $("<div class='selector_dotes'></div>").css({position: 'absolute', left: 0, top: 0, 
        opacity: .4, border: "1px dotted blue"});
    this.selectorDiv.append(this.selectorDivDotes);
    this.nodes = [];
    this.views = [];
    this.startRect = null;
    
    var _thisSelector = this;
    var selectorPen = new Pen(new Color(0x0,0x0,0x80,0xC0),2,[3,3]);

    var transform = _.bind(function()
    {
        // Move only currently
        if(this.diagram.readOnly)
        {
            this.diagram.restoreSelection();
            return;
        }
        var sx = this.rect.width/this.startRect.width;
        var sy = this.rect.height/this.startRect.height;
        var deltaX = Math.floor((this.rect.x/sx - this.startRect.x) / this.diagram.zoom);
        var deltaY = Math.floor((this.rect.y/sy - this.startRect.y) / this.diagram.zoom);
        if(deltaX == 0 && deltaY == 0 && sx == 1 && sy == 1)
        {
            this.diagram.restoreSelection();
            return;
        }

        queryBioUML("web/diagram/move",
        {
            de: this.diagram.completeName,
            type: this.diagram.drawType,
            e: $.toJSON(this.nodes),
            x: deltaX,
            y: deltaY,
            sx: sx,
            sy: sy
        }, 
        function(data)
        {
            var isMoved = !( data.values.view["class"] == "DummyView" );
            _thisSelector.diagram.update(data, isMoved);
            if( !isMoved )
                _thisSelector.selectorDiv.animate(  _thisSelector.selectorDiv.data().origPosition, "slow" );
            //DataElement move to category in annotation diagram is actually clone, selectorDiv should be hidden in this case
            //restoreSelection will hide selectorDiv in a new place of DataElement and leave selectorDiv for other moved nodes
            if(isMoved && _thisSelector.diagram.isAnnotation)
                _thisSelector.restoreSelection();
        },
        function(data)
        {
            _thisSelector.restoreSelection();
            logger.error(data.message);
        });
    }, this);

    var setRect = _.bind(function(x, y, width, height)
    {
        x = Math.round(x);
        y = Math.round(y);
        width = Math.round(width);
        height = Math.round(height);

        this.rect = new Rectangle(x, y, width, height);
        this.selectorDiv.css({left: x - 2, top: y - 2, width: width + 4, height: height + 4, outline: 'none'});
        this.selectorDivDotes.css({width: width + 2, height: height + 2});
        this.top_left.css({left: 0, top: 0});
        this.top_right.css({left: width, top: 0});
        this.bottom_left.css(({left: 0, top: height}));
        this.bottom_right.css(({left: width, top: height}));
        this.top.css({left: width/2, top: 0});
        this.bottom.css({left: width/2, top: height});
        this.left.css({left: 0, top: height / 2});
        this.right.css({left: width, top: height / 2});
        if(this.canvas)
            this.canvas.css({width: width, height: height});
    }, this);
    
    //init selectors
    var dragProperties =
    {
        start: function(event, ui)
        {
            $(this).hide();
            _thisSelector.startRect = _thisSelector.rect;
            _thisSelector.lastX = event.originalEvent.clientX + _thisSelector.diagram.diagramContainerDiv.scrollLeft();
            _thisSelector.lastY = event.originalEvent.clientY + _thisSelector.diagram.diagramContainerDiv.scrollTop();
        },
        drag: function(event, ui)
        {
            var deltaX = event.originalEvent.clientX + _thisSelector.diagram.diagramContainerDiv.scrollLeft() - _thisSelector.lastX;
            var deltaY = event.originalEvent.clientY + _thisSelector.diagram.diagramContainerDiv.scrollTop() - _thisSelector.lastY;
            _thisSelector.lastX = event.originalEvent.clientX + _thisSelector.diagram.diagramContainerDiv.scrollLeft();
            _thisSelector.lastY = event.originalEvent.clientY + _thisSelector.diagram.diagramContainerDiv.scrollTop();
            var parent = $(this).parent();
            var x, y, w, h;
            var d = 2;//border size
            switch ($(this).attr('id'))
            {
                case 'top_left':
                    x = parent.offset().left + deltaX + d;
                    y = parent.offset().top + deltaY + d;
                    w = parent.width() - deltaX - 2 * d;
                    h = parent.height() - deltaY - 2 * d;
                    break;
                case 'top_right':
                    x = parent.offset().left + d;
                    y = parent.offset().top + deltaY + d;
                    w = parent.width() + deltaX - 2 * d;
                    h = parent.height() - deltaY - 2 * d;
                    break;
                case 'bottom_left':
                    x = parent.offset().left + deltaX + d;
                    y = parent.offset().top + d;
                    w = parent.width() - deltaX - 2 * d;
                    h = parent.height() + deltaY - 2 * d;
                    break;
                case 'bottom_right':
                    x = parent.offset().left + d;
                    y = parent.offset().top + d;
                    w = parent.width() + deltaX - 2 * d;
                    h = parent.height() + deltaY - 2 * d;
                    break;
                case 'top':
                    x = parent.offset().left + d;
                    y = parent.offset().top + deltaY + d;
                    w = parent.width() - 2 * d;
                    h = parent.height() - deltaY - 2 * d;
                    break;
                case 'bottom':
                    x = parent.offset().left + d;
                    y = parent.offset().top + d;
                    w = parent.width() - 2 * d;
                    h = parent.height() + deltaY - 2 * d;
                    break;
                case 'left':
                    x = parent.offset().left + deltaX + d;
                    y = parent.offset().top + d;
                    w = parent.width() - deltaX - 2 * d;
                    h = parent.height() - 2 * d;
                    break;
                case 'right':
                    x = parent.offset().left + d;
                    y = parent.offset().top + d;
                    w = parent.width() + deltaX - 2 * d;
                    h = parent.height() - 2 * d;
                    break;
            }
            if (w > 4 && h > 4)
            {
                var parentX = _thisSelector.diagram.diagramContainerDiv.offset().left - _thisSelector.diagram.diagramContainerDiv.scrollLeft();
                var parentY = _thisSelector.diagram.diagramContainerDiv.offset().top - _thisSelector.diagram.diagramContainerDiv.scrollTop();
                setRect(x - parentX - 1, y - parentY - 1, w, h);
            }
        },
        stop: function(event, ui)
        {
            var e = $(this);
            e.show();
            var parent = e.parent();
            switch (e.attr('id'))
            {
                case 'top_left':
                    e.css({left: 0, top: 0});
                    break;
                case 'top_right':
                    e.css({left: parent.width() - 4, top: 0});
                    break;
                case 'bottom_left':
                    e.css({left: 0, top: parent.height() - 4});
                    break;
                case 'bottom_right':
                    e.css({left: parent.width() - 4, top: parent.height() - 4});
                    break;
                case 'top':
                    e.css({left: parent.width() / 2 - 2, top: 0});
                    break;
                case 'bottom':
                    e.css({left: parent.width() / 2 - 2, top: parent.height() - 4});
                    break;
                case 'left':
                    e.css({left: 0, top: parent.height() / 2 - 2});
                    break;
                case 'right':
                    e.css({left: parent.width() - 4, top: parent.height() / 2 - 2});
                    break;
            }
            transform();
        }
    };
    this.top_left = $("<div id='top_left' class='selector_control'/>").css('cursor', 'nw-resize');
    this.top_right = $("<div id='top_right' class='selector_control'/>").css('cursor', 'ne-resize');
    this.bottom_left = $("<div id='bottom_left' class='selector_control'/>").css('cursor', 'sw-resize');
    this.bottom_right = $("<div id='bottom_right' class='selector_control'/>").css('cursor', 'se-resize');
    this.top = $("<div id='top' class='selector_control'/>").css('cursor', 'n-resize');
    this.bottom = $("<div id='bottom' class='selector_control'/>").css('cursor', 's-resize');
    this.left = $("<div id='left' class='selector_control'/>").css('cursor', 'w-resize');
    this.right = $("<div id='right' class='selector_control'/>").css('cursor', 'e-resize');
    this.selectorDiv.append(this.top_left).append(this.top_right).append(this.bottom_left).append(this.bottom_right)
        .append(this.top).append(this.bottom).append(this.left).append(this.right);
    this.selectorOutline = this.selectorDiv.children(".selector_control, .selector_dotes");

    this.selectorDiv.children('.selector_control').css('position', 'absolute').draggable(dragProperties).mousedown(function(event)
    {
        _thisSelector.selectorDiv.draggable('disable');
    });

    parent.mouseup(function(event)
    {
        _thisSelector.selectorDiv.draggable('enable');
    });
    parent.mouseout(function(event)
    {
        _thisSelector.selectorDiv.draggable('enable');
    });

    //compartment selection support
    this.selectorDiv.mousedown(function(event)
    {
        _thisSelector.mouseDown = {
            x : event.originalEvent.clientX,
            y : event.originalEvent.clientY
        };
    });
    
    this.selectorDiv.on( "keydown", function(ev) {
    	var deltaX = 0, deltaY = 0 ;
    	if(ev.which == 37 && ev.ctrlKey == true) //left
		{
    		deltaX = -1;
		}
    	if(ev.which == 38 && ev.ctrlKey == true) //up
		{
    		deltaY = -1;
		}
    	if(ev.which == 39 && ev.ctrlKey == true) //right
		{
    		deltaX = 1;
		}
    	if(ev.which == 40 && ev.ctrlKey == true) //down
		{
    		deltaY = 1;
		}
    	if(ev.which == 37 && ev.shiftKey == true) //left
		{
    		deltaX = -5;
		}
    	if(ev.which == 38 && ev.shiftKey == true) //up
		{
    		deltaY = -5;
		}
    	if(ev.which == 39 && ev.shiftKey == true) //right
		{
    		deltaX = 5;
		}
    	if(ev.which == 40 && ev.shiftKey == true) //down
		{
    		deltaY = -5;
		}
    	if(deltaX != 0 || deltaY != 0)
		{
    		_thisSelector.startRect = _thisSelector.rect;
            _thisSelector.rect = new Rectangle(parseInt(_thisSelector.selectorDiv.css("left")) + 2 + deltaX, 
                    parseInt(_thisSelector.selectorDiv.css("top")) + 2 + deltaY, _thisSelector.startRect.width, _thisSelector.startRect.height);
            transform();
		}
	});
    
    
    parent.mouseup(function(event)
    {
        if (_thisSelector.mouseDown)
        {
            if ((_thisSelector.mouseDown.x - event.originalEvent.clientX == 0) && (_thisSelector.mouseDown.y - event.originalEvent.clientY == 0))
            {
                var parentX = _thisSelector.diagram.diagramContainerDiv.offset().left;
                var parentY = _thisSelector.diagram.diagramContainerDiv.offset().top;
                var x = event.originalEvent.clientX - parentX + _thisSelector.diagram.diagramContainerDiv.scrollLeft();
                var y = event.originalEvent.clientY - parentY + _thisSelector.diagram.diagramContainerDiv.scrollTop();
                var newEvent = jQuery.Event("mousedown");
                newEvent.originalEvent = event.originalEvent;
                _thisSelector.diagram.diagramContainerDiv.trigger(newEvent);
            }
            _thisSelector.mouseDown = null;
        }
    });

    this.selectorDiv.draggable(
    {
        zIndex: 1000,
        containment: 'parent',
        cursor: 'move',
        start: function(event, ui)
        {
            if(_thisSelector.diagram.edgeCreation)
                return false;
            _thisSelector.startRect = _thisSelector.rect;
            _thisSelector.selectorDiv.data( "origPosition", {
                    "top" : _thisSelector.selectorDiv.css("top"),
                    "left" : _thisSelector.selectorDiv.css("left")
                });
        },
        stop: function(event, ui)
        {
            var offset = $(this).offset();
            _thisSelector.rect = new Rectangle(parseInt(_thisSelector.selectorDiv.css("left")) + 2, 
                    parseInt(_thisSelector.selectorDiv.css("top")) + 2,
                    _thisSelector.startRect.width, _thisSelector.startRect.height);
            transform();
        }
    });

    parent.append(this.selectorDiv);
    
    var repaint = _.bind(function()
    {
        if(this.nodes.length == 0)
        {
            this.selectorDiv.hide();
            if(this.canvas)
                this.canvas.remove();
            return;
        }
        var compositeView = new CompositeView();
        for(var i=0; i<this.views.length; i++)
        {
            compositeView.add(this.views[i]);
            var bounds = this.views[i].getBounds();
            bounds = new Rectangle(Math.floor(bounds.x), Math.floor(bounds.y), Math.ceil(bounds.width), Math.ceil(bounds.height));
            compositeView.add(new BoxView(selectorPen, null, bounds));
        }
        var rect = compositeView.getBounds();
        if(this.canvas)
            this.canvas.remove();
        this.canvas = $("<canvas/>").css({position: "absolute", left: 2, top: 2, opacity: 0.4})
            .attr({width: Math.round(rect.width), height: Math.round(rect.height)}).prependTo(this.selectorDiv);
        setRect(rect.x*diagram.zoom, rect.y*diagram.zoom, rect.width*diagram.zoom, rect.height*diagram.zoom);
        var ctx = this.canvas.get(0).getContext("2d");
        ctx.translate(-Math.round(rect.x), -Math.round(rect.y));
        compositeView.onLoad(function() {compositeView.paint(ctx, rect);});
        if(this.diagram.edgeCreation)
        {
            this.selectorOutline.hide();
        }
        else
        {
            this.selectorOutline.show();
            if(!this.isResizable())
                this.selectorDiv.children('.selector_control').hide();
        }
        this.selectorDiv.show();
        this.selectorDiv.focus();
    }, this);
    
    this.isEmpty = function()
    {
        return this.nodes.length == 0;
    }
    
    this.getSelectedNodes = function()
    {
        return this.nodes;
    }
    
    this.isResizable = function()
    {
        return this.nodes.length >0 && this.diagram.isResizable(this.nodes);
    }
    
    this.selectView = function(view)
    {
        var node = diagram.getRelativeNodeName(view.model);
        if(this.nodes.length == 1 && this.nodes[0] == node)
            return;
        this.nodes = [node];
        this.views = [diagram.viewPane.view.findViewByModel(view.model)];
        repaint();
        this.diagram.updateSelection(this.nodes);
        this.diagram.fireSelectNode(node);
        this.diagram.fireSelectionChanged([node]);
    };
    
    this.selectViews = function(views)
    {
        if(views.length == 1)
        {
            this.selectView(views[0]);
            return;
        }
        this.nodes = [];
        this.views = [];
        for(var i=0; i<views.length; i++)
        {
            var view = views[i];
            this.nodes.push(diagram.getRelativeNodeName(view.model));
            this.views.push(diagram.viewPane.view.findViewByModel(view.model));
        }
        repaint();
        this.diagram.updateSelection(this.nodes);
        this.diagram.fireSelectionChanged(this.nodes);
    };
    
    this.scrollToSelection = function()
    {
        if(this.nodes.length > 0)
            this.diagram.viewPane.ensureVisible(this.rect.getCenterX(), this.rect.getCenterY());
    };
    
    this.restoreSelection = function()
    {
        var oldNodes = this.nodes;
        this.views = [];
        this.nodes = [];
        for(var i=0; i<oldNodes.length; i++)
        {
            var model = diagram.completeName + "/" + oldNodes[i];
            var view = diagram.viewPane.view.findViewByModel(model);
            if(view)
            {
                this.views.push(view);
                this.nodes.push(oldNodes[i]);
            }
        }
        repaint();
        this.diagram.updateSelection(this.nodes);
        this.diagram.fireSelectionChanged(this.nodes);
    };
    
    this.toggleView = function(view)
    {
        var node = diagram.getRelativeNodeName(view.model);
        var removed = false;
        for(var i=0; i<this.nodes.length; i++)
            if(this.nodes[i] == node)
            {
                this.nodes.splice(i, 1);
                this.views.splice(i, 1);
                removed = true;
            }
        if(!removed)
        {
            this.nodes.push(node);
            this.views.push(diagram.viewPane.view.findViewByModel(view.model));
        }
        repaint();
        this.diagram.updateSelection(this.nodes);
        this.diagram.fireSelectionChanged(this.nodes);
    };
    
    this.addView = function(view)
    {
        var node = diagram.getRelativeNodeName(view.model);
        for(var i=0; i<this.nodes.length; i++)
            if(this.nodes[i] == node)
                return;
        this.nodes.push(node);
        this.views.push(diagram.viewPane.view.findViewByModel(view.model));
        repaint();
        this.diagram.updateSelection(this.nodes);
        this.diagram.fireSelectionChanged(this.nodes);
    };
    
    this.clear = function()
    {
        this.nodes = [];
        this.views = [];
        repaint();
        this.diagram.updateSelection();
        this.diagram.fireSelectionChanged([]);
    };
    
    this.initContextMenu = function()
    {
    };
}

function EdgeEditor(parent, diagram)
{
    this.isEdgeEditor = true;
    this.diagram = diagram;
    this.scaleX = diagram.zoom;
    this.scaleY = diagram.zoom;
    this.clickPoint = null;
    this.selectorDiv = $("<div class='selector edge_selector'/>").css('position', 'absolute');
    parent.append(this.selectorDiv);
    
    var _thisSelector = this;
    
    this.dragProperties = 
    {
        stop: function(event, ui)
        {
            if(_thisSelector.diagram.readOnly)
            {
                return;
            }
            _thisSelector.clickPoint = _thisSelector.getRelativeClickPoint(event);
            _thisSelector.editVertex(event.target.id, "move", _thisSelector.clickPoint);
        }
    };
    
    this.initSelectorDotes = function ()
    {
        this.selectorDiv.children().remove();
        for(var i = 0; i < _thisSelector.path.npoints; i++)
        {
            var vertexDiv = $("<div class='path_vertex' id='path_vertex_"+i+"'></div>").css('left', Math.round(_thisSelector.path.xpoints[i]*_thisSelector.scaleX - _thisSelector.rect.x*_thisSelector.scaleX)).css('top', Math.round(_thisSelector.path.ypoints[i]*_thisSelector.scaleY - _thisSelector.rect.y*_thisSelector.scaleY));
            if(i>=0 && i <= _thisSelector.path.npoints-1)
            {
                vertexDiv.addClass('editable');
            }
            _thisSelector.selectorDiv.append(vertexDiv);
        }
        var vertexXSize = 8, vertexYSize = 8;
        _thisSelector.selectorDiv.children('.path_vertex').css('width', vertexXSize).css('height', vertexYSize);
        _thisSelector.selectorDiv.children('.editable').css('position', 'absolute').draggable(_thisSelector.dragProperties);
    };
    
    this.addContextMenu = function()
    {
    };
    
    this.editVertex = function(id, action, point)
    {
        var ind = /path_vertex_(\d+)$/.exec(id);
        if (ind && ind.length == 2) 
        {
            _thisSelector.diagram.editVertex(_thisSelector.node, action, ind[1], point);
        }
    };
    
    this.clear = function()
    {
        this.selectorDiv.hide();
        _thisSelector.diagram.viewPane.deselectView();
    };
    
    this.isActive = function()
    {
        return this.selectorDiv.is(":visible");
    };
    
    this.getRelativeClickPoint = function(event)
    {
        return new Point(event.originalEvent.clientX + _thisSelector.diagram.diagramContainerDiv.scrollLeft() - _thisSelector.diagram.diagramContainerDiv.offset().left, event.originalEvent.clientY + _thisSelector.diagram.diagramContainerDiv.scrollTop() - _thisSelector.diagram.diagramContainerDiv.offset().top);
    };
    
    this.setRect = function(rect)
    {
        this.rect = rect;
        this.scaleX = diagram.zoom;
        this.scaleY = diagram.zoom;
        this.selectorDiv.css('left', Math.round((rect.x - 4)*this.scaleX)).css('top', Math.round((rect.y-4)*this.scaleY)).css('width', Math.round((rect.width + 8)*this.scaleX)).css('height', Math.round((rect.height + 8)*this.scaleY));
    };
    
    this.select = function(edge, path)
    {
        if(path == undefined)
            return;
        this.node = edge;
        this.path = path;
        this.scaleX = diagram.zoom;
        this.scaleY = diagram.zoom;
        this.initSelectorDotes();
        this.clickPoint = null;
        this.addContextMenu();
        this.selectorDiv.show();
    };
    
    this.restoreSelection = function()
    {
    	if(this.isActive() && this.path)
			this.select(this.node, this.path);
    };
    
    this.getSelection = function()
    {
        if(this.isActive() && this.node)
            return [this.node];
        else
            return [];
    }
    
}

function initDiagramContextMenu()
{
    if(contextMenuInitialized['diagram'])
        return;
    
    $.contextMenu({
        selector: '.edge_selector',
        zIndex: 90,
        reposition : false,
        build: function($triggerElement, event){
            var doc = getActiveDocument();
            var curitems = {};
            if(doc && doc.edgeEditor && !doc.readOnly) {
                var isEdge = false;
                var isVertex = false;
                var vertexIndex = -1;
                doc.edgeEditor.clickPoint = doc.edgeEditor.getRelativeClickPoint(event);
                var point = doc.edgeEditor.clickPoint;
                var clickedRect = new Rectangle(Math.round(point.x/doc.edgeEditor.scaleX) - 2, Math.round(point.y/doc.edgeEditor.scaleY) - 2, 5, 5);
                for(var i = 0; i < doc.edgeEditor.path.npoints - 1; i++ )
                {
                    var view = new LineView(null, doc.edgeEditor.path.xpoints[i], doc.edgeEditor.path.ypoints[i], doc.edgeEditor.path.xpoints[i+1], doc.edgeEditor.path.ypoints[i+1]);
                    if (view.intersects(clickedRect)) 
                    {
                        isEdge = true;
                        break;
                    }
                }
                var boxSize = 4;
                for(var i = 0; i < doc.edgeEditor.path.npoints; i++ )
                {
                    var vertexRect = new Rectangle(doc.edgeEditor.path.xpoints[i]-boxSize, doc.edgeEditor.path.ypoints[i]-boxSize, boxSize*2, boxSize*2);
                    if (vertexRect.intersects(clickedRect)) 
                    {
                        isVertex = true;
                        vertexIndex = "path_vertex_" + i;
                        break;
                    }
                }
                curitems =  {
                'add_vertex': { 
                    name: resources.dgrMenuVertexAdd,
                    visible: isEdge && !isVertex,
                    callback: function(itemKey, opt, originalEvent)
                    {
                        doc.edgeEditor.diagram.editVertex(doc.edgeEditor.node, "add", -1, doc.edgeEditor.clickPoint);
                    }
                },
                'line_type': { 
                    name: resources.dgrMenuVertexLinear,
                    visible: isVertex,
                    callback: function(itemKey, opt, originalEvent)
                    {
                        doc.edgeEditor.editVertex(vertexIndex, "line");
                    }
                },
                'quadric_type': { 
                    name: resources.dgrMenuVertexQuadric,
                    visible: isVertex,
                    callback: function(itemKey, opt, originalEvent)
                    {
                        doc.edgeEditor.editVertex(vertexIndex, "quadric");
                    }
                },
                'cubic_type': { 
                    name: resources.dgrMenuVertexCubic,
                    visible:  isVertex,
                    callback: function(itemKey, opt, originalEvent)
                    {
                        doc.edgeEditor.editVertex(vertexIndex, "cubic");
                    }
                },
                'remove': { 
                    name: resources.dgrMenuVertexRemove,
                    visible: isVertex,
                    callback: function(itemKey, opt, originalEvent)
                    {
                        doc.edgeEditor.editVertex(vertexIndex, "remove");
                    }
                },
                'edit': { 
                    name: resources.dgrMenuEdit,
                    icon: 'edit',
                    visible:  isEdge || isVertex,
                    callback: function(itemKey, opt, originalEvent)
                    {
                        createBeanEditorDialog(resources.dlgEditViewOptionsTitle, BioUML.selection.lastSelected, function() {
                            doc.edgeEditor.diagram.refresh();
                            doc.edgeEditor.diagram.fireChanged();
                        }, true);
                    }
                },
                'straighten': { 
                    name: resources.dgrMenuEdgeStraighten,
                    visible:  isEdge,
                    callback: function(itemKey, opt, originalEvent)
                    {
                        doc.edgeEditor.diagram.editVertex(doc.edgeEditor.node, "straighten", -1);
                    }
                },
                'remove_edge': { 
                    name: resources.dgrMenuEdgeRemove,
                    visible:  isEdge,
                    callback: function(itemKey, opt, originalEvent)
                    {
                        doc.edgeEditor.diagram.removeElements(doc.edgeEditor.node, function()
                        {
                            doc.edgeEditor.clear();
                        });
                    }
                },
                'set_fixed_edge': { 
                    name: resources.dgrMenuSetEdgeFixed,
                    visible:  isEdge && !isVertex,
                    callback: function(itemKey, opt, originalEvent)
                    {
                        doc.edgeEditor.diagram.setFixed(true, [doc.edgeEditor.node]);
                    }
                },
                'set_unfixed_edge': { 
                    name: resources.dgrMenuSetEdgeUnFixed,
                    visible:  isEdge && !isVertex,
                    callback: function(itemKey, opt, originalEvent)
                    {
                        doc.edgeEditor.diagram.setFixed(true, [doc.edgeEditor.node]);
                    }
                },
            }
        }
        return {
            items : curitems
        }
        }
     });
    
    $.contextMenu(
    {
        selector: '.node_selector',
        zIndex: 91,
        className: 'diagram-context-menu',
        build: function ($triggerElement, event){
            var doc = getActiveDocument();
            if(doc && !doc.readOnly && doc.isAnnotation)
                return false;
            var curitems = {};
            if(doc && !doc.readOnly && !doc.isAnnotation && doc.selector) {
                var _thisSelector = doc.selector;
                curitems =  {
                    'edit': { 
                        name: resources.dgrMenuEdit,
                        icon: 'edit',
                        visible: true,
                        callback: function(itemKey, opt, originalEvent)
                        {
                            createBeanEditorDialog(resources.dlgEditViewOptionsTitle, BioUML.selection.lastSelected, function() {
                                _thisSelector.diagram.refresh();
                                _thisSelector.diagram.fireChanged();
                            }, true);
                        }
                    },
                    'copy': { 
                        name: resources.dgrMenuCopy,
                        icon: 'copy',
                        visible: true,
                        callback: function(itemKey, opt, originalEvent)
                        {
                            var nodes = _thisSelector.getSelectedNodes();
                            if(nodes.length == 1)
                            {
                                _thisSelector.diagram.copyElement = BioUML.selection.lastSelected;
                                _thisSelector.diagram.fireChanged();
                            }
                        }
                    },
                    'remove': { 
                        name: resources.dgrMenuRemove,
                        icon: 'remove',
                        visible: true,
                        callback: function(itemKey, opt, originalEvent)
                        {
                            _thisSelector.diagram.removeElements(_thisSelector.nodes, function()
                            {
                                _thisSelector.clear();
                            });
                        }
                    },
//                    'addRow': { 
//                        name: "Add row",
//                        icon: 'edit',
//                        visible: false,
//                        callback: function(itemKey, opt, originalEvent)
//                        {
//                            queryBioUMLWatched("annotationTable", "web/annotationTable/addRow",
//                            {
//                                de: _thisSelector.diagram.completeName,
//                                path: BioUML.selection.lastSelected
//                            }, 
//                            function()
//                            {
//                                _thisSelector.diagram.refresh();
//                            });
//                        }
//                    },
//                    'addColumn': { 
//                        name: "Add column",
//                        icon: 'edit',
//                        visible: false,
//                        callback: function(itemKey, opt, originalEvent)
//                        {
//                            queryBioUMLWatched("annotationTable", "web/annotationTable/addColumn",
//                            {
//                                de: _thisSelector.diagram.completeName,
//                                path: BioUML.selection.lastSelected
//                            }, 
//                            function()
//                            {
//                                _thisSelector.diagram.refresh();
//                            });
//                        }
//                    },
                    
                    'set_fixed': { 
                        name: resources.dgrMenuSetFixed,
                        icon: 'fix',
                        visible: true,
                        callback: function(itemKey, opt, originalEvent)
                        {
                            _thisSelector.diagram.setFixed(true);
                        }
                    },
                    'set_unfixed': { 
                        name: resources.dgrMenuSetUnFixed,
                        icon: 'unfix',
                        visible: true,
                        callback: function(itemKey, opt, originalEvent)
                        {
                            _thisSelector.diagram.setFixed(false);
                        }
                    },
                }
            }
            return {
                items : curitems
            }
        }
    });
    
    contextMenuInitialized['diagram'] = true;
}

