var ViewPane = $.inherit(
{
    __constructor: function(mainDiv, properties)
    {
        var _this = this;
        this.mainDiv = mainDiv;
        this.width = mainDiv.width();
        this.height = mainDiv.height();
        this.canvasScale = new Dimension(1, 1);
        this.viewPane = $('<div></div>').width(this.width).height(this.height).css('overflow', 'hidden').css('position', 'relative');
        this.canvasDiv = $('<div></div>').width(this.width).height(this.height).css('overflow', 'hidden');
        this.clickEnabled = false;
		this.initTiles(properties.tile);
        this.maxAllowedSize = 32767;
        this.maxAllowedArea = 50000000;
        this.tags = [];
        this.activeTags = {};
        this.canvasDiv.mousedown(function(event)
        {
            _this.clickEnabled = true;
        });
        this.viewPane.bind("contextmenu", function(event)
        {
        	event.preventDefault();
        	event.stopPropagation();
        	_this.onRightClick(event.pageX, event.pageY);
        });
        this.canvasDiv.click(function(event)
        {
            if(!_this.clickEnabled) return;
            var x = event.originalEvent.clientX - _this.viewPane.offset().left;
            var y = event.originalEvent.clientY - _this.viewPane.offset().top;
        	_this.onClick(x, y);
        });
        
        this.mainDiv.append(this.viewPane);
        this.viewPane.append(this.canvasDiv);
        
        //this.mainDiv.resize(function() {_this.resize();return true;});
        
        this.canvasDOM = $('<canvas/>').attr("class", "mapping").attr("width", this.width).attr("height", this.height).get(0); 
        this.canvasDiv.append(this.canvasDOM);
        if (window.G_vmlCanvasManager) 
        {
            this.canvasDOM = G_vmlCanvasManager.initElement(this.canvasDOM);
        }
        
        //highlighting layer
        this.highlightCanvasDOM = $('<canvas/>').css({position: "absolute", left: "0px", top: "0px"}).attr("width", this.width).attr("height", this.height).get(0);
        this.canvasDiv.append(this.highlightCanvasDOM);
        if (window.G_vmlCanvasManager) 
        {
            this.highlightCanvasDOM = G_vmlCanvasManager.initElement(this.highlightCanvasDOM);
        }
        
        //selector layer
        this.selectCanvasDOM = $('<canvas/>').css({position: "absolute", left: "0px", top: "0px"}).attr("width", this.width).attr("height", this.height).get(0);
        this.canvasDiv.append(this.selectCanvasDOM);
        if (window.G_vmlCanvasManager) 
        {
            this.selectCanvasDOM = G_vmlCanvasManager.initElement(this.selectCanvasDOM);
        }
        
        this.clipRectangle = new Rectangle(0, 0, 0, 0);
        
        var _this = this;
        var dragProperties = 
        {
            cursor: 'crosshair',
            start: function(event, ui)
            {
            
            },
            drag: function(event, ui)
            {
                if (_this.ondrag) 
                    _this.ondrag();
            },
            stop: function(event, ui)
            {
                _this.initScrollUpdate(_this.viewPane.offset().left - _this.canvasDiv.offset().left, _this.viewPane.offset().top - _this.canvasDiv.offset().top);
                if (_this.ondragstop) 
                    _this.ondragstop();
            }
        };
        if (properties) 
        {
            this.backgroundBrush = properties.backgroundBrush;
            if (properties['fullHeight'] != undefined) 
            {
                this.fullHeight = properties['fullHeight'];
            }
            else 
            {
                this.fullHeight = true;
            }
            if (properties['fullWidth'] != undefined) 
            {
                this.fullWidth = properties['fullWidth'];
            }
            else 
            {
                this.fullWidth = true;
            }
            if (properties['dragAxis']) 
            {
                dragProperties['axis'] = properties['dragAxis'];
            }
            if (properties['scrollAxis']) 
            {
                if (properties['scrollAxis'] == 'x') 
                {
                    this.xScroll = $('<div></div>').css('position', 'absolute').css('left', 5).css('top', this.height - 15).width(this.width - 15).height(10);
                    this.viewPane.append(this.xScroll);
                    this.xScroll.slider(
                    {
                        start: function(event, ui)
                        {
                            _this.currentXScrollPos = $(this).slider('option', 'value');
                        },
                        slide: function(event, ui)
                        {
                            var delta = $(this).slider('option', 'value') - _this.currentXScrollPos;
                            _this.canvasDiv.css('left', -delta);
                            if (_this.ondrag) 
                                _this.ondrag();
                        },
                        stop: function(event, ui)
                        {
                            var scrollX = $(this).slider('option', 'value');
                            _this.initScrollUpdate(scrollX, 0);
                            if (_this.ondragstop) 
                                _this.ondragstop();
                        }
                    });
                    this.xScroll.hide();
                }
                else 
                    if (properties['scrollAxis'] == 'y') 
                    {
                        this.yScroll = $('<div></div>').css('position', 'absolute').css('top', 5).css('left', this.width - 15).height(this.height - 15).width(10);
                        this.viewPane.append(this.yScroll);
                        this.yScroll.slider(
                        {
                            orientation: 'vertical',
                            start: function(event, ui)
                            {
                                _this.currentYScrollPos = $(this).slider('option', 'value');
                            },
                            slide: function(event, ui)
                            {
                                var delta = $(this).slider('option', 'value') - _this.currentYScrollPos;
                                _this.canvasDiv.css('top', delta);
                                if (_this.ondrag) 
                                    _this.ondrag();
                            },
                            stop: function(event, ui)
                            {
                                var scrollY = $(this).slider('option', 'value');
                                _this.initScrollUpdate(0, -scrollY);
                                if (_this.ondragstop) 
                                    _this.ondragstop();
                            }
                        });
                        this.yScroll.hide();
                    }
            }
        }
        if (dragProperties['axis'] != "none")
        {
            this.canvasDiv.draggable(dragProperties);
        }
    },
    scrollTo: function(x, y)
    {
        if (this.view) 
        {
            this.view.move(this.clipRectangle.x - x, this.clipRectangle.y - y);
        }
        this.clipRectangle.x = x;
        this.clipRectangle.y = y;
        this.canvasDiv.css('left', 0).css('top', 0);
    },
    
    setView: function(view, resetScroll)
    {
		if(view != this.view)
		{
			if(view.repaintRect)
			{
				this.invalidateTiles(new Rectangle(view.repaintRect.x*this.canvasScale.width,view.repaintRect.y*this.canvasScale.height,
					view.repaintRect.width*this.canvasScale.width,view.repaintRect.height*this.canvasScale.height));
				//delete view.repaintRect;
			} else
			{
				this.invalidateTiles();
			}
		}
        this.view = view;
		this.viewChanged = true;
        var rect = view.getBounds();
        rect.x *= this.canvasScale.width;
        rect.width *= this.canvasScale.width;
        rect.y *= this.canvasScale.height;
        rect.height *= this.canvasScale.height;
        if (resetScroll == undefined) 
            resetScroll = true;
        this.clipRectangle = resetScroll ? new Rectangle(0, 0, rect.x + rect.width, rect.y + rect.height) : 
        	new Rectangle(this.clipRectangle.x, this.clipRectangle.y, Math.max(0,rect.x + rect.width), Math.max(0,rect.y + rect.height));
        if (this.fullHeight) 
        {
            this.height = this.mainDiv.height();
        }
        else 
        {
            this.height = Math.max(rect.y + rect.height + 5, 16);
        }
        if(!this.fullWidth)
        {
            this.width = Math.max(rect.x + rect.width + 5, 16);
        }
		this.origWidth = this.width;
		this.origHeight = this.height;
        
		if(this.viewPane.height() != this.height || this.viewPane.width() != this.width)
		{
	        this.viewPane.height(this.height).width(this.width);//.resize();
		}
        this.canvasDiv.height(this.height).width(this.width);
		if (this.canvasDOM.getAttribute("width") != this.width || this.canvasDOM.getAttribute("height") != this.height) 
		{
			this.canvasDOM.setAttribute("width", this.width);
			this.canvasDOM.setAttribute("height", this.height);
            
            this.highlightCanvasDOM.setAttribute("width", this.width);
            this.highlightCanvasDOM.setAttribute("height", this.height);
            
            this.selectCanvasDOM.setAttribute("width", this.width);
            this.selectCanvasDOM.setAttribute("height", this.height);
			
            this.invalidateTiles();
		}
        
        if (this.xScroll) 
        {
            var viewWidth = this.width - 20;
            if (this.clipRectangle.width > viewWidth) 
            {
                this.xScroll.slider('option', 'min', 0);
                this.xScroll.slider('option', 'max', this.clipRectangle.width - viewWidth);
                this.xScroll.slider('option', 'value', 0);
                this.xScroll.show();
            }
            else 
            {
                this.xScroll.hide();
            }
        }
        if (this.yScroll) 
        {
            var viewHeight = this.height - 20;
            if (this.clipRectangle.height > viewHeight) 
            {
                this.yScroll.slider('option', 'min', viewHeight - this.clipRectangle.height);
                this.yScroll.slider('option', 'max', 0);
                this.yScroll.slider('option', 'value', 0);
                this.yScroll.show();
            }
            else 
            {
                this.yScroll.hide();
            }
        }
    },
    resize: function(repaint)
    {
        var newwidth = this.mainDiv.width();
        if (newwidth == 0 || newwidth == this.width) 
            return;
        this.width = newwidth;
        if (this.view) 
        {
            this.setView(this.view, false);
        }
        this.initScrollUpdate(0, 0);
    },
    initScrollUpdate: function(x, y)
    {
    	this.initUpdate(this.clipRectangle.x + x, this.clipRectangle.y + y);
    },
    
    initUpdate: function(x, y)
    {
        if (this.updateView) 
            this.updateView(x, y);
        this.scrollTo(x, y);
        this.repaint();
    },
    
    scrollToVisible: function()
    {
        if (this.view == undefined) 
            return;
        var p = this.view.getLeftTopPosition();
        if( p == undefined )
        	return;
        if(p.x > (this.mainDiv.scrollLeft() + this.mainDiv.width()/3)/this.canvasScale.width)
        {
        	this.mainDiv.scrollLeft(p.x*this.canvasScale.width - this.mainDiv.width()/4);
        }
        if(p.y > (this.mainDiv.scrollTop() + this.mainDiv.height()/3)/this.canvasScale.height)
        {
        	this.mainDiv.scrollTop(p.y*this.canvasScale.height - this.mainDiv.height()/4);
        }
    },
	
    ensureVisible: function(x, y)
    {
        if (this.view == undefined) 
            return;
        if(x > (this.mainDiv.scrollLeft() + this.mainDiv.width()*2/3)/this.canvasScale.width ||
                x < (this.mainDiv.scrollLeft() + this.mainDiv.width()/3)/this.canvasScale.width)
        {
            this.mainDiv.scrollLeft(x*this.canvasScale.width - this.mainDiv.width()/2);
        }
        if(y > (this.mainDiv.scrollTop() + this.mainDiv.height()*2/3)/this.canvasScale.height ||
                y < (this.mainDiv.scrollTop() + this.mainDiv.height()/3)/this.canvasScale.height)
        {
            this.mainDiv.scrollTop(y*this.canvasScale.height - this.mainDiv.height()/2);
        }
    },
    
	doRepaint: function(repaintRect)
	{
        if (this.view == undefined) 
            return;
			
		if(repaintRect == undefined)
		{
			repaintRect = new Rectangle(0,0, this.width, this.height);
		}
		// Should simply redraw everything in IE as it doesn't support clip()
		var clipRect = window.G_vmlCanvasManager?repaintRect:this.repaintTiles(repaintRect);
		if(clipRect.x == undefined) return; // Everything is already painted
        var ctx = this.getContext();
        if (ctx != null) 
        {
			var started = new Date();
			if (window.G_vmlCanvasManager) 
			{
				ctx.clearRect();
			}
			
			var scaledClipRect = new Rectangle(Math.floor(clipRect.x/this.canvasScale.width), Math.floor(clipRect.y/this.canvasScale.height),
				Math.ceil(clipRect.width/this.canvasScale.width+1), Math.ceil(clipRect.height/this.canvasScale.height+1));
            ctx.save();
			ctx.beginPath();
			ctx.rect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
			ctx.clip();
			new BoxView(undefined, this.backgroundBrush || new Brush(new Color(255,255,255)), clipRect).paint(ctx, clipRect);
            ctx.scale(this.canvasScale.width, this.canvasScale.height);
            this.view.paint(ctx, scaledClipRect, this.activeTags);
            ctx.restore();
        }
        else 
        {
            alert('Graphics is not supporting in your browser');
        }
	},
    
    repaint: function(repaintRect)
    {
		var _this = this;
		if (window.G_vmlCanvasManager && !this.viewChanged)
		{
			if(this.repaintTimer) clearTimeout(this.repaintTimer);
			this.repaintTimer = setTimeout(
					function() 
					{
						_this.view.onLoad(function()
						{
							_this.doRepaint(repaintRect);
						});
					}, 200);
		} else
		{
			if(this.view == undefined) return;
			this.view.onLoad(function()
			{
				_this.doRepaint(repaintRect);
				_this.viewChanged = false;
			});
		}
    },
    
    getContext: function()
    {
        var ctx = null;
        if (this.canvasDOM.getContext) 
        {
            ctx = this.canvasDOM.getContext('2d');
        }
        return ctx;
    },
    
    onRightClick: function(x, y)
    {
    	if(!this.tags || this.tags.length == 0) return;
    	var _this = this;
    	var menu = $("<div/>").addClass("genericComboData").css({
					position : "absolute",
					left : x,
					top : y,
					background : "white",
					fontFamily : "verdana",
					fontSize : "10pt",
					maxHeight : "200px",
					overflow : "auto"
				});
    	for(var i in this.tags)
    	{
    		var div = $("<div/>").text(this.tags[i]);
    		var input = $("<input type='checkbox'/>").data("value", this.tags[i]);
    		input.change(function()
    		{
    			var __this = $(this);
    			if(__this.attr("checked")) _this.activeTags[__this.data("value")] = 1;
    			else delete _this.activeTags[__this.data("value")];
    			_this.invalidateTiles();
    			_this.repaint();
    		});
    		if(this.activeTags[this.tags[i]]) input.attr("checked", "checked");
    		div.prepend(input);
    		menu.append(div);
    	}
    	$(document.body).append(menu);
    },
    
    onClick: function(x, y)
    {
        var viewX = x;
        var viewY = y;
        if (!this.view) 
            return;
        var activeView = this.view.getDeepestActive(new Point(viewX, viewY), null);
        if ((activeView != null) && (activeView != this.view)) 
        {
            this.selectHandler(activeView.model, activeView.getBounds());
        }
    },
    
    scale: function(scaleX, scaleY, relative)
    {
		if(relative == undefined) relative = true;
        if (this.view) 
        {
			if (relative) 
			{
				this.canvasScale.width *= scaleX;
				this.canvasScale.height *= scaleY;
				this.width *= scaleX;
				this.height *= scaleY;
			} else
			{
				this.canvasScale.width = scaleX;
				this.canvasScale.height = scaleY;
				this.width = this.origWidth*scaleX;
				this.height = this.origHeight*scaleY;
			}
            //this.resize(false);
			this.setView(this.view, false);
        }
    },
	
    selectHandler: function(elementId)
    {
    },
	
	initTiles: function(size)
	{
		this.tileSize = size;
		// If user don't want to use tiles, it equals to single big tile
		if(!this.tileSize) this.tileSize = 1e6;
		this.invalidateTiles();
	},
	
	invalidateTiles: function(rect)
	{
		if (rect == undefined) 
		{
			this.tiles = {};
			return;
		}
		for(var i=Math.floor(rect.x/this.tileSize); i<Math.ceil((rect.x+rect.width)/this.tileSize); i++)
			for(var j=Math.floor(rect.y/this.tileSize); j<Math.ceil((rect.y+rect.height)/this.tileSize); j++)
				delete this.tiles[i+","+j];
	},
	
	repaintTiles: function(rect)
	{
		var bounds = new Rectangle();
		for(var i=Math.floor(rect.x/this.tileSize); i<Math.ceil((rect.x+rect.width)/this.tileSize); i++)
			for(var j=Math.floor(rect.y/this.tileSize); j<Math.ceil((rect.y+rect.height)/this.tileSize); j++)
			{
				if(!this.tiles[i+","+j])
				{
					this.tiles[i+","+j] = 1;
					bounds.add(new Rectangle(i*this.tileSize, j*this.tileSize, this.tileSize, this.tileSize));
				}
			}
		if(bounds.x < 0) bounds.x = 0;
		if(bounds.y < 0) bounds.y = 0;
		if(bounds.width > this.width) bounds.width = this.width;
		if(bounds.height > this.height) bounds.height = this.height;
		return bounds;
	},
    
    highlightView: function(view)
    {
        if(this.highlightedView == view)
            return;
        if (this.highlightCanvasDOM.getContext) 
        {
            var ctx = this.highlightCanvasDOM.getContext('2d');
            ctx.clearRect(0,0,this.width,this.height);
            this.highlightedView = view;
            if (view != undefined) 
            {
                var clipRect = new Rectangle(0,0,this.width,this.height);
                var scaledClipRect = new Rectangle(0, 0, Math.ceil(this.width/this.canvasScale.width+1), Math.ceil(this.height/this.canvasScale.height+1));
                ctx.save();
			    ctx.beginPath();
                ctx.rect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
			    ctx.clip();
			    ctx.scale(this.canvasScale.width, this.canvasScale.height);
                highlightBrush = new Brush(new Color(151, 227, 255, 1));
                highlightPen = new Pen(new Color(151, 227, 255, 1), 3);
                view.paint(ctx, scaledClipRect);
                ctx.restore();
                highlightBrush = undefined;
                highlightPen = undefined;
            }
        }
    },
    
    selectView: function(view)
    {
        if (this.selectCanvasDOM.getContext) 
        {
            var ctx = this.selectCanvasDOM.getContext('2d');
            ctx.clearRect(0,0,this.width,this.height);
            this.selectedView = view;
            if (view != undefined) 
            {
                var clipRect = new Rectangle(0,0,this.width,this.height);
                var scaledClipRect = new Rectangle(0, 0, Math.ceil(this.width/this.canvasScale.width+1), Math.ceil(this.height/this.canvasScale.height+1));
                ctx.save();
			    ctx.beginPath();
                ctx.rect(clipRect.x, clipRect.y, clipRect.width, clipRect.height);
			    ctx.clip();
			    ctx.scale(this.canvasScale.width, this.canvasScale.height);
                highlightBrush = new Brush(new Color(242, 153, 153, 1));
                highlightPen = new Pen(new Color(242, 153, 153), 3);
                view.paint(ctx, scaledClipRect);
                ctx.restore();
                highlightBrush = undefined;
                highlightPen = undefined;
            }
        }
    },
    deselectView: function()
    {
    	if (this.selectCanvasDOM.getContext) 
        {
            var ctx = this.selectCanvasDOM.getContext('2d');
            ctx.clearRect(0,0,this.width,this.height);
            this.selectedView = undefined;
        }
    }
});

