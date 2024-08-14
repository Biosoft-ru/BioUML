/**
 * $Id: JSTextScriptEditBox.js,v 1.3 2013/08/13 07:53:47 lan Exp $
 *
 * Script editor box
 */

function JSTextScriptEditBox( propInfo, format )
{
    JSInput.call(this, propInfo, format); // call parent constructor
    this.objectClassName = "JSTextScriptEditBox";
};

JSTextScriptEditBox.prototype = new JSInput();
JSTextScriptEditBox.superClass = JSInput.prototype;

// override
JSTextScriptEditBox.prototype.createHTMLNode = function()
{
    var propName = this.propInfo.getName();
	
    var width = 220;
	this.node = $('<div/>');

    this.HTMLNode = document.createElement("input");
    this.HTMLNode.type = "text";
    this.HTMLNode.readOnly = true;
    this.HTMLNode.id = this.createNodeId(propName);
    if( this.changeListener != null )
    {
        addModificationListener(this.HTMLNode, this.changeListener);
    }
    $(this.HTMLNode).mousedown(function()
    {
        _this.onMouseDown(propName);
    });
    
    this.HTMLNode.style.width = width+"px";
	this.controlWidth = width;
    
    var detailsButton = $("<input type='button' class='button' value='...'/>").addClass("ui-corner-all").addClass("ui-state-default");
    
    this.node.append(this.HTMLNode);
    this.node.append(detailsButton);
    
    var _this = this;

    $(detailsButton).click(function() {_this.showScriptEditor();});
    return this.node.get(0);
};

JSTextScriptEditBox.prototype.getValue = function()
{
	return this.value;
};

JSTextScriptEditBox.prototype.setValue = function(val)
{
    var oldValue = this.value;
    this.value = val;
    fitElement(this.getHTMLNode(), val, true, this.controlWidth);
    this.fireChangeListeners(oldValue, val);
};

JSTextScriptEditBox.prototype.showScriptEditor = function()
{
    var _this = this;
    var textArea = $('<textarea/>');
    this.dialogContent = $('<div/>').append($('<div style="width: 580px; height: 300px;"/>').append(textArea))
    .append($("<div/>").text(resources.dlgEditJavaScriptHint));
    
    var save = function()
    {
        _this.setValue(_this.editor.getValue());
        _this.dialogContent.dialog("close");
        _this.dialogContent.remove();
    };
    
    this.dialogContent.dialog(
    {
        modal: true,
        autoOpen: true,
        width: 600,
        height: 400,
        title: resources.dlgEditJavaScriptTitle,
        open: function(event, ui) {
        	CodeMirror.commands.autocomplete = function(cm) {
        	    CodeMirror.showHint(cm, CodeMirror.hint.javascript);
        	};
        	textArea.val(_this.getValue());
            _this.editor = CodeMirror.fromTextArea(textArea.get(0), {
                mode: "javascript",
                lineNumbers: true,
                styleActiveLine: true,
                styleSelectedText: true,
                extraKeys: {"Ctrl-Space": "autocomplete", "Ctrl-H": "replace", "Ctrl-Enter": save}
              });
            _this.editor.focus();
        },
        buttons: 
        {
            "Cancel": function()
            {
                $(this).dialog("close");
                $(this).remove();
            },
            "Ok": save
        }
    });
};
