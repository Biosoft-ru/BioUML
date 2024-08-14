/**
 * $Id: JSArrayControl.js,v 1.4 2011/09/21 03:50:30 anna Exp $
 *
 * Script editor box
 */

function JSArrayControl( propInfo, format )
{
    JSInput.call(this, propInfo, format); // call parent constructor
    this.objectClassName = "JSArrayControl";
};

JSArrayControl.prototype = new JSInput();
JSArrayControl.superClass = JSInput.prototype;

// override
JSArrayControl.prototype.createHTMLNode = function()
{
    var propName = this.propInfo.getName();
	
    this.node = $('<div/>');

    this.HTMLNode = document.createElement("span");
    this.HTMLNode.id = this.createNodeId(propName);
    if( this.changeListener != null )
    {
        addModificationListener(this.HTMLNode, this.changeListener);
    }
    
    this.HTMLNode.style.width = "1px";
	this.controlWidth = 1;
    
    var addButton = $("<input type='button' class='button' value='Add'/>").addClass("ui-corner-all").addClass("ui-state-default");
    var removeButton = $("<input type='button' class='button' value='Remove'/>").addClass("ui-corner-all").addClass("ui-state-default");
    
    this.node.append(this.HTMLNode);
    if (!this.getModel().isReadOnly() && !this.getModel().getAttribute("fixedLengthProperty")) 
    {
        this.node.append(addButton);
        this.node.append(removeButton);
        this.node.css("margin-top", '-15px');
        var _this = this;
        $(addButton).click(function() 
        {
        	_this.actionName = "item-add";
        	_this.fireChangeListeners(null, {});
        });
        $(removeButton).click(function() 
        {
           	_this.actionName = "item-remove";
           	_this.fireChangeListeners(null, {});
        });
    }
    return this.node.get(0);
};

JSArrayControl.prototype.updateModel = function ()
{
    var property = this.getModel();
    if(this.actionName)
    {
    	//set action info to property descriptor
        property.getDescriptor().setValue("array-action", this.actionName);
    }
}