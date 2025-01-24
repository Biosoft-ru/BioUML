/**
 * Table management functions
 * @param oTable - html table object, created as jquery.dataTable
 */
function getTableDisplayParameters (oTable)
{
    var params = {};
    var oSettings = oTable.fnSettings();
    var iCols = oSettings.aaSorting.length;
    if(iCols > 0)
    {
        params["iSortingCols"] = iCols;
        for ( i=0 ; i<iCols ; i++ )
        {
            params["iSortCol_"+i] = oSettings.aaSorting[i][0];
            params["sSortDir_"+i] = oSettings.aaSorting[i][1];
        }
    }
    params["iDisplayStart"] = oSettings._iDisplayStart;
    params["iDisplayLength"] = oSettings._iDisplayLength;
    return params;
}

function getTableSelectedRows(oTable, checkClass)
{ 
    var rows = [];
    var cnt = 0;
    if(checkClass && oTable.hasClass('all_rows_selected'))
    {
        rows[0] = "all";
        return rows;
    }
    oTable.children('tbody').children('tr').each(function(){
        if($(this).hasClass('row_selected'))
        {
            var pos = oTable.fnGetPosition(this);
            var id = $(oTable.fnGetData(pos)[0]).text();
            if(id == "")
                id = $(oTable.fnGetData(pos)[0]).val();
            rows[cnt++] = id; 
        }
    });
    return rows;
}

function setTableSelectedRows(oTable, idsToSelect)
{ 
    var rows = [];
    oTable.children('tbody').children('tr').each(function(){
        var pos = oTable.fnGetPosition(this);
        var id = $(oTable.fnGetData(pos)[0]).text();
        if(id == "")
            id = $(oTable.fnGetData(pos)[0]).val();
        if($.inArray(id, idsToSelect) != -1)
            $(this).addClass('row_selected');
    });
    return rows;
}

function setTableSelectedRowIds(oTable, idsToSelect)
{ 
    var rows = [];
    oTable.children('tbody').children('tr').each(function(){
        var pos = oTable.fnGetPosition(this);
        var id = $(oTable.fnGetData(pos)[0]).attr('data-id');
        if($.inArray(id, idsToSelect) != -1)
            $(this).addClass('row_selected');
    });
    return rows;
}



function getTableSelectedRowIds(oTable)
{ 
    var rows = [];
    var cnt = 0;
    oTable.children('tbody').children('tr').each(function(){
        if($(this).hasClass('row_selected'))
        {
            var pos = oTable.fnGetPosition(this);
            var id = $(oTable.fnGetData(pos)[0]).attr('data-id');
            rows[cnt++] = id; 
        }
    });
    return rows;
}


function getSelectedRowNumbers(oTable)
{ 
    var rows = [];
    var cnt = 0;
    oTable.children('tbody').children('tr').each(function(){
        if($(this).hasClass('row_selected'))
        {
            var pos = oTable.fnGetPosition(this);
            var id = $(oTable.fnGetData(pos)[0]).attr('id');
            rows[cnt++] = id.split(':')[0]; 
        }
    });
    return rows;
}

function setRowSelectionListeners(oTable)
{
    oTable.addClass('selectable_table');
}

function clearSelection(oTable)
{
    oTable.children('tbody').children('tr').each(function()
        {
            $(this).removeClass('row_selected');
        });
    oTable.removeClass('all_rows_selected');
}

function selectAll(oTable)
{
    if (oTable.hasClass('all_rows_selected')) 
    {
        clearSelection(oTable);
    }
    else 
    {
        oTable.children('tbody').children('tr').each(function()
        {
            $(this).addClass('row_selected');
        });
        oTable.addClass('all_rows_selected');
    } 
}

/**
 * Find column index by name
 */
        
function getColumnIndexByName(oTable, columnName)
{
    var index = -1;
    var th = $("th", oTable);
    th.each(function(i)
    {
        if($(this).text() == columnName)
        {
            index = i;
        }
    });
    return index;
}


function getTableColumnValues(oTable, colName)
{
    var values = [];
    var cnt = 0;
    var index = getColumnIndexByName(oTable, colName);
    if(index != -1)
    {
        oTable.children('tbody').children('tr').each(function(){
            var pos = oTable.fnGetPosition(this);
            var val = $(oTable.fnGetData(pos)[index]).text();
            if(val == "")
                val = $(oTable.fnGetData(pos)[index]).val();
            values[cnt++] = val; 
        });
    }
    return values;
}

