/*
/*
 *	Initialize view parts for diagrams 
 */
function initDiagramViewParts()
{
	if(viewPartsInitialized['diagram'])
        return;
        
    var startIndex = viewPartsInitialized['table'] ? 2 : 0;
    var simulationVP = new ComplexSimulationViewPart();
    simulationVP.init();
    var modelVP = new ComplexModelViewPart();
    modelVP.init();
    var statesVP = new StatesViewPart();
    statesVP.init();
    var microenvVP = new MicroenvironmentViewPart();
    microenvVP.init();
    var celltypesVP = new CellTypesViewPart();
    celltypesVP.init();
    
    viewParts.splice(startIndex, 0, new DiagramOverviewViewPart(), new DiagramLayoutViewPart(), modelVP, simulationVP, statesVP, microenvVP, celltypesVP, new AntimonyViewPart(),
            new FbcViewPart(), new DiagramFilterViewPart());
    
    viewParts.push(new WorkflowViewPart());
    viewParts.push(new JournalViewPart());
    viewParts.push(new VersionHistoryViewPart());
    viewParts.push(new HemodynamicsViewPart());
    viewPartsInitialized['diagram'] = true;
}


/*
 * Diagram plot view part
 */
function DiagramPlotViewPart()
{
    this.tabId = "diagram.plot";
    this.tabName = resources.vpPlot;
    this.currentObject = null;
    this.data = null;
    this.tabDiv = createViewPartContainer(this.tabId);
    this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
    this.tabDiv.append(this.containerDiv); 
    this.lockDiv = $('<div id="lock_plot_bean" class="ui-widget-overlay" style="position:absolute; top:0; left:0; z-index:1001;"></div>');
    this.containerDiv.append(this.lockDiv);
    this.propertyInspector = $('<div id="' + this.tabId + '_pi"></div>');
    this.containerDiv.append(this.propertyInspector);
    

    this.colorIndex = 0;
    this.colorArray = ["[255,85,85]", "[85,85,255]", "[0,255,0]", "[18,34,123]", "[139,0,0]", "[0,179,230]", "[255,51,255]",  
                      "[230,179,51]", "[51,102,230]", "[153,255,153]", "[179,77,77]", "[102,128,179]", "[204,128,204]", 
                      "[51,153,26]", "[204,153,153]", "[77,128,204]", "[153,0,179]", "[230,77,102]", "[77,179,128]", "[153,230,230]"];


    var _this = this;

    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram)) 
            if( documentObject.getDiagram().getModelObjectName() != null)
            {
                callback(true);
            }
            else
            {
                callback(false);    
            }    
        else 
            callback(false);
    };

    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
            this.currentObject = documentObject.getDiagram();
            this.loadModel();
        }
    };
    
    
    this.show = function()
    {
        this.loadModel();
    };
    
    /*
     * Creates toolbar actions for this tab
     */
    this.initActions = function(toolbarBlock)
    {
        //TODO: move messages to messageBundle.js
        this.editAction = createToolbarButton("Edit curves and experiments", "edit.gif", _this.editCurves);
        toolbarBlock.append(this.editAction);
        
    };
    
    this.getNextColor = function()
    {
        return this.colorArray[(this.colorIndex++) % this.colorArray.length];
    }
    
    this.editCurves = function()
    {
        queryBioUML("web/diagramplot/plots_list", 
        {
            de: _this.currentObject.completeName
        },
        function(data)
        {
            _this.plots = data.values;
            _this.editCurvesWithList();
        });
    }
    
    this.editCurvesWithList = function()
    {
        var curvesDialogDiv = $('<div title="Edit plot curves"></div>');
        
        var plotSelect = $('<select></select>');
        for(var i =0; i < _this.plots.length; i++)
        {
            plotSelect.append($('<option></option>').val(_this.plots[i]).text(_this.plots[i]));
        }
        
        var typeSelect = $('<select></select>');
        typeSelect.append($('<option></option>').val("curves").text("Curves"));
        typeSelect.append($('<option></option>').val("experiments").text("Experiments"));
        
        plotSelect.change(function()
        {
            _this.loadTable($(this).val(), typeSelect.val());
        });
        
        typeSelect.change(function()
        {
            _this.loadTable(plotSelect.val(), $(this).val());
        });

        curvesDialogDiv.append($('<div></div>').css("padding-bottom", "10px").append('<b>Select plot:</b>&nbsp;').append(plotSelect));
        curvesDialogDiv.append($('<div></div>').css("padding-bottom", "10px").append('<b>Select line type:</b>&nbsp;').append(typeSelect));
        
        _this.table = $('<div>'+resources.dlgPlotEditorLoading+'</div>');
        curvesDialogDiv.append(_this.table);
       
        curvesDialogDiv.dialog(
        {
            autoOpen: false,
            width: 700,
            buttons:
            {
                
                "Save" : function()
                {
                    var _thisDialog = $(this);
                    _this.saveTable(plotSelect.val(), typeSelect.val(), function(){
                        _thisDialog.dialog("close");
                        _thisDialog.remove();
                        _this.reloadModel();
                    });
                    return false;
                },
                "Remove plot" : function()
                {
                    var plotName = plotSelect.val();
                    createConfirmDialog("Do you really want to remove plot " + plotName, 
                    function(){
                        _this.removePlot(plotName, plotSelect);
                    });
                    
                    return false;
                },
                "Add plot" : function()
                {
                    createPromptDialog("Create new plot", "Enter plot name: ", 
                    function(name){
                        _this.addPlot(name, plotSelect);
                    }, makeUniqueName("Plot",  plotSelect.children().map((index, option) => option.value).get()));
                    
                    return false;
                },
                "Remove line": function()
                {
                    _this.removeCurve(plotSelect.val(), typeSelect.val());
                    return false;
                },
                "Add line": function()
                {
                    _this.addToPlot(plotSelect.val(), typeSelect.val());
                    return false;
                },
            }
        });
        _this.loadTable(plotSelect.val(), typeSelect.val());
        curvesDialogDiv.dialog("open");  
    };
    
    this.addPlot = function(newName, plotSelect)
    {
        queryBioUML("web/diagramplot/add_plot", 
                {
                    de: _this.currentObject.completeName,
                    plotname: newName
                },
                function()
                {
                    plotSelect.append($('<option></option>').val(newName).text(newName));
                    plotSelect.val(newName).change();
                });
    };
    
    this.removePlot = function(plotName, plotSelect)
    {
        queryBioUML("web/diagramplot/remove_plot", 
                {
                    de: _this.currentObject.completeName,
                    plotname: plotName
                },
                function()
                {
                    plotSelect.find('[value="'+plotName+'"]').remove();
                    plotSelect.change();
                });
    };
    
    
    this.saveTable = function(plotName, typeName, callback)
    {
        if (_this.tableObj) 
        {
            var dataParams = {
                rnd: rnd(),
                action: 'change',
                de: _this.currentObject.completeName,
                type: "plotinfo",
                tabletype: typeName,
                plotname: plotName
            };
            saveChangedTable(_this.tableObj, dataParams, callback);
        }
        else
        {
            callback();    
        }
    };
    
    this.loadTable = function(plotName, typeName)
    {
        if(plotName == null)
        {
            _this.table.html('<div>'+resources.dlgPlotEditorTableNoPlots.replace("{diagram}", getElementName(_this.currentObject.completeName))+'</div>');
            return;
        }
        
        _this.table.html('<div>'+resources.dlgPlotEditorLoading+'</div>');
        var params = {
                de: _this.currentObject.completeName,
                type: "plotinfo",
                tabletype: typeName,
                plotname: plotName,
                add_row_id: "true"
            };
        queryBioUML("web/table/sceleton", params, 
        function(data)
        {
            _this.table.html(data.values);
            _this.tableObj = _this.table.children("table");
            _this.tableObj.addClass('selectable_table');
            params["rnd"] = rnd();
            params["read"] = true;
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "sDom": "frti",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?" + toURI(params) 
            };
            _this.tableObj.dataTable(features);
            _this.tableObj.css('width', '100%');
        });
    };
    
    this.addToPlot = function(plotName, typeName)
    {
        if(typeName == "curves")
          queryBioUML("web/diagramplot/subdiagrams", 
          {
              de: _this.currentObject.completeName
          }, function(data)
          {
            _this.addCurve(plotName, typeName, data.values);
          });
        else if(typeName == "experiments")
            _this.addExperiment(plotName, typeName);
    };
    
    this.addCurve = function(plotName, typeName, subDiagrams)
    {
        var dialogDiv = $('<div title="Add ' + typeName + ' to plot"></div>');
        
        var variableSelector = $('<select></select>');
        
        var diagramSelector = undefined;
        if(subDiagrams && subDiagrams.length > 0){
            diagramSelector = $('<select></select>').css('margin-bottom', '10px');
            diagramSelector.append($("<option>").val("").text(" "));
            for(var i=0; i<subDiagrams.length; i++)
            {
                diagramSelector.append($("<option>").val(subDiagrams[i]).text(subDiagrams[i]));
            }
            dialogDiv.append('<br/><b>Select subdiagram:</b>&nbsp;');
            dialogDiv.append(diagramSelector);
            diagramSelector.change(function() {
                var val = $(this).val();
                _this.loadCurveVariables(plotName, val, function(values){
                    variableSelector.empty();
                    if (values) 
                    {
                        $.each(values, function(index, value)
                        {
                            variableSelector.append($('<option></option>').val(value.name).text(value.name).attr("title", value.title));
                            variableSelector.trigger('change');
                        });
                    }
                });
            });
        }
        
        var variableDiv = createSinglePropertyControl("Select variable", variableSelector);
        dialogDiv.append(variableDiv);
        
        var variableTitle = $('<input></input>').attr("size", 35);
        var variableTitleDiv = createSinglePropertyControl("Line title", variableTitle);
        dialogDiv.append(variableTitleDiv);
        
        variableSelector.change(function() {
            var option = $(this).find('option:selected');
            var title = option.attr("title");
            variableTitle.val(title);
        });
        
        var property = new DynamicProperty("Color", "color-selector", null );
        property.getDescriptor().setReadOnly(false);
        property.setCanBeNull("no");
        
        var colorSelector = new JSColorSelector(property, null);
        colorSelector.setModel(property);
        var colorNode = colorSelector.createHTMLNode();
        var color = _this.getNextColor();
        colorSelector.setValue([color]);
        var colorDiv = createSinglePropertyControl("Line color", $(colorNode));
        dialogDiv.append(colorDiv);
        
        var currentDiagram = diagramSelector ? diagramSelector.val() : "";
        _this.loadCurveVariables(plotName, currentDiagram, function(values){
            variableSelector.empty();
            if (values) 
            {
                $.each(values, function(index, value)
                {
                    variableSelector.append($('<option></option>').val(value.name).text(value.name).attr("title", value.title));
                });
            }
            variableSelector.trigger("change");
        });

        dialogDiv.dialog(
        {
            autoOpen: false,
            width: 400,
            buttons:
            {
                "OK": function()
                {
                    var addParams = {
                            de: _this.currentObject.completeName,
                            plotname: plotName,
                            color: $.toJSON(colorSelector.getValue()),
                            varname: variableSelector.val(),
                            title: variableTitle.val()
                        };
                        if(diagramSelector)
                            addParams.subdiagram = diagramSelector.val();
                        queryBioUML("web/diagramplot/add", addParams,
                            function(data)
                            {
                                _this.loadTable(plotName, typeName);
                                var color = _this.getNextColor();
                                colorSelector.setValue([color]);
                            },
                            function(data)
                            {
                                logger.error(data.message);
                            });
                    
                    return false;
                },
                "Close": function()
                {
                    $(this).dialog("close");
                    $(this).remove();
                }
            }
        });
        dialogDiv.dialog("open");  
    };
    
    this.loadCurveVariables = function(plotName, subDiagram, callback)
    {
        var params = {de: _this.currentObject.completeName};
        if(subDiagram)
            params.subdiagram = subDiagram;
        
        queryBioUML("web/diagramplot/plot_variables", params,
        function(data)
        {
            callback(data.values);
        },
        function(data)
        {
            logger.error(data.message);
        });
    };

    this.addExperiment = function(plotName, typeName)
    {
        var expDialogDiv = $('<div title="Add ' + typeName + ' to plot"></div>');
        var defaultPath = "";
        if(_this.prevExpPath)
            defaultPath = _this.prevExpPath;
        var property = new DynamicProperty("experimentPath", "data-element-path", defaultPath);
        property.getDescriptor().setDisplayName(resources.dlgPlotEditorPlotPath);
        property.getDescriptor().setReadOnly(false);
        property.setCanBeNull("no");
        property.setAttribute("elementMustExist", true);
        property.setAttribute("dataElementType", "ru.biosoft.table.TableDataCollection");
        property.setAttribute("promptOverwrite", false);
        
        var pathEditor = new JSDataElementPathEditor(property, null);
        pathEditor.setModel(property);
        var plotNode = pathEditor.createHTMLNode();
        pathEditor.setValue(defaultPath);
        pathEditor.addChangeListener (function(control, oldValue, newValue) {
            if (newValue == oldValue) 
            {
                _this.fillVariablesCombos(comboX, _this.xValues, comboY, _this.yValues);
            }
            else 
            {
                _this.updateVariables(newValue, comboX, comboY);
            }
        });
        
        var expDiv = createSinglePropertyControl("Experiment table", $(plotNode));
        expDialogDiv.append(expDiv);
        
        var comboX = $('<select></select>');
        var xDiv = createSinglePropertyControl(resources.dlgPlotEditorAddSeriesX, comboX);
        expDialogDiv.append(xDiv);
        
        var comboY = $('<select></select>');
        var yDiv = createSinglePropertyControl(resources.dlgPlotEditorAddSeriesY, comboY);
        expDialogDiv.append(yDiv);
        
        var variableTitle = $('<input></input>').attr("size", 35);
        var variableTitleDiv = createSinglePropertyControl("Line title", variableTitle);
        expDialogDiv.append(variableTitleDiv);
        
        comboY.change(function() {
            variableTitle.val($(this).val());
        });
        
        var property = new DynamicProperty("Color", "color-selector", null );
        property.getDescriptor().setReadOnly(false);
        property.setCanBeNull("no");
        
        var colorSelector = new JSColorSelector(property, null);
        colorSelector.setModel(property);
        var colorNode = colorSelector.createHTMLNode();
        var color = _this.getNextColor();
        colorSelector.setValue([color]);
        var colorDiv = createSinglePropertyControl("Line color", $(colorNode));
        expDialogDiv.append(colorDiv);
        
        if(_this.xValues != undefined && _this.yValues != undefined)
            _this.fillVariablesCombos(comboX, _this.xValues, comboY, _this.yValues);
        else if(_this.prevExpPath != undefined)
            _this.updateVariables(_this.prevExpPath, comboX, comboY);
        
        expDialogDiv.dialog(
        {
            autoOpen: false,
            width: 400,
            buttons:
            {
                "OK": function()
                {
                    var params = {
                        de: _this.currentObject.completeName,
                        x : comboX.val(),
                        y : comboY.val(),
                        color: $.toJSON(colorSelector.getValue()),
                        source : pathEditor.getValue(),
                        title: variableTitle.val(),
                        plotname: plotName
                    };
                    queryBioUML("web/diagramplot/addexp", params, 
                        function(data)
                        {
                            _this.loadTable(plotName, typeName);
                            _this.prevExpPath = pathEditor.getValue();
                            var color = _this.getNextColor();
                            colorSelector.setValue([color]);
                        },
                        function(data)
                        {
                            logger.error(data.message);
                        });
                    
                    return false;
                },
                "Close": function()
                {
                    $(this).dialog("close");
                    $(this).remove();
                }
            }
        });
        expDialogDiv.dialog("open");  
    };
    
    this.loadVariables= function(completeName, callback)
    {
        queryBioUML("web/plot/variables", 
        {
            "de": completeName
        }, callback); 
    };
    
    this.updateVariables = function(newValue, comboX, comboY)
    {
        _this.loadVariables(newValue, function(data)
        {
            if (data != null) 
            {
                comboX.empty();
                comboY.empty();
                if (data.type == 0) 
                {
                    _this.xValues = data.values.x;
                    _this.yValues = data.values.y;
                    _this.fillVariablesCombos(comboX, _this.xValues, comboY, _this.yValues);
                }
                else 
                {
                    logger.error(data.message);
                }
            }
        });
    };
    
    this.fillVariablesCombos = function (comboX, valuesX, comboY,  valuesY)
    {
        comboX.empty();
        if (valuesX) 
        {
            $.each(valuesX, function(index, value)
            {
                comboX.append($('<option></option>').val(value).text(value));
            });
        }
        comboY.empty();
        if (valuesY) 
        {
            $.each(valuesY, function(index, value)
            {
                comboY.append($('<option></option>').val(value).text(value));
            });
            comboY.trigger("change");
        }
    };
    
    this.removeCurve = function(plotName, typeName)
    {
        var indices = getTableSelectedRowIds(_this.tableObj);
        if(indices == null || indices.length == 0)
            return false;
        
        queryBioUML("web/diagramplot/remove",
        {
            de: _this.currentObject.completeName,
            plotname: plotName,
            what: typeName,
            rows : $.toJSON(indices)
        }, function(data)
        {
            _this.loadTable(plotName, typeName);
        });
    };
    

    this.loadModel = function()
    {
        queryBioUML("web/bean/get", 
            {
                de: "diagram/plot/" + _this.currentObject.completeName
            }, function(data)
            {
                _this.tabDiv.empty().append(_this.containerDiv);
                _this.lockDiv.hide();
                _this.data = data;
                _this.initFromJson(data);
            }, function(data)
            {
                _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
            });
    };
    
    this.reloadModel = function()
    {
        queryBioUML("web/bean/get", 
            {
                de: "diagram/plot/" + _this.currentObject.completeName,
                useCache:false
            }, function(data)
            {
                _this.tabDiv.empty().append(_this.containerDiv);
                _this.lockDiv.hide();
                _this.data = data;
                _this.initFromJson(data);
            }, function(data)
            {
                _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
            });
    };

    this.initFromJson = function(data)
    {
        _this.propertyInspector.empty();
        var beanDPS = convertJSONToDPS(data.values);
        _this.propertyPane = new JSPropertyInspector();
        _this.propertyPane.setParentNodeId(_this.propertyInspector.attr('id'));
        _this.propertyPane.setModel(beanDPS);
        _this.propertyPane.generate();
        _this.propertyPane.addChangeListener(function(ctl,oldval,newval) {
            _this.propertyPane.updateModel();
            var json = convertDPSToJSON(_this.propertyPane.getModel(), ctl);
            _this.setFromJson(json);
        });
    };

    this.setFromJson = function(json)
    {
        _this.lockDiv.show();
        queryBioUML("web/bean/set",
            {
                de: "diagram/plot/" + _this.currentObject.completeName,
                json: json
            }, function(data)
            {
                _this.lockDiv.hide();
                _this.data = data;
                _this.initFromJson(data);
            });
        
    };
    
    this.save = function(){};
    
}

/*
 * Diagram overview view part class
 */
