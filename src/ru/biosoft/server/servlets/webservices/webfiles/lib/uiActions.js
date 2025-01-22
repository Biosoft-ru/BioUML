/**
 * Dialogs to perform some specific actions
 */

/*
 * Creates new generic collection and put to repository tree
 */
function createScript(path, type)
{
    if(!path) return;
    createSaveElementDialog(resources.dlgCreateScriptTitle,
            "ru.biosoft.access.TextDataElement", createPath(path, resources.dlgCreateScriptDefaultName+"."+type),
            function(completePath)
            {
                queryBioUML("web/script/create",
                        {
                            de: completePath,
                            type: type
                        },
                        function(data)
                        {
                            refreshTreeBranch(getElementPath(completePath));
                            openDocument(completePath);
                        });
            });
}

function createJavaCode(path, type)
{
    if(!path) return;
    createSaveElementDialog(resources.dlgCreateScriptTitle,
            "biouml.plugins.physicell.javacode.JavaElement", createPath(path, resources.dlgCreateScriptDefaultName+"."+type),
            function(completePath)
            {
                queryBioUML("web/script/create",
                        {
                            de: completePath,
                            type: type
                        },
                        function(data)
                        {
                            refreshTreeBranch(getElementPath(completePath));
                            openDocument(completePath);
                        });
            });
}

// //////////////////////////////
// Generic collection functions
// //////////////////////////////

/*
 * Creates new generic collection and put to repository tree
 */
function createGenericCollection(path, callback)
{
    if(!path) return;
    createPromptDialog(resources.dlgCreateFolderTitle, resources.dlgCreateFolderName, function(folderName)
    {
        queryService('access.service', 25,
        {
            dc: path,
            de: folderName
        }, function(data)
        {
            refreshTreeBranch(path);
            if(callback) callback(folderName);
        });
    }, "New folder", true);
}

/*
 * Remove elements
 */
function removeElements(completePaths)
{
    if(!completePaths || completePaths.length === 0) return;
    if(completePaths.length === 1)
    {
        removeElement(completePaths[0]);
        return;
    }
    createConfirmDialog(resources.commonRemoveElementsPrompt.replace("{count}", completePaths.length), function()
    {
    	var number = completePaths.length;
    	showWaitDialog("Removing in progress, please wait...");
    	
    	var refocusPath = getElementPath(completePaths[0]);
    	openBranch(refocusPath, true);
    	_.each(completePaths, function(completePath)
    	{
    		closeDocumentByPath(completePath);
    	});
    	
        _.each(completePaths, function(completePath)
        {
            var path = getElementPath(completePath);
            var name = getElementName(completePath);
            
            queryService('access.service', 26, 
                {
                    dc: path,
                    de: name
                },
                function(data)
                {
                    if (data.values === 'ok') 
                    {
                        removeElementOnClient(completePath);
                    }
                    number = number - 1;
                    if( number === 0 )
                    	removeWaitDialog();
                },
                function(){
                	removeWaitDialog();
                }
            );
        });
    });
}

/*
 * Remove element
 */
function removeElement(completePath)
{
    if(!completePath) return;
    var path = getElementPath(completePath);
    var name = getElementName(completePath);
    
    createConfirmDialog(resources.commonRemoveElementPrompt.replace("{name}", name.escapeHTML()), function()
    {
    	showWaitDialog("Removing in progress, please wait...");
        queryService('access.service', 26, 
            {
                dc: path,
                de: name
            },
            function(data)
            {
            	removeWaitDialog();
            	if (data.values == 'ok') 
                {
                    removeElementOnClient(completePath);
                    //TODO: do not update whole branch, just change totalsize attribute in "Load next xx/Load last xx" tree element
                    refreshTreeBranch(getElementPath(completePath));
                }
            },
            function(){
            	removeWaitDialog();
            }
        );
    });
}

/**
 * Remove element from DOM-tree (including all linked paths) 
 * and from parent DataCollection (including all linked collections)
 * Should be called after successful remove response from server
 * 
 * @param completePath path like data/Projects/Test
 * @param isRealPath is used to avoid error in getTargetPath when element was already removed from server
 */
