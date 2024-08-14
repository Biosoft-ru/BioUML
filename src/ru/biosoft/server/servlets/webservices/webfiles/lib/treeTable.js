/* $Id: treetable.js,v 1.7 2011/12/19 11:22:24 lan Exp $ */
/**
 * TreeTable document realization
 * 
 * @author tolstyh
 */
function TreeTableDocument(completeName)
{
	var _this = this;
    this.completeName = completeName;
    
    queryBean(completeName, {fields: "treePath"}, function(data)
    {
    	_this.selectionBase = data.values[0].value;
    	_this.visibleActions = null;
    	updateToolbar();
    });
    
    this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId(completeName);
    
    var dc = getDataCollection(this.completeName);
    dc.addChangeListener(this);
    dc.addRemoveListener(this);

    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var tableDocument = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(tableDocument);
		
        this.tableContainerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        tableDocument.append(this.tableContainerDiv);
        
        this.tableDataDiv = $('<div id="' + this.tabId + '_data">'+resources.commonLoading+'</div>').css("width", "95%").css("padding", "2pt");
        this.tableContainerDiv.append(this.tableDataDiv);
        
        this.loadTreeTable();
        
        //update document size
        resizeDocumentsTabs();
        selectViewPart('common.description');
    };

    this.getSelection = function()
    {
    	return $.map(this.table.children("tbody").children("tr.row_selected"), function(e)
    	{
    		return $(e).data("path");
    	});
    };

    this.loadTreeTable = function()
    {
        queryBioUML("web/treetable", 
        {
        	action: "sceleton",
        	de: _this.completeName
        }, 
        function(data)
        {
        	_this.tableDataDiv.html(data.values);
        	_this.table = _this.tableDataDiv.children("table");
        	_this.table.addClass("selectable_table");
        	
        	_this.loadRows();
        });
    };
    
    this.loadRows = function(parentNode)
    {
    	var parentPath = parentNode?parentNode.data("path"):"";
    	queryBioUML("web/treetable", 
    	{
    		action: "children",
    		parentPath: parentPath,
            de: _this.completeName
        }, 
        function(data)
        {
        	var parent = _this.table.children("tbody");
        	var lastNode = parentNode;
        	for(i in data.values)
        	{
        		var rowData = data.values[i];
        		var path = (parentPath.length == 0)?(rowData.id):(parentPath+'/'+rowData.id);
        		
        		var tr = $("<tr></tr>");
        		tr.data("path", path);
        		if(parentNode)
        		{
        		    tr.addClass("child-of-"+parentNode.attr("id")).insertAfter(lastNode);
        		    lastNode = tr;
        		}
        		else
            		parent.append(tr);
        		
        		tr.attr('id',this.tabId+"-"+path);
        		tr.addClass("parent");
        		var spanType = "file";
        		if(rowData.isLeaf)
        		{
        			tr.addClass("leaf");
        		}
        		else
        		{
        			spanType = "folder";
        			tr.addClass("notloaded");
        		}
        		tr.append("<td><span class='"+spanType+"'>"+rowData.id+"</span></td>");
        		for(j in rowData.values)
        		{
        			var val = rowData.values[j];
        			if(val.length == 0)
        				val = "&nbsp;";
        			var td = $("<td></td>");
        			tr.append(td);
        			td.html(val);
        		}
        		tr.mousedown(function() 
        	    {
            		var node = $(this);
            	    $(this).find(".expander").click(function()
            	    {
            	    	if(node.hasClass("notloaded"))
            	    	{
            	    		node.removeClass("notloaded");
            	    		_this.loadRows(node);
            	    	}
            	    });
            	});
        		
        		if(_this.treetable)
        			_this.treetable.initializeRow(tr);
        	}
        	if(!_this.treetable)
        		_this.treetable = _this.table.treeTable();

            _this.table.find('.table_script_node').each(function() {
                eval($(this).text());
            }).remove();
        });
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
}