function DiagramOverviewViewPart()
{
    this.tabId = "diagram.overview";
    this.tabName = resources.vpOverviewTitle;
    this.tabDiv = createViewPartContainer(this.tabId);
    this.toReload = false;
    var _this = this;
    
    this.controllerDiv = $('<div class="overview_controller"/>');
    this.tabDiv.append(this.controllerDiv);
    this.selectorDiv = $("<div class='selector_dotes'/>").css({opacity: .4, position: 'absolute'});
    this.controllerDiv.append(this.selectorDiv);
    
    /*
	 * Indicates if view part is visible
	 */
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram || 
        		documentObject instanceof ComplexDocument || documentObject instanceof WorkflowDocument)) 
        {
            callback(true);
        }
        else 
        {
            callback(false);
        }
    };
    
    /*
	 * Open document event handler
	 */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram || documentObject instanceof ComplexDocument || documentObject instanceof WorkflowDocument)) 
        {
        	var newDocument = documentObject instanceof WorkflowDocument && !(this.currentDiagram == documentObject.getDiagram());
            var oldDiagram = this.currentDiagram;
            if(oldDiagram)
            {
                oldDiagram.removeViewAreaListener(this);
            }
            this.currentDiagram = documentObject.getDiagram();
            this.currentDiagram.addViewAreaListener(this);
            if (!this.currentDiagram.dimension) 
            {
                // get dimension from server if it is not defined in diagram
                // document
                queryBioUML("web/diagram",
                {
                	get_dimension: 1,
                    de: this.currentDiagram.jobID ? "jobData/"+this.currentDiagram.jobID+"/diagram" : this.currentDiagram.completeName
                }, function(data)
                {
                    _this.currentDiagram.dimension = data.size;
                    _this.openOverview(false, newDocument);
                });
            }
            else 
            {
                _this.openOverview(true, newDocument);
            }
        }
        else 
        {
            this.tabDiv.html(resources.commonErrorViewpartUnavailable);
        }
    };
    
    /*
	 * Save function
	 */
    this.save = function()
    {
        // nothing to do
    };
	
	/*
	 * Show viewpart event handler
	 */
	this.show = function(documentObject)
    {
        if (this.toReload) 
        {
            this.openOverview(true, false);
        }    
    };
    
    /*
	 * Open diagram picture with known dimension
	 */
    this.openOverview = function(full, newDocument)
    {
        this.controllerDiv.children('img').remove();
        // this.controllerDiv.children('.diagramOverview').remove();
        if(!this.currentDiagram || !this.currentDiagram.dimension) return;
        var width = this.currentDiagram.dimension.width;
        var height = this.currentDiagram.dimension.height;
		if(width == 0 || height == 0) return;
        this.scale = 1.0;
        if (height > 200) 
        {
            this.scale = 200 / height;
        } 
		else if (width > 600) 
        {
            this.scale = 600 / width;
        }
        if(this.currentDiagram  instanceof WorkflowDocument)
            this.scale = 0.75;
        
        this.controllerDiv.css({width: width * this.scale + 4, height: height * this.scale + 4});
        if(this.currentDiagram instanceof Diagram && !browserApp.msie)
        {
        	if(!this.currentDiagram.viewPane.view) return;
			if (!this.viewPaneDiv) 
			{
				this.viewPaneDiv = $('<div class="diagramOverview"></div>');
				this.controllerDiv.prepend(this.viewPaneDiv);
				this.viewPane = new ViewPane(this.viewPaneDiv, {
					dragAxis: 'none',
					fullWidth: false,
					fullHeight: false,
					tile: 20
				});
			}
        	this.viewPane.setView(this.currentDiagram.viewPane.view);
        	this.viewPane.scale(this.scale, this.scale, false);
			if(full) this.viewPane.invalidateTiles();
        	this.viewPane.repaint();
        }
        else if(this.currentDiagram  instanceof WorkflowDocument && !browserApp.msie) 
        {
            if (!this.viewPaneDiv) 
			{
				this.viewPaneDiv = $('<div class="diagramOverview"></div>');
				this.controllerDiv.prepend(this.viewPaneDiv);
				this.viewPane = new ViewPane(this.viewPaneDiv, {
					dragAxis: 'none',
					fullWidth: false,
					fullHeight: false,
					tile: 20
				});
			}
            var queryParams = {
                 type: "json",
                 de: this.currentDiagram.jobID ? "jobData/"+this.currentDiagram.jobID+"/diagram" : this.currentDiagram.completeName,
                 scale: this.scale
            };
            if(!newDocument)
                queryParams['action'] = 'refresh';
            queryBioUMLWatched("overviewViewPart", "web/diagram", queryParams, function(data)
            {
                var diagramView = null;
                if (newDocument) 
                {
                    diagramView = CompositeView.createView(data.values.view, _this.viewPane.getContext());
                    _this.newDocument = false;
                }
                else 
                    diagramView = CompositeView.createView(data.values.view, _this.viewPane.getContext(), _this.viewPane.view);
                
                _this.viewPane.setView(diagramView, true);
                _this.viewPane.scale(_this.scale, _this.scale, false);
		        _this.viewPane.invalidateTiles();
    	        _this.viewPane.repaint();
                _this.selectorDiv.hide();
                this.toReload = false;
            });
        }
        else
        {
            if(this.viewPaneDiv)
                this.viewPaneDiv.remove();
			delete(this.viewPane);
			delete(this.viewPaneDiv);
        	var image = $('<img border="0"/>').attr("src", appInfo.serverPath+"web/diagram?"
				+toURI({rnd: rnd(), de: this.currentDiagram.completeName, xmin: 0, width: Math.ceil(width), ymin: 0, height: Math.ceil(height), scale: this.scale}));
        	this.controllerDiv.append(image);
        }
        
        if(! (this.currentDiagram  instanceof WorkflowDocument))
        {
            this.toReload = false;
            this.selectorDiv.show();
            this.selectorDiv.draggable(
            {
                zIndex: 1000,
                ghosting: false,
                containment: 'parent',
                cursor: 'move',
                start: function(event, ui)
                {
                    _this.dragging = true;
                },
                drag: function(event, ui)
                {
                    var x = parseFloat(_this.selectorDiv.css('left'));
                    var y = parseFloat(_this.selectorDiv.css('top'));
                    if(_this.currentDiagram.setViewArea)
                        _this.currentDiagram.setViewArea(x / _this.scale, y / _this.scale);
                },
                stop: function(event, ui)
                {
                	_this.dragging = false;
                }
            });
            // refresh selector after document selection
            if(this.currentDiagram.viewAreaChanged)
                this.currentDiagram.viewAreaChanged();
        }
        
    };
    
    /*
	 * View area changed event listener
	 */
    this.viewAreaChanged = function(left, top, width, height)
    {
    	if(this.dragging)
    		return;
    	var scale = this.scale;
    	if(!scale) return;
    	// if(this.currentDiagram.zoom) scale *= this.currentDiagram.zoom;
        this.selectorDiv.css('left', left * scale + 1).css('top', top * scale + 1).css('width', width * scale).css('height', height * scale);
    };
    
    /*
	 * Image changed event listener
	 */
    this.imageChanged = function()
    {
        this.openOverview(false, false);
    };
    
    /*
	 * Workaround to reload overview only if DiagramOverviewTab is active
	 */
    this.setToReload = function()
    {
        this.toReload = true;  
    };
}

/*
 * View part for hemodynamic models
 */
function HemodynamicsViewPart ()
{
	createViewPart(this, "hemodynamics.main", resources.vpHemodynamicsTitle);
    this.visible = false;
    var _this = this;
    
    this.progressbar = $('<div/>').css("margin-top", "5pt").css("margin-bottom", "5pt");
    this.tabDiv.prepend(this.progressbar);
    
    this.tableOfVessels = $('<div style="overflow: hidden"></div>').attr("id", this.tabId+'_tbl_cont');
    
    this.containerDiv.append(this.tableOfVessels);
    
    
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram || documentObject instanceof ComplexDocument)) 
        {
            documentObject.getDiagram().checkArterialTree(callback);
        }
        else 
        {
            callback(false);
        }
    };
    
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram || documentObject instanceof ComplexDocument )) 
        {
            if(!this.currentDiagram || this.currentDiagram != documentObject.getDiagram())
            {
                this.currentDiagram = documentObject.getDiagram();
                this.jobID = undefined;
                this.loadVesselsTable();
            }
            
        }
    };
    
    this.loadVesselsTable = function()
    {
    	queryService("hemodynamics.service", 603,
        {
            de: _this.currentDiagram.completeName
        },
        function(data)
        {
            _this.resolverName = data.values;
            var params = {
    	        	cached: _this.resolverName,
    	        	de: _this.currentDiagram.completeName
    	        }; 
	        queryBioUML("web/table/sceleton", params, function(data)
	        {
	            _this.tableOfVessels.html(data.values);
	            _this.table = _this.tableOfVessels.children("table");
	            var features = 
	            {
	                "bProcessing": true,
	                "bServerSide": true,
	                "bFilter": false,
	                "bPaginate": false,
	                "sDom": "pfrlti",
	                "fnDrawCallback": function()
	                {
	                },
	                "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+$.param(params)
	            };
	            _this.table.dataTable(features);
	            _this.table.css('width', '100%');
	        });
        });
    };
 
    
    this.save = function()
    {
        this.saveVesselsTable();
    };
    
    /*
	 * Save Vessels Table
	 */
    this.saveVesselsTable = function (callback)
    {
        if(this.table)
        {    
            var dataDPS = getDPSFromTable(this.table);
            var dataParams = 
                {
                    cached: _this.resolverName,
                    de: _this.currentDiagram.completeName,
                    data: convertDPSToJSON(dataDPS)
                };
            if(_this.table != undefined)
            {
                var tableparams = getTableDisplayParameters(_this.table);
                for(par in tableparams)
                {
                    dataParams[par] = tableparams[par];   
                }
            }
            queryBioUML("web/table/change", dataParams, function(data)
            {
                if(_this.table != undefined)
                {
                    _this.table.fnClearTable( 0 );
                    _this.table.fnDraw();
                    if(callback)
                        callback();
                }
            });
        }
    };
}

/*
 * Diagram layout view part class
 */
function DiagramLayoutViewPart()
{
	createViewPart(this, "diagram.layout", resources.vpLayoutTitle);
    var _this = this;
    
    this.layoutTable = $('<table border=0 width="100%" height="100%"><tr><td width="400px" valign="bottom" id="properties_1"></td><td valign="bottom" id="preview_1"></td></tr><tr><td valign="top" id="properties_2"></td><td align="center" valign="top" id="preview_2"></td></tr></table>');
    this.containerDiv.append(this.layoutTable);
    
    this.layoutTable.find("#properties_1").html("<div>"+resources.commonLoading+"</div>");
    
    this.progressbar = $('<div></div>');
    this.layoutTable.find('#preview_1').append(this.progressbar);
    
    this.previewImage = $('<div></div>');
    this.layoutTable.find('#preview_2').append(this.previewImage);
    
    /*
	 * Indicates if view part is visible
	 */
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram || documentObject instanceof ComplexDocument )) 
        {
        	callback(true);
        }
        else 
        {
            callback(false);
        }
    };
    
    /*
	 * Open document event handler
	 */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram || documentObject instanceof ComplexDocument )) 
        {
        	if(this.currentDiagram != documentObject.getDiagram())
    		{
    			if(this.applyAction) setToolbarButtonEnabled(this.applyAction, false);
    			this.progressbar.hide();
    			this.previewImage.empty();
    		}
            this.currentDiagram = documentObject.getDiagram();

            queryBioUML("web/diagram/layout_info", 
            {
                de: documentObject.getDiagram().completeName
            }, function(data)
            {
                var list = data.values.list;
                var selected = data.values.selected;
                _this.selectLayoutElement = $('<select name="layouter"></select>');
                _this.propertiesPane = $('<span/>').html(resources.vpLayoutPrompt);
                for (i = 0; i < list.length; i++) 
                {
                    if(list[i].length > 0) 
                    {
                        var option = $('<option value="' + list[i] + '">' + list[i] + '</option>');
                        if( selected == list[i] )
                            option.prop('selected', 'selected');
                        _this.selectLayoutElement.append(option);
                    }
                }
                _this.propertiesPane.append(_this.selectLayoutElement);
                _this.layoutTable.find("#properties_1").empty();
                _this.layoutTable.find("#properties_1").append(_this.propertiesPane);
                
                _this.propertyInspector = $('<div id="' + _this.tabId + '_pi"></div>');
                _this.layoutTable.find("#properties_2").empty();
                _this.layoutTable.find("#properties_2").append(_this.propertyInspector);
                
                if( data.values.properties )
                    _this.setLayouterProperties(data.values.properties);
                else
                    _this.openProperties(_this.selectLayoutElement.val());
                _this.selectLayoutElement.change(function()
                {
                    _this.openProperties(_this.selectLayoutElement.val());
                });
            }, function(data)
            {
                _this.propertiesPane.html(resources.vpLayoutErrorCannotLoadLayouters);
                logger.error(data.message);
            });
        }
        else 
        {
            this.tabDiv.html(resources.commonErrorViewpartUnavailable);
        }
    };
    
    /*
	 * Save function
	 */
    this.save = function()
    {
        // nothing to do
    };
    
    /*
	 * Creates toolbar actions for this tab
	 */
    this.initActions = function(toolbarBlock)
    {
        this.runAction = createToolbarButton(resources.vpLayoutButtonRun, "simulate.gif", this.layoutActionClick);
        toolbarBlock.append(this.runAction);

        this.stopAction = createDisabledToolbarButton(resources.vpLayoutButtonStop, "stopLayout.gif", this.stopActionClick);
        toolbarBlock.append(this.stopAction);
        
        this.applyAction = createDisabledToolbarButton(resources.vpLayoutButtonApply, "applyLayout2.gif", this.applyActionClick);
        toolbarBlock.append(this.applyAction);
        
        this.saveAction = createToolbarButton(resources.vpLayoutButtonSave, "saveLayout.gif", this.saveActionClick);
        toolbarBlock.append(this.saveAction);
    };
    
    this.openProperties = function(layouterName)
    {
        queryBean("properties/layout/" + layouterName, {}, function(data) {
            _this.setLayouterProperties(data.values);
        });
    };
    this.setLayouterProperties = function(data)
    {
        _this.propertyInspector.empty();
        var beanDPS = convertJSONToDPS(data);
        _this.propertyPane = new JSPropertyInspector();
        _this.propertyPane.setParentNodeId(_this.propertyInspector.attr('id'));
        _this.propertyPane.setModel(beanDPS);
        _this.propertyPane.generate();
        _this.propertyPane.addChangeListener(function(ctl,oldval,newval) {
            _this.propertyPane.updateModel();
            var json = convertDPSToJSON(_this.propertyPane.getModel(), ctl);
            _this.setFromJson(json);
        });
    };

    this.setFromJson = function(json)
    {
        queryBioUML("web/bean/set",
        {
            de: "properties/layout/" + _this.selectLayoutElement.val(),
            json: json
        }, function(data)
        {
            _this.setLayouterProperties(data.values);
        });
    };
    
    
    this.layoutActionClick = function()
    {
        var dps = _this.propertyPane.getModel();
        var json = convertDPSToJSON(dps);
		this.jobID = rnd();
        queryBioUML("web/diagram/layout", 
        {
            de: _this.currentDiagram.completeName,
            layouter: _this.selectLayoutElement.val(),
            options: json,
			jobID: this.jobID
        }, function(data)
        {
            _this.previewImage.empty();
            var message = $('<p>'+resources.vpLayoutProcessing+'</p>');
            _this.previewImage.append(message);
			_this.progressbar.show();
			setToolbarButtonEnabled(_this.runAction, _this.saveAction, false);
			setToolbarButtonEnabled(_this.stopAction, true);
			createProgressBar(_this.progressbar, _this.jobID, function(status, message)
			{
				_this.layoutFinished(status, message);
			});
        });
    };
	
	this.layoutFinished = function(status, message)
	{
		this.previewImage.empty();
		if (status == JobControl.COMPLETED) 
		{
			var image = $('<img/>').attr("src", appInfo.serverPath+'web/img?' + toURI({
				de: message,
				rnd: rnd()
			}));
			setToolbarButtonEnabled(this.applyAction, true);
			this.previewImage.append(image);
		}
        else 
        {
            this.previewImage.empty();
            var message = $('<p>'+resources.vpLayoutErrorLayouting+'</p>');
            this.previewImage.append(message);
			setToolbarButtonEnabled(this.applyAction, false);
        }
        this.progressbar.hide();
		setToolbarButtonEnabled(this.runAction, this.saveAction, true);
		setToolbarButtonEnabled(this.stopAction, false);
		delete this.jobID;
	};
    
    this.stopActionClick = function()
    {
		cancelJob(this.jobID);
    };
    
    this.applyActionClick = function()
    {
    	setToolbarButtonEnabled(this.applyAction, false);
        var dps = _this.propertyPane.getModel();
        var json = convertDPSToJSON(dps);
        var diagramType = ( this.currentDiagram instanceof Diagram || this.currentDiagram instanceof CompositeDiagram )? "json" : "";
        queryBioUML("web/diagram/layout_apply", 
        {
            de: _this.currentDiagram.completeName,
            layouter: _this.selectLayoutElement.val(),
            options: json,
            type: diagramType
        }, function(data)
        {
        	setToolbarButtonEnabled(_this.applyAction, true);
            if(diagramType == "json")
            {
                _this.currentDiagram.update(data);
            }
            else
            {
                _this.currentDiagram.changeDiagramSize(data.size);
                _this.currentDiagram.invalidateTiles(data.refreshArea);
                _this.currentDiagram.viewChanged();
            }
        }, function(data)
        {
        	setToolbarButtonEnabled(_this.applyAction, true);
        	logger.error(data.message);
        });
    };
    
    this.saveActionClick = function()
    {
        queryBioUML("web/diagram/layout_save", 
        {
            de: _this.currentDiagram.completeName
        }, function(data)
        {
            _this.progressbar.progressbar('value', 0);
            _this.previewImage.empty();
            var image = $('<img src="'+appInfo.serverPath+'web/img?de=layout_preview&rnd=' + rnd() + '">');
            _this.previewImage.append(image);
			setToolbarButtonEnabled(_this.applyAction, true);
        });
    };

    _.bindAll(this, _.functions(this));
}

/*
 * Workflow view part
 */
