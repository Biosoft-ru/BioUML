package biouml.plugins.brain.diagram;

import java.awt.Color;
import java.awt.Graphics;
import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

import biouml.model.DiagramViewOptions;
import biouml.model.Node;
import biouml.model.dynamics.TableElement;
import biouml.model.dynamics.TableElement.Variable;
import biouml.standard.diagram.MathDiagramViewBuilder;
import biouml.standard.type.Base;
import ru.biosoft.graphics.BoxView;
import ru.biosoft.graphics.Brush;
import ru.biosoft.graphics.ComplexTextView;
import ru.biosoft.graphics.CompositeView;
import ru.biosoft.graphics.LineView;
import ru.biosoft.graphics.PolygonView;
import ru.biosoft.graphics.TextView;

public class BrainDiagramViewBuilder extends MathDiagramViewBuilder
{	
    @Override
    public Icon getIcon(Object type)
    {    	
    	Icon icon = null;
    	if (type instanceof String)
    	{
    		String imageFile = "resources/" + ((String)type).toLowerCase() + ".gif";
            URL url = getIconURL(getResourcesRoot(), imageFile);
            if (url == null)
            {
            	url = getIconURL(MathDiagramViewBuilder.class, imageFile);
            }
            icon = new ImageIcon(url);
    	}
    	else if (type instanceof Class)
    	{
    		icon = getIcon((Class<?>)type, getClass());
    		if (icon == null) 
    		{
    			icon = getIcon((Class<?>)type, MathDiagramViewBuilder.class);
    		}
    	}
    	return icon;
    }
	
    @Override
    public boolean createNodeCoreView(CompositeView container, Node node, DiagramViewOptions viewOptions, Graphics g)
    {
    	DiagramViewOptions options = viewOptions;

        Base kernel = node.getKernel();
        String type = kernel.getType();

        if (BrainType.TYPE_CONNECTIVITY_MATRIX.equals(type) || BrainType.TYPE_DELAY_MATRIX.equals(type))
        {
        	return createBrainMatrixCoreView(container, node, options, g);
        }
        else if (BrainType.TYPE_REGIONAL_MODEL.equals(type))
        {
            return createRegionalModelCoreView(container, node, options, g);
        }
        else if (BrainType.TYPE_CELLULAR_MODEL.equals(type))
        {
            return createCellularModelCoreView(container, node, options, g);
        }
        else if (BrainType.TYPE_RECEPTOR_MODEL.equals(type))
        {
            return createReceptorModelCoreView(container, node, options, g);
        }
        
        return super.createNodeCoreView(container, node, options, g);
    }

