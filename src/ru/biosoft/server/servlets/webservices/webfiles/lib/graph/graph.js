/**
 * Basic canvas output classes
 */
var highlightBrush = undefined;
var highlightPen = undefined;

var Color = $.inherit(
{
    /*
     * a is optional
     */
    __constructor: function(r, g, b, a)
    {
        if(a == undefined) a = 1;
        this.r = r;
        this.g = g;
        this.b = b;
        this.a = a;
    },
        
    toRGBA: function()
    {
        return "rgba(" + this.r + ", " + this.g + ", " + this.b + ", " + this.a + ")"; 
    }
},
{
    colorMap: {},
    fromJSON: function(json)
    {
        if(json == undefined) return undefined;
        var colorStr = json.join(";");
        if(!Color.colorMap[colorStr])
            Color.colorMap[colorStr] = new Color(json[0], json[1], json[2], json[3]/255.);
        return Color.colorMap[colorStr];
    }
});

var Font = $.inherit(
{
    __constructor: function(name, style, size)
    {
    	if(name == "SansSerif")
    		name = "Sans-serif";
    	else if(name == "Monospaced")
    		name = "Monospace";
        this.name = name;
        this.style = style;
        this.size = size==undefined?10:size;
    },
    toCSS: function()
    {
        var css = [];
        if(this.style & Font.BOLD) css.push("bold");
        if(this.style & Font.ITALIC) css.push("italic");
        css.push(this.size+"px");
        css.push(this.name);
        return css.join(" ");
    },
    /*
     * Similar to Java getStringBounds. Maybe interface should be unified in future
     */
    getExtent: function(text, ctx)
    {
        ctx.save();
        ctx.font = this.toCSS();
        var width = ctx.measureText == undefined ? text.length*this.size:ctx.measureText(text).width;
        ctx.restore();
        var height = this.size;
        return [width, height];
    },
    
    scale: function(scale)
    {
        this.size = this.size*scale;
    }
},
{
    PLAIN: 0,
    BOLD: 1,
    ITALIC: 2
});

var ColorFont = $.inherit(
{
    __constructor: function(font, color)
    {
        if(color == undefined) color = new Color(0,0,0);
        if(font == undefined) font = new Font("Courier", Font.PLAIN, 12);
        this.color = color;
        this.font = font;
    }
},
{
    fontMap: {},
    fromJSON: function (json)
    {
        if(json == undefined) return undefined;
        var fontStr = json.font.join(";")+";"+json.color.join(";");
        if(!ColorFont.fontMap[fontStr]) {
            var color = Color.fromJSON(json.color);
            var font;
            font = new Font(json.font[0], json.font[1], json.font[2]);
            ColorFont.fontMap[fontStr] = new ColorFont(font, color);
        }
        return ColorFont.fontMap[fontStr];
    }
});


var Pen = $.inherit(
{
    __constructor: function(color, width, dash, dashOffset)
    {
        this.color = color;
        this.width = width>=0?width:1;
        this.dash = dash;
        this.dashOffset = dashOffset;
    }
},
{
    penMap: {},
    fromJSON: function(json)
    {
        if(json == undefined) return undefined;
        var penStr = json.color.join(";")+json.width+"["+(json.dash?json.dash.join(";"):"")+"]"+(json.dashOffset||0);
        if(!Pen.penMap[penStr]) {
            var color = Color.fromJSON(json.color);
            if(color != undefined)
                Pen.penMap[penStr] = new Pen(color, json.width, json.dash, json.dashOffset);
        }
        return Pen.penMap[penStr];
    }
});
Pen.BLACK = new Pen(new Color(0,0,0));

var Brush = $.inherit(
{
    __constructor: function(color)
    {
        this.color = color;
        this.angle = 0;
        this.cos = 1;
        this.sin = 0;
    },
    setColor2: function(color2)
    {
    	this.color2 = color2;
    },
    setAngle: function(angle)
    {
    	this.angle = angle;
    	this.cos = Math.cos(angle);
    	this.sin = Math.sin(angle);
    }
},
{
    brushMap: {},
    fromJSON: function(json)
    {
        if(json == undefined) return undefined;
        var brushStr = json.color.join(";");
        if(json.color2 != undefined) brushStr+=";"+json.color2.join(";");
        if(json.angle != undefined) brushStr+=";"+json.angle;
        if(!Brush.brushMap[brushStr]) {
            var color = Color.fromJSON(json.color);
            if(color != undefined)
            {
                Brush.brushMap[brushStr] = new Brush(color);
                if(json.color2 != undefined)
                {
                	var color2 = Color.fromJSON(json.color2);
                	if(color2 != undefined) Brush.brushMap[brushStr].setColor2(color2);
                }
                if(json.angle != undefined)
                {
                	Brush.brushMap[brushStr].setAngle(json.angle)
                }
            }
        }
        return Brush.brushMap[brushStr];
    }
});

var Point = $.inherit(
{
    __constructor: function(x, y)
    {
        this.x = x;
        this.y = y;
    }
});

var Dimension = $.inherit(
{
    __constructor: function(width, height)
    {
        this.width = width;
        this.height = height;
    }
});

var Rectangle = $.inherit(
{
    __constructor: function(x, y, width, height)
    {
		if(width < 0)
		{
			x += width;
			width = -width;
		}
		if(height < 0)
		{
			y += height;
			height = -height;
		}
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    },
    toString: function()
    {
        return "("+this.x+", "+this.y+")-("+(this.x+this.width)+", "+(this.y+this.height)+")";
    },
    grow: function(dx, dy)
    {
        this.x -= dx;
        this.y -= dy;
        this.width += 2*dx;
        this.height += 2*dy;
    },
    add: function(p)
    {
        var points = [];
        if(p instanceof Point)
        {
            points.push(p);
        }
        if(p instanceof Rectangle)
        {
            points.push(new Point(p.x, p.y));
            points.push(new Point(p.x + p.width, p.y + p.height));
        }
        for(var i = 0; i < points.length; i++)
        {
            if(this.x == undefined || this.y == undefined)
            {
                this.x = points[i].x;
                this.y = points[i].y;
                this.width = 0;
                this.height = 0;
                continue;
            }
            if (points[i].x == undefined || points[i].y == undefined) 
            {
                continue;
            }
            if(points[i].x < this.x)
            {
                this.width += this.x - points[i].x; 
                this.x = points[i].x;
            }
            if(points[i].y < this.y)
            {
                this.height += this.y - points[i].y;
                this.y = points[i].y;
            }
            if(points[i].x > this.x + this.width) this.width = points[i].x - this.x;   
            if(points[i].y > this.y + this.height) this.height = points[i].y - this.y;   
        }
    },
    intersects: function(r)
    {
        /*
         * Simplification of Separating Axis Theorem applied to rectangles orthogonal to axes 
         */
        if(this.x+this.width < r.x) return false;
        if(this.x > r.x+r.width) return false;
        if(this.y+this.height < r.y) return false;
        if(this.y > r.y+r.height) return false;
        return true;
    },
    getCenterX: function(r)
    {
    	return this.x+this.width/2;
    },
    getCenterY: function(r)
    {
    	return this.y+this.height/2;
    },
    clone: function()
    {
        return new Rectangle(this.x, this.y, this.width, this.height);
    },
    contains: function(r)
    {
        /*
         * Entirely contains
         */
        return (r.x>=this.x && r.y>=this.y && r.x+r.width<=this.x+this.width && r.y+r.height<=this.y+this.height)&&
        !(r.x==this.x && r.y==this.y && r.width==this.width && r.height==this.height);
    }
},
{
    getBoundingRect: function(xpoints, ypoints)
    {
        var rect = new Rectangle();
        if(xpoints instanceof Array && ypoints instanceof Array)
            for(var i = 0; i < xpoints.length; i++) rect.add(new Point(xpoints[i], ypoints[i]));
        return rect;
    }
});

var Path = $.inherit(
{
    /*
     * Constructor syntax:
     * new Path(Array xpoints, Array ypoints[, Array pointTypes])
     * new Path(x1, y1, x2, y2, ...)
     */
    __constructor: function()
    {
        if((arguments[0] instanceof Array) && (arguments[1] instanceof Array))
        {
            this.xpoints = arguments[0];
            this.ypoints = arguments[1];
            if(arguments[2] instanceof Array) {
                this.pointTypes = arguments[2];
            } else {
                this.pointTypes = [];
                for(i=0; i<this.xpoints.length; i++)
                    this.pointTypes.push(0);
            }
        } else
        {
            this.xpoints = [];
            this.ypoints = [];
            this.pointTypes = [];
            for(i=0; i<arguments.length-1; i+=2)
            {
                this.xpoints.push(arguments[i]);
                this.ypoints.push(arguments[i+1]);
                this.pointTypes.push(0);
            }
        }
        this.npoints = this.xpoints.length;
    },
    getLeftTopPosition: function()
    {
    	var minSum = 1e10;
    	var bestPt = null;
    	for(var i=0; i<this.xpoints.length; i++)
    	{
    		if(this.xpoints[i]+this.ypoints[i] < minSum)
    		{
    			minSum = this.xpoints[i]+this.ypoints[i];
    			bestPt = new Point(this.xpoints[i], this.ypoints[i]);
    		}
    	}
    	return bestPt;
    }
});

var View = $.inherit(
{
    __constructor: function()
    {
        this.scaleX = 1.0;
        this.scaleY = 1.0;
        this.type = 0;
        this.model = null;
    },
    
    setLocation: function(x, y)
    {
        rect = this.getBounds();
        this.move(x - rect.x, y - rect.y);
    },
    
    move: function(x, y)
    {
    },
    
    /**
     * @returns {Point} point of the most left-top pixel where something is actually painted or null of nothing is painted in this view
     */
    getLeftTopPosition: function()
    {
    	var r = this.getBounds();
    	if(r.width == 0 || r.height == 0) return null;
    	return new Point(r.x, r.y);
    },
    
    scale: function(scaleX, scaleY)
    {
    },
    
    /**
     * Returns <code>true</code> if view is visible
     *
     * @returns {Boolean} <code>true</code> if view is visible
     */
    isVisible: function()
    {
        return ( this.type & View.HIDE ) == 0;
    },
    
    isLoaded: function()
    {
    	return true;
    },
    
    onLoad: function(callback)
    {
    	callback();
    },
    
    /**
     * Returns <code>true</code> if view is active
     *
     * @returns Boolean <code>true</code> if view is active
     */
    isActive: function()
    {
        return ( this.type & View.ACTIVE ) != 0;
    },

    /**
     * Returns <code>true</code> if view is selectable
     *
     * @returns {Boolean} <code>true</code> if view is selectable
     */
    isSelectable: function()
    {
        return ( this.type & View.SELECTABLE ) != 0;
    },

    /**
     * Sets visible state of view
     *
     * @param {Boolean} isVisible new value of visible flag
     */
    setVisible: function(isVisible)
    {
        if( !isVisible )
            this.type |= View.HIDE;
        else
            this.type &= ~View.HIDE;
    },

    paint: function(ctx, clipRect, tags)
    {
        if(this.isVisible())
            this.doPaint(ctx, clipRect, tags);
    },
    
    doPaint: function(ctx, clipRect, tags)
    {
    },
    
    strokeAndFill: function(ctx)
    {
		// Patched IE canvas supports stroke&fill as single command which significantly faster
		var mode = -1;
        var curBrush = this.brush != undefined ? highlightBrush != undefined ? highlightBrush : this.brush : undefined;
        if(curBrush != undefined)
        {
        	if(curBrush.color2 == undefined)
        		ctx.fillStyle = curBrush.color.toRGBA();
        	else
        	{
        		var proj = (Math.abs(this.getBounds().width*curBrush.sin)+Math.abs(this.getBounds().height*curBrush.cos))/2;
        		var sinDist = proj*curBrush.sin;
        		var cosDist = proj*curBrush.cos;
        		var gradient = ctx.createLinearGradient(this.getBounds().getCenterX()-sinDist,this.getBounds().getCenterY()-cosDist,this.getBounds().getCenterX()+sinDist,this.getBounds().getCenterY()+cosDist);
        		gradient.addColorStop(0, curBrush.color.toRGBA());
        		gradient.addColorStop(1, curBrush.color2.toRGBA()); 
        		ctx.fillStyle = gradient;
        	}
			if(window.G_vmlCanvasManager)
				mode = 1;
			else
            	ctx.fill();
        }
        var curPen = this.pen != undefined ? highlightPen != undefined ? highlightPen : this.pen : undefined;
        if(curPen && curPen.width <= 0)
        	curPen = undefined;
        if(curPen != undefined)
        {
            ctx.lineWidth = curPen.width;
            ctx.lineCap = "butt";
            ctx.lineJoin = this.lineJoin != undefined ? this.lineJoin : "round";
            if(ctx.setLineDash)
            {
            	ctx.setLineDash(curPen.dash || []);
            	ctx.lineDashOffset = curPen.dashOffset?curPen.dashOffset:0;
            } else
            {
            	// Mozilla-only support for dashes
            	ctx.mozDash = curPen.dash;
            	ctx.mozDashOffset = curPen.dashOffset?curPen.dashOffset:0;
            }
            ctx.strokeStyle = curPen.color.toRGBA();
			if(window.G_vmlCanvasManager)
				mode++;
			else
	            ctx.stroke();
        }
		if(window.G_vmlCanvasManager && mode>=0)
			ctx.stroke(mode);
    },
    
    /**
     * @returns {Rectangle}
     */
    getBounds: function()
    {
        return new Rectangle();
    },
    
    /**
     * @param {Rectangle} rect
     * @returns {Number}
     */
    getSelectionPriority: function(rect)
    {
        return 0;
    },
    
    /**
     * @param {Rectangle} r
     * @returns {Boolean}
     */
    intersects: function(r)
    {
        var bounds = this.getBounds();
        return bounds.intersects(r);
    }
},
{
    /**
     * Alignment modes.
     */
    LEFT: 0,
    RIGHT: 1,
    CENTER: 2,
    BOTTOM: 8,
    TOP: 16,
    BASELINE: 0,
    /**
     * Bit field of {@link #type}.It is set, if the view has visible state.
     */
    HIDE: 8,
    ACTIVE: 4,
    SELECTABLE: 16
});