function removeElementOnClient(completePath, isRealPath)
{
    if(!completePath) return;
    
    var realPath = isRealPath ? completePath : getTargetPath(completePath); 
    if(!realPath) return;

    var paths = getBackPaths(realPath);
    for(var i=0; i<paths.length; i++)
    {
        var node = getTreeNode(paths[i]);
        if(node) getTreeObject(paths[i]).jstree('delete_node', node);
    }
    var parentNode = getTreeNode(getElementPath(realPath));
    if(parentNode)
        getTreeObject(getElementPath(realPath)).jstree('select_node', parentNode);
    
    var path = getElementPath(realPath);
    var name = getElementName(realPath);
    var parent = getDataCollection(path);
    if(parent) 
    {
        parent.remove(name);
        var backLinks = getBackPaths(path);
        if(backLinks)
        {
            backLinks.forEach(function(backLink){
                var dc = getDataCollection(backLink);
                if(dc)
                    dc.remove(name);
                });
        }
    }
}

function createJournalDiagram(path, type, autoopen)
{
    if(!path) return;
    createSaveElementDialog(resources["dlgCreate"+type.ucfirst()+"Title"],
            "biouml.model.Diagram", createPath(path, resources["dlgCreate"+type.ucfirst()+"DefaultName"]),
            function(completePath)
            {
                var path = getElementPath(completePath)
                var deName = getElementName(completePath);
                queryBioUML("web/journal/create",
                {
                    dc: path,
                    de: deName,
                    type: type
                }, function(data)
                {
                    refreshTreeBranch(path);
                    if(autoopen)
                    {
                        var completeName = createPath(path, deName);
                        openDiagram(completeName);
                        openBranch(path, true); 
                    }
                    else
                    {
                        refreshTreeBranch(path);
                    }
                });
            });
}

/*
 * Create new diagram, type list is loaded from server for every module
 */
function createNewDiagram(path, name, callback, types)
{
    var _this = this;
    
    var diagramName = name ? name : resources.dlgCreateDiagramDefaultName;
    var defaultPath = createPath( path ? path : "data", diagramName);
        
    var property = new DynamicProperty("Diagram", "data-element-path", defaultPath);
    property.getDescriptor().setDisplayName(resources.dlgCreateOptimizationDiagram);
    property.getDescriptor().setReadOnly(false);
    property.setCanBeNull("no");
    property.setAttribute("dataElementType", "biouml.model.Diagram");
    property.setAttribute("elementMustExist", false);
    property.setAttribute("promptOverwrite", true);
    
    var pathEditor = new JSDataElementPathEditor(property, null);
    pathEditor.setModel(property);
    var dgrNode = pathEditor.createHTMLNode();
    pathEditor.setValue(defaultPath);
    pathEditor.addChangeListener(
        function(control, oldValue, newValue)
        {
            var select = dialogDiv.find('#diagramtype');
            var diagamType = undefined;
            if(select)
                 diagamType = select.val();
            _this.updateTypeList(getElementPath(newValue), diagamType);
        });
    
    var dialogDiv = $('<div title="'+resources.dlgCreateDiagramTitle+'" id="new_diagram_dlg'+ rnd() + '">' + resources.dlgCreateDiagramName + '</div>');
    dialogDiv.append(dgrNode);
    dialogDiv.append('<br/><br/>' + resources.dlgCreateDiagramType+' <span id="loading_dummy" ><img src="icons/busy.png"/>'+resources.commonLoading+'</span><div id="type_container"></div><br/><br/>'+
    '<div id="type_description"></div><br/><br/>');
    this.loadingDummy = dialogDiv.find('#loading_dummy');
    this.typeContainer = dialogDiv.find('#type_container');
    this.typeDescription = dialogDiv.find('#type_description');
    this.loadingDummy.hide();
    
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 320,
        buttons: 
        {
            "Ok": function()
            {
                _this.createDiagram();
            },
            "Cancel" : function()
            {
                $(this).dialog("close");
                $(this).remove();
            }
        }
    });
    addDialogKeys(dialogDiv);
    sortButtons(dialogDiv);
    
    this.updateTypeList = function(dcPath, prevType)
    {
        dialogDiv.parent().find(":button:contains('Ok')").attr("disabled", "disabled");
        var dc = getDataCollection(dcPath);
        _this.typeContainer.hide();
        _this.typeDescription.hide();
        _this.loadingDummy.show();
        dc.getDiagramTypes(function(types) {
            _this.loadingDummy.hide();
            _this.showDiagramTypes(types, prevType);
            _this.typeContainer.show();
            _this.typeDescription.show();
        });
    };
    
    this.showDiagramTypes = function(diagramTypes, type)
    {
        var diagramTypeDiv = _this.typeContainer;
        if (diagramTypes.length == 0) 
            diagramTypeDiv.html(resources.dlgCreateDiagramNoTypes);
        else 
        {
            var select = $('<select/>').attr('id', 'diagramtype').css('width', '280px');
            diagramTypeDiv.html(select);
            var descriptions = {};
            for (var i = 0; i < diagramTypes.length; i++) 
            {
                var option = $('<option value="' + diagramTypes[i].name + '">' + diagramTypes[i].title + '</option>');
                if(type && diagramTypes[i].name == type)
                    option.attr('selected', 'selected');
                select.append(option);
                descriptions[diagramTypes[i].name] = diagramTypes[i].description;
            }
            select.attr("size", diagramTypes.length);
            select.change(function()
            {
                var descr = descriptions[$(this).val()];
                if (descr != undefined) 
                    _this.typeDescription.html(resources.dlgCreateDiagramTypeDescription + ' ' + descr);
                else 
                    _this.typeDescription.html('');
            });
            dialogDiv.parent().find(":button:contains('Ok')").removeAttr("disabled");
            select.trigger('change');
        }
    };
    
    this.createDiagram = function()
    {
        var diagramPath = pathEditor.getValue();
        var diagramName = getElementName(diagramPath);
        if (!diagramName) 
        {
            logger.error(resources.commonErrorEmptyNameProhibited);
            return false;
        }
        else if(diagramName == resources.dlgCreateDiagramDefaultName) //check non-changed default name
        {
    		var dc = getDataCollection(getElementPath(diagramPath));
        	var info = dc.getElementInfo(diagramName);
            if(info != null)
            {
            	createConfirmDialog(resources.dlgOpenSaveConfirmOverwrite.replace("{element}",diagramName), _this.doCreateDiagram);
            }
            else
            	_this.doCreateDiagram();
        }
        else
        	_this.doCreateDiagram();
    };
    
    this.doCreateDiagram = function()
    {
    	var diagramPath = pathEditor.getValue();
        var diagramName = getElementName(diagramPath);
        var select = dialogDiv.find('#diagramtype');
        var diagamType = select.val();
        queryService("diagram.service", 214,
        {
            dc: getElementPath(diagramPath),
            diagram: diagramName,
            type: diagamType
        }, 
        function(data)
        {
            var diagramObj = JSON.parse(data.values);
            refreshTreeBranch(path);
            if(callback)
                callback(diagramObj["name"], diagramObj["type"]);
        });
        dialogDiv.dialog("close");
        dialogDiv.remove(); 
    };
    
    
    dialogDiv.dialog("open");
    dialogDiv.parent().find(":button:contains('Ok')").attr("disabled", "disabled");
    var prevType = types == undefined || types[0] == undefined ? undefined : types[0].name;
    _this.updateTypeList(path, prevType);
}

