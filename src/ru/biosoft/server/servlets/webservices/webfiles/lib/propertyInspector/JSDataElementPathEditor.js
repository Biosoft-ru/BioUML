/**
 * $Id: JSDataElementPathEditor.js,v 1.59 2013/08/02 04:57:40 lan Exp $
 *
 * Input edit box or combobox.
 */
var NONE_VALUE = "(select element)";
function JSDataElementPathEditor( propInfo, format )
{
    JSInput.call(this, propInfo, format); // call parent constructor
    this.objectClassName = "JSDataElementPathEditor";
    this.elementClass = propInfo.getAttribute("dataElementType");
	this.childClass = propInfo.getAttribute("childElementType");
	this.referenceType = propInfo.getAttribute("referenceType");
	this.promptOverwrite = propInfo.getAttribute("promptOverwrite");
	this.elementMustExist = propInfo.getAttribute("elementMustExist");
	this.multiSelect = propInfo.getAttribute("multiSelect");
	this.icon = propInfo.getAttribute("icon");
	this.auto = propInfo.getAttribute("auto");

	this.elementHeight=18;
	this.minElementWidth=150;
	this.maxElementWidth=500;
	this.elementWidth=this.minElementWidth;
};

JSDataElementPathEditor.prototype = new JSInput();
JSDataElementPathEditor.superClass = JSInput.prototype;

JSDataElementPathEditor.prototype.getValue = function()
{
	if(this.value == NONE_VALUE) return "";
	return this.value;
};

JSDataElementPathEditor.prototype.setValue = function(val)
{
	if (this.multiSelect)
	{
		if (val === "")
			val = [];
	}
    else if(!val) val = NONE_VALUE; 
    else if (val.indexOf("/") == -1 && val != NONE_VALUE && val != "")
		val += "/";
	if(!this.multiSelect 
			&& val !== "" 
			&& val.substring(0, "data/".length) !== "data/"
			&& val.substring(0, "databases/".length) !== "databases/"
			&& !_.any(perspective.repository, function(root)
			{
				return val.substring(0, root.path.length+1) === root.path+"/";  
			}))
	{
		val = NONE_VALUE;
	}
	
	var oldValue = this.value;
	this.value = val;
	if(this.multiSelect)
	{
		var names = [];
		for(var i = 0; i < val.length; i++)
		{
			names.push(getElementName(val[i]));
		}
		fitElement(this.getHTMLNode(), "["+names.length+"] "+names.join(";"), true, this.controlWidth);
	} else
	{
		fitElement(this.getHTMLNode(), val, false, this.controlWidth);
		if(val != NONE_VALUE)
		{
			this.node.attr("data-path", val);
			createTreeItemDraggable(this.node, {cancel: ""});
			addTreeItemContextMenu(this.node);
		}
	}
	this.fireChangeListeners(oldValue, val); 
	if(this.checkBox)
	{
		if(this.getValue()=='')
			this.checkBox.prop("checked", false);
		else
			this.checkBox.prop("checked", true);
	}
};