var TextView = $.inherit(View,
{
    __constructor: polymorph(
            function(text, pt, alignment, colorFont, textScale, ctx)
            {
                this.__base();
                this.text       = text;
                this.alignment  = alignment;
                this.colorFont  = colorFont;
                this.textScale = textScale;
                var extent = this.colorFont.font.getExtent(text, ctx);
                this.rect = new Rectangle(pt.x, pt.y, extent[0]*textScale.width, extent[1]*textScale.height);
                // Translations are done on the server already
/*                if(this.alignment & View.RIGHT) this.rect.x -= extent[0];
                else if(this.alignment & View.CENTER) this.rect.x -= Math.ceil(extent[0]/2);*/
                if(this.alignment & View.BOTTOM) this.rect.y -= extent[1];
                else if(this.alignment & View.TOP) this.rect.y -= 0;
                else this.rect.y -= Math.ceil(0.8*extent[1]);    // For baseline a little bit dirty: consider revising
            },
            function(text, colorFont, ctx)
            {
                this.constructor(text, new Point(0,0), View.LEFT | View.BASELINE, colorFont, new Dimension(1,1), ctx);
            },
            function(text, pt, alignment, colorFont, ctx)
            {
                this.constructor(text, pt, alignment, colorFont, new Dimension(1,1), ctx);
            }
        ),
    move: function(x, y)
    {
        this.rect.x += x;
        this.rect.y += y;        
    },
    scale: function(sx, sy)
    {
        this.rect.x *= sx;
        this.rect.y *= sy;
        this.textScale.width *= sx;
        this.textScale.height *= sy;
        this.colorFont.font.scale(sx);
    },
    doPaint: function(ctx, clipRect)
    {
        ctx.save();
        ctx.translate(this.rect.x, this.rect.y);
        ctx.scale(this.textScale.width, this.textScale.height);
        ctx.translate(-this.rect.x, -this.rect.y);
        ctx.font = this.colorFont.font.toCSS();
        ctx.fillStyle = this.colorFont.color.toRGBA();
        ctx.textAlign = "left";
        ctx.textBaseline = "top";
        if(this.rect.width <2 || this.rect.height < 2)
            ctx.fillRect(this.rect.x, this.rect.y, this.rect.width, this.rect.height);
        else
            ctx.fillText(this.text, this.rect.x, this.rect.y);
        ctx.restore();
    },
    getBounds: function()
    {
        return this.rect.clone();
    }
},
{
    createView: function(json, ctx)
    {
        var font = ColorFont.fromJSON(json.font);
        txt = new TextView(json.text, new Point(json.x, json.y), json.alignment, font, new Dimension(json.scaleX ,json.scaleY), ctx);
        txt.type = parseInt(json.type);
        txt.model = json.model;
        txt.description = json.description;
        return txt;
    }
});

var ShapeView = $.inherit(View, 
{
    __constructor: function()
    {
        this.__base();
    }
});

var BoxView = $.inherit(ShapeView, 
{
    __constructor: polymorph(
            function(pen, brush, rectangle, arcWidth, arcHeight)
            {
                this.__base();
                this.pen = pen;
                this.brush = brush;
                this.arcWidth = arcWidth;
                this.arcHeight = arcHeight;
                this.rectangle = rectangle.clone();
                if(this.arcWidth > this.rectangle.width/2)
                    this.arcWidth = this.rectangle.width/2;
                if(this.arcHeight > this.rectangle.height/2)
                    this.arcHeight = this.rectangle.height/2;
            },
            function(pen, brush, rectangle)
            {
                this.constructor(pen, brush, rectangle, 0, 0);
            },
            function(pen, brush, x, y, w, h)
            {
                this.constructor(pen, brush, new Rectangle(x, y, w, h), 0, 0);
            },
            function(pen, brush, x, y, w, h, arcWidth, arcHeight)
            {
                this.constructor(pen, brush, new Rectangle(x, y, w, h), arcWidth, arcHeight);
            }
        ),
    paintRoundRect: function(ctx)
    {
        ctx.moveTo(this.rectangle.x, this.rectangle.y+this.arcHeight);
        ctx.quadraticCurveTo(this.rectangle.x, this.rectangle.y, this.rectangle.x+this.arcWidth, this.rectangle.y);
        ctx.lineTo(this.rectangle.x+this.rectangle.width-this.arcWidth, this.rectangle.y);
        ctx.quadraticCurveTo(this.rectangle.x+this.rectangle.width, this.rectangle.y, this.rectangle.x+this.rectangle.width, this.rectangle.y+this.arcHeight);
        ctx.lineTo(this.rectangle.x+this.rectangle.width, this.rectangle.y+this.rectangle.height-this.arcHeight);
        ctx.quadraticCurveTo(this.rectangle.x+this.rectangle.width, this.rectangle.y+this.rectangle.height, this.rectangle.x+this.rectangle.width-this.arcWidth, this.rectangle.y+this.rectangle.height);
        ctx.lineTo(this.rectangle.x+this.arcWidth, this.rectangle.y+this.rectangle.height);
        ctx.quadraticCurveTo(this.rectangle.x, this.rectangle.y+this.rectangle.height, this.rectangle.x, this.rectangle.y+this.rectangle.height-this.arcHeight);
        ctx.lineTo(this.rectangle.x, this.rectangle.y+this.arcHeight);
    },
    doPaint: function(ctx, clipRect)
    {
        ctx.beginPath();
        if(this.arcWidth <= 0 && this.arcHeight <= 0)
            ctx.rect(this.rectangle.x, this.rectangle.y, this.rectangle.width, this.rectangle.height);
        else
            this.paintRoundRect(ctx);
        this.strokeAndFill(ctx);
    },
    move: function(sx, sy)
    {
        this.rectangle.x+=sx;
        this.rectangle.y+=sy;
    },
    scale: function(sx, sy)
    {
        this.rectangle.x*=sx;
        this.rectangle.y*=sy;
        this.rectangle.width*=sx;
        this.rectangle.height*=sy;
    },
    resize: function(sx, sy)
    {
        this.rectangle.width+=sx;
        this.rectangle.height+=sy;
    },
    getBounds: function()
    {
        var r = this.rectangle.clone();
		if(this.pen != undefined && r.width > 0 && r.height > 0)
		{
			r.x -= this.pen.width/2;
			r.y -= this.pen.width/2;
			r.width += this.pen.width;
			r.height += this.pen.width;
		}
		return r;
    }
},
{
    createView: function(json, ctx)
    {
        var pen = Pen.fromJSON(json.pen);
        var brush = Brush.fromJSON(json.brush);
        var view = json.arcWidth == undefined ?
                new BoxView(pen, brush, json.x, json.y, json.width, json.height):
                new BoxView(pen, brush, json.x, json.y, json.width, json.height, json.arcWidth, json.arcHeight);
        view.type = parseInt(json.type);
        view.model = json.model;
        view.description = json.description;
        return view;
    }
});

var ImageView = $.inherit(ShapeView, 
{
    /*
     * image is URL to the image
     */
    __constructor: function(image, x, y, width, height)
    {
        this.__base();
        this.rectangle = new Rectangle(x, y, width, height);
        this.image = new Image();
        this.image.src = image;
        this.callbacks = [];
    	if(!this.image.complete && !this.image.loaded)
		{
            var _this = this;
            this.image.onload = function()
            {
                _this.image.loaded = true;
                for(var i=0; i<_this.callbacks.length; i++)
                	_this.callbacks[i]();
                _this.callbacks = [];
            };
		}
    },
    move: function(sx, sy)
    {
        this.rectangle.x+=sx;
        this.rectangle.y+=sy;
    },
    scale: function(sx, sy)
    {
        this.rectangle.x*=sx;
        this.rectangle.y*=sy;
        this.rectangle.width*=sx;
        this.rectangle.height*=sy;
    },
    isLoaded: function()
    {
    	return this.image.complete || this.image.loaded;
    },
    onLoad: function(callback)
    {
    	if(!this.image.complete && !this.image.loaded)
		{
    		this.callbacks.push(callback);
		} else callback();
    },
    doPaint: function(ctx, clipRect)
    {
        if(this.rectangle.width != undefined && this.rectangle.width >= 1 && this.rectangle.height != undefined && this.rectangle.height >= 1)
            ctx.drawImage(this.image, this.rectangle.x, this.rectangle.y, this.rectangle.width, this.rectangle.height);
        else
            ctx.drawImage(this.image, this.rectangle.x, this.rectangle.y);
    },
    getBounds: function()
    {
        return this.rectangle.clone();
    }
},
{
    createView: function(json, ctx)
    {
        var view = new ImageView(appInfo.serverPath+"web/img?id="+encodeURIComponent(json.path), json.x, json.y, json.width, json.height);
        view.type = parseInt(json.type);
        view.model = json.model;
        view.description = json.description;
        return view;
    }
});

var HtmlView = $.inherit(ImageView,
{
    __constructor: function(image, x, y, width, height)
    {
        this.__base(image, x, y, width, height);
    }
},
{
    createView: function(json, ctx)
    {
        var view = new HtmlView(appInfo.serverPath+"web/img?html=" + encodeURIComponent(json.text) + "&font=" + encodeURIComponent($.toJSON(json.font)) + "&w=" + json.width + "&h=" + json.height, json.x, json.y, json.width, json.height);
        view.type = parseInt(json.type);
        view.model = json.model;
        view.text = $("<div/>").html(json.text).text();
        view.description = json.description;
        return view;
    }
});