function createNewOptimization(path)
{
    if(!path) return;
    
    var dialogDiv = $('<div><b>'+resources.dlgCreateOptimizationTitle+'</b>&nbsp;</div>');
    
    var defaultOptPath = path + "/";
    dialogDiv.append("<p><b>"+resources.dlgCreateOptimizationName+":</b></p>");
    var inputField = $('<input type="text"/>').width(250);
    var defaultValue = "New optimization";
    if(defaultValue !== undefined)
        inputField.val(defaultValue);
    dialogDiv.append(inputField);
    dialogDiv.append('<br/><br/><b>'+resources.dlgCreateOptimizationDiagram+':</b>&nbsp;');
    
    var defaultPath = defaultOptPath;
    var property = new DynamicProperty("optimizationDiagram", "data-element-path", defaultPath);
    property.getDescriptor().setDisplayName(resources.dlgCreateOptimizationDiagram);
    property.getDescriptor().setReadOnly(false);
    property.setCanBeNull("no");
    property.setAttribute("dataElementType", "biouml.model.Diagram");
    property.setAttribute("elementMustExist", true);
    property.setAttribute("promptOverwrite", false);
    
    var pathEditor = new JSDataElementPathEditor(property, null);
    pathEditor.setModel(property);
    var dgrNode = pathEditor.createHTMLNode();
    pathEditor.setValue(defaultPath);
    dialogDiv.append(dgrNode);
    
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 320,
        buttons: 
        {
            "Ok": function()
            {
                var optimizatioName = inputField.val();
                if (!optimizatioName) 
                {
                    logger.error(resources.dlgCreateOptimizationErrorNoPath);
                    return false;
                }
                var diagramPath = pathEditor.getValue();
                if (!getElementName(diagramPath)) 
                {
                    logger.error(resources.dlgCreateOptimizationErrorNoDiagram);
                    return false;
                }
                var optimizationPath = path + "/" + optimizatioName;
                var _this = $(this);
                var okHandler = function ()
                {
                    queryBioUML("web/optimization/create", 
                    {
                        de: optimizationPath,
                        diagram: diagramPath,
                        method: ""
                    }, 
                    function(data)
                    {
                        var path = getElementPath(optimizationPath);
                        getDataCollection(path).invalidateCollection();
                        refreshTreeBranch(path);
                        CreateOptimizationDocument(optimizationPath, function (optimizationDoc) {
                            openDocumentTab(optimizationDoc);
                        });
                    });
                            
                    _this.dialog("close");
                    _this.remove();
                }
                
                if(isPathExists(optimizationPath))
                {
                    createConfirmDialog(resources.dlgOpenSaveConfirmOverwrite.replace("{element}", optimizatioName), 
                        function()
                        {
                            closeDocumentByPath(optimizationPath);
                            okHandler();
                            return;
                        });
                    return false;
                }
                okHandler();
            },
            "Cancel" : function()
            {
                $(this).dialog("close");
                $(this).remove();
            }
        }
    });
    dialogDiv.dialog("open");
    addDialogKeys(dialogDiv);
    sortButtons(dialogDiv);
}