// override
JSDataElementPathEditor.prototype.createHTMLNode = function()
{
    var propName = this.propInfo.getName();
	
    var width = 250;
	this.node = $('<div/>').addClass(this.propInfo.getCanBeNull() != "no"?"data-element-path-editor-checkboxed":"data-element-path-editor");

    this.HTMLNode = document.createElement("input");
    this.HTMLNode.type = "text";
    this.HTMLNode.readOnly = true;
    this.HTMLNode.id = this.createNodeId(propName);
    if( this.changeListener != null )
    {
        addModificationListener(this.HTMLNode, this.changeListener);
    }

    var _this = this;

    $(this.HTMLNode).mousedown(function()
    {
        _this.onMouseDown(propName);
    });
	
	$(this.HTMLNode).click(function()
	{
		_this.openDialog();
	});
	
	createTreeItemDroppable(this.HTMLNode, null, function(element, event) {
		var path = getElementPath(element);
		var name = getElementName(element);
		var dc = getDataCollection(path);
		dc.setFilters(_this.elementClass, _this.childClass, _this.referenceType);
		dc.getElementInfo(name, function(info, enabled)
		{
			if(info == undefined)
			{
				logger.error(resources.dlgOpenSaveErrorNotFound.replace("{element}", element));
				return;
			}
			if(info.enabled === false && _this.elementMustExist)
			{
				if(_this.multiSelect && instanceOf(info["class"], "ru.biosoft.access.core.FolderCollection"))
				{
					var dc1 = getDataCollection(element); 
					dc1.setFilters(_this.elementClass, _this.childClass, _this.referenceType);
					var MAX_ELEMENTS_TO_ADD = 100;
					dc1.getElementInfoRange(0,MAX_ELEMENTS_TO_ADD, function(info, enabled)
					{
						var val = [];
						for(var i=0; i<info.length; i++)
						{
							if(info[i].enabled)
							{
								val.push(createPath(element, info[i].name));
							}
						}
						if(val.length > 0)
						{
							val.sort();
							_this.setValue(val);
							if(dc1.getSize() > MAX_ELEMENTS_TO_ADD)
								logger.message(resources.dlgOpenSaveWarnBigFolder);
						} else
						{
							logger.error(resources.dlgOpenSaveErrorBadFolder.replace("{folder}", element));
						}
					});
				} else
				{
					if(_this.referenceType)
						logger.error(resources.dlgOpenSaveErrorInvalidTypeVerbose.replace("{element}", element).replace("{type}", _this.referenceType));
					else
						logger.error(resources.dlgOpenSaveErrorInvalidType.replace("{element}", element));
				}
				return;
			}
			if(!enabled && !_this.elementMustExist)
			{
				logger.error(resources.dlgOpenSaveErrorReadOnlyFolder);
				return;
			}
			if(!_this.elementMustExist && 
					instanceOf(info["class"], "ru.biosoft.access.core.FolderCollection") && 
					(_this.elementClass == null || !instanceOf(_this.elementClass, "ru.biosoft.access.core.FolderCollection")))
			{
				var name = getElementName(_this.getValue());
				if(name == undefined || name === "")
					name = _this.getModel().getDisplayName();
				_this.setValue(createPath(element, name));
				return;
			}
			if(_this.multiSelect)
			{
				if((event.ctrlKey || event.metaKey) && (_this.value != undefined && _this.value.length > 0))
				{
					var val = [element];
					for(var j = 0; j<_this.value.length; j++) 
					{
						if(_this.value[j] == element) return;
						val.push(_this.value[j]);
					}
					val.sort();
					_this.setValue(val);
				} else
					_this.setValue([element]);
			} else _this.setValue(element);
		});
	});

	if(this.icon !== undefined)
	{
		this.node.append($("<img/>").addClass("path-editor-icon").attr("src", appInfo.serverPath+"web/img?id="+this.icon).width(16).height(16));
		width-=18;
	}
	if(this.propInfo.getCanBeNull() != "no")
	{
		this.checkBox = $('<input type="checkbox"'+(this.getValue()==''?'':' checked')+'>');
		this.checkBox.change(function(e) {
			if(_this.checkBox.prop("checked"))
			{
				_this.checkBox.prop("checked", false);
				_this.openDialog();
			} else
			{
				_this.setValue(NONE_VALUE);
			}
		});
		this.node.append(this.checkBox);
		width-=25;
	}
	if(this.multiSelect)
	{
		this.plusButton = $('<input type="button" class="ui-button ui-widget ui-state-default ui-corner-all ui-button-text-only" value="+"/>')
			.css({width: 16, margin: "0px 1px", padding: 0}).attr("title", resources.dlgOpenSaveAddButtonTitle);
		this.node.append(this.plusButton);
		this.plusButton.click(function() {
			_this.openDialog(true);
		});
		width-=18;
	}
	this.HTMLNode.style.width = width+"px";
	this.controlWidth = width;
	this.node.append(this.HTMLNode);
	
	if(this.auto == "off")
	{
		this.node.append($("<input type='button' class='button ui-corner-all ui-state-default'>").val(resources.dlgOpenSaveButtonAuto).click(function() {_this.setValue("");}));
	}
	
    return this.node.get(0);
};