var EllipseView = $.inherit(ShapeView, 
{
    __constructor: function(pen, brush, xCenter, yCenter, width, height)
    {
        this.__base();
        this.pen = pen;
        this.brush = brush;
        this.xCenter = xCenter;
        this.yCenter = yCenter;
        this.width = width;
        this.height = height;
    },
    move: function(sx, sy)
    {
        this.xCenter+=sx;
        this.yCenter+=sy;
    },
    scale: function(sx, sy)
    {
        this.xCenter*=sx;
        this.yCenter*=sy;
        this.width*=sx;
        this.height*=sy;
    },
    ellipse: window.G_vmlCanvasManager?
            /*
             * Fallback implementation for IE as it cannot scale arc()
             */
            function(ctx, mX, mY, aWidth, aHeight){
                var hB = (aWidth / 2) * .5522848,
                    vB = (aHeight / 2) * .5522848,
                    eX = mX + aWidth / 2,
                    eY = mY + aHeight / 2,
                    aX = mX - aWidth / 2,
                    aY = mY - aHeight / 2;
                ctx.beginPath();
                ctx.moveTo(aX, mY);
                ctx.bezierCurveTo(aX, mY - vB, mX - hB, aY, mX, aY);
                ctx.bezierCurveTo(mX + hB, aY, eX, mY - vB, eX, mY);
                ctx.bezierCurveTo(eX, mY + vB, mX + hB, eY, mX, eY);
                ctx.bezierCurveTo(mX - hB, eY, aX, mY + vB, aX, mY);
                ctx.closePath();
            }:
            /*
             * Normal implementation
             */
            function(ctx, aX, aY, aWidth, aHeight){
                ctx.save();
                ctx.scale(1, this.height/this.width);
                ctx.beginPath();
                ctx.arc(this.xCenter, this.yCenter*this.width/this.height, this.width/2, 0, 2*Math.PI, false);
                ctx.closePath();
                ctx.restore();    // necessary because we want to scale the path only, without scaling the stroke
            },

    doPaint: function(ctx, clipRect)
    {
        if(this.height==0) return;
        this.ellipse(ctx, this.xCenter, this.yCenter, this.width, this.height);
        this.strokeAndFill(ctx);
    },

    getBounds: function()
    {
        return new Rectangle(Math.floor(this.xCenter-this.width/2), Math.floor(this.yCenter-this.height/2),
                this.width, this.height);
    }
},
{
    createView: function(json, ctx)
    {
        var pen = Pen.fromJSON(json.pen);
        var brush = Brush.fromJSON(json.brush);
        
        var view = new EllipseView(pen, brush, json.x, json.y, json.width, json.height);
        view.type = parseInt(json.type);
        view.model = json.model;
        view.description = json.description;
        return view;
    }
});

var GradientBorderedBoxView = $.inherit(ShapeView, 
        {
            __constructor: polymorph(
                    function(brush, rectangle, arcWidth, arcHeight, gradientRadius, side)
                    {
                        this.__base();
                        var pen = new Pen(new Color(0,0,0));
                        this.brush = brush;
                        this.arcWidth = arcWidth;
                        this.arcHeight = arcHeight;
                        this.rectangle = rectangle.clone();
                        if(side == undefined)
                            side = GradientBorderedBoxView.SIDE_BOTH;
                        this.side = side;
                        if(this.side == GradientBorderedBoxView.SIDE_LEFT || this.side == GradientBorderedBoxView.SIDE_RIGHT)
                        {
                            if(this.arcWidth > this.rectangle.width)
                                this.arcWidth = this.rectangle.width;
                        }
                        else if(this.side == GradientBorderedBoxView.SIDE_BOTH)
                        {
                            if(this.arcWidth > this.rectangle.width/2)
                                this.arcWidth = this.rectangle.width/2;
                        }
                        if(this.arcHeight > this.rectangle.height/2)
                            this.arcHeight = this.rectangle.height/2;
                        this.gradientRadius = gradientRadius;
                    },
                    function(brush, rectangle, gradientRadius, side)
                    {
                        this.constructor(brush, rectangle, 0, 0, gradientRadius, side);
                        //TODO: do we need this?
                    },
                    function(brush, x, y, w, h, gradientRadius, side)
                    {
                        this.constructor(brush, new Rectangle(x, y, w, h), 0, 0, gradientRadius, side);
                    },
                    function(brush, x, y, w, h, arcWidth, arcHeight, gradientRadius, side)
                    {
                        this.constructor(brush, new Rectangle(x, y, w, h), arcWidth, arcHeight, gradientRadius, side);
                    }
                ),
            doPaintSimpleRect: function(ctx)
            {
                //TODO
            },
            doPaintRoundRect: function(ctx)
            {
                var brushColor =this.brush.color 
                var color1=brushColor.toRGBA();
                var color2=new Color(brushColor.r, brushColor.g, brushColor.b, 0.0).toRGBA();
                var rad = (this.arcHeight+this.arcWidth)/2;
                var gradientRadius = this.gradientRadius;
                var addLeft = 0, addRight = 0;
                

                if(this.side & GradientBorderedBoxView.SIDE_LEFT) 
                {
                    //top left
                    ctx.beginPath();
                    ctx.moveTo(this.rectangle.x-gradientRadius, this.rectangle.y+this.arcHeight);
                    ctx.quadraticCurveTo(this.rectangle.x-gradientRadius, this.rectangle.y-gradientRadius, this.rectangle.x+this.arcWidth, this.rectangle.y-gradientRadius);
                    ctx.lineTo(this.rectangle.x+this.arcWidth, this.rectangle.y+this.arcHeight);
                    ctx.lineTo(this.rectangle.x-gradientRadius, this.rectangle.y+this.arcHeight);
                    ctx.closePath();
                    var grd=ctx.createRadialGradient(this.rectangle.x+rad,this.rectangle.y+rad,rad,this.rectangle.x+rad,this.rectangle.y+rad,rad+gradientRadius);
                    grd.addColorStop(0,color1);
                    grd.addColorStop(1,color2);
                    ctx.fillStyle = grd;
                    ctx.fill();
                    //bottom left
                    ctx.beginPath();
                    ctx.moveTo(this.rectangle.x-gradientRadius, this.rectangle.y+this.rectangle.height-this.arcHeight);
                    ctx.quadraticCurveTo(this.rectangle.x-gradientRadius, this.rectangle.y+this.rectangle.height+gradientRadius, this.rectangle.x+this.arcWidth, this.rectangle.y+this.rectangle.height+gradientRadius);
                    ctx.lineTo(this.rectangle.x+this.arcWidth, this.rectangle.y+this.rectangle.height-this.arcHeight);
                    ctx.lineTo(this.rectangle.x-gradientRadius, this.rectangle.y+this.rectangle.height-this.arcHeight);
                    ctx.closePath();
                    var grd=ctx.createRadialGradient(this.rectangle.x+rad, this.rectangle.y+this.rectangle.height-rad,rad,this.rectangle.x+rad, this.rectangle.y+this.rectangle.height-rad,rad+gradientRadius);
                    grd.addColorStop(0,color1);
                    grd.addColorStop(1,color2);
                    ctx.fillStyle = grd;
                    ctx.fill();
                    //left
                    var grd=ctx.createLinearGradient(this.rectangle.x+this.arcWidth, this.rectangle.y+this.arcHeight,this.rectangle.x-gradientRadius,this.rectangle.y+this.arcHeight);
                    grd.addColorStop(this.arcWidth/(this.arcWidth+gradientRadius),color1);
                    grd.addColorStop(1,color2);
                    ctx.fillStyle=grd;
                    ctx.fillRect(this.rectangle.x-gradientRadius, this.rectangle.y+this.arcHeight,gradientRadius+this.arcWidth,this.rectangle.height-2*this.arcHeight);
                }
                else
                {
                    addLeft = this.arcWidth;
                }   
                
                if(this.side & GradientBorderedBoxView.SIDE_RIGHT) 
                {
                    //top right
                    ctx.beginPath();
                    ctx.moveTo(this.rectangle.x+this.rectangle.width-this.arcWidth, this.rectangle.y-gradientRadius);
                    ctx.quadraticCurveTo(this.rectangle.x+this.rectangle.width+gradientRadius, this.rectangle.y-gradientRadius, this.rectangle.x+this.rectangle.width+gradientRadius, this.rectangle.y+this.arcHeight);
                    ctx.lineTo(this.rectangle.x+this.rectangle.width-this.arcWidth, this.rectangle.y+this.arcHeight);
                    ctx.lineTo(this.rectangle.x+this.rectangle.width-this.arcWidth, this.rectangle.y-gradientRadius);
                    ctx.closePath();
                    var grd=ctx.createRadialGradient(this.rectangle.x+this.rectangle.width-rad,this.rectangle.y+rad,rad,this.rectangle.x+this.rectangle.width-rad,this.rectangle.y+rad,rad+gradientRadius);
                    grd.addColorStop(0,color1);
                    grd.addColorStop(1,color2);
                    ctx.fillStyle = grd;
                    ctx.fill();
                    //right
                    var grd=ctx.createLinearGradient(this.rectangle.x+this.rectangle.width-this.arcWidth, this.rectangle.y+this.arcHeight,this.rectangle.x+this.rectangle.width + gradientRadius,this.rectangle.y+this.arcHeight);
                    grd.addColorStop(this.arcWidth/(this.arcWidth+gradientRadius),color1);
                    grd.addColorStop(1,color2);
                    ctx.fillStyle=grd;
                    ctx.fillRect(this.rectangle.x+this.rectangle.width-this.arcWidth, this.rectangle.y+this.arcHeight,gradientRadius+this.arcWidth,this.rectangle.height-2*this.arcHeight);
                    //bottom right
                    ctx.beginPath();
                    ctx.moveTo(this.rectangle.x+this.rectangle.width+gradientRadius, this.rectangle.y+this.rectangle.height-this.arcHeight);
                    ctx.quadraticCurveTo(this.rectangle.x+this.rectangle.width+gradientRadius, this.rectangle.y+this.rectangle.height+gradientRadius, this.rectangle.x+this.rectangle.width-this.arcWidth, this.rectangle.y+this.rectangle.height+gradientRadius);
                    ctx.lineTo(this.rectangle.x+this.rectangle.width-this.arcWidth, this.rectangle.y+this.rectangle.height-this.arcHeight);
                    ctx.lineTo(this.rectangle.x+this.rectangle.width+gradientRadius, this.rectangle.y+this.rectangle.height-this.arcHeight);
                    ctx.closePath();
                    var grd=ctx.createRadialGradient(this.rectangle.x+this.rectangle.width-rad, this.rectangle.y+this.rectangle.height-rad,rad,this.rectangle.x+this.rectangle.width-rad, this.rectangle.y+this.rectangle.height-rad,rad+gradientRadius);
                    grd.addColorStop(0,color1);
                    grd.addColorStop(1,color2);
                    ctx.fillStyle = grd;
                    ctx.fill();
                }
                else
                {
                    addRight = this.arcWidth;
                }
                
                // top
                var grd=ctx.createLinearGradient(this.rectangle.x+this.arcWidth, this.rectangle.y+this.arcHeight,this.rectangle.x+this.arcWidth,this.rectangle.y-gradientRadius);
                grd.addColorStop(this.arcHeight/(this.arcHeight+gradientRadius),color1);
                grd.addColorStop(1,color2);
                ctx.fillStyle=grd;
                ctx.fillRect(this.rectangle.x+this.arcWidth-addLeft, this.rectangle.y-gradientRadius,this.rectangle.width-2*this.arcWidth+addLeft+addRight,gradientRadius+this.arcHeight);
                //bottom
                var grd=ctx.createLinearGradient(this.rectangle.x, this.rectangle.y+this.rectangle.height-this.arcHeight,this.rectangle.x,this.rectangle.y+this.rectangle.height+gradientRadius);
                grd.addColorStop(this.arcHeight/(this.arcHeight+gradientRadius),color1);
                grd.addColorStop(1,color2);
                ctx.fillStyle=grd;
                ctx.fillRect(this.rectangle.x+this.arcWidth-addLeft, this.rectangle.y+this.rectangle.height-this.arcHeight,this.rectangle.width-2*this.arcWidth+addLeft+addRight,gradientRadius+this.arcHeight);
                //center
                ctx.beginPath();
                ctx.fillStyle = color1;
                ctx.fillRect(this.rectangle.x+this.arcWidth-addLeft, this.rectangle.y+this.arcHeight, this.rectangle.width-2*this.arcWidth+addLeft+addRight, this.rectangle.height-2*this.arcHeight);
                
            },
            doPaint: function(ctx, clipRect)
            {
                if(this.arcWidth <= 0 && this.arcHeight <= 0)
                    this.doPaintSimpleRect(ctx);
                else
                    this.doPaintRoundRect(ctx);
            },
            move: function(sx, sy)
            {
                this.rectangle.x+=sx;
                this.rectangle.y+=sy;
            },
            scale: function(sx, sy)
            {
                this.rectangle.x*=sx;
                this.rectangle.y*=sy;
                this.rectangle.width*=sx;
                this.rectangle.height*=sy;
            },
            resize: function(sx, sy)
            {
                this.rectangle.width+=sx;
                this.rectangle.height+=sy;
            },
            getBounds: function()
            {
                var r = this.rectangle.clone();
                if(this.gradientRadius > 0 && r.width > 0 && r.height > 0)
                {
                    r.x -= this.gradientRadius;
                    r.y -= this.gradientRadius;
                    r.width += 2*this.gradientRadius;
                    r.height += 2*this.gradientRadius;//TODO: take sides into account
                }
                return r;
            }
        },
        {
            SIDE_RIGHT: 1,
            SIDE_LEFT: 2,
            SIDE_BOTH: 3,
            createView: function(json, ctx)
            {
                var brush = Brush.fromJSON(json.brush);
                var view = json.arcWidth == undefined ?
                        new GradientBorderedBoxView(brush, json.x, json.y, json.width, json.height, json.gradientRadius):
                        new GradientBorderedBoxView(
                                brush, json.x, json.y, json.width, json.height, json.arcWidth, json.arcHeight, json.gradientRadius, json.side);
                view.type = parseInt(json.type);
                view.model = json.model;
                view.description = json.description;
                return view;
            }
        }
);