function WorkflowViewPart()
{
	createViewPart(this, "diagram.workflow.main", resources.vpWorkflowTitle);
    var _this = this;

    this.selectedProperty = undefined;
    this.bindMode = false;
    
    this.progressbar = $('<div/>');
    this.containerDiv.append(this.progressbar);
    this.containerDiv.append('<br>');
    
    this.controlDiv = $('<div></div>');
    this.containerDiv.append(this.controlDiv);
    
    this.log = $("<div/>").height(150).addClass("logArea").hide();
    this.containerDiv.append(this.log);
    
    /*
	 * Save function
	 */
    this.save = function()
    {
        //Stop progress and remove progress data when viewpart is switched off to avoid controls duplication
    	this.progressbar.trigger("destroy");
    	this.progressbar.removeData();
    	this.progressbar.empty();
    };
    
    /*
	 * Indicates if view part is visible
	 */
    this.isVisible = function(documentObject, callback)
    {
    	if ((documentObject != null) && documentObject instanceof Diagram ) 
        {
    		documentObject.getDiagramType(function(type)
            {
            	callback(type.match(/WorkflowDiagramType$/) || type.match(/SedMlDiagramType$/));
            });
        }
        else 
        {
            callback(false);
        }
    };
    
    /*
	 * Open document event handler
	 */
    this.explore = function(documentObject)
    {
    	if(this.currentDiagram)
    	{
    		this.currentDiagram.removeSelectListener(this);
    	}
    	if ((documentObject != null) && documentObject instanceof Diagram ) 
        {
    		this.currentDiagram = documentObject.getDiagram();
    		this.currentDiagram.addSelectListener(this);
            if( this.currentDiagram.runMode == undefined )
                this.currentDiagram.runMode = false;
    		
    		this.propertyInspector = $('<div id="' + _this.tabId + '_pi"></div>');
            this.controlDiv.html(_this.propertyInspector);
            
            this.showStopped();
        }
    };
    
    this.show = function(documentObject)
    {
        this.initRunning();
    };
    
    this.initRunning = function()
    {
    	if(this.currentDiagram.runMode && this.currentDiagram.workflowJobID)
		{
    		this.jobID = this.currentDiagram.workflowJobID;
			this.showRunning();
		}
    };
    
    /*
     * Listener for diagram selection 
     */
    this.selectionChanged = function(nodes)
    {
		if(nodes.length == 0)
		{
			_this.propertyInspector.empty();
			_this.selectedNodeName = undefined;
		}
		else
		{
			if (this.currentDiagram.runMode) 
				return;
			this.log.hide();
			this.progressbar.hide();
			this.controlDiv.show();
			var node = nodes[0];
			if(_this.bindMode && _this.selectedProperty && nodes.length == 1)
			{
				queryBioUML("web/research", {
					action : "bind_parameter",
					de: this.currentDiagram.completeName,
					analysis : _this.selectedNodeName,
					property : _this.selectedProperty,
					variable : node
				}, function() {
					getDataCollection(_this.currentDiagram.completeName).fireChanged();
				});
				_this.selectNodeProperty();
				return;
			}
			this.selectedNodeName = node;
			queryBean("properties/workflow/" + this.currentDiagram.completeName + "/" + node, {showMode: SHOW_EXPERT, ignoreErrors: true},
				this.initNodeProperties);
		}
	};
    
    this.selectNodeProperty = function(model)
    {
		_this.selectedProperty = model?model.getAttribute("completeName"):undefined;
		_this.bindMode = false;
        if(!_this.bindParameterAction)
            return;
        setToolbarButtonEnabled(_this.bindParameterAction, Boolean(_this.selectedProperty));
    };
    
    this.initNodeProperties = function(data)
    {
		_this.propertyInspector.empty();
		_this.selectNodeProperty();
	    if (data.type == 0) 
	    {
	    	var beanDPS = convertJSONToDPS(data.values);
	        _this.propertyPane = new JSPropertyInspector();
	        _this.propertyPane.setParentNodeId(_this.propertyInspector.attr('id'));
	        _this.propertyPane.setModel(beanDPS);
	        _this.propertyPane.generate();
	        if(instanceOf(getDataCollection(this.currentDiagram.completeName).getChildClass(this.selectedNodeName), "biouml.model.Compartment"))
	        	makePropertyInspectorSelectable(_this.propertyPane, function(model) {_this.selectNodeProperty(model);});
	        _this.propertyPane.addChangeListener(function(control, oldValue, newValue) {
	        	_this.propertyPane.updateModel();
	            var json = convertDPSToJSON(_this.propertyPane.getModel(), control.getModel().getName());
	            queryBioUML("web/bean/set", 
	            {
                    showMode: SHOW_EXPERT,
	                de: "properties/workflow/"+_this.currentDiagram.completeName + "/" + _this.selectedNodeName,
	    			json: json
	            }, function(data)
	            {
			        if(control.getModel().getName()=="name")
			        {
			        	_this.selectedNodeName = control.getValue(); 
			        	getDataCollection(_this.currentDiagram.completeName).fireChanged();
			        }
                    else if(control.getModel().getName()=="script")
                    {
                        getDataCollection(_this.currentDiagram.completeName).fireChanged();
                    }
                    _this.initNodeProperties(data);
	            });
			});
	    }
    };
    
    /*
	 * Creates toolbar actions for this tab
	 */
    this.initActions = function(toolbarBlock)
    {
        this.runAction = createToolbarButton(resources.vpWorkflowButtonRun, "simulate.gif", this.runActionClick);
        toolbarBlock.append(this.runAction);
        
        this.stopAction = createToolbarButton(resources.vpWorkflowButtonStop, "stopLayout.gif", this.stopActionClick);
        toolbarBlock.append(this.stopAction);
        
        this.bindParameterAction = createToolbarButton(resources.vpWorkflowButtonBind, "bindparameter.gif", this.bindParameterActionClick);
        toolbarBlock.append(this.bindParameterAction);
        
        this.returnAction = createToolbarButton(resources.vpWorkflowButtonReturn, "closeAndReturn.gif", this.returnActionClick);
        toolbarBlock.append(this.returnAction);
   };
    
    this.bindParameterActionClick = function()
    {
    	this.bindMode = true;
    };
    
    this.runActionClick = function()
    {
    	queryBean("workflow/parameters/" + this.currentDiagram.completeName, {showMode: SHOW_EXPERT, useCache: "no", ignoreErrors: true}, 
        function(data)
    	{
    		if(data.values.length == 0)
            {
                _this.startWorkflow();
            }
            else
            {
                var dialogDiv = $('<div/>').attr("title", resources.vpWorkflowParametersDialogTitle);
                var parentID = "property_inspector_dialog_" + rnd();
                var parentDiv = $('<div id="' + parentID + '"></div>');
                dialogDiv.append(parentDiv);
                $(document.body).append(dialogDiv);
                _this.initWorkflowParameters(data, parentDiv);
                
                dialogDiv.append($("<hr/>"));
                var researchSaveID = "property_inspector_research_save_" + rnd();
                var researchSaveDiv = $('<div id="' + researchSaveID + '"></div>');
                dialogDiv.append(researchSaveDiv);
                var saveResearchPropertyPane = new JSPropertyInspector();
                var researchDPS = new DynamicPropertySet();
                var researchPath = getElementPath(_this.currentDiagram.completeName);
                if(_this.currentDiagram.completeName.match(/^analyses/))
                    researchPath = getDefaultProjectDataPath();
                var property = new DynamicProperty("researchPath", "data-element-path", createPath(researchPath, getElementName(_this.currentDiagram.completeName)+" research"));
                property.getDescriptor().setDisplayName(resources.vpWorkflowParametersDialogSaveResearch);
        		property.setAttribute("dataElementType", "biouml.model.Diagram");
        		property.setAttribute("promptOverwrite", true);
        		property.setCanBeNull("yes");
                researchDPS.add(property);
                saveResearchPropertyPane.setParentNodeId(researchSaveID);
                saveResearchPropertyPane.setModel(researchDPS);
                saveResearchPropertyPane.generate();
                _this.saveResearchPropertyPane = saveResearchPropertyPane;
                dialogDiv.dialog(
                {
                    autoOpen: false,
                    width: 500,
                    modal: true,
                    buttons:
                    {
                        "Ok": function()
                        {
                            var dps = _this.parametersPropertyPane.getModel();
    		                var json = convertDPSToJSON(dps);
                            var saveResearchPath = _this.saveResearchPropertyPane.getModel().getAllProperties()["researchPath"].getValue();
                            if(saveResearchPath == NONE_VALUE) saveResearchPath = "";
                            _this.startWorkflow(json, saveResearchPath);
                            $(this).dialog("close");
                            $(this).remove();
                        },
                        "Cancel": function()
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
    	});
        
    };
    
    this.initWorkflowParameters = function(data, parentDiv)
    {
        var beanDPS = convertJSONToDPS(data.values);
        _this.parametersPropertyPane = new JSPropertyInspector();
        parentDiv.empty();
        _this.parametersPropertyPane.setParentNodeId(parentDiv.get(0).id);
        _this.parametersPropertyPane.setModel(beanDPS);
        _this.parametersPropertyPane.generate();
        _this.parametersPropertyPane.addChangeListener(function(control, oldValue, newValue) {
	    	_this.parametersPropertyPane.updateModel();
	        var json = convertDPSToJSON(_this.parametersPropertyPane.getModel(), control);
	        queryBioUML("web/bean/set", 
	        {
	            de: "workflow/parameters/" + _this.currentDiagram.completeName,
	            showMode: SHOW_EXPERT,
				json: json
	        }, function(data)
	        {
				_this.initWorkflowParameters(data, parentDiv);
	        });
	    });
    };
    
    this.startWorkflow = function ( json, saveResearchPath )
    {
        _this.jobID = rnd();
        var requestParameters ={
            de: _this.currentDiagram.completeName,
            jobID: _this.jobID
        };
        if(saveResearchPath)
            requestParameters["researchPath"] = saveResearchPath;
            
        if(json)
            requestParameters["json"] = json;
        _this.log.html("");
        _this.opened = {};    
        queryBioUML("web/research/start_workflow", requestParameters,
        function(data)
        {
        	_this.currentDiagram.runMode = true;
        	_this.currentDiagram.workflowJobID = _this.jobID;
            if(saveResearchPath)
        	{
        		refreshTreeBranch(getElementPath(saveResearchPath));
        	}
            _this.showRunning();
        }, function(data)
        {
            _this.currentDiagram.runMode = false;
            logger.error(data.message);
        });
    };
    
    this.showRunning = function()
    {
    	_this.log.show();
    	_this.progressbar.show();
    	_this.controlDiv.hide();
    	setToolbarButtonEnabled(_this.runAction, false);
    	createProgressBar(_this.progressbar, _this.jobID, function(status, message, pathToOpen)
		{
			getDataCollection(_this.currentDiagram.completeName).fireChanged();
            updateLog(_this.log, message);
            
            _this.workflowFinished(status, message, pathToOpen);
		}, function(status, message, pathToOpen)
		{
			updateLog(_this.log, message);
            _this.openResults(pathToOpen);
		});
    }
    
    this.showStopped = function()
    {
    	_this.log.hide();
    	_this.progressbar.hide();
    	_this.controlDiv.show();
    	setToolbarButtonEnabled(_this.runAction, true);
    }
	
	this.workflowFinished = function(status, message, pathsToOpen)
	{
		if (status == JobControl.COMPLETED) 
		{
			logger.message(resources.vpWorkflowComplete);
            if(pathsToOpen)
            {
                this.openResults(pathsToOpen);
            }
		} else
		{
			logger.message(resources.vpWorkflowFailed.replace("{error}", message));
		}
		delete _this.jobID;
		_this.currentDiagram.runMode = false;
		delete _this.currentDiagram.workflowJobID;
		setToolbarButtonEnabled(_this.runAction, true);
	};
    
    this.openResults = function(pathsToOpen)
    {
        if(!pathsToOpen)
            return;
        if(pathsToOpen.length > 0)
	       	refreshTreeBranch(pathsToOpen[0]);
        for(var i = 0; i < pathsToOpen.length; i++)
        {
            if (this.opened[pathsToOpen[i]] == undefined) 
            {
                this.opened[pathsToOpen[i]] = true;
                (function(i)
                {
                    setTimeout(function()
                    {
                        openDocument(pathsToOpen[i]);
                    }, 200 * (i));
                })(i);
            }
        }
    };
    
    this.returnActionClick = function()
    {
    	var name = _this.currentDiagram.completeName;
    	_this.currentDiagram.save(function() {
        	closeDocument(_this.currentDiagram.tabId);
        	openDocument(name);
    	});
    };
    
    this.stopActionClick = function()
    {
		_this.currentDiagram.runMode = false;
        cancelJob(_this.jobID);
    };
    
    this.saveElementOptions = function(callback)
    {
    	if(_this.propertyPane && _this.selectedNodeName)
    	{
    		var dps = _this.propertyPane.getModel();
    		var json = convertDPSToJSON(dps);
    		queryBioUML("web/bean/set", 
    	    {
    			de: "properties/workflow/" + _this.currentDiagram.completeName + "/" + _this.selectedNodeName,
    			json: json
    	    }, function(data)
    	    {
	    		if(callback != null)
	    		{
	    			callback();
	    		}
    	    });
    	}
    	else
    	{
    		if(callback != null)
	    	{
	    		callback();
	    	}
    	}
    };
    
    _.bindAll(this, _.functions(this));
}

/*
 * Journal History view part
 */
function JournalViewPart()
{
	createViewPart(this, "history.main", resources.vpJournalTitle);
    var _this = this;
    
    this.table = $('<div>'+resources.commonLoading+'</div>');
    this.containerDiv.append(this.table);
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && documentObject instanceof Diagram ) 
        {
            documentObject.getDiagramType(function(type)
            {
            	callback(type.match(/(Workflow|Research)DiagramType$/));
            });
        }
        else 
        {
            this.currentDiagram = null;
            callback(false);
        }
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && documentObject instanceof Diagram ) 
        {
            if (!this.currentDiagram || this.currentDiagram != documentObject.getDiagram()) 
            {
                this.currentDiagram = documentObject.getDiagram();
                this.loadTable();
            }
            else if (this.tableObj != undefined)
            {
                //renew click listeners
                setRowSelectionListeners(this.tableObj);
            }
        }
    };
    
    this.loadTable = function()
    {
        this.table.html('<div>'+resources.commonLoading+'</div>');
        var params = {
                "de": this.currentDiagram.completeName,
                type: "journal",
                add_row_id: 1
            };
        queryBioUML("web/table/sceleton", params, 
        function(data)
        {
            _this.table.html(data.values);
            _this.tableObj = _this.table.children("table");
            _this.tableObj.addClass('selectable_table');
            params["rnd"] = rnd();
            params["read"] = true;
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "sPaginationType": "full_numbers_no_ellipses",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sDom": "pfrlti",
                "aaSorting": [[ 2, "desc" ]],	// sort by time (descending)
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?" + toURI(params) 
            };
            _this.tableObj.dataTable(features);
            _this.tableObj.css('width', '100%');
        }, function(data)
        {
            _this.table.html(resources.commonErrorViewpartUnavailable);
            _this.tableObj = null;
            logger.error(data.message);
        });
    };
    
    /*
     * Save function
     */
    this.save = function()
    {
        // nothing to do
    };
    
    this.initActions = function(toolbarBlock)
    {
        this.pasteAction = createToolbarButton(resources.vpJournalButtonPaste, "insertClipboard.gif", this.pasteActionClick);
        toolbarBlock.append(this.pasteAction);
        
        this.removeAction = createToolbarButton(resources.vpJournalButtonRemove, "removefilter.gif", function()
        {
            _this.removeRows("selected");
        });
        toolbarBlock.append(this.removeAction);
        
        this.removeAllAction = createToolbarButton(resources.vpJournalButtonRemoveAll, "removefilter.gif", function()
        {
            createConfirmDialog(resources.vpJournalConfirmRemoveAll, _this.removeRows);
        });
        toolbarBlock.append(this.removeAllAction);
    };
    
    this.pasteActionClick = function()
    {
        var pasteRows = getTableSelectedRowIds(this.tableObj);
        if( pasteRows.length > 0 )
        {
            var diagramType = ( this.currentDiagram instanceof Diagram || this.currentDiagram instanceof CompositeDiagram )? "json" : "";
            var dataParams = {
                    de: _this.currentDiagram.completeName,
                    jsonrows: $.toJSON(pasteRows),
                    type: diagramType
                };
              var _thisVp = this;
              _this.currentDiagram.selectControl(function(event){
                    var point = _thisVp.currentDiagram.getEventPoint(event);
                    dataParams["x"] = point.x;
                    dataParams["y"] = point.y;
                    queryBioUML("web/journal/add", dataParams, function(data)
                    {
                        clearSelection(_thisVp.tableObj);
                        var _this = _thisVp.currentDiagram;
                        _this.reloadChanged(data.values);
                    	_this.restoreSelection();
                    });
                    _thisVp.currentDiagram.selectControl(function(event){return true;});
                    return false;
                });
        }
    };
    
    this.removeRows = function (mode)
    {
        var dataParams = {
                    de: _this.currentDiagram.completeName
                };
        if( mode == "selected" )
        {
            var deleteRows = getTableSelectedRowIds(this.tableObj);
            if( deleteRows.length > 0 )
            {
                dataParams["jsonrows"] = $.toJSON(deleteRows);
            }
            else
            {
                return;
            }
        }
        queryBioUML("web/journal/remove", dataParams, function(data)
        {
            _this.loadTable();
        });
    };
    
    _.bindAll(this, _.functions(this));
}

function VersionHistoryViewPart()
{
	createViewPart(this, "common.versions", resources.vpVersionTitle);
	var _this = this;
	this.sceleton = $("<table width='100%'><tr><td width='50%' valign='top'><td width='50%' valign='top'></table>");
	this.tableBlock = this.sceleton.find("td").eq(0);
	this.infoBlock = this.sceleton.find("td").eq(1);
	this.containerDiv.append(this.sceleton);

    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if (documentObject != null && documentObject.getHistory && documentObject.getHistory() !== undefined) 
        {
        	callback(true);
        }
        else 
        {
            callback(false);
        }
    };
    
    this.initActions = function(toolbarBlock)
    {
        this.applyAction = createDisabledToolbarButton(resources.vpJournalButtonPaste, "ok.gif", this.applyActionClick);
        toolbarBlock.append(this.applyAction);
        
        this.revertAction = createDisabledToolbarButton(resources.vpJournalButtonPaste, "cancel.gif", this.revertActionClick);
        toolbarBlock.append(this.revertAction);
    };
    
    this.applyActionClick = function()
    {
    	if(this.document.revertToVersion)
    	{
    		this.document.revertToVersion(this.leftRow.data("version"));
    	}
    };
    
    this.revertActionClick = function()
    {
    	this.selectRow(this.rows.eq(0));
    };
    
    this.fillHistory = function(history)
    {
    	this.table = $("<table cellspacing='0'/>").css({width: "100%", cursor: "pointer"});
    	
    	_.each([{name: resources.vpVersionCurrentItem, user: "", version: -1}].concat(history), function(row)
    	{
    		var tr = $("<tr><td class='historyLeft'>&#x25BA;<td><td><td><td class='historyRight'>&#x25C4;</tr>");
    		tr.children().eq(1).text(row.user);
    		tr.children().eq(2).text(row.name);
    		if(row.time)
    		{
    			var date = new Date(row.time);
    			tr.children().eq(3).text(date.toString().replace(/GMT.+/, ""));
    		}
    		tr.data("version", row.version);
    		tr.data("comment", row.comment);
    		this.table.append(tr);
    	}, this);
    	
    	this.rows = this.table.find("tr");
    	this.selectRow(this.rows.eq(0));
    	this.table.delegate("tr", "click", function(event)
    	{
    		if($(event.target).hasClass("historyLeft"))
    		{
    			_this.selectRow($(this), _this.rightRow);
    		} else if($(event.target).hasClass("historyRight"))
    		{
    			_this.selectRow(_this.leftRow, $(this));
    		} else
    		{
    			_this.selectRow($(this));
    		}
    	});
    	
    	this.tableBlock.empty().append(this.table);
    };
    
    this.selectRow = function(row, row2)
    {
    	if(row2 == undefined) row2 = row;
    	this.leftRow = row;
    	this.rightRow = row2;
		this.rows.removeClass("historySelected");
    	this.rows.find(".historyLeft").css("color", "lightgray");
    	this.rows.find(".historyRight").css("color", "lightgray");
    	var from = this.rows.index(row);
    	var to = this.rows.index(row2);
    	if(from > to)
    	{
    		var tmp = from;
    		from = to;
    		to = tmp;
    	}
		this.rows.slice(from, to+1).addClass("historySelected");
    	row.find(".historyLeft").css("color", "black");
    	row2.find(".historyRight").css("color", "black");
		
		this.infoBlock.empty();
		
		if(row.get(0) == row2.get(0))
		{
			if(row.data("comment") !== undefined)
				this.infoBlock.append($(resources.vpVersionCommentTitle)).append($("<div/>").text(row.data("comment")));
			if(this.applyAction && this.revertAction)
				setToolbarButtonEnabled(this.applyAction, this.revertAction, row.data("version") > -1);
			if(row.data("version") == -1)
			{
		    	if(this.document.cancelChangesMode)
		    	{
		    		this.document.cancelChangesMode(true);
		    	}
			}
			else
			{
				if(this.document.showVersion)
				{
					this.document.showVersion(row.data("version"));
				}
			}
		} else
		{
			if(this.applyAction && this.revertAction)
			{
				setToolbarButtonEnabled(this.revertAction, true);
				setToolbarButtonEnabled(this.applyAction, false); 
			}
			if(this.document.showVersion)
			{
				this.document.showVersion(row.data("version"), row2.data("version"));
			}
		}
    };

    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if (documentObject != null && documentObject.getHistory) 
        {
        	this.document = documentObject;
        	this.fillHistory(documentObject.getHistory());
        }
    };

    /*
     * Save function
     */
    this.save = function()
    {
        // nothing to do
    };
    
    _.bindAll(this, _.functions(this));
}

/*
 * Diagram filter view part
 */
function DiagramFilterViewPart()
{
	createViewPart(this, "diagram.expression", resources.vpDiagramFilterTitle);
    var _this = this;
    
    this.currentObject = null;
    this.currentFilter = null;

    this.filterCombo = $('<div>'+resources.commonLoading+'</div>');
    this.containerDiv.append(this.filterCombo);
    this.filterProperties = $('<div id="diagramFilterProperties"/>');
    this.containerDiv.append(this.filterProperties);
    
    this.defaultProperties = $("<div/>").css("padding", "1em").css("color", "gray").text(resources.vpDiagramFilterQuickMappingHint);
    this.filterProperties.append(this.defaultProperties);
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    { 
    	if ((documentObject != null) && (documentObject instanceof Diagram || documentObject instanceof ComplexDocument ))
        {
            var name = documentObject.completeName;
			documentObject.getDiagramType(function(type)
			{
			    callback(!type.match(/(Workflow|Research|Annotation)DiagramType$/));
			});
        }
        else
        {
            callback(false);
        }
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && documentObject instanceof Diagram) 
        {
            abortQueries("filterViewPart");
        	this.currentObject = documentObject;
        	this.loadList();
        }
    };
    
    this.fillList = function(values)
    {
    	var selector = $("<select><option value=''>"+resources.vpDiagramFilterNoMapping+"</option></select>");
    	this.currentFilter = "";
    	for(var i=0; i<values.length; i++)
		{
    		var option = $("<option/>").val(values[i].name).text(values[i].name);
    		if(values[i].active)
    		{
    			option.attr("selected", "selected");
    			this.currentFilter = values[i].name;
    		}
    		selector.append(option);
		}
    	this.filterCombo.text(resources.vpDiagramFilterMappingPrompt).append(selector);
    	selector.change(function() {
    		if(selector.val() == _this.currentFilter) return;
        	queryBioUMLWatched("filterViewPart", "web/diagramFilter/select",
			{
	    		de: _this.currentObject.completeName,
	    		name: selector.val()
			},
			function(data)
			{
				_this.fillList(data.values);
				_this.updateProperties();
				_this.currentObject.dataCollectionChanged();
			});
    	});
    };
    
    this.loadList = function()
    {
    	var name = this.currentObject.completeName;
        queryBioUMLWatched("filterViewPart", "web/diagramFilter/list", { de: name },
		function(data)
		{
            _this.fillList(data.values);
			_this.updateProperties();
		});
    };
    
    /*
     * Save function
     */
    this.save = function()
    {
        // nothing to do
    };
    
    this.updateProperties = function()
    {
    	if(!_this.currentObject || _this.currentFilter == "")
   		{
    		_this.filterProperties.empty().append(_this.defaultProperties);
    		return;
   		}
    	disableDPI(_this.filterProperties);
    	queryBioUMLWatched("filterViewPart", "web/bean/get",
    	{
    		de: "properties/filter/" + _this.currentObject.completeName + "/" + _this.currentFilter,
            showMode: SHOW_EXPERT
    	}, function(data)
    	{
    		_this.initFilterProperties(data, _this.filterProperties);
    	});
    };
    
    this.initFilterProperties = function(data, parentDiv)
    {
        var beanDPS = convertJSONToDPS(data.values);
        _this.parametersPropertyPane = new JSPropertyInspector();
        parentDiv.empty();
        _this.parametersPropertyPane.setParentNodeId(parentDiv.get(0).id);
        _this.parametersPropertyPane.setModel(beanDPS);
        _this.parametersPropertyPane.generate();
        _this.parametersPropertyPane.addChangeListener(function(control, oldValue, newValue) {
	    	_this.parametersPropertyPane.updateModel();
	        var json = convertDPSToJSON(_this.parametersPropertyPane.getModel(), control);
	        disableDPI(parentDiv);
	        queryBioUMLWatched("filterViewPart", "web/bean/set", 
	        {
	            de: "properties/filter/" + _this.currentObject.completeName + "/" + _this.currentFilter,
	            showMode: SHOW_EXPERT,
				json: json
	        }, function(data)
	        {
				_this.initFilterProperties(data, parentDiv);
				_this.currentObject.dataCollectionChanged();
	        });
	    });
    };
    
    this.initActions = function(toolbarBlock)
    {
        this.addAction = createToolbarButton(resources.vpDiagramFilterButtonNew, "icon_plus.gif", this.addActionClick);
        toolbarBlock.append(this.addAction);
        
        this.removeAction = createToolbarButton(resources.vpDiagramFilterButtonRemove, "removefilter.gif", this.removeActionClick);
        toolbarBlock.append(this.removeAction);
    };
    
    this.addActionClick = function()
    {
    	createPromptDialog(resources.vpDiagramFilterNewDialogTitle, resources.vpDiagramFilterNewDialogPrompt, function(filterName)
    	{
    		queryBioUMLWatched("filterViewPart", "web/diagramFilter/add",
    		{
    			de: _this.currentObject.completeName,
    			name: filterName
    		}, function(data)
    		{
				_this.fillList(data.values);
				_this.updateProperties();
				_this.currentObject.dataCollectionChanged();
    		});
    	});
    };
    
    this.removeActionClick = function ()
    {
    	if(this.currentFilter == "") return;
    	createConfirmDialog(resources.vpDiagramFilterConfirmRemove.replace("{filter}", this.currentFilter), function() {
    		queryBioUMLWatched("filterViewPart", "web/diagramFilter/remove",
    		{
    			de: _this.currentObject.completeName,
    			name: _this.currentFilter
    		}, function(data)
    		{
				_this.fillList(data.values);
				_this.updateProperties();
				_this.currentObject.dataCollectionChanged();
    		});
    	});
    };
    
    _.bindAll(this, _.functions(this));
}

/*
 * Diagram units view part
 */
function DiagramUnitsViewPart()
{
    this.tabId = "diagram.units";
    this.tabName = resources.vpUnits;
    this.diagram = null;
    this.data = null;
    this.tabDiv = createViewPartContainer(this.tabId);
    this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
    this.tabDiv.append(this.containerDiv);      
    this.propertyInspector = $('<div id="' + this.tabId + '_pi"></div>');
    this.containerDiv.append(this.propertyInspector);

    var _this = this;
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram))
        {
            if( documentObject.getDiagram().getModelObjectName() != null)
            {
                callback(true);
            }
            else
            {
                callback(false);    
            }
        }
        else 
            callback(false);
    };

    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
            this.diagram = documentObject.getDiagram();
            this.loadModel();
        }
    };
    
    
    this.show = function()
    {
        this.loadModel();
    };

    this.loadModel = function()
    {
        queryBioUML("web/bean/get", 
            {
                de: "diagram/units/" + _this.diagram.completeName
            }, function(data)
            {
                _this.tabDiv.empty().append(_this.containerDiv);
                _this.data = data;
                _this.initFromJson(data);
            }, function(data)
            {
                _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
            });
    };

    this.initFromJson = function(data)
    {
        _this.propertyInspector.empty();
        var beanDPS = convertJSONToDPS(data.values);
        _this.propertyPane = new JSPropertyInspector();
        _this.propertyPane.setParentNodeId(_this.propertyInspector.attr('id'));
        _this.propertyPane.setModel(beanDPS);
        _this.propertyPane.generate();
        _this.propertyPane.addChangeListener(function(ctl,oldval,newval) {
            _this.propertyPane.updateModel();
            var json = convertDPSToJSON(_this.propertyPane.getModel(), ctl);
            _this.setFromJson(json);
        });
    };

    this.setFromJson = function(json)
    {
        queryBioUML("web/bean/set",
            {
                de: "diagram/units/" + _this.diagram.completeName,
                json: json
            }, function(data)
            {
                _this.initFromJson(data);
            });
    };
    
    this.save = function(){};
    this.initActions = function(toolbarBlock){};  
}

