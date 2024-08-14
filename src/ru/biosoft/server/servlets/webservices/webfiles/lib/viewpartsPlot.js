/**
 *	View parts for plot document 
 *
 *  @author anna
 */
function initPlotViewParts()
{
	if(viewPartsInitialized['plot'])
        return;
        
    var plotEditorViewPart = new PlotEditorViewPart();
    plotEditorViewPart.init();
    viewParts.push(plotEditorViewPart);

    var tableViewPart = new PlotTableViewPart();
    tableViewPart.init();
    viewParts.push(tableViewPart);
    
    viewPartsInitialized['plot'] = true;
}

function PlotEditorViewPart()
{
    this.tabId = "plot.editor";
    this.tabName = "Plot editor";
    this.tabDiv;
    this.visible = true;
    
    var _this = this;
    
    /*
	 * Create div for view part
	 */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);
        this.table = $('<div>'+resources.dlgPlotEditorLoading+'</div>');
        this.containerDiv.append(this.table);
    };
    
    /*
	 * Indicates if view part is visible
	 */
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof PlotDocument)) 
        {
            callback(true);
        }
        else 
        {
            callback(false);
        }
    };
    
    /*
     * explore view part
     */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof PlotDocument)) 
        {
            if( this.currentPlot == documentObject )
                return;
            this.currentPlot = documentObject;
            this.prevSeriesPath = undefined;
            this.loadTable();
        }
    };
    
    /*
	 * Creates toolbar actions for this tab
	 */
    this.initActions = function(toolbarBlock)
    {
        //TODO: move messages to messageBundle.js
        this.addAction = createToolbarButton("Add series", "icon_plus.gif");
        this.addAction.click(function()
        {
        	_this.addSeries();
        });
        toolbarBlock.append(this.addAction);
        
        this.removeAction = createToolbarButton("Remove series", "remove.gif");
        this.removeAction.click(function()
        {
            _this.removeSeries();
        });
        toolbarBlock.append(this.removeAction);
        
        this.saveAction = createToolbarButton(resources.dlgPlotEditorSeriesSave, "save.gif");
        this.saveAction.click(function()
        {
            _this.saveTable(function(){
                _this.updatePlot();
            });
        });
        toolbarBlock.append(this.saveAction);
        
        this.editAction = createToolbarButton(resources.dlgPlotEditorEdit, "edit.gif", function(){
            createBeanEditorDialog(resources.dlgPlotEditorEdit,_this.currentPlot.completeName, function() {
                _this.updatePlot();
            }, true);
        });
        toolbarBlock.append(this.editAction);
    };
   
    /*
	 * Save function
	 */
    this.save = function()
    {
    };
    
    this.loadTable = function()
    {
        this.table.html('<div>'+resources.dlgPlotEditorLoading+'</div>');
        var params = {
                "de": this.currentPlot.completeName,
                type: "plot",
                add_row_id: 1,
                "rnd": rnd()
            };
        queryBioUML("web/table/sceleton", params, 
        function(data)
        {
            _this.table.html(data.values);
            _this.tableObj = _this.table.children("table");
            _this.tableObj.addClass('selectable_table');
            params["rnd"] = rnd();
            params["read"] = false;
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "bPaginate": false,
                "sDom": "rti",
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?" + toURI(params) 
            };
            _this.tableObj.dataTable(features);
            _this.tableObj.css('width', '100%');
        });
    };
    
    this.addSeries = function()
    {
        var defaultPath = null;
        if(_this.prevSeriesPath)
            defaultPath = _this.prevSeriesPath;
        else if(_this.currentPlot.additionalPath)
            defaultPath = _this.currentPlot.additionalPath;
        else
        {
            var usedSeriesPaths = getTableColumnValues(_this.tableObj, "Source");
            if(usedSeriesPaths.length > 0)
                defaultPath = usedSeriesPaths[usedSeriesPaths.length-1];
        }
        addPlotSeriesDialog(_this.currentPlot.completeName, defaultPath, function(data, source){
            if(source)
                _this.prevSeriesPath = source;
            _this.loadTable();
            _this.updatePlot();
        });
    };
    
    this.removeSeries = function()
    {
        var indices = getTableSelectedRowIds(_this.tableObj);
        if(indices == null || indices.length == 0)
            return false;
        
        queryBioUML("web/plot/remove",
        {
            de: _this.currentPlot.completeName,
            series : $.toJSON(indices)
        }, function(data)
        {
            _this.loadTable();
            _this.updatePlot();
        });
    };
    
    this.loadVariables= function(completeName, pathName, callback)
    {
        queryBioUML("web/plot/variables", 
		{
	        de: completeName,
	        path: pathName
        }, callback); 
    };
    
    this.loadPaths = function(completeName, callback)
    {
        queryBioUML("web/plot/paths", 
                {
                    de: completeName
                }, callback);
    };
    
    
    this.fillVariablesCombos = function (comboX, valuesX, comboY,  valuesY)
    {
        if (valuesX) 
        {
            comboX.empty();
            $.each(valuesX, function(index, value)
            {
                comboX.append($('<option></option>').val(value).text(value));
            });
        }
        if (valuesY) 
        {
            comboY.empty();
            $.each(valuesY, function(index, value)
            {
                comboY.append($('<option></option>').val(value).text(value));
            });
        }
    };
    
    this.saveTable = function(callback)
    {
        if (this.tableObj) 
        {
            var dataParams = {
                rnd: rnd(),
                action: 'change',
                de: this.currentPlot.completeName,
                type: "plot"
            };
            saveChangedTable(this.tableObj, dataParams, callback);
        }
        else
        {
            callback();    
        }
    };
    
    this.updatePlot = function()
    {
        _this.currentPlot.update(function(){
            var vp = lookForViewPart('plot.table');
            if(vp)
                vp.reloadData();
         });
    };
}