var LineView = $.inherit(View, 
{
    __constructor: function(pen, x1, y1, x2, y2)
    {
        this.__base();
        this.pen = pen;
        this.x1 = x1;
        this.x2 = x2;
        this.y1 = y1;
        this.y2 = y2;
    },
    move: function(sx, sy)
    {
        this.x1+=sx;
        this.y1+=sy;
        this.x2+=sx;
        this.y2+=sy;
    },
    scale: function(sx, sy)
    {
        this.x1*=sx;
        this.y1*=sy;
        this.x2*=sx;
        this.y2*=sy;
    },
    doPaint: function(ctx, clipRect)
    {
        ctx.beginPath();
        ctx.moveTo(this.x1, this.y1);
        if(this.x2 == this.x1 && this.y2==this.y1)
            ctx.lineTo(this.x2+0.1, this.y2);
        else
            ctx.lineTo(this.x2, this.y2);
        ctx.closePath();
        this.strokeAndFill(ctx);
    },
    getLeftTopPosition: function()
    {
    	if(this.x1+this.y1 < this.x2+this.y2)
    		return new Point(this.x1, this.y1);
    	return new Point(this.x2, this.y2);
    },
    
    getBounds: function()
    {
        var rect = new Rectangle(this.x1, this.y1, this.x2-this.x1+1, this.y2-this.y1+1);
        if(this.pen) rect.grow(Math.ceil(this.pen.width/2), Math.ceil(this.pen.width/2));
        return rect;
    },
    
    intersects: function(r)
    {
        if(this.x1 < r.x && this.x2 < r.x) return false;
        if(this.x1 > r.x+r.width && this.x2 > r.x+r.width) return false;
        if(this.y1 < r.y && this.y2 < r.y) return false;
        if(this.y1 > r.y+r.height && this.y2 > r.y+r.height) return false;
        if(Math.abs(this.x1 - this.x2) < CompositeView.DELTA)
        {
            if((this.y1-r.y)*(this.y2-r.y) <= 0) return true;
            if((this.y1-r.y-r.height)*(this.y2-r.y-r.height) <= 0) return true;
            return false;
        }
        else if(Math.abs(this.y1 - this.y2) < CompositeView.DELTA)
        {
            if((this.x1-r.x)*(this.x2-r.x) <= 0) return true;
            if((this.x1-r.x-r.width)*(this.x2-r.x-r.width) <= 0) return true;
            return false;
        }
        else if(Math.abs((this.y2-this.y1)/(this.x2 - this.x1)) > 1 )
        {
            var x_u = parseFloat((this.x2-this.x1)*(r.y - this.y1)/(this.y2-this.y1)+this.x1);
            if(x_u >= r.x && x_u <= r.x+r.width) return true;    
            var x_b = (this.x2-this.x1)*(r.y+r.height - this.y1)/(this.y2-this.y1)+this.x1;
            if(x_b >= r.x && x_b <= r.x+r.width) return true;
            return false;
        }
        else
        {
            var y_l = parseFloat((this.y2-this.y1)*(r.x - this.x1)/(this.x2-this.x1)+this.y1);
            if(y_l >= r.y && y_l <= r.y+r.height) return true;    
            var y_r = (this.y2-this.y1)*(r.x+r.width - this.x1)/(this.x2-this.x1)+this.y1;
            if(y_r >= r.y && y_r <= r.y+r.height) return true;
            return false;
        }
    }
},
{
    createView: function(json, ctx)
    {
        var pen = Pen.fromJSON(json.pen);
        var view = new LineView(pen, json.x1, json.y1, json.x2, json.y2);
        view.type = parseInt(json.type);
        view.model = json.model;
        view.description = json.description;
        return view;
    }
});

var PolylineView = $.inherit(View, 
{
    __constructor: function(pen, xpoints, ypoints)
    {
        this.__base();
        this.pen = pen;
        this.xpoints = [];
        this.ypoints = [];
        if(xpoints != undefined) this.xpoints = xpoints;
        if(ypoints != undefined) this.ypoints = ypoints;
        this.rect = Rectangle.getBoundingRect(this.xpoints, this.ypoints);
    },
    getLeftTopPosition: function()
    {
    	return Path.prototype.getLeftTopPosition.apply(this);
    },
    move: function(x, y)
    {
        this.translate(x, y);
    },
    translate: function(x, y)
    {
        for(var i = 0; i < this.xpoints.length; i++ )
        {
            this.xpoints[i] += x;
            this.ypoints[i] += y;
        }
        this.rect = Rectangle.getBoundingRect(this.xpoints, this.ypoints);
    },
    scale: function(sx, sy)
    {
        for(var i = 0; i < this.xpoints.length; i++ )
        {
            this.xpoints[i] *= sx;
            this.ypoints[i] *= sy;
        }
        this.rect = Rectangle.getBoundingRect(this.xpoints, this.ypoints);
    },
    
    doPaint: function(ctx, clipRect)
    {
        ctx.beginPath();
        ctx.moveTo(this.xpoints[0], this.ypoints[0]);
        for(var i = 1; i < this.xpoints.length; i++)
        {
            ctx.lineTo(this.xpoints[i], this.ypoints[i]);
        }
        this.strokeAndFill(ctx);
    },
    
    getBounds: function()
    {
        return this.rect.clone();
    },
    
    intersects: function(r)
    {
        for(var i = 0; i < this.xpoints.length - 1; i++ )
        {
            var view = new LineView(this.pen, this.xpoints[i], this.ypoints[i], this.xpoints[i+1], this.ypoints[i+1]);
            if( view.intersects(r) )
                return true;
        }
        return false;
    }
},
{
    createView: function(json, ctx)
    {
        var pen = Pen.fromJSON(json.pen);
        var view = new PolylineView(pen, json.xpoints, json.ypoints);
        view.type = parseInt(json.type);
        view.model = json.model;
        view.description = json.description;
        return view;
    }
});