function getTableSelectedRowsValues(oTable, colName)
{ 
    var rows = [];
    var cnt = 0;
    if(oTable.hasClass('all_rows_selected'))
    {
        return rows;
    }
    var th = $("th", oTable);
    var index = getColumnIndexByName(oTable, colName);
    if(index != -1)
    {
        oTable.children('tbody').children('tr').each(function(){
            if($(this).hasClass('row_selected'))
            {
                var pos = oTable.fnGetPosition(this);
                var id = $(oTable.fnGetData(pos)[0]).text();
                if(id == "")
                    id = $(oTable.fnGetData(pos)[0]).val();
                var val = $(oTable.fnGetData(pos)[index]).text();
                if(val == "")
                    val = $(oTable.fnGetData(pos)[index]).val();
                rows[cnt++] = {"name":id, "value":val}; 
            }
        });
    }
    return rows;
}

/*
 * DataTables plug-in for pagination
 */
$.fn.dataTableExt.oPagination.input = 
{
    "fnInit": function(oSettings, nPaging, fnCallbackDraw)
    {
        var nFirst = document.createElement('span');
        var nPrevious = document.createElement('span');
        var nNext = document.createElement('span');
        var nLast = document.createElement('span');
        var nInput = document.createElement('input');
        var nPage = document.createElement('span');
        var nOf = document.createElement('span');
        
        nFirst.innerHTML = oSettings.oLanguage.oPaginate.sFirst;
        nPrevious.innerHTML = oSettings.oLanguage.oPaginate.sPrevious;
        nNext.innerHTML = oSettings.oLanguage.oPaginate.sNext;
        nLast.innerHTML = oSettings.oLanguage.oPaginate.sLast;
        
        nFirst.className = "paginate_button first";
        nPrevious.className = "paginate_button previous";
        nNext.className = "paginate_button next";
        nLast.className = "paginate_button last";
        nOf.className = "paginate_of";
        nPage.className = "paginate_page";
        
        if (oSettings.sTableId !== '') 
        {
            nPaging.setAttribute('id', oSettings.sTableId + '_paginate');
            nPrevious.setAttribute('id', oSettings.sTableId + '_previous');
            nPrevious.setAttribute('id', oSettings.sTableId + '_previous');
            nNext.setAttribute('id', oSettings.sTableId + '_next');
            nLast.setAttribute('id', oSettings.sTableId + '_last');
        }
        
        nInput.type = "text";
        nInput.style.width = "25px";
        nInput.style.display = "inline";
        nPage.innerHTML = "Page ";
        
        nPaging.appendChild(nFirst);
        nPaging.appendChild(nPrevious);
        nPaging.appendChild(nPage);
        nPaging.appendChild(nInput);
        nPaging.appendChild(nOf);
        nPaging.appendChild(nNext);
        nPaging.appendChild(nLast);
        
        $(nFirst).click(function()
        {
            oSettings._iDisplayStart = 0;
            fnCallbackDraw(oSettings);
        });
        
        $(nPrevious).click(function()
        {
            oSettings._iDisplayStart -= oSettings._iDisplayLength;
            
            /* Correct for underrun */
            if (oSettings._iDisplayStart < 0) 
            {
                oSettings._iDisplayStart = 0;
            }
            
            fnCallbackDraw(oSettings);
        });
        
        $(nNext).click(function()
        {
            /* Make sure we are not over running the display array */
            if (oSettings._iDisplayStart + oSettings._iDisplayLength < oSettings.fnRecordsDisplay()) 
            {
                oSettings._iDisplayStart += oSettings._iDisplayLength;
            }
            
            fnCallbackDraw(oSettings);
        });
        
        $(nLast).click(function()
        {
            var iPages = parseInt((oSettings.fnRecordsDisplay() - 1) / oSettings._iDisplayLength, 10) + 1;
            oSettings._iDisplayStart = (iPages - 1) * oSettings._iDisplayLength;
            
            fnCallbackDraw(oSettings);
        });
        
        $(nInput).keyup(function(e)
        {
        
            if (e.which == 38 || e.which == 39) 
            {
                this.value++;
            }
            else 
                if ((e.which == 37 || e.which == 40) && this.value > 1) 
                {
                    this.value--;
                }
            
            if (this.value == "" || this.value.match(/[^0-9]/)) 
            {
                /* Nothing entered or non-numeric character */
                return;
            }
            
            var iNewStart = oSettings._iDisplayLength * (this.value - 1);
            if (iNewStart > oSettings.fnRecordsDisplay()) 
            {
                /* Display overrun */
                oSettings._iDisplayStart = (Math.ceil((oSettings.fnRecordsDisplay() - 1) /
                oSettings._iDisplayLength) -
                1) *
                oSettings._iDisplayLength;
                fnCallbackDraw(oSettings);
                return;
            }
            
            oSettings._iDisplayStart = iNewStart;
            fnCallbackDraw(oSettings);
        });
        
        /* Take the brutal approach to canceling text selection */
        $('span', nPaging).bind('mousedown', function()
        {
            return false;
        });
        $('span', nPaging).bind('selectstart', function()
        {
            return false;
        });
        
        oSettings.nPagingOf = nOf;
        oSettings.nPagingInput = nInput;
    },
    
    "fnUpdate": function(oSettings, fnCallbackDraw)
    {
        if (!oSettings.aanFeatures.p) 
        {
            return;
        }
        
        var iPages = Math.ceil((oSettings.fnRecordsDisplay()) / oSettings._iDisplayLength);
        var iCurrentPage = Math.ceil(oSettings._iDisplayStart / oSettings._iDisplayLength) + 1;
        
        oSettings.nPagingOf.innerHTML = " of " + iPages
        oSettings.nPagingInput.value = iCurrentPage;
    }
}

