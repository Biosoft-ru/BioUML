(function($){
/*
 * includeMany 1.2.2
 *
 * Copyright (c) 2009 Arash Karimzadeh (arashkarimzadeh.com)
 * Licensed under the MIT (MIT-LICENSE.txt)
 * http://www.opensource.org/licenses/mit-license.php
 *
 * Date: Dec 03 2009
 * 
 * Modified by Anna & Lan
 * There 
 */
    
var browserMsie = navigator.userAgent.match(/msie/i) || navigator.userAgent.match(/trident/i)

$.chainclude = function(urls,finaly){
	var onload = function(callback,data){
						if(typeof urls.length!='undefined'){
							if(urls.length==0)
								return typeof finaly === "function"
											?finaly(data)
											:null;
							urls.shift();
							return $.chainclude.load(urls,onload);
						}
						for(var item in urls){
							urls[item](data);
							delete urls[item];
							var count = 0;
							for(var i in urls)
								count++;
							return (count==0)
										?typeof finaly === "function"?finaly(data):null
										:$.chainclude.load(urls,onload);
						}
					}
	$.chainclude.load(urls,onload);
};
$.chainclude.load = function(urls,onload){
	if(typeof urls=='object' && typeof urls.length=='undefined')
		for(var item in urls)
			return $.include.load(item,onload,urls[item].callback);
	urls = $.makeArray(urls);
	$.include.load(urls[0],onload,null);
};
$.include = function(urls,finaly){
	var luid = $.include.luid++;
	var onload = function(callback,data){
						if($.isFunction(callback))
							callback(data);
						if(--$.include.counter[luid]==0&&$.isFunction(finaly))
							finaly();
					}
	if(typeof urls=='object' && typeof urls.length=='undefined'){
		$.include.counter[luid] = 0;
		for(var item in urls)
			$.include.counter[luid]++;
		return $.each(urls,function(url,callback){$.include.load(url,onload,callback);});
	}
	urls = $.makeArray(urls);
	$.include.counter[luid] = urls.length;
	$.each(urls,function(){$.include.load(this,onload,null);});
}
$.extend(
	$.include,
	{
		luid: 0,
		counter: [],
		load: function(url,onload,callback){
			url = url.toString();
			if($.include.exist(url))
				return onload(callback);
			if(/.css$/.test(url))
				$.include.loadCSS(url,onload,callback);
			else if(/.js$/.test(url))
				$.include.loadJS(url,onload,callback);
			else
				$.get(url,function(data){onload(callback,data)});
		},
		loadCSS: function(url,onload,callback){
			var css=document.createElement('link');
			css.setAttribute('type','text/css');
			css.setAttribute('rel','stylesheet');
			css.setAttribute('href',''+url);
			$('head').get(0).appendChild(css);
			browserMsie
				?$.include.IEonload(css,onload,callback)
				:onload(callback);//other browsers do not support it
		},
		loadJS: function(url,onload,callback){
			var js=document.createElement('script');
			js.setAttribute('type','text/javascript');
			js.setAttribute('src',''+url+'?'+appInfo.build);
			browserMsie
				?$.include.IEonload(js,onload,callback)
				:js.onload = function(){onload(callback)};
			$('head').get(0).appendChild(js);
		},
		IEonload: function(elm,onload,callback){
			elm.onreadystatechange = 
					function(){
						if(this.readyState=='loaded'||this.readyState=='complete')
							onload(callback);
					}
		},
		exist: function(url){
			var fresh = false;
            $('head script').each(
								function(){
                                    
									if(/.css$/.test(url)&&(url == this.href.substring(this.href.length - url.length)))
											return fresh=true;
									else if(/.js$/.test(url))
                                    {
                                        var src = this.src;
                                        if( new RegExp("\\?" + appInfo.build, "").test(src) )
                                            src = src.substring(0, src.length - appInfo.build.length - 1);
                                        if( url == src.substring(src.length - url.length))
                                            return fresh=true;
                                    }
								}
							);
			return fresh;
		}
	}
);
//
})(jQuery);