function DiagramUnitsViewPartNew()
{
    this.tabId = "diagram.units2";
    this.tabName = resources.vpUnits;
    this.diagram = null;
    this.tableChanged = false;

    var _this = this;
    
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);      
        this.table = $('<div>'+resources.vpModelParametersLoading.replace("{type}", "units")+'</div>');
        this.containerDiv.append(this.table);
    };
    
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram))
        {
            if( documentObject.getDiagram().getModelObjectName() != null)
            {
                callback(true);
            }
            else
            {
                callback(false);    
            }
        }
        else 
            callback(false);
    };

    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
            this.diagram = documentObject.getDiagram();
            this.tableObj = undefined;
            this.loadTable();
        }
    };
    
    
    this.show = function()
    {
        this.loadTable();
    };

    this.loadTable = function()
    {
        this.table.html('<div>'+resources.vpModelParametersLoading.replace("{type}", "units")+'</div>');
        
        queryBioUML("web/table/sceleton",
        {
            "de": _this.diagram.completeName,
            "read": true,
            "type": "model",
            "add_row_id" : 1,
            "tabletype": "units"
        }, 
        function(data)
        {
            _this.table.html(data.values);
            _this.tableObj = _this.table.children("table");
            
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "sPaginationType": "full_numbers_no_ellipses",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sDom": "pfrlti",
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+toURI({de: _this.diagram.completeName, read: true, rnd: rnd(), type: "model", tabletype: "units", add_row_id:1}),
                "fnDrawCallback": function( nRow, aData, iDisplayIndex ) {
                    addTableChangeHandler(_this.tableObj, function(ctrl){
                        _this.tableChanged = true;
                    });
                },
            };
            _this.tableObj.addClass('selectable_table');
            _this.tableObj.dataTable(features);
            _this.tableObj.css('width', '100%');
            _this.tableChanged = false;
            
        }, function(data)
        {
            _this.table.html(resources.commonErrorViewpartUnavailable);
            _this.tableObj = null;
            logger.error(data.message);
        });
    };
    
    this.initActions = function(toolbarBlock)
    {
        this.addAction = createToolbarButton("Add unit", "icon_plus.gif", this.addActionClick);
        toolbarBlock.append(this.addAction);
        
        this.editAction = createToolbarButton("Edit unit", "edit.gif", this.editActionClick);
        toolbarBlock.append(this.editAction);
        
        this.removeAction = createToolbarButton("Remove unit", "removefilter.gif", this.removeActionClick);
        toolbarBlock.append(this.removeAction);
    };
    
    this.addActionClick = function ()
    {
        var addParams = {
            de: _this.diagram.completeName,
        };
        queryBioUML("web/units/add_new", addParams,
        function(data)
        {
            _this.editUnit(data.values, true);
        },
        function(data)
        {
            logger.error(data.message);
        });
    };
    
    this.editActionClick = function()
    {
        var selectedRows = getTableSelectedRowIds(_this.tableObj);
        if(selectedRows.length==0)
            return;
        _this.editUnit(selectedRows[0], false);
    };
    
    this.editUnit = function (name, isNew)
    {
        let idarr = getTableRowIds(_this.tableObj);
        var names = idarr.map (v => v[1]);
        this.createBeanEditorDialog(isNew ? "Add unit" : "Edit unit", "diagram/units/" + _this.diagram.completeName + "/" + name, isNew, names);
    };
    
    this.removeActionClick = function()
    {
        var selectedRows = getTableSelectedRowIds(_this.tableObj);
        if(selectedRows.length==0)
            return;
        var selectedStr = selectedRows.join(',');
        queryBioUML("web/units/remove",
        {
            de: _this.diagram.completeName,
            "names": selectedStr
        }, function(data)
        {
            _this.diagram.setChanged(true);
            _this.loadTable();
        },
        function(data)
        {
            logger.error(data.message);
        });
    };
    
    this.save = function()
    {
        
    };
    
    this.createBeanEditorDialog = function (title, beanPath, isNew, names)
    {
        var propertyPane = new JSPropertyInspector();
        var parentID = "property_inspector_dialog_" + rnd();
        var origData;
        var wasChanged = false;
        var oldName = getElementName(beanPath);
        const index = names.indexOf(oldName);
        if (index > -1) {
            names.splice(index, 1);
        }
        
        queryBean(beanPath, {}, function(data)
        {
            function changeListener(control, oldValue, newValue) 
            {
                let newBeanPath = undefined;
                if(control.getModelName()=="name")
                {
                    if(isUsedName(newValue))
                    {
                        logger.error("Unit with name '" + newValue + "' already exists. Use 'Edit unit' instead.");
                        control.updateView();
                        return;
                    }
                    newBeanPath = getElementPath(beanPath) + "/" + newValue;
                }
                syncronizeData(control, newBeanPath);
            }
            
            function isUsedName (name)
            {
                return names.includes(name);
            }
            
            function syncronizeData(control, newBeanPath)
            {
                queryBioUML("web/bean/set", 
                {
                    de: beanPath,
                    json: convertDPSToJSON(propertyPane.getModel(), control)
                }, function(data)
                {
                    wasChanged = true;
                    if(newBeanPath)
                    {
                        beanPath = newBeanPath;
                        queryBean(beanPath, {}, function(data)
                        {
                            $(getJQueryIdSelector(parentID)).empty();
                            var beanDPS = convertJSONToDPS(data.values);
                            propertyPane = new JSPropertyInspector();
                            propertyPane.setParentNodeId(parentID);
                            propertyPane.setModel(beanDPS);
                            propertyPane.generate();
                            propertyPane.addChangeListener(changeListener);
                        });
                    }
                    else
                    {
                        $(getJQueryIdSelector(parentID)).empty();
                        var beanDPS = convertJSONToDPS(data.values);
                        propertyPane = new JSPropertyInspector();
                        propertyPane.setParentNodeId(parentID);
                        propertyPane.setModel(beanDPS);
                        propertyPane.generate();
                        propertyPane.addChangeListener(changeListener);
                    }
                });
            }
            
            var beanDPS = convertJSONToDPS(data.values);
            origData = convertDPSToJSON(beanDPS);
            
            var dialogDiv = $('<div title="'+title+'"></div>');
            dialogDiv.append('<div id="' + parentID + '"></div>');
            var closeDialog = function()
            {
                dialogDiv.dialog("close");
                dialogDiv.remove();
            };
            dialogDiv.dialog(
            {
                autoOpen: false,
                width: 500,
                height: 500,
                buttons: 
                {
                    "Cancel": function()
                    {
                        var newName = getElementName(beanPath);
                        if(isNew)
                        {
                            queryBioUML("web/units/remove",
                            {
                                de: _this.diagram.completeName,
                                "names": newName
                            }, function(data)
                            {
                                closeDialog();
                            },
                            function(data)
                            {
                                closeDialog();
                                logger.error(data.message);
                            });
                        }
                        else
                        {
                            if(wasChanged)
                            {
                                queryBioUML("web/bean/set",
                                {
                                    de: beanPath,
                                    json: origData
                                }, closeDialog, closeDialog);
                            } else closeDialog();
                        }
                    },
                    "Save": function()
                    {
                        var data = convertDPSToJSON(propertyPane.getModel());
                        queryBioUML("web/bean/set", 
                        {
                            de: beanPath,
                            json: data
                        }, function()
                        {
                            closeDialog();
                            _this.diagram.setChanged(true);
                            var vp = lookForViewPart("diagram.variables");
                            if(vp)
                                vp.loadTable();
                            _this.loadTable();
                        }, closeDialog);
                    }
                }
            });
            addDialogKeys(dialogDiv);
            sortButtons(dialogDiv);
            dialogDiv.dialog("open");
            
            propertyPane.setParentNodeId(parentID);
            propertyPane.setModel(beanDPS);
            propertyPane.generate();
            propertyPane.addChangeListener(changeListener);
        });
    };
}

function FbcViewPart(){
	
    createViewPart(this, "diagram.fbc", resources.vpFbc);
    var _this = this;
    
    this.table = $('<div>'+resources.commonLoading+'</div>');
    this.containerDiv.append(this.table);
    this.params = undefined;
    this.isActive = false;
    this.needUpdate = false;
    
	this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram))
        {
            if( documentObject.getDiagram().getModelObjectName() != null)
            {
                cdocumentObject.getDiagram().checkcell(callback);
            }
            else
            {
                callback(false);    
            }
        }
        else 
            callback(false);
    };
	
	this.explore = function(documentObject)
	{
		if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
			if(this.currentObject != documentObject.getDiagram())
            {
                this.diagram = documentObject.getDiagram();
                this.tableObj = undefined;
                this.diagram.addChangeListener(this);
                this.loadTable(true);
                this.isActive = false;
                this.needUpdate = false;
            }
        }
	};
	
	this.save = function()
	{
	    this.isActive = false;
		this.saveTable();
	};
	
	this.show = function()
	{
	    this.isActive = true;
	    if(this.needUpdate)
        {
	        this.needUpdate = false;
	        this.diagramChanged();
        }
	};
	
	this.loadTable = function(useCache, tableType, readonly)
    {
		if(!tableType)
			tableType = "input";
		this.tableType = tableType;
        this.table.html('<div>'+resources.commonLoading+'</div>');
        var params = {
                de: this.diagram.completeName,
                type: "fbc",
                useCache: useCache,
                tableType: tableType
            };
        if(readonly)
        	params["read"] = true;
        
        queryBioUML("web/table/sceleton", params, 
        function(data)
        {
            _this.table.html(data.values);
            _this.tableObj = _this.table.children("table");
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "sPaginationType": "full_numbers_no_ellipses",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sDom": "pfrlti",
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?" + toURI(params) 
            };
            _this.tableObj.dataTable(features);
            _this.tableObj.css('width', '100%');
        }, function(data)
        {
            _this.table.html(resources.commonErrorViewpartUnavailable);
            _this.tableObj = null;
            logger.error(data.message);
        });
    };
    
	this.initActions = function(toolbarBlock)
	{
		this.tableAction = createToolbarButton(resources.vpFbcShowTable, "fbc_table.gif", this.showTableActionClick);
        toolbarBlock.append(this.tableAction);
		//this.saveAction = createToolbarButton(resources.vpModelButtonSave, "save.gif", this.saveActionClick);
        //toolbarBlock.append(this.saveAction);
	    this.calcAction = createToolbarButton(resources.vpFbcCalculate, "run.gif", this.calcActionClick);
	    toolbarBlock.append(this.calcAction);
	    this.exportAction = createToolbarButton(resources.vpFbcExport, "export.gif", this.exportActionClick);
	    toolbarBlock.append(this.exportAction);
	};
	
	this.diagramChanged = function()
    {
	    if(this.isActive)
	        this.loadTable(false);
        else
            this.needUpdate = true;
        
    };
    
    this.saveActionClick = function()
    {
        _this.saveTable();
    };
    
    this.showTableActionClick = function()
    {
    	_this.loadTable(true);
    };
    
    this.calcActionClick = function()
    {
    	_this.saveTable(_this.getParameters, _this.createDialog);
    };
    
    this.exportActionClick = function()
    {
    	_this.saveTable( function(){
    		createSaveElementDialog(resources.vpTableFilterSaveDialogTitle,
				"ru.biosoft.table.TableDataCollection", _this.diagram.completeName+ " fbc " + _this.tableType, function(value) {
					_this.exportTable(value);
				});
    	});
    };
    
    this.exportTable = function(path)
    {
        var _this = this;
        var jobID = rnd();
        var params = {
            de: _this.diagram.completeName,
            type: "fbc",
            exportTablePath: path,
            tableType: _this.tableType,
            useCache: true,
            jobID: jobID
        };
        queryBioUML("web/table/export", params, 
	        function(data)
	        {
	            showProgressDialog(jobID, data.values, function() {
	                refreshTreeBranch(getElementPath(path));
	                performTreeAction(path, "open_table");
	            });
        });
    };
    
    this.saveTable = function (callback, arg)
    {
    	if(_this.tableObj)
        {
            var params = {
            		rnd: rnd(),
                    de: _this.diagram.completeName,
                    type: "fbc",
                    action: "change",
                    tableType: _this.tableType,
                    useCache: true
                };
            	saveChangedTable(_this.tableObj, params, function(data){
	                if(_this.tableObj != undefined){
	                    _this.tableObj.fnClearTable( 0 );
	                    _this.tableObj.fnDraw();
	                }
	                queryService("fbc.service", 801,
                        {
                            de: _this.diagram.completeName
                        },
                        function(data)
                        {
                        	if(callback)
                        		callback(arg);
                        });
            	});
        }
    };
    
    this.getParameters = function(callback)
    {
    	if(_this.params == undefined)
		{
    		queryService("fbc.service", 803, {},
	            function(data){
	            	_this.params = $.evalJSON(data.values);
	            	callback();
	            },
	            function()
	            {
	            	_this.tabDiv.html(resources.commonErrorViewpartUnavailable);
	            });
		}
    	else
    		callback()
    }
    
    this.createDialog = function()
    {
    	var dialogDiv = $('<div title="'+resources.vpFbcSelectorDialogTitle +'"></div>');
        dialogDiv.html("<p>"+resources.vpFbcSelectorType+"</p>");
        var selectControl = $('<select/>').css('width', 200);
        var funcTypes = _this.params.funcType;
        for(var i=0; i < funcTypes.length; i++)
        {
            var option = $('<option value="'+funcTypes[i]+'">' + funcTypes[i]+'</option>');
            selectControl.append(option);    
        }
        dialogDiv.append(selectControl);
        dialogDiv.append("<p>"+resources.vpFbcSelectorSolver+"</p>");
        var selectControl2 = $('<select/>').css('width', 200);
        var solvers = _this.params.solver;
        for(var i=0; i < solvers.length; i++)
        {
            var option = $('<option value="'+solvers[i]+'">' + solvers[i]+'</option>');
            selectControl2.append(option);    
        }
        dialogDiv.append(selectControl2);
        dialogDiv.dialog(
        {
            autoOpen: false,
            width: 320,
            modal: true,
            buttons: 
            {
                "Ok": function()
                {
                	queryService("fbc.service", 802,
                    {
                        de: _this.diagram.completeName,
                        "funcType": selectControl.val(),
                        "solver" : selectControl2.val()
                    },
                    function(data){
                        _this.currentProcess = data.values;
                        _this.table.html('<div>Calculation started...</div>');
                        lookForViewPart('diagram.fbc').onProcessTimer();
                    	//_this.loadTable(true, "result", true);
                    });
                    $(this).dialog("close");
                    $(this).remove();
                },
    			"Cancel": function()
    			{
                    $(this).dialog("close");
                    $(this).remove();
    			}
            }
        });
        addDialogKeys(dialogDiv);
        sortButtons(dialogDiv);
        dialogDiv.dialog("open");
    };
    
    /*
     * Callback function for process timer
     */
    this.onProcessTimer = function()
    {
        queryService("fbc.service", 804,
        {
            de: _this.currentProcess
        },
        function(data)
        {
            if (data.values == "Running" || data.values == "Created") 
            {
                _this.running = setTimeout("lookForViewPart('diagram.fbc').onProcessTimer()", 2000);
            }
            else 
            {
                _this.running = undefined;
                if (data.values == "Completed") 
                {
                    queryService("fbc.service", 805,
                    {
                        de: _this.currentProcess,
                        diagram: _this.diagram.completeName
                    },
                    function(){
                        _this.loadTable(true, "result", true);
                    },
                    function()
                    {
                        logger.error(data.message);
                    });
                }
                else 
                {
                    logger.error(resources.commonErrorUnknownStatus);
                }
            }
        }, 
        function(data)
        {
            logger.error(data.message);
        });
    };
}

function AntimonyViewPart() {

	createViewPart(this, "diagram.antimony", "Antimony");
	this.containerDiv.css("height", "100%");
	this.textArea = $('<div style="width: 100%; height: 100%;"/>');
	this.errorArea = $('<div style="width: 100%; display:none;"><div/>');
    this.containerDiv.append(this.errorArea);
    this.containerDiv.append(this.textArea);


	var _this = this;
	this.diagram = null;
	this.text = null;
        this.editor = null;
	this.isActive = false;
	this.needUpdate = false;

	this.isVisible = function(documentObject, callback)
	{
	    if ((documentObject != null) && (documentObject instanceof Diagram)) 
            if( documentObject.getDiagram().getModelObjectName() != null)
            {
                //TODO: queryBioUML("web/antimony/canExplore",...) should be called here by synchronous call, otherwise 2 tabs will be created
//              queryBioUML("web/antimony/canExplore", 
//              {
//                  diagram: documentObject.getDiagram().completeName
//              }, function(data)
//              {
//                  if( data.values === "true" )
//                      callback(true);
//                  else
//                      callback(false);
//              });
               documentObject.getDiagram().checkEModel(callback);
            }
            else
            {
                callback(false);    
            }    
        else 
            callback(false);
	};

	this.explore = function(documentObject)
	{
	  //Called on new document opening, after isVisible returned true
	  //Init some document-specific variables here
		if ((documentObject != null) && documentObject instanceof Diagram ) 
        {
		    if(!this.diagram || this.diagram != documentObject.getDiagram())
	        {
		        this.isActive = false;
		        this.needUpdate = false;
		        if( this.diagram )
		            this.diagram.removeChangeListener(this);
			    this.diagram = documentObject.getDiagram();
			    this.diagram.addChangeListener(this);
			    this.getText();
	        }
        }
	};
	
	this.diagramChanged = function()
    {
	    if(this.isActive)
	        this.updateText();
	    else
	        this.needUpdate = true;
    };
    
    this.getText = function()
    {
        queryBioUML("web/antimony/getText", 
            {
                diagram: _this.diagram.completeName
            }, function(data)
            {
                if( data.values )
                {
                    _this.setNewText(data.values);
                }
            }, function(data)
            {
                _this.setNewText(data.message);
            });
    }

	this.updateText = function()
	{
		queryBioUML("web/antimony/updateText", 
        {
			diagram: _this.diagram.completeName
        }, function(data)
        {
        	if( data.values )
        	{
        		_this.setNewText(data.values);
        	}
        }, function(data)
        {
        	_this.setNewText(data.message);
        });
	}
	
	this.setNewText = function(newText)
	{
	    this.errorArea.hide();
	    this.text = newText;
            this.textArea.empty();
            _this.editor = CodeMirror(this.textArea.get(0), {
                value: newText,
                mode: "antimony",
                lineNumbers: true,
                styleActiveLine: true,
                styleSelectedText: true,
                extraKeys: {
                        "Ctrl-Space": "antimony_autocomplete",
                        "Tab": "antimony_autocomplete",
                        "Ctrl-H": "replace",
                        //"Ctrl-R": function() {_this.run();},
                        //"Ctrl-S": function() {_this.save();}
                }
              });
           setTimeout(function() {
               _this.editor.refresh();
               _this.editor.focus();
           },100)
	}
	
	this.setError = function(errorText)
	{
	    this.errorArea.text(errorText).show();
	}
	
	this.show = function(documentObject)
	{
	    this.isActive = true;
	    if(this.needUpdate)
        {
	        this.needUpdate = false;
	        this.diagramChanged();
        }
           setTimeout(function() {
               if(_this.editor) {
                 _this.editor.refresh();
                 _this.editor.focus();
               }
           },100)
	};
	
	this.save = function()
	{
	    this.isActive = false;
	};
	
	this.initActions = function(toolbarBlock)
	{
	  this.applyAction = createToolbarButton("Apply antimony", "applyLayout2.gif", this.manualModeClick);
	  toolbarBlock.append(this.applyAction);
	  this.removeConnection = createToolbarButton("Auto mode", "apply.gif", this.autoModeClick);
      toolbarBlock.append(this.removeConnection);
	};
    
    this.manualModeClick = function()
    {
        var newText = _this.editor.getValue();
        queryBioUML("web/antimony/manualMode", 
            {
                diagram: _this.diagram.completeName,
                text: newText
            }, function(data)
            {
                if( data.values )
                {
                    _this.diagram.updateDocumentView();
                }
            }, function(data)
            {
                _this.setError(data.message);
            });
    }
    
    this.autoModeClick = function()
    {
        queryBioUML("web/antimony/autoMode", 
        {
            diagram: _this.diagram.completeName,
        }, function(data)
        {
            if( data.values )
            {
                _this.setNewText(data.values);
            }
        }, function(data)
        {
            _this.setError(data.message);
        });
    };
    
}