var CompositeView = $.inherit(View, 
{
    __constructor: function()
    {
        this.__base();
        this.children = [];
        this.rect = new Rectangle(); 
    },
    
    move: function(x, y)
    {
        for(var i=0; i<this.children.length; i++)
        {
            this.children[i].move(x, y);
        }
        this.rect.x += x;
        this.rect.y += y;
    },
    
    isLoaded: function()
    {
    	for(var i=0; i<this.children.length; i++)
    		if(!this.children[i].isLoaded()) return false;
    	return true;
    },
    
    getLeftTopPosition: function()
    {
    	var minSum = 1e10;
    	var bestPt = null;
    	for(var i=0; i<this.children.length; i++)
    	{
    		var pt = this.children[i].getLeftTopPosition();
    		if(pt != null && pt.x+pt.y < minSum)
    		{
    			bestPt = pt;
    			minSum = pt.x+pt.y;
    		}
    	}
    	return bestPt;
    },
    
    onLoad: function(callback)
    {
    	var loaded = true;
    	for(var i=0; i<this.children.length; i++)
    	{
    		if(!this.children[i].isLoaded())
    		{
    			loaded = false;
    			var _this = this;
    			this.children[i].onLoad(function()
    			{
    				if(_this.isLoaded() && callback)
    				{
    					callback();
    					callback = undefined;
    				}
    			});
    		}
    	}
    	if(loaded) callback();
    },    
    
    scale: function(scaleX, scaleY)
    {
        for(var i=0; i<this.children.length; i++)
        {
            //this.children[i].move(0,0);
            this.children[i].scale(scaleX, scaleY);
        }
        this.rect.x *= scaleX;
        this.rect.y *= scaleY;
        this.rect.width *= scaleX;
        this.rect.height *= scaleY;
    },
    
    /**
     *  Adds new elements and arrange them relative the previous objects.
     *
     * @param v     specified View
     * @param mode  mode to arrange new element relative previous:
     *
     *  Abrevations:
     *  <pre>
     *  X_RL
     *  | ||
     *  | |--- boundary of new element
     *  | ---- boundary of minimal rectangle, described all
     *  |      previous elements
     *  ------ x or y coordinate
     *
     *  X - the x coordinate:
     *   L - left boundary of the object
     *   C - center of the object
     *   R - right boundary of the object
     *
     *  Y - the y coordinate:
     *   T - top boundary of the object
     *   C - center of the object
     *   B - bottom boundary of the object
     *
     *   Special:
     *  UN  - don't change the corresponding x or y boundory of new element.
     *  REL - If this bit is cleared, target coordinates is used from location of view.
     * </pre>
     * @param insets  Determines the insets of this view in relation to the side of rectangle.<br>
     *                When mode along X axis is X_RR or X_LR added view shifts to the left. Otherwise it shifts to the right for the rest of modes.<br>
     *                When mode along Y axis is Y_BB or Y_TB added view shifts to the up. Otherwise it shifts to the down for the rest of modes.
     */
    add: function(v, mode, insets)
    {
        if(mode != undefined)
        {
            var r = v.getBounds();
            var rect = this.getBounds();
            if(rect.width == undefined) {
                rect = new Rectangle(0,0,0,0);
            }
            var right = rect.x + rect.width; // right border of this object
            var bottom = rect.y + rect.height; // bottom border of this object
    
            if( insets == undefined )
                insets = new Point(0, 0);
    
            var x = 0;
            var y = 0;
            if( ( mode & CompositeView.REL ) == 0 )
            {
                x = r.x;
                y = r.y;
            }
            else
            {
                switch( mode & 0x0F )
                {
                    case CompositeView.X_RL:
                        x = right + insets.x;
                        break;
                    case CompositeView.X_RC:
                        x = right - r.width / 2 + insets.x;
                        break;
                    case CompositeView.X_RR:
                        x = right - r.width - insets.x;
                        break;
                    case CompositeView.X_LL:
                        x = rect.x + insets.x;
                        break;
                    case CompositeView.X_LC:
                        x = rect.x - r.width / 2 + insets.x;
                        break;
                    case CompositeView.X_LR:
                        x = rect.x - r.width - insets.x;
                        break;
                    case CompositeView.X_CC:
                        x = rect.x + rect.width / 2 - r.width / 2 + insets.x;
                        break;
                    case CompositeView.X_UN:
                        ;
                    default:
                        x = r.x + insets.x;
                        break;
                }
                switch( mode & 0x78 )
                {
                    case CompositeView.Y_BT:
                        y = bottom + insets.y;
                        break;
                    case CompositeView.Y_BC:
                        y = bottom - r.height / 2 + insets.y;
                        break;
                    case CompositeView.Y_BB:
                        y = bottom - r.height - insets.y;
                        break;
                    case CompositeView.Y_TT:
                        y = rect.y + insets.y;
                        break;
                    case CompositeView.Y_TC:
                        y = rect.y - r.height / 2 + insets.y;
                        break;
                    case CompositeView.Y_TB:
                        y = rect.y - r.height - insets.y;
                        break;
                    case CompositeView.Y_CC:
                        y = rect.y + rect.height / 2 - r.height / 2 + insets.y;
                        break;
                    case CompositeView.Y_UN:
                        ;
                    default:
                        y = r.y + insets.y;
                        break;
                }
            }
            v.setLocation(x, y);
        }
        
        this.children.push(v);
        this.rect.add(v.getBounds());
    },
    
    doPaint: function(ctx, clipRect, tags)
    {
    	if(this.tag != null && this.tag != "" && !tags[this.tag]) return;
        for(var i=0; i<this.children.length; i++)
        {
			if(this.children[i].getBounds().intersects(clipRect))
            	this.children[i].paint(ctx, clipRect, tags);
        }
    },
    
    getBounds: function()
    {
        return this.rect.clone();
    },
    
    getDeepestActive: function(pt, ignoreModels)
    {
        this.curRect = new Rectangle(pt.x - CompositeView.DELTA, pt.y - CompositeView.DELTA, 2 * CompositeView.DELTA, 2 * CompositeView.DELTA);
        var maxView = null;
        if( this.isSelectable() )
            maxView = this;

        return this.traceFor(this, ignoreModels, maxView);
    },
    
    indexView: function()
    {
        var index = {};
        
        function gatherFromString(selectable, txt) {
            var parts = txt.split(/\s+/);
            if(typeof(selectable.strings) !== 'object') {
                selectable.strings = {};
            }
            for(var i=0; i<parts.length; i++) {
                selectable.strings[parts[i].toLowerCase().replace(/^[^\w\u0400-\u04FF]+/, "").replace(/[^\w\u0400-\u04FF]+$/, "")] = 1;
            }
        }
        
        function gatherText(v, selectable) {
            if(typeof(v.model) === 'string') {
                var pos = v.model.lastIndexOf('/');
                gatherFromString(selectable, pos >= 0 ? v.model.substring(pos+1) : v.model);
            }
            if(typeof(v.text) === 'string') {
                gatherFromString(selectable, v.text);
            }
        }
        
        function collect(v) {
            if(typeof(v.strings) === 'object') {
                for(var str in v.strings) {
                    if(index[str] === undefined) {
                        index[str] = [v];
                    } else {
                        if(index[str].find(function(el, ind){ return el.model == v.model;}) == undefined)
                            index[str].push(v);
                    }
                }
            }
        }
        
        function visit(cv, selectable) {
            for(var i=0; i<cv.children.length; i++) {
                var v = cv.children[i];
                var childSelectable = v.isSelectable() ? v : selectable;
                if(childSelectable !== selectable) {
                    delete childSelectable.strings;
                }
                gatherText(v, childSelectable);
                if(v instanceof CompositeView) {
                    visit(v, childSelectable);
                }
                if(childSelectable !== selectable) {
                    collect(childSelectable);
                }
            }
        }
        
        visit(this, this);
        var tags = [];
        for(var key in index) {
            tags.push(key);
        }
        tags.sort();
        this.index = {};
        this.indexTags = tags;
        for(var i in tags) {
            this.index[tags[i]] = index[tags[i]];
        }
    },
    
    searchText: function(txt) {
        if(typeof(this.index) !== 'object')
            this.indexView();
        return this.index[txt];
    },
    
    availableTags: function() {
        if(typeof(this.index) !== 'object')
            this.indexView();
        return this.indexTags;
    },
    
    /**
     * Search of view by given model
     */
    findViewByModel: function(model)
    {
    	if(model == undefined) return undefined;
        for(var i=0; i<this.children.length; i++)
        {
        	if(this.children[i].model == model) return this.children[i];
        }
        for(var i=0; i<this.children.length; i++)
        {
        	if(this.children[i] instanceof CompositeView)
        	{
        		var res = this.children[i].findViewByModel(model);
        		if(res != undefined) return res;
        	}
        }
        return undefined;
    },
    
    traceFor: function(cv, ignoreModels, maxView)
    {
        var selectedViews = [];
        for( var i = cv.children.length - 1; i >= 0; i-- )
        {
            var v = cv.children[i];
            var curMaxView = maxView;

            if( v.isSelectable() )
            {
                var bounds = v.getBounds();
                if( v.intersects(this.curRect) && ( maxView == null || maxView.getBounds().contains(bounds) ))
                {
                    var setUp = true;
                    if( ignoreModels != null )
                    {
                        for( var k = 0; k < ignoreModels.length; k++ )
                        {
                            if( ignoreModels[k] == v.model )
                            {
                                setUp = false;
                                break;
                            }
                        }
                    }

                    if( setUp )
                    {
                        curMaxView = v;
                    }
                }
            }

            if( v instanceof CompositeView )
            {
                var childView = this.traceFor(v, ignoreModels, curMaxView);
                if( childView != null )
                {
                    curMaxView = childView;
                }
            }

            if( curMaxView != null && curMaxView != maxView )
            {
                selectedViews.push(curMaxView);
            }
        }

        if( selectedViews.length == 0 )
        {
            return maxView;
        }
        else
        {
            var result = selectedViews[0];
            var maxPriority = result.getSelectionPriority(this.curRect);
            var bounds = result.getBounds();
            var record = bounds.width * bounds.height;
            for( var i = 1; i < selectedViews.length; i++ )
            {
                var v = selectedViews[i];
                var priority = v.getSelectionPriority(this.curRect);
                bounds = v.getBounds();
                if( priority > maxPriority )
                {
                    maxPriority = priority;
                    record = bounds.width * bounds.height;
                    result = v;
                }
                else if( priority == maxPriority )
                {
                    if( ( bounds.width * bounds.height ) < record )
                    {
                        record = bounds.width * bounds.height;
                        result = v;
                    }
                }
            }
            return result;
        }
    },
    
    intersects: function(r)
    {
        for(var i=0; i<this.children.length; i++)
        {
        	if(this.children[i].intersects(r) ) return true;
        }
        return false;
    }
},
{
    /**  If this bit is cleared,then coordinates are used directly from x,y coordinates of view location */
    REL: 0x08,
    /** Arrange mode, X coordinate is get from x coordinate of view location */
    X_UN: 0x00 + 0x08,
    /**  Arrange mode, view is arranged by left side to the right of previous rectangle*/
    X_RL: 0x01 + 0x08,
    /**  Arrange mode, view is arranged by center along X axis to the right of previous rectangle*/
    X_RC: 0x02 + 0x08,
    /**  Arrange mode, view is arranged by right side to the right side of previous rectangle*/
    X_RR: 0x03 + 0x08,
    /**  Arrange mode, view is arranged by left side to the left side of previous rectangle*/
    X_LL: 0x04 + 0x08,
    /**  Arrange mode, view is arranged by center along X axis to the left of previous rectangle*/
    X_LC: 0x05 + 0x08,
    /**  Arrange mode, view is arranged by right side to the left side of previous rectangle*/
    X_LR: 0x06 + 0x08,
    /**  Arrange mode, view is arranged by center along X axis to the center along X axis of previous rectangle*/
    X_CC: 0x07 + 0x08,
    /** Arrange mode, Y coordinate is get from y coordinate of view location */
    Y_UN: 0x00 + 0x08,
    /**  Arrange mode, view is arranged by top side to the top of previous rectangle*/
    Y_TT: 0x10 + 0x08,
    /**  Arrange mode, view is arranged by center along Y axis to the top of previous rectangle*/
    Y_TC: 0x20 + 0x08,
    /**  Arrange mode, view is arranged by bottom side  to the top of previous rectangle*/
    Y_TB: 0x30 + 0x08,
    /**  Arrange mode, view is arranged by top side to the bottom of previous rectangle*/
    Y_BT: 0x40 + 0x08,
    /**  Arrange mode, view is arranged by center along Y axis to the bottom of previous rectangle*/
    Y_BC: 0x50 + 0x08,
    /**  Arrange mode, view is arranged by bottom side to the bottom of previous rectangle*/
    Y_BB: 0x60 + 0x08,
    /**  Arrange mode, view is arranged by center along Y axis to the  center along Y axis of previous rectangle*/
    Y_CC: 0x70 + 0x08,
    
    DELTA: 3,
    
    createView: function(json, ctx, oldView)
    {
        if (oldView && json['class'] == "DummyView")
        {
            oldView.repaintRect = new Rectangle();
            return oldView;
        }
        var view = new CompositeView();
        view.type = parseInt(json.type);
        view.model = json.model;
        view.description = json.description;
        view.tag = json.tag;
        view.repaintRect = new Rectangle();
        var oldModels = {};
        var addRedraw = {};
        if(oldView && oldView.children)
        {
            for (var j in oldView.children)
            {
                var oldChild = oldView.children[j];
                if(oldChild.model != "__extents__")
                    addRedraw[j] = oldChild;
                if(oldChild.model != undefined)
                    oldModels[oldChild.model] = j;
            }
        }
            
        for (var i in json.children) 
        {
            var child = json.children[i];
            var childClass = window[child['class']];
            var model = child['model'];
            var oldChild2;
            if (model != undefined && oldModels[model] != undefined) 
            {
                oldChild2 = oldView.children[oldModels[model]];
                delete addRedraw[oldModels[model]];
            }
            var childView;
            if (child['class'] == "DummyView") 
                childView = oldChild2;
            else 
                if (childClass && childClass.createView) 
                {
                    childView = childClass.createView(child, ctx, oldChild2);
                    if (childView != undefined) 
                    {
                        if (childView.repaintRect == undefined) 
                        {
                            view.repaintRect.add(childView.getBounds());
                            if(oldChild2 != undefined)
                                view.repaintRect.add(oldChild2.getBounds());
                        }
                        else 
                            view.repaintRect.add(childView.repaintRect);
                    }
                }
            if (childView != undefined) 
                view.add(childView);
        }
        for(var i in addRedraw)
        {
            view.repaintRect.add(addRedraw[i].getBounds());
        }
        delete view.index;
        return view;
    }
});

var ComplexTextView = $.inherit(CompositeView,
{
    __constructor: function()
    {
        this.__base();
    }
},
{
    createView: function(json, ctx)
    {
        return CompositeView.createView(json,ctx);
    }
});

var BoxText = $.inherit(CompositeView,
{
    __constructor: function()
    {
        this.__base();
    }
},
{
    createView: function(json, ctx)
    {
        return CompositeView.createView(json,ctx);
    }
});

var PolygonView = $.inherit(ShapeView, 
{
    __constructor: function(pen, brush, xpoints, ypoints)
    {
        this.__base();
        this.pen = pen;
        this.brush = brush;
        this.xpoints = [];
        this.ypoints = [];
        if(xpoints != undefined) this.xpoints = xpoints;
        if(ypoints != undefined) this.ypoints = ypoints;
        this.rect = Rectangle.getBoundingRect(this.xpoints, this.ypoints);
    },
    getLeftTopPosition: function()
    {
    	return Path.prototype.getLeftTopPosition.apply(this);
    },    
    addPoint: function(x, y)
    {
        this.xpoints.push(x);
        this.ypoints.push(y);
        this.rect.add(new Point(x, y));
    },
    
    rotate: function(alpha)
    {
        var cosa = Math.cos(alpha);
        var sina = Math.sin(alpha);
        for(var i = 0; i < this.xpoints.length; i++ )
        {
            var x = this.xpoints[i];
            var y = this.ypoints[i];
            this.xpoints[i] = Math.floor(x*cosa - y*sina + 0.5);
            this.ypoints[i] = Math.floor(x*sina + y*cosa + 0.5);
        }
        this.rect = Rectangle.getBoundingRect(this.xpoints, this.ypoints);
    },
    move: function(x, y)
    {
        this.translate(x, y);
    },
    translate: function(x, y)
    {
        for(var i = 0; i < this.xpoints.length; i++ )
        {
            this.xpoints[i] += x;
            this.ypoints[i] += y;
        }
        this.rect = Rectangle.getBoundingRect(this.xpoints, this.ypoints);
    },
    scale: function(sx, sy)
    {
        for(var i = 0; i < this.xpoints.length; i++ )
        {
            this.xpoints[i] *= sx;
            this.ypoints[i] *= sy;
        }
        this.rect = Rectangle.getBoundingRect(this.xpoints, this.ypoints);
    },
    
    doPaint: function(ctx, clipRect)
    {
        ctx.beginPath();
        ctx.moveTo(this.xpoints[0], this.ypoints[0]);
        for(var i = 1; i < this.xpoints.length; i++)
        {
            ctx.lineTo(this.xpoints[i], this.ypoints[i]);
        }
        ctx.closePath();
        this.strokeAndFill(ctx);
    },
    
    getBounds: function()
    {
        return this.rect.clone();
    }
},
{
    createView: function(json, ctx)
    {
        var pen = Pen.fromJSON(json.pen);
        var brush = Brush.fromJSON(json.brush);
        var view = new PolygonView(pen, brush, json.xpoints, json.ypoints);
        view.type = parseInt(json.type);
        view.model = json.model;
        view.description = json.description;
        return view;
    }
});

