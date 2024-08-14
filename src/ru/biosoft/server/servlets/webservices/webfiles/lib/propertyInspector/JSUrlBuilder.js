
function JSUrlBuilder( propInfo, format )
{
    JSControl.call(this, format); // call parent constructor
    this.objectClassName = "JSUrlBuilder";
    this.originalName = propInfo.getAttribute("originalName");
    this.value = "";
    this.propInfo = propInfo;
};

JSUrlBuilder.prototype = new JSControl();
JSUrlBuilder.superClass = JSControl.prototype;

JSUrlBuilder.prototype.createHTMLNode = function()
{
	var params = this.propInfo.getAttribute("parameters");
	for(var key in params)
	{
		if(params[key].indexOf("$baseurl$") >= 0)
		{
			params[key] = params[key].replace("$baseurl$", window.location.protocol+"//"+window.location.host+window.location.pathname+appInfo.serverPath);
		}
	}
    var url = this.propInfo.getValue()+"?"+toURI(params);
    return $("<a/>").text("Link").attr("target", "_blank").attr("href", url).get(0);
};