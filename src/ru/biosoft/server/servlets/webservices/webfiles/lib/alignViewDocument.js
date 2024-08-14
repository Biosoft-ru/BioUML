/**
 * 
 * Open fasta etc. files in AlignmentViewer2.0
 * 
 * @author zha
 */

function AlignmentViewerDocument(completeName)
{
    this.completeName = completeName;
    this.name = getAV2Name(getElementName(this.completeName));
    this.tabId = allocateDocumentId(getElementPath(completeName)+this.name);
    var _this = this;
    this.type = "text";
    this.scrollPos = undefined;

    function makeAv2IframeURL()
    {
        //var url = window.location.href.replace( "https:", "http:" );
        var url = window.location.href;

        return url.substring( 0, url.indexOf( "/bioumlweb/" ) ) + "/AV2/";
    }
    
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var documentDiv = $('<div id="' + this.tabId + '"></div>').css('padding', 0).css('position', 'relative').attr('doc', this);
        parent.append(documentDiv);
        
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        var frameId = this.tabId+'_frame';
        var url = makeAv2IframeURL() + "?alignment-url=";
        var fname = this.completeName;
        var lastSlash = fname.lastIndexOf( "/" );
        if( lastSlash >= 1 )
        {
            fname = fname.substring( 0, lastSlash ) + "/file_collection.files" + fname.substring( lastSlash );    
        }
        url += fname;
        var frame = '<iframe src="' + url + '" id="'+frameId+'"/>'
        this.textContainer = $(frame).css('width', '100%').css('height','100%').css('border', 'none');
        this.containerDiv.append(this.textContainer);
        documentDiv.append(this.containerDiv);
        resizeDocumentsTabs();
        
        var prevFunction;
        var bubbleIframeMouseMove = function( iframe ){
            
            if( prevFunction )
                iframe.contentWindow.removeEventListener('mousemove', prevFunction);
            prevFunction = function( event ) {
                var boundingClientRect = iframe.getBoundingClientRect();
                var evt = new CustomEvent( 'mousemove', {bubbles: true, cancelable: false});
                evt.clientX = event.clientX + boundingClientRect.left;
                evt.clientY = event.clientY + boundingClientRect.top;
                iframe.dispatchEvent( evt );
            };
            iframe.contentWindow.addEventListener('mousemove', prevFunction);
        };
        var i = 0;
        var myIframe = document.getElementById(frameId);
        setTimeout(function run() {
            bubbleIframeMouseMove(myIframe);
            i++;
            if( i < 600 )
                setTimeout(run, 100);
        }, 100);
    
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
        var _this = this;
    };
    
 
}
function getAV2Name( name )
{
    var ind = name.lastIndexOf('.');
    var nameAV2 = ( ind > 0 ? name.substring( 0, ind ) : name ) + " AV2";
    return nameAV2;
};