//Simulation engine / Plot joined viewpart
function ComplexSimulationViewPart()
{
    this.tabId = "diagram.csimulation";
    this.tabName = resources.vpSimulationTitle;
    this.diagram = null;
    this.shown = "engine_container";
    this.shownIndex = 0;

    var _this = this;
    
    this.colorIndex = 0;
    this.colorArray = ["[255,85,85]", "[85,85,255]", "[0,255,0]", "[18,34,123]", "[139,0,0]", "[0,179,230]", "[255,51,255]",  
                      "[230,179,51]", "[51,102,230]", "[153,255,153]", "[179,77,77]", "[102,128,179]", "[204,128,204]", 
                      "[51,153,26]", "[204,153,153]", "[77,128,204]", "[153,0,179]", "[230,77,102]", "[77,179,128]", "[153,230,230]"];
    
    this.init = function()
    {
        createViewPart(this, this.tabId, this.tabName);
        this.containerDiv.css({"margin-left":"-15px", "margin-top":"-10px", "min-width":"1000px"});
        
        var tabD = $('<div id="cslinkedTabs">'+
                '<ul>'+
                '<li><a href="#engine_container"><span>Engine</span></a></li>'+
                '<li><a href="#plot_container"><span>Plot</span></a></li>'+
                '</ul>'+
                '<div id="engine_container" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
                '</div>'+
                '<div id="plot_container" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
                '</div>'+
                '</div>');
        this.containerDiv.append(tabD);
        
        var tabDiv = this.containerDiv.find("#cslinkedTabs");
        
        tabDiv.addClass( "ui-tabs-vertical ui-helper-clearfix" );
        tabDiv.css("width","auto");
        tabDiv.find("ul").css({"position":"absolute","border-right":0, "min-height":"300px", "width":"97px"});
        
        tabDiv.find(".complex_vp_container").css({"padding-left":"105px", "padding-right":"5px"});
        tabDiv.find( "li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" ).css({"width":"95px"});
        
        
        this.plotDiv = tabDiv.find("#plot_container");
        this.engineDiv = tabDiv.find("#engine_container");
        //this.engineDiv.css({"width":"850px"});
        
        //plot
        this.lockDiv = $('<div id="lock_plot_bean" class="ui-widget-overlay" style="position:absolute; top:0; left:0; z-index:1001;"></div>');
        this.plotDiv.append(this.lockDiv);
        this.plotPI = $('<div id="' + this.tabId + '_pi"></div>');
        this.plotDiv.append(this.plotPI);
        
        //engine
        this.enginePI = $('<div id="' + this.tabId + '_pi2"></div>').css({"width":"500px", "float":"left"});
        this.engineDiv.append(this.enginePI);
        this.mainLogDiv = $('<div id="' + this.tabId + '_log"><div style="text-align:center;">Simulation log</div></div>').css({"min-width":"320px", "float":"left", "min-height":"300px", "border":"1px solid #ccc", "margin-left":"15px", "padding":"5px"});
        this.engineDiv.append(this.mainLogDiv);
        
        
        _.bindAll(this, _.functions(this));
    };
    
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram))
        {
            if( documentObject.getDiagram().getModelObjectName() != null)
            {
                documentObject.getDiagram().checkEModel(callback);
            }
            else
            {
                callback(false);    
            }
        }
        else 
            callback(false);
    };

    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
            this.diagram = documentObject.getDiagram();
            this.currentObject = this.diagram; //TODO: overhead
            //this.loadEngineModel();
            this.currentProcess = undefined;
        }
    };
    
    
    this.show = function()
    {
        this.containerDiv.find("#cslinkedTabs").tabs({
            beforeActivate: function(event, ui)
            {
                _this.shown = $(ui.newPanel).attr("id")
                _this.shownIndex = ui.newTab.index();
                updateViewPartsToolbar(_this);
            }
        });
        this.loadPlotModel();
        this.loadEngineModel();
        this.containerDiv.find("#cslinkedTabs").tabs("option", "active", _this.shownIndex);
    };
    
    this.save = function()
    {
        this.isActive = false;
        this.containerDiv.find("#cslinkedTabs").tabs("destroy");
    };
    
    this.initActions = function(toolbarBlock)
    {
        if(this.shown == "plot_container")
        {
            this.editAction = createToolbarButton("Edit curves and experiments", "edit.gif", _this.editCurves);
            toolbarBlock.append(this.editAction);
        }
        else
        {
            this.runAction = createToolbarButton(resources.vpSimulationButtonSimulate, "simulate.gif", this.runActionClick);
            toolbarBlock.append(this.runAction);
            
            this.stopAction = createDisabledToolbarButton(resources.vpSimulationButtonSimulate, "stopTask.gif", this.stopSimulation);
            toolbarBlock.append(this.stopAction);
            
            this.saveAction = createToolbarButton(resources.vpSimulationButtonSave, "save.gif", this.saveSimulatorOptions);
            toolbarBlock.append(this.saveAction);
            
            this.saveResultAction = createDisabledToolbarButton(resources.vpSimulationButtonSaveResult, "saveresult.png", this.saveSimulationResult);
            toolbarBlock.append(this.saveResultAction);
            
            this.adjustButtons(_this.running != undefined);
        
        }
    };
    
    
    //**** Plot ****
    this.getNextColor = function()
    {
        return this.colorArray[(this.colorIndex++) % this.colorArray.length];
    }
    
    this.editCurves = function()
    {
        queryBioUML("web/diagramplot/plots_list", 
        {
            de: _this.currentObject.completeName
        },
        function(data)
        {
            _this.plots = data.values;
            _this.editCurvesWithList();
        });
    }
    
    this.editCurvesWithList = function()
    {
        var curvesDialogDiv = $('<div title="Edit plot curves"></div>');
        
        var plotSelect = $('<select></select>');
        for(var i =0; i < _this.plots.length; i++)
        {
            plotSelect.append($('<option></option>').val(_this.plots[i]).text(_this.plots[i]));
        }
        
        var typeSelect = $('<select></select>');
        typeSelect.append($('<option></option>').val("curves").text("Curves"));
        typeSelect.append($('<option></option>').val("experiments").text("Experiments"));
        
        plotSelect.change(function()
        {
            _this.loadPlotTable($(this).val(), typeSelect.val());
        });
        
        typeSelect.change(function()
        {
            _this.loadPlotTable(plotSelect.val(), $(this).val());
        });

        curvesDialogDiv.append($('<div></div>').css("padding-bottom", "10px").append('<b>Select plot:</b>&nbsp;').append(plotSelect));
        curvesDialogDiv.append($('<div></div>').css("padding-bottom", "10px").append('<b>Select line type:</b>&nbsp;').append(typeSelect));
        
        _this.table = $('<div>'+resources.dlgPlotEditorLoading+'</div>');
        curvesDialogDiv.append(_this.table);
       
        curvesDialogDiv.dialog(
        {
            autoOpen: false,
            width: 700,
            buttons:
            {
                
                "Save" : function()
                {
                    var _thisDialog = $(this);
                    _this.saveTable(plotSelect.val(), typeSelect.val(), function(){
                        _thisDialog.dialog("close");
                        _thisDialog.remove();
                        _this.reloadPlotModel();
                    });
                    return false;
                },
                "Remove plot" : function()
                {
                    var plotName = plotSelect.val();
                    createConfirmDialog("Do you really want to remove plot " + plotName, 
                    function(){
                        _this.removePlot(plotName, plotSelect);
                    });
                    
                    return false;
                },
                "Add plot" : function()
                {
                    createPromptDialog("Create new plot", "Enter plot name: ", 
                    function(name){
                        _this.addPlot(name, plotSelect);
                    }, makeUniqueName("Plot",  plotSelect.children().map((index, option) => option.value).get()));
                    
                    return false;
                },
                "Remove line": function()
                {
                    _this.removeCurve(plotSelect.val(), typeSelect.val());
                    return false;
                },
                "Add line": function()
                {
                    _this.addToPlot(plotSelect.val(), typeSelect.val());
                    return false;
                },
            }
        });
        _this.loadPlotTable(plotSelect.val(), typeSelect.val());
        curvesDialogDiv.dialog("open");  
    };
    
    this.addPlot = function(newName, plotSelect)
    {
        queryBioUML("web/diagramplot/add_plot", 
                {
                    de: _this.currentObject.completeName,
                    plotname: newName
                },
                function()
                {
                    plotSelect.append($('<option></option>').val(newName).text(newName));
                    plotSelect.val(newName).change();
                });
    };
    
    this.removePlot = function(plotName, plotSelect)
    {
        queryBioUML("web/diagramplot/remove_plot", 
                {
                    de: _this.currentObject.completeName,
                    plotname: plotName
                },
                function()
                {
                    plotSelect.find('[value="'+plotName+'"]').remove();
                    plotSelect.change();
                });
    };
    
    
    this.saveTable = function(plotName, typeName, callback)
    {
        if (_this.tableObj) 
        {
            var dataParams = {
                rnd: rnd(),
                action: 'change',
                de: _this.currentObject.completeName,
                type: "plotinfo",
                tabletype: typeName,
                plotname: plotName
            };
            saveChangedTable(_this.tableObj, dataParams, callback);
        }
        else
        {
            callback();    
        }
    };
    
    this.loadPlotTable = function(plotName, typeName)
    {
        if(plotName == null)
        {
            _this.table.html('<div>'+resources.dlgPlotEditorTableNoPlots.replace("{diagram}", getElementName(_this.currentObject.completeName))+'</div>');
            return;
        }
        
        _this.table.html('<div>'+resources.dlgPlotEditorLoading+'</div>');
        var params = {
                de: _this.currentObject.completeName,
                type: "plotinfo",
                tabletype: typeName,
                plotname: plotName,
                add_row_id: "true"
            };
        queryBioUML("web/table/sceleton", params, 
        function(data)
        {
            _this.table.html(data.values);
            _this.tableObj = _this.table.children("table");
            _this.tableObj.addClass('selectable_table');
            params["rnd"] = rnd();
            params["read"] = true;
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "sDom": "frti",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?" + toURI(params) 
            };
            _this.tableObj.dataTable(features);
            _this.tableObj.css('width', '100%');
        });
    };
    
    this.addToPlot = function(plotName, typeName)
    {
        if(typeName == "curves")
          queryBioUML("web/diagramplot/subdiagrams", 
          {
              de: _this.currentObject.completeName
          }, function(data)
          {
            _this.addCurve(plotName, typeName, data.values);
          });
        else if(typeName == "experiments")
            _this.addExperiment(plotName, typeName);
    };
    
    this.addCurve = function(plotName, typeName, subDiagrams)
    {
        var dialogDiv = $('<div title="Add ' + typeName + ' to plot"></div>');
        
        var variableSelector = $('<select></select>');
        
        var diagramSelector = undefined;
        if(subDiagrams && subDiagrams.length > 0){
            diagramSelector = $('<select></select>').css('margin-bottom', '10px');
            diagramSelector.append($("<option>").val("").text(" "));
            for(var i=0; i<subDiagrams.length; i++)
            {
                diagramSelector.append($("<option>").val(subDiagrams[i]).text(subDiagrams[i]));
            }
            dialogDiv.append('<br/><b>Select subdiagram:</b>&nbsp;');
            dialogDiv.append(diagramSelector);
            diagramSelector.change(function() {
                var val = $(this).val();
                _this.loadCurveVariables(plotName, val, function(values){
                    variableSelector.empty();
                    if (values) 
                    {
                        $.each(values, function(index, value)
                        {
                            variableSelector.append($('<option></option>').val(value.name).text(value.name).attr("title", value.title));
                            variableSelector.trigger('change');
                        });
                    }
                });
            });
        }
        
        var variableDiv = createSinglePropertyControl("Select variable", variableSelector);
        dialogDiv.append(variableDiv);
        
        var variableTitle = $('<input></input>').attr("size", 35);
        var variableTitleDiv = createSinglePropertyControl("Line title", variableTitle);
        dialogDiv.append(variableTitleDiv);
        
        variableSelector.change(function() {
            var option = $(this).find('option:selected');
            var title = option.attr("title");
            variableTitle.val(title);
        });
        
        var property = new DynamicProperty("Color", "color-selector", null );
        property.getDescriptor().setReadOnly(false);
        property.setCanBeNull("no");
        
        var colorSelector = new JSColorSelector(property, null);
        colorSelector.setModel(property);
        var colorNode = colorSelector.createHTMLNode();
        var color = _this.getNextColor();
        colorSelector.setValue([color]);
        var colorDiv = createSinglePropertyControl("Line color", $(colorNode));
        dialogDiv.append(colorDiv);
        
        var currentDiagram = diagramSelector ? diagramSelector.val() : "";
        _this.loadCurveVariables(plotName, currentDiagram, function(values){
            variableSelector.empty();
            if (values) 
            {
                $.each(values, function(index, value)
                {
                    variableSelector.append($('<option></option>').val(value.name).text(value.name).attr("title", value.title));
                });
            }
            variableSelector.trigger("change");
        });

        dialogDiv.dialog(
        {
            autoOpen: false,
            width: 400,
            buttons:
            {
                "OK": function()
                {
                    var addParams = {
                            de: _this.currentObject.completeName,
                            plotname: plotName,
                            color: $.toJSON(colorSelector.getValue()),
                            varname: variableSelector.val(),
                            title: variableTitle.val()
                        };
                        if(diagramSelector)
                            addParams.subdiagram = diagramSelector.val();
                        queryBioUML("web/diagramplot/add", addParams,
                            function(data)
                            {
                                _this.loadPlotTable(plotName, typeName);
                                var color = _this.getNextColor();
                                colorSelector.setValue([color]);
                            },
                            function(data)
                            {
                                logger.error(data.message);
                            });
                    
                    return false;
                },
                "Close": function()
                {
                    $(this).dialog("close");
                    $(this).remove();
                }
            }
        });
        dialogDiv.dialog("open");  
    };
    
    this.loadCurveVariables = function(plotName, subDiagram, callback)
    {
        var params = {de: _this.currentObject.completeName};
        if(subDiagram)
            params.subdiagram = subDiagram;
        
        queryBioUML("web/diagramplot/plot_variables", params,
        function(data)
        {
            callback(data.values);
        },
        function(data)
        {
            logger.error(data.message);
        });
    };

    this.addExperiment = function(plotName, typeName)
    {
        var expDialogDiv = $('<div title="Add ' + typeName + ' to plot"></div>');
        var defaultPath = "";
        if(_this.prevExpPath)
            defaultPath = _this.prevExpPath;
        var property = new DynamicProperty("experimentPath", "data-element-path", defaultPath);
        property.getDescriptor().setDisplayName(resources.dlgPlotEditorPlotPath);
        property.getDescriptor().setReadOnly(false);
        property.setCanBeNull("no");
        property.setAttribute("elementMustExist", true);
        property.setAttribute("dataElementType", "ru.biosoft.table.TableDataCollection");
        property.setAttribute("promptOverwrite", false);
        
        var pathEditor = new JSDataElementPathEditor(property, null);
        pathEditor.setModel(property);
        var plotNode = pathEditor.createHTMLNode();
        pathEditor.setValue(defaultPath);
        pathEditor.addChangeListener (function(control, oldValue, newValue) {
            if (newValue == oldValue) 
            {
                _this.fillVariablesCombos(comboX, _this.xValues, comboY, _this.yValues);
            }
            else 
            {
                _this.updateVariables(newValue, comboX, comboY);
            }
        });
        
        var expDiv = createSinglePropertyControl("Experiment table", $(plotNode));
        expDialogDiv.append(expDiv);
        
        var comboX = $('<select></select>');
        var xDiv = createSinglePropertyControl(resources.dlgPlotEditorAddSeriesX, comboX);
        expDialogDiv.append(xDiv);
        
        var comboY = $('<select></select>');
        var yDiv = createSinglePropertyControl(resources.dlgPlotEditorAddSeriesY, comboY);
        expDialogDiv.append(yDiv);
        
        var variableTitle = $('<input></input>').attr("size", 35);
        var variableTitleDiv = createSinglePropertyControl("Line title", variableTitle);
        expDialogDiv.append(variableTitleDiv);
        
        comboY.change(function() {
            variableTitle.val($(this).val());
        });
        
        var property = new DynamicProperty("Color", "color-selector", null );
        property.getDescriptor().setReadOnly(false);
        property.setCanBeNull("no");
        
        var colorSelector = new JSColorSelector(property, null);
        colorSelector.setModel(property);
        var colorNode = colorSelector.createHTMLNode();
        var color = _this.getNextColor();
        colorSelector.setValue([color]);
        var colorDiv = createSinglePropertyControl("Line color", $(colorNode));
        expDialogDiv.append(colorDiv);
        
        if(_this.xValues != undefined && _this.yValues != undefined)
            _this.fillVariablesCombos(comboX, _this.xValues, comboY, _this.yValues);
        else if(_this.prevExpPath != undefined)
            _this.updateVariables(_this.prevExpPath, comboX, comboY);
        
        expDialogDiv.dialog(
        {
            autoOpen: false,
            width: 400,
            buttons:
            {
                "OK": function()
                {
                    var params = {
                        de: _this.currentObject.completeName,
                        x : comboX.val(),
                        y : comboY.val(),
                        color: $.toJSON(colorSelector.getValue()),
                        source : pathEditor.getValue(),
                        title: variableTitle.val(),
                        plotname: plotName
                    };
                    queryBioUML("web/diagramplot/addexp", params, 
                        function(data)
                        {
                            _this.loadPlotTable(plotName, typeName);
                            _this.prevExpPath = pathEditor.getValue();
                            var color = _this.getNextColor();
                            colorSelector.setValue([color]);
                        },
                        function(data)
                        {
                            logger.error(data.message);
                        });
                    
                    return false;
                },
                "Close": function()
                {
                    $(this).dialog("close");
                    $(this).remove();
                }
            }
        });
        expDialogDiv.dialog("open");  
    };
    
    this.loadVariables= function(completeName, callback)
    {
        queryBioUML("web/plot/variables", 
        {
            "de": completeName
        }, callback); 
    };
    
    this.updateVariables = function(newValue, comboX, comboY)
    {
        _this.loadVariables(newValue, function(data)
        {
            if (data != null) 
            {
                comboX.empty();
                comboY.empty();
                if (data.type == 0) 
                {
                    _this.xValues = data.values.x;
                    _this.yValues = data.values.y;
                    _this.fillVariablesCombos(comboX, _this.xValues, comboY, _this.yValues);
                }
                else 
                {
                    logger.error(data.message);
                }
            }
        });
    };
    
    this.fillVariablesCombos = function (comboX, valuesX, comboY,  valuesY)
    {
        comboX.empty();
        if (valuesX) 
        {
            $.each(valuesX, function(index, value)
            {
                comboX.append($('<option></option>').val(value).text(value));
            });
        }
        comboY.empty();
        if (valuesY) 
        {
            $.each(valuesY, function(index, value)
            {
                comboY.append($('<option></option>').val(value).text(value));
            });
            comboY.trigger("change");
        }
    };
    
    this.removeCurve = function(plotName, typeName)
    {
        var indices = getTableSelectedRowIds(_this.tableObj);
        if(indices == null || indices.length == 0)
            return false;
        
        queryBioUML("web/diagramplot/remove",
        {
            de: _this.currentObject.completeName,
            plotname: plotName,
            what: typeName,
            rows : $.toJSON(indices)
        }, function(data)
        {
            _this.loadPlotTable(plotName, typeName);
        });
    };
    

    this.loadPlotModel = function()
    {
        queryBioUML("web/bean/get", 
            {
                de: "diagram/plot/" + _this.currentObject.completeName
            }, function(data)
            {
                _this.lockDiv.hide();
                _this.data = data;
                _this.initPlotFromJson(data);
            }, function(data)
            {
                _this.plotDiv.html(resources.commonErrorViewpartUnavailable);
            });
    };
    
    this.reloadPlotModel = function()
    {
        queryBioUML("web/bean/get", 
            {
                de: "diagram/plot/" + _this.currentObject.completeName,
                useCache:false
            }, function(data)
            {
                //_this.plotDiv.empty().append(_this.containerDiv);
                _this.lockDiv.hide();
                _this.data = data;
                _this.initPlotFromJson(data);
            }, function(data)
            {
                _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
            });
    };

    this.initPlotFromJson = function(data)
    {
        _this.plotPI.empty();
        var beanDPS = convertJSONToDPS(data.values);
        _this.propertyPane = new JSPropertyInspector();
        _this.propertyPane.setParentNodeId(_this.plotPI.attr('id'));
        _this.propertyPane.setModel(beanDPS);
        _this.propertyPane.generate();
        _this.propertyPane.addChangeListener(function(ctl,oldval,newval) {
            _this.propertyPane.updateModel();
            var json = convertDPSToJSON(_this.propertyPane.getModel(), ctl);
            _this.setPlotFromJson(json);
        });
    };

    this.setPlotFromJson = function(json)
    {
        _this.lockDiv.show();
        queryBioUML("web/bean/set",
            {
                de: "diagram/plot/" + _this.currentObject.completeName,
                json: json
            }, function(data)
            {
                _this.lockDiv.hide();
                _this.data = data;
                _this.initPlotFromJson(data);
            });
        
    };
    
    
    //**** Engine ****
    
    this.loadEngineModel = function()
    {
        this.currentObject.getModelObjectName(function(modelName)
        {
            if (modelName != null) 
            {
                queryBioUML("web/bean/get", 
                {
                    de: modelName
                }, function(data)
                {
                    //TODO: _this.tabDiv.empty().append(_this.containerDiv);
                    _this.data = data;
                    _this.initEngineFromJson(data);
                }, function(data)
                {
                    _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
                });
            }
            else
            {
                _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
            }   
        });
    };

    this.initEngineFromJson = function(data)
    {
        _this.enginePI.empty();
        var beanDPS = convertJSONToDPS(data.values);
        _this.enginePane = new JSPropertyInspector();
        _this.enginePane.setParentNodeId(_this.enginePI.attr('id'));
        _this.enginePane.setModel(beanDPS);
        _this.enginePane.generate();
        _this.enginePane.addChangeListener(function(ctl,oldval,newval) {
            _this.enginePane.updateModel();
            var json = convertDPSToJSON(_this.enginePane.getModel(), ctl.getModel().getName());
            _this.setEngineFromJson(json);
        });
    };

    this.setEngineFromJson = function(json)
    {
        _this.currentObject.getModelObjectName(function(modelName)
        {
            queryBioUML("web/bean/set",
            {
                de: modelName,
                json: json
            }, function(data)
            {
                _this.initEngineFromJson(data);
                _this.mainLogDiv.html('<div style="text-align:center;">Simulation log</div>');
            });
        });
    };

    //method is for testing, not used from BioUML interface
    this.getSimulationResult = function()
    {
        queryBioUML("web/simulation/result", 
        {
            de: _this.currentProcess
        },
        function(data)
        {
            logger.message("Got result with fields " + Object.keys(data.values).join(', '));
        });
    }
    
    this.adjustButtons = function (isRuning)
    {
        setToolbarButtonEnabled(_this.runAction, !isRuning);
        setToolbarButtonEnabled(_this.saveResultAction, !isRuning && _this.currentProcess != undefined);
        setToolbarButtonEnabled(_this.stopAction, isRuning);
    };

    /*
     * Simulate button action handler
     */
    this.runActionClick = function()
    {
        if (_this.enginePane) 
        {
            var dps = _this.enginePane.getModel();
            var json = convertDPSToJSON(dps);
            queryBioUML("web/simulation/simulate", 
            {
                de: _this.currentObject.completeName,
                engine: json
            }, function(data)
            {
                _this.mainLogDiv.show();
                updateLog(_this.mainLogDiv,"Simulation started...");
                _this.adjustButtons(true);
                _this.currentProcess = data.values[0];
                var procId = getElementName(_this.currentProcess);
                const plotId = "plot_container_" + procId;
                const simStatusMsgId = "simulation_status_message_" + procId;
                _this.n = data.values[1];
                _this.dialogDiv = [];
                if(_this.n > 0 ) 
                {
                    _this.dialogDiv[0] = $('<div title="'+_this.currentObject.name +' '+resources.vpSimulationResultTitle+'"></div>');
                    
                    var tabDivExt = $(
                            '<div>'+
                            '<div id="'+plotId+'" class="floating_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
                            '</div>'+
                            '</div>'
                            );
                    
                    tabDivExt.addClass( "ui-tabs ui-helper-clearfix" );
                    tabDivExt.css("width","auto");
                    tabDivExt.find(".floating_container").css({float:"left", "max-width":"600px"});
                    
                    _this.dialogDiv[0].append(tabDivExt);
                        
                    
                    for(var i=0; i<_this.n; i++)
                    {
                        var plotDiv = $('<div><div id="simulation_status_message'+i+'" style="float:left;">'+
                                resources.vpSimulationResultComplete+':</div><div id="percent'+i+
                                '" "float:left;"></div><div id="graph'+i+'">&nbsp;</div></div>').css({"min-width":"500px"});
                        tabDivExt.find("#"+plotId).append(plotDiv);
                    }
                    
                    var dialogHeight = _this.n > 1 ? 700 : 520;
                    _this.dialogDiv[0].dialog(
                    {
                        autoOpen: true,
                        width: 620,
                        height: dialogHeight,
                        buttons: 
                        {
                            "Close": function()
                            {
                                $(this).dialog("close");
                                $(this).remove();
                            }
                            
                        },
                        beforeClose: function( event, ui ) {
                            _this.stopSimulation();
                        }
                        ,
                        open: function(event, ui){
                        }
                    });
                }
                lookForViewPart(_this.tabId).onProcessTimer();
            });
        }
    };

    /*
     * Callback function for process timer
     */
    this.onProcessTimer = function()
    {
        queryBioUML("web/simulation/status", 
        {
            de: this.currentProcess
        },
        function(data)
        {
            if (data.status < 3) 
            {
                if(_this.dialogDiv[0])
                    for(var i=0; i<_this.n; i++)
                    {
                        _this.dialogDiv[0].find('#percent'+i).html(data.percent+'%');
                        if (data.values) 
                        {
                            if(data.values[i])
                                _this.dialogDiv[0].find('#graph'+i).html('<img src="'+appInfo.serverPath+'web/img?de=' + data.values[i] + '&rnd=' + rnd() + '" style="width:550px;height:350px;">');
                        }
                    }
                if (data.values && data.values.length >=_this.n && data.values[_this.n]){
                    updateLog(_this.mainLogDiv, data.values[_this.n]);
                }
                _this.running = setTimeout("lookForViewPart('"+_this.tabId+"').onProcessTimer()", 2000);
            }
            else 
            {
                _this.running = undefined;
                if (data.status == 3 || data.status == 4) 
                {
                    if(_this.dialogDiv[0])
                        for(var i=0; i<_this.n; i++)
                        {
                            
                            _this.dialogDiv[0].find('#percent'+i).html(data.percent+'%');
                            if (data.values) 
                            {
                                _this.dialogDiv[0].find('#graph'+i).html('<img src="'+appInfo.serverPath+'web/img?de=' + data.values[i] + '&rnd=' + rnd() + '" style="width:550px;height:350px;">');
                            }
                        } 
                    if (data.values && data.values.length >=_this.n && data.values[_this.n]){
                        updateLog(_this.mainLogDiv, data.values[_this.n]);
                    }
                }
                else 
                {
                    logger.error(resources.commonErrorUnknownStatus);
                }
                _this.adjustButtons(false);
            }
        }, 
        function(data)
        {
            if(_this.dialogDiv[0])
            {
                _this.dialogDiv[0].dialog("close");
                _this.dialogDiv[0].remove();
            }
            let procId = getElementName(_this.currentProcess);
            let simTabid = "simulationTabs_" + procId;
            let logId = "log_container_" + procId;
            _this.running = undefined;
            _this.currentProcess = undefined;
            _this.adjustButtons(false);
            updateLog(_this.mainLogDiv, data.message);
        });
    };
    
    this.stopSimulation = function()
    {
        if(_this.running)
        {
            clearTimeout(_this.running);
            queryBioUML("web/simulation/stop", 
                {
                    de: _this.currentProcess
                },
                function(){
                    for(var i=0; i<_this.n; i++)
                    {
                        if(_this.dialogDiv[0])
                            _this.dialogDiv[0].find('#simulation_status_message'+i).html('Terminated by user:');
                    }   
                }, 
                function(){
                });
            _this.adjustButtons(false);
            _this.running = undefined;
        }
    };
    
    this.saveSimulatorOptions = function()
    {
        _this.currentObject.getModelObjectName(function(modelName)
        {
            if (modelName != null) 
            {
                queryBioUML("web/simulation/save_options", 
                {
                    de: _this.currentObject.completeName,
                    engineBean: modelName
                }, function(data){
                   //bean saved to diagram attributes
                    _this.currentObject.setChanged(true);
                }, function(data){
                    //bean not found
                });
            }
        });
    };
    
    this.saveSimulationResult = function()
    {
        createSaveElementDialog(resources.vpSimulationButtonSaveResult, "biouml.standard.simulation.SimulationResult", "", function(resultPath)
        {
            queryBioUML("web/simulation/save_result", 
            {
                de: resultPath,
                jobID: _this.currentProcess
            }, function(data){
                logger.message(resources.vpSimulationResultSaved);
                refreshTreeBranch(getElementPath(resultPath));
            }, function(data){
                logger.error(data.message);
            });
        });
    }
}


