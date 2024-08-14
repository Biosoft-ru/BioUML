/**
 * Table document realization
 * 
 * @author tolstyh
 */
function Table(completeName)
{
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    if(this.name=="summary" && instanceOf(getDataCollection(getElementPath(this.completeName)).getClass(),"ru.biosoft.bsa.analysis.SiteSearchResult"))
    {
    	this.name = getElementName(getElementPath(this.completeName));
    }
    this.tabId = allocateDocumentId(completeName);
    
    this.openAsTrackAction = _.find(treeActions, function(action) {return action.id==="open_genome_browser";});
    
    var _this = this;
    var dc = getDataCollection(this.completeName);
    this.readOnly = !dc.isMutable();
    dc.addChangeListener(this);
    dc.addRemoveListener(this);

    this.tableResolver = "";
    this.loadListener = null;
    this.additionalParams = {};
    if(instanceOf(dc.getClass(), "ru.biosoft.bsa.Track"))
    	this.additionalParams = {"type":"track"};
    
    this.activeView = -1;
    
    this.setTitle = function(title)
    {
        this.name = title;
    };
    
    this.setAdditionalParams = function(additionalParams)
    {
        this.additionalParams = additionalParams;
    };
    
    // Code to be called when table is loaded
    this.setLoadedListener = function(listener)
    {
        this.loadedListener = listener;
    };

    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var tableDocument = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(tableDocument);
		
        this.tableContainerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        tableDocument.append(this.tableContainerDiv);
        
        this.tableControlButtons = $('<div id="' + this.tabId + '_actions" class="table_actions"></div>');
        this.initActions();
        this.tableContainerDiv.append(this.tableControlButtons);
        
        this.tableFilter = $('<div class="table_filter_info"/>');
        this.tableContainerDiv.append(this.tableFilter);
        
        this.tableDataDiv = $('<div id="' + this.tabId + '_data" class="documentData">'+resources.commonLoading+'</div>');
        this.tableContainerDiv.append(this.tableDataDiv);
        
        this.loadTable(true);
        
        createTreeItemDroppable(this.tableContainerDiv, "ru.biosoft.table.columnbeans.Descriptor", function(path,event) 
		{
        	_this.addDescriptor(path);
        });        
        
        //update document size
        resizeDocumentsTabs();
        selectViewPart('common.description');
    };
    
    this.canOpenAsTrack = function()
    {
    	return this.openAsTrackAction && this.openAsTrackAction.isVisible(this.completeName) === true;
    };
    
    this.openAsTrack = function()
    {
    	if(!this.canOpenAsTrack()) return;
    	this.openAsTrackAction.doAction(this.completeName);
    };    
    
    this.addDescriptor = function(path)
    {
    	var params = {
    			action : "columns",
    			colaction : "addDescriptor",
    			de : this.completeName,
    			descriptor : path
    	};
    	_.extend(params, _this.additionalParams);
    	queryBioUML("web/table", params, function()
		{
			_this.refresh();
		});
    };

    /*
     * Export table into
     */
    this.exportElement = function(value)
    {
		var _this = this;
	    $.chainclude(
		    {
		        'lib/export.js':function(){
					exportElement(_this.completeName, "Table");
		        }
		    }
	    );
    };
    
    this.getSelection = function()
    {
    	return getTableSelectedRows(this.getTable(), true);
    };
    
    this.selectPage = function()
    {
    	this.getTable().children('tbody').children('tr').addClass('row_selected');
    };

    this.initActions = function()
    {
        this.editButton = $('<input type="button" value="'+resources.tblButtonEdit+'"/>');
        this.applyButton = $('<input type="button" value="'+resources.tblButtonApplyEdit+'"/>').attr("disabled", true);
        this.cancelButton = $('<input type="button" value="'+resources.tblButtonCancelEdit+'"/>').attr("disabled", true);
        
        var _this = this;
        
        var updateClasses = function()
        {
        	_this.tableControlButtons.children().removeClass("ui-state-disabled").addClass("ui-state-default");
        	_this.tableControlButtons.children("[disabled]").addClass("ui-state-disabled");
        };
        
        this.editButton.click(function()
        {
            _this.editButton.attr("disabled", true);
            _this.applyButton.attr("disabled", false);
            _this.cancelButton.attr("disabled", false);
            updateClasses();
            _this.loadTable(false);
        });
        this.applyButton.click(function()
        {
            _this.editButton.attr("disabled", false);
            _this.applyButton.attr("disabled", true);
            _this.cancelButton.attr("disabled", true);
            updateClasses();
            _this.changeTable();
        });
        this.cancelButton.click(function()
        {
            _this.editButton.attr("disabled", false);
            _this.applyButton.attr("disabled", true);
            _this.cancelButton.attr("disabled", true);
            updateClasses();
            _this.loadTable(true);
        });
        
        this.selectAllButton = $('<input type="button" value="'+resources.tblButtonSelectAll+'"/>').addClass("ui-state-default");
        this.selectAllButton.click(function()
        {
            selectAll(_this.table);
        });

        this.selectPageButton = $('<input type="button" value="'+resources.tblButtonSelectPage+'"/>').addClass("ui-state-default");
        this.selectPageButton.click(function()
        {
        	_this.selectPage();
        });
        
        this.changeViewButton = $('<input type="button" value="'+resources.tblButtonChangeView+'"/>').addClass("ui-state-default");
        this.changeViewButton.click(function()
        {
        	_this.activeView++;
        	_this.changeView();
        });
        
        if(!this.readOnly)
        {
        	this.tableControlButtons.append(this.editButton);
        	this.tableControlButtons.append(this.applyButton);
        	this.tableControlButtons.append(this.cancelButton);
        }
        this.tableControlButtons.append(this.selectAllButton);
        this.tableControlButtons.append(this.selectPageButton);
        updateClasses();
    };
    
    this.loadTable = function(isReadOnly, tableFeatures)
    {
        var _this = this;
        var params = {de: _this.completeName};
        _.extend(params, _this.additionalParams);
        queryBioUML("web/table/sortOrder", params, 
            function(data){
                var sortOrder = undefined;
                if(data.values.direction != undefined && data.values.columnNumber != undefined)
                    sortOrder = data.values;
                _this.loadTableData(isReadOnly, tableFeatures, sortOrder);    
            }, 
            function(){
                _this.loadTableData(isReadOnly, tableFeatures);
            });
    };
    
    
    this.loadTableData = function(isReadOnly, tableFeatures, sortOrder)
    {
        var _this = this;
        var sceletonParams = 
        {
            de: _this.completeName
        };
        _.extend(sceletonParams, _this.additionalParams);
        delete this.columnList;
        queryBioUML("web/table/sceleton", sceletonParams, function(data)
        {
            _this.tableDataDiv.html(data.values);
            _this.table = _this.tableDataDiv.children("table");
            _this.table.attr("data-path", _this.completeName);
            _this.table.addClass('selectable_table');
            if(!_this.table.hasClass('editable_table'))
            {
                _this.editButton.hide();
                _this.applyButton.hide();
                _this.cancelButton.hide();
            }
            var params = 
            {
                de: _this.completeName,
                read : isReadOnly,
                rnd: rnd()
            };
            if(_this.rowFilter)
            {
                params['filter'] = _this.rowFilter;
            }
            if(!isReadOnly || !instanceOf(dc.getClass(), "ru.biosoft.table.TableDataCollection"))
            	params['add_row_id'] = 1;
            _.extend(params, _this.additionalParams);
            var features = 
            {
                "bProcessing": true,
                "bServerSide": true,
                "bFilter": false,
                "bAutoWidth": false,
                "lengthMenu": [[30, 50, 100, 200, 500, 1000, -2], [30, 50, 100, 200, 500, "1000", "Custom..."]],
                "pageLength": 50,
                "lengthChange": true,
                "bSort": _this.table.hasClass("sortable_table"),
                "sPaginationType": "input",
                "sDom": "pfilrt",
                "sServerMethod": "POST",
                "fnRowCallback": function( nRow, aData, iDisplayIndex ) {
                    if(_this.table.hasClass("all_rows_selected"))
                        $(nRow).addClass('row_selected');
                    return nRow;
                },
                "fnDrawCallback": function() {
                // Clear list of inner genome browsers if any
                	if(_this.innerGenomeBrowser) _this.innerGenomeBrowser = [];
                // JSON response may include JavaScript, which does not get executed when inserted as innerHTML to table cell
                // So, call eval() manually
                    _this.table.find('.table_script_node').each(function() {
                        eval($(this).text());
                    }).remove();
                
                // JSON response may include some different view for one cell, we should display only view for current settings
                    if((_this.activeView == -1) && (_this.table.find('.alternativeView').length > 0)){
                    	_this.activeView = 0;
                    	_this.tableControlButtons.append(_this.changeViewButton);
                    }
                    _this.changeView();
                    updateViewParts();
                },
                "sAjaxSource": appInfo.serverPath+"web/table/datatables?" + toURI(params)
            };
            if(sortOrder)
            {
                var aaSorting = new Array();
                aaSorting.push(sortOrder.columnNumber);
                aaSorting.push(sortOrder.direction);
                features['aaSorting'] = [aaSorting];
            }
            if(tableFeatures)
            {
                for(var key in tableFeatures)
                    features[key] = tableFeatures[key];
            }
            
            var dataTable = _this.table.dataTable(features);
            addHideColumnsFunctions(_this.table, dataTable);
            if(_this.loadedListener)
                 _this.loadedListener();
            updateViewParts();
        });
    };
    
    this.changeTable = function()
    {
        
        var _this = this;
        var d = getDPSFromTable(this.table);
        var js = convertDPSToJSON(d);
        var changeTableParams = {
                data: js,
                de: this.completeName
                };
        var rowIds = getTableRowIds(this.table);
        if(rowIds && rowIds.length>0)
            changeTableParams["rowids"] = $.toJSON(rowIds);
        var tableSettings = getTableObject(this.table);
        if(tableSettings.aaSorting[0])
        {
            $.extend( changeTableParams,
            {
              iSortCol_0: tableSettings.aaSorting[0][0],
              sSortDir_0: tableSettings.aaSorting[0][1],
            });
        }
        queryBioUML("web/table/change", changeTableParams, 
        function(data)
        {
            if(data && data.type==0)
            {
                _this.loadTable(true);    
            }
            else
            {
                logger.error(data.message);
                _this.loadTable(true);
            }
            
        }, function(data)
        {
            logger.error(data.message);
            _this.loadTable(true);
        });
    };
    
    this.getColumnList = function(callback)
    {
        var _this = this;
        
        if(!this.columnList)
        {
        	var params = {de: _this.completeName};
            _.extend(params, _this.additionalParams);
            queryBioUML("web/table/columns", params, function(data)
            {
                _this.columnList = _.pluck(data.values, "jsName");
                callback(_this.columnList);
            });
        }
        else
        {
            callback(_this.columnList);
        }
    };
    
    this.getFilter = function()
    {
        return this.rowFilter;
    };
    
    this.setFilter = function(value)
    {
    	if(value == "" || value == null)
		{
    		this.clearFilter();
    		return;
		}
    	var _this = this;
    	var params = {de: _this.completeName,
    				filter: value};
        _.extend(params, _this.additionalParams);
    	queryBioUML("web/table/checkFilter", params, function()
    	{
            _this.rowFilter = value;
            _this.tableFilter.text(resources.tblFilterMessage.replace("{filter}", value));
            _this.loadTable(true);
    	});
    };
    
    this.clearFilter = function()
    {
        this.rowFilter = "";
        this.tableFilter.text("");
        this.loadTable(true);
    };
    
	// TODO: check if necessary (remove, if not)
    this.exportTable = function(path)
    {
        var _this = this;
        var jobID = rnd();
        var params = {
            de: this.completeName,
            exportTablePath: path,
            jobID: jobID
        };
        if(_this.rowFilter)
        {
            params['filter'] = _this.rowFilter;
        }
        queryBioUML("web/table/export", params, 
        function(data)
        {
            showProgressDialog(jobID, data.values, function() {
                refreshTreeBranch(getElementPath(path));
                performTreeAction(path, "open_table");
            });
        });
    };
    
    this.getTable = function()
    {
        return this.table;
    };
    
    this.dataCollectionRemoved = function()
    {
        closeDocument(this.tabId);
    };
    
    this.close = function(callback)
    {
        getDataCollection(this.completeName).removeChangeListener(this);
        getDataCollection(this.completeName).removeRemoveListener(this);
        if(callback) callback();
    };
    
    this.isChanged = function()
    {
    	return false;
    };
    
    this.saveAs = function(newPath, callback)
    {
        var _this = this;
        queryBioUML("web/doc/save", { de: _this.completeName, newPath: newPath }, function(data)
        {
        	if(callback) callback(data);
        }, function(data)
        {
        	if(callback) callback(data);
        });
    };
    
    this.refresh = function()
    {
        this.loadTable(true);
    };
    
    this.activate = function()
    {
        selectViewPart('common.description');
    };
    
    this.changeView = function()
    {
    	if(_this.activeView > -1){
    		_this.table.find('.alternativeView').each(function() {
            	var pos=0;
            	var modValue = $(this).children('div').length;
        		$(this).children('div').each(function() {
            		if(pos == (_this.activeView % modValue))
            			$(this).css('display', 'block');
            		else
            			$(this).css('display', 'none');
            		pos++;
        		});
    		});
    	}
    };
}