    private boolean createBrainMatrixCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
    {
        Base kernel = node.getKernel();
        String type = kernel.getType();
    	TableElement te = node.getRole(TableElement.class);
        // Default initialization
	    if (te.getTable() == null)
	    {
	    	TextView header;
	    	if (BrainType.TYPE_CONNECTIVITY_MATRIX.equals(type))
	    	{
	    		header = new TextView("Please specify connectivity matrix!", options.getDefaultFont(), g);
	    	}
	    	else if (BrainType.TYPE_DELAY_MATRIX.equals(type))
	    	{
	    		header = new TextView("Please specify delay matrix!", options.getDefaultFont(), g);
	    	}
	    	else
	    	{
	    		header = new TextView("Unknown brain matrix!", options.getDefaultFont(), g);
	    	}
	        header.setLocation(5, 1);
	
	        int width = header.getBounds().width + 10;
	        int headerheight = header.getBounds().height;
	        int height = headerheight * 3 + 15;
	        container.add(new BoxView(options.getDefaultPen(), new Brush(Color.white), 0, 0, width, height));
	
	        //vertical
	        container.add(new LineView(options.getDefaultPen(), width/2f, headerheight+5, width/2f, height));
	
	        //horizontal
	        container.add(new LineView(options.getDefaultPen(), 0, headerheight+5, width, headerheight+5));
	        container.add(new LineView(options.getDefaultPen(), 0, headerheight*2+10, width, headerheight*2+10));
	
	        container.add(header);
	        return false;
	    }
	      
	    // Header
	    Variable[] variables = te.getVariables();
	    String formula = te.getFormula();
	    if (formula == null)
	    {
	    	if (BrainType.TYPE_CONNECTIVITY_MATRIX.equals(type))
	    	{
	    		formula = "Connectivity matrix";
	    	}
	    	else if (BrainType.TYPE_DELAY_MATRIX.equals(type))
	    	{
	    		formula = "Delay matrix";
	    	}
	    	else
	    	{
	    		formula = "Unknown matrix";
	    	}
	    }
	    TextView header = new TextView(formula, options.getDefaultFont(), g);
	    header.setLocation(0, 0);
	    container.add(header);
	
	    // Table size without names
	    int sizeRows = te.getTable().getSize();
	    int sizeColumns = variables.length;
	      
	    TextView[] colNames = new TextView[sizeColumns];
	    TextView[] varNames = new TextView[sizeColumns];
	    TextView[] varValues = new TextView[sizeColumns];
	    LineView[] verticalLines = new LineView[sizeColumns];
	    LineView[] horizontalLines = new LineView[sizeRows];
	      
	    // Calculate max number width
	    double cellValueMax = 0;
	    int cellWidthMax = 5;
	    for (int i = 0; i < sizeRows; i++) 
	    {
	        Object[] rowValues = te.getTable().getAt(i).getValues();
	        for (int j = 0; j < sizeColumns; j ++) 
	        {
	            double cellValue = ((Number)rowValues[j]).doubleValue();  
	          	cellValueMax = Math.max(cellValueMax, cellValue);
	          	varValues[j] = new TextView(String.format("%.2f", cellValue), options.getDefaultFont(), g);
	          	cellWidthMax = Math.max(cellWidthMax, varValues[j].getBounds().width);
	        }
	    }
	    cellWidthMax += 1;
	    
	    // Calculate max name size and table size
	    int x = 0;
	    int y = 0;
	    int nameWidthMax = 5;
	    int nameHeightMax = 5;
	    for (int j = 0; j < sizeColumns; j++)
	    {
	    	String colName = variables[j].getColumnName();
	        String varName = variables[j].getName();
	
	        colNames[j] = new TextView(colName, options.getDefaultFont(), g);
	        varNames[j] = new TextView(varName, options.getDefaultFont(), g); 
	          
	        nameWidthMax = Math.max(nameWidthMax, Math.max(colNames[j].getBounds().width, varNames[j].getBounds().width));
	        nameHeightMax = Math.max(nameHeightMax, Math.max(colNames[j].getBounds().height, varNames[j].getBounds().height));
	         
	        x += 5;
	        x += Math.max(Math.max(colNames[j].getBounds().width, varNames[j].getBounds().width) + 5, cellWidthMax);
	    }
	    nameWidthMax += 1;
	    for (int i = 0; i < sizeRows; i ++) 
	    {
	        y += nameHeightMax + 5;
	    }
	    int xStart = 5 + nameWidthMax;
	    if (sizeRows != sizeColumns) 
	    {
	    	xStart = 5;
	    }
	    int xEnd = xStart + x;
	    int yStart = header.getBounds().height + 5 + nameHeightMax;
	    int yEnd = yStart + y;
	         
	    // Visualize table block
	    container.add(new BoxView(options.getDefaultPen(), new Brush(Color.white), 0, header.getBounds().height, xEnd - 0, yEnd - header.getBounds().height));
	      
	    // Visualize names
	    x = xStart;
	    for (int j = 0; j < sizeColumns; j++) 
	    {   
	        x += 5;
	        colNames[j].setLocation(x, header.getBounds().height + 5);
	        varNames[j].setLocation(x, header.getBounds().height + 5);
	        x += Math.max(Math.max(colNames[j].getBounds().width, varNames[j].getBounds().width) + 5, cellWidthMax);
	        container.add(colNames[j]);
	        container.add(varNames[j]);
        }
	      
	    // For a square matrix display variable names on both sides.
	    y = yStart;
	    if (sizeColumns == sizeRows) 
	    {
	        TextView[] colNamesLeft = new TextView[sizeRows];
	        TextView[] varNamesLeft = new TextView[sizeRows];
	        
	        for (int i = 0; i < sizeRows; i++) 
	        {
	        	String colName = variables[i].getColumnName();
	            String varName = variables[i].getName();
	
	            colNamesLeft[i] = new TextView(colName, options.getDefaultFont(), g);
	            varNamesLeft[i] = new TextView(varName, options.getDefaultFont(), g); 
	      		
	            y += 5;
	      		colNamesLeft[i].setLocation(5, y);
	      		varNamesLeft[i].setLocation(5, y);
	      		y += nameHeightMax;
	            container.add(colNamesLeft[i]);
	            container.add(varNamesLeft[i]);
	      	}
	    }
	    
	    // Visualize vertical lines
	    x = xStart;
	    for (int j = 0; j < sizeColumns; j++) 
	    {
	    	x += 5;
	      	verticalLines[j] = new LineView(options.getDefaultPen(), x - 5, header.getBounds().height, x - 5, yEnd);
	      	x += Math.max(Math.max(colNames[j].getBounds().width, varNames[j].getBounds().width) + 5, cellWidthMax);
	      	if ((j == 0) & (sizeRows != sizeColumns)) 
	      	{
	      		continue;
	      	}
	      	else
	      	{
	      		container.add(verticalLines[j]);
	      	}
	    }
	      
	    // Visualize horizontal lines
	    y = yStart;
	    for (int i = 0; i < sizeRows; i++) 
	    {
	    	horizontalLines[i] = new LineView(options.getDefaultPen(), 0, y, xEnd - 0, y);
	      	y += nameHeightMax + 5;
	      	container.add(horizontalLines[i]);
	    }
	      
	    // Visualize numbers and colorize cells
	    y = yStart;
	    for (int i = 0; i < sizeRows; i++)
	    {
	    	Object[] rowValues = te.getTable().getAt(i).getValues();
	      	y += 5;
	      	
	      	x = xStart;
	      	for (int j = 0; j < sizeColumns; j++) 
	      	{
	      		double cellValue = ((Number)rowValues[j]).doubleValue();
	      		
	          	// Colorize cells
	          	if (sizeRows == sizeColumns) 
	          	{
	          		int red = 255, green = 255, blue = 255;
	          		
	          		if (BrainType.TYPE_CONNECTIVITY_MATRIX.equals(type))
	    	    	{
		          		red = 255;
		          		green = 255 - (int)(cellValue / cellValueMax * 255);
		          		blue = 255 - (int)(cellValue / cellValueMax * 255);
	    	    	}
	          		else if (BrainType.TYPE_DELAY_MATRIX.equals(type))
	          		{
	          			red = 255 - (int)(cellValue / cellValueMax * 255);
		          		green = 255 - (int)(cellValue / cellValueMax * 255);
		          		blue = 255;
	          		}
	  
	          		Color scaledColor = new Color(red, green, blue);
	          		int cellX = verticalLines[j].getBounds().x + 1;
	          		int cellY = horizontalLines[i].getBounds().y + 1;
	          		int cellWidth;
	          		int cellHeight;
	          		
	          		if (i < sizeRows - 1) 
	          		{
	          			cellHeight = horizontalLines[i + 1].getBounds().y - horizontalLines[i].getBounds().y;
	          		}
	          		else 
	          		{
	          			cellHeight = yEnd - cellY;
	          		}
	          		
	          		if (j < sizeColumns - 1) 
	          		{
	          			cellWidth = verticalLines[j + 1].getBounds().x - verticalLines[j].getBounds().x;
	          		}
	          		else 
	          		{
	          			cellWidth = xEnd - cellX;
	          		}
	          		
	          		container.add(new BoxView(options.getDefaultPen(), new Brush(scaledColor), cellX, cellY, cellWidth, cellHeight));
	          	}
	      		
	          	// Visualize numbers
	      		x += 5;
	      		varValues[j] = new TextView(String.format("%.2f", cellValue), options.getDefaultFont(), g);
	          	varValues[j].setLocation(x, y);
	          	x += Math.max(Math.max(colNames[j].getBounds().width, varNames[j].getBounds().width) + 5, cellWidthMax);
	          	container.add(varValues[j]);
	      	}
	          
	  		y += nameHeightMax;
	    }
	      
        return false;
    }
      