function ComplexModelViewPart()
{
    this.tabId = "diagram.cmodel";
    this.tabName = "Model"; //TODO: resources.vpUnits;
    this.diagram = null;
    this.tableChanged = {};
    this.tables = {};
    this.tableObjs = {};
    this.diagramSelector = {};
    this.show_mode = "entities_container";
    this.shownIndex = 1;
    this.type = "entities";
    this.readonly = {};
    
    var _this = this;
    
    this.init = function()
    {
        this.types = ["compartments", "entities", "variables", "reactions", "equations", "functions", "events", "constraints", "ports", "connections", "subdiagrams", "buses", "units"];
        this.needSelector = {"compartments": true, "entities":true, "reactions": true, "variables" : true, "equations":true, "functions":true, "events":true, "constraints":true, "ports":true, "connections":true, "subdiagrams":true, "buses":true, "units":true};
        
        createViewPart(this, this.tabId, this.tabName);
        this.containerDiv.css({"margin-left":"-15px", "margin-top":"-10px", "min-width":"800px"});
        
        //fill DOM
        var tabDiv = $('<div id="cmlinkedTabs"></div>');
        var ul = $('<ul/>');
        tabDiv.append(ul);
        
        for(var i = 0; i < this.types.length; i++)
        {
            let type = this.types[i];
            let type_container = type+"_container";
            var li =('<li><a href="#'+type_container+'"><span>' + type.substr(0,1).toUpperCase() + type.substr(1,type.length) + '</span></a></li>');
            ul.append(li);
            var typeContainerDiv = $('<div id="'+type_container+'" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content"></div>');
            
            if(this.needSelector[type])
            {
                this.diagramSelector[type] = $('<select id="modelparams_selector_'+type+'"></select>').css('margin-bottom', '5px');
                typeContainerDiv.append(this.diagramSelector[type]);
                this.diagramSelector[type].hide();
            }
            this.tables[type] = $('<div>'+resources.vpModelParametersLoading.replace("{type}", type)+'</div>');
            typeContainerDiv.append(this.tables[type]);
            
            tabDiv.append(typeContainerDiv);
        }

        //Add styles
        this.containerDiv.append(tabDiv);
        
        tabDiv.addClass( "ui-tabs-vertical ui-helper-clearfix" );
        tabDiv.css("width","auto");
        tabDiv.find("ul").css({"position":"absolute","border-right":0, "min-height":"300px", "width":"97px"});
        
        tabDiv.find(".complex_vp_container").css({"min-width":"650px", "padding-left":"105px", "padding-right":"5px"});
        tabDiv.find( "li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" ).css({"width":"95px"});
 
        _.bindAll(this, _.functions(this));
    };
    
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram))
        {
            if( documentObject.getDiagram().getModelObjectName() != null)
            {
                callback(true);
            }
            else
            {
                callback(false);    
            }
        }
        else 
            callback(false);
    };

    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
            if(this.diagram != documentObject.getDiagram())
            {
                this.diagram = documentObject.getDiagram();
                
                this.tableObjs = {};
                this.diagramPath = this.diagram.completeName;
                this.diagram.addChangeListener(this);
               
                this.loadModelTable("reactions");
                this.loadModelTable("units");
                this.loadModelTable("equations");
                this.loadModelTable("functions");
                this.loadModelTable("events");
                this.loadModelTable("constraints");
                this.loadModelTable("ports");
                this.loadModelTable("connections");
                this.loadModelTable("buses");
                this.loadModelTable("subdiagrams");
                
                this.fillSelector();
                
                this.detectTypesActionClick();
            }
        }
    };
    
    
    this.show = function()
    {
        this.containerDiv.find("#cmlinkedTabs").tabs({
            beforeActivate: function(event, ui)
            {
                _this.show_mode = ui.newPanel.attr("id");
                _this.shownIndex = ui.newTab.index(); 
                _this.type = _this.show_mode.substring(0, _this.show_mode.indexOf("_"));
                //console.log(_this.type);
                updateViewPartsToolbar(_this);
                _this.restoreSelection();
            }
        });
        this.containerDiv.find("#cmlinkedTabs").tabs("option", "active", _this.shownIndex);
        //this.containerDiv.find("#cmlinkedTabs").tabs("select", _this.show_mode);
    };
    
    this.save = function()
    {
        this.containerDiv.find("#cmlinkedTabs").tabs("destroy");
        var tosave = [];
        for(var i = 0; i < this.types.length; i++)
        {
            let type = _this.types[i];
            if(this.tableChanged[type])
            {
                tosave.push(type);
            }
        }
        if(tosave.length>0)
            createYesNoConfirmDialog(tosave.length> 1 ? resources.vpModelSaveAutomatically.replace("{type}", tosave.join (",")) : 
                resources.vpModelSaveAutomaticallyOne.replace("{type}", tosave[0]), function(yes){
                    for(var i = 0; i < tosave.length; i++)
                    {
                        let type = tosave[i];
                        if(yes)
                            _this.saveActionClick(null, type);
                        else
                            _this.loadModelTable(type);
                    }
                    _this.tableChanged = {};
            })
    };
    
    this.initActions = function(toolbarBlock)
    {
        if(this.type == "units")
        {
            this.addAction = createToolbarButton("Add unit", "icon_plus.gif", this.addActionClick);
            toolbarBlock.append(this.addAction);
            
            this.editAction = createToolbarButton("Edit unit", "edit.gif", this.editActionClick);
            toolbarBlock.append(this.editAction);
            
            this.removeAction = createToolbarButton("Remove unit", "removefilter.gif", this.removeActionClick);
            toolbarBlock.append(this.removeAction);
        }
        else if(this.type == "equations" || this.type == "functions" || this.type == "events" || this.type == "constraints" || this.type == "subdiagrams" || this.type == "ports" || this.type == "connections")
        {
            this.highlightAction = createToolbarButton(resources.vpModelButtonHighlightOn.replace("{type}", this.type), "highlight_on.png", this.highlightActionClick);
            toolbarBlock.append(this.highlightAction);
            
            this.clearHighlightAction = createToolbarButton(resources.vpModelButtonHighlightOff.replace("{type}", this.type), "highlight_off.png", this.clearHighlightActionClick);
            toolbarBlock.append(this.clearHighlightAction);
            
            this.saveAction = createToolbarButton(resources.vpModelButtonSave, "save.gif", this.saveActionClick);
            toolbarBlock.append(this.saveAction);
        }
        else if (this.type == "reactions")
        {
            this.saveAction = createToolbarButton(resources.vpModelButtonSave, "save.gif", this.saveActionClick);
            toolbarBlock.append(this.saveAction);     
            this.highlightAction = createToolbarButton(resources.vpModelButtonHighlightOn.replace("{type}", this.type), "highlight_on.png", this.highlightActionClick);
            toolbarBlock.append(this.highlightAction);   
            this.clearHighlightAction = createToolbarButton(resources.vpModelButtonHighlightOff.replace("{type}", this.type), "highlight_off.png", this.clearHighlightActionClick);
            toolbarBlock.append(this.clearHighlightAction);
        }
        else
        {
            this.saveAction = createToolbarButton(resources.vpModelButtonSave, "save.gif", this.saveActionClick);
            toolbarBlock.append(this.saveAction);
            this.detectTypesAction = createToolbarButton(resources.vpModelButtonDetectTypes.replace("{type}", this.type), "apply.gif", this.detectTypesActionClick);
            toolbarBlock.append(this.detectTypesAction);
            
            this.highlightAction = createToolbarButton(resources.vpModelButtonHighlightOn.replace("{type}", this.type), "highlight_on.png", this.highlightActionClick);
            toolbarBlock.append(this.highlightAction);
            
            this.clearHighlightAction = createToolbarButton(resources.vpModelButtonHighlightOff.replace("{type}", this.type), "highlight_off.png", this.clearHighlightActionClick);
            toolbarBlock.append(this.clearHighlightAction);
            
            if(this.type == "variables")
            {
                this.addVariableAction = createToolbarButton(resources.vpModelButtonAddVariable, "icon_plus.gif", this.addVariableActionClick);
                toolbarBlock.append(this.addVariableAction);
                
                this.removeVariablesAction = createToolbarButton(resources.vpModelButtonRemoveVariables, "icon_minus.gif", this.removeVariablesActionClick);
                toolbarBlock.append(this.removeVariablesAction);

                this.setInitialValuesAction = createToolbarButton("Set initial values", "setValues.gif", this.setInitialValues);
                toolbarBlock.append(this.setInitialValuesAction);
            }
            
            this.addToPlotAction = createToolbarButton(resources.vpModelButtonAddToPlot.replace("{type}", this.type), "plot.gif", this.addToPlotActionClick);
            toolbarBlock.append(this.addToPlotAction);
        }
    };
    
    this.diagramChanged = function()
    {
        this.loadModelTable("entities");
        this.loadModelTable("variables");
        this.loadModelTable("equations");
        this.loadModelTable("reactions");
        this.loadModelTable("functions");
        this.loadModelTable("events");
        this.loadModelTable("constraints");
        this.loadModelTable("ports");
        this.loadModelTable("connections");
        this.loadModelTable("subdiagrams");
        this.loadModelTable("buses");
        this.restoreSelection();
    };
    
    this.detectTypesActionClick = function()
    {
        queryBioUML("web/diagram/detect_variable_types", {de: _this.diagramPath}, function(data)
        {
            _this.loadModelTable("entities");
            _this.loadModelTable("variables");
            _this.loadModelTable("compartments");
            _this.restoreSelection();
        });
    };
    
    //units
    this.addActionClick = function ()
    {
        var addParams = {
            de: _this.diagram.completeName,
        };
        queryBioUML("web/units/add_new", addParams,
        function(data)
        {
            _this.editUnit(data.values, true);
        },
        function(data)
        {
            logger.error(data.message);
        });
    };
    
    //units 
    this.editActionClick = function()
    {
        var selectedRows = getTableSelectedRowIds(_this.tableObjs["units"]);
        if(selectedRows.length==0)
            return;
        _this.editUnit(selectedRows[0], false);
    };
    
    //units
    this.editUnit = function (name, isNew)
    {
        let idarr = getTableRowIds(_this.tableObjs["units"]);
        var names = idarr.map (v => v[1]);
        this.createBeanEditorDialog(isNew ? "Add unit" : "Edit unit", "diagram/units/" + _this.diagram.completeName + "/" + name, isNew, names);
    };
    
    //units
    this.removeActionClick = function()
    {
        var selectedRows = getTableSelectedRowIds(_this.tableObjs["units"]);
        if(selectedRows.length==0)
            return;
        var selectedStr = selectedRows.join(',');
        queryBioUML("web/units/remove",
        {
            de: _this.diagram.completeName,
            "names": selectedStr
        }, function(data)
        {
            _this.diagram.setChanged(true);
            _this.loadModelTable("units", true);
        },
        function(data)
        {
            logger.error(data.message);
        });
    };
    
    this.createBeanEditorDialog = function (title, beanPath, isNew, names)
    {
        var propertyPane = new JSPropertyInspector();
        var parentID = "property_inspector_dialog_" + rnd();
        var origData;
        var wasChanged = false;
        var oldName = getElementName(beanPath);
        const index = names.indexOf(oldName);
        if (index > -1) {
            names.splice(index, 1);
        }
        
        queryBean(beanPath, {}, function(data)
        {
            function changeListener(control, oldValue, newValue) 
            {
                let newBeanPath = undefined;
                if(control.getModelName()=="name")
                {
                    if(isUsedName(newValue))
                    {
                        logger.error("Unit with name '" + newValue + "' already exists. Use 'Edit unit' instead.");
                        control.updateView();
                        return;
                    }
                    newBeanPath = getElementPath(beanPath) + "/" + newValue;
                }
                syncronizeData(control, newBeanPath);
            }
            
            function isUsedName (name)
            {
                return names.includes(name);
            }
            
            function syncronizeData(control, newBeanPath)
            {
                queryBioUML("web/bean/set", 
                {
                    de: beanPath,
                    json: convertDPSToJSON(propertyPane.getModel(), control)
                }, function(data)
                {
                    console.log(newBeanPath);
                    wasChanged = true;
                    if(newBeanPath)
                    {
                        beanPath = newBeanPath;
                        queryBean(beanPath, {}, function(data)
                        {
                            $(getJQueryIdSelector(parentID)).empty();
                            var beanDPS = convertJSONToDPS(data.values);
                            propertyPane = new JSPropertyInspector();
                            propertyPane.setParentNodeId(parentID);
                            propertyPane.setModel(beanDPS);
                            propertyPane.generate();
                            propertyPane.addChangeListener(changeListener);
                        });
                    }
                    else
                    {
                        $(getJQueryIdSelector(parentID)).empty();
                        var beanDPS = convertJSONToDPS(data.values);
                        propertyPane = new JSPropertyInspector();
                        propertyPane.setParentNodeId(parentID);
                        propertyPane.setModel(beanDPS);
                        propertyPane.generate();
                        propertyPane.addChangeListener(changeListener);
                    }
                });
            }
            
            var beanDPS = convertJSONToDPS(data.values);
            origData = convertDPSToJSON(beanDPS);
            
            var dialogDiv = $('<div title="'+title+'"></div>');
            dialogDiv.append('<div id="' + parentID + '"></div>');
            var closeDialog = function()
            {
                dialogDiv.dialog("close");
                dialogDiv.remove();
            };
            dialogDiv.dialog(
            {
                autoOpen: false,
                width: 500,
                height: 500,
                buttons: 
                {
                    "Cancel": function()
                    {
                        var newName = getElementName(beanPath);
                        if(isNew)
                        {
                            queryBioUML("web/units/remove",
                            {
                                de: _this.diagram.completeName,
                                "names": newName
                            }, function(data)
                            {
                                closeDialog();
                            },
                            function(data)
                            {
                                closeDialog();
                                logger.error(data.message);
                            });
                        }
                        else
                        {
                            if(wasChanged)
                            {
                                queryBioUML("web/bean/set",
                                {
                                    de: beanPath,
                                    json: origData
                                }, closeDialog, closeDialog);
                            } else closeDialog();
                        }
                    },
                    "Save": function()
                    {
                        var data = convertDPSToJSON(propertyPane.getModel());
                        queryBioUML("web/bean/set", 
                        {
                            de: beanPath,
                            json: data
                        }, function()
                        {
                            closeDialog();
                            _this.diagram.setChanged(true);
                            _this.loadModelTable("variables");
                            _this.loadModelTable("units", true);
                        }, closeDialog);
                    }
                }
            });
            addDialogKeys(dialogDiv);
            sortButtons(dialogDiv);
            dialogDiv.dialog("open");
            
            propertyPane.setParentNodeId(parentID);
            propertyPane.setModel(beanDPS);
            propertyPane.generate();
            propertyPane.addChangeListener(changeListener);
        });
    };
    
    
    
    this.loadModelTable = function(type, read)
    {
        var diagramSelector;
        diagramSelector = this.diagramSelector[type];
        _this.tables[type].html('<div>'+resources.vpModelParametersLoading.replace("{type}", type)+'</div>');
        
        var deName = this.diagramPath;
        var tableResolver = "model";
        
        var dataParams = {
                de: deName,
                type: tableResolver,
                tabletype: type,
                read: read,
                "add_row_id" : 1
            };
        if(diagramSelector != undefined && !diagramSelector.is(':empty'))
        {
            var val = diagramSelector.val();
            if(val)
            {
                dataParams["subDiagram"] = val;
                //Show read only for subdiagrams
                dataParams["read"] = true;
            }
        }
        //console.log("Loading " + type + ":" + dataParams["read"]);
        
        queryBioUML("web/table/sceleton",
                dataParams, 
        function(data)
        {
            _this.tables[type].html(data.values);
            _this.tableObjs[type] = _this.tables[type].children("table");
            
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "sPaginationType": "full_numbers_no_ellipses",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sDom": "pfrlti",
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+toURI(dataParams),
                "fnDrawCallback": function( nRow, aData, iDisplayIndex ) {
                    addTableChangeHandler(_this.tableObjs[type], function(ctrl){
                        _this.tableChanged[type] = true;
                        //console.log("changed " + type);
                    });
                    if(_this.tableObjs[type] != undefined && _this.diagram.selectedParameters[type])
                    {
                        setTableSelectedRowIds(_this.tableObjs[type], _this.diagram.selectedParameters[type]);
                    }
                },
            };
            _this.tableObjs[type].addClass('selectable_table');
            _this.tableObjs[type].dataTable(features);
            _this.tableObjs[type].css('width', '100%');
            _this.tableChanged[type] = false;
            
        }, function(data)
        {
            _this.tables[type].html(resources.commonErrorViewpartUnavailable);
            _this.tableObjs[type] = null;
            logger.error(data.message);
        });
    };
    
    this.fillSelector = function()
    {
        for(var i = 0; i < _this.types.length; i++)
        {
            let type = _this.types[i];
            var diagramSelector = _this.diagramSelector[type];
            if(diagramSelector != undefined)
            {
                diagramSelector.empty();
                diagramSelector.hide();
                diagramSelector.unbind('change');
            }
        }
        queryBioUML("web/diagram/subdiagrams", 
        {
            de: _this.diagramPath
        }, function(data)
        {
            var subs = data.values.subdiagrams;
            if(subs && subs.length > 0)
            {
                for(var type in _this.needSelector)
                {
                    if(_this.needSelector[type])
                    {
                        var diagramSelector = _this.diagramSelector[type];
                        diagramSelector.append($("<option>").val("").text("Composite diagram"));
                        for(var i=0; i<subs.length; i++)
                        {
                            diagramSelector.append($("<option>").val(subs[i].path).text(subs[i].title.replace(/<br>+/g, " ")));
                        }
                        diagramSelector.show();
                        diagramSelector.change({msg: type}, function(event) {
                            _this.loadModelTable(event.data.msg);
                        });
                    }
                }
            }
        });
    };
    
    
    
    //common
    this.saveActionClick = function(event, stype)
    {
        if(!stype)
            stype = this.type;
        if(this.tableObjs[stype])
        {
            if( !this.tableChanged[stype] )
                return;
            var tableResolver = "model";
            var deName = _this.diagramPath;
            var dataParams = {
                    rnd: rnd(),
                    action: 'change',
                    de: deName,
                    type: tableResolver,
                    tabletype: stype
                };
            
            if(!_this.diagramSelector[stype].is(':empty'))
            {
                var val = _this.diagramSelector[stype].val();
                if(val)
                {
                    dataParams["subDiagram"] = val;
                }
            }
            
            saveChangedTable(this.tableObjs[stype], dataParams, function(data){
                _this.tableChanged[stype] = false;
                if(_this.tableObjs[stype] != undefined){
                    _this.tableObjs[stype].fnClearTable( 0 );
                    _this.tableObjs[stype].fnDraw();
                }
                _this.diagram.dataCollectionChanged();
            }, function(data)
            {
                logger.error(data.message);
                _this.tableChanged[stype] = false;
                if(_this.tableObjs[stype] != undefined){
                    _this.tableObjs[stype].fnClearTable( 0 );
                    _this.tableObjs[stype].fnDraw();
                }
            }, true);
        }
    };
    
    //diagram
    this.highlightActionClick = function ()
    {
        var addRows = getTableSelectedRowIds(this.tableObjs[this.type]);//this.getSelectedRowNames();
        if(addRows.length > 0)
            this.diagram.highlightOn(this.type, addRows);
    };
    
    //diagram
    this.clearHighlightActionClick = function()
    {
        this.diagram.highlightOff(this.type);
        if(this.tableObjs[this.type] != undefined)
            clearSelection(this.tableObjs[this.type]);
    };
    
    //diagram
    this.restoreSelection = function()
    {
        if(this.diagram.selectedParameters[this.type])
        {
            this.diagram.highlightOn(this.type, this.diagram.selectedParameters[this.type]);
        }
        else
        {
            this.diagram.highlightOff(this.type);
        }
    };
    
    
    //diagram
    this.reselectTableRows = function()
    {
        if(this.tableObj[this.type] != undefined)
        {
            clearSelection(this.tableObjs[this.type]);
            if(this.currentObject.selectedParameters[this.type])
                setTableSelectedRowIds(this.tableObjs[this.type], this.currentObject.selectedParameters[this.type]);
        }
    };
    
    //diagram variables
    this.removeVariablesActionClick = function()
    {
        var selected = getTableSelectedRowsValues(this.tableObjs[this.type], "Type");
        var toRemove = [];
        var toAlert = [];
        for(var i = 0; i < selected.length; i++)
        {
            if(selected[i].value == "Not used")
                toRemove.push(selected[i].name);
            else
                toAlert.push(selected[i].name);
        }
        if(toAlert.length > 0)
            if(toAlert.length == selected.length)
            {
                logger.message(resources.vpModelErrorRemoveVars.replace("{quantity}", "All"));
                return;
            }
            else
                logger.message(resources.vpModelErrorRemoveVars.replace("{quantity}", "Some of the"));
        
        if(toRemove.length > 0)
            this.diagram.removeVariables(this.type, toRemove);
    };

    this.setInitialValues = function()
    {
            createOpenElementDialog("Choose table with variable initial values", "ru.biosoft.table.TableDataCollection", "", function(tablePath)
            {
                queryBioUML("web/diagram/set_initial", {
                    de: _this.diagram.completeName,
                    table: tablePath,
                }, function (data){
                    if(data.values != "ok")
                    {
                        logger.message(data.values);
                    }
                    _this.loadModelTable("variables");
                });
            });
    };
    
    //diagram variables
    this.addVariableActionClick = function()
    {
        _this.diagram.addVariable(function(){
            _this.loadModelTable(_this.type);
            _this.restoreSelection();
        });
    };
    
    //diagram
    this.addToPlotActionClick = function()
    {
        var selected = getTableSelectedRowIds(this.tableObjs[this.type]);//this.getSelectedRowNames();
        var addParams = {
                de: _this.diagramPath,
                varname: selected[0]
            };
        queryBioUML("web/diagramplot/add", addParams,
            function(data)
            {
                //do nothing
            },
            function(data)
            {
                logger.error(data.message);
            });
    };
}