var RulerViewPane = $.inherit(ViewPane,
{
	__constructor: function(mainDiv, sequenceName, sequenceLength, rulerFont)
	{
		this.__base(mainDiv,
		{
            dragAxis: 'none',
            fullHeight: false
        });
		this.sequenceName = sequenceName;
		this.sequenceLength = sequenceLength;
		this.rulerFont = rulerFont;
	},
    updateView: function(x, y)
    {
        var _this = this;
        var blackpen = new Pen(new Color(0, 0, 0), 2);
        var cfont = this.rulerFont;
        var options = 
        {
            majorFont: cfont,
            minorFont: cfont,
            decDig: new Dimension(0, 0),
            axisPen: blackpen,
            ticksPen: blackpen,
            tickSize: new Dimension(10, 5),
            textOffset: new Dimension(5, 5)
        };
        var seqoptions = 
        {
            font: cfont,
            rulerOptions: options,
            type: this.scaleX >= cfont.font.getExtent("A", this.getContext())[0]?SequenceView.PT_BOTH:SequenceView.PT_RULER,
            density: this.scaleX
        };
        var from = Math.floor(x / this.scaleX);
        var origFrom = from;
        if (from < 0) 
            from = 0;
        if (from > this.sequenceLength) 
            from = this.sequenceLength;
        var to = Math.ceil((x + this.width) / this.scaleX);
        if (to < 0) 
            to = 0;
        if (to > this.sequenceLength) 
            to = this.sequenceLength;
        if (to - from < 1) 
            to = from + 1;
        if (seqoptions.type != SequenceView.PT_RULER) 
        {
            /*var seq = new SequenceView(new Sequence(
            {
                from: from,
                to: to,
                type: "test"                
            }), seqoptions, from, to, _this.getContext());
            if (seq.getNucleotideWidth() != _this.scaleX) 
            {
                _this.scaleX = seq.getNucleotideWidth();
            }
            var rulerView = new CompositeView();
            rulerView.add(seq, CompositeView.Y_BT);
            rulerView.move(from * _this.scaleX, 0);
            _this.viewPane.css('left',0).css('top',0);
            _this.setView(rulerView, true);
            _this.scrollTo(x, y);
            _this.doRepaint();*/
            queryService('bsa.service', 46, 
            {
                de: this.sequenceName,
                from: from,
                to: to
            }, function(data)
            {
                var seq = new SequenceView(new Sequence(
                {
                    from: from,
                    to: to,
                    type: "raw",
                    data: data.values.split(":")[2]
                }), seqoptions, from, to, _this.getContext());
                var rulerView = new CompositeView();
                rulerView.add(seq, CompositeView.Y_BT);
                rulerView.move(from * _this.scaleX, 0);
                _this.viewPane.css('left',0).css('top',0);
                _this.setView(rulerView, true);
                _this.scrollTo(x, y);
                _this.doRepaint();
            });
        }
        else 
        {
            var seq = new SequenceView(new Sequence(
            {
                from: 0,
                to: this.sequenceLength,
                type: "test"
            }), seqoptions, from, to, this.getContext());
            if (seq.getNucleotideWidth() != this.scaleX) 
            {
                this.scaleX = seq.getNucleotideWidth();
            }
            var rulerView = new CompositeView();
            rulerView.add(seq, CompositeView.Y_BT);
            rulerView.move(from * this.scaleX - this.clipRectangle.x, 0);
            this.setView(rulerView, false);
			this.viewPane.css('left',0).css('top',0);
        }
    }
});