function showImage(imagePath)
{
    var dialogDiv = $('<div/>').attr("title", resources.commonPlotBoxTitle);
    var img = $("<img/>");
    dialogDiv.append(img);
    var resizeFn = function() {
        var width = dialogDiv.dialog("option", "width");
        var height = dialogDiv.dialog("option", "height");
        img.attr("src", appInfo.serverPath+"web/img?" + toURI({de: imagePath, w:width-30, h:height-100}));
    };
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 600,
        height: 500,
        open: resizeFn,
        close: function()
        {
            queryBioUML("web/img/close", {de: imagePath}, function() {}, function() {});
        },
        resizeStop: resizeFn,
        buttons: 
        {
            "Ok": function()
            {
                $(this).dialog("close");
                $(this).remove();
            },
            "Save": function()
            {
                var width = dialogDiv.dialog("option", "width");
                var height = dialogDiv.dialog("option", "height");
                saveImage(imagePath, {w:width-30, h:height-100});
            }
        }
    });
    addDialogKeys(dialogDiv);
    sortButtons(dialogDiv);
    dialogDiv.dialog("open");
}

function saveImage(imagePath, addParams)
{
    createSaveElementDialog("Save image", "ru.biosoft.access.ImageDataElement", null, function(path)
    {
        var params = {
            de: imagePath,
            path: path
        };
        if(addParams)
            _.extend(params, addParams);
        queryBioUML("web/img/save", params, 
        function(data)
        {
            logger.message(data.values);
            refreshTreeBranch(getElementPath(path));
        });
    });
};

function createNewElement(parentPath)
{
    var dc = getDataCollection(parentPath);
    var dialogNewElementName = function(initialName, callback)
    {
        createPromptDialog(resources.dlgCreateElementTitle, resources.dlgCreateElementName, callback, initialName, true);
    };
    var dialogRenameElement = function(callback)
    {
        createPromptDialog(resources.dlgCreateElementTitle, "Element with this name already exists, please provide another name", callback);
    };
    if(dc.getAttributes()['ask-user-for-id'] == 'false')
    {
        dialogNewElementName = function(initialName, then) { then(initialName); };
        dialogRenameElement = function(callback)
        {
            queryBioUML("web/newElement", {action: "generateName", de: parentPath}, function(data)
            {
               createConfirmDialog("Element with this name already exists, new name will be generated. Please check it and save again.", function() { callback(data.values); });
            });
        };
    }
    var editNewElement = function(elementPath)
    {
          createBeanEditorDialog(resources.commonEditElementInfoTitle, "beans/newElement/" + elementPath, function()
          {
              queryBioUML("web/newElement", {action: "save", de: elementPath},function(data)
              {
                  if(data.values=="ok")
                      refreshTreeBranch(parentPath);
                  else if(data.values=="exists")
                      dialogRenameElement(function(newName)
                      {
                         queryBioUML("web/newElement", {action: "rename", de: elementPath, newName: newName}, function() { editNewElement(parentPath + "/" + newName); });
                      });
              },
              function(data) {
                  createConfirmDialog("Error saving element: " + data.message + ", would you like to continue editing element?", function() { editNewElement(elementPath); });
              });
          }, true);
    };
    queryBioUML("web/newElement", {action: "generateName", de: parentPath}, function(data)
    {
        var defaultName = data.values;
        dialogNewElementName(defaultName, function(elementName)
        {
            var elementPath = parentPath + "/" + elementName;
            queryBioUML("web/newElement", {action: "createElement", de: elementPath}, function()
            {
                editNewElement(elementPath);
            });
        });
    });
};