function StatesViewPart()
{
    this.tabId = "diagram.states";
    this.tabName = "States";
    this.diagram = null;
    this.shown = "st_states";
    this.shownIndex = 0;
    this.tables = {};
    this.tableObjs = {};
    this.type="states";
    this.activeState = null;

    var _this = this;
    
    this.init = function()
    {
        createViewPart(this, this.tabId, this.tabName);
        this.containerDiv.css({"margin-left":"-15px", "margin-top":"-10px", "min-width":"1000px"});
        var curStateDiv = $('<div>Current state name: <b><span id="st_cur_state">none</span></b></div>');
        this.containerDiv.append(curStateDiv);
        
        var tabDiv = $('<div id="stlinkedTabs">'+
                '<ul>'+
                '<li><a href="#st_states"><span>States</span></a></li>'+
                '<li><a href="#st_changes"><span>Changes</span></a></li>'+
                '</ul>'+
                '<div id="st_states" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
                '</div>'+
                '<div id="st_changes" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
                '</div>'+
                '</div>');
        this.containerDiv.append(tabDiv);
        
        tabDiv.addClass( "ui-tabs-vertical ui-helper-clearfix" );
        tabDiv.css("width","auto");
        tabDiv.find("ul").css({"position":"absolute","border-right":0, "min-height":"300px", "width":"97px"});
        
        tabDiv.find(".complex_vp_container").css({"padding-left":"105px", "padding-right":"5px"});
        tabDiv.find( "li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" ).css({"width":"95px"});
        
        
        this.statesDiv = tabDiv.find("#st_states");
        this.tables["states"] = $('<div>Loading states..</div>');
        this.statesDiv.append(this.tables["states"]);
        
        this.changesDiv = tabDiv.find("#st_changes");
        this.tables["changes"] = $('<div>Loading changes..</div>');
        this.changesDiv.append(this.tables["changes"]);
        
        _.bindAll(this, _.functions(this));
    }
    
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram))
        {
            if( documentObject.getDiagram().getModelObjectName() != null)
            {
                callback(true);
            }
            else
            {
                callback(false);    
            }
        }
        else 
            callback(false);
    };
    
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
            if(this.diagram != documentObject.getDiagram())
            {
                this.diagram = documentObject.getDiagram();
                
                this.tableObjs = {};
                this.diagramPath = this.diagram.completeName;
                this.diagram.addChangeListener(this);
                this.activeState = null;
                this.loadTable("states");
            }
        }
    };
    
    
        
    this.show = function()
    {
        this.containerDiv.find("#stlinkedTabs").tabs({
            beforeActivate: function(event, ui)
            {
                 _this.show_mode = ui.newPanel.attr("id");
                 _this.shownIndex = ui.newTab.index(); 
                 _this.type = _this.show_mode.substring(1+_this.show_mode.indexOf("_"));
                 updateViewPartsToolbar(_this);
                 setToolbarButtonEnabled(_this.applyAction, _this.activeState==null);
                 setToolbarButtonEnabled(_this.restoreDiagramAction, _this.activeState!=null);
                 if(_this.type=="changes")
                 {
                    if(_this.activeState!=null) 
                        _this.loadTable("changes");
                    else
                        _this.tables["changes"].html('<div>Apply state to view changes</div>');
                 }
                
            }
        });
        this.containerDiv.find("#stlinkedTabs").tabs("option", "active", _this.shownIndex);
        this.getCurrentState(function(stateName){
            setToolbarButtonEnabled(_this.applyAction, stateName.length==0 || stateName =="non");
            setToolbarButtonEnabled(_this.restoreDiagramAction, stateName.length>0 && stateName !="non");
        });
        //this.containerDiv.find("#cmlinkedTabs").tabs("select", _this.show_mode);
    };
    
    this.save = function()
    {
        this.containerDiv.find("#stlinkedTabs").tabs("destroy");
    };
    
    this.initActions = function(toolbarBlock)
    {
        if(this.type == "states")
        {
            this.applyAction = createDisabledToolbarButton("Apply selected state to diagram", "state_apply.gif", this.applyActionClick);
            toolbarBlock.append(this.applyAction);
            
            this.restoreDiagramAction = createDisabledToolbarButton("Restore diagram", "state_restore.gif", this.restoreActionClick);
            toolbarBlock.append(this.restoreDiagramAction);
            
            this.addAction = createToolbarButton("Add new state to diagram and activate it", "state_add.gif", this.addStateActionClick);
            toolbarBlock.append(this.addAction);
            
            this.editAction = createToolbarButton("Edit selected state", "edit.gif", this.editActionClick);
            toolbarBlock.append(this.editAction);
            
            this.removeAction = createToolbarButton("Remove selected state from diagram", "state_remove.gif", this.removeStateActionClick);
            toolbarBlock.append(this.removeAction);
        }
        else if(this.type == "changes" )
        {
            //this.removeTransactionAction = createToolbarButton("Remove selected transaction from the state", "state_remove_transaction.gif", this.removeTransactionClick);
            //toolbarBlock.append(this.removeTransactionAction);
        }
    };
    
    this.diagramChanged = function()
    {
        this.loadTable("states");
        this.loadTable("changes");
    };
    
    this.getCurrentState = function(callback)
    {
        queryBioUML("web/states/current",
        {
            de: _this.diagram.completeName
        }, function(data)
        {
            if(data.values.length>0 && data.values != "non")
            {
                _this.activeState = data.values;
                _this.containerDiv.find("#st_cur_state").html(_this.activeState);
            }
            else
                _this.containerDiv.find("#st_cur_state").html("none");
            if(callback)
                callback(data.values);
        },
        function(data)
        {
            logger.error(data.message);
        });
    }
    
    this.loadTable = function(type)
    {
        var diagramSelector;
        _this.tables[type].html('<div>'+resources.vpModelParametersLoading.replace("{type}", type)+'</div>');
        
        var deName = this.diagramPath;
        var tableResolver = "states";
        
        var dataParams = {
                de: deName,
                type: tableResolver,
                tabletype: type,
                read: true,
                "add_row_id" : 1
            };
            
        if(type == "changes" && _this.activeState != null)
            dataParams['stateName'] = _this.activeState;

        queryBioUML("web/table/sceleton",
                dataParams, 
        function(data)
        {
            _this.tables[type].html(data.values);
            _this.tableObjs[type] = _this.tables[type].children("table");
            
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "bSort": type == "states",
                "sPaginationType": "full_numbers_no_ellipses",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sDom": "pfrlti",
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+toURI(dataParams),
                "fnDrawCallback": function( nRow, aData, iDisplayIndex ) {
                    addTableChangeHandler(_this.tableObjs[type], function(ctrl){
                        _this.tableChanged[type] = true;
                    });
                },
            };
            _this.tableObjs[type].addClass('selectable_table');
            if(type == "states")
                _this.tableObjs[type].addClass('single_row_selected');
                
            _this.tableObjs[type].dataTable(features);
            if(type == "changes" && _this.activeState != null)
                _this.tableObjs[type].fnSetColumnVis( 0, false);
            _this.tableObjs[type].css('width', '100%');
            
        }, function(data)
        {
            _this.tables[type].html(resources.commonErrorViewpartUnavailable);
            _this.tableObjs[type] = null;
            logger.error(data.message);
        });
        
    };

    this.editActionClick = function()
    {
        var selectedRows = getTableSelectedRowIds(_this.tableObjs["states"]);
        if(selectedRows.length==0)
        {
            logger.message("Please, select state to edit");
            return;
        }
        _this.editState(selectedRows[0], false, function (){
            _this.diagram.setChanged(true);
            _this.loadTable("states");
        });
    };
    
    this.editState = function (name, isNew, callback)
    {
        this.createBeanEditorDialog(isNew ? "Add state" : "Edit state", "diagram/states/" + _this.diagram.completeName + "/" + name, isNew, callback);
    };
    
    this.createBeanEditorDialog = function (title, beanPath, isNew, callback)
    {
        var propertyPane = new JSPropertyInspector();
        var parentID = "property_inspector_dialog_" + rnd();
        var origData;
        var wasChanged = false;
        var stateName = getElementName(beanPath);
        
        queryBean(beanPath, {'useCache': false}, function(data)
        {
            
            var beanDPS = convertJSONToDPS(data.values);
            origData = convertDPSToJSON(beanDPS);
            
            var dialogDiv = $('<div title="'+title+'"></div>');
            dialogDiv.append('<div id="' + parentID + '"></div>');
            var closeDialog = function()
            {
                dialogDiv.dialog("close");
                dialogDiv.remove();
            };
            dialogDiv.dialog(
            {
                autoOpen: false,
                width: 500,
                height: 500,
                buttons: 
                {
                    "Cancel": function()
                    {
                        if(isNew)
                        {
                            queryBioUML("web/states/remove",
                            {
                                de: _this.diagram.completeName,
                                "state": stateName
                            }, function(data)
                            {
                                closeDialog();
                            },
                            function(data)
                            {
                                closeDialog();
                                logger.error(data.message);
                            });
                        }
                        else
                        {
                            if(wasChanged)
                            {
                                queryBioUML("web/bean/set",
                                {
                                    de: beanPath,
                                    json: origData
                                }, closeDialog, closeDialog);
                            } else closeDialog();
                        }
                    },
                    "Save": function()
                    {
                        var data = convertDPSToJSON(propertyPane.getModel());
                        queryBioUML("web/bean/set", 
                        {
                            de: beanPath,
                            json: data
                        }, function()
                        {
                            closeDialog();
                            
                            if(callback)
                                callback(stateName);
                        }, closeDialog);
                    }
                }
            });
            addDialogKeys(dialogDiv);
            sortButtons(dialogDiv);
            dialogDiv.dialog("open");
            
            propertyPane.setParentNodeId(parentID);
            propertyPane.setModel(beanDPS);
            propertyPane.generate();
        });
    };
    

    
    this.applyActionClick = function()
    {
        var selectedRows = getTableSelectedRowIds(_this.tableObjs["states"]);
        if(selectedRows.length==0)
            logger.message("Please, select state to be applied");
        else
            _this.applyState(selectedRows[0]);
    };
    
    this.applyState = function (stateName)
    {
        
        queryBioUML("web/states/apply",
        {
            de: _this.diagram.completeName,
            "state": stateName
        }, function(data)
        {
            _this.activeState = stateName;
            _this.containerDiv.find("#st_cur_state").html(_this.activeState);
            _this.diagram.setChanged(true);
            _this.diagram.updateDocumentView();
            _this.loadTable("states");
            setToolbarButtonEnabled(_this.applyAction, false);
            setToolbarButtonEnabled(_this.restoreDiagramAction, true);
        },
        function(data)
        {
            _this.activeState = null;
            logger.error(data.message);
        });
    };
    
    this.restoreActionClick = function()
    {
        queryBioUML("web/states/restore",
        {
            de: _this.diagram.completeName,
        }, function(data)
        {
            _this.activeState = null;
            _this.containerDiv.find("#st_cur_state").html("none");
            _this.diagram.setChanged(true);
            _this.diagram.updateDocumentView();
            _this.loadTable("states");
            setToolbarButtonEnabled(_this.applyAction, true);
            setToolbarButtonEnabled(_this.restoreDiagramAction, false);
            
        },
        function(data)
        {
            logger.error(data.message);
        });
        
    };
    
    this.addStateActionClick = function()
    {
        var num = _this.tableObjs["states"].fnGetData().length + 1;
        createPromptDialog("New state", "Type new state name", function(stateName)
        {
            var addParams = {
                de: _this.diagram.completeName,
                state: stateName
            };
            queryBioUML("web/states/add", addParams,
            function(data)
            {
                _this.editState(stateName, true, _this.applyState);
            },
            function(data)
            {
                logger.error(data.message);
            });
        }, "State " + num, true);
    };
    
    this.removeStateActionClick = function()
    {
        var selectedRows = getTableSelectedRowIds(_this.tableObjs["states"]);
        if(selectedRows.length==0)
        {
            logger.message("Please, select state to remove");
            return;
        }
        queryBioUML("web/states/remove",
        {
            de: _this.diagram.completeName,
            "state": selectedRows[0]
        }, function(data)
        {
            if(_this.activeState == selectedRows[0])
            {
                _this.activeState = null;
                _this.containerDiv.find("#st_cur_state").html("none");
            }
            _this.diagram.setChanged(true);
            if(data.values == "changed")
            {
                _this.diagram.updateDocumentView();
            }
            _this.loadTable("states");
        },
        function(data)
        {
            logger.error(data.message);
        });
        
    };
    
    this.removeTransactionClick = function()
    {
        
    };
    
}