	private boolean createRegionalModelCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
	{
		int width = 120;
		int height = 100;
		
		container.add(new PolygonView(options.getDefaultPen(), new Brush(Color.orange), new int[] {0, 15, width - 15, width,
				width - 15, 15}, new int[] {height / 2, 0, 0, height / 2, height, height}));
	    container.add(new ComplexTextView(node.getTitle(), options.getNodeTitleFont(), options.getFontRegistry(),
	    		ComplexTextView.TEXT_ALIGN_CENTER, g, width), CompositeView.X_CC | CompositeView.Y_CC);
	    
	    return false;
	}
	
	private boolean createCellularModelCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
	{
		int width = 80;
		int height = 80;
		
		container.add(new PolygonView(options.getDefaultPen(), new Brush(Color.orange), new int[] {0, 15, width - 15, width,
				width - 15, 15}, new int[] {height / 2, 0, 0, height / 2, height, height}));
	    container.add(new ComplexTextView(node.getTitle(), options.getNodeTitleFont(), options.getFontRegistry(),
	    		ComplexTextView.TEXT_ALIGN_CENTER, g, width), CompositeView.X_CC | CompositeView.Y_CC);
	    
	    return false;
	}
	
	private boolean createReceptorModelCoreView(CompositeView container, Node node, DiagramViewOptions options, Graphics g)
	{
		int width = 80;
		int height = 60;
		
		container.add(new PolygonView(options.getDefaultPen(), new Brush(Color.orange), new int[] {0, 15, width - 15, width,
				width - 15, 15}, new int[] {height / 2, 0, 0, height / 2, height, height}));
	    container.add(new ComplexTextView(node.getTitle(), options.getNodeTitleFont(), options.getFontRegistry(),
	    		ComplexTextView.TEXT_ALIGN_CENTER, g, width), CompositeView.X_CC | CompositeView.Y_CC);
	    
	    return false;
	}
}
