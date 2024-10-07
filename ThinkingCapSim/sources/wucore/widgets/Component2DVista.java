package wucore.widgets;import java.awt.*;import java.awt.geom.*;import java.awt.print.*;public class Component2DVista extends Object implements Pageable, Printable {	protected double 						mScaleX;	protected double 						mScaleY;	protected Component 					mComponent;	protected int 							mNumPagesX;	protected int 							mNumPagesY;	protected int 							mNumPages;	protected Printable 					mPainter;	protected PageFormat 					mFormat;	/**	 * Create a Pageable that can print a	 * Swing JComponent over multiple pages.	 *	 * @param c The swing JComponent to be printed.	 *	 * @param format The size of the pages over which	 * the componenent will be printed.	 */	public Component2DVista (Component component, PageFormat format) 	{		setPageFormat (format);		setPrintable (this);		setComponent (component);		/* Tell the Vista we subclassed the size of the canvas.		 */		Rectangle componentBounds = component.getBounds(null);		setSize (componentBounds.width, componentBounds.height);		setScale (1, 1);	}	// Accessors	protected void			setPrintable (Printable painter) 		{ mPainter = painter; }	protected void 			setPageFormat (PageFormat pageFormat) 	{ mFormat = pageFormat; }	public int				getNumberOfPages()						{ return mNumPages; }	protected PageFormat	getPageFormat()							{ return mFormat; }	protected void 			setComponent (Component component)		{ mComponent = component; }	protected void setScale (double scaleX, double scaleY) 	{		mScaleX = scaleX;		mScaleY = scaleY;	}	public void scaleToFitX () 	{		PageFormat format = getPageFormat();		Rectangle componentBounds = mComponent.getBounds(null);		double scaleX = format.getImageableWidth() /componentBounds.width;		double scaleY = scaleX;		if (scaleX < 1) 		{			setSize ((float) format.getImageableWidth(), (float) (componentBounds.height * scaleY));			setScale (scaleX, scaleY);		}	}	public void scaleToFitY () 	{		PageFormat format = getPageFormat();		Rectangle componentBounds = mComponent.getBounds(null);		double scaleY = format.getImageableHeight() /componentBounds.height;		double scaleX = scaleY;		if (scaleY < 1) 		{			setSize ((float) (componentBounds.width * scaleX),(float) format.getImageableHeight());			setScale (scaleX, scaleY);		}	}	public void scaleToFit (boolean useSymmetricScaling) 	{		PageFormat format = getPageFormat();		Rectangle componentBounds = mComponent.getBounds(null);		double scaleX = format.getImageableWidth() /componentBounds.width;		double scaleY = format.getImageableHeight() /componentBounds.height;		if (scaleX < 1 || scaleY < 1) 		{			if (useSymmetricScaling) 			{				if (scaleX < scaleY) 					scaleY = scaleX;				else 					scaleX = scaleY;			}			setSize ((float) (componentBounds.width * scaleX), (float) (componentBounds.height * scaleY) );			setScale (scaleX, scaleY);		}	}	public int print (Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException 	{		Graphics2D g = (Graphics2D) graphics;		g.translate(pageFormat.getImageableX(), pageFormat.getImageableY());		Rectangle componentBounds = mComponent.getBounds(null);		g.translate(-componentBounds.x, -componentBounds.y);		g.scale(mScaleX, mScaleY);		//		boolean wasBuffered = mComponent.isDoubleBuffered();				mComponent.paint (g);		//		mComponent.setDoubleBuffered(wasBuffered);				return PAGE_EXISTS;	}		/**	 * Set the size of the canvas to be drawn.	 * 	 * @param width The width, in 1/72nds of an inch, of	 * the vist's canvas.	 *	 * @param height The height, in 1/72nds of an inch, of	 * the vista's canvas.	 */	protected void setSize (float width, float height) 	{		mNumPagesX = (int) ((width + mFormat.getImageableWidth() - 1)/ mFormat.getImageableWidth());		mNumPagesY = (int) ((height + mFormat.getImageableHeight() - 1)/ mFormat.getImageableHeight());		mNumPages = mNumPagesX * mNumPagesY;	}	/** 	 * Returns the PageFormat of the page specified by	 * pageIndex. For a Vista the PageFormat	 * is the same for all pages.	 *	 * @param pageIndex the zero based index of the page whose	 * PageFormat is being requested	 * @return the PageFormat describing the size and	 * orientation.	 * @exception IndexOutOfBoundsException	 * the Pageable  does not contain the requested	 * page.	 */	public PageFormat getPageFormat (int pageIndex) throws IndexOutOfBoundsException 	{		if (pageIndex >= mNumPages)			throw new IndexOutOfBoundsException();		return getPageFormat();	}	/**	 * Returns the <code>Printable</code> instance responsible for	 * rendering the page specified by <code>pageIndex</code>.	 * In a Vista, all of the pages are drawn with the same	 * Printable. This method however creates	 * a Printable which calls the canvas's	 * Printable. This new Printable	 * is responsible for translating the coordinate system	 * so that the desired part of the canvas hits the page.	 *	 * The Vista's pages cover the canvas by going left to	 * right and then top to bottom. In order to change this	 * behavior, override this method.	 *	 * @param pageIndex the zero based index of the page whose	 * Printable is being requested	 * @return the Printable that renders the page.	 * @exception IndexOutOfBoundsException	 * the Pageable does not contain the requested	 * page.	 */	public Printable getPrintable (int pageIndex) throws IndexOutOfBoundsException 	{		if (pageIndex >= mNumPages)			throw new IndexOutOfBoundsException();		double originX = (pageIndex % mNumPagesX) * mFormat.getImageableWidth();		double originY = (pageIndex / mNumPagesX) * mFormat.getImageableHeight();		Point2D.Double origin = new Point2D.Double(originX, originY);		return new TranslatedPrintable(mPainter, origin);	}	/**	 * This inner class's sole responsibility is to translate	 * the coordinate system before invoking a canvas's	 * painter. The coordinate system is translated in order	 * to get the desired portion of a canvas to line up with	 * the top of a page.	 */	public static final class TranslatedPrintable implements Printable 	{		/**		 * The object that will draw the canvas.		 */		private Printable 					mPainter;		/**		 * The upper-left corner of the part of the canvas		 * that will be displayed on this page. This corner		 * is lined up with the upper-left of the imageable		 * area of the page.		 */		private Point2D 					mOrigin;		/**		 * Create a new Printable that will translate		 * the drawing done by painter on to the		 * imageable area of a page.		 *		 * @param painter The object responsible for drawing		 * the canvas		 *		 * @param origin The point in the canvas that will be		 * mapped to the upper-left corner of		 * the page's imageable area.		 */		public TranslatedPrintable (Printable painter, Point2D origin) 		{			mPainter = painter;			mOrigin = origin;		}		/**		 * Prints the page at the specified index into the specified 		 * {@link Graphics} context in the specified		 * format. A PrinterJob calls the 		 * Printableinterface to request that a page be		 * rendered into the context specified by 		 * graphics. The format of the page to be drawn is		 * specified by pageFormat. The zero based index		 * of the requested page is specified by pageIndex. 		 * If the requested page does not exist then this method returns		 * NO_SUCH_PAGE; otherwise PAGE_EXISTS is returned.		 * The Graphics class or subclass implements the		 * {@link PrinterGraphics} interface to provide additional		 * information. If the Printable object		 * aborts the print job then it throws a {@link PrinterException}.		 * @param graphics the context into which the page is drawn 		 * @param pageFormat the size and orientation of the page being drawn		 * @param pageIndex the zero based index of the page to be drawn		 * @return PAGE_EXISTS if the page is rendered successfully		 * or NO_SUCH_PAGE if pageIndex specifies a		 * non-existent page.		 * @exception java.awt.print.PrinterException		 * thrown when the print job is terminated.		 */		public int print (Graphics graphics, PageFormat pageFormat, int pageIndex) throws PrinterException 		{			Graphics2D g2 = (Graphics2D) graphics;			g2.translate (-mOrigin.getX(), -mOrigin.getY());			return mPainter.print (g2, pageFormat, pageIndex);		}	}}