function getTableRowIds(oTable)
{
    var rows = [];
    oTable.fnGetData().forEach(function(rowdata){
        var cell = $(rowdata[0]);
        var cellId = cell.attr('id');
        var rowId = cell.attr('data-id');
        if(cellId && rowId)
        {
            var idarr = [];
            idarr.push(cellId);
            idarr.push(rowId);
            rows.push(idarr);
        }
    });
    return rows;
}

/**
 * Save changed table data
 * Add table content and required parameters and send query to the server
 * @param oTable - table object
 * @param parameters - parameters required to get table on the server side (de, type, resolver)
 * @param callback
 * @param failureCallback
 * @param addRowIds if true, server will check client/server consistency and return error if table was changed from another source
 */
function saveChangedTable(oTable, parameters, callback, failureCallback, addRowIds)
{
    var dataDPS = getDPSFromTable(oTable);
    parameters["data"] = convertDPSToJSON(dataDPS);
    if(addRowIds)
    {
        var rowIds = getTableRowIds(oTable);
        parameters["rowids"]= $.toJSON(rowIds);
    }
    var tableparams = getTableDisplayParameters(oTable);
    for(par in tableparams)
    {
        parameters[par] = tableparams[par];   
    }
    
    queryBioUML("web/table/change", parameters, 
	    function(data)
	    {
	    	if(callback)
	            callback(data.values);
	    }, 
	    failureCallback);
}

/**
 * Redraw table content
 */
function redrawTable(oTable)
{
    if(oTable != undefined)
    {
        oTable.fnClearTable( 0 );
        oTable.fnDraw();
    }
}


function initTableSelector()
{
    var lastSelected;
    //$(".selectable_table tbody").on("mousedown", "tr", function(e)
    $(document).on("mousedown", ".selectable_table tbody tr", function(e)
    {
        var tagName = e.target.tagName.toLowerCase();
        if(tagName == "input" || tagName == "option") return;
    	if(e.ctrlKey || e.metaKey || e.shiftKey)
    		e.preventDefault();
    });
    $(document).on("click", ".selectable_table tbody tr", function(e) {
        if(e.target.tagName == "INPUT") return;
        var tables = $(e.target).parents('table');
        var table = $(e.target).closest(".selectable_table");
        var items = tables.find("tbody tr");
        function selectSingleRow(that)
        {
    		items.removeClass("row_selected");
        	$(that).addClass("row_selected");
    		lastSelected = items.index($(that));
        }
        tables.removeClass('all_rows_selected');
        if (table.hasClass("single_row_selected"))
        {
        	selectSingleRow(this);
        }
        else if ((e.ctrlKey || e.MetaKey) && !e.shiftKey) 
		{
        	$(this).toggleClass("row_selected");
			lastSelected = items.index($(this));
		}
		else if(e.shiftKey)
		{
			if(!e.ctrlKey && !e.metaKey)
				items.removeClass("row_selected");
			var curSelected = items.index($(this));
			if(items.eq(lastSelected).hasClass("row_selected") || (!e.ctrlKey && !e.metaKey))
			{
				items.slice(Math.min(curSelected, lastSelected), Math.max(curSelected, lastSelected)+1).addClass("row_selected");
			} else
			{
				items.slice(Math.min(curSelected, lastSelected), Math.max(curSelected, lastSelected)+1).removeClass("row_selected");
			}
		} else
		{
            selectSingleRow(this);
			var path = table.attr("data-path");
			if(path !== undefined)
			{
				var id = $(this).children("td:first").children("p:first").attr("data-id");
				if(id !== undefined)
				{
					showElementInfo(createPath(path, id));
				}
			} else
			{
				var path = $(this).attr("data-path");
				if(path !== undefined)
				{
					showElementInfo(path);
				}
			}
		}
		table.trigger("change");
	});
}

