/**
 * 
 * Open OASYS worlflow text files in docker process 
 * 
 */

function OasysDocument(completeName, displayNum, uniqueId, documentUrl )
{
    this.completeName = completeName;
    this.name =  getElementName(this.completeName);
    this.tabId = allocateDocumentId(this.completeName);

    var _this = this;
    this.type = "text";
    this.scrollPos = undefined;

    var displayNum = displayNum;
    var uniqueId = uniqueId;
    var documentUrl = documentUrl;

    function makeOasysIframeURL()
    {
        //var url = window.location.href.replace( "https:", "http:" );
        var url = window.location.href;
        if( url.lastIndexOf( ":" ) > 7 ) // port number specified
        {
            return url.substring( 0, url.lastIndexOf( ":" ) ) + ":71" + displayNum;
        }

        return url.substring( 0, url.indexOf( "/bioumlweb/" ) ) + ":71" + displayNum;
    }
   
    this.open = function(parent)
    {
        opennedDocuments[this.tabId] = this;
        var documentDiv = $('<div id="' + this.tabId + '"></div>').attr('doc', this)
            .css( 'padding', 0 )
            .css( 'position', 'relative' )
            .css( 'background', '#ffffff' )
            .css( 'background-image', 'url(images/orange-spinner.gif)' )
            .css( 'background-position', 'center' )
            .css( 'background-repeat', 'no-repeat' )
        ;

        parent.append(documentDiv);
        
        this.containerDiv = $('<div id="' + this.tabId + '_container" class="documentTab"></div>');
        var frameId = this.tabId+'_frame';
        var url = displayNum ? makeOasysIframeURL() : "";
        var frame = '<iframe src="' + url + '" id="'+frameId+'"/>'
        this.textContainer = $(frame).css('width','100%').css('height','100%').css('border','none').css('background','transparent');
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
        var myIframe = document.getElementById( frameId );
        setTimeout(function run() {
            bubbleIframeMouseMove(myIframe);
            i++;
            if( i < 600 )
                setTimeout(run, 100);
        }, 100);

        if( !displayNum )
        {
            queryBioUML("web/oasys/start",
            {
                de: this.completeName
            },
            function(data)
            {
                displayNum = data.values[ 0 ];
                uniqueId = data.values[ 1 ];
                documentUrl = data.values[ 2 ];
                var newurl = makeOasysIframeURL(); 
                console.log( "Opening OASYS at: " + newurl );
                documentDiv.css( 'background-image', 'none' )
                myIframe.src = newurl; 
            });
        }      
    };
        
    this.close = function()
    {
        queryBioUML("web/oasys/stop",
        {
            de: this.completeName,
            uniqueId: uniqueId
        });
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