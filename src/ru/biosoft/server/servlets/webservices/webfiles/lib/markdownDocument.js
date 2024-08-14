function MarkdownDocument(completeName)
{
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId("markdown_"+rnd()+"/"+completeName);
    var _this = this;
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var documentDiv = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('height','100%').css('position', 'relative').attr('doc', this);
        parent.append(documentDiv);
        
       
        var frameId = this.tabId+'_frame';
        var html = '<iframe src="markdown.html" id="'+frameId+'"/>'
        this.frame = $(html)
          .css('width', '100%').css('height','100%').css('border', 'none');
        
        documentDiv.append(this.frame);
        this.frame.on("load", function() {
          _this.loadContent();
          resizeDocumentsTabs();
        });
    };

    this.loadContent = function()
    {
        var params = {
            "de": this.completeName    
        };
        queryBioUML("web/doc/getcontent", params, function(data)
        {
            var converter = new showdown.Converter();
            converter.setOption('tables', true);
            html = converter.makeHtml(data.values);
            _this.frame.contents().find('body').html(html)
        });
    };

    this.close = function(callback)
    {
        if(callback) callback();
    };

    this.isChanged = function()
    {
    	return false;
    };

}