function MicroenvironmentViewPart()
{
    this.tabId = "diagram.microenv";
    this.tabName = "Microenvironment";
    this.diagram = null;
    this.shown = "me_domain";
    this.shownIndex = 0;
    this.tables = {};
    this.tableObjs = {};
    this.tableChanged = {};
    this.type="domain"; //6 types: domain, substrates, uparams, initial, options, cell types

    var _this = this;
    
    this.init = function()
    {
        createViewPart(this, this.tabId, this.tabName);
        this.containerDiv.css({"margin-left":"-15px", "margin-top":"-10px", "min-width":"1000px"});
        
        var tabDiv = $('<div id="melinkedTabs">'+
                '<ul>'+
                '<li><a href="#me_domain"><span>Domain</span></a></li>'+
                '<li><a href="#me_substrates"><span>Substrates</span></a></li>'+
				'<li><a href="#me_celltypes"><span>Cell Types</span></a></li>'+
                '<li><a href="#me_uparams"><span>User Parameters</span></a></li>'+
                '<li><a href="#me_initial"><span>Initial Condition</span></a></li>'+
				'<li><a href="#me_report"><span>Report Properties</span></a></li>'+
				'<li><a href="#me_options"><span>Options</span></a></li>'+
                '</ul>'+
                '<div id="me_domain" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
                '</div>'+
                '<div id="me_substrates" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
                '</div>'+
				'<div id="me_celltypes" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
				'</div>'+
                '<div id="me_uparams" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
                '</div>'+
                '<div id="me_initial" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
                '</div>'+
				'<div id="me_report" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
				'</div>'+
				'<div id="me_options" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content">'+
				'</div>'+
                '</div>');
        this.containerDiv.append(tabDiv);
        
        tabDiv.addClass( "ui-tabs-vertical ui-helper-clearfix" );
        tabDiv.css("width","auto");
        tabDiv.find("ul").css({"position":"absolute","border-right":0, "min-height":"300px", "width":"102px"});
        
        tabDiv.find(".complex_vp_container").css({"padding-left":"105px", "padding-right":"5px"});
        tabDiv.find( "li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" ).css({"width":"100px"});
        
        
        this.domainDiv = tabDiv.find("#me_domain");
        this.domainPI = $('<div id="' + this.tabId + '_pi2">Loading domain..</div>').css({"width":"500px", "float":"left"});
        this.domainDiv.append(this.domainPI);
        
        this.substratesDiv = tabDiv.find("#me_substrates");
        this.tables["substrates"] = $('<div>Loading substrates..</div>');
        this.substratesDiv.append(this.tables["substrates"]);
		
		this.celltypesDiv = tabDiv.find("#me_celltypes");
		this.tables["cell_types"] = $('<div>Loading cell types..</div>');
		this.substratesDiv.append(this.tables["cell_types"]);
        
        this.uparamsDiv = tabDiv.find("#me_uparams");
        this.uparamsPI = $('<div id="' + this.tabId + '_pi3">Loading user parameters..</div>').css({"width":"500px", "float":"left"});
        this.uparamsDiv.append(this.uparamsPI);
        
        this.initialDiv = tabDiv.find("#me_initial");
        this.initialPI = $('<div id="' + this.tabId + '_pi4">Loading initial values..</div>').css({"width":"500px", "float":"left"});
        this.initialDiv.append(this.initialPI);
		
		this.reportDiv = tabDiv.find("#me_report");
		this.reportPI = $('<div id="' + this.tabId + '_pi5">Loading report properties..</div>').css({"width":"500px", "float":"left"});
		this.reportDiv.append(this.reportPI);
        
		this.optionsDiv = tabDiv.find("#me_options");
		reportPI = $('<div id="' + this.tabId + '_pi6">Loading report properties..</div>').css({"width":"500px", "float":"left"});
		this.optionsDiv.append(this.tables["options"]);
        _.bindAll(this, _.functions(this));
    }
    
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram))
        {
            if( documentObject.getDiagram().getModelObjectName() != null)
            {
                documentObject.getDiagram().checkPhysicell(callback);
            }
            else
            {
                callback(false);    
            }
        }
        else 
            callback(false);
    };
    
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
            if(this.diagram != documentObject.getDiagram())
            {
                this.diagram = documentObject.getDiagram();
                
                this.tableObjs = {};
                this.diagramPath = this.diagram.completeName;
                this.diagram.addChangeListener(this);
                this.activeState = null;
                this.loadDomain();
                this.loadUserParameters();
                this.loadInitialCondition();
				this.loadReportProperties();
				this.loadOptions();
            }
        }
    };
        
    this.show = function()
    {
        this.containerDiv.find("#melinkedTabs").tabs({
            beforeActivate: function(event, ui)
            {
                 _this.show_mode = ui.newPanel.attr("id");
                 _this.shownIndex = ui.newTab.index(); 
                 _this.type = _this.show_mode.substring(1+_this.show_mode.indexOf("_"));
                 updateViewPartsToolbar(_this);
                if(_this.type=="substrates")
                   _this.loadTable("substrates");
                else if (_this.type=="cell_types")
			       _this.loadTable("cell_types");
                else
                {
                    if(_this.tableChanged["substrates"])
                    {
                        createYesNoConfirmDialog( "Substrates table was changed. Do you want to save it?", function(yes){
                            if(yes)
                                _this.saveTable("substrates");
                        });
                    }
					else if(_this.tableChanged["cell types"])
					{
					     createYesNoConfirmDialog( "Cell types table was changed. Do you want to save it?", function(yes){
					        if(yes)
					           _this.saveTable("cell types");
					     });
					 }
                }
            }
        });
        this.containerDiv.find("#melinkedTabs").tabs("option", "active", _this.shownIndex);
    };
    
    this.save = function()
    {
        this.containerDiv.find("#melinkedTabs").tabs("destroy");
        if(this.tableChanged["substrates"])
        {
            createYesNoConfirmDialog( "Substrates table was changed. Do you want to save it?", function(yes){
                if(yes)
                    _this.saveTable("substrates");
            }); 
        }
		else if(this.tableChanged["cell types"])
		{
		     createYesNoConfirmDialog( "Cell types table was changed. Do you want to save it?", function(yes){
		         if(yes)
		            _this.saveTable("cell types");
		     }); 
       }
    };
    
    this.initActions = function(toolbarBlock)
    {
        if (this.type == "substrates" || this.type == "cell_types")
        {
            this.saveAction = createToolbarButton(resources.vpModelButtonSave, "save.gif", this.saveTable);
            toolbarBlock.append(this.saveAction);     
        }
    };
    
    this.diagramChanged = function()
    {
        this.loadDomain();
        this.loadUserParameters();
        this.loadInitialCondition();
		this.loadReportProperties();
		this.loadOptions();
        this.loadTable("substrates");
		this.loadTable("cell_types");
    };
    
    this.loadTable = function(type)
    {
        var diagramSelector;
        _this.tables[type].html('<div>'+resources.vpModelParametersLoading.replace("{type}", type)+'</div>');
        
        var deName = this.diagramPath;
        var tableResolver = "physicell";
        
        var dataParams = {
                de: deName,
                type: tableResolver,
                tabletype: type,
                read: false,
                "add_row_id" : 1
            };
            
        queryBioUML("web/table/sceleton",
                dataParams, 
        function(data)
        {
            _this.tables[type].html(data.values);
            _this.tableObjs[type] = _this.tables[type].children("table");
            
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "bSort": type == "states",
                "sPaginationType": "full_numbers_no_ellipses",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sDom": "pfrlti",
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+toURI(dataParams),
                "fnDrawCallback": function( nRow, aData, iDisplayIndex ) {
                    addTableChangeHandler(_this.tableObjs[type], function(ctrl){
                        _this.tableChanged[type] = true;
                    });
                },
            };
            //_this.tableObjs[type].addClass('selectable_table');
            _this.tableObjs[type].dataTable(features);
            _this.tableObjs[type].css('width', '100%');
            
        }, function(data)
        {
            _this.tables[type].html(resources.commonErrorViewpartUnavailable);
            _this.tableObjs[type] = null;
            logger.error(data.message);
        });
    };
    
    this.loadDomain = function()
    {
        queryBioUML("web/bean/get", 
        {
            de: "diagram/physicell/" + _this.diagram.completeName + "/stub/domain"
        }, function(data)
        {
            _this.data = data;
            _this.initDomainFromJson(data);
        }, function(data)
        {
            _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
        });
    };

    this.initDomainFromJson = function(data)
    {
        _this.domainPI.empty();
        var beanDPS = convertJSONToDPS(data.values);
        _this.domainPane = new JSPropertyInspector();
        _this.domainPane.setParentNodeId(_this.domainPI.attr('id'));
        _this.domainPane.setModel(beanDPS);
        _this.domainPane.generate();
        _this.domainPane.addChangeListener(function(ctl,oldval,newval) {
            _this.domainPane.updateModel();
            var json = convertDPSToJSON(_this.domainPane.getModel(), ctl.getModel().getName());
            _this.setDomainFromJson(json);
        });
    };

    this.setDomainFromJson = function(json)
    {
        queryBioUML("web/bean/set",
        {
            de: "diagram/physicell/" + _this.diagram.completeName + "/stub/domain",
            json: json
        }, function(data)
        {
            _this.initDomainFromJson(data);
            _this.diagram.dataCollectionChanged();
        });
    };
    
    this.loadUserParameters = function()
    {
        queryBioUML("web/bean/get", 
        {
            de: "diagram/physicell/" + _this.diagram.completeName + "/stub/user_parameters"
        }, function(data)
        {
            _this.data = data;
            _this.initUserParametersFromJson(data);
        }, function(data)
        {
            _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
        });
    };
	
	this.initUserParametersFromJson = function(data)
    {
        _this.uparamsPI.empty();
        var beanDPS = convertJSONToDPS(data.values);
        _this.uparamsPane = new JSPropertyInspector();
        _this.uparamsPane.setParentNodeId(_this.uparamsPI.attr('id'));
        _this.uparamsPane.setModel(beanDPS);
        _this.uparamsPane.generate();
        _this.uparamsPane.addChangeListener(function(ctl,oldval,newval) {
            _this.uparamsPane.updateModel();
            var json = convertDPSToJSON(_this.uparamsPane.getModel(), ctl.getModel().getName());
            _this.setUserParametersFromJson(json);
        });
    };
	
    this.setUserParametersFromJson = function(json)
    {
        queryBioUML("web/bean/set",
        {
            de: "diagram/physicell/" + _this.diagram.completeName + "/stub/user_parameters",
            json: json
        }, function(data)
        {
            _this.initUserParametersFromJson(data);
        });
    };
	
	 this.loadInitialCondition = function()
    {
        queryBioUML("web/bean/get", 
        {
            de: "diagram/physicell/" + _this.diagram.completeName + "/stub/initial_condition"
        }, function(data)
        {
            _this.data = data;
            _this.initInitialConditionFromJson(data);
        }, function(data)
        {
            _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
        });
    };
	
	this.initInitialConditionFromJson = function(data)
    {
        _this.initialPI.empty();
        var beanDPS = convertJSONToDPS(data.values);
        _this.initialPane = new JSPropertyInspector();
        _this.initialPane.setParentNodeId(_this.initialPI.attr('id'));
        _this.initialPane.setModel(beanDPS);
        _this.initialPane.generate();
        _this.initialPane.addChangeListener(function(ctl,oldval,newval) {
            _this.initialPane.updateModel();
            var json = convertDPSToJSON(_this.initialPane.getModel(), ctl.getModel().getName());
            _this.setInitialConditionFromJson(json);
        });
    };
	
    this.setInitialConditionFromJson = function(json)
    {
        queryBioUML("web/bean/set",
        {
            de: "diagram/physicell/" + _this.diagram.completeName + "/stub/initial_condition",
            json: json
        }, function(data)
        {
            _this.initInitialConditionFromJson(data);
        });
    };
	
	this.loadReportProperties = function()
	   {
	       queryBioUML("web/bean/get", 
	       {
	           de: "diagram/physicell/" + _this.diagram.completeName + "/stub/report_properties"
	       }, function(data)
	       {
	           _this.data = data;
	           _this.initReportPropertiesFromJson(data);
	       }, function(data)
	       {
	           _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
	       });
	   };

	this.initReportPropertiesFromJson = function(data)
	   {
	       _this.reportPI.empty();
	       var beanDPS = convertJSONToDPS(data.values);
	       _this.reportPane = new JSPropertyInspector();
	       _this.reportPane.setParentNodeId(_this.reportPI.attr('id'));
	       _this.reportPane.setModel(beanDPS);
	       _this.reportPane.generate();
	       _this.reportPane.addChangeListener(function(ctl,oldval,newval) {
	           _this.reportPane.updateModel();
	           var json = convertDPSToJSON(_this.reportPane.getModel(), ctl.getModel().getName());
	           _this.setReportPropertiesFromJson(json);
	       });
	   };

	   this.setReportPropertiesFromJson = function(json)
	   {
           queryBioUML("web/bean/set",
           {
               de: "diagram/physicell/" + _this.diagram.completeName + "/stub/report_properties",
               json: json
           }, function(data)
           {
               _this.initReportPropertiesFromJson(data);
           });
	   };
	   
	   this.loadOptions = function()
	      {
	          queryBioUML("web/bean/get", 
	          {
	              de: "diagram/physicell/" + _this.diagram.completeName + "/stub/options"
	          }, function(data)
	          {
	              _this.data = data;
	              _this.initOptionsFromJson(data);
	          }, function(data)
	          {
	              _this.tabDiv.html(resources.commonErrorViewpartUnavailable);
	          });
	      };

	      this.initOptionsFromJson = function(data)
	      {
	          _this.optionsPI.empty();
	          var beanDPS = convertJSONToDPS(data.values);
	          _this.optionsPane = new JSPropertyInspector();
	          _this.optionsPane.setParentNodeId(_this.optionsPI.attr('id'));
	          _this.optionsPane.setModel(beanDPS);
	          _this.optionsPane.generate();
	          _this.optionsPane.addChangeListener(function(ctl,oldval,newval) {
	              _this.optionsPane.updateModel();
	              var json = convertDPSToJSON(_this.optionsPane.getModel(), ctl.getModel().getName());
	              _this.setOptionsFromJson(json);
	          });
	      };

	      this.setOptionsFromJson = function(json)
	      {
	          queryBioUML("web/bean/set",
	          {
	              de: "diagram/physicell/" + _this.diagram.completeName + "/stub/options",
	              json: json
	          }, function(data)
	          {
	              _this.initOptionsFromJson(data);
	              _this.diagram.dataCollectionChanged();
	          });
	      };
    
    this.saveTable = function(type)
    {
        if(_this.tableObjs[type])
        {
            var deName = this.diagramPath;
            var tableResolver = "physicell";
            var dataParams = {
                    rnd: rnd(),
                    de: deName,
                    type: tableResolver,
                    action: "change",
                    tabletype: type,
                    useCache: true
                };
            saveChangedTable(_this.tableObjs[type], dataParams, function(data){
                _this.tableChanged[type] = false;
                if(_this.tableObjs[type] != undefined){
                    _this.tableObjs[type].fnClearTable( 0 );
                    _this.tableObjs[type].fnDraw();
                }
                _this.diagram.dataCollectionChanged();
            }, function(data)
            {
                logger.error(data.message);
                _this.tableChanged[type] = false;
                if(_this.tableObjs[type] != undefined){
                    _this.tableObjs[type].fnClearTable( 0 );
                    _this.tableObjs[type].fnDraw();
                }
            }, true);
        }
    };
    
}

function CellTypesViewPart()
{
    this.tabId = "diagram.celltypes";
    this.tabName = "Cell Types"; 
    this.diagram = null;
    this.dpi = {};
    this.dpiPane = {};
    this.show_mode = "cycle_container";
    this.shownIndex = 1;
    this.type = "cycle";
    this.readonly = {};
    this.tables = {};
    this.tableObjs = {};
    this.tableChanged = {};
    this.selectedNode = null;
    
    var _this = this;
    
    this.init = function()
    {
        this.types = ["cycle", "death", "volume", "mechanics", "motility", "secretion", "interactions", "transformations", "custom_data", "functions", "intracellular", "rules"];
        //this.needSelector = {"compartments": true, "entities":true, "reactions": true, "variables" : true, "equations":true, "functions":true, "events":true, "constraints":true, "ports":true, "connections":true, "subdiagrams":true, "buses":true, "units":true};
        
        createViewPart(this, this.tabId, this.tabName);
        this.containerDiv.css({"margin-left":"-15px", "margin-top":"-10px", "min-width":"800px"});
        
        //fill DOM
        var tabDiv = $('<div id="ctlinkedTabs"></div>');
        var ul = $('<ul/>');
        tabDiv.append(ul);
        
        for(var i = 0; i < this.types.length; i++)
        {
            let type = this.types[i];
            let type_container = type+"_container";
            var li =('<li><a href="#'+type_container+'"><span>' + type.substr(0,1).toUpperCase() + type.substr(1,type.length).replaceAll('_', ' ') + '</span></a></li>');
            ul.append(li);
            var typeContainerDiv = $('<div id="'+type_container+'" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content"></div>');
            if(type == "rules")
            {
                this.tables[type] = $('<div>'+resources.vpModelParametersLoading.replace("{type}", type)+'</div>');
                typeContainerDiv.append(this.tables[type]);
            }
            else
            {
                this.dpi[type] = $('<div id="dpi_ct_'+type+'">'+resources.vpModelParametersLoading.replace("{type}", type)+'</div>');
                typeContainerDiv.append(this.dpi[type]);
            }
            tabDiv.append(typeContainerDiv);
        }

        //Add styles
        this.containerDiv.append(tabDiv);
        
        tabDiv.addClass( "ui-tabs-vertical ui-helper-clearfix" );
        tabDiv.css("width","auto");
        tabDiv.find("ul").css({"position":"absolute","border-right":0, "min-height":"300px", "width":"97px"});
        
        tabDiv.find(".complex_vp_container").css({"min-width":"650px", "padding-left":"105px", "padding-right":"5px"});
        tabDiv.find( "li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" ).css({"width":"95px"});
 
        _.bindAll(this, _.functions(this));
    };
    
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram))
        {
            if( documentObject.getDiagram().getModelObjectName() != null)
            {
                documentObject.getDiagram().checkPhysicell(callback);
            }
            else
            {
                callback(false);    
            }
        }
        else 
            callback(false);
    };

    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof Diagram )) 
        {
            if(this.diagram != documentObject.getDiagram())
            {
                this.diagram = documentObject.getDiagram();
                
                this.diagramPath = this.diagram.completeName;
                this.diagram.addChangeListener(this);
                this.diagram.addSelectListener(this);
                this.tableObjs = {};
                this.tableChanged = {};
                this.selectedNode = null;
                this.clear();
            }
        }
    };
    
    
    this.show = function()
    {
        this.containerDiv.find("#ctlinkedTabs").tabs({
            beforeActivate: function(event, ui)
            {
                _this.show_mode = ui.newPanel.attr("id");
                _this.shownIndex = ui.newTab.index(); 
                _this.type = _this.show_mode.substring(0, _this.show_mode.lastIndexOf("_"));
                updateViewPartsToolbar(_this);
                if(_this.type=="rules")
                {
                     if(_this.selectedNode != null)
                        _this.loadTable("rules", _this.selectedNode);
                }
                else
                {
                    if(_this.tableChanged["rules"] && _this.selectedNode != null)
                    {
                        createYesNoConfirmDialog( "Rules table was changed. Do you want to save it?", function(yes){
                            if(yes)
                                _this.saveTable("rules", _this.selectedNode);
                        });
                    }
                }
                //console.log(_this.type);
            }
        });
        this.containerDiv.find("#ctlinkedTabs").tabs("option", "active", _this.shownIndex);
    };
    
    this.clear = function()
    {
        for(var i = 0; i < this.types.length; i++)
        {
            let type = this.types[i];
            if(type == "rules")
                _this.tables[type].html("Please, select element on diagram to view available properties");
            else
                _this.dpi[type].html("Please, select element on diagram to view available properties");
        }
    };
    
    this.save = function()
    {
        this.containerDiv.find("#ctlinkedTabs").tabs("destroy");
    };
    
    this.initActions = function(toolbarBlock)
    {
        if(this.type == "rules")
        {
            this.addAction = createToolbarButton("Add rule", "icon_plus.gif", this.addActionClick);
            toolbarBlock.append(this.addAction);
            
            this.removeAction = createToolbarButton("Remove rule", "removefilter.gif", this.removeActionClick);
            toolbarBlock.append(this.removeAction);
            
            this.saveAction = createToolbarButton("Save rules", "save.gif", this.saveActionClick);
            toolbarBlock.append(this.saveAction);
        }
    };
    
    this.diagramChanged = function()
    {
        let sel = this.diagram.getSelection();
        if(sel.length ==1)
        {
            this.elementSelected(sel[0]);
        }
        else
            this.elementSelected(null);
    };
    
    this.selectionChanged = function(nodes)
    {
        if(nodes.length==1)
        {
            this.elementSelected(nodes[0]);
        }
        else
            this.elementSelected( null);
    };
    
    this.elementSelected = function(name)
    {
        //change selection or deselect, save table if needed
        if(this.selectedNode != null && this.selectedNode != name)
        {
            if(_this.tableChanged["rules"])
            {
                createYesNoConfirmDialog( "Rules table was changed. Do you want to save it?", function(yes){
                    if(yes)
                        _this.saveTable("rules", _this.selectedNode);
                });
            }
        }
        if(name == null) //deselect
        {
            this.selectedNode = null;
            this.clear();
        }
        else
        {
            this.selectedNode = name;
            for(var i = 0; i < this.types.length; i++)
            {
                if(this.types[i] == "rules")
                    this.loadTable(this.types[i], name);
                else
                    this.loadDPI(this.types[i], name);
            }
        }
    }
    
    this.loadDPI = function(type, name)
    {
        queryBioUML("web/bean/get", 
        {
            de: "diagram/physicell/" + _this.diagram.completeName + "/" + name + "/" + type
        }, function(data)
        {
            _this.initDPIFromJson(data, type, name);
        }, function(data)
        {
            _this.dpi[type].html("Please, select element on diagram to view available properties");
        });
    };

    this.initDPIFromJson = function(data, type, name)
    {
        _this.dpi[type].empty();
        var beanDPS = convertJSONToDPS(data.values);
        _this.dpiPane[type] = new JSPropertyInspector();
        _this.dpiPane[type].setParentNodeId(_this.dpi[type].attr('id'));
        _this.dpiPane[type].setModel(beanDPS);
        _this.dpiPane[type].generate();
        var type2 = type;
        var name2 = name;
        _this.dpiPane[type].addChangeListener(function(ctl,oldval,newval) {
            //console.log('type-name ' + type2 + " " + name2);
            _this.dpiPane[type2].updateModel();
            var json = convertDPSToJSON(_this.dpiPane[type2].getModel(), ctl.getModel().getName());
            _this.setDPIFromJson(json, type2, name2);
        });
    };

    this.setDPIFromJson = function(json, type, name)
    {
        queryBioUML("web/bean/set",
        {
            de: "diagram/physicell/" + _this.diagram.completeName + "/" + name + "/" + type,
            json: json
        }, function(data)
        {
            _this.initDPIFromJson(data, type, name);
            _this.diagram.dataCollectionChanged();
        });
    };
    
    this.loadTable = function(type, name)
    {
        var diagramSelector;
        _this.tables[type].html('<div>'+resources.vpModelParametersLoading.replace("{type}", type)+'</div>');
        
        var deName = this.diagramPath + "/" + name ;
        var tableResolver = "physicell";
        
        var dataParams = {
                de: deName,
                type: tableResolver,
                tabletype: type,
                read: false,
                "add_row_id" : 1
            };
            
        queryBioUML("web/table/sceleton",
                dataParams, 
        function(data)
        {
            _this.tables[type].html(data.values);
            _this.tableObjs[type] = _this.tables[type].children("table");
            
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "bSort": type == "states",
                "sPaginationType": "full_numbers_no_ellipses",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sDom": "pfrlti",
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+toURI(dataParams),
                "fnDrawCallback": function( nRow, aData, iDisplayIndex ) {
                    addTableChangeHandler(_this.tableObjs[type], function(ctrl){
                        _this.tableChanged[type] = true;
                    });
                },
            };
            _this.tableObjs[type].addClass('selectable_table');
            _this.tableObjs[type].dataTable(features);
            _this.tableObjs[type].css('width', '100%');
            
        }, function(data)
        {
            _this.tables[type].html("Please, select proper element on diagram to view available properties");
            _this.tableObjs[type] = null;
            //logger.error(data.message);
        });
    };
    
    this.saveTable = function(type, name)
    {
        if(_this.tableObjs[type])
        {
            var deName = this.diagramPath + "/" + name;
            var tableResolver = "physicell";
            var dataParams = {
                    rnd: rnd(),
                    de: deName,
                    type: tableResolver,
                    action: "change",
                    tabletype: type,
                    useCache: true
                };
            saveChangedTable(_this.tableObjs[type], dataParams, function(data){
                _this.tableChanged[type] = false;
                if(_this.tableObjs[type] != undefined){
                    _this.tableObjs[type].fnClearTable( 0 );
                    _this.tableObjs[type].fnDraw();
                }
                _this.diagram.dataCollectionChanged();
            }, function(data)
            {
                logger.error(data.message);
                _this.tableChanged[type] = false;
                if(_this.tableObjs[type] != undefined){
                    _this.tableObjs[type].fnClearTable( 0 );
                    _this.tableObjs[type].fnDraw();
                }
            }, true);
        }
    };
    
    this.saveActionClick = function()
    {
        if(this.selectedNode != null)
        {
            this.saveTable("rules", this.selectedNode);
        }
    };
    
    this.addActionClick = function()
    {
        var addParams = {
            de: _this.diagram.completeName,
            node: _this.selectedNode
        };
        queryBioUML("web/physicell/add_rule", addParams,
        function(data)
        {
            _this.loadTable("rules", _this.selectedNode);
        },
        function(data)
        {
            logger.error(data.message);
        });
    };
    
    this.removeActionClick = function()
    {
        var selectedRows = getSelectedRowNumbers(_this.tableObjs["rules"]);
        if(selectedRows.length==0)
            return;
        queryBioUML("web/physicell/remove_rule",
        {
            de: _this.diagram.completeName,
            node: _this.selectedNode,
            "index": selectedRows[0]
        }, function(data)
        {
            _this.diagram.setChanged(true);
            _this.loadTable("rules", _this.selectedNode);
        },
        function(data)
        {
            logger.error(data.message);
        });
    };
    
}
