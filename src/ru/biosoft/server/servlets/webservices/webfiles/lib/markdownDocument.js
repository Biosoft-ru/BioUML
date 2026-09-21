function MarkdownDocument(completeName)
{
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId("markdown_"+rnd()+"/"+completeName);
    var _this = this;
    this.originalContent = "";
    this.markdownMode = true;
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var documentDiv = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('height','100%').css('position', 'relative').attr('doc', this);
        parent.append(documentDiv);
        
        this.toolbar = $('<div id="' + this.tabId + '_toolbar" class="fg-toolbar ui-widget-header ui-corner-all ui-helper-clearfix"></div>').height(25);
        this.markdownToolbar = $('<div class="fg-buttonset fg-buttonset-single ui-helper-clearfix" id="mdtb-'+this.tabId+'"></div>');
        var editBtn = $('<input type="button" value="Edit">').css('min-width','50px');
        editBtn.click(function() {
            _this.switchEditor();
            _this.textArea.val(_this.originalContent);
            if(!_this.editor)
            {
                _this.editor = CodeMirror.fromTextArea(_this.textArea.get(0), {
                    mode: "text/plain",
                    lineNumbers: true,
                    styleActiveLine: true,
                    styleSelectedText: true,
                    extraKeys: {"Ctrl-H": "replace", "Ctrl-S": function() {_this.save();}}
                  });
            }
            _this.editor.focus();
        });
        if(!getDataCollection(this.completeName).isMutable())
            editBtn.attr("disabled", true).addClass("ui-state-disabled");
        this.markdownToolbar.append(editBtn);
        this.toolbar.append(this.markdownToolbar);
        
        
        this.editorToolbar = $('<div class="fg-buttonset fg-buttonset-single ui-helper-clearfix" id="edtb-'+this.tabId+'"></div>');
        var saveBtn = $('<input type="button" value="Save">').css('min-width','50px');
        saveBtn.click(function() {
            
            _this.saveEdits();
            
        });
        var cancelBtn = $('<input type="button" value="Cancel">').css('min-width','50px');
        cancelBtn.click(function() {
            
            _this.cancelEdits();
            
        });
        this.editorToolbar.append(saveBtn);
        this.editorToolbar.append(cancelBtn);
        this.toolbar.append(this.editorToolbar);
        
        documentDiv.append(this.toolbar);
        
        
        
        var frameId = this.tabId+'_frame';
        var html = '<iframe src="markdown.html" id="'+frameId+'"/>'
        this.frame = $(html)
          .css('width', '100%').css('height','100%').css('border', 'none');
        
        documentDiv.append(this.frame);
        this.frame.on("load", function() {
          _this.loadContent();
          resizeDocumentsTabs();
        });
        
        
        this.editorDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        documentDiv.append(this.editorDiv);
                
        this.textArea = $('<textarea class="text_document"/>');
                
        documentDiv.resize(function()
        {
            _this.textArea.width(_this.editorDiv.width()-10).height(_this.editorDiv.height()-10);
        });
        this.editorDiv.append(this.textArea);
        
        _this.editorToolbar.hide();
        _this.editorDiv.hide();
        
    };

    this.loadContent = function()
    {
        var params = {
            "de": this.completeName    
        };
        queryBioUML("web/doc/getcontent", params, function(data)
        {
            _this.originalContent = data.values;
            _this.displayMarkdown(data.values);
            
        });
    };
    
    this.displayMarkdown = function()
    {
        var converter = new showdown.Converter();
        converter.setOption('tables', true);
        html = converter.makeHtml(_this.originalContent);
        _this.frame.contents().find('body').html(html);
    }

    this.close = function(callback)
    {
        if(callback) callback();
    };

    this.isChanged = function()
    {
    	return false;
    };
    
    this.saveEdits = function()
    {
        if(!_this.editor) return;
        _this.editor.save();
        var text = _this.textArea.val();
        queryBioUML("web/doc/savecontent", 
        {
            "de": _this.completeName,
            "newPath":_this.completeName,
            "content": text
        }, function(data)
        {
            _this.switchEditor();
            _this.originalContent = text;
            _this.displayMarkdown();
            
            
        }, function(data)
        {
            logger.error(data.message);
        });
};
    
    this.cancelEdits = function()
    {
        _this.switchEditor();
        _this.displayMarkdown();
    };
    
    this.switchEditor = function()
    {
        _this.markdownMode = !_this.markdownMode;
        if(_this.markdownMode)
        {
            _this.frame.show();
            _this.editorDiv.hide();
            _this.markdownToolbar.show();
            _this.editorToolbar.hide();
        }
        else
        {
            _this.frame.hide();
            _this.editorDiv.show();
            _this.markdownToolbar.hide();
            _this.editorToolbar.show();
        }    
    };

}