// TODO: correctly initialize dialog in multiselect mode when several elements are selected on different screens
JSDataElementPathEditor.prototype.updateList = function()
{
	var _this = this;
	// TODO: fix list in the case of huge scrolling (> 1e7 elements)
	var pos = _this.list.scrollLeft();
	var colFrom = Math.floor(pos/_this.elementWidth);
	var colTo = Math.min(Math.floor((pos+_this.list.innerWidth())/_this.elementWidth), Math.ceil(_this.listSize/_this.nRows));
	for(;colFrom<=colTo && _this.columns[colFrom]; colFrom++);
	for(;colTo>=colFrom && _this.columns[colTo]; colTo--);
	if(colFrom>colTo) return;
	var elementFrom = colFrom * _this.nRows;
	var elementTo = (colTo+1) * _this.nRows;
	_this.dc.setFilters(_this.elementClass, _this.childClass, _this.referenceType);
	_this.dc.getElementInfoRange(elementFrom, elementTo, function(nameList)
	{
		if(nameList == null) return;
		var values = {};
		if(_this.multiSelect)
		{
			var vals = _this.nameField.val().split(";");
			for(var i = 0; i < vals.length; i++)
				values[vals[i]] = 1;
		}
		else
			values[_this.nameField.val()] = 1;
        for ( var i = 0; i < nameList.length; i++) 
        {
        	if(_this.columns[Math.floor((i + elementFrom) / _this.nRows)]) continue;
        	var title = nameList[i].title == undefined?nameList[i].name:nameList[i].title;
			var element = $("<li>").attr("data-name", nameList[i].name).attr("title", title).attr("data-pos", i+elementFrom)
					.width(_this.elementWidth).height(_this.elementHeight).css(
					{
						"background-image" : getNodeIcon(_this.dc, nameList[i].name),
						"left" : Math.floor((i + elementFrom) / _this.nRows) * _this.elementWidth + "px",
						"top" : Math.floor((i + elementFrom) % _this.nRows) * _this.elementHeight + "px"
					});
			if(values[nameList[i].name]) element.addClass("selected");
			if(nameList[i].enabled === false) element.addClass("disabled");
			if(nameList[i].hasChildren) element.addClass("hasChildren");
			var textDiv = $("<div/>").css("left", 17);
			element.append(textDiv);
			_this.list.append(element);
			fitElement(textDiv, title, true, _this.elementWidth-21);
		}
		for ( var i = colFrom; i <= colTo; i++)
			_this.columns[i] = true;
	});
};

JSDataElementPathEditor.prototype.loadBranch = function(path, callback)
{
	var _this = this;
	if(!path) return;
	_this.dc = getDataCollection(path);
	if(instanceOf(_this.dc.getClass(), "ru.biosoft.access.core.FolderCollection"))
	{
		_this.folderButton.removeAttr("disabled").removeClass("ui-state-disabled");
	} else
	{
		_this.folderButton.attr("disabled", "disabled").addClass("ui-state-disabled");
	}
	_this.dc.setFilters(_this.elementClass, _this.childClass, _this.referenceType);
	_this.dc.getSize(function(size, enabled)
	{
		_this.enabled = enabled;
		_this.list.html("");
		_this.listSize = size;
		_this.columns = [];
		
		_this.elementWidth = _this.minElementWidth;
		var names = _this.dc.getElementInfoRange(0, NAME_LIST_CHUNK_SIZE);
		var style = [_this.list.css("font-style"),_this.list.css("font-variant"),_this.list.css("font-weight"),_this.list.css("font-size"),_this.list.css("font-family")].join(" ");
		for(var i=0; i<names.length; i++)
		{
			var length = measureTextLength(style, names[i].name)+22;
			if(length > _this.elementWidth) _this.elementWidth = length;
			if(_this.elementWidth >= _this.maxElementWidth)
			{
				_this.elementWidth = _this.maxElementWidth;
				break;
			}
		}
		
		_this.nRows = Math.floor(_this.list.innerHeight()/(_this.elementHeight));
		// Scrollbar will be visible: reduce column size by 1
		// TODO: calculate actual scrollbar height
		if(Math.ceil(size/_this.nRows)*(_this.elementWidth) > _this.list.innerWidth())
			_this.nRows--;
		_this.nColumns = Math.ceil(size/_this.nRows);
		_this.list.css("position", "relative");
		_this.list.append($("<div/>").addClass("ridge").width(_this.nColumns*_this.elementWidth));
		if(_this.nameField.val() == "" || _this.nameField.val() == NONE_VALUE)
		{
			_this.updateList();
			return;
		}
		var value = _this.multiSelect?_this.nameField.val().split(";")[0]:_this.nameField.val();
		_this.dc.getElementInfo(value, function(info)
		{
			if(info != null)
			{
				var i = _this.dc.nameMap[value];
				_this.list.scrollLeft(Math.floor(i / _this.nRows) * _this.elementWidth - _this.list.innerWidth() / 2);
			} else if(_this.elementMustExist)
				_this.nameField.val("");
			_this.updateList();
		});
	});
};