function addHideColumnsFunctions(table, dataTable)
{
	var bindings = {
			"column-hide": function(target)
			{
				dataTable.fnSetColumnVis($(target).data("column-id"), false);
			}
	};
	var th = $("th", table);
	th.each(function(i)
	{
		$(this).data("column-id", i);
	});
	if($("#tableColumnsMenu").length == 0)
	{
		$(document.body).append($("<div id='tableColumnsMenu' class='contextMenu'><ul></ul></div>"));
	}
	th.contextMenu("tableColumnsMenu", 
	{
		menuStyle:
		{
			width: "300px"
		},
		onContextMenu: function()
		{
			var menu = $("#tableColumnsMenu").children("ul");
			menu.empty();
			menu.append("<li id='column-hide'>"+resources.menuHideThisColumn+"</li>");
			var cols = dataTable.fnSettings().aoColumns;
			for(var i in cols)
			{
				if(!cols[i].bVisible)
				{
					var id = "column-show-"+i;
					bindings[id] = _.bind(function()
					{
						dataTable.fnSetColumnVis(this, true);
					}, i);
					menu.append($("<li/>").attr("id", id).text(resources.menuShowColumn.replace("{name}", th.eq(i).text())));
				}
			}
			return true;
		},
		bindings: bindings
	});
}

function getTableObject(tableNode)
{
	tableNode = $(tableNode).get(0);
	return _.find($.fn.dataTableSettings, function(t)
	{
		return t.nTable == tableNode;
	});
}

function changeColorFromTable(colorBtn)
{
    var dialogDiv = $("<div title='Select color'/>");
            var pickerDiv = $("<div/>");
            var color = null;
            var colored = $(colorBtn);
            if(colored)
            {
                var colorValue = colored.css('backgroundColor'); //"rgb(r,g,b)"
                var rgb = colorValue.substring(4, colorValue.length-1).replace(/[^\d,.]/g, '').split(',');
                color = {active: new $.jPicker.Color({r:rgb[0], g:rgb[1], b:rgb[2]})};
            } else
                color = {active: new $.jPicker.Color()};
            dialogDiv.append(pickerDiv);
            pickerDiv.jPicker({
                color : color
            },
                function(color)
                {
                    var val = color.val("hex") === null?"":"["+color.val('r')+","+color.val('g')+","+color.val('b')+"]";
                    if(colored)
                        colored.css({'backgroundColor': "rgb("+color.val('r')+","+color.val('g')+","+color.val('b') +")"});
                    dialogDiv.dialog("close");
                    dialogDiv.remove();
                }, 
                function(color)
                {
                    
                },
                function(color)
                {
                    dialogDiv.dialog("close");
                    dialogDiv.remove();
                }
            );
            dialogDiv.dialog({
                autoOpen: true,
                modal: true,
                width: 580,
                height: 380
            });
}