var FigureView = $.inherit(ShapeView, 
{
    __constructor: function(pen, brush, xpoints, ypoints, pointTypes)
    {
        this.__base();
        this.pen = pen;
        this.brush = brush;
        this.xpoints = [];
        this.ypoints = [];
        this.pointTypes = [];
        if(xpoints != undefined) this.xpoints = xpoints;
        if(ypoints != undefined) this.ypoints = ypoints;
        if(pointTypes != undefined) this.pointTypes = pointTypes;
        this.rect = Rectangle.getBoundingRect(this.xpoints, this.ypoints);
    },
    move: function(x, y)
    {
        this.translate(x, y);
    },
    translate: function(x, y)
    {
        for(var i = 0; i < this.xpoints.length; i++ )
        {
            this.xpoints[i] += x;
            this.ypoints[i] += y;
        }
        this.rect = Rectangle.getBoundingRect(this.xpoints, this.ypoints);
    },
    scale: function(sx, sy)
    {
        for(var i = 0; i < this.xpoints.length; i++ )
        {
            this.xpoints[i] *= sx;
            this.ypoints[i] *= sy;
        }
        this.rect = Rectangle.getBoundingRect(this.xpoints, this.ypoints);
    },
    
    doPaint: function(ctx, clipRect)
    {
        ctx.beginPath();
        ctx.moveTo(this.xpoints[0], this.ypoints[0]);
        for(var i = 1; i < this.xpoints.length; i++)
        {
            if( this.pointTypes[i] == 1 && i < this.xpoints.length - 1 )
            {
                ctx.quadraticCurveTo(this.xpoints[i], this.ypoints[i], this.xpoints[i + 1], this.ypoints[i + 1]);
                i += 1;
            }
            if( this.pointTypes[i] == 2 && i < this.xpoints.length - 2 )
            {
                ctx.bezierCurveTo(this.xpoints[i], this.ypoints[i], this.xpoints[i + 1], this.ypoints[i + 1], this.xpoints[i + 2], this.ypoints[i + 2]);
                i += 2;
            }
            else
            {
                ctx.lineTo(this.xpoints[i], this.ypoints[i]);
            }
        }
        ctx.closePath();
        this.strokeAndFill(ctx);
    },
    
    getBounds: function()
    {
        return this.rect.clone();
    }
},
{
    createView: function(json, ctx)
    {
        var pen = Pen.fromJSON(json.pen);
        var brush = Pen.fromJSON(json.brush);
        var view = new FigureView(pen, brush, json.xpoints, json.ypoints, json.pointtypes);
        view.type = parseInt(json.type);
        view.model = json.model;
        view.description = json.description;
        return view;
    }
});

var PathView = $.inherit(ShapeView, 
{
    __constructor: function(pen, path)
    {
        this.__base();
        this.pen = pen;
        this.path = path;
        this.rect = Rectangle.getBoundingRect(this.path.xpoints, this.path.ypoints);
        this.calcIntersectionPath();
    },
    move: function(x, y)
    {
        this.translate(x, y);
    },
    translate: function(x, y)
    {
        for(var i = 0; i < this.path.xpoints.length; i++ )
        {
            this.path.xpoints[i] += x;
            this.path.ypoints[i] += y;
        }
        this.rect = Rectangle.getBoundingRect(this.path.xpoints, this.path.ypoints);
        this.calcIntersectionPath();
    },
    scale: function(sx, sy)
    {
        for(var i = 0; i < this.path.xpoints.length; i++ )
        {
            this.path.xpoints[i] *= sx;
            this.path.ypoints[i] *= sy;
        }
        this.rect = Rectangle.getBoundingRect(this.path.xpoints, this.path.ypoints);
        this.calcIntersectionPath();
    },
    getLeftTopPosition: function()
    {
    	return this.path.getLeftTopPosition();
    },
    calcIntersectionPath: function()
    {
    	this.intersectionPath = [];
    	this.intersectionPath.xpoints = this.getIntersectionPathPoints(this.path.xpoints);
    	this.intersectionPath.ypoints = this.getIntersectionPathPoints(this.path.ypoints);
    },
    getIntersectionPathPoints: function(points)
    {
    	var j = 0;
    	var newPoints = [];
    	for(var i = 0; i < points.length; i++ )
        {
    		if( this.path.pointTypes[i] == 1 && i < points.length - 1 && i>0 )
            {
    			for(var t = 1; t < 6; t++)
    			{
    				newPoints[j++] = this.getBezierPoint(t/6, points[i-1], points[i], points[i+1]);
    			}
    			newPoints[j++] = points[i+1];
                i += 1;
            }
            if( this.path.pointTypes[i] == 2 && i < this.path.xpoints.length - 2 && i>0)
            {
            	for(var t = 1; t < 6; t++)
    			{
    				newPoints[j++] = this.getBezierPoint(t/6, points[i-1], points[i], points[i+1], points[i+2]);
    			}
            	newPoints[j++] = points[i+2];
                i += 2;
            }
            else
            {
            	newPoints[j++] = points[i];
            }
        }
    	return newPoints;
    },
    getBezierPoint: function(t, p0, p1, p2, p3)
    {
    	if(p3 == undefined) //quadratic curve
		{
    		return Math.pow(1-t, 2)*p0 + + 2*t*(1-t)*p1 + Math.pow(t, 2)*p2; 
		}
    	else //cubic curve
		{
    		return Math.pow(1-t, 3)*p0 + + 3*Math.pow(1-t, 2)*t*p1 + 3*(1-t)*Math.pow(t, 2)*p2 + Math.pow(t, 3)*p3;
		}
    },
    doPaint: function(ctx)
    {
        ctx.beginPath();
        ctx.moveTo(this.path.xpoints[0], this.path.ypoints[0]);
        for(var i = 1; i < this.path.xpoints.length; i++)
        {
            if( this.path.pointTypes[i] == 1 && i < this.path.xpoints.length - 1 )
            {
                ctx.quadraticCurveTo(this.path.xpoints[i], this.path.ypoints[i], this.path.xpoints[i + 1], this.path.ypoints[i + 1]);
                i += 1;
            }
            if( this.path.pointTypes[i] == 2 && i < this.path.xpoints.length - 2 )
            {
                ctx.bezierCurveTo(this.path.xpoints[i], this.path.ypoints[i], this.path.xpoints[i + 1], this.path.ypoints[i + 1], this.path.xpoints[i + 2], this.path.ypoints[i + 2]);
                i += 2;
            }
            else
            {
                ctx.lineTo(this.path.xpoints[i], this.path.ypoints[i]);
            }
        }
        this.strokeAndFill(ctx);
    },
    
    getBounds: function()
    {
        return this.rect.clone();
    },
    
    intersects: function(r)
    {
        for(var i = 0; i < this.intersectionPath.xpoints.length - 1; i++ )
        {
            var view = new LineView(this.pen, this.intersectionPath.xpoints[i], this.intersectionPath.ypoints[i], 
            		this.intersectionPath.xpoints[i+1], this.intersectionPath.ypoints[i+1]);
            if( view.intersects(r) )
                return true;
        }
        return false;
    }
},
{
    createView: function(json, ctx)
    {
        var pen = Pen.fromJSON(json.pen);
        var view = new PathView(pen, new Path(json.xpoints, json.ypoints, json.pointtypes));
        view.type = parseInt(json.type);
        view.model = json.model;
        view.description = json.description;
        return view;
    }
});

var Tip = $.inherit(
{
    __constructor: function(view, width)
    {
        this.view = view;
        this.width = width;
    },
    locate: function(alpha, x, y)
    {
        this.view.rotate(alpha);
        this.view.translate(x, y);
    }
});

var ArrowView = $.inherit(CompositeView, 
{
    __constructor: function(pen, brush, path, startTip, endTip)
    {
        this.__base();
        if(startTip != undefined && !(startTip instanceof Tip)) // Tip type instead of Tip object passed
            startTip = this.__self.createTip(pen, brush, startTip);
        if(endTip != undefined && !(endTip instanceof Tip)) // Tip type instead of Tip object passed
            endTip = this.__self.createTip(pen, brush, endTip);
        this.path = path;
        this.pen = pen;
        this.brush = brush;
        if( path.npoints >= 2 )
        {
            pathView = new PathView(pen, path);
            this.add(pathView);

            if( startTip != undefined )
            {
                var dx = path.xpoints[1] - path.xpoints[0];
                var dy = path.ypoints[1] - path.ypoints[0];
                var l = Math.sqrt(dx * dx + dy * dy);
                var alpha = Math.asin(dy / l);
                if( dx < 0 ) alpha = Math.PI - alpha;
                startTip.locate(alpha + Math.PI, path.xpoints[0], path.ypoints[0]);
                this.add(startTip.view);
            }

            if( endTip != undefined )
            {
                var dx = path.xpoints[path.npoints - 1] - path.xpoints[path.npoints - 2];
                var dy = path.ypoints[path.npoints - 1] - path.ypoints[path.npoints - 2];
                var l = Math.sqrt(dx * dx + dy * dy);
                var alpha = Math.asin(dy / l);
                if( dx < 0 ) alpha = Math.PI - alpha;
                endTip.locate(alpha, path.xpoints[path.npoints - 1], path.ypoints[path.npoints - 1]);
                this.add(endTip.view);
            }
        }
        for(var i in this.children)
        {
            this.children[i].lineJoin = "miter";
        }
    }
},
{
    ARROW_TIP: 1,
    TRIANGLE_TIP: 2,
    SIMPLE_TIP: 3,
    DIAMOND_TIP: 4,
    createTip: function(pen, brush, tipType)
    {
        var w1 = 10;
        var w2 = 20;
        var h = 5;
        var tip;
        switch( tipType )
        {
            case this.ARROW_TIP:
                tip = this.createArrowTip(pen, brush, w1, w2, h);
                break;
            case this.DIAMOND_TIP:
                tip = this.createDiamondTip(pen, brush, w1, w2, h);
                break;
            case this.TRIANGLE_TIP:
                tip = this.createTriangleTip(pen, brush, w2, h);
                break;
            case this.SIMPLE_TIP:
                tip = this.createSimpleTip(pen, w2, h);
                break;
        }
        return tip;
    },
    createArrowTip: function(pen, brush, w1, w2, h)
    {
        return new Tip(new PolygonView(pen, brush, [-w1, -w2, 0, -w2], [0, h, 0, -h]), w2 - w1);
    },
    createTriggerTip: function(pen, brush, w, h)
    {
        return new Tip(new PolygonView(pen, brush, [-w, -w, -w, -w + 2, -w + 2, 0, -w + 2, -w + 2, -w, -w],
                    [0, -h, 0, 0, -h + 2, 0, h - 2, 0, 0, h]), w);
    },
    createTriangleTip: function(pen, brush, w, h)
    {
        return new Tip(new PolygonView(pen, brush, [-w, 0, -w], [h, 0, -h]), w);
    },
    createReverseTriangleTip: function(pen, brush, w, h)
    {
        return new Tip(new PolygonView(pen, brush, [-w, 0, 0], [0, h, -h]), w);
    },
    createSimpleTip: function(pen, w, h)
    {
        return new Tip(new PolygonView(pen, undefined, [-w, 0, -w], [h, 0, -h]), pen.width);
    },
    createDiamondTip: function(pen, brush, w1, w2, h)
    {
        return new Tip(new PolygonView(pen, brush, [-w2, -w1, 0, -w1], [0, h, 0, -h]), w2 - w1);
    },
    createLineTip: function(pen, brush, w, h)
    {
        return new Tip(new PolygonView(pen, brush, [-w, -w], [h, -h]), 0);
    },
    createEllipseTip: function(pen, brush, r)
    {
        var edgeCount = 16;
        var xArray = [];
        var yArray = [];
        for(var  i = 0; i < edgeCount; i++)
        {
            xArray.push( Math.floor( r * Math.cos(2 * Math.PI * i / edgeCount) + 0.5 ) - r);
            yArray.push( Math.floor( r * Math.sin(2 * Math.PI * i / edgeCount) + 0.5 ) );
        }
        return new Tip(new PolygonView(pen, brush, xArray, yArray), 2 * r);
    },
    locateTip: function(tip, alpha, x, y)
    {
        tip.locate(alpha, x, y);
    },
    
    intersects: function(r)
    {
        return this.children[0].intersects(r);
    },
    createView: function(json, ctx, oldView)
    {
        var view = CompositeView.createView(json, ctx, oldView);
        for(var i in view.children)
        {
            view.children[i].lineJoin = "miter";
        }
        return view;
    }
});