JSDataElementPathEditor.prototype.openDialog = function(addMode)
{
	var _this = this;
	var dialogContent = $('<div/>');
	var tableSkeleton = $('<table width="500" height="300"><tr><td width="5%" align="right">'
			+ resources.dlgOpenSaveLabelCollection
			+ '<td width="90%" id="dataElementDialogTreePopupBlock"><td width="5%" id="dataElementDialogUpButtonBlock" style="white-space: nowrap;">'
			+ '<tr><td height="80%" width="100%" colspan="3" id="dataElementDialogListBlock"><tr><td align="right" id="refTypeLabel" style="white-space: nowrap"><td id="refTypeName" style="font-weight: bold"><tr><td align="right">'
			+ resources.dlgOpenSaveLabelName
			+ '<td width="95%" colspan="2" id="dataElementDialogNameBlock"></tr></table>');
	this.nameField = $('<input type="text">').css("width", "100%");
	var value = this.getValue();
	var path;
	if(this.multiSelect)
	{
		if(value == undefined || value.length == 0) 
			path = getPathFromPreferences(this.childClass, this.elementClass);
		else 
			path = getElementPath(value[0]);
		if(!addMode)
		{
			var names = [];
			for(var i = 0; i < value.length; i++)
			{
				if(getElementPath(value[i]) === path)
				{
					names.push(getElementName(value[i]));
				}
			}
			this.nameField.val(names.join(";"));
		}
	} else
	{
		if(value == "") path = getPathFromPreferences(this.childClass, this.elementClass);
		else if(this.elementMustExist && instanceOf(getDataCollection(value).getClass(),"ru.biosoft.access.core.FolderCollection"))
		{
			path = value;
			this.nameField.val("");
		}
		else
		{
			path = getElementPath(value);
			this.nameField.val(getElementName(value));
		}
	}
	this.nameField.keydown(function(e) {
		if (e.keyCode == 13)	// enter 
		{
			_this.okHandler(_this.popup.value, _this.nameField.val());
			e.stopPropagation();
			e.preventDefault();
		}
	});
	tableSkeleton.find("#dataElementDialogNameBlock").append(this.nameField);
	if(this.referenceType)
	{
		tableSkeleton.find("#refTypeLabel").text(resources.dlgOpenSaveLabelType);
		tableSkeleton.find("#refTypeName").text(this.referenceType);
	}
	var upButton = $('<input type="button">').val(resources.dlgOpenSaveButtonUp).addClass("ui-corner-all").addClass("ui-state-default");
	upButton.click(function() {
		var newCollection = getElementPath(_this.popup.value);
		if(newCollection)
			_this.popup.setValue(newCollection);
	});
	this.folderButton = $('<input type="button">').attr("title", resources.dlgOpenSaveButtonNewFolderTitle).val(resources.dlgOpenSaveButtonNewFolder).addClass("ui-corner-all").addClass("ui-state-default");
	this.folderButton.click(function() {
		createGenericCollection(_this.popup.value, function(name) {
			_this.popup.invalidateBranch(_this.popup.value);
			_this.popup.setValue(createPath(_this.popup.value, name));
			if($(this).attr("data-name") == _this.nameField.val())
				_this.nameField.val("");				
		});
	});
	tableSkeleton.find("#dataElementDialogUpButtonBlock").append(upButton).append(" ").append(this.folderButton);
	this.list = $('<ul/>').addClass("elementList").css("width", "95%").css("height", "95%").css("border", "1px groove gray");
	tableSkeleton.find("#dataElementDialogListBlock").append(this.list);
	this.popup = new JSCollectionSelectPopup(tableSkeleton.find("#dataElementDialogTreePopupBlock"), this.getModel().getName()+"_popup",
	function(value)
	{
		_this.loadBranch(value);
	});
	dialogContent.append(tableSkeleton);
	this.finalOkHandler = function(path, names)
	{
		var value;
		if(_this.multiSelect)
		{
			if(addMode)
				value = this.getValue();
			else
				value = [];
			for(var i = 0; i < names.length; i++)
				value.push(createPath(path, names[i]));
		} else
		{
			value = createPath(path, names[0]);
		}
		dialogContent.dialog("close");
		dialogContent.remove();
		_this.setValue(value);
        if(!_this.multiSelect)
            storePathToPreferences(_this.childClass, _this.elementClass, value);
		if (_this.checkBox) 
			_this.checkBox.attr("checked", "checked");
	};
	this.normalizeName = function(name)
	{
		return name.replace(/^\s\s*/, '').replace(/\s\s*$/, '');	// trim spaces
	};
	this.okHandler = function(path, name)
	{
		name = this.normalizeName(name);
		var dc = getDataCollection(path);
		if (name.indexOf("/") > -1 || !isDataElementNameValid(name)) 
		{
			logger.error(resources.dlgOpenSaveErrorInvalidCharactersVerbose);
			return;
		}
		if (name=="")
		{
			if (_this.elementMustExist) 
			{
				var newPath = getElementPath(path);
				var newName = getElementName(path);
				_this.okHandler(newPath, newName);
			} else
				logger.error(resources.commonErrorEmptyNameProhibited);
			return;
		}
		var names = _this.multiSelect?name.split(";"):[name];
		var exists = false;
		var typeOk = false;
		dc.setFilters(_this.elementClass, _this.childClass, _this.referenceType);
		for(var nameIndex = 0; nameIndex < names.length; nameIndex++)
		{
			name = names[nameIndex];
			if(_this.elementMustExist || _this.promptOverwrite)
			{
	            var info = dc.getElementInfo(name);
	            if(info != null)
	            {
	                exists = true;
	                typeOk = (info.enabled !== false);
	            }
			}
			if(_this.elementMustExist && !exists)
			{
				logger.error(resources.dlgOpenSaveErrorNotFound.replace("{element}", name));
				return;
			}
			if(_this.elementMustExist && !typeOk)
			{
				logger.error(resources.dlgOpenSaveErrorInvalidType.replace("{element}", name));
				return;
			}
			if(!_this.elementMustExist && !_this.enabled)
			{
				logger.error(resources.dlgOpenSaveErrorIncompatibleFolder);
				return;
			}
			if(_this.promptOverwrite && exists)
			{
				createConfirmDialog(resources.dlgOpenSaveConfirmOverwrite.replace("{element}", name), 
                function()
                {
                    _this.finalOkHandler(path, names);
                });
				return;
			}
		}
		_this.finalOkHandler(path, names);
	};
	var itemClick = function(e) {
		var items = _this.list.children("li");
		if (_this.multiSelect && (e.ctrlKey || e.metaKey) && !e.shiftKey) 
		{
			$(this).toggleClass("selected");
			_this.lastSelected = items.index($(this));
		}
		else if(_this.multiSelect && e.shiftKey)
		{
			var curSelected = items.index($(this));
			var last = items.eq(_this.lastSelected);
			var colFrom = Math.floor($(this).attr("data-pos")/_this.nRows);
			var colTo = Math.floor(last.attr("data-pos")/_this.nRows);
			if(colTo < colFrom)
			{
				var tmp = colTo; colTo = colFrom; colFrom = tmp;
			}
			// Now Shift-selection is disabled if not all columns between from and to were loaded
			// TODO: load all columns instead
			for(var i=colFrom; i<=colTo; i++)
				if(!_this.columns[i]) return;
			if(!e.ctrlKey && !e.metaKey)
				items.removeClass("selected");
			if(last.hasClass("selected") || (!e.ctrlKey && !e.metaKey))
			{
				items.slice(Math.min(curSelected, _this.lastSelected), Math.max(curSelected, _this.lastSelected)+1).addClass("selected");
			} else
			{
				items.slice(Math.min(curSelected, _this.lastSelected), Math.max(curSelected, _this.lastSelected)+1).removeClass("selected");
			}
		} else
		{
			items.removeClass("selected");
			$(this).addClass("selected");
			_this.lastSelected = items.index($(this));
		}
		var selection = items.filter(".selected");
		if(!_this.elementMustExist && selection.length == 1 && 
				selection.hasClass("disabled") && 
				instanceOf(getDataCollection(_this.popup.value).getElementInfo(selection.attr("data-name"))["class"], "ru.biosoft.access.core.FolderCollection"))
		{
			selection = selection.not(".disabled");
		}
		var values = [];
		selection.each(function() {values.push($(this).attr("data-name"));});
		if(values.length > 0 || _this.elementMustExist)
		{
			_this.nameField.val(values.join(";"));
			_this.nameField.focus();
			_this.nameField.get(0).select(0, -1);
		}
		e.stopPropagation();
		e.preventDefault();
	};
	var itemDoubleClick = function(e) {
		if($(this).hasClass("disabled") || 
		        (!_this.elementClass && !_this.childClass && $(this).hasClass("hasChildren")) || 
		        instanceOf(getDataCollection(_this.popup.value).getChildClass($(this).attr("data-name")),"ru.biosoft.access.core.FolderCollection") ||
		        instanceOf(getDataCollection(_this.popup.value).getChildClass($(this).attr("data-name")),"biouml.model.Module"))
		{
			if($(this).hasClass("hasChildren"))
			{
				_this.popup.setValue(createPath(_this.popup.value, $(this).attr("data-name")));
				if($(this).attr("data-name") == _this.nameField.val())
					_this.nameField.val("");				
			}
		} else
		{
			_this.okHandler(_this.popup.value, _this.nameField.val());
		}
		e.stopPropagation();
		e.preventDefault();
	};
	dialogContent.dialog(
    {
        modal: true,
        autoOpen: true,
		width: 550,
		title: _this.getModel().getDescriptor().getDisplayName()+(addMode?resources.dlgOpenSaveAddModeTitleText:""),
		open: function()
		{
			_this.list.scroll(function() {_this.updateList();});
			$("#dataElementDialogListBlock")
					.on("click", "li", itemClick)
					.on("dblclick", "li", itemDoubleClick);
		},
        close: function()
        {
        	$("#dataElementDialogListBlock li").off();
        },
        buttons: 
        {
			"Cancel": function()
			{
                $(this).dialog("close");
                $(this).remove();
			},
            "Ok": function()
			{
				_this.okHandler(_this.popup.value, _this.nameField.val());
			}
        }
    });
	this.popup.setValue(path);
	this.nameField.focus();
	this.nameField.get(0).select(0,-1);
};