function displayTableCell(cell)
{
	cell = $(cell);
	var cellId = cell.closest("td").children(".cellControl").attr("id");
	var tableSettings = getTableObject(cell.closest("table.display"));
	if(tableSettings === undefined) return;
	var prefix = appInfo.serverPath+"web/table/datatables";
	if(tableSettings.sAjaxSource.substring(0, prefix.length) !== prefix) return;
	queryBioUML("web/table/cell"+tableSettings.sAjaxSource.substring(prefix.length), 
	{
		iSortCol_0: tableSettings.aaSorting[0][0],
		sSortDir_0: tableSettings.aaSorting[0][1],
		cellId: cellId
	}, function(data)
	{
		var message = data.values;
		if(message.indexOf("<br") < 0)
			message = message.replace(/\n/g, "<br>");
		var dialogDiv = $("<div/>").attr("title", resources.dlgTableValueTitle).css({overflow: "auto"}).html(message);
		dialogDiv.find("a").attr("target", "_blank");
        dialogDiv.dialog(
        {
            autoOpen: true,
            width: 620,
            height: 500,
			modal: false,
            buttons: 
            {
                "Ok": function()
                {
                    $(this).dialog("close");
                    $(this).remove();
                }
            },
            close: function()
            {
            }
        });
	});
}

function addTableChangeHandler(oTable, changeHandler)
{
    oTable.children('tbody').children('tr').children('td').find('input, select').each(function()
    {
        var ctrl = this;
            $(ctrl).on('change', function(e){changeHandler(ctrl)});
    });
}

var BioUMLTable = {
        toggleCellContent: function(el) {
            $(el).parent().parent().children().toggle();
        }
}

/**
 * Add change handler to controls of one column only
 */
function addTableColumnChangeHandler(oTable, columnName, changeHandler)
{
    var index = getColumnIndexByName(oTable, columnName);
    if(index != -1)
    {
        index++; //nth-child start indexing from 1, but returned value is in 0-started range
        oTable.children('tbody').children('tr').each(function(){
            $(this).children('td:nth-child('+index+')').find('input, select').each(function()
                {
                    var ctrl = this;
                    $(ctrl).on('change', function(e){changeHandler(ctrl)});
            });
        });
    }
}

/*
 * DataTables plugin for pagination
 * Full Numbers - No Ellipses
 */

$.fn.DataTable.ext.pager.full_numbers_no_ellipses = function(page, pages){
    var numbers = [];
    var buttons = $.fn.DataTable.ext.pager.numbers_length;
    var half = Math.floor( buttons / 2 );
  
    var _range = function ( len, start ){
       var end;
     
       if ( typeof start === "undefined" ){
          start = 0;
          end = len;
  
       } else {
          end = start;
          start = len;
       }
  
       var out = [];
       for ( var i = start ; i < end; i++ ){ out.push(i); }
     
       return out;
    };
      
  
    if ( pages <= buttons ) {
       numbers = _range( 0, pages );
  
    } else if ( page <= half ) {
       numbers = _range( 0, buttons);
  
    } else if ( page >= pages - 1 - half ) {
       numbers = _range( pages - buttons, pages );
  
    } else {
       numbers = _range( page - half, page + half + 1);
    }
  
    numbers.DT_el = 'span';
  
    return [ 'first', 'previous', numbers, 'next', 'last' ];
 };
 
 function getDataTable(jqDomElement)
 {
     return jqDomElement.closest("table.display").dataTable();
 }
 
 function initTableContextMenu()
 {
     if(contextMenuInitialized['table'])
         return;
     $.contextMenu( {
         selector : '.documentData table th',
         className: 'table-context-menu',
         build: function($triggerElement, e){
             var dataTable = getDataTable($triggerElement);
             var curitems = {
                 'column_hide': { 
                     name: resources.menuHideThisColumn,
                     visible: true,
                     callback: function(itemKey, opt, originalEvent)
                     {
                         if(dataTable)
                             dataTable.fnSetColumnVis($triggerElement.data("column-id"), false);
                     }
                 }
             };
             var cols = dataTable.fnSettings().aoColumns;
             for(var i in cols)
             {
                 if(!cols[i].bVisible)
                 {
                     var id = "column-show-"+i;
                     var ind = i;
                     var showColumnI = _.bind( function(){
                         if(dataTable)
                             dataTable.fnSetColumnVis(this, true);
                     },i);
                     curitems[id] = { 
                         name: resources.menuShowColumn.replace("{name}", cols[i].sTitle),
                         visible: true,
                         callback: showColumnI
                     };
                 }
             }
             return {
                 callback: function(){},
                 items : curitems
             }
         }
     });
     contextMenuInitialized['table'] = true;
 }