var Ruler = $.inherit(CompositeView, 
{
    __constructor: function(type, anchor, scale, min, max, rulerOptions, density, ctx)
    {
        this.__base();
        if( min > max )
        {
            this.isReversed = true;
            //[min,max] = [max,min];
			// Old-school swap just to make Aptana editor happy
			var tmp = min;
			min = max;
			max = tmp;
        }
        // 2.
        this.rulerType = type;
        this.anchor = anchor;
        this.scale = scale;
        this.min = min;
        this.max = max;
        this.options = rulerOptions;
        this.density = density;
        this.step = this.options.step;
        this.tPerT = this.options.ticks;

        // 3.
        var majorFont = this.options.majorFont.font;
        var minorFont = this.options.minorFont.font;
        if( this.step == undefined || this.step == 0 )
        {
            // 3.1.
            var decDig = this.options.decDig;
            var lMin = min.toFixed(decDig.width);
            var lMax = max.toFixed(decDig.height);

            var labelMaxSize;
            if( ( type & Ruler.HORIZONTAL ) != 0 )
                labelMaxSize = Math.max(Math.max(majorFont.getExtent(lMin, ctx)[0], majorFont.getExtent(lMax, ctx)[0]), 
                        Math.max(minorFont.getExtent(lMin, ctx)[0], minorFont.getExtent(lMax, ctx)[0]));
            else
                labelMaxSize = Math.max(majorFont.size, minorFont.size);

            // 3.2.
            var length = max - min;
            var intervalNumber = Math.floor( length * scale / ( labelMaxSize * 2 ) ) + 1;

            // 3.3.
            var stepApr = length / intervalNumber;
            this.step = 1;

            if( stepApr > this.step )
            {
                while( this.step < stepApr )
                {
                    this.step *= 2;
                    this.tPerT = 1;
                    if( this.step > stepApr )
                        break;
                    this.step *= 2.5;
                    this.tPerT = 4;
                    if( this.step > stepApr )
                        break;
                    this.step *= 2;
                    this.tPerT = 9;
                }
            }
            else
            {
                while( this.step > stepApr )
                {
                    this.step /= 2;
                    this.tPerT = 1;
                    if( this.step < stepApr )
                        break;
                    this.step /= 2.5;
                    this.tPerT = 4;
                    if( this.step < stepApr )
                        break;
                    this.step /= 2;
                    this.tPerT = 9;
                }
            }
            rulerOptions.step = this.step;
            rulerOptions.ticks = this.tPerT;
        }
        this.initSize(ctx);
    },
    initSizeUsingTicksAndLabels: function(step, stepMajor, pFrom, pTicks, fLabel, ctx,
            tickUB, tickDB, decDig, textOffset, ticksShow, labelsShow, labelsUp)
    {
        var cur = step * Math.floor( this.min / step );

        var pCur = new Point(0, 0);
        if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
        {
            pCur.y = pFrom.y;
        }
        else
        {
            pCur.x = pFrom.x;
        }

        var labelWidth, label;

        var pText = new Point(pCur.x, pCur.y);
        var textalign = View.BASELINE;
        if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
        {
            if( labelsUp )
            {
                pText.y = tickUB - textOffset.height;
                textalign = View.BOTTOM;
            }
            else
            {
                pText.y = tickDB + textOffset.height;
                textalign = View.TOP;
            }
        }
        else
        {
            if( !labelsUp )
                pText.x = tickDB + textOffset.width;
        }

        var shift, curTick = 0;
        if( cur <= this.min )
        {
            curTick = this.min;
        }
        else if( cur > this.max )
        {
            curTick = this.max;
        }
        else
        {
            curTick = cur - 1;
        }

        if( !this.isReversed )
            shift = this.scale * ( curTick - this.min );
        else
            shift = this.scale * ( this.max - curTick );

        if( ( this.rulerType & Ruler.GENE ) != 0 && curTick > 0 && this.min < 0 )
            shift -= this.scale;

        if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
            pCur.x = pFrom.x + Math.floor(shift);
        else
            pCur.y = pFrom.y - Math.floor(shift);


        if( ticksShow )
        {
            if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
            {
                this.rect.add(new LineView(pTicks, pCur.x, tickUB, pCur.x, tickDB).getBounds());
            }
            else
            {
                this.rect.add(new LineView(pTicks, tickUB, pCur.y, tickDB, pCur.y).getBounds());
            }
        }

        if( labelsShow )
        {
            label = curTick.toFixed(decDig);

            var labelWidth = fLabel.font.getExtent(label, ctx)[0];

            if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
                pText.x = pCur.x - labelWidth / 2;
            else
            {
                pText.y = pCur.y;
                if( labelsUp )
                    pText.x = tickDB - labelWidth - textOffset.width;
            }

            if( pText.x < 0 )
            {
                pText.x = 0;
            }

            var txt = new TextView(label, new Point(pText.x, pText.y), View.LEFT|textalign, fLabel, ctx);
            this.rect.add(txt.getBounds());
        }
    },
    initSize: function(ctx)
    {
        var d = new Dimension(0, 0);

        // 1.
        var pAxis = this.options.axisPen;
        var pTicks = this.options.ticksPen;
        var dText = this.options.textOffset;
        var dTickSize = this.options.tickSize;
        var decDig = this.options.decDig;

        // 2.
        var pFrom = new Point(this.anchor.x - d.width, this.anchor.y - d.height);
        var pTo = new Point(pFrom.x, pFrom.y);
        var axisLen = Math.floor ( ( this.max - this.min ) * this.scale );

        if( ( this.rulerType & Ruler.GENE ) != 0 && this.min < 0 && this.max > 0 )
            axisLen -= this.scale;

        if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
            pTo.x += axisLen;
        else
            pTo.y -= axisLen;

        this.rect.add(new LineView(pAxis, pFrom.x, pFrom.y, pTo.x, pTo.y).getBounds());

        // 3.
        var TickUB, tickUB; // ticks upper(left)  boundary
        var TickDB, tickDB; // ticks down (right) boundary

        if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
            TickUB = pFrom.y;
        else
            TickUB = pFrom.x;

        TickDB = tickDB = tickUB = TickUB;

        if( ( this.rulerType & Ruler.TICKS_MAJOR_UP ) != 0 )
        {
            TickUB -= dTickSize.width;
            tickUB -= dTickSize.height;
        }

        if( ( this.rulerType & Ruler.TICKS_MAJOR_DOWN ) != 0 )
        {
            TickDB += dTickSize.width;
            tickDB += dTickSize.height;
        }

        // 4.
        var start, label, pStart;
        if( ( this.rulerType & ( Ruler.TICKS_MAJOR_UP | Ruler.TICKS_MAJOR_DOWN | Ruler.LABELS_MAJOR_SHOW ) ) != 0 )
        {
            this.initSizeUsingTicksAndLabels(this.step, 0, pFrom, pTicks, this.options.majorFont, ctx, TickUB, TickDB, decDig.width, dText,
                    ( this.rulerType & ( Ruler.TICKS_MAJOR_UP | Ruler.TICKS_MAJOR_DOWN ) ) != 0, ( this.rulerType & Ruler.LABELS_MAJOR_SHOW ) != 0,
                    ( this.rulerType & Ruler.LABELS_MAJOR_UP ) != 0);
        }

        // 5
        if( ( this.rulerType & ( Ruler.TICKS_MINOR_UP | Ruler.TICKS_MINOR_DOWN | Ruler.LABELS_MINOR_SHOW ) ) != 0 )
        {
            this.initSizeUsingTicksAndLabels(this.step / ( this.tPerT + 1 ), this.step, pFrom, pTicks, this.options.minorFont, ctx, tickUB, tickDB, decDig.height,
                    dText, ( this.rulerType & ( Ruler.TICKS_MINOR_UP | Ruler.TICKS_MINOR_DOWN ) ) != 0, ( this.rulerType & Ruler.LABELS_MINOR_SHOW ) != 0,
                    ( this.rulerType & Ruler.LABELS_MINOR_UP ) != 0);
        }
    },
    /**
     * Moves the ruler (left/bottom axis end) to the specified point.
     *
     * @param p the specified point.
     */
    move: function(x, y)
    {
        this.rect.x += x;
        this.rect.y += y;
        this.anchor.x += x;
        this.anchor.y += y;
    },
    paintTicksAndLabels: function(step, stepMajor, pFrom, pTicks, fLabel, ctx,
            tickUB, tickDB, decDig, textOffset, ticksShow, labelsShow, labelsUp, shortLabelFormat)
    {
        var cur = stepMajor * Math.floor( this.min / stepMajor );

        var pCur = new Point(0, 0);
        if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
        {
            pCur.y = pFrom.y;
        }
        else
        {
            pCur.x = pFrom.x;
        }

        var labelWidth, label;

        var pText = new Point(pCur.x, pCur.y);
        if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
        {
            if( labelsUp )
            {
                pText.y = tickUB - textOffset.height;
                ctx.textBaseline = "bottom";
            }
            else
            {
                pText.y = tickDB + textOffset.height;
                ctx.textBaseline = "top";
            }
        }
        else
        {
            if( !labelsUp )
                pText.x = tickDB + textOffset.width;
        }

        ctx.font = fLabel.font.toCSS();
        ctx.fillStyle = fLabel.color.toRGBA();
        if(ticksShow)
            ctx.strokeStyle = pTicks.color.toRGBA();
        // cycle
        var shift;
        var labelRight = 0;
        var labelBounds;
        for( ; cur <= this.max + step + 1; cur += step )
        {
            var curTick = 0;
            if( cur <= this.min )
            {
                curTick = this.min;
            }
            else if( cur > this.max )
            {
                curTick = this.max;
            }
            else
            {
                curTick = cur;
            }

            if( ( this.rulerType & Ruler.GENE ) != 0 && curTick == 0 )
            {
                continue;
            }

            if( !this.isReversed )
            {
                shift = this.scale * ( curTick - this.min );
            }
            else
            {
                shift = this.scale * ( this.max - curTick );
            }

            if( ( this.rulerType & Ruler.GENE ) != 0 && curTick > 0 && this.min < 0 )
                shift -= this.scale;

            if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
                pCur.x = pFrom.x + Math.floor(shift);
            else
                pCur.y = pFrom.y - Math.floor(shift);

            if( labelsShow )
            {
                //process Mb = mega base if needed
                if( shortLabelFormat )
                {
                    label = (curTick / 1000000.0).toFixed(decDig + 2) + "Mb";
                }
                else
                {
                    label = curTick.toFixed(decDig);
                }

                var labelWidth = fLabel.font.getExtent(label, ctx)[0];
                var zeroWidth = fLabel.font.getExtent("0", ctx)[0];
                if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
                    pText.x = pCur.x - labelWidth / 2;
                else
                {
                    pText.y = pCur.y;
                    if( labelsUp )
                        pText.x = tickDB - labelWidth - textOffset.width;
                }

                curLabelBounds = new Rectangle(pText.x, pText.y, labelWidth + zeroWidth / 2, fLabel.font.size);
                if( labelBounds != null && labelBounds.intersects(curLabelBounds) )
                {
                    continue;
                }
                ctx.fillText(label, pText.x, pText.y);
                labelBounds = curLabelBounds;
            }

            if( ticksShow )
            {
                ctx.beginPath();
                if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
                {
                    ctx.moveTo(pCur.x, tickUB);
                    ctx.lineTo(pCur.x, tickDB);
                }
                else
                {
                    ctx.moveTo(tickUB, pCur.y);
                    ctx.lineTo(tickDB, pCur.y);
                }
                ctx.stroke();
            }
        }
    },
    doPaint: function(ctx, clip)
    {
        var loc = new Point(this.rect.x, this.rect.y);
        if( clip == null )
        {
            clip = new Rectangle(0, 0, 0, 0); //Integer.MAX_VALUE
        }
        var d = new Dimension( -loc.x, - ( loc.y + this.rect.height ));

        // 1.
        var pAxis = this.options.axisPen;
        var pTicks = this.options.ticksPen;
        var dText = this.options.textOffset;
        var dTickSize = this.options.tickSize;
        var decDig = this.options.decDig;

        // 2.
        var pFrom = new Point(this.anchor.x, this.anchor.y);

        var _min = this.min;
        var _max = this.max;

        // setup min
        var delta = ( clip.x - pFrom.x ) / this.scale;
        var num = ( Math.ceil(delta / this.step) - 1 );
        this.min = this.min + num * this.step;
        if( this.min < _min )
        {
            this.min = _min;
        }
        else
        {
            diff = Math.floor ( num * this.step * this.scale );
            pFrom.x = pFrom.x + diff;
        }

        // setup max
        delta = clip.width / this.scale + this.step;
        num = ( Math.ceil(delta / this.step) );
        this.max = this.min + ( num + 1 ) * this.step;
        if( this.max > _max )
        {
            this.max = _max;
        }

        var pTo = new Point(pFrom.x, pFrom.y);
        var axisLen = Math.floor ( ( this.max - this.min ) * this.scale );

        if( ( this.rulerType & Ruler.GENE ) != 0 && this.min < 0 && this.max > 0 )
            axisLen -= this.scale;

        if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
            pTo.x += axisLen;
        else
            pTo.y -= axisLen;

        ctx.save();
        ctx.strokeStyle = pAxis.color.toRGBA();
        ctx.beginPath();
        ctx.moveTo(pFrom.x, pFrom.y);
        ctx.lineTo(pTo.x, pTo.y);
        ctx.stroke();

        // 3.
        var TickUB, tickUB; // ticks upper(left)  boundary
        var TickDB, tickDB; // ticks down (right) boundary

        if( ( this.rulerType & Ruler.HORIZONTAL ) != 0 )
            TickUB = pFrom.y;
        else
            TickUB = pFrom.x;

        TickDB = tickDB = tickUB = TickUB;

        if( ( this.rulerType & Ruler.TICKS_MAJOR_UP ) != 0 )
        {
            TickUB -= dTickSize.width;
            tickUB -= dTickSize.height;
        }

        if( ( this.rulerType & Ruler.TICKS_MAJOR_DOWN ) != 0 )
        {
            TickDB += dTickSize.width;
            tickDB += dTickSize.height;
        }

        // 4.
        var start, label, pStart;
        var shortLabelFormat = ( this.density < 0.01 ); //use short label format (example, 125.20 Mb) if pixel/nucleotide less than 0.01
        if( ( this.rulerType & ( Ruler.TICKS_MAJOR_UP | Ruler.TICKS_MAJOR_DOWN | Ruler.LABELS_MAJOR_SHOW ) ) != 0 )
        {
            this.paintTicksAndLabels(this.step, this.step, pFrom, pTicks, this.options.majorFont, ctx, TickUB, TickDB, decDig.width, dText,
                    ( this.rulerType & ( Ruler.TICKS_MAJOR_UP | Ruler.TICKS_MAJOR_DOWN ) ) != 0, ( this.rulerType & Ruler.LABELS_MAJOR_SHOW ) != 0,
                    ( this.rulerType & Ruler.LABELS_MAJOR_UP ) != 0, shortLabelFormat);
        }

        // 5
        if( ( this.rulerType & ( Ruler.TICKS_MINOR_UP | Ruler.TICKS_MINOR_DOWN | Ruler.LABELS_MINOR_SHOW ) ) != 0 )
        {
            this.paintTicksAndLabels(this.step / ( this.tPerT + 1 ), this.step, pFrom, pTicks, this.options.minorFont, ctx, tickUB, tickDB, decDig.height,
                    dText, ( this.rulerType & ( Ruler.TICKS_MINOR_UP | Ruler.TICKS_MINOR_DOWN ) ) != 0, ( this.rulerType & Ruler.LABELS_MINOR_SHOW ) != 0,
                    ( this.rulerType & Ruler.LABELS_MINOR_UP ) != 0, shortLabelFormat);
            this.min = _min;
            this.max = _max;
        }
        ctx.restore();
    }
},
{
    /**
     * Ruler type: HORIZONTAL/VERTICAL
     */
    VERTICAL: 0,
    HORIZONTAL: 1,

    /**
     * Ruler type: GENE/ USUAL.
     * If GENE, the position zero absent.
     */
    USUAL: 0,
    GENE: 2,

    /**
     * Ruler type: TICKS_MAJOR_UP/ABSENT.
     * For vertical ruler this is mean: TICKS_MAJOR_LEFT/ABSENT.
     */
    TICKS_MAJOR_UP: 4,

    /**
    * Ruler type: TICKS_MAJOR_DOWN/ABSENT.
    * For vertical ruler this is mean: TICKS_MAJOR_RIGHT/ABSENT.
    */
    TICKS_MAJOR_DOWN: 8,

    /**
     * Ruler type: TICKS_MINOR_UP/ABSENT.
     * For vertical ruler this is mean: TICKS_MINOR_LEFT/ABSENT.
     */
    TICKS_MINOR_UP: 16,

    /**
    * Ruler type: TICKS_MINOR_DOWN/ABSENT.
    * For vertical ruler this is mean: TICKS_MINOR_RIGHT/ABSENT.
    */
    TICKS_MINOR_DOWN: 32,

    /**
    * Ruler type: LABELS_MAJOR_SHOW/ABSENT.
    */
    LABELS_MAJOR_SHOW: 64,

    /**
    * Ruler type: LABELS_MAJOR_UP/LABELS_MAJOR_DOWN.
    * For vertical ruler this is mean: LABELS_MAJOR_LEFT/LABELS_MAJOR_RIGHT
    */
    LABELS_MAJOR_UP: 128,

    /**
    * Ruler type: LABELS_MINOR_SHOW/ABSENT.
    */
    LABELS_MINOR_SHOW: 256,

    /**
    * Ruler type: LABELS_MINOR_UP/LABELS_MINOR_DOWN.
    * For vertical ruler this is mean: LABELS_MINOR_LEFT/LABELS_MINOR_RIGHT
    */
    LABELS_MINOR_UP: 512
});

