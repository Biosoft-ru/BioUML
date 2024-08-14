/**
 * JavaScript OptimizationDocument
 *
 * @author anna
 */

function OptimizationDocument (completeName)
{
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId(completeName);
    
    this.modelDetected = false;
    this.loadedListeners = [];
    this.experimentsListeners = [];
    this.loaded = false;
    this.changed = false;
    this.status = "";
    this.selectedParameters = {};
    var _this = this;
    getDataCollection(this.completeName).addChangeListener(this);
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var optimizationDocument = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(optimizationDocument);
        
        this.optimizationContainerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        optimizationDocument.append(this.optimizationContainerDiv);
        
        this.optimizationControlButtons = $('<div id="' + this.tabId + '_actions" class="table_actions"></div>').css('float', 'left');
        this.initActions();
        this.optimizationContainerDiv.append(this.optimizationControlButtons);
        
        this.dataDiv = $('<div id="' + this.tabId + '_data">Loading...</div>');
        this.optimizationContainerDiv.append(this.dataDiv);
        
        this.loadOptimizationInfo();
        
        //update document size
        resizeDocumentsTabs();
    };
    
    this.getOptimization = function ()
    {
        return this;
    };
    
    // Code to be called when optimization is loaded
    this.addLoadedListener = function(listener)
    {
    	if(this.loaded)	// already loaded
    		listener.optimizationLoaded();
    	else
    		this.loadedListeners.push(listener);
    };
    
    this.loadOptimizationInfo = function()
    {
        queryBioUML("web/optimization",
        {
            de: this.completeName,
            action: "info"
        }, function(data)
        {
            if (data.type == 0)
            {
                _this.diagramPath = data.values.diagramPath;
                _this.method = data.values.method;
                _this.methodData = data.values.methodData;
                _this.loaded = true;
                _this.optimizationDiagram = data.values.optimizationDiagram;
                for (li = 0; li < _this.loadedListeners.length; li++)
                {
                    _this.loadedListeners[li].optimizationLoaded();
                }
                _this.loadedListeners = [];
                selectViewPart('optimization.complex');
                _this.loadParamsTable(true);
                
            }
            else if(data.type == 2)
            {
                _this.diagramPath = data.values.diagramPath;
                _this.method = data.values.method;
                _this.methodData = data.values.methodData;
                createYesNoConfirmDialog("The optimization diagram does not coincide with the optimization document."+ 
                    "<br/>Do you want to rewrite it? If not, the new diagram will be created.", function(rewrite)
	                {
                        queryBioUML("web/optimization/optimization_diagram",
                        {
                            de: _this.completeName,
                            "rewrite": rewrite
                        },
                        function(data)
                        {
                            _this.optimizationDiagram = data.values;
                            _this.loaded = true;
                            for (li = 0; li < _this.loadedListeners.length; li++)
                            {
                                _this.loadedListeners[li].optimizationLoaded();
                            }
                            _this.loadedListeners = [];
                            selectViewPart('optimization.complex');
                            _this.loadParamsTable(true);
                        });
                  });
            }
        });
    };
    
    this.activate = function()
    {
        selectViewPart('optimization.complex');
    };
    
    
    this.addExperimentsListener = function(listener)
    {
        var alreadyAdded = false;
        for (var li = 0; li < this.experimentsListeners.length; li++)
        {
            if (this.experimentsListeners[li] == listener)
            {
                alreadyAdded = true;
                break;
            }
        }
        if (!alreadyAdded)
        {
            this.experimentsListeners.push(listener);
        }
    };
    
    this.loadExperiments = function(callback)
    {
        queryBioUML("web/optimization",
        {
            de: this.completeName,
            action: "experiments"
        }, function(data)
        {
            _this.experiments = data.values;
            for (var li = 0; li < _this.experimentsListeners.length; li++)
            {
                _this.experimentsListeners[li].experimentsLoaded();
            }
            if(callback)
                callback();
        });
    };
    
    this.modelDetected = false;
    this.getModelObjectName = function(callback)
    {
        if (_this.modelDetected)
        {
            callback(_this.modelObjectName);
        }
        else if(!_this.diagramPath)
        {
            callback(null);
        }
        else
        {
            queryBioUML("web/optimization",
            {
                de: _this.completeName,
                action: "model"
            }, function(data)
            {
                _this.modelObjectName = data.values;
                _this.modelDetected = true;
                callback(_this.modelObjectName);
            }, function()
            {
                _this.modelObjectName = null;
                _this.modelDetected = true;
                callback(_this.modelObjectName);
            });
        }
    };

    this.loadParamsTable = function(isReadOnly)
    {
        if(isReadOnly == undefined)
            isReadOnly = true;
        queryBioUML("web/table", {
        	action: "sceleton",
        	type: "optimization",
        	tabletype: "fittingparams",
        	read: isReadOnly,
        	de: this.completeName
        }, function(data)
        {
            _this.dataDiv.html(data.values);
            _this.table = _this.dataDiv.children("table");
            _this.table.addClass('selectable_table');
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "bPaginate": false,
                "sDom": "irt",
                "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
                    return nRow;
                },
                "sAjaxSource": appInfo.serverPath+"web/table?action=datatables"
                +"&type=optimization&tabletype=fittingparams"
                +"&de=" + encodeURIComponent(_this.completeName)
                +"&read=" + isReadOnly
                +'&rnd=' + rnd() 
            };
            _this.table.dataTable(features);
        });
    };
    
    this.reloadParamsTable = function(model)
    {
        queryBioUML("web/optimization",
            {
                de: this.completeName,
                action: "change",
                what: model
            }, function(data)
            {
                if(_this.table != undefined)
                {
                    _this.table.fnClearTable( 0 );
                    _this.table.fnDraw();
                }
                else
                {
                    _this.loadParamsTable();
                }            
            });
    };
    
    this.removeParameters = function()
    {
        if(this.table == undefined)
        {
            logger.error("Parameters are not loaded yet");
            return false;
        }
        
        var deleteRows = getTableSelectedRows(this.table);
        if(deleteRows.length > 0)
        {
            var dataParams = {
                    action: 'remove',
                    de: _this.completeName,
                    jsonrows: $.toJSON(deleteRows),
                    what: "fittingparams"
                };
            queryBioUML("web/optimization", dataParams, function(data)
            {
                _this.loadParamsTable();
            });
        }
    };
    
    this.addParameters = function (model, addParams, subDiagram)
    {
        var dataParams = {
                action: 'add',
                de: _this.completeName,
                jsonrows: $.toJSON(addParams),
                what: model
            };
        if(subDiagram)
        	dataParams["subDiagram"] = subDiagram;
        queryBioUML("web/optimization", dataParams, function(data)
        {
        	_this.changed = true;
            _this.loadParamsTable();
        });
    };
    
    this.saveParameters = function(callback)
    {
        var dataDPS = getDPSFromTable(_this.table);
        var dataParams = {
                action: "change",
                type: "optimization",
                tabletype: "fittingparams",
                de: _this.completeName,
                data: convertDPSToJSON(dataDPS)
            };
        var tableparams = getTableDisplayParameters(_this.table);
        for(par in tableparams)
        {
            dataParams[par] = tableparams[par];   
        }
        queryBioUML("web/table", dataParams,
            function(data)
            {
        		_this.changed = true;
                if(callback != undefined)
                {
                    callback();
                }
                else
                {
                    _this.loadParamsTable();
                }
            }
        );  
    };
    
    this.changeMethod = function(methodName, json)
    {
        this.method = methodName;
        var dataParams = {
                action: "change",
                de: _this.completeName,
                method: this.method,
                options: json,
                what: "method"
            };
        queryBioUML("web/optimization", dataParams, function(data)
        {
            _this.dataDiv.html("");
            _this.table = undefined;
            _this.loadParamsTable();
        });
    };
    
    this.initActions = function()
    {
//        var saveButton = $('<input type="button" value="Save parameters" title="Update fitting parameters"/>');
//        saveButton.click(function()
//        {
//            _this.saveParameters();
//        });
//        this.optimizationControlButtons.append(saveButton);
        
        this.editButton = $('<input type="button" value="'+resources.tblButtonEdit+'"/>');
        this.applyButton = $('<input type="button" value="'+resources.tblButtonApplyEdit+'"/>').attr("disabled", true);
        this.cancelButton = $('<input type="button" value="'+resources.tblButtonCancelEdit+'"/>').attr("disabled", true);
        
        var _this = this;
        
        var updateClasses = function()
        {
            _this.optimizationControlButtons.children().removeClass("ui-state-disabled").addClass("ui-state-default");
            _this.optimizationControlButtons.children("[disabled]").addClass("ui-state-disabled");
        };
        
        this.editButton.click(function()
        {
            _this.editButton.attr("disabled", true);
            _this.applyButton.attr("disabled", false);
            _this.cancelButton.attr("disabled", false);
            updateClasses();
            _this.loadParamsTable(false);
        });
        this.applyButton.click(function()
        {
            _this.editButton.attr("disabled", false);
            _this.applyButton.attr("disabled", true);
            _this.cancelButton.attr("disabled", true);
            updateClasses();
            _this.saveParameters();
        });
        this.cancelButton.click(function()
        {
            _this.editButton.attr("disabled", false);
            _this.applyButton.attr("disabled", true);
            _this.cancelButton.attr("disabled", true);
            updateClasses();
            _this.loadParamsTable(true);
        });
        
        this.optimizationControlButtons.append(this.editButton);
        this.optimizationControlButtons.append(this.applyButton);
        this.optimizationControlButtons.append(this.cancelButton);
        updateClasses();
    };
    
    this.undo = function(value)
    {
        var _this = this;
        queryBioUML("web/optimization",
        {
            de: this.completeName,
            action: "undo"
        }, function(data)
        {
            _this.loadOptimizationInfo();
            _this.loadParamsTable(true);
            _this.loadConstraints();
            _this.loadExperiments();
        });
    };

    this.redo = function(value)
    {
        var _this = this;
        queryBioUML("web/optimization",
        {
            de: this.completeName,
            action: "redo"
        }, function(data)
        {
            _this.loadOptimizationInfo();
            _this.loadParamsTable(true);
            _this.loadConstraints();
            _this.loadExperiments();
        });
    };
    
    this.loadConstraints = function()
    {
        var vp = lookForViewPart('optimization.complex');
        if(vp != null)
            vp.loadConstraints();
    };
    
    this.close = function(callback)
    {
    	if(callback) callback();
    };
    
    this.isChanged = function()
    {
    	return this.changed;
    };
    
    this.save = function(callback)
    {
        queryBioUML("web/optimization/save",
        {
            "de": this.completeName
        },
        function(data)
        {
        	_this.changed = false;
        	if(callback) callback(data);
        });
    };
    
    this.dataCollectionChanged = function()
    {
        queryBioUML("web/optimization/optimization_diagram",
        {
            de: this.completeName,
            "rewrite": true
        },
        function(data)
        {
            if (_this.optimizationDiagram != data.values) 
            {
                refreshTreeBranch(getElementPath(_this.completeName));
                _this.optimizationDiagram = data.values;
            }
        });
    };
}

