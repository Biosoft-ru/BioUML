/**
 * View parts for table
 * @author anna
 */
/*
 *  Initialize view parts for table 
 */
function initTableViewParts()
{
    if(viewPartsInitialized['table'])
        return;
        
    var filtersViewPart = new FiltersViewPart();
    filtersViewPart.init();
    //newViewParts.push(filtersViewPart);
    
    var columnsViewPart = new ColumnsViewPart();
    columnsViewPart.init();
    //newViewParts.push(columnsViewPart);
    
    var detailsViewPart = new DetailsViewPart();
    detailsViewPart.init();
    
    var genomeBrowserViewPart = new GenomeBrowserViewPart();
    genomeBrowserViewPart.init();
    
    var siteColorsViewPart = new SiteColorsViewPart();
    siteColorsViewPart.init();
    
    var structuresViewPart = new StructuresViewPart();
    structuresViewPart.init();
    
    viewParts.splice(0,0, filtersViewPart, columnsViewPart, genomeBrowserViewPart, siteColorsViewPart, detailsViewPart, structuresViewPart);    
    viewPartsInitialized['table'] = true;
}

/*
 * Filter view part class
 */
function FiltersViewPart()
{
    this.tabId = "table.filters";
    this.tabName = resources.vpTableFilterTitle;
    this.tabDiv;
    
    var _this = this;
    
    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if (documentObject != null && documentObject instanceof Table) 
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
        if (documentObject != null && documentObject instanceof Table)
        {
            this.currentTable = documentObject;
            this.tabDiv.html("");
            this.tabDiv.append(getColumnsFilterPane(this.tabId, this.currentTable, resources.tblExpressionTemplatesFilter));
            this.filterField = $(getJQueryIdSelector(this.tabId+"_filter_field"));
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
        this.applyAction = createToolbarButton(resources.vpTableFilterButtonApply, "apply.gif");
        this.applyAction.click(function()
        {
            _this.applyActionClick();
        });
        toolbarBlock.append(this.applyAction);
        
        this.removeAction = createToolbarButton(resources.vpTableFilterButtonClear, "removefilter.gif");
        this.removeAction.click(function()
        {
            _this.removeActionClick();
        });
        toolbarBlock.append(this.removeAction);
        
        this.exportAction = createToolbarButton(resources.vpTableFilterButtonExport, "export.gif");
        this.exportAction.click(function()
        {
            _this.exportActionClick();
        });
        toolbarBlock.append(this.exportAction);
    };
    
    this.applyActionClick = function()
    {
        this.currentTable.setFilter(this.filterField.val());
    };
    
    this.removeActionClick = function()
    {
        this.filterField.val("");
        this.currentTable.clearFilter();
    };
    
    this.exportActionClick = function()
    {
        createSaveElementDialog(resources.vpTableFilterSaveDialogTitle,
				"ru.biosoft.table.TableDataCollection", this.currentTable.completeName+" filtered", function(value) {
					_this.currentTable.exportTable(value);
				});
    };
}

/*
 * Columns view part class
 */
