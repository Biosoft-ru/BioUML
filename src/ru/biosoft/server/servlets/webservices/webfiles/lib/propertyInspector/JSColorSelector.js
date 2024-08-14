/* $Id: JSColorSelector.js,v 1.1 2012/01/26 11:51:16 lan Exp $ */

$.fn.jPicker.defaults.images.clientPath='lib/jpicker/images/';

function JSColorSelector( propInfo, format )
{
    JSInput.call(this, propInfo, format); // call parent constructor
    this.objectClassName = "JSColorSelector";
    this.originalName = propInfo.getAttribute("originalName");
};

JSColorSelector.prototype = new JSInput();
JSColorSelector.superClass = JSInput.prototype;

JSColorSelector.prototype.JSInput_updateView = JSInput.prototype.updateView;

JSColorSelector.prototype.createHTMLNode = function()
{
	var propName = this.propInfo.getName();
	var _this = this;
	this.node = $("<div/>");
	this.HTMLNode = $("<input type='hidden'/>").get(0);
	this.span = $("<div/>").width(100).height(16).css({border: "1px solid black", cursor: "pointer"});
	this.span.click(function()
	{
		var dialogDiv = $("<div title='Select color'/>");
		var pickerDiv = $("<div/>");
		var color = null;
		if(_this.value[0])
		{
			var val = JSON.parse(_this.value[0]);
			color = {active: new $.jPicker.Color({r:val[0], g:val[1], b:val[2]})};
		} else
			color = {active: new $.jPicker.Color()};
		dialogDiv.append(pickerDiv);
		pickerDiv.jPicker({
			color : color
		},
			function(color)
			{
				var val = color.val("hex") === null?"":"["+color.val('r')+","+color.val('g')+","+color.val('b')+"]";
				var oldValue = _this.value[0];
				_this.value[0] = val;
				_this.HTMLNode.value = val;
				_this.updateValue();
				_this.fireChangeListeners(oldValue, val);
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
	});
	this.node.append(this.span);
    return this.node.get(0);
};

JSColorSelector.prototype.getValue = function()
{
	return this.value;
};

JSColorSelector.prototype.updateValue = function()
{
    if(!this.value[0])
    	this.span.css({backgroundColor: "white"}).text("(default)");
    else
    {
		var val = $.parseJSON(this.value[0]);
		this.span.css({backgroundColor: "rgb("+val[0]+","+val[1]+","+val[2]+")"}).text("");
    }
};

JSColorSelector.prototype.setValue = function( val )
{
    var oldValue = this.getValue();
    this.value = val;
	this.HTMLNode.value = this.value[0];
	this.updateValue();
};