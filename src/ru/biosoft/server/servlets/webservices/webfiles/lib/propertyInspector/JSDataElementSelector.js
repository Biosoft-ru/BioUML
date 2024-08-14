/**
 * $Id: JSDataElementSelector.js,v 1.9 2011/06/14 11:00:48 lan Exp $
 *
 * DataElementSelector control (used in BioUML)
 */

function JSDataElementSelector( propInfo, format )
{
    JSGenericPopupControl.call(this, propInfo, format); // call parent constructor
    this.objectClassName = "JSDataElementSelector";
    this.elementClass = propInfo.getAttribute("dataElementType");
	this.childClass = propInfo.getAttribute("childElementType");
};

JSDataElementSelector.prototype = new JSGenericPopupControl();
JSDataElementSelector.superClass = JSGenericPopupControl.prototype;

JSDataElementSelector.prototype.JSInput_updateView = JSInput.prototype.updateView;

JSDataElementSelector.prototype.getTreeNodeId = function(path)
{
	return "tree_"+this.getModel().getName()+"_"+path;
};

JSDataElementSelector.prototype.getTreeNode = function(path)
{
	var id = this.getTreeNodeId(path);
	if(this.treeNode.ownerDocument != undefined)
		return this.treeNode.ownerDocument.getElementById(id);
	id = id.replace(/([\#\;\&\,\.\+\*\~\'\:\"\!\^\$\[\]\(\)\=\>\|\/\ ])/g, "\\$1");
	var node = $(this.treeNode).find("#"+id);
	return node.get(0);
};

JSDataElementSelector.prototype.getPathByTreeNode = function(node)
{
	var id = $(node).get(0).id;
	var prefix = "tree_"+this.getModel().getName()+"_";
	if(id.substr(0,prefix.length) == prefix) return id.substr(prefix.length);
	return null;
};

JSDataElementSelector.prototype.createDummy = function(parent)
{
	this.creatingItem = true;
	var dummy = this.tree.create({
		attributes: {
			id: this.getTreeNodeId("dummy!"+this.getPathByTreeNode(parent)),
			rel: "disabled"
		},
		data: {
			title: "Loading..."
		}
	}, parent).addClass("treeitem-disabled");
	dummy.children("a").get(0).style.backgroundImage = "url('icons/busy.png')";
	this.tree.close_branch(parent, true);
	this.creatingItem = false;
};

JSDataElementSelector.prototype.isDummyExists = function(path)
{
	return this.getTreeNode("dummy!"+path) != undefined;
};

JSDataElementSelector.prototype.createElement = function(path, isEnabled, hasChildren)
{
	var parentPath = getElementPath(path);
	var name = getElementName(path);
	var parentNode = parentPath == ""?-1:this.getTreeNode(parentPath);
    var childNode = this.tree.create({
        attributes: {
            id: this.getTreeNodeId(path),
			rel: isEnabled?"enabled":"disabled"
        },
        data: {
            title: name
        }
    }, parentNode);
	if(!isEnabled)
		childNode.addClass("treeitem-disabled");
	var dc = getDataCollection(parentPath);
    var icon = getNodeIcon(dc, name);
	if(hasChildren == undefined)
		hasChildren = icon == null;
    if (icon != null) 
        childNode.children("a").get(0).style.backgroundImage = icon;
	if (hasChildren)
		this.createDummy(childNode);
};

JSDataElementSelector.prototype.initTreeRoot = function()
{
	if(this.propInfo.getCanBeNull() != "no")
	{
		this.createElement(NONE_VALUE, true);
	}
	this.createElement("databases", false, true);
	this.createElement("data", false, true);
};

JSDataElementSelector.prototype.loadBranch = function(path, callback)
{
	var _this = this;
	if(!path || !_this.isDummyExists(path)) return;
	var dc = getDataCollection(path);
	dc.getFlaggedNameList(_this.elementClass, _this.childClass, null, function(nameList)
	{
		_this.creatingItem = true;
		$(_this.getTreeNode(path)).children("ul").children("li").each(function()
		{
			_this.tree.remove(this);
		});
        if (nameList != null) 
        {
            for (var i = 0; i < nameList.length; i++) 
            {
				_this.createElement(createPath(path, nameList[i].name), nameList[i].enabled, nameList[i].hasChildren);
			}
		}
		_this.creatingItem = false;
		if(callback) callback();
	});				
};

JSDataElementSelector.prototype.fill = function( tokens, addEmpty )
{
	this.tree = $.tree_create();
	this.treeNode = $('<div><ul></ul></div>');
	this.data.css("background-color", "white");
	this.data.append(this.treeNode);
	var _this = this;
	this.data.height(this.maxHeight);
	this.tree.init(this.treeNode, {
		callback: {
			onselect: function(node, treeObj)
			{
				var path = _this.getPathByTreeNode(node);
				if (path) 
				{
					_this.data.remove();
					var oldValue = _this.value; 
					_this.value = path;
					_this.text.val(path);
					if(!_this.bySystem)
						_this.fireChangeListeners(oldValue, _this.value, _this);
				}
			},
			onopen: function(node, treeObj)
			{
				if(_this.creatingItem) return;
				
				var path = _this.getPathByTreeNode(node);
				_this.loadBranch(path);
			}
		},
		rules: {
			clickable: ["enabled"]
		}
	});
	this.initTreeRoot();
	this.setValue(this.value);
};

/*
 * this function sorts items by innerHTML
 */
JSDataElementSelector.prototype.sortByHTML = function()
{
};

JSDataElementSelector.prototype.setValue = function(value)
{
	if(value == undefined) value = NONE_VALUE;
	var _this = this;
	var fields = getPathComponents(value);
	var curPath = fields[0];
	for(var i=1; i<fields.length; i++)
	{
		var node = this.getTreeNode(curPath);
		if(node == undefined) return;
		if(this.isDummyExists(curPath))
		{
			this.loadBranch(curPath, function() {_this.setValue(value);});
			return;
		}
		curPath = createPath(curPath, fields[i]);
	}
	var node = this.getTreeNode(value); 
	this.bySystem = true;
	this.tree.select_branch(node);
	this.bySystem = false;
};

JSDataElementSelector.prototype.setControlHandlers = function()
{
	var _this = this.tree;
	if(this.tree.selected)
		this.data.scrollTop(Math.max($(this.tree.selected).position().top-this.maxHeight/2,0));
};