var Sequence = $.inherit(
{
    /*
     * params.from, params.to = start and end coordinates
     * params.type => "raw"/"ajax"/"test"
     * if "raw", then params.data is sequence itself
     * if "test" then sequence looks like 'ACGTACGTACGT...' and has arbitrary length
     * if "ajax" (NOT IMPLEMENTED), then 
     *    params.URL => ajax service returning sequence
     *    params.ajaxParam => static ajax params
     *    params.ajaxParamFrom => name of 'from' parameter
     *    params.ajaxParamTo => name of 'to' parameter
     */
    __constructor: function(params)
    {
        var _this = this;
        $.each("from,to,type,data,URL,ajaxParam,ajaxParamFrom,ajaxParamTo".split(","), function() {
            _this[this] = params[this];
        });
    },
    getLength: function()
    {
        return this.to-this.from+1;
    },
    isCircular: function()
    {    // circular sequences are not implemented
        return false;
    },
    getLetterAt: function(pos)
    {
        if(pos<this.from || pos>this.to) return "";
        if(this.type == "test")
            return ' ';
        return this.data.substr(pos-this.from, 1);
    },
    regionAt: function(from, to)
    {
        if(from<this.from || to<this.from || from>this.to || to>this.to) return "";
        if(this.type == "test")
        {
            var result = "";
            for(var pos=from; pos<=to; pos++) {
                result+=' ';
            }
            return result;
        }
            
        return this.data.substr(from-this.from, to-from+1);
    }
});

var SequenceView = $.inherit(CompositeView, 
{
    __constructor: function(sequence, options, start, end, ctx)
    {
        this.__base();
        this.sequence = sequence;
        this.options = options;
        this.start = start;
        this.end = end;
        this.ctx = ctx;
        
        var backColor = new Color(170, 200, 250);

        var front = new CompositeView();

        var seqfont = options.font.font;

        var s = "";
        var factor = 0;

        var rulerOptions = options.rulerOptions;

        var textLength = 0;
        if( options.type == SequenceView.PT_RULER )
        {
            factor = this.options.density || 1;
        }
        else
        {
            s = sequence.regionAt(start, end);
            textLength = seqfont.getExtent(s, ctx)[0];
            factor = textLength / s.length;
        }
        this.pixelsPerNucleotide = factor;

        var offset = 0;
        var selectionHeight = 0;

        if( options.type != 0 )
        {
            var type = Ruler.HORIZONTAL | Ruler.TICKS_MAJOR_UP | Ruler.TICKS_MINOR_UP | Ruler.LABELS_MAJOR_SHOW | Ruler.LABELS_MAJOR_UP;

            var shift = this.getNucleotideWidth() / 2.;

            var ruler = new Ruler(type, new Point(shift, 0), factor, start, end, rulerOptions, options.density||1, ctx);
            selectionHeight = ruler.getBounds().height / 4;
            offset = selectionHeight;
            front.add(ruler);
        }
        if( options.type != SequenceView.PT_RULER )
        {
            var text = new TextView(s, options.font, ctx);
            front.add(text, CompositeView.Y_BT);
            selectionHeight += text.getBounds().height;
        }
        this.selection = new BoxView(new Pen(backColor), new Brush(backColor), 0, -offset, 30, selectionHeight);
        this.add(this.selection);
        this.hideSelection();
        this.add(front);
    },
    getNucleotideWidth: function()
    {
        return this.pixelsPerNucleotide;
    },
    hideSelection: function()
    {
        this.selection.setVisible(false);
    },
    previousSelectionX: 0,
    setSelection: function(x1, x2)
    {
        if( x1 < this.start )
            x1 = this.start;
        var length = this.sequence.getLength();
        if( x2 > this.end )
            x2 = this.end;
        if( x1 > x2 )
            return;
        this.selection.setVisible(true);
        x1 = this.getStartPoint(x1 - this.start + 1).x;
        x2 = this.getEndPoint(x2 - this.start + 1).x;
        var w = this.selection.getBounds().width;
        this.selection.resize(x2 - x1 - w, 0);
        this.selection.move(x1 - this.previousSelectionX, 0);
        this.previousSelectionX = x1;
    },
    getPosition: function(pt)
    {
        return Math.floor(pt.x / this.getNucleotideWidth());
    },
    /**
     * Calculate coordinate of the begin of the interesting subsequence
     * in absolute coordinates.
     *
     * @param position where subsequence starts
     * @param graphics graphics for which position is calculated
     *
     * @return The left bound of the symbol in the specified <CODE>position</CODE>.
     */
    getStartPoint: function(position)
    {
        var shift = Math.floor( this.getNucleotideWidth() / 2 );
        if( position == 1 )
        {
            return new Point(shift, 0);
        }

        var len = position - 1;

        var start = Math.floor ( this.getNucleotideWidth() * len );
        return new Point(start + shift, 0);
    },
    /**
     * Calculate coordinate of the end of the interesting subsequence
     * in absolute coordinates.
     *
     * @param position where subsequence ends
     * @param graphics graphics for which position is calculated
     *
     * @return The right bound of the symbol in the specified <CODE>position</CODE>.
     */
    getEndPoint: function(position)
    {
        return this.getStartPoint(position+1);
    }
},
{
    // Printing type constants
    PT_LETTERS: 0,
    PT_RULER: 1,
    PT_BOTH: 2
});

var TrackBackgroundView = $.inherit(BoxView, 
{
},
{
    COLOR: new Color(200, 200, 200, 0.2),
    createView: function(json, ctx)
    {
    	var brush = Brush.fromJSON(json.brush);
        var view = new BoxView(undefined, brush, json.x, json.y, json.width, json.height);
        return view;
    }
});

var TrackView = $.inherit(CompositeView,
{
    __constructor: function()
    {
        this.__base();
    }
},
{
    createView: function(json, ctx)
    {
        return CompositeView.createView(json, ctx);
    }
});
var TruncatedView = $.inherit(ShapeView,
{
	__constructor: polymorph (
		function(baseView, percent)
		{
			this.baseView = baseView;
			this.truncationPercent = percent;
		},
		function(baseView)
		{
			this.constructor(baseView, 0.75);
		}
	),
	/**
	 * set clipping area as (1-truncationPercent)% height rectangle from the bottom of the shape
	 */
	doPaint: function(ctx, clipRect)
    {
        var r = this.baseView.getBounds();
        var cr = r.clone();
        cr.height = r.height*(1-this.truncationPercent);
        cr.y = r.y+r.height-cr.height;
		ctx.save();
		ctx.beginPath();
		ctx.rect(cr.x, cr.y, cr.width, cr.height);
        ctx.clip();
        ctx.beginPath();
		this.baseView.paint(ctx, clipRect);
		ctx.restore();
    },
    move: function(sx, sy)
    {
        this.baseView.move();
    },
    scale: function(sx, sy)
    {
    	this.baseView.scale();
    },
    resize: function(sx, sy)
    {
    	this.baseView.resize();
    },
    getBounds: function()
    {
    	return this.baseView.getBounds();
    }
},
{
    createView: function(json, ctx)
    {
        var pen = Pen.fromJSON(json.pen);
        var brush = Brush.fromJSON(json.brush);
        var truncationPercent = json.truncationPercent == undefined ? 0.75 : json.truncationPercent;
        var base = json.base[0];
        var childClass = window[base['class']];
        var baseView;
        if (childClass && childClass.createView) 
        {
        	baseView = childClass.createView(base, ctx);
        }
        var view = new TruncatedView(baseView, truncationPercent);
        view.type = parseInt(json.type);
        view.model = json.model;
        view.description = json.description;
        return view;
    }
});
