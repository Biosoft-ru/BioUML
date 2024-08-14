/**
 * Useful UI functions for BioUML
 */

function getIconUri(icon)
{
    var pos = icon.indexOf("/");
    if(pos < 0)
        return "icons/"+icon;
    if(pos == 0)
        return appInfo.serverPath+icon.substring(1);
    return icon.replace(/([\'\"\(\)\s])/g, '\\$1');
}

function createToolbarButton(title, icon, action)
{
	var iconUri = getIconUri(icon);
	var button = $('<span class="fg-button ui-state-default fg-button-icon-solo ui-corner-all">'+
			'<img class="fg-button-icon-span" src="'+iconUri+'"></img>'+
			'</span>').attr("title", title);
	if(action) button.click(function()
	{
		if(!button.hasClass("ui-state-disabled")) action.apply(this, arguments);
	});
	return button;
}

function createDisabledToolbarButton(title, icon, action)
{
	var button = createToolbarButton(title, icon, action);
	button.removeClass("ui-state-default").addClass("ui-state-disabled");
	return button;
}

/**
 * Will set the enabled state (true/false) of arbitrary number of buttons
 * setToolbarButtonEnabled(button1, button2, ..., state)
 */
function setToolbarButtonEnabled()
{
	var state = arguments[arguments.length-1];
	for(var i=0; i<arguments.length-1; i++)
	{
		var button = arguments[i];
		if(button)
		{
			if(state)
			{
				button.addClass('ui-state-default');
				button.removeClass('ui-state-disabled');
			} else
			{
				button.addClass('ui-state-disabled');
				button.removeClass('ui-state-default');
		        button.removeClass('ui-state-hover');
			}
		}
	}
}

var globalSelectorDiv; 
function removeSelector()
{
	if(globalSelectorDiv)
	{
		globalSelectorDiv.remove();
		globalSelectorDiv = null;
	}
}

BioUML.selection.addListener(removeSelector);

/*
 * Element selector
 */
function showSelector(parent, x, y, width, height)
{
	removeSelector();
    var selectorDiv = $("<div class='selector'></div>").css('position', 'absolute');
    var selectorDivDotes = $("<div class='selector_dotes'></div>").css('opacity', .4);
    selectorDiv.append(selectorDivDotes);
    parent.append(selectorDiv);
    selectorDiv.css('left', x - 2).css('top', y - 2).css('width', width + 4).css('height', height + 4);
    selectorDiv.children(".selector_dotes").css('left', x).css('top', y).css('width', width).css('height', height);
    selectorDiv.show();
    globalSelectorDiv = selectorDiv;
    return selectorDiv;
}

function bindAppear(elements, f)
{
	elements.each(function()
	{
		var t = $(this);
		
		var ps = t.parents();
		
		var check = function()
		{
			for(var i=0; i<ps.length; i++)
			{
				var p = ps.eq(i);
				var rect1 = new Rectangle(0, 0, p.width(), p.height());
				var rect2 = new Rectangle(t.offset().left - p.offset().left, t.offset().top - p.offset().top, t.width(), t.height());
				if(!rect1.intersects(rect2)) return;
			}
			ps.each(function ()
			{
				var p = $(this);
				p.unbind("scroll", check);
			});
			f();
		};
		
		ps.each(function ()
		{
			var p = $(this);
			p.scroll(check);
		});
		
		check();
	});
}

function showGenomeBrowser(parentDivId, projectValues)
{
	var parent = $(getJQueryIdSelector(parentDivId));
	//set flag to document to show special view part
	var documentId = parent.closest(".documentTab").parent().attr("id");
	if(opennedDocuments[documentId] && !opennedDocuments[documentId].innerGenomeBrowser)
	{
		opennedDocuments[documentId].innerGenomeBrowser = [];
		opennedDocuments[documentId].innerGenomeBrowserTracks = {};
		updateViewParts();
	}
	var curWidth = 600;
	parent.width(curWidth);

    var bean = convertJSONToDPS(projectValues);
	var region = bean.getProperty("regions").getValue()[0];
	var sequenceName = region.getProperty("sequenceName").getValue();
	var sequenceStart = parseInt(region.getProperty("from").getValue());
	var sequenceEnd = parseInt(region.getProperty("to").getValue());
	var sequenceLength = sequenceEnd-sequenceStart+1;
	parent.closest("tr").data("sequenceName", sequenceName).data("sequenceStart", sequenceStart).data("sequenceEnd", sequenceEnd);
	var tracks = bean.getProperty("tracks").getValue();
    var trackNumber = 1;
    var availableTracks = {};
	
	for(var i=0; i<tracks.length; i++)
	{
		var path = tracks[i].getProperty("dbName").getValue();
        availableTracks[trackNumber++] = 
        {
            displayName: tracks[i].getProperty("title").getValue(),
            de: path
        };
        if( documentId )
    	    opennedDocuments[documentId].innerGenomeBrowserTracks[path] = tracks[i].getProperty("title").getValue();
        else
        {
        	parent.innerGenomeBrowser = [];
        	parent.innerGenomeBrowserTracks = {};
        	parent.innerGenomeBrowserTracks[path] = tracks[i].getProperty("title").getValue();
        }
	}

	bindAppear(parent, function()
	{
		for(var i in availableTracks)
		{
			(function(trackPath)
			{
			    var viewPane = new AjaxViewPane(parent, 
			    {
			        dragAxis: 'none',
			        URL: appInfo.serverPath+"web/data",
			        ajaxParam: 
			        {
			            de: trackPath,
			            mode: "compact",
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
			    
			    if(opennedDocuments[documentId] && opennedDocuments[documentId].innerGenomeBrowser)
					opennedDocuments[documentId].innerGenomeBrowser.push(viewPane);
			})(availableTracks[i].de);
		}
	});
}

function showDataElementLink(parentDiv, path, iconId, title, exists)
{
    if(title.length > 40)
        title = title.substring(0,38)+"...";
	if(typeof(parentDiv) == "string")
		parentDiv = $(document.getElementById(parentDiv));
	else
		parentDiv = $(parentDiv);
	if(!exists)
	{
		parentDiv.append($("<span/>").text(title+" ").attr("title", path+" (not found)"));
		return;
	}
	var a = $("<span/>").attr("data-path", path).attr("title", path).addClass("dataElementLink");
	if(iconId)
	{
		a.append($("<img/>").attr("src", appInfo.serverPath+"web/img?"+toURI({id: iconId})).css("height", "16px"));
	}
	a.append($("<span/>").text(title));
	a.click(function(e) {
		openDocument(path);
		return false;
	});
	parentDiv.append(a);
	createTreeItemDraggable(a);
	addTreeItemContextMenu(a);
}

function showViewPane(parentDivId, viewStr)
{
	var parent = $("#"+parentDivId);
	var viewPane = new ViewPane(parent, {
		dragAxis: 'none',
		fullWidth: false,
		fullHeight: false,
		tile: 20
	});
	try
	{
		var viewJson = JSON.parse( viewStr );
		var viewObj = CompositeView.createView(viewJson, viewPane.getContext());
        viewPane.setView(viewObj);
        if(viewPane.height > 50.0)
        {
            var scale = 50.0/viewPane.height;
            viewPane.scale(scale, scale);
            
            parent.click(function(){
                var dialogDiv = $('<div title="Fullsize view"></div>');
                var width = Math.min(700, viewObj.getBounds().width + 20);
                dialogDiv.dialog(
                {
                    autoOpen: false,
                    width: width,
                    buttons: 
                    {
                        "Ok": function()
                        {
                            $(this).dialog("close");
                            $(this).remove();
                        }
                    }
                });
                var viewPaneComplete = new ViewPane(dialogDiv, {
            		dragAxis: 'none',
            		fullWidth: false,
            		fullHeight: false,
            		tile: 20
            	});
                viewPaneComplete.setView(viewObj);
                viewPaneComplete.repaint();
                dialogDiv.dialog("open");
            }); 
        }
        viewPane.backgroundBrush = new Brush(new Color(0,0,0,0));
        viewPane.repaint();
	}
	catch(e)
	{
		parent.html("<p>"+e+"</p>");
	} 
}

/**
 * Insert analysis launch control which is simpler than analysis document (no expert options and log)
 * @param parentDiv - div to insert the control to
 * @param analysis - analysis path
 * @param options.params - predefined analysis parameters if necessary
 * @param options.autoOpen - auto open results
 * @param options.showProgress - show task progress bar
 * @param options.runButtonTitle - title for run button
 * @param options.allowCanceling - allow to cancel started job
 */
function showAnalysisControl(parentDiv, analysis, options)
{
    if (typeof(options)==='undefined') options={};
    if (typeof(options.params)==='undefined') options.params=[];
    if (typeof(options.autoOpen)==='undefined') options.autoOpen = true;
    if (typeof(options.showProgress)==='undefined') options.showProgress = true;
    if (typeof(options.runButtonTitle)==='undefined') options.runButtonTitle = resources.anButtonRun;
    if (typeof(options.allowCanceling)==='undefined') options.allowCanceling = true;
    var _this = {
      propertyPane : new JSPropertyInspector()
    };
	var beanPath = "properties/method/parameters/"+getElementName(analysis); 
    var parentID = "property_inspector_dialog_" + rnd();
    parentDiv.empty().append($("<div style='margin-bottom: 3px'/>").attr("id", parentID));
    var jobID;

    
    queryBioUML("web/bean/set", 
    {
    	de: beanPath,
    	json: $.toJSON(options.params)
    }, function(data)
	{
        function syncronizeData(control)
		{
            var json = convertDPSToJSON(_this.propertyPane.getModel(), control);
			disableDPI(parentDiv);
            queryBioUML("web/bean/set", 
            {
                de: beanPath,
                json: json
            }, function(data)
            {
            	enableDPI(parentDiv);
        	    $(getJQueryIdSelector(parentID)).empty();
        		var beanDPS = convertJSONToDPS(data.values);
                _this.propertyPane.removeChangeListener(syncronizeData);
        		_this.propertyPane = new JSPropertyInspector();
        		_this.propertyPane.setParentNodeId(parentID);
        		_this.propertyPane.setModel(beanDPS);
        		_this.propertyPane.generate();
        		_this.propertyPane.addChangeListener(syncronizeData);
            }, function(data)
			{
                console.log("Error updating in " + callID);
            	enableDPI(parentDiv);
				logger.error(data.message);
			});
		}
		
		var beanDPS = convertJSONToDPS(data.values);
		_this.propertyPane.setParentNodeId(parentID);
		_this.propertyPane.setModel(beanDPS);
		_this.propertyPane.generate();
		_this.propertyPane.addChangeListener(syncronizeData);
		var runButton = $("<input type='button' value='" + options.runButtonTitle + "'/>");
		var progressBar = $("<div style='float:right; width: 400px'/>");
		parentDiv.append(runButton);
		if(options.showProgress)
			parentDiv.append(progressBar);
		runButton.click(function() {
			if(runButton.val() == resources.anButtonStop)
			{
				if(jobID)
				{
		            cancelJob(jobID);
					runButton.val(options.runButtonTitle);
					jobID = undefined;
				}
			} else
			{
				if(options.allowCanceling)
					runButton.val(resources.anButtonStop);
				else
					runButton.prop("disabled", true);
		        jobID = rnd();
				_this.propertyPane.updateModel();
				var newParams = convertDPSToJSON(_this.propertyPane.getModel());
                newParams = $.toJSON(_.union(options.params, $.parseJSON(newParams)));
		        queryBioUML("web/analysis", 
		        {
		            de: analysis,
					json: newParams,
		            showMode : SHOW_USUAL,
					jobID: jobID
		        }, function(data)
		        {
		        	createProgressBar(progressBar, jobID, function(status, message, results) {
						if(options.allowCanceling)
							runButton.val(options.runButtonTitle);
						else
							runButton.prop("disabled", false);
		        		if (status == JobControl.COMPLETED)
		                {
                            if(options.autoOpen)
		                    	for(var i=0; i<results.length; i++)
		                    	{
		                    		reopenDocument(results[i]);
		                    	}
                            if(options.success)
                                options.success(results);
		                } else
		                	logger.error(message);
		        	});
		        });
			}
		});
	});
    return _this;
}

function createProgressBar(element, jobID, completeCallback, processCallback)
{
	element = $(element);
	var progressMeter, progressText;
	
	if (element.data("progressMeter") != undefined) 
	{
		progressMeter = element.data("progressMeter");
		progressText = element.data("progressText");
	    progressMeter.progressbar("value", 0);
	}
	else
	{
		progressMeter = $('<span/>').css({float: 'left', margin: '0pt 2pt'}).width(300);
		progressText = $('<span/>');
		element.data("progressMeter", progressMeter);
		element.data("progressText", progressText);
		element.append(progressMeter);
		element.append(progressText);
	    progressMeter.progressbar(
	    {
	        value: 0
	    });
	}
	progressText.text('0%');
    var percent = 0;
    var message = "";
    var timeout;
    var destroyed = false;
	var periodicalUpdater = function()
	{
		queryBioUML("web/jobcontrol", {jobID: jobID}, function(data)
		{
			if(destroyed) return;
			if(data.status != undefined)
			{
				if(data.status == JobControl.CREATED)
				{
					progressText.text("Waiting");
					progressMeter.progressbar("value", 0);
					if (message != data.values[0] && processCallback != undefined) 
                    {
                        processCallback(data.status, data.values[0], data.results);
                    }
					setTimeout(periodicalUpdater, 1000);
				}
				if(data.status == JobControl.RUNNING)
				{
					progressText.text(data.percent + "%");
					progressMeter.progressbar("value", data.percent);
					if ((percent != data.percent || message != data.values[0]) && processCallback != undefined) 
                    {
                        processCallback(data.status, data.values[0], data.results);
                        percent = data.percent;
                    }
					setTimeout(periodicalUpdater, 1000);
				}
				if(data.status == JobControl.COMPLETED)
				{
					progressMeter.progressbar("value", 100);
					progressText.text(resources.commonProgressComplete);
					completeCallback(data.status, data.values[0], data.results);
				}
				if(data.status == JobControl.TERMINATED_BY_REQUEST)
				{
					if(processCallback != undefined)
						processCallback(data.status, data.values[0], data.results);
					progressMeter.progressbar("value", 0);
					progressText.text(resources.commonProgressTerminatedByUser);
					completeCallback(data.status, data.values[0], data.results);
				}
				if(data.status == JobControl.TERMINATED_BY_ERROR)
				{
					if(processCallback != undefined)
						processCallback(data.status, data.values[0], data.results);
					progressMeter.progressbar("value", 0);
					progressText.text(resources.commonProgressTerminatedByError);
					completeCallback(data.status, data.values[0], data.results);
				}
			}
		}, function() {timeout = setTimeout(periodicalUpdater, 1000);});
	};
	timeout = setTimeout(periodicalUpdater, 1000);
	element.bind("destroy", function()
	{
		clearTimeout(timeout);
		destroyed = true;
	});
}

function showWaitDialog(title)
{
    removeWaitDialog();
    var dialog = $('<div/>').attr("title", resources.commonWait).append($('<h2/>').text(title));
    dialog.dialog( {
        autoOpen : true,
        width : 400,
        height : 100,
        modal : true,
    });
    showWaitDialog.dlg = dialog;
}

function removeWaitDialog()
{
    var waitDialog = showWaitDialog.dlg;
    if(waitDialog && waitDialog.hasClass('ui-dialog-content'))
    {
        waitDialog.dialog("close");
        waitDialog.remove();
    }
}

function showProgressDialog(jobID, title, successCallback)
{
	var dialog = $('<div/>').attr("title", title);
	var progress = $('<div/>');
	dialog.append(progress);
	createProgressBar(progress, jobID, function(status, message, results) {
		jobID = undefined;
		dialog.dialog("close");
		dialog.remove();
		if(status == JobControl.COMPLETED)
		{
			if(successCallback) successCallback(message, results);
		} else if(status == JobControl.TERMINATED_BY_ERROR)
		{
			logger.error(message);
		}
	});
	dialog.dialog( {
		autoOpen : true,
		width : 400,
		height : 120,
		modal : true,
		beforeClose: function()
		{
			if (jobID != undefined) 
			{
				cancelJob(jobID);
			}
			$(this).remove();
		},
		buttons : {
			"Cancel" : function() {
				if (jobID != undefined)
				{
					cancelJob(jobID);
				} else
				{
					$(this).dialog("close");
					$(this).remove();
				}
			}
		}
	});
}

function processEnvironment(jobID, callback)
{
    queryBioUML("web/script/environment", 
    {
		"jobID": jobID
    }, function(data)
    {
        for (var it = 0; it < data.tables.length; it++) 
        {
        	createTableDocument(data.tables[it], function(tableDoc) {
                tableDoc.setTitle("Result " + (it + 1));
                openDocumentTab(tableDoc);
        	});
        }
        for (var it = 0; it < data.images.length; it++) 
        {
        	showImage(data.images[it]);
        }
        if(callback) callback(data);
    }, function(data)
    {
		if (data.message != resources.commonErrorQueryException.replace(
				"{message}", "error"))
			logger.error(data.message);
		if(callback) callback(data);
	});
}

function addDialogKeys(dialogDiv, inputField, okName, cancelName)
{
	if(!okName) okName = "Ok";
	if(!cancelName) cancelName = "Cancel";
	if(!inputField) inputField = dialogDiv;
	var buttons = dialogDiv.dialog("option", "buttons");
	var okHandler = buttons[okName];
	var cancelHandler = buttons[cancelName];
    inputField.keydown(function(e)
    {
    	if (e.keyCode && !$(e.target).is(":button") && dialogDiv.parent().find(":button:contains("+okName+")").is(":enabled") && 
    			e.keyCode == 13 && (!$(e.target).is("textarea") || e.ctrlKey) && okHandler)
    	{
			okHandler.apply(dialogDiv);
    		return false;
    	}
    	if (e.keyCode && e.keyCode == 27 && cancelHandler)
    	{
			cancelHandler.apply(dialogDiv);
    		return false;
    	}
    });
}

// Necessary in jQueryUI 1.7.x as buttons are specified in hash, thus unsorted
function sortButtons(dialogDiv)
{
	var buttonsOrder = ["Ok", "Import", "(default)", "Save", "View", "Add", "Remove", "Yes", "No", "Close", "Cancel"];
	var buttonsPane = dialogDiv.parent().find(".ui-dialog-buttonpane");
	var buttons = buttonsPane.children().sort(function(a,b)
	{
		var posA = -1, posB = -1, posDefault;
		var textA = $(a).text();
		var textB = $(b).text();
		for(var i=0; i<buttonsOrder.length; i++)
		{
			if(buttonsOrder[i] == textA) posA = i;
			if(buttonsOrder[i] == textB) posB = i;
			if(buttonsOrder[i] == "(default)") posDefault = i;
		}
		if(posA == -1) posA = posDefault;
		if(posB == -1) posB = posDefault;
		return posA > posB ? -1 : posA < posB ? 1 : textA > textB ? -1 : textA < textB ? 1 : 0;
	});
	buttonsPane.append(buttons);
}

//TODO: move this to better place
function PlotDialog( path, prevSeriesPath )
{
    var _thisDialog = this;

    this.path = path;
    this.dialogDiv = $('<div title="'+resources.dlgPlotEditorTitle+'"></div>');
    this.prevSeriesPath = prevSeriesPath != undefined ? prevSeriesPath : null;
    
    this.loadTable = function()
    {
        this.table.html('<div>'+resources.dlgPlotEditorLoading+'</div>');
        var params = {
                "de": this.path,
                type: "plot",
                add_row_id: 1
            };
        queryBioUML("web/table/sceleton", params, 
        function(data)
        {
            _thisDialog.table.html(data.values);
            _thisDialog.tableObj = _thisDialog.table.children("table");
            _thisDialog.tableObj.addClass('selectable_table');
            params["rnd"] = rnd();
            params["read"] = false;
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "sDom": "frti",
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?" + toURI(params) 
            };
            _thisDialog.tableObj.dataTable(features);
            _thisDialog.tableObj.css('width', '100%');
        });
    };
    
    this.open = function(isNew, defaultPlotPath)
    {
        var defaultPath = "";
        if(!isNew)
            defaultPath = path;
        else if(defaultPlotPath != undefined)
            defaultPath = defaultPlotPath;
        var property = new DynamicProperty("plotPath", "data-element-path", defaultPath);
        property.getDescriptor().setDisplayName(resources.dlgPlotEditorPlotPath);
        property.getDescriptor().setReadOnly(false);
        property.setCanBeNull("no");
        property.setAttribute("dataElementType", "biouml.standard.simulation.plot.Plot");
        property.setAttribute("elementMustExist", false);
        property.setAttribute("promptOverwrite", true);
        
        var pathEditor = new JSDataElementPathEditor(property, null);
        pathEditor.setModel(property);
        var plotNode = pathEditor.createHTMLNode();
        $(plotNode).find('input').width(350);
        pathEditor.setValue(defaultPath);
        
        this.dialogDiv.append('<b>'+resources.dlgPlotEditorPlotPath+'</b>&nbsp;');
        this.dialogDiv.append(plotNode);
        
        this.table = $('<div>'+resources.dlgPlotEditorLoading+'</div>');
        this.dialogDiv.append(this.table);

        this.dialogDiv.dialog(
        {
            autoOpen: false,
            width: 800,
            buttons:
            {
                "Save" : function()
                {
                    var plotPath = pathEditor.getValue();
                    if(!getElementName(plotPath))
                    {
                        logger.error(resources.commonErrorEmptyNameProhibited);
                        return false;
                    }
                    var _this = $(this);
                    _thisDialog.savePlot(plotPath,  function(){
                        refreshTreeBranch(getElementPath(plotPath));
                        createPlotDocument( plotPath, function(plot) {
                            openDocumentTab(plot);
                        });
                        _this.dialog("close");
                        _this.remove();
                    });
                    return false;
                    
                },
                "View": function()
                {
                    _thisDialog.showPlot();
                    return false;
                },
                "Remove": function()
                {
                    _thisDialog.removeSeries();
                    return false;
                },
                "Add": function()
                {
                    _thisDialog.addSeries();
                    return false;
                },
                "Cancel": function()
                {
                    var _this = $(this);
                    _this.dialog("close");
                    _this.remove();
                    return false;
                }
            }
        });
        sortButtons(this.dialogDiv)
        if (isNew) 
        {
            var paramsUpdate = {
                "de": _thisDialog.path
            };
            
            queryBioUML("web/plot/new", paramsUpdate, function(data)
            {
                _thisDialog.loadTable();
                _thisDialog.dialogDiv.dialog("open");
            });
        }
        else
        {
            _thisDialog.loadTable();
            _thisDialog.dialogDiv.dialog("open");
        }    
    }
    
    this.addSeries = function()
    {
        var defaultPath = "data/";
        if(_thisDialog.prevSeriesPath)
            defaultPath = _thisDialog.prevSeriesPath;
        else
        {
            var usedSeriesPaths = getTableColumnValues(_thisDialog.tableObj, "Source");
            if(usedSeriesPaths.length > 0)
                defaultPath = usedSeriesPaths[usedSeriesPaths.length-1];
        }
        
        addPlotSeriesDialog(_thisDialog.path, defaultPath, function(data, source){
            if(source)
                _thisDialog.prevSeriesPath = source;
            _thisDialog.loadTable();
        });
    };
    
    this.removeSeries = function()
    {
        var indices = getTableSelectedRowIds(_thisDialog.tableObj);
        if(indices == null || indices.length == 0)
            return false;
        
        queryBioUML("web/plot/remove",
        {
            "de": _thisDialog.path,
            "series" : $.toJSON(indices)
        }, function(data)
        {
            _thisDialog.loadTable();
        });
    };

    this.showPlot = function()
    {
        _thisDialog.saveTable(function(){
            queryBioUML("web/plot/plot",
            {
                de: _thisDialog.path
            }, function(data)
            {
                _thisDialog.plotDiv = $('<div title="' + getElementName(_thisDialog.path) + '"><div id="graph"></div></div>');
                _thisDialog.plotDiv.children('#graph').html('<img src="'+appInfo.serverPath+'web/img?de=' + data.values.image + '&rnd=' + rnd() + '">');
                _thisDialog.plotDiv.dialog(
                {
                    autoOpen: true,
                    width: 600,
                    height: 500,
                    buttons: 
                    {
                        "Ok": function()
                        {
                            $(this).dialog("close");
                            $(this).remove();
                        }
                    }
                });
            });
        });
    };
    
    this.saveTable = function(callback)
    {
        if (this.tableObj) 
        {
            var dataParams = {
                rnd: rnd(),
                action: 'change',
                de: this.path,
                type: "plot"
            };
            saveChangedTable(this.tableObj, dataParams, callback);
        }
        else
        {
            callback();    
        }
    };
    
    this.savePlot = function (plotPath, callback)
    {
        var path = plotPath;
        this.saveTable( function(){
            queryBioUML("web/plot/save",
            {
                "de": _thisDialog.path,
                "plot_path" : path
            }, callback); 
        });
    };
}

function addPlotSeriesDialog(plotPath, sourcePath, addCallback)
{
    var seriesDialogDiv = $('<div title="'+resources.dlgPlotEditorAddSeriesTitle+'"></div>');
    var property = new DynamicProperty("seriesPath", "data-element-path", sourcePath);
    property.getDescriptor().setDisplayName(resources.dlgPlotEditorPlotPath);
    property.getDescriptor().setReadOnly(false);
    property.setCanBeNull("no");
    property.setAttribute("elementMustExist", true);
    property.setAttribute("promptOverwrite", false);
    var _this = this;
    
    var pathEditor = new JSDataElementPathEditor(property, null);
    pathEditor.setModel(property);
    var plotNode = pathEditor.createHTMLNode();
    pathEditor.setValue(sourcePath);
    pathEditor.addChangeListener (function(control, oldValue, newValue) {
        if (newValue == oldValue) 
        {
            //_this.fillVariablesCombos(_this.xValues, _this.yValues);
        }
        else 
        {
            _this.updateVariables(newValue);
        }
    });
    
    seriesDialogDiv.append('<b>'+resources.dlgPlotEditorAddSeriesTable+'</b>&nbsp;');
    seriesDialogDiv.append(plotNode);
    
    var comboPathX = $('<select></select>');
    var comboX = $('<select></select>');
    seriesDialogDiv.append('<br/><b>'+resources.dlgPlotEditorAddSeriesX+':</b>&nbsp;');
    seriesDialogDiv.append(comboPathX);
    seriesDialogDiv.append(comboX);
    
    var comboPathY = $('<select></select>');
    var comboY = $('<select></select>');
    seriesDialogDiv.append('<br/><b>'+resources.dlgPlotEditorAddSeriesY+':</b>&nbsp;');
    seriesDialogDiv.append(comboPathY);
    seriesDialogDiv.append(comboY);
    
    comboPathX.change(function(){
        _this.pathX = comboPathX.val();
        _this.loadVariables(pathEditor.getValue(), comboPathX.val(), function(data)
        {
            if (data != null) 
            {
                if (data.type == 0) 
                {
                    _this.xValues = data.values.x.sort();
                    comboX.empty();
                    _this.fillVariablesCombos(_this.xValues, null);
                }
                else 
                {
                    logger.error(data.message);
                }
            }
        });
    });
    
    comboPathY.change(function(){
        _this.pathY = comboPathY.val();
        _this.loadVariables(pathEditor.getValue(), comboPathY.val(), function(data)
        {
            if (data != null) 
            {
                if (data.type == 0) 
                {
                    _this.yValues = data.values.y.sort();
                    comboY.empty();
                    _this.fillVariablesCombos(null, _this.yValues);
                }
                else 
                {
                    logger.error(data.message);
                }
            }
        });
    });
    
    seriesDialogDiv.dialog(
    {
        autoOpen: false,
        width: 500,
        buttons:
        {
            "OK": function()
            {
                _this.addSeries(addCallback, plotPath);
                return false;
            },
            "Close": function()
            {
                $(this).dialog("close");
                $(this).remove();
            }
        }
    });
    
    
    seriesDialogDiv.dialog("open"); 
    
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
    
    this.fillVariablesCombos = function (valuesX, valuesY)
    {
        if (valuesX) 
            $.each(valuesX, function(index, value)
            {
                comboX.append($('<option></option>').val(value).text(value));
            });
        if (valuesY) 
            $.each(valuesY, function(index, value)
            {
                comboY.append($('<option></option>').val(value).text(value));
            });
    };
    
    this.updateVariables = function(newValue)
    {
        _this.loadPaths(pathEditor.getValue(), function(data)
        {
            if(data.values == "" || data.values.length == 1 && data.values[0]=="")
            {
                comboPathX.empty();
                comboPathX.hide();
                comboPathY.empty();
                comboPathY.hide();
            }
            else
            {
                comboPathX.empty();
                comboPathX.show();
                comboPathY.empty();
                comboPathY.show();
                _this.paths = data.values;
                $.each(data.values, function(index, value)
                {
                    comboPathX.append($('<option></option>').val(value).text(value));
                    comboPathY.append($('<option></option>').val(value).text(value));
                    comboPathX.val("");
                    comboPathY.val("");
                });
            }
            _this.loadVariables(newValue, "", function(data)
            {
                if (data != null) 
                {
                    comboX.empty();
                    comboY.empty();
                    if (data.type == 0) 
                    {
                        _this.xValues = data.values.x;
                        _this.yValues = data.values.y;
                        _this.fillVariablesCombos(_this.xValues, _this.yValues);
                        if(_this.xValues.includes("time"))
                            comboX.val("time");
                    }
                    else 
                    {
                        logger.error(data.message);
                    }
                }
            });
        });
    };
    
    this.addSeries = function(addCallback, plotPath)
    {
        var xPath = comboPathX.val() != null ? comboPathX.val() : "";
        var yPath = comboPathY.val() != null ? comboPathY.val() : "";
        var params = {
                de: plotPath,
                x : comboX.val(),
                y : comboY.val(),
                xPath : xPath,
                yPath : yPath,
                source : pathEditor.getValue()
            };
            queryBioUML("web/plot/add", params, 
                function(data)
                {
                    addCallback(data, pathEditor.getValue());
                },
                function(data)
                {
                    logger.error(data.message);
                });
            
    };
    
    if(sourcePath)
        _this.updateVariables(sourcePath);
}


var __textContainer;
/**
 * Returns width in pixels of given text string styled by given style
 * @param style - font style of string (may contain font-weight; font-size; font-family; font-variant values)
 * @param text - string to measure
 * @return width in pixels
 */
function measureTextLength(style, text)
{
	if(!__textContainer)
	{
		__textContainer = $('<div/>').appendTo('body');
		__textContainer.css('position','absolute').css('left', '-1000px').css('top', '-1000px').css('margin', '0px').css('padding', '0px');
	}
	__textContainer.css('font', style).text(text);
	return __textContainer.innerWidth();
}

/**
 * Fits text into specified width, replacing beginning of line with ellipsis if line is too long 
 * @param style - font style of string (may contain font-weight; font-size; font-family; font-variant values)
 * @param text - text to fit
 * @param wantedWidth - width in pixels of area to fit text to
 * @param trimRight - if true, then string will be trimmed from right (otherwise from left)
 */
function fitBox(style, text, wantedWidth, trimRight)
{
	var ellipsis = "...";
	var rWidth = measureTextLength(style, text);
	if(rWidth < wantedWidth) return text;
	var ellipsisWidth = measureTextLength(style, ellipsis);
	wantedWidth-=ellipsisWidth;
	var lWidth = 0;
	var lLength = 0;
	var rLength = text.length;
	while(rLength-lLength>1)
	{
		var newLength = Math.floor((rLength-lLength)*(wantedWidth-lWidth)/(rWidth-lWidth)+lLength+0.5);
		if(newLength == rLength) newLength--;
		if(newLength == lLength) newLength++;
		var newWidth = measureTextLength(style, trimRight?text.substring(0, newLength):text.substring(text.length-newLength));
		if(newWidth>wantedWidth)
		{
			rWidth = newWidth;
			rLength = newLength;
		} else
		{
			lWidth = newWidth;
			lLength = newLength;
		}
		if(lWidth == wantedWidth) break;
	}
	return trimRight?text.substring(0,lLength)+ellipsis:ellipsis+text.substring(text.length-lLength);
}

/**
 * Fit text into given HTML element
 * @param element jQuery object, DOM element or jQuery selector to fit text into
 * @param text text to fit
 * @param trimRight - if true, then string will be trimmed from right (otherwise from left)
 * @param width - if present overrides maximal width of the element
 */
function fitElement(element, text, trimRight, width)
{
	element = $(element).eq(0);
	if(width == undefined) width = element.width(); 
	if(element.is("input"))
		element.val(fitBox([element.css("font-style"),element.css("font-variant"),element.css("font-weight"),element.css("font-size"),element.css("font-family")].join(" "), text, width, trimRight));
	else
		element.text(fitBox([element.css("font-style"),element.css("font-variant"),element.css("font-weight"),element.css("font-size"),element.css("font-family")].join(" "), text, width, trimRight));
}

$.fn.extend({
	/**
	 * Insert text into cursor position of textarea
	 * @param myValue text to insert
	 */
    insertAtCaret: function(myValue){
    	var node = this.get(0);
   		if (document.selection)
   		{
			node.focus();
			sel = document.selection.createRange();
			sel.text = myValue;
			node.focus();
		}
   		else if (node.selectionStart || node.selectionStart == '0')
		{
			var startPos = node.selectionStart;
			var endPos = node.selectionEnd;
			var scrollTop = node.scrollTop;
			node.value = node.value.substring(0, startPos) + myValue + node.value.substring(endPos, node.value.length);
			node.focus();
			node.selectionStart = startPos + myValue.length;
			node.selectionEnd = startPos + myValue.length;
			node.scrollTop = scrollTop;
		}
   		else
		{
			node.value += myValue;
			node.focus();
		}
    }
});

function updateLog(element, message)
{
	element = $(element);
	if(element.data("message") == message) return;
	element.data("message", message);
	message = message.escapeHTML().replace("&quot;", "\"").replace(/^(WARN .+|WARNING .+|Reason\:.+)$/gm, "<span class='log_warning'>$1</span>")
	    .replace(/^(ERROR .+|SEVERE.+)$/gm, "<span class='log_error'>$1</span>").replace(/\n/g, "<br>");
	element.html(message);
	element.scrollTop(element.get(0).scrollHeight);
}

function makeEditable(text, callback)
{
	$(text).click(function(event)
	{
		var ctl = $(this);
		if(ctl.children("input").length) return;
		var text = ctl.text();
		var blurFunction = function() {
			var val = $(this).val();
			$(this).remove();
			ctl.text(val);
			if (callback)
				callback(val);
		};
		var textField = $("<input type=\"text\">").val(text).blur(blurFunction).keydown(function(event) {
			if(event.keyCode == 13) blurFunction.apply(this);
		}).click(function(event) {
			event.stopImmediatePropagation();
		});
		ctl.empty().append(textField);
		textField.focus().select();
		event.stopImmediatePropagation();
	});
};

function formatSize(size)
{
    var suffixes = "kMGTPE";
    var normalizedSize = size;
    var i;
    for(i=0; normalizedSize>=1024 && i<suffixes.length; i++, normalizedSize/=1024);
    return i == 0 
            ? size == 1 ? "1 byte" : size+" bytes" 
            : normalizedSize.toFixed(1)+suffixes.substring(i-1,i)+"b ("+size+" bytes)";
};

function makeUniqueName(baseName, existingNames)
{
    var name = baseName;
    var i=0;
    var nameExist = function(n)
    {
        if(Array.isArray(existingNames))
            return $.inArray(n, existingNames) !== -1;
        else
            return (n in existingNames);
    };
    
    while(nameExist(name))
    {
        name = baseName + "_" + ++i;
    }
    return name;
}

/**
 * Create div with one line "<b>text</b>: value"
 * Should be used in custom dialogs without property inspector for uniform display of elements
 * @param value should be jquery object editor (selector, color editor, path editor)
 */
function createSinglePropertyControl(text, value)
{
    var controlDiv = $('<div class="singlePropertyDiv"></div>');
    var nameDiv = $('<div class="singlePropertyName"></div>').text(text + ":");
    controlDiv.append(nameDiv);
    value.addClass("singlePropertyValue");
    controlDiv.append(value);
    return controlDiv;
}


/*
 * Check data element name
 * Other characters may cause problems with access and indexing
 */
function isDataElementNameValid(name)
{
    return /^[\x20-\x2E\x30-\x7F]+$/.test(name);
}