$(function() {$(document).bind("mousedown", function(e) {
    if(!$(e.target).closest(".genericComboData2").length)
        $(".genericComboData2").trigger("remove").detach();
})});

function JSCollectionSelectPopup( parent, id, changeCallback )
{
	this.id = id;
	this.treeLoaded = false;
	this.nextValue = undefined;
	this.changeCallback = changeCallback;
	this.width = 350;
	this.maxHeight = 300;	// if popup height exceeds this, vertical scrollbar appears
	var _this = this;
    this.container = $("<div/>").addClass("genericComboContainer").width(this.width);
    this.text = $("<input type='text' readonly autocomplete='off'/>").addClass("genericComboText").width(this.width - 20);
    this.img = $("<div/>").addClass("genericComboButton");
    this.container.append(this.img).append(this.text);
	this.text.addClass("form-value-control-el-js");
	$(parent).append(this.container);
	$(parent).height(this.container.outerHeight());
    this.treeCont = $("<div id='tree_container_data_xxx'/>").addClass("genericComboData2").addClass("ui-widget")
        .css("position", "absolute").css("overflow", "auto").css("border", "1px solid black");
	this.container.click(function(e)
    {
        _this.toggleDropDown(e);
    });
//	if(this.tree)
//	    this.tree.jstree('destroy', 'true');
//	delete this.tree;
	this.fill();
}