function PlotTableViewPart()
{
    this.tabId = "plot.table";
    this.tabName = "Table";
    this.tabDiv;
    
    var _this = this;
    
    /*
	 * Create div for view part
	 */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);
        
        this.allColumns = $('<select/>');
        this.addButton = $('<input type="button" value="Add"/>');
        this.visibleColumns = $('<select/>');
        this.removeButton = $('<input type="button" value="Remove"/>');
        this.lowerTimeLimit = $('<input type="text"/>');
        this.upperTimeLimit = $('<input type="text"/>');
        this.timeStep = $('<input type="text"/>');
        this.timeFilterButton = $('<input type="button" value="Apply time filter"/>');
        var colfilters = $('<table/>');
        colfilters.append($('<tr></tr>')
                        .append($('<td></td>').append($('<b>All columns:</b>')))
                        .append($('<td></td>').append(this.allColumns))
                        .append($('<td></td>').append(this.addButton)));
        colfilters.append($('<tr></tr>')
                        .append($('<td></td>').append($('<b>Visible columns:</b>')))
                        .append($('<td></td>').append(this.visibleColumns))
                        .append($('<td></td>').append(this.removeButton)));
        this.containerDiv.append(colfilters);
        
        var filters = $('<table/>');
        filters.append($('<tr></tr>')
                        .append($('<td></td>').append($('<b>Lower time limit:</b>')))
                        .append($('<td></td>').append(this.lowerTimeLimit))
                        .append($('<td></td>').append($('<b>Upper time limit:</b>')))
                        .append($('<td></td>').append(this.upperTimeLimit)));
        filters.append($('<tr></tr>')
                        .append($('<td></td>').append($('<b>Time step:</b>')))
                        .append($('<td></td>').append(this.timeStep))
                        .append($('<td></td>').append(this.timeFilterButton))
                        .append($('<td></td>')));
        this.containerDiv.append(filters);
        
        this.table = $('<div>'+resources.dlgPlotEditorLoading+'</div>');
        this.containerDiv.append(this.table);
    };
    
    /*
	 * Indicates if view part is visible
	 */
    this.isVisible = function(documentObject, callback)
    {
        if ((documentObject != null) && (documentObject instanceof PlotDocument)) 
        {
            callback(true);
        }
        else 
        {
            callback(false);
        }
    };
    
    /*
     * explore view part
     */
    this.explore = function(documentObject)
    {
        if ((documentObject != null) && (documentObject instanceof PlotDocument)) 
        {
            if( this.currentPlot == documentObject )
                return;
            this.currentPlot = documentObject;
            this.reloadData();
        }
    };
    
    /*
	 * Save function
	 */
    this.save = function()
    {
    };
    
    this.reloadData = function()
    {
        this.table.html('<div>' + resources.dlgPlotEditorLoading + '</div>');
        var params = {
            "de": this.currentPlot.completeName
        };
        queryBioUML("web/plot/filters", params, function(data)
        {
            if (data.values.seriesColumns && data.values.timeFilter) 
            {
                _this.seriesColumns = data.values.seriesColumns;
                _this.initColumns();
                _this.times = data.values.timeFilter;
                _this.initTimes();
                _this.loadTable();
            }
            else
            {
                _this.table.html("Table is not available");
            }
            
        },
        function(){_this.table.html("Table is not available");});
    };
    
    /*
	 * Creates toolbar actions for this tab
	 */
    this.initActions = function(toolbarBlock)
    {
        this.addButton.click(function()
        {
            var index = _this.allColumns[0].selectedIndex;
            var value = _this.allNames[index];
            var str = value[0] + " " + value[1];
            var option = _this.visibleColumns.find('option[value="'+str+'"]')[0];
            if( option )
                return;
            var name = getElementName(value[0]) + " " + value[1];
            _this.visibleColumns.append($('<option></option>').val(str).text(name));
            _this.visibleNames.push(value);
            _this.loadTable();
        });
        
        this.removeButton.click(function(){
            var index = _this.visibleColumns[0].selectedIndex;//.val();
            _this.visibleNames.splice(index, 1);
            _this.visibleColumns.find('option:selected').remove();
            _this.loadTable();
        });
        
        this.timeFilterButton.click(function(){
            _this.times[0] = _this.lowerTimeLimit.val();
            _this.times[1] = _this.upperTimeLimit.val();
            _this.times[2] = _this.timeStep.val();
            _this.loadTable();
        });
        
    };
   
    this.initColumns = function()
    {
        this.allColumns.find('option').remove();
        this.visibleColumns.find('option').remove();
        this.visibleNames = new Array();
        this.allNames = new Array();
        
        $.each(_this.seriesColumns, function(index, value)
        {
            var name = getElementName(value[0]) + " " + value[1];
            _this.allColumns.append($('<option></option>').val(name).text(name));
            _this.allNames[index] = value;
            _this.visibleColumns.append($('<option></option>').val(value[0] + " " + value[1]).text(name));
            _this.visibleNames[index] = value;
        });
    };
    
    this.initTimes = function()
    {
        this.lowerTimeLimit.val(this.times[0]);
        this.upperTimeLimit.val(this.times[1]);
        this.timeStep.val(this.times[2]);
    };

    this.loadTable = function()
    {
        this.table.html('<div>'+resources.dlgPlotEditorLoading+'</div>');
        var params = {
                "de": this.currentPlot.completeName,
                type: "plot",
                read: true,
                times: $.toJSON(this.times),
                names: $.toJSON(_this.visibleNames)
            };
        queryBioUML("web/table/sceleton", params, 
        function(data)
        {
            _this.table.html(data.values);
            _this.tableObj = _this.table.children("table");
            _this.tableObj.addClass('selectable_table');
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "bSort": false,
                "sPaginationType": "input",
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "sDom": "rtip",
                "fnHeaderCallback": function( nHead, aasData, iStart, iEnd, aiDisplay ) { 
                    nHead.getElementsByTagName('th')[0].innerHTML = "Time"; 
                },
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?" + toURI(params) 
            };
            _this.tableObj.dataTable(features);
            _this.tableObj.css('width', '100%');
        });
    };
}
