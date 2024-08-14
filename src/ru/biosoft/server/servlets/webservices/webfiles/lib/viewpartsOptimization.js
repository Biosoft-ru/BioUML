/*
 *	Initialize view parts for optimizations 
 */
function initOptimizationViewParts()
{
	if(viewPartsInitialized['optimization'])
        return;
    
    var optVP = new ComplexOptimizationViewPart();
    optVP.init();
    viewParts.push(optVP);
    
    viewPartsInitialized['optimization'] = true;
}


function ComplexOptimizationViewPart()
{
    this.tabId = "optimization.complex";
    this.tabName = resources.vpOptimizationTitle;
    this.show_mode = "opt_method_container";
    this.show_index = 0;
    this.type = "method";
    var _this = this;
    
    this.init = function()
    {
        createViewPart(this, this.tabId, this.tabName);
        
        this.table = {};
        this.tableObj = {};
        
        this.containerDiv.css({"margin-left":"-15px", "margin-top":"-10px", "min-width":"1000px"});
        var types = ['method', 'experiments', 'constraints', 'variables', 'entities', 'simulation'];
        var tabStructure = $('<div id="optComplexTabs"/>');
        var ul = $('<ul/>');
        tabStructure.append(ul);
        this.containers = [];
        for(var i = 0; i < types.length; i++)
        {
            var type = types[i];
            let contId = 'opt_'+type+'_container';
            ul.append($('<li><a href="#'+contId+'"><span>'+resources['vpOpt'+type.ucfirst()]+'</span></a></li>'))
            this.containers[type] = $('<div id="'+contId+'" class="complex_vp_container ui-tabs-panel ui-corner-bottom ui-widget-content"/>')
            tabStructure.append(this.containers[type]);
        }
        this.containerDiv.append(tabStructure);
        
        var tabDiv = this.containerDiv.find("#optComplexTabs");
        tabDiv.addClass( "ui-tabs-vertical ui-helper-clearfix" );
        tabDiv.css("width","auto");
        tabDiv.find("ul").css({"position":"absolute","border-right":0, "min-height":"300px", "width":"93px"});
        tabDiv.find(".complex_vp_container").css({"padding-left":"100px", "padding-right":"5px"});
        tabDiv.find( "li" ).removeClass( "ui-corner-top" ).addClass( "ui-corner-left" ).css({"width":"91px"});
        
        //Method
        this.waiterMDiv = $('<div ><img src="icons/busy.png"/>'+resources.commonLoading+'</div>');
        this.contentDiv = $('<div></div>').hide();
        
        //Method parameters
        this.selectMethodElement = $('<select></select>');
        this.contentDiv.append($('<b/>').text(resources.vpOptimizationMethod));
        this.contentDiv.append(this.selectMethodElement);
                
        // Property inspector
        this.propertyInspector = $('<div id="' + _this.tabId + '_pi"></div>');
        this.contentDiv.append(this.propertyInspector);
        
        this.containers['method'].append(this.waiterMDiv);
        this.containers['method'].append(this.contentDiv);
        
        this.progressbar = $('<div/>').css("margin-top", "5pt");
        this.containers['method'].append(this.progressbar);
        this.optInfo = $('<div/>').css("margin-top", "10pt");
        this.containers['method'].append(this.optInfo);
        
        //Experiments
        this.waiterExpDiv = $('<div ><img src="icons/busy.png"/>'+resources.commonLoading+' experiments</div>');
        this.noExperimentsDiv = $('<div>No experiments added yet</div>');
        
        this.containers['experiments'].append(this.waiterExpDiv);
        this.containers['experiments'].append(this.noExperimentsDiv);

        this.experimentTabs = $('<div class="experiments_left"></div>').attr("id", "experimentTabs");

        var div1 = $('<div class="experiments_right"></div>');
        var div2 = $('<div style="overflow: hidden"></div>');
        this.expSettings = $('<table></table>');
        div1.append(div2.append(this.expSettings));
        
        var contentDiv = $('<div class="container_table experiments_right"></div>');
        this.table['experiments'] = $('<div style="overflow:hidden;"></div>');
        contentDiv.append(this.table['experiments']);
        
        this.expContent = $('<div/>');
        this.expContent.append(this.experimentTabs);
        this.expContent.append(div1);
        this.expContent.append(contentDiv);
        this.containers['experiments'].append(this.expContent);
        
        //Simulation
        
        this.waiterSimDiv = $('<div><img src="icons/busy.png"/>'+resources.commonLoading+' simulation engines</div>');
        this.noExperimentsSimDiv = $('<div>No experiments added yet</div>');
        this.errorSimDiv = $('<div></div>').hide();
        
        this.containers['simulation'].append(this.waiterSimDiv);
        this.containers['simulation'].append(this.noExperimentsSimDiv);
        this.containers['simulation'].append(this.errorSimDiv);

        this.simulationTabs = $('<div class="experiments_left"></div>').attr("id", "experimentEnginesTabs").hide();

        var contentSimDiv = $('<div class="container_engines experiments_right" style="overflow:hidden;"></div>');
        this.propertyInspectorSim = $('<div id="' + this.tabId + '_sim_pi"></div>');
        contentSimDiv.append(this.propertyInspectorSim);
        
        this.simContent = $('<div/>');
        this.simContent.append(this.simulationTabs);
        this.simContent.append(contentSimDiv);
        this.containers['simulation'].append(this.simContent);
        
        var types = ['entities', 'variables'];
        this.diagramSelector = {};
        this.tableChanged = {};
        for(var i = 0; i < types.length; i++)
        {
            var type = types[i];
            this.diagramSelector[type] = $('<select id="modelparams_selector_'+type+'"></select>').css('margin-bottom', '5px');
            this.containers[type].append(this.diagramSelector[type]);
            this.diagramSelector[type].hide();
            this.table[type] = $('<div>'+resources.vpModelParametersLoading.replace("{type}", type)+'</div>');
            this.containers[type].append(this.table[type]);
        }
        
        _.bindAll(this, _.functions(this));
    }
    
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof OptimizationDocument)) 
        {
            callback(true);
        }
        else 
        {
            callback(false);
        }
    };
    
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof OptimizationDocument))
        {
            if (this.optimization != documentObject.getOptimization()) 
            {
                this.contentDiv.hide();
                this.waiterMDiv.show();
                this.waiterExpDiv.show();
                
                this.optimization = documentObject.getOptimization();
                this.experimentsLoadedFlag = false;
                this.diagramVars = undefined;
                this.methodData = undefined;
                this.optimization.addLoadedListener(this);
                this.optimization.addExperimentsListener(this);
                
                this.tabDiv.bind("DiagramStatesLoaded", this.loadExperiments);
                this.loadExperimentCommons(this.loadDiagramStates);
                
                this.loadConstraints();
                
                this.tableObj['entities'] = undefined;
                this.tableObj['variables'] = undefined;
                this.diagramSelector['entities'].empty();
                this.diagramSelector['variables'].empty();
                //console.log("explore");
                this.table['entities'].empty();// = $('<div>'+resources.vpModelParametersLoading.replace("{type}", 'entities')+'</div>');
                this.table['variables'].empty();// = $('<div>'+resources.vpModelParametersLoading.replace("{type}", 'variables')+'</div>');
                this.table['experiments'].empty();// = $('<div>'+resources.vpModelParametersLoading.replace("{type}", 'connections')+'</div>');
                this.tableChanged = {};
            }
        }
    };
    
    //as optimization listener
    this.optimizationLoaded = function()
    {
        this.loadMethods();
        this.showJob();
        //console.log("optLoaded");
        this.table['entities'].html('<div>'+resources.vpModelParametersLoading.replace("{type}", 'entities')+'</div>');
        this.table['variables'].html('<div>'+resources.vpModelParametersLoading.replace("{type}", 'variables')+'</div>');
        this.optimization.getModelObjectName(function(modelName)
        {
            if (modelName != null) 
            {
                _this.currentDeName = _this.optimization.completeName;
                _this.diagramPath = _this.optimization.diagramPath;
                _this.fillVarSelector('entities');
                _this.loadVarTable('entities');
                _this.fillVarSelector('variables');
                _this.loadVarTable('variables');
            }
            else 
            {
                _this.table['entities'].html(resources.commonErrorViewpartUnavailable);
                _this.tableObj['entities'] = null;
                _this.table['variables'].html(resources.commonErrorViewpartUnavailable);
                _this.tableObj['variables'] = null;
            }
        });
    };
    
    //as experimentsListener
    this.experimentsLoaded = function()
    {
        //console.log("experimentsLoaded");
        this.experimentsLoadedFlag = true;
        this.drawTabs();
        this.drawSimulationTabs();
    };
    
    this.show = function(documentObject)
    {
        //console.log("show");
        this.containerDiv.find("#optComplexTabs").tabs({
            beforeActivate: function(event, ui)
            {
                var prevMode = ui.oldPanel.attr("id");
                if(prevMode)
                {
                    var prevType = prevMode.substring(4, prevMode.indexOf("_", 5));
                    if(_this.tableChanged[prevType])
                        _this.saveActionClick(null, prevType);
                }
                _this.show_mode = ui.newPanel.attr("id");
                _this.show_index = ui.newTab.index();
                _this.type = _this.show_mode.substring(4, _this.show_mode.indexOf("_", 5));
                updateViewPartsToolbar(_this);
            }
        });
        this.containerDiv.find("#optComplexTabs").tabs("option", "active", _this.show_index);
        
        if(this.methodData)
            this.setMethodDPS(this.methodData);
        this.showJob();
        this.drawTabs();
        this.drawSimulationTabs();
    };
    
    this.save = function()
    {
        if(this.containerDiv.find('#optComplexTabs').data("ui-tabs"))
            this.containerDiv.find('#optComplexTabs').tabs('destroy');
        
        if (this.experimentTabs.data('ui-tabs'))
            this.experimentTabs.tabs('destroy');
        
        if (this.simulationTabs.data("ui-tabs"))
            this.simulationTabs.tabs('destroy');
            
        this.progressbar.trigger("destroy");
            
        if(this.tableChanged['variables'])
            this.saveActionClick(null, 'variables');
        if(this.tableChanged['entities'])
            this.saveActionClick(null, 'entities');
        if(this.tableChanged['experiments'])
            this.saveExperiment(this.currentExperiment);
        if(this.tableChanged['constraints'])
            this.saveConstraint();
        
    };
    
    this.initActions = function(toolbarBlock)
    {
        switch(this.type)
        {
        case 'method':
            this.initMethodActions(toolbarBlock);
            break;
        case 'experiments':
            this.initExperimentsActions(toolbarBlock);
            break;
        case 'constraints':
            this.initConstraintsActions(toolbarBlock);
            break;
        case 'variables':
            this.initVariablesActions(toolbarBlock, 'variables');
            break;
        case 'entities':
            this.initVariablesActions(toolbarBlock, 'entities');
            break;
        case 'simulation':
            break;
        }
    };
    
    this.initMethodActions = function(toolbarBlock)
    {
        this.startAction = createToolbarButton(resources.vpOptimizationButtonStart, "simulate.gif", function()
        {
            setToolbarButtonEnabled(_this.startAction, _this.plotAction, false);
            _this.startOptimization(function(){
                setToolbarButtonEnabled(_this.stopAction, true);
            });
              
        });
        toolbarBlock.append(this.startAction);
        
        this.stopAction = createDisabledToolbarButton(resources.vpOptimizationButtonStop, "stopLayout.gif", function()
        {
            setToolbarButtonEnabled(_this.startAction, _this.plotAction, true);
            setToolbarButtonEnabled(_this.stopAction, false);
            _this.stopOptimization();
            
        });
        toolbarBlock.append(this.stopAction);
        
        this.plotAction = createToolbarButton(resources.vpOptimizationButtonPlot, "plot.gif", function()
        {
            createSaveElementDialog(resources.dlgPlotEditorPlotPath, 'biouml.standard.simulation.plot.Plot', _this.optimization.completeName + '_plot', function(plotPath)
            {
                queryBioUML('web/plot/savenew', {de:plotPath}, function(data)
                {
                    var plotDialog = new PlotDialog(plotPath);
                    plotDialog.open(true, plotPath);
                });
            });
        });
        toolbarBlock.append(this.plotAction);
        
        this.diagramAction = createToolbarButton(resources.vpOptimizationButtonDiagram, "generateDiagram.gif", function()
        {
            if (_this.optimization.optimizationDiagram) 
            {
                openDiagram(_this.optimization.optimizationDiagram);
            }
        });
        toolbarBlock.append(this.diagramAction);
        
        if (this.optimization) 
        {
            if (this.optimization.status == "running")
            {
                setToolbarButtonEnabled(_this.stopAction,true);
                setToolbarButtonEnabled(_this.startAction, _this.plotAction, false);
            }
            else if (this.optimization.status == "completed" || this.optimization.status == "saved")
            {
                setToolbarButtonEnabled(_this.stopAction,false);
                setToolbarButtonEnabled(_this.startAction, _this.plotAction, true);
            }
        }
        this.setInitialAction = createToolbarButton(resources.vpOptimizationButtonSetInitial, "setValues.gif", function()
        {
            //We don't have possibility to use more than one class for opening, "biouml.standard.state.State" also should be supported 
            createOpenElementDialog(resources.vpOptimizationDialogSetInitial, "ru.biosoft.table.TableDataCollection", "", function(tablePath)
            {
                queryBioUML("web/optimization/set_initial", {
                    de: _this.optimization.completeName,
                    table: tablePath,
                }, function (data){
                    if(data.values != "ok")
                    {
                        logger.message(data.values);
                    }
                    _this.optimization.loadParamsTable();
                });
            });
        });
        toolbarBlock.append(this.setInitialAction);
        
        this.selectMethodElement.unbind("change");
        this.selectMethodElement.change( function()
        {
            var methodName = _this.selectMethodElement.val();
            _this.optimization.changed = true;
            _this.selectMethod(methodName);
        });
    };
    
    this.initExperimentsActions = function(toolbarBlock)
    {
        this.addExperimentAction = createToolbarButton("Add new experimental data for analysis", "addExperiment.gif", this.addExperimentClick);
        toolbarBlock.append(this.addExperimentAction);
        this.removeExperimentAction = createToolbarButton("Remove selected experimental data from analysis", "removeExperiment.gif", this.removeExperiment);
        toolbarBlock.append(this.removeExperimentAction);
        //this.saveExperimentAction = createToolbarButton("Save changes", "save.gif", this.saveExperiment);
        //toolbarBlock.append(this.saveExperimentAction);
    };
    
    this.initConstraintsActions = function(toolbarBlock)
    {
        this.addConstrAction = createToolbarButton("Add new constraint for analysis", "addConstraint.gif", function()
        {
             _this.saveConstraint(_this.addConstraint);      
        });
        toolbarBlock.append(this.addConstrAction);
        this.removeConstrAction = createToolbarButton("Remove selected constraints from analysis", "removeConstraint.gif", function()
        {
            _this.saveConstraint(_this.removeConstraint);      
        });
        toolbarBlock.append(this.removeConstrAction);
        this.saveConstrAction = createToolbarButton("Save constraints", "save.gif", function()
        {
            _this.saveConstraint();      
        });
        toolbarBlock.append(this.saveConstrAction);
    };
    
    this.initVariablesActions = function(toolbarBlock, varType)
    {
        this.addAction = createToolbarButton(resources.vpModelButtonAdd.replace("{type}", varType), "applyLayout2.gif", this.addActionClick);
        toolbarBlock.append(this.addAction);
    
        this.removeAction = createToolbarButton(resources.vpModelButtonRemove.replace("{type}", varType), "saveLayout.gif", this.removeActionClick);
        toolbarBlock.append(this.removeAction);
        
        this.saveAction = createToolbarButton(resources.vpModelButtonSave, "save.gif", this.saveActionClick);
        toolbarBlock.append(this.saveAction);
    };
    

    
    //Method tab
    this.loadMethods = function()
    {
        if(this.methodsLoaded)
        {
            _this.showMethods();
        }
        else    
        queryBioUML("web/optimization/methods",
        {
            de: this.optimization.completeName
        }, function(data)
        {
            _this.selectMethodElement.html("");
            $.each(data.values, function(index, value)
            {
                _this.selectMethodElement.append($('<option/>').val(value).text(value));
            });
            
            _this.methodsLoaded = true;
            _this.showMethods();
        });
    };
    
    this.showMethods = function()
    {
        this.waiterMDiv.hide();
        this.contentDiv.show();
        this.selectMethodElement.val(this.optimization.method);
        this.selectMethod(this.optimization.method);
    };
    
    this.selectMethod = function (methodName)
    {
        queryBioUML("web/optimization/method_info", {
            method: methodName,
        }, function (data){
            showSimpleElementInfo(data.values);
        });
        
        this.propertyInspector.html('<div ><img src="icons/busy.png"/>'+resources.commonLoading+'</div>');
        queryBioUML("web/optimization/bean", {
            what: "method",
            method: methodName,
            de: this.optimization.completeName
        }, 
        function(data)
        {
            _this.setMethodDPS(data);
        }, 
        function()
        {
            _this.propertyInspector.html(resources.vpOptimizationErrorLoading);
        });
    };
    
    this.setMethodDPS = function (data)
    {
        this.propertyInspector.empty();
        this.methodData = data;
        var beanDPS = convertJSONToDPS(data.values);
        this.propertyPane = new JSPropertyInspector();
        this.propertyPane.setParentNodeId(this.propertyInspector.attr('id'));
        this.propertyPane.setModel(beanDPS);
        this.propertyPane.generate();    
        this.propertyPane.addChangeListener(function(ctl,oldval,newval) {
            _this.propertyPane.updateModel();
            var json = convertDPSToJSON(_this.propertyPane.getModel(), ctl.getModel().getName());
            var methodName = _this.selectMethodElement.val();
            _this.optimization.changed = true;
            _this.optimization.methodData.values = $.evalJSON(json);
            var dataParams = {
                    de: _this.optimization.completeName,
                    method: methodName,
                    options: json,
                    what: "method"
                };
            queryBioUML("web/optimization/bean", dataParams, function(data) {_this.setMethodDPS(data);});
        });
    };
    
    this.getMethodJson = function()
    {
        var dps = _this.propertyPane.getModel();
        var json = convertDPSToJSON(dps);
        return json;
    };
    
    this.startOptimization = function (success, failure)
    {
        _this.jobID = rnd();
        var json = _this.getMethodJson();
        var dataParams = {
            de: _this.optimization.completeName,
            method: _this.selectMethodElement.val(),
            options: json,
            jobID: _this.jobID
        };
        
        _this.optimization.jobID = _this.jobID;
        _this.optimization.status = "running";
        queryBioUML("web/optimization/run", dataParams,
            function(data)
            {
                _this.showJob();
                if(success)
                    success();
            }, function(data)
            {
                _this.optimizationFinished(JobControl.TERMINATED_BY_ERROR, data.message, _this.jobID);
            }
        );
    };
    
    this.showJob = function()
    {
        if (this.optimization.status != "running" || !this.optimization.jobID) 
        {
            this.progressbar.hide();
            this.optInfo.hide();
            return;
        }
        
        var infoUpdater = function()
        {
            queryBioUML("web/optimization/opt_info", {de: _this.optimization.completeName}, 
                function(data)
                {
                    _this.optInfo.html(resources.vpOptimizationInfoObjective
                        + data.values.deviation + "<br/>"
                        + resources.vpOptimizationInfoPenalty
                        + data.values.penalty + "<br/>"
                        + resources.vpOptimizationInfoEvaluations
                        + data.values.evaluations);
                    if(_this.optimization.status == "running")
                        setTimeout(infoUpdater, 1000);          
                }
            );
        };
        
        this.jobID = this.optimization.jobID;        
        this.progressbar.removeData().empty();
        this.optInfo.html("");
        this.progressbar.show();
        this.optInfo.show();
        var jobID = this.jobID;
        createProgressBar(this.progressbar, this.jobID, function(status, message, results)
        {
            _this.optimizationFinished(status, message, jobID);
            if(!results)
                return;
            var refreshPaths = {};
            for(var i = 0; i < results.length; i++)
            {
                var path = getElementPath(results[i]);
                if(!refreshPaths[path])
                {
                    refreshTreeBranch(path, true);
                    refreshPaths[path] = true;
                }
            }
        });
        setTimeout(infoUpdater, 1000);
        
    };
    
    this.optimizationFinished = function(status, message, jobID)
    {
        if(jobID != this.optimization.jobID)
            return;
        delete(this.optimization.jobID);
        if (status == JobControl.COMPLETED) 
        {
            setToolbarButtonEnabled(_this.startAction, _this.plotAction, true);
            setToolbarButtonEnabled(_this.stopAction,false);
            this.optimization.status = "completed";
            if(this.optimization.resultPath)
                refreshTreeBranch(getElementPath(this.optimization.resultPath));
        }
        else if (status == JobControl.TERMINATED_BY_ERROR)
        {
            logger.error(message);
            setToolbarButtonEnabled(_this.startAction,true);
            setToolbarButtonEnabled(_this.stopAction,false);
            this.optimization.status = "";
        }
        delete this.jobID;
        this.progressbar.hide();
        this.optInfo.hide();
    };
    
    this.stopOptimization = function()
    {
        this.optimization.status = "";
        cancelJob(this.jobID);
    };
    //Method tab finished
    
    //Experiments tab
    this.loadExperimentCommons = function(callback)
    {
        if(this.loaded)
            callback();
        queryBioUML("web/optimization/experiments",
        {
            "what": "commons",
            "de": _this.optimization.completeName
        },
        function(data)
        {
            _this.expSettings.html("");
            _this.selectDiagramExperiment = $('<select></select>');
             _this.diagramExperiment = $('<input type="text" disabled/>');
            
            // Select data file
            _this.dataFile = $('<input type="text" disabled/>');
            
            // Select weight method
            _this.selectWeightMethod = $('<select></select>');
            $.each(data.values.wmethods, function(index, value)
            {
                _this.selectWeightMethod.append($('<option></option>').val(value).text(value));
            });
            // Select experiment type
            _this.selectExperimentType = $('<select></select>');
            $.each(data.values.exptypes, function(index, value)
            {
                _this.selectExperimentType.append($('<option></option>').val(value).text(value));
            });
            
            _this.cellLine = $('<input type="text"/>');
            _this.expSettings
                .append($('<tr></tr>')
                    .append($('<td></td>').append($('<b>Diagram state:</b>')))
                    .append($('<td></td>').append(_this.diagramExperiment))
                    .append($('<td></td>').append($('<b>Weight method:</b>')))
                    .append($('<td></td>').append(_this.selectWeightMethod))
                    .append($('<td></td>').append($('<b>Cell line:</b>')))
                    .append($('<td></td>').append(_this.cellLine))
                )
                .append($('<tr></tr>')
                    .append($('<td></td>').append($('<b>Experiment file:</b>')))
                    .append($('<td></td>').append(_this.dataFile))
                    .append($('<td></td>').append($('<b>Experiment type:</b>')))
                    .append($('<td></td>').append(_this.selectExperimentType))
                    .append($('<td></td>'))
                    .append($('<td></td>'))
                );
            _this.loaded = true;
            _this.selectWeightMethod.change(function()
            {
                queryBioUML("web/optimization/change",
                {
                    de : _this.optimization.completeName,
                    "what": "experiment",
                    "expname": _this.currentExperiment,
                    "wmethod": _this.selectWeightMethod.val()    
                }, function(){
                    _this.loadConnectionsTable();
                });
            });
            _this.selectExperimentType.change(function(){
                _this.experimentChanged = true;
            });
            _this.cellLine.change(function(){
                _this.experimentChanged = true;
            });
            callback();
        });
    };
    
    this.loadDiagramStates = function()
    {
        queryBioUML("web/optimization/experiments",
        {
            "what": "diagram",
            "de": _this.optimization.completeName
        },
        function(data)
        {
            _this.selectDiagramExperiment = $('<select></select>');
            var defval = "no state";
            _this.selectDiagramExperiment.append($('<option></option>').val(defval).text(defval));
            $.each(data.values.states, function(index, value)
            {
                _this.selectDiagramExperiment.append($('<option></option>').val(value).text(value));
            });
            _this.diagramVars = data.values.diagramvars;
            _this.tabDiv.trigger("DiagramStatesLoaded");
        });
    };
    
    this.loadExperiments = function()
    {
        _this.optimization.loadExperiments();
    };
    
    this.drawTabs = function ()
    {
        if(!_this.experimentsLoadedFlag)
            return;
        _this.expContent.hide();
        _this.noExperimentsDiv.hide();
        if(!_this.optimization.experiments)
        {
            _this.noExperimentsDiv.show();
            return;
        }
        
        _this.waiterExpDiv.show();
        _this.exp2id = {};
        _this.id2exp = {};
        
        if (_this.experimentTabs.data("ui-tabs"))
            _this.experimentTabs.tabs('destroy');
        
        _this.experimentTabs.html('<ul></ul>');
        _this.experimentTabs.tabs(
        {
            beforeActivate: function(event, ui)
            {
                var oldExp = ui.oldPanel.attr('id');
                var newExp = ui.newPanel.attr('id');
                if(oldExp)
                    _this.saveExperiment(_this.id2exp[oldExp], function(){
                        _this.setExperimentValues(_this.id2exp[newExp]);
                    });
                else
                    _this.setExperimentValues(_this.id2exp[newExp]);
            }
        }).addClass('ui-tabs-vertical ui-helper-clearfix');
        
        var sortedNames = [];
        $.each(_this.optimization.experiments, function(index, value) {
            sortedNames.push(index);
        });
        
        if(sortedNames.length == 0)
        {
            _this.waiterExpDiv.hide();
            _this.noExperimentsDiv.show();
            return;
        }
        
        $.each(sortedNames.sort(), function(index, value) {
            const regex = /\ /g;
            let expId = value.replace(regex, "_");
            var i = 1;
            var expIdU = expId;
            while(_this.id2exp[expIdU] != undefined)
            {
                expIdU = expId + "_" + i;
                i++;
            }
            _this.exp2id[value] = expIdU;
            _this.id2exp[expIdU] = value;
            var expdiv = $('<div></div>').attr("id", expIdU).hide();
            _this.experimentTabs.append(expdiv);
            _this.experimentTabs.find('.ui-tabs-nav').append($( "<li><a href='" + getJQueryIdSelector(expIdU)+ "'>"+value+"</a></li>" ));
        });
        _this.experimentTabs.tabs( "refresh" );
        _this.experimentTabs.find('li').removeClass('ui-corner-top').addClass('ui-corner-left');
        _this.experimentTabs.tabs( "option", "active", 0);
        _this.waiterExpDiv.hide();
        _this.expContent.show();
        
    };
    
    this.setExperimentValues = function(expname)
    {
        if(_this.optimization.experiments && _this.optimization.experiments[expname]){
            var experiment = _this.optimization.experiments[expname];
            _this.currentExperiment = expname;
            _this.diagramExperiment.val(experiment.diagst);
            _this.selectWeightMethod.val(experiment.wmethod);
            _this.selectExperimentType.val(experiment.exptype);
            _this.dataFile.val(experiment.file);
            _this.cellLine.val(experiment.cellline);
            _this.loadConnectionsTable();
        }
    };
    
    this.loadConnectionsTable = function ()
    {
        if(! this.currentExperiment)
            return false;
        var params =
        {
            type: "optimization",
            tabletype: "experiment",
            expname: this.currentExperiment,
            de: this.optimization.completeName
        };
        queryBioUML("web/table/sceleton", params, function(data)
        {
            _this.table['experiments'].html(data.values);
            _this.tableObj['experiments'] = _this.table['experiments'].children("table");
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "bPaginate": false,
                "sDom": "pfrlti",
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?"+$.param(params),
                "fnDrawCallback": function( nRow, aData, iDisplayIndex ) {
                    addTableColumnChangeHandler(_this.tableObj['experiments'], "Path", function(ctrl){
                            var expname = _this.currentExperiment;
                            _this.saveConnectionTable(expname);
                        });
                    addTableChangeHandler(_this.tableObj['experiments'], function(ctrl){
                        _this.tableChanged['experiments'] = true;
                    });
                }
            };
            _this.tableObj['experiments'].dataTable(features);
            _this.tableObj['experiments'].css('width', '100%');
        });
    };  
    
    this.addExperimentClick = function()
    {
        var dialogDiv = $('<div title="New optimization experiment"></div>');
        var newNameInput = $('<input type="text"/>');
        var newName = "experiment_";
        if(this.optimization.experiments)
        {
            var cnt = 1;
            while(this.optimization.experiments[newName + cnt] != undefined)
            {
                cnt++;
            }
            newNameInput.val(newName + cnt);
        }
        dialogDiv.append("<b>Name:&nbsp;</b>");
        dialogDiv.append(newNameInput);
        
        dialogDiv.append("<br/><br/><b>Diagram state:&nbsp;</b>");
        dialogDiv.append(this.selectDiagramExperiment);
        
        var value = getElementPath(this.optimization.completeName) + "/";
        var property = new DynamicProperty("experimentFile", "data-element-path", value);
        property.getDescriptor().setDisplayName("File");
        property.getDescriptor().setReadOnly(false);
        property.setCanBeNull("no");
        property.setAttribute("dataElementType", "ru.biosoft.table.TableDataCollection");
        property.setAttribute("elementMustExist", true);
        property.setAttribute("promptOverwrite", false);
        
        var experimentFileEditor = new JSDataElementPathEditor(property, null);
        experimentFileEditor.setModel(property);
        var experimentNode = experimentFileEditor.createHTMLNode();
        experimentFileEditor.setValue(value);
        
        dialogDiv.append("<br/><br/><b>Experiment data:&nbsp;</b>");
        dialogDiv.append(experimentNode);
        
        dialogDiv.dialog(
        {
            autoOpen: false,
            width: 300,
            buttons:
            {
                "Ok": function()
                {
                    var newName = newNameInput.val();
                    var what = null;
                    if (!newName) 
                    {
                        what = "Experiment name";
                    }
                    else if(!_this.selectDiagramExperiment.val())
                    {
                        what = "Diagram state";
                    }
                    else if(!getElementName(experimentFileEditor.getValue()))
                    {
                        what = "Experiment file name";
                    }
                    if(what != null)
                    {
                        logger.error(what + " can not be null");
                        return false;
                    }
                    $(this).dialog("close");
                    $(this).remove();
                    if(_this.optimization.experiments && _this.optimization.experiments[newName] != undefined)
                    {
                        var confirmDialog = $('<div title="Confirm"></div>');
                        var message = "Experiment with name "+newName+" already exists. Do you realy want to replace it?"
                        confirmDialog.html("<p>" + message + "</p>");
                        confirmDialog.dialog(
                        {
                            autoOpen: false,
                            width: 500,
                            buttons: 
                            {
                                "Yes": function()
                                {
                                    $(this).dialog("close");
                                    $(this).remove();
                                    _this.addExperiment(newName, _this.selectDiagramExperiment.val(), experimentFileEditor.getValue(), true);
                                },
                                "No": function()
                                {
                                    $(this).dialog("close");
                                    $(this).remove();
                                }
                            }
                        });
                        confirmDialog.dialog("open");
                    }
                    else
                    {
                        _this.addExperiment(newName, _this.selectDiagramExperiment.val(), experimentFileEditor.getValue());
                    }
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
    };
    
    this.addExperiment = function (newName, diagramExperiment, filePath, overwrite)
    {
        var params = {
            "what": "experiment",
            "de": _this.optimization.completeName,
            "expname": newName,
            "diagram_state": diagramExperiment,
            "wmethod" : _this.selectWeightMethod.val(),
            "exptype": _this.selectExperimentType.val(),
            "celline": "",
            "file": filePath
        };
        if( overwrite )
            params['overwrite'] = 1;
            
        queryBioUML("web/optimization/add", params,
        function(data)
        {
            _this.loadExperiments(); 
        });
    };
    
    this.removeExperiment = function()
    {
        queryBioUML("web/optimization/remove",
        {
            "what": "experiment",
            "de": _this.optimization.completeName,
            "expname": _this.currentExperiment
        },
        function(data)
        {
            _this.loadExperiments();
        });
    };
    
    this.saveExperiment = function(expname, callback)
    {
        //setToolbarButtonEnabled(this.saveExperimentAction, false);
        if(!expname)
            return;
        if(this.experimentChanged)
        {
            var experiment = 
            {
                "wmethod": this.selectWeightMethod.val(),
                "exptype": this.selectExperimentType.val(),
                "celline": this.cellLine.val()
            };
            $.extend(this.optimization.experiments[this.currentExperiment], experiment);
            this.optimization.changed = true;
            
            var queryParams = {
                "what": "experiment",
                "expname": expname,
                "de": this.optimization.completeName
            };
            $.extend(queryParams, experiment);
            queryBioUML("web/optimization/change", queryParams, function(data){
                _this.experimentChanged = false;
                _this.saveConnectionTable(expname, function(){
                    if(callback)
                        callback();
                    //logger.message(resources.vpExperimentsSaved.replace("{expname}", expname));
                    //setToolbarButtonEnabled(_this.saveExperimentAction, true);
                });
            }, function(data)
            {
                _this.experimentChanged = false;
                logger.error(data.message);
                //setToolbarButtonEnabled(_this.saveExperimentAction, true);
            });
        }
        else
        {
            _this.saveConnectionTable(expname, callback);
        }
       
    };
    
    this.saveConnectionTable = function (expname, callback)
    {
        if(!this.tableChanged['experiments'])
        {
            if(callback)
                callback();
            return;
        }
        var params = {
                type: "optimization",
                tabletype: "experiment",
                expname: this.currentExperiment,
                de: this.optimization.completeName,
                rnd: rnd()
        };
        
        saveChangedTable(_this.tableObj['experiments'], params, function(data){
            _this.tableChanged['experiments'] = false;
            if(callback)
                callback();
            redrawTable(_this.tableObj['experiments']);
            
        }, function(data)
        {
            _this.tableChanged['experiments'] = false;
            logger.error(data.message);
            redrawTable(_this.tableObj['experiments']);
            
        }, true);
    };
    //Experiments tab finished
    
    //Constraints tab
    this.loadConstraints = function ()
    {
        queryBioUML("web/table/sceleton",
        {
            type: "optimization",
            tabletype: "constraints",
            de: this.optimization.completeName
        }, function(data)
        {
            _this.containers['constraints'].html(data.values);
            _this.tableObj['constraints'] = _this.containers['constraints'].children("table");
            _this.tableObj['constraints'].addClass('selectable_table');
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "bPaginate": false,
                "sDom": "pfrlti",
                "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
                    addTableChangeHandler(_this.tableObj['constraints'], function(ctrl){
                        _this.tableChanged['constraints'] = true;
                    });
                    var row = $(nRow); 
                    row.find('.cellControl').css('width', '100%');
                    return nRow;
                },
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?type=optimization&add_row_id=1&tabletype=constraints&de=" + encodeURIComponent(_this.optimization.completeName)+"&rnd="+rnd()  
            };
            _this.tableObj['constraints'].dataTable(features);
            _this.tableObj['constraints'].css('width', '100%');
            _this.tableChanged['constraints'] = false;
        });
    };
    this.addConstraint = function()
    {
        queryBioUML("web/optimization/add",
        {
            "what": "constraint",
            "de": _this.optimization.completeName
        },
        function(data)
        {
            _this.loadConstraints();
        }, function() {});
    };
    
    this.removeConstraint = function()
    {
        var rows = getTableSelectedRowIds(_this.tableObj['constraints']);
        if(rows.length > 0){
            var dataParams = {
                "what": "constraint",
                "de": _this.optimization.completeName,
                jsonrows: $.toJSON(rows)
            };
            queryBioUML("web/optimization/remove", dataParams, function()
            {
                _this.loadConstraints();
            });
        }
    };
    
    this.saveConstraint = function(callback)
    {
        var dataDPS = getDPSFromTable(_this.tableObj['constraints']);
        var dataParams = {
                "type": "optimization",
                "tabletype": "constraints",
                de: _this.optimization.completeName,
                data: convertDPSToJSON(dataDPS)
            };
        var tableparams = getTableDisplayParameters(_this.tableObj['constraints']);
        $.extend(dataParams, tableparams);
        
        queryBioUML("web/table/change", dataParams, function(data)
        {
            if(callback != undefined)
            {
                callback();
            }
            else
            {
                redrawTable(_this.tableObj['constraints']);
            }
        });
    };
    //Constraints tab finished
    
    //Simulation tab
    this.drawSimulationTabs = function ()
    {
        this.simContent.hide();
        this.noExperimentsSimDiv.hide();
        if(!this.optimization.experiments)
        {
            return;
        }
        this.waiterSimDiv.show();
        this.exp2idS = {};
        this.id2expS = {};
        if (this.simulationTabs.data("ui-tabs"))
            this.simulationTabs.tabs('destroy');
        this.simulationTabs.html('<ul></ul>');
        this.simulationTabs.tabs(
        {
            beforeActivate: function(event, ui)
            {
                _this.setExperimentValuesSim(_this.id2expS[ui.newPanel.attr("id")]);
            }
        }).addClass('ui-tabs-vertical ui-helper-clearfix');
        
        var sortedNames = [];
        $.each(this.optimization.experiments, function(index, value) {
            sortedNames.push(index);
        });
        
        if(sortedNames.length == 0)
        {
            this.waiterSimDiv.hide();
            this.noExperimentsSimDiv.show();
            return;
        }
        $.each(sortedNames.sort(), function(index, value) {
            var experimentId = _this.name2IdS(value);
            const regex = /\ /g;
            let expId = experimentId.replace(regex, "_");
            var i = 1;
            var expIdU = expId;
            while(_this.id2expS[expIdU] != undefined)
            {
                expIdU = expId + "_" + i;
                i++;
            }
            _this.exp2idS[experimentId] = expIdU;
            _this.id2expS[expIdU] = experimentId;
            
            
            var expdiv = $('<div></div>').attr("id", expIdU).hide();
            _this.simulationTabs.append(expdiv);
            _this.simulationTabs.find('.ui-tabs-nav').append($( "<li><a href='" + getJQueryIdSelector(expIdU)+ "'>"+value+"</a></li>" ));
        });
        this.simulationTabs.tabs( "refresh" );
        this.simulationTabs.find('li').removeClass('ui-corner-top').addClass('ui-corner-left');
    
        if(sortedNames.length) 
        {
            var selectedId = this.name2IdS(sortedNames[0]);
            if(this.currentSimExperiment && this.optimization.experiments[this.currentSimExperiment])
                selectedId = this.name2IdS(this.currentSimExperiment);
            this.simulationTabs.show();
            //TODO: use mapping of experiment name to index and select it's tab
            this.simulationTabs.tabs("option", "active", 0);
            //this.setExperimentValuesSim(selectedId);
        }
        this.waiterSimDiv.hide();
        this.simContent.show();
    };
    
    //Two methods below generate unique ids for experiment tabs to not coinside with Experiments viewpart tabs
    this.name2IdS = function(name)
    {
        return name + "_osvp";
    };
    
    this.id2NameS = function(id)
    {
        return id.substring(0, id.length-5); 
    };
    
    this.setExperimentValuesSim = function(experimentId)
    {
        var expname = this.id2NameS(experimentId); 
        if(_this.optimization.experiments && _this.optimization.experiments[expname])
        {
            _this.currentSimExperiment = expname;
            _this.loadSimModel();
        }
    };

    this.loadSimModel = function()
    {
        this.optimization.getModelObjectName(function(modelName)
        {
            if (modelName != null) 
            {
                var experimentBeanName = modelName + "/" + _this.currentSimExperiment;
                queryBioUML("web/bean/get", 
                {
                    de: experimentBeanName
                }, function(data)
                {
                    //_this.tabDiv.empty().append(_this.containerDiv);
                    _this.data = data;
                    _this.errorSimDiv.hide();
                    _this.initSimFromJson(data);
                }, function(data)
                {
                    //console.log(resources.commonErrorViewpartUnavailable + " Error during experiment loading.");
                    _this.errorSimDiv.html(resources.commonErrorViewpartUnavailable + " Error during experiment loading.");
                    _this.errorSimDiv.show();
                });
            }
            else
            {
                //console.log(resources.commonErrorViewpartUnavailable + " Error with model.");
                _this.errorSimDiv.html(resources.commonErrorViewpartUnavailable + " Error with model.");
                _this.errorSimDiv.show();
            }   
        });
    };

    this.initSimFromJson = function(data)
    {
        _this.propertyInspectorSim.empty();
        var beanDPS = convertJSONToDPS(data.values);
        _this.propertyPaneSim = new JSPropertyInspector();
        _this.propertyPaneSim.setParentNodeId(_this.propertyInspectorSim.attr('id'));
        _this.propertyPaneSim.setModel(beanDPS);
        _this.propertyPaneSim.generate();
        _this.propertyPaneSim.addChangeListener(function(ctl,oldval,newval) {
            _this.propertyPaneSim.updateModel();
            var json = convertDPSToJSON(_this.propertyPaneSim.getModel(), ctl.getModel().getName());
            _this.setSimFromJson(json);
        });
    };

    this.setSimFromJson = function(json)
    {
        _this.optimization.getModelObjectName(function(modelName)
        {
            var experimentBeanName = modelName + "/" + _this.currentSimExperiment;
            queryBioUML("web/bean/set",
            {
                de: experimentBeanName,
                json: json
            }, function(data)
            {
                _this.initSimFromJson(data);
            });
        });
    };
    
    //Simulation tab finished
    
    //Entities and variables
    this.fillVarSelector = function(type)
    {
        this.diagramSelector[type].empty();
        this.diagramSelector[type].hide();
        this.diagramSelector[type].unbind('change');
        queryBioUML("web/diagram/subdiagrams", 
        {
            de: _this.diagramPath
        }, function(data)
        {
            var subs = data.values.subdiagrams;
            if(subs && subs.length > 0)
            {
                _this.diagramSelector[type].append($("<option>").val("").text("Composite diagram"));
                for(var i=0; i<subs.length; i++)
                {
                    _this.diagramSelector[type].append($("<option>").val(subs[i].path).text(subs[i].title.replace(/<br>+/g, " ")));
                }
                _this.diagramSelector[type].show();
                _this.diagramSelector[type].change(function() {
                    _this.loadVarTable(type);
                });
            }
        });
    };

    
    this.diagramChanged = function()
    {
        this.loadVarTable('entities');
        this.restoreVarSelection('entities');
        this.loadVarTable('variables');
        this.restoreVarSelection('variables');
    };
    
    
    this.loadVarTable = function(type, deName, tableResolver)
    {
        if(!type)
            type = this.type;
        
        this.table[type].html('<div>'+resources.vpModelParametersLoading.replace("{type}", this.type)+'</div>');
        
        if(!deName)
            deName = this.currentDeName;
        if(!tableResolver)
            tableResolver = "optimization";
        
        var dataParams = {
                de: deName,
                type: tableResolver,
                tabletype: type,
                "add_row_id" : 1
            };
        
        if(!_this.diagramSelector[type].is(':empty'))
        {
            var val = _this.diagramSelector[type].val();
            if(val)
            {
                dataParams["subDiagram"] = val;
            }
        }
        
        queryBioUML("web/table/sceleton",
                dataParams, 
        function(data)
        {
            _this.table[type].html(data.values);
            _this.tableObj[type] = _this.table[type].children("table");
            
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
                    addTableChangeHandler(_this.tableObj[type], function(ctrl){
                        _this.tableChanged[type] = true;
                    });
                    if(_this.tableObj[type] != undefined && _this.optimization.selectedParameters[type])
                    {
                        setTableSelectedRowIds(_this.tableObj[type], _this.optimization.selectedParameters[type]);
                    }
                },
            };
            _this.tableObj[type].addClass('selectable_table');
            _this.tableObj[type].dataTable(features);
            _this.tableObj[type].css('width', '100%');
            _this.tableChanged[type] = false;
            
        }, function(data)
        {
            _this.table[type].html(resources.commonErrorViewpartUnavailable);
            _this.tableObj[type] = null;
            logger.error(data.message);
        });
    };


    this.detectTypesActionClick = function(type)
    {
        if(!type)
            type = this.type;
        queryBioUML("web/diagram/detect_variable_types", {de: _this.currentDeName}, function(data)
        {
            _this.loadVarTable(type);
            _this.restoreVarSelection(type);
        });
    };
    
    //common
    this.saveActionClick = function(event, type)
    {
        if(!type)
            type = this.type;
        //console.log("Saving " + type);
        if(type == 'experiments')
        {
            this.saveExperiment(this.currentExperiment);
            return;
        }
        if( type == 'constraints')
        {
            this.saveConstraint();
            return;
        }
        if(this.tableObj[type])
        {
            if( !this.tableChanged[type] )
                return;
            var tableResolver = "optimization";
            var deName = _this.currentDeName;
            var dataParams = {
                    rnd: rnd(),
                    action: 'change',
                    de: deName,
                    type: tableResolver,
                    tabletype: type
                };
            
            if(!_this.diagramSelector[type].is(':empty'))
            {
                var val = _this.diagramSelector[type].val();
                if(val)
                {
                    dataParams["subDiagram"] = val;
                }
            }
            
            saveChangedTable(this.tableObj[type], dataParams, function(data){
                _this.tableChanged[type] = false;
                redrawTable(_this.tableObj[type]);
                _this.optimization.reloadParamsTable(_this.type);
            }, function(data)
            {
                logger.error(data.message);
                _this.tableChanged[type] = false;
                redrawTable(_this.tableObj[type]);
            }, true);
        }
    };
    
    //optimization
    this.addActionClick = function (event, type)
    {
        if(!type)
            type = this.type;
        var addRows = getTableSelectedRowIds(this.tableObj[type]);
        if(addRows.length > 0)
            _this.optimization.addParameters(_this.type, addRows, _this.diagramSelector[type].val());
        this.tableObj[type].children('tbody').children('tr').removeClass("row_selected");
    };
    
    //optimization
    this.removeActionClick = function ()
    {
        _this.optimization.removeParameters();
    };
    

    
    this.restoreVarSelection = function(type)
    {
        if(!type)
            type = this.type;
        if(this.optimization.selectedParameters[type])
        {
            this.optimization.highlightOn(type, this.optimization.selectedParameters[type]);
        }
    };
    
    this.reselectTableRows = function(type)
    {
        if(!type)
            type = this.type;
        if(this.tableObj != undefined)
        {
            clearSelection(this.tableObj[type]);
            if(this.optimization.selectedParameters[type])
                setTableSelectedRowIds(this.tableObj[type], this.optimization.selectedParameters[type]);
        }
    };
    
    
    
}