JSCollectionSelectPopup.prototype.getTreeNodeId = function(path)
{
	return "tree_"+this.id+"_"+path;
};

JSCollectionSelectPopup.prototype.getTreeNode = function(path)
{
	var id = this.getTreeNodeId(path);
	if(this.treeNode.ownerDocument != undefined)
		return this.treeNode.ownerDocument.getElementById(id);
	// Special symbols should be escaped in selectors:
	// http://api.jquery.com/category/selectors/
    //id = id.replace(/([\!\"\#\$\%\&\'\(\)\*\+\,\.\/\:\;\<\=\>\?\@\[\\\]\^\`\{\|\}\~\ ])/g, "\\$1");
	var node = $.jstree.reference(this.tree).get_node("#"+id);
	return node;
};

JSCollectionSelectPopup.prototype.getPathByTreeNode = function(node)
{
	var id = $(node).get(0).id;
	var prefix = "tree_"+this.id+"_";
	if(id.substr(0,prefix.length) == prefix) return id.substr(prefix.length);
	return null;
};

JSCollectionSelectPopup.prototype.invalidateBranch = function(path)
{
    var node = this.getTreeNode(path);
	if(node == undefined ) return;
	$.jstree.reference(this.tree).refresh_node(node);
};

JSCollectionSelectPopup.prototype.createElement = function(parentNode, path, isEnabled, hasChildren)
{
    var parentPath = getElementPath(path);
    var name = getElementName(path);
    var childNode =
    {
        id : this.getTreeNodeId(path),
        parent : parentNode.id,
        text : name,
        state : {
            opened    : false, 
            disabled  : false, 
            selected  : false
        }
    }; 
    
    if(!isEnabled)
        childNode.state.disabled = true;
    var dc = getDataCollection(parentPath);
    var icon = getNodeIcon(dc, name);
    childNode.icon = icon.substring(5, icon.length - 2);
    if(hasChildren == undefined)
        hasChildren = dc==null?!!rootMap[name]:!dc.isChildLeaf(name);
    childNode.children = hasChildren;
    return childNode;
};

