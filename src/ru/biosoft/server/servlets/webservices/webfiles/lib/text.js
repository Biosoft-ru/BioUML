/* $Id: text.js,v 1.5 2013/08/13 05:48:19 lan Exp $ */
/**
 * Text document implementation
 * 
 * @author lan
 */
function TextDocument(completeName)
{
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.savedDocument = ""; 
    this.tabId = allocateDocumentId(completeName);
    var dc = getDataCollection(this.completeName);
    this.readOnly = !dc.isMutable();
    dc.addRemoveListener(this);
    
    var _this = this;
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var scriptDocument = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(scriptDocument);
        
        this.scriptContainerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        scriptDocument.append(this.scriptContainerDiv);
        
        this.textArea = $('<textarea class="text_document"/>');
        
        scriptDocument.resize(function()
        {
        	_this.textArea.width(_this.scriptContainerDiv.width()-10).height(_this.scriptContainerDiv.height()-10);
        });
        
        if(!getDataCollection(this.completeName).isMutable())
        {
        	this.textArea.attr("readonly", "readonly").css("background-color", "");
        }
        
        this.scriptContainerDiv.append(this.textArea);
        
        this.loadContent();
        
        //update document size
        resizeDocumentsTabs();
    };
    
    this.loadContent = function()
    {
        var params = {
            "de": this.completeName    
        };
        queryBioUML("web/doc/getcontent", params, function(data)
        {
            _this.savedDocument = data.values;
			_this.textArea.val(_this.savedDocument);
			if(!_this.textArea.attr("readonly"))
			{
	            _this.editor = CodeMirror.fromTextArea(_this.textArea.get(0), {
	                mode: "text/plain",
	                lineNumbers: true,
	                styleActiveLine: true,
	                styleSelectedText: true,
	                extraKeys: {"Ctrl-H": "replace", "Ctrl-S": function() {_this.save();}}
	              });
	            _this.editor.focus();
			}
        });
    };
    
    this.save = function(callback)
    {
    	this.saveAs(this.completeName, callback);
    };
    
    this.saveAs = function(newPath, callback)
    {
    	if(!_this.editor) return;
		_this.editor.save();
    	var text = _this.textArea.val();
        queryBioUML("web/doc/savecontent", 
        {
            "de": this.completeName,
            "newPath": newPath,
            "content": text
        }, function(data)
        {
        	if(newPath == _this.completeName)
        		_this.savedDocument = text;
        	if(callback) callback(data);
        }, function(data)
        {
        	if(callback) callback(data);
        });
    };
    
    this.close = function(callback)
    {
        if(callback) callback();
    };

    this.isChanged = function()
    {
    	return _this.savedDocument != _this.textArea.val();
    };

    this.dataCollectionRemoved = function()
    {
        closeDocument(this.tabId);
    };
}