function ColumnsViewPart()
{
    this.tabId = "table.columns";
    this.tabName = resources.vpColumnsTitle;
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
        
        this.table = $('<div>'+resources.commonLoading+'</div>');
        this.containerDiv.append(this.table);
        
        _.bindAll(this, _.functions(this));
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if (documentObject != null && documentObject instanceof Table && !documentObject.readOnly) 
        {
            if (instanceOf(getElementClass(documentObject.completeName), "ru.biosoft.table.TableDataCollection"))
            {
                callback(true);
            }
            else
            {
                callback(false);    
            }
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
        if (documentObject != null && documentObject instanceof Table)
        {
            this.currentDocument = documentObject;
            this.loadTable();
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
     * Load table of columns
     */
    this.loadTable = function()
    {
        this.table.html('<div>'+resources.commonLoading+'</div>');
        var params = {
                de: this.currentDocument.completeName,
                type2: "columns"
            };
        _.extend(params, _this.currentDocument.additionalParams);
        queryBioUML("web/table/sceleton", params, 
        function(data)
        {
            _this.table.html(data.values);
            _this.tableObj = _this.table.children("table");
            _this.tableObj.addClass('selectable_table');
            params["rnd"] = rnd();
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
    
    /*
     * Creates toolbar actions for this tab
     */
    this.initActions = function(toolbarBlock)
    {
        this.recalculateAction = createToolbarButton(resources.vpColumnsButtonRefresh, "apply.gif", this.recalculateDocument);
        toolbarBlock.append(this.recalculateAction);
        
        this.addColumnAction = createToolbarButton(resources.vpColumnsButtonAdd, "icon_plus.gif", this.addColumn);
        toolbarBlock.append(this.addColumnAction);
        
        this.removeColumnAction = createToolbarButton(resources.vpColumnsButtonRemove, "removefilter.gif", this.removeColumn);
        toolbarBlock.append(this.removeColumnAction);

        this.convertToValuesAction = createToolbarButton(resources.vpColumnsButtonConvert, "tovalues.png", this.convertToValues);
        toolbarBlock.append(this.convertToValuesAction);
    };
    
    this.addColumn = function()
    {
    	var params = 
    	{
            colaction: "add",
            de: this.currentDocument.completeName
    	};
    	_.extend(params, this.currentDocument.additionalParams);
        queryBioUML("web/table/columns", params, function(data)
        {
            _this.currentDocument.loadTable(true);
            _this.loadTable();
        });
    };
    
    this.removeColumn = function()
    {
        var removeCols = getTableSelectedRows(this.tableObj);
        if(removeCols.length > 0)
        {
            var confirmString = resources.vpColumnsConfirmRemove;
            createConfirmDialog(confirmString, function()
            {
            	var params = 
            	{
                    colaction: "remove",
                    jsoncols: $.toJSON(removeCols),
                    de: _this.currentDocument.completeName
            	};
            	_.extend(params, _this.currentDocument.additionalParams);
            	queryBioUML("web/table/columns", params, function(data)
                {
                    _this.currentDocument.loadTable(true);
                    _this.loadTable();
                    if(data.values != "")
                        logger.message(data.values);
                });
            });
        } else logger.message(resources.vpColumnsNoSelection);
    };
    
    this.convertToValues = function()
    {
        var convertCols = getTableSelectedRows(this.tableObj);
        if(convertCols.length > 0)
        {
            createConfirmDialog(resources.vpColumnsConfirmConvert, function()
            {
            	var params = 
            	{
                    colaction: "toValues",
                    jsoncols: $.toJSON(convertCols),
                    de: _this.currentDocument.completeName
            	};
            	_.extend(params, _this.currentDocument.additionalParams);
            	queryBioUML("web/table/columns", params, function(data)
                {
                    _this.loadTable();
                });
            });
        } else logger.message(resources.vpColumnsNoSelection);
    };
    
    this.recalculateDocument = function()
    {
        this.saveChanges();
    };
    
    this.saveChanges = function()
    {
        if(this.tableObj)
        {    
            var dataDPS = getDPSFromTable(this.tableObj);
            
            var dataParams = {
                    type2: "columns",
                    de: this.currentDocument.completeName,
                    data: convertDPSToJSON(dataDPS)
                };
            if(_this.tableObj != undefined){
                var tableparams = getTableDisplayParameters(_this.tableObj);
                for(par in tableparams)
                {
                    dataParams[par] = tableparams[par];   
                }
            }
            _.extend(dataParams, _this.currentDocument.additionalParams);
            queryBioUML("web/table/change", dataParams,
            function(data)
            {
                if(_this.tableObj != undefined){
                    _this.tableObj.fnClearTable( 0 );
                    _this.tableObj.fnDraw();
                    _this.currentDocument.loadTable(true);
                }
            });
        }
    };
}

function SiteColorsViewPart()
{
    this.tabId = "table.sitecolors";
    this.tabName = resources.vpSiteColorsTitle;
    this.tabDiv;
    
    var _this = this;

    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);
    };

    this.isVisible = function(documentObject, callback)
    {
    	if ((documentObject != null) && (documentObject instanceof Table) && documentObject.innerGenomeBrowser) 
        {
            callback(true);
        }
        else 
        {
            callback(false);
        }
    };

    this.save = function()
    {
        // nothing to do
    };
    
    this.explore = function(documentObject)
    {
    	this.show(documentObject);
    };

    this.show = function(documentObject)
    {
    	if (documentObject != null && documentObject instanceof Table)
        {
            this.currentDocument = documentObject;
        	this.propertyPane = new JSPropertyInspector();
            var parentID = this.tabId + "_container";
            var beanPath = "bsa/genomebrowsercolors/"+this.currentDocument.completeName;
            
        	queryBean(beanPath, {}, function(data)
        	{
        		function syncronizeData(control)
        		{
                    queryBioUML("web/bean/set", 
                    {
                        de: beanPath,
                        json: convertDPSToJSON(_this.propertyPane.getModel())
                    }, function(data)
                    {
                		_.each(_this.currentDocument.innerGenomeBrowser, function(viewPane)
        				{
        					viewPane.updateView(viewPane.clipRectangle.x, viewPane.clipRectangle.y);
        				});
                		_this.containerDiv.empty();
                		var beanDPS = convertJSONToDPS(data.values);
                		_this.propertyPane = new JSPropertyInspector();
                		_this.propertyPane.setParentNodeId(parentID);
                		_this.propertyPane.setModel(beanDPS);
                		_this.propertyPane.generate();
                		_this.propertyPane.addChangeListener(function(control, oldValue, newValue) {
                			syncronizeData(control);
                		});
                    });
        		}
        		
                _this.containerDiv.empty();
        		var beanDPS = convertJSONToDPS(data.values);
        		_this.propertyPane.setParentNodeId(parentID);
        		_this.propertyPane.setModel(beanDPS);
        		_this.propertyPane.generate();
    	    	_this.propertyPane.addChangeListener(function(control, oldValue, newValue) {
    				syncronizeData(control);
    			});
        	});
        }
    };
    
    this.showDelayed = _.debounce(function() {_this.show.call(_this, _this.currentDocument);}, 100);
}

/*
 * View part for controlling genome browser in the table 
 */
function GenomeBrowserViewPart()
{
    this.tabId = "table.genomebrowser";
    this.tabName = resources.vpGenomeBrowserTitle;
    this.tabDiv;
	var labelWidth = 150;
	this.viewPanes = [];
    
    var _this = this;
    
    this.opennedDocuments = {};
    
    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
        
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="viewPartTab"></div>');
        this.tabDiv.append(this.containerDiv);
        
        this.tagPanel = $('<div/>');
        this.containerDiv.append(this.tagPanel);
        
        this.browserPanel = $('<div/>').css({"margin-left": labelWidth+"px", "clear": "both"});
        this.containerDiv.append(this.browserPanel);
        
        $('#viewPartTabs').resize(function(event)
        {
        	if($('#viewPartTabs').has(event.target).length) return;
			if (_this.handler)
				_this.handler();
		});
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
    	if ((documentObject != null) && (documentObject instanceof Table) && documentObject.innerGenomeBrowser) 
        {
            callback(true);
        }
        else 
        {
            callback(false);
        }
    };
    
    this.updateBrowser = function(documentObject)
    {
    	var selection = this.currentTable.find("tr.row_selected");
    	var selectedRow = selection.eq(0);	// only first selected row
    	this.browserPanel.empty();
    	if(selection.length == 0) return;
    	var sequenceName = selectedRow.data("sequenceName");
    	if(!sequenceName) return;
    	var sequenceStart = parseInt(selectedRow.data("sequenceStart"));
    	var sequenceEnd = parseInt(selectedRow.data("sequenceEnd"));
		var sequenceLength = sequenceEnd-sequenceStart+1;
		var curWidth = this.tabDiv.width() - 20 - labelWidth;
		if(curWidth < 100) return;
		var font = new Font("courier new", Font.PLAIN, 12);
		var rulerPane = new RulerViewPane(this.browserPanel, sequenceName, sequenceLength, new ColorFont(font));
		var maxScale = font.getExtent("A", rulerPane.getContext())[0];
		this.viewPanes = [];
		rulerPane.scaleX = curWidth / sequenceLength;
		if(rulerPane.scaleX > maxScale)
		{
			rulerPane.scaleX = maxScale;
			curWidth = sequenceLength * maxScale;
		}
		rulerPane.initScrollUpdate(sequenceStart, 0);
		for(var trackPath in this.currentDocument.innerGenomeBrowserTracks)
    	{
			(function(trackPath)
			{
			    var viewPane = new AjaxViewPane(_this.browserPanel, 
			    {
			        dragAxis: 'none',
			        URL: appInfo.serverPath+"web/data",
			        ajaxParam: 
			        {
			            de: trackPath,
			            sequence: sequenceName,
			            command: 45,
			            service: "bsa.service",
			            logscale: -Math.log(sequenceLength * 3 / curWidth) / Math.log(2)
			        },
			        ajaxParamFromX: "from",
			        ajaxParamToX: "to",
			        scaleX: curWidth/sequenceLength,
			        fullHeight: false
			    });
			    _this.viewPanes.push(viewPane);
		        viewPane.labelDiv = $('<div class="trackLabel"/>')
		        	.css({position: "absolute", left: (-labelWidth-20)+"px", width: labelWidth+"px", top: "0px", whiteSpace: "nowrap"});
		        viewPane.viewPane.append(viewPane.labelDiv);
		        viewPane.viewPane.css("overflow", "visible");
		        var doRepaint = viewPane.doRepaint;
		        viewPane.doRepaint = function()
		        {
		        	this.activeTags = _this.activeTags;
		        	doRepaint.apply(this);
		        };
		        fitElement(viewPane.labelDiv, _this.currentDocument.innerGenomeBrowserTracks[trackPath], true, labelWidth);
			    viewPane.selectHandler = function(elementId, elementBounds)
			    {
			    	showSelector(viewPane.canvasDiv, elementBounds.x, elementBounds.y, elementBounds.width, elementBounds.height);
			    	queryService("bsa.service", 47,
			        {
			            de: trackPath,
			            sequence: sequenceName,
			            site: elementId
			        }, function(data)
			        {
			            $("#info_area").html(data.values);
			        });
			    };
			    viewPane.initScrollUpdate(0, 0);
			})(trackPath);
    	}
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
    	if (documentObject != null && documentObject instanceof Table)
        {
            this.currentDocument = documentObject;
            this.tags = [];
            this.activeTags = {};
            this.tagPanel.empty();
            
            if(this.opennedDocuments[this.currentDocument.completeName])
            {
            	// TODO: erroneous code?
            	this.addTags(this.opennedDocuments[this.currentDocument.completeName]);
            }

        	if(documentObject.table && documentObject.table != this.currentTable)
        	{
            	if(this.currentTable && this.handler)
            	{
            		this.currentTable.unbind("change", this.handler);
            	}
            	this.currentTable = documentObject.table;
            	this.handler = _.debounce(function()
        		{
        			_this.updateBrowser(documentObject);
        		}, 300);
        		this.currentTable.bind("change", this.handler);
        		this.handler();
        	}
        }
    };
    
    /*
     * Save function
     */
    this.save = function()
    {
        // nothing to do
    };
    
    this.addTags = function(newTags)
    {
    	if(_this.currentDocument == null)
    		return;
    	var newCount = 0;
    	if(_this.tags == undefined) _this.tags = [];
    	if(_this.activeTags == undefined) _this.activeTags = {};
    	_.each(_.difference(newTags, _this.tags), function(tag)
    	{
    		_this.tags.push(tag);
			_this.activeTags[tag] = 1;
    		newCount++;
    	});
    	if(newCount>0)
    	{
            for(var i=_this.tags.length - newCount; i<_this.tags.length; i++)
            {
            	var tag = _this.tags[i];
    			var div = $("<div/>").css({"float": "left", "min-width": "200pt"}).text(tag);
    			_this.tagPanel.append(div);
    			
    			var input = $("<input type='checkbox'/>").data("value", tag);
    			input.change(function()
    		    {
    		    	if($(this).attr("checked")) _this.activeTags[$(this).data("value")] = 1;
    		    	else delete _this.activeTags[$(this).data("value")];
    		    	
    		    	_.each(_this.currentDocument.innerGenomeBrowser.concat(_this.viewPanes), function(e)
    		    	{
    		    		e.activeTags = _this.activeTags;
    		    		e.invalidateTiles();
    		    		e.repaint();
    		    	});
    		    });
    			if(_this.activeTags[tag]) input.attr("checked", "checked");
    			div.prepend(input);
    		}
    		_this.opennedDocuments[_this.currentDocument.completeName] = _this.tags;
    	}
    };
}

function getExpressionDialog(id, caller)
{
    var dialogDiv = $('<div title="'+resources.dlgColumnExpressionTitle+'"></div>');
    dialogDiv.append(getColumnsFilterPane("expr_dialog", opennedDocuments[activeDocumentId], resources.tblExpressionTemplatesColumn));
    var oldVal = $(caller).parents('table').find(getJQueryIdSelector(id)).val();
    if(oldVal )
    	dialogDiv.find("#expr_dialog_filter_field").val(oldVal);
    
    dialogDiv.dialog(
    {
        autoOpen: false,
        width: 500,
        buttons: 
        {
            "Ok": function()
            {
                $(caller).parents('table').find(getJQueryIdSelector(id)).val($("#expr_dialog_filter_field").val());
                $(this).dialog("close");
                $(this).remove();
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

function getColumnsFilterPane(id, table, templates)
{
    var containerDiv = $('<div id="columns_filter"></div>');
    
    var columnSelect = $('<select></select>').attr("size", "6");
    var templateParams = $('<div/>').addClass("expressionTemplateParameters");
    var templateSelector = $('<select/>').addClass("expressionTemplateSelector").append($("<option/>").text(resources.tblExpressionSelectTemplate).val(-1));
    containerDiv.append($("<table/>")
    		.append($("<tr/>")
    				.append($("<th/>").append(resources.tblExpressionTemplatePrompt))
    				.append($("<th/>").css({paddingLeft: "3em"}).append(resources.tblExpressionColumnList))
    		)
    		.append($("<tr/>")
    				.append($("<td valign='top' align='right'/>").append(templateSelector).append(templateParams))
    				.append($("<td valign='top'/>").css({paddingLeft: "3em"}).append(columnSelect))
    		));
    containerDiv.append(resources.tblExpressionPrompt);
    containerDiv.append($('<br/>'));
    var filterField = $('<input type="text" id="'+id+'_filter_field">').css("width", "90%");
    containerDiv.append(filterField).append("<br/><br/>");
    var descDiv = $('<div></div>').html(resources.tblExpressionHint);
    containerDiv.append(descDiv);
    table.getColumnList(function(columnList)
    {
        for(var i=0; i < columnList.length; i++)
        {
            columnSelect.append('<option value="' + columnList[i] + '">' + columnList[i] + '</option>');
        }
        for(var i=0; i<templates.length; i++)
        {
        	templateSelector.append($("<option/>").text(templates[i][0]).val(i));
        }
        var paramNames = [];
        var paramValues = [];
        var paramMap = {};
        templateSelector.change(function()
        {
        	var index = templateSelector.val();
        	templateParams.empty();
        	if(index == -1) return;
        	var template = templates[index];

        	paramValues = new Array(template.length-2);
        	paramNames = [];
        	
        	var updateFromTemplate = function()
        	{
        		var templateStr = template[1];
        		for(var i=0; i<paramValues.length; i++)
    			{
        			templateStr = templateStr.replace(RegExp("\\$"+(i+1), "g"), paramValues[i]);
    			}
    			filterField.val(templateStr);
        	};
        	for(var i=0; i<template.length-2; i++)
    		{
        		var paramType = template[i+2].substring(0,1);
        		var paramName = template[i+2].substring(2);
        		paramNames.push(paramName);
        		
        		var paramControlCreators = 
        		{
    				C: function(i, paramName)
    				{
            			var paramControl = $('<select/>');
            			var selected = paramMap[paramName];
            			for(var j=0; j < columnList.length; j++)
            				paramControl.append('<option value="' + columnList[j] + '"'+(selected==columnList[j]?" selected":"")+'>' + columnList[j] + '</option>');
            			paramControl.change(
           					function()
        					{
        						paramValues[i] = paramControl.val();
        						paramMap[paramName] = paramControl.val();
        						updateFromTemplate();
        					});
            			return paramControl;
    				},
    				N: function(i, paramName)
    				{
    					var paramControl = $('<input type="text"/>');
    					paramControl.val(paramMap[paramName]);
    					paramControl.bind("change keyup",
    						function()
    						{
    							paramValues[i] = parseFloat(paramControl.val()==""?0:paramControl.val());
        						paramMap[paramName] = paramControl.val();
    							updateFromTemplate();
    						});
    					return paramControl;
    				},
    				S: function(i, paramName)
    				{
    					var paramControl = $('<input type="text"/>');
    					paramControl.val(paramMap[paramName]);
    					paramControl.bind("change keyup",
    						function()
    						{
    							paramValues[i] = "'"+paramControl.val().replace(/([\'\"\\])/g, "\\$1")+"'";
        						paramMap[paramName] = paramControl.val();
    							updateFromTemplate();
    						});
    					return paramControl;
    				}
        		};
        		var paramControl = paramControlCreators[paramType](i, paramName);
        		paramControl.change();
        		templateParams.append($("<span/>").text(paramName+": ")).append(paramControl).append("<br/>");
    		}
        });
    
        columnSelect.dblclick(function()
        {
        	filterField.insertAtCaret(columnSelect.val());
        });
    });
    return containerDiv;
}

/*
 * Details view part class
 */
function DetailsViewPart()
{
    this.tabId = "table.details";
    this.tabName = resources.vpTableDetailsTitle;
    this.tabDiv;
    
    var _this = this;
    
    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId).addClass("elementDescription");
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if (documentObject != null && documentObject instanceof Table && documentObject.table && documentObject.table.find(".hiddenDetails").length) 
        {
            callback(true);
        }
        else 
        {
            callback(false);
        }
    };
    
    this.updateTab = function(documentObject)
    {
        this.tabDiv.empty();
        var content = $();
        var table = documentObject.table;
        if(table == undefined) return;
        table.find(".row_selected").each(function()
        {
        	var row = $(this);
        	var newContent = $("<div/>").append($("<h3/>").text(row.children("td").eq(0).text()));
        	row.find(".hiddenDetails").each(function()
        	{
        		var details = $(this);
        		var text = details.parent().find(".summaryText").text();
        		if(text == "" && details.text() == "") return;
        		var index = row.children("td").index(details.closest("td"));
        		var block = $("<div/>").css("margin-left", "2em");
        		block.append($("<strong/>").text(table.find("th").eq(index).text()+" ("+text+")"));
        		var detailsBlock = $("<div/>").css("margin-left", "2em").html(details.html());
        		detailsBlock.find("a").attr("target", "_blank");
        		block.append(detailsBlock);
        		newContent.append(block);
        	});
        	content = content.add(newContent);
        });
        this.tabDiv.append(content);
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if (documentObject != null && documentObject instanceof Table)
        {
        	if(documentObject.table && documentObject.table != this.currentTable)
        	{
            	var _this = this;
            	if(this.currentTable && this.handler)
            	{
            		this.currentTable.unbind("change", this.handler);
            	}
            	this.currentTable = documentObject.table;
            	this.handler = function()
        		{
        			_this.updateTab(documentObject);
        		};
        		this.currentTable.bind("change", this.handler);
        		this.handler();
        	}
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
    };
}

/*
 * Structures view part class
 */
function StructuresViewPart()
{
    this.tabId = "table.structure";
    this.tabName = resources.vpTableStructureTitle;
    this.tabDiv;
    
    var _this = this;
    
    /*
     * Create div for view part
     */
    this.init = function()
    {
        this.tabDiv = createViewPartContainer(this.tabId);
    };
    
    /*
     * Indicates if view part is visible
     */
    this.isVisible = function(documentObject, callback)
    {
        if (documentObject != null && documentObject instanceof Table && documentObject.table && documentObject.table.find(".structureDetails").length) 
        {
            callback(true);
        }
        else 
        {
            callback(false);
        }
    };
    
    this.updateTab = function(documentObject)
    {
        this.tabDiv.empty();
        var newContent = $("<div/>");
        var table = documentObject.table;
        if(table == undefined) return;
        table.find(".row_selected").each(function()
        {
        	var row = $(this);
        	row.find(".structureDetails").each(function()
        	{
        		var details = $(this);
        		var text = details.parent().children("span").text();
        		if(text == "" && details.text() == "") return;
        		
        		jmolInitialize("lib/jmol");
        		jmolSetDocument(false);
        		jmolSetAppletColor('white');
        		var jmol = jmolApplet(300, "load "+details.text());
        		newContent.html(jmol);
        	});
        });
        this.tabDiv.append(newContent);
    };
    
    /*
     * Open document event handler
     */
    this.explore = function(documentObject)
    {
        if (documentObject != null && documentObject instanceof Table)
        {
        	if(documentObject.table && documentObject.table != this.currentTable)
        	{
            	var _this = this;
            	if(this.currentTable && this.handler)
            	{
            		this.currentTable.unbind("change", this.handler);
            	}
            	this.currentTable = documentObject.table;
            	this.handler = function()
        		{
        			_this.updateTab(documentObject);
        		};
        		this.currentTable.bind("change", this.handler);
        		this.handler();
        	}
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
    };
}