JSCollectionSelectPopup.prototype.getTreeNodes = function(parentNode, callback)
{
    var paths = {};
    if(parentNode.id=="#")
    {
        var elements = [];
        _.each(perspective.repository, function(root)
        {
            paths[root.path] = 1;
            elements.push(this.createElement(parentNode, root.path, true));
        }, this);
        if(!paths.data) elements.push(this.createElement(parentNode, "data", true));
        if(!paths.databases) elements.push(this.createElement(parentNode, "databases", true));
        callback(elements);
    }
    else
    {
        var _this = this;
        var path = _this.getPathByTreeNode(parentNode);
        if(!path)
        {
            callback([]);
            return;
        }
        var dc = getDataCollection(path);
        dc.getSize( new LoadNameListCallback2(parentNode, dc, path, _this, callback).func );
    }
}

function LoadNameListCallback2(parentNode, dc, path, _this, callback)
{
    var parentPath = getTreeNodePath(parentNode);
    var maxItemsToLoad = 5000;
    this.func = function(size)
    {
        if (size > maxItemsToLoad && size > maxItemsToLoad*2) 
        {        
            dc.getElementInfoRange(0, maxItemsToLoad, function(nameList)
            {
                _this.creatingItem = true;
                var elements = [];
                if (nameList != null) 
                {
                    for (var i = 0; i < nameList.length; i++) 
                    {
                        var title = nameList[i].name;
                        if(nameList[i].title)
                        {
                            title = nameList[i].title;
                        }
                        var childNode =
                        {
                            id : _this.getTreeNodeId(createPath(path, nameList[i].name)),
                            parent : parentNode.id,
                            text : title,
                            state : {
                                opened    : false, 
                                disabled  : false, 
                                selected  : false
                            },
                            'children' : nameList[i].hasChildren
                        }; 
                        
                        var dc = getDataCollection(path);
                        var icon = getNodeIcon(dc, nameList[i].name);
                        childNode.icon = icon.substring(5, icon.length - 2);
                        elements.push(childNode);
                    }
                }
                if (nameList != null && nameList.length>0) 
                {
                    var numToLoad = size-maxItemsToLoad;
                    var lastElement =
                    {
                        id : _this.getTreeNodeId("dummy_" + createPath(path, "dummy")),
                        parent : parentNode.id,
                        text : numToLoad + " more elements...",
                        state : {
                            opened    : false, 
                            disabled  : true, 
                            selected  : false
                        }
                    };
                    elements.push(lastElement);
                    _this.creatingItem = false;
                    callback(elements);
                }
            });
        } 
        else
        {
            dc.getNameList(function(nameList)
            {
                _this.creatingItem = true;
                var elements = [];
                if (nameList != null) 
                {
                    for (var i = 0; i < nameList.length; i++) 
                    {
                        var title = nameList[i].name;
                        if(nameList[i].title)
                        {
                            title = nameList[i].title;
                        }
                        var childNode =
                        {
                            id : _this.getTreeNodeId(createPath(path, nameList[i].name)),
                            parent : parentNode.id,
                            text : title,
                            state : {
                                opened    : false, 
                                disabled  : false, 
                                selected  : false
                            },
                            'children' : nameList[i].hasChildren
                        }; 
                        
                        var dc = getDataCollection(path);
                        var icon = getNodeIcon(dc, nameList[i].name);
                        childNode.icon = icon.substring(5, icon.length - 2);
                        elements.push(childNode);
                    }
                }
                _this.creatingItem = false;
                callback(elements);
            });
         }
    };
}


