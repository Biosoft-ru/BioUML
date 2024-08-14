/**
 * 
 * Open video with partial streaming
 * 
 * @author anna
 */

function VideoDocument(completeName)
{
    this.completeName = completeName;
    this.name = getElementName(this.completeName);
    this.tabId = allocateDocumentId(getElementPath(completeName)+this.name);
    this.format = "mp4";
    this.videoId = this.tabId + "_video";
    this.width = 640;
    this.height = 480;
    var _this = this;
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var documentDiv = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(documentDiv);
        
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        this.containerDiv.html("Loading video file...");
        queryBioUML("web/video",
        {
            de: _this.completeName,
            action: "format"
        }, function(data)
        {
            _this.format = data.values.format;
            _this.description = data.values.description;
            if(data.values.width)
            {
                _this.width = data.values.width;
                _this.height = data.values.height;
            }
            _this.containerDiv.empty();
            _this.videoContainer = $('<video width="'+_this.width+'" height="'+_this.height+'" controls autoplay controlsList="nodownload" oncontextmenu="return false; id="'+_this.videoId+'">Your browser does not support the video tag.</video>');
            _this.sourceContainer = $('<source src="'+appInfo.serverPath+'web/video?'+toURI({de: _this.completeName, action: "stream"})+'" type="video/'+_this.format+'" ></source>');
            _this.videoContainer.append(_this.sourceContainer);
            _this.containerDiv.append(_this.videoContainer);
        
            if(_this.description)
            {
                _this.textContainer = $("<div/>").append($('<h2>'+_this.description+'</h2>'));
                _this.containerDiv.append(_this.textContainer);
            }
        
        }, function(data){
            _this.containerDiv.html(data.message);
            
        });
        documentDiv.append(this.containerDiv);
        resizeDocumentsTabs();
    
    };
    
    
    this.close = function()
    {
    };
    
    this.isChanged = function()
    {
        return false;
    };
    
    this.save = function(callback)
    {
    };
    
 
}
