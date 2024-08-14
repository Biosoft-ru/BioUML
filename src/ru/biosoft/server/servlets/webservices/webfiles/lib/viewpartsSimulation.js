/**
 * Viewparts for Interactive Simulation document
 */
function initSimulationViewParts()
{
    if(viewPartsInitialized['simulation'])
        return;
    
    viewParts.push(new SimulationEditorViewpart());
    
    viewPartsInitialized['simulation'] = true;
}

function SimulationEditorViewpart()
{
    createViewPart(this, "simulation.editor", "Simulation editor");
    var _this = this;
    this.selectionDiv = $('<div></div>').css("margin-bottom", "10px");
    this.containerDiv.append(this.selectionDiv);
    this.table = $('<div>'+resources.commonLoading+'</div>');
    this.containerDiv.append(this.table);
    //TODO: move messages to messageBundle.js
    
    this.isVisible = function(documentObject, callback)
    {
      if((documentObject != null) && (documentObject instanceof SimulationDocument))
          callback(true);
      else
          callback(false);
    };
    
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof SimulationDocument))
        {
            if (this.simulation != documentObject) 
            {
                this.simulation = documentObject;
                this.tableObj = undefined;
                this.selectedParameters=[];
                this.simulation.addLoadedListener(this);
            }
        }
    };
    
    this.simulationLoaded = function()
    {
        this.loadTable();
    };
    
    this.show = function(documentObject) //[Optional]
    {
      //Called on new document opening and on switching between viewparts
        
    };
    
    this.loadTable = function()
    {
        this.table.html('<div>'+resources.commonLoading+'</div>');
        var params = {
                de: _this.simulation.diagramName,
                simulation: _this.simulation.simulationName,
                type: "simulation_editor"
            };
        
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
               // "bLengthChange": false,
                "iDisplayLength": 50,
                //"bInfo": false,
                "sPaginationType": "full_numbers_no_ellipses",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sDom": "rtpi",
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?" + toURI(params),
                "fnDrawCallback": function( nRow, aData, iDisplayIndex ) {
                    if(_this.tableObj != undefined && _this.selectedParameters)
                    {
                        setTableSelectedRows(_this.tableObj, _this.selectedParameters);
                        var rows = getTableSelectedRowsValues(_this.tableObj, "Value");
                        if(rows.length>0)
                            _this.selectionDiv.html("<b>Selected: </b> " + rows.map(function(r){return r.name + " [" + r.value + "]";}).join(", "));
                        else
                            _this.selectionDiv.html("");
                    }
                },
            };
            _this.tableObj.addClass('selectable_table');
            _this.tableObj.dataTable(features);
            _this.tableObj.css('width', '100%');
        }, function(data)
        {
            _this.table.html(resources.commonErrorViewpartUnavailable);
            _this.tableObj = null;
            logger.error(data.message);
        });
    };
    
    this.save = function() 
    {
    };
    
    this.initActions = function(toolbarBlock)
    {
        this.resetAction = createToolbarButton("Reset parameters", "apply.gif", this.resetActionClick);
        toolbarBlock.append(this.resetAction);
        this.increaseAction = createToolbarButton("Increase parameter", "icon_plus.gif", this.increaseActionClick);
        toolbarBlock.append(this.increaseAction);
        this.decreaseAction = createToolbarButton("Decrease parameter", "icon_minus.gif", this.decreaseActionClick);
        toolbarBlock.append(this.decreaseAction);
        this.runAction = createToolbarButton("Run simulation", "simulate.gif", this.runActionClick);
        toolbarBlock.append(this.runAction);
        this.saveAction = createToolbarButton("Save parameters to diagram", "export.gif", this.saveActionClick);
        toolbarBlock.append(this.saveAction);
        this.reloadAction = createToolbarButton("Reload parameters from diagram", "import.gif", this.reloadActionClick);
        toolbarBlock.append(this.reloadAction);
    };
    
    this.resetActionClick = function()
    {
        var rows = getTableSelectedRows(_this.tableObj);
        if(rows.length>0)
            _this.rowParametersAction("reset", false);
        else //all rows to reset, don't save table
        {
            _this.selectionDiv.html("");
            _this.selectedParameters = rows;
            queryBioUML("web/simulation/parameters_reset",
                {
                    de: _this.simulation.simulationName,
                    jsonrows: $.toJSON(rows)
                }, function(data)
                {
                    _this.loadTable();
                    _this.simulation.update();
                });
        }
    };
    
    this.increaseActionClick = function()
    {
        _this.rowParametersAction("increase", true);
    };
    
    this.decreaseActionClick = function()
    {
        _this.rowParametersAction("decrease", true);
    };
    
    this.rowParametersAction = function(actionName)
    {
        var rows = getTableSelectedRows(_this.tableObj);
        if(rows.length==0)
        {
            logger.message("Please, select at least one variable.");
            return;
        }
        _this.selectedParameters = rows;
        var dataParams = {
                rnd: rnd(),
                action: 'change',
                de: _this.simulation.diagramName,
                simulation: _this.simulation.simulationName,
                type: "simulation_editor"
            };
        saveChangedTable(_this.tableObj, dataParams, function(data){
            if(_this.tableObj != undefined){
                _this.tableObj.fnClearTable( 0 );
                _this.tableObj.fnDraw();
            }
            queryBioUML("web/simulation/parameters_" + actionName,
            {
                de: _this.simulation.simulationName,
                jsonrows: $.toJSON(rows)
            }, function(data)
            {
                _this.loadTable();
                _this.simulation.update();
            });
        });
    };
    
    this.runActionClick = function()
    {
        var dataParams = {
                rnd: rnd(),
                action: 'change',
                de: _this.simulation.diagramName,
                simulation: _this.simulation.simulationName,
                type: "simulation_editor"
            };
        saveChangedTable(_this.tableObj, dataParams, function(data){
            if(_this.tableObj != undefined){
                _this.tableObj.fnClearTable( 0 );
                _this.tableObj.fnDraw();
            }
            queryBioUML("web/simulation/simulation_document_update",
                {
                    de: _this.simulation.simulationName
                }, function(data2)
                {
                    _this.simulation.update();
                });
        });
    };
    
    this.saveActionClick = function()
    {
        var dataParams = {
                rnd: rnd(),
                action: 'change',
                de: _this.simulation.diagramName,
                simulation: _this.simulation.simulationName,
                type: "simulation_editor"
            };
        saveChangedTable(_this.tableObj, dataParams, function(data){
            if(_this.tableObj != undefined){
                _this.tableObj.fnClearTable( 0 );
                _this.tableObj.fnDraw();
            }
            queryBioUML("web/simulation/save_to_diagram",
                {
                    de: _this.simulation.simulationName
                }, function(data2)
                {
                    _this.simulation.update();
                });
        });
    };
    
    this.reloadActionClick = function()
    {
        _this.simulation.createSimulationDocument( function()
        {
            _this.loadTable();
            _this.simulation.update();
        });
    };
}