JSCollectionSelectPopup.prototype.fill = function( )
{
	this.treeNode = $('<div><ul></ul></div>');
	this.treeCont.css("background-color", "white");
	this.treeCont.append(this.treeNode);
	var _this = this;
	this.treeCont.height(this.maxHeight);
	this.createTreeObject();

	//JSUPDATE check
//	var _this = this;
//	setTimeout(function(){
	    //_this.setValue(_this.value);}, 500);
};

JSCollectionSelectPopup.prototype.createTreeObject = function()
{
    var instance = this;
    this.tree = this.treeNode.jstree({
        'core' : 
        {
          'check_callback' : function(o, n, p, i, m) {
              if(o === "move_node" || o === "copy_node") {
                  return false;
              }
              return true;
          },
          'data' : function (obj, callback) {
              let _this = this;
              instance.getTreeNodes(obj, function(newNodes){
                  callback.call(_this, newNodes)
              });
          },
          'multiple' : true,
          'animation' : 0,
          'themes' : {
              'variant' : 'small'
          },
        },
      'types' : {
          'default' : { 'icon' : 'folder' },
      },
      
      'plugins' : ['changed']
    });
    
    this.tree.on('changed.jstree', function (e, data) {
        if(!data.node)
            return;
        var path = instance.getPathByTreeNode(data.node);
        if (path) 
        {
            instance.treeCont.detach();
            var oldValue = instance.value; 
            instance.value = path;
            instance.text.val(path);
            if(instance.changeCallback)
                instance.changeCallback(instance.value, instance.bySystem);
        }
    }).on('ready.jstree', function(){ 
        instance.treeLoaded = true;
        if(instance.nextValue)
            instance.setValue(instance.nextValue);
    });
}

JSCollectionSelectPopup.prototype.setValue = function(value)
{
    if(value == undefined || value == "") return;
    if(!this.treeLoaded)
    {
        this.nextValue = value;
        return;
    }
	var _this = this;
	var fields = getPathComponents(value);
	var curPath = fields[0];
	let tries = 0;
	for(var i=1; i<fields.length; i++)
	{
		var jstreeNode = this.getTreeNode(curPath);
		if(!jstreeNode || !jstreeNode.state)
	    {
		    return;
	    }
		if(!jstreeNode.state.loaded)
	    {
		    $.jstree.reference(this.tree).load_node( jstreeNode, function(){_.defer(function() {_this.setValue(value);})});
            return;
	    }
		curPath = createPath(curPath, fields[i]);
	}
	var node = this.getTreeNode(value);
	while(node == undefined)
	{
		value = getElementPath(value);
		node = this.getTreeNode(value);
	}
	this.bySystem = true;
	$.jstree.reference(this.tree).deselect_all(true);
	$.jstree.reference(this.tree).select_node(node.id);
	this.bySystem = false;
};

JSCollectionSelectPopup.prototype.toggleDropDown = function(e)
{
	if(this.treeCont.filter(":visible").length)
    {
	    this.treeCont.detach();
    }
	else
	{
		$(".genericComboData2").detach();
		$(".genericComboContainer").css("z-index", 1000);
		this.container.css("z-index", 2000);
		//this.treeCont.show();
		//JSUPDATE
		this.treeCont.css("top", this.container.offset().top+this.container.outerHeight())
			.css("left", this.container.offset().left)
			.width(this.container.outerWidth() - 2).css("z-index", 2000);
		$('body').append(this.treeCont);
		// vertical scrolling
		if(this.treeCont.height() > this.maxHeight)
			this.treeCont.height(this.maxHeight);
		if(this.tree.selected)
			this.treeCont.scrollTop(Math.max($(this.tree.selected).position().top-this.maxHeight/2,0));
	}
	e.stopPropagation();
};