var AjaxViewPane = $.inherit(ViewPane, 
{
    __constructor: function(mainDiv, properties)
    {
        this.__base(mainDiv, properties);
        this.URL = properties.URL;
        this.ajaxParam = properties.ajaxParam; // List of parameters for Ajax query
        this.ajaxParamFromX = properties.ajaxParamFromX; // Parameter name to pass FromX coordinate
        this.ajaxParamToX = properties.ajaxParamToX; // Parameter name to pass ToX coordinate
        this.ajaxParamFromY = properties.ajaxParamFromY; // Parameter name to pass FromY coordinate
        this.ajaxParamToY = properties.ajaxParamToY; // Parameter name to pass ToY coordinate
        this.scaleX = properties.scaleX || 1; // Scale coefficient for X coords
        this.scaleY = properties.scaleY || 1; // Scale coefficient for Y coords
    },
    updateView: function(x, y)
    {
        var xscaled = Math.floor(x / this.scaleX);
        var yscaled = Math.floor(y / this.scaleY);
        if (this.ajaxParamFromX != undefined) 
            this.ajaxParam[this.ajaxParamFromX] = xscaled;
        if (this.ajaxParamToX != undefined) 
            this.ajaxParam[this.ajaxParamToX] = Math.ceil((x + this.width) / this.scaleX);
        if (this.ajaxParamFromY != undefined) 
            this.ajaxParam[this.ajaxParamFromY] = yscaled;
        if (this.ajaxParamToY != undefined) 
            this.ajaxParam[this.ajaxParamToY] = Math.ceil((y + this.height) / this.scaleY);
        var _this = this;
        if (this.activeJSON) 
            this.activeJSON.abort();
        if (this.waiterDiv) 
        {
            this.waiterDiv.hide();
            if (this.trackTimer) 
                clearTimeout(this.trackTimer);
            this.trackTimer = setTimeout(function()
            {
                _this.waiterDiv.show();
            }, 200);
        }
        this.activeJSON = $.getJSON(this.URL, this.ajaxParam, function(data)
        {
            if (data.type == 0) 
            {
				if (_this.waiterDiv) 
                {
                    _this.waiterDiv.hide();
                    if (_this.trackTimer) 
                        clearTimeout(_this.trackTimer);
                }
                var jsonData = $.evalJSON(data.values);
                var trackView = CompositeView.createView(jsonData.view, _this.getContext());
                _this.tags = jsonData.tags || [];
                _this.activeTags = {};
                for(var i in _this.tags) _this.activeTags[_this.tags[i]] = 1;
                var view = new CompositeView();
                view.add(trackView, CompositeView.Y_BT, new Point(Math.floor(xscaled * _this.scaleX), Math.floor(yscaled * _this.scaleY)));
				_this.viewPane.css('left',0).css('top',0);
                _this.setView(view, true);
                _this.scrollTo(x, y);
                _this.doRepaint();
                
                //notify special view part about new tags
                var genomeBrowserViewPart = lookForViewPart("table.genomebrowser");
                if(genomeBrowserViewPart)
                	genomeBrowserViewPart.addTags(_this.tags);
		        var vp = getActiveViewPart();
		        if (vp && vp.showDelayed) 
		            vp.showDelayed(this);
            }
        });
    }
});
