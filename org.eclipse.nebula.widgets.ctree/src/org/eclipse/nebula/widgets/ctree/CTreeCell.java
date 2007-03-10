/****************************************************************************
 * Copyright (c) 2005-2006 Jeremy Dowdall
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jeremy Dowdall <jeremyd@aspencloud.com> - initial API and implementation
 *****************************************************************************/

package org.eclipse.nebula.widgets.ctree;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;

/**
 * <p>
 * NOTE:  THIS WIDGET AND ITS API ARE STILL UNDER DEVELOPMENT.  THIS IS A PRE-RELEASE ALPHA 
 * VERSION.  USERS SHOULD EXPECT API CHANGES IN FUTURE VERSIONS.
 * </p> 
 */
public class CTreeCell {

//	private class DeferredListener {
//	public int eventType;
//	public Listener handler;

//	DeferredListener(int eventType, Listener handler) {
//	this.eventType = eventType;
//	this.handler = handler;
//	}

//	public boolean equals(Object object) {
//	if(object instanceof DeferredListener) {
//	DeferredListener handler = (DeferredListener) object;
//	return (handler.eventType == this.eventType && handler.handler == this.handler);
//	}
//	return false;
//	}

//	public boolean isType(int type) {
//	return eventType == type;
//	}
//	}

	private class EventHandler {
		List[] handlers;

		boolean add(int eventType, Listener handler) {
			if(eventType > 0 && handler != null) {
				if(handlers == null) {
					handlers = new List[4];
				}
				if(eventType >= handlers.length) {
					List[] newHandlers = new List[eventType+1];
					System.arraycopy(handlers, 0, newHandlers, 0, handlers.length);
					handlers = newHandlers;
				}
				if(handlers[eventType] == null) {
					handlers[eventType] = new ArrayList();
				}
				if(!handlers[eventType].contains(handler)) {
					handlers[eventType].add(handler);
					return true;
				}
			}
			return false;
		}

		int[] getEventTypes() {
			int count = 0;
			for(int i = 0; i < handlers.length; i++) {
				if(handlers[i] != null && !handlers[i].isEmpty()) count++;
			}
			int[] types = new int[count];
			count = 0;
			for(int i = 0; i < handlers.length; i++) {
				if(handlers[i] != null && !handlers[i].isEmpty()) types[count++] = i;
			}
			return types;
		}

		boolean hasHandler(int eventType) {
			return (eventType > 0 && eventType < handlers.length &&
					handlers[eventType] != null && !handlers[eventType].isEmpty());
		}

		boolean remove(int eventType, Listener handler) {
			if(handlers != null && eventType > 0 && eventType < handlers.length && 
					handlers[eventType] != null) {
				return handlers[eventType].remove(handler);
			}
			return false;
		}

		void sendEvent(Event event) {
			if(hasHandler(event.type)) {
				event.data = CTreeCell.this;
				event.item = CTreeCell.this.item; // TODO: necessary?
				Listener[] la = (Listener[]) handlers[event.type].toArray(new Listener[handlers[event.type].size()]);
				for(int i = 0; i < la.length; i++) {
					la[i].handleEvent(event);
				}
			}
		}
	}
	
	/**
	 * true if the platform is detected as being "carbon"
	 */
	public static final boolean carbon = "carbon".equals(SWT.getPlatform());
	/**
	 * true if the platform is detected as being "gtk"
	 */
	public static final boolean gtk = "gtk".equals(SWT.getPlatform());
	/**
	 * true if the platform is detected as being "win32"
	 */
	public static final boolean win32 = "win32".equals(SWT.getPlatform());
	/**
	 * cell state indicating that all is normal
	 */
	protected static final int CELL_NORMAL 	= 0;
	/**
	 * cell state indicating that the cell is in the process of being moved
	 */
	protected static final int CELL_MOVING 	= 1 << 0;
	/**
	 * cell state indicating that the cell is in the process of being resized
	 */
	protected static final int CELL_RESIZING 	= 1 << 1;
	/**
	 * return value indicating no action is necessary
	 */
	protected static final int RESULT_NONE		= 0; 
	/**
	 * return value indicating that this cell wants to consume the event
	 * thereby preventing the delivery of the event to other cells and/or items.
	 */
	protected static final int RESULT_CONSUME	= 1 << 0; 
	/**
	 * return value indicating that a redraw is necessary
	 */
	protected static final int RESULT_REDRAW	= 1 << 1; 
	/**
	 * return value indicating that a layout is necessary
	 */
	protected static final int RESULT_LAYOUT	= 1 << 2;

	private static final Color bgOver = gtk ? Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BORDER) :
		Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);
	private static final Color bgNorm = gtk ? Display.getCurrent().getSystemColor(SWT.COLOR_WHITE) :
		Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
	private static final Color border = gtk ? Display.getCurrent().getSystemColor(SWT.COLOR_WIDGET_BORDER) :
		Display.getCurrent().getSystemColor(SWT.COLOR_DARK_GRAY);
//	private static final Color fgOver = gtk ? border :
//	Display.getCurrent().getSystemColor(SWT.COLOR_WHITE);
	private static final Color fgNorm = gtk ? border :
		Display.getCurrent().getSystemColor(SWT.COLOR_BLACK);

	private static final int[] closedPoints = gtk ? new int[] { 2,  0,  7, 5,  2, 10 } :
//		new int[] { 2,4, 4,4, 4,2, 5,2, 5,4, 7,4, 7,5, 5,5, 5,7, 4,7, 4,5, 2,5 };
		new int[] { 2,4, 6,4, 4,4, 4,2, 4,4, 4,6 };
	private static final int[] openPoints 	= gtk ? new int[] { 10, 2,  5, 7,  0, 2  } :
		new int[] { 2,4, 6,4 };//, 7,5, 2,5 };
	private static final int pointsWidth = gtk ? 11 : 8;

	/**
	 * A recursive utility function used to get every child control of a composite, 
	 * including the children of children.<br/>
	 * NOTE: This method will <b>NOT</b> return disposed children.
	 * @param c the composite to start from
	 * @param exclusions a list of controls to be excluded from the return list. If
	 * an item in this list is a composite, then its children will also be excluded.
	 * @return all the children and grandchildren of the given composite
	 */
	private static List getControls(Control c, List exclusions) {
		if(c != null && !c.isDisposed()) {
			if(exclusions == null) {
				exclusions = Collections.EMPTY_LIST;
			} else if(exclusions.contains(c)) {
				return Collections.EMPTY_LIST;
			}

			List l = new ArrayList();
			l.add(c);
			if(c instanceof Composite) {
				Control[] a = ((Composite) c).getChildren();
				for(int i = 0; i < a.length; i++) {
					if(!a[i].isDisposed() && !exclusions.contains(a[i])) {
						if(a[i] instanceof Composite) {
							l.addAll(getControls(a[i], exclusions));
						} else {
							l.add(a[i]);
						}
					}
				}
			}
			return l;
		} else {
			return Collections.EMPTY_LIST;
		}
	}
	
	
	/**
	 * the container to which this cell's item belongs
	 */
	protected CTree ctree;
	/**
	 * the item to which this cell belongs
	 */
	protected CTreeItem item;
	/**
	 * the style settings for this cell
	 */
	protected int style;
	/**
	 * the check button control, if this is a check cell
	 */
	private Button check;
	/**
	 * the control which is in the client area of this cell, if created
	 */
	private Control control;
	/**
	 * the horizontal alignment to use when positioning the control in the client area, if it was created
	 */
	int hAlign = SWT.FILL;
	/**
	 * the vertical alignment to use when positioning the control in the client area, if it was created
	 */
	int vAlign = SWT.FILL;
	/**
	 * the control which is in the child area of this cell, if created
	 */
	private Control childControl;
	/**
	 * indicates whether or not this cell is considered 'open' or 'closed'.
	 * the actual meaning may vary between concrete implementations.
	 */
	protected boolean open = false;
	/**
	 * indicates whether or not this cell is selected, and thereby included in the
	 * container's current selection list.
	 */
	protected boolean selected = false;
	/**
	 * indicates whether or not this cell is visible and should be considered for
	 * painting by the container.
	 */
	protected boolean visible = true;
	/**
	 * indicates whether or not this cell's child area is visible
	 */
	protected boolean childVisible = false;
	/**
	 * indicates whether or not this cell's toggle is visible
	 */
	protected boolean toggleVisible = false;
	/**
	 * indicates whether or not this cell is actually painted to the screen.
	 */
	private   boolean painted = false;
	/**
	 * If true, the toggle will not be drawn, but will take up space
	 */
	protected boolean ghostToggle = false;
	/**
	 * indicates whether or not the primary mouse button is down, whether or not it is over this cell.
	 */
	protected boolean mouseDown = false;
	/**
	 * indicates whether or not the mouse is over this cell.
	 */
	protected boolean mouseOver = false;
	/**
	 * indicates whether or not the mouse is over this cell's toggle.
	 */
	protected boolean mouseOverToggle = false;
	/**
	 * An amount of margin to be applied to the left side of the cell.
	 */
	public int marginLeft 	= gtk ? 2 : 1;
	/**
	 * An amount of margin to be applied to both the left and right sides of the cell.
	 */
	public int marginWidth 	= 0;
	/**
	 * An amount of margin to be applied to the right side of the cell.
	 */
	public int marginRight 	= 0;
	/**
	 * An amount of margin to be applied to the top of the cell.
	 */
	public int marginTop 	= gtk ? 0 : 0;
	/**
	 * An amount of margin to be applied to both the top and bottom of the cell.
	 */
	public int marginHeight = gtk ? 4 : 1;
	/**
	 * An amount of margin to be applied to the bottom of the cell.
	 */
	public int marginBottom = 0;

	/**
	 * sets the width of the toggle
	 */
	public int toggleWidth = 16;
	/**
	 * the bounds of this cell relative to...
	 */
	protected Rectangle bounds;
	/**
	 * the bounds of this cell the <b>last</b> time it was painted
	 */
	protected Rectangle boundsOld;
	/**
	 * the bounds of this cell's child area relative to the cell's own
	 * origin (bounds.x, bounds.y)
	 * @see #bounds
	 */
	protected Rectangle childBounds;
	/**
	 * the bounds of this cell's child area the <b>last</b> time it was painted
	 */
	protected Rectangle childBoundsOld;
	/**
	 * indicates whether or not this cell has a child area.
	 */
	private boolean hasChild;

	/**
	 * the position of the scroll bars the <b>last</b> time this cell was painted
	 */
	private Point scrollPos;
	/**
	 * the bounds for the toggle, relative to... to what? TODO
	 */
	protected Rectangle toggleBounds = new Rectangle(0,0,0,0);
	/**
	 * the font to use for any text in this cell.
	 */
	protected Font font;
	/**
	 * The color that the cell will actually use for its background.  It may be different
	 * from the storedBackground if, for instance, the cell is selected.
	 * @see #storedBackground
	 */
	protected Color activeBackground;
	/**
	 * The color that the cell will actually use for its forground.  It may be different
	 * from the storedForeground if, for instance, the cell is selected.
	 * @see #storedForeground
	 */
	protected Color activeForeground;
	/**
	 * The color that the cell would like to use for its background.
	 * @see #activeBackground
	 */
	protected Color storedBackground = null;
	/**
	 * The color that the cell would like to use for its foreground.
	 * @see #activeForeground
	 */
	protected Color storedForeground = null;

	private int cellState = CELL_NORMAL;
	
	private List colorExclusions;
	private List eventExclusions;
	private EventHandler ehandler = new EventHandler();

	private Listener l = new Listener() {
		public void handleEvent(Event event) {
			ehandler.sendEvent(event);
		}
	};

	private PropertyChangeSupport pcListeners;

	private boolean isCheck = false;

	private boolean isChecked = false;

	private Image[] images;
	private String text;

	private int horizontalSpacing = 2;
	private Rectangle[] iBounds = new Rectangle[0];
	private Rectangle tBounds = new Rectangle(0,0,0,0);

	/**
	 * the amount by which the toggle, and thus the rest of the cell, is to be indented
	 */
	protected int indent = 0;
	/**
	 * indicates whether or not this cell is to be drawn as though it were a gridline.
	 * typically only relevant to GTK.
	 */
	protected boolean isGridLine = false;

	private int[] childSpan = new int[] { -1, 1 };	// default setting keeps the child area
													// within the same column as the title area
	
	private boolean holdControl = true;
	
	private boolean holdChild = true;

	public CTreeCell(CTreeItem item, int style) {
		this.ctree = item.ctree;
		this.item = item;
		this.style = style;
		bounds = new Rectangle(0,0,0,0);
		if((style & SWT.CHECK) != 0) {
			isCheck = true;
		}
		if((style & SWT.DROP_DOWN) != 0) {
			hasChild = true;
			setChildVisible(true);
		} else {
			hasChild = false;
		}
		if((style & SWT.TOGGLE) != 0) {
			setToggleVisible(true, true);
		}
	}

	/**
	 * @param eventType
	 * @param handler
	 */
	public void addListener(int eventType, Listener handler) {
		if(ehandler.add(eventType, handler)) {
			List controls = getEventManagedControls();
			for(Iterator iter = controls.iterator(); iter.hasNext(); ) {
				Control control = (Control) iter.next();
				control.addListener(eventType, l);
			}

//			if((getStyle() & SWT.DROP_DOWN) != 0 && childArea == null) {
//			DeferredListener dl = new DeferredListener(eventType, handler);
//			boolean contains = false;
//			for(Iterator i = dlisteners.iterator(); i.hasNext(); ) {
//			if(dl.equals(i.next())) {
//			contains = true;
//			break;
//			}
//			}
//			if(!contains) dlisteners.add(dl);
//			}
		}			
	}

	/**
	 * @param listener
	 */
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		if(pcListeners == null) {
			pcListeners = new PropertyChangeSupport(this);
		}
		pcListeners.addPropertyChangeListener(listener);
	}

	/**
	 * @param propertyName
	 * @param listener
	 */
	public void addPropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if(pcListeners == null) {
			pcListeners = new PropertyChangeSupport(this);
		}
		pcListeners.addPropertyChangeListener(propertyName, listener);
	}
	
	public void clear() {
		text = null;
		images = null;
	}

	Point computeChildSize(int wHint, int hHint) {
		return null;
	}

	/**
	 * compute the size of this cell's client area; that is, compute the size
	 * of the area which is not covered by margins, toggles, or checks.
	 * @param wHint
	 * @param hHint
	 * @return a <code>Point</code> representing the computed size
	 */
	public Point computeClientSize(int wHint, int hHint) {
		Point size = new Point(0,0);
		
		Image[] images = getImages();
		Rectangle iBounds = null;
		if(images != null && images.length > 0) {
			iBounds = images[0].getBounds();
			for(int i = 1; i < images.length; i++) {
				Rectangle ib = images[i].getBounds();
				iBounds.width += horizontalSpacing + ib.width;
				iBounds.height = Math.max(iBounds.height, ib.height);
			}
		} else {
			iBounds = new Rectangle(0,0,0,0);
		}
		
		Point tSize = computeTextSize(wHint-iBounds.width, -1);

		size.x = iBounds.width + ((iBounds.width > 0 && tSize.x > 0) ? horizontalSpacing : 0) + tSize.x;
		size.y = Math.max(iBounds.height, tSize.y);

		if(open) {
			Point cSize = computeControlSize(wHint-size.x, -1);
			size.x += cSize.x;
			size.y = Math.max(size.y, cSize.y);
		}

		return size;
	}
	
	Point computeControlSize(int wHint, int hHint) {
		return (control != null) ? control.computeSize(wHint, hHint) : new Point(0,0);
	}

	/**
	 * Compute the preferred size of the cell for its current state (open or closed, if applicable)
	 * just the way it would be done in a regular SWT layout.
	 * @param wHint
	 * @param hHint
	 * @return a Point representing the preferred size of the cell
	 */
	public Point computeSize(int wHint, int hHint) {
		if(wHint == 0 || hHint == 0) return new Point(0,0);
		
		Point size = new Point(marginLeft+marginWidth+marginWidth+marginRight, marginTop+marginHeight+marginHeight+marginBottom);
		if(toggleVisible || ghostToggle) size.x += toggleWidth;
		
		Point clientSize = computeClientSize(wHint-size.x, hHint-size.y);
		if(clientSize != null) {
			size.x += clientSize.x;
			size.y += clientSize.y;
		}
		
		Point childSize = computeChildSize(wHint-size.x, hHint-size.y);
		if(childSize != null) {
			size.x += (childSize.x + (childBounds.x - bounds.x));
			size.y += (childSize.y + (childBounds.y - bounds.y));
		}
		
		if(wHint != SWT.DEFAULT) {
			size.x = Math.min(size.x, wHint);
		}
		if(hHint != SWT.DEFAULT) {
			size.y = Math.min(size.y, hHint);
		}
		
		return size;
	}

	protected Point computeTextSize(int wHint, int hHint) {
		return (text != null && text.length() > 0) ?
				internalGC().textExtent(text, SWT.DRAW_DELIMITER | SWT.DRAW_TAB | SWT.DRAW_TRANSPARENT) : 
					new Point(0,0);
	}

	/**
	 * @param control
	 * @return true if the control is part of this cell, false otherwise
	 */
	boolean contains(Control control) {
		//TODO: contains?
		return getEventManagedControls().contains(control);
	}

	private Button createCheck() {
		if(isCheck) {
			Button b = new Button(ctree.body, SWT.CHECK);
			b.setBackground(activeBackground);
			b.setForeground(activeForeground);
			b.setSelection(isChecked);
			b.addListener(SWT.FocusIn, new Listener() {
				public void handleEvent(Event event) {
					if(SWT.FocusIn == event.type) ctree.setFocus();
				}
			});
			b.addListener(SWT.Selection, new Listener() {
				public void handleEvent(Event event) {
					if(SWT.Selection == event.type) {
						isChecked = ((Button) event.widget).getSelection();
					}
				}
			});
			// TODO set size of check
			b.setSize(b.computeSize(-1, -1));
			return b;
		}
		return null;
	}

	/**
	 * Create the contents of your custom cell's Child Area here.
	 * @param parent the parent composite of your new control
	 * @return the newly created control (for multiple controls, create them inside
	 * a composite, and return the composite here)
	 */
	protected Control createChildControl(Composite parent) {
		return null;
	}

	/**
	 * Create the contents of your custom cell here
	 * @param parent the parent composite of your new control
	 * @return the newly created control (for multiple controls, create them inside
	 * a composite, and return the composite here)
	 */
	protected Control createControl(Composite parent) {
		return null;
	}

	void dispose() {
//		if(titleArea != null && !titleArea.isDisposed()) titleArea.dispose();
//		if(childArea != null && !childArea.isDisposed()) childArea.dispose();
	}

	/**
	 * @param propertyName
	 * @param oldValue
	 * @param newValue
	 */
	protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
		if(pcListeners != null) {
			pcListeners.firePropertyChange(propertyName, oldValue, newValue);
		}
	}

	/**
	 * Returns the active background color.
	 * @return Color
	 * @see #activeBackground
	 * @see #getForeground()
	 * @see #setBackground(Color)
	 */
	public Color getBackground() {
		return activeBackground;
	}

	/**
	 * Returns the bounds of this cell, relative to the container's body.
	 * @return Rectangle
	 */
	public Rectangle getBounds() {
		return new Rectangle(bounds.x, bounds.y, bounds.width, bounds.height);
	}

	/**
	 * Returns the stored background color.
	 * @return Color
	 * @see #storedBackground
	 */
	public Color getCellBackground() {
		return storedBackground;
	}

	/**
	 * Returns the stored foreground color.
	 * @return Color
	 * @see #storedForeground
	 */
	public Color getCellForeground() {
		return storedForeground;
	}

	public int getCellState() {
		return cellState;
	}

	/**
	 * Returns whether or not this cell is checked.  If the cell is not of
	 * style SWT.CHECK, false is returned.
	 * @return boolean representing the check state of the cell.
	 */
	public boolean getChecked() {
		return isChecked;
	}

	/**
	 * Get information on if and how the child area of this CTableTree will span
	 * the columns.
	 * 
	 * @return an int[] with a length of 2: int[0] represents the starting
	 *         column, and int[1] represents the number of columns, from the
	 *         start, to span.
	 * @see #setChildSpan(int, int)
	 */
	public int[] getChildSpan() {
		return childSpan;
	}

	/**
	 * Get the client area of this cell.  This is the area
	 * where controls can be placed and painting can occur by subclasses.
	 * @return Rectangle
	 */
	public Rectangle getClientArea() {
		Rectangle ca = new Rectangle(0,0,bounds.width, bounds.height);
		ca.x = marginLeft + marginWidth;
		if(toggleVisible || ghostToggle) ca.x += toggleWidth;
		ca.y = marginTop + marginHeight;
		ca.width -= (ca.x + marginRight + marginWidth);
		ca.height -= (ca.y + marginBottom + marginHeight);
		return ca;
	}

//	public CTreeItem getItem() {
//		return item;
//	}

	/**
	 * Get a list containing all of the controls whose colors (foreground and background)
	 * are managed by this cell.
	 * @return List
	 */
	protected List getColorManagedControls() {
		// TODO add child controls
		List l = new ArrayList(getControls(control, colorExclusions));
		if(check != null) l.add(check);
		return l;
	}

	/**
	 * Get a list containing all of the controls whose events (such as mouse and keyboard)
	 * are managed by this cell.
	 * @return List
	 */
	protected List getEventManagedControls() {
		// TODO add child controls
		List l = new ArrayList(getControls(control, eventExclusions));
		if(check != null) l.add(check);
		return l;
	}

	/**
	 * Get the font in use by this cell.
	 * @return Font
	 */
	public Font getFont() {
		return (font == null || font.isDisposed()) ? null : font;
	}

	/**
	 * Returns the active foreground color.
	 * @return Color
	 * @see #activeForeground
	 * @see #getBackground()
	 * @see #setForeground(Color)
	 */
	public Color getForeground() {
		return activeForeground;
	}

	public Image getImage() {
		if(images.length > 0) return images[0];
		return null;
	}

	public Image[] getImages() {
		return images == null ? new Image[0] : images;
	}

	public int getIndent() {
		return indent;
	}

	public Point getLocation() {
		Rectangle r = getBounds();
		return new Point(r.x, r.y);
	}

	public Point getSize() {
		Rectangle r = getBounds();
		return new Point(r.width, r.height);
	}

	public int getStyle() {
		return style;
	}

	public String getText() {
		return (text == null) ? "" : text;
	}
	
	Rectangle getToggleBounds() {
		return mapRectangle(toggleBounds);
	}

	boolean getToggleVisible() {
		return toggleVisible;
	}

	/**
	 * Give the Item a chance to handle the mouse event
	 * @param event the Event
	 * @param selectionActive 
	 * @return 0: do nothing, 1: redraw, 2: layout
	 */
	protected int handleMouseEvent(Event event, boolean selectionActive) {
		// TODO: handleMouseEvent uses bitwise OR'ed return code, allow "consumed" code?
		int result = RESULT_NONE;
		switch(event.type) {
		case SWT.MouseDown:
			if(bounds != null) {
				Point pt = new Point(event.x, event.y);
				boolean tmp = bounds.contains(pt);
				if(tmp != mouseOver) {
					mouseOver = tmp;
					result |= RESULT_REDRAW;
				}
				if(!mouseOver && mouseDown) {
					mouseDown = false;
					result |= RESULT_REDRAW;
				}
				tmp = toggleVisible && toggleBounds.contains(pt);
				if(tmp != mouseOverToggle) {
					mouseOverToggle = tmp;
					result |= RESULT_REDRAW;
				}
			}
			break;
		}
		return result;
	}

	void internalFirstPainting() {
		if(isCheckCell()) setCheck(true);
		if(isTreeCell()) {
			if(((CTreeItem) item).hasParentItem()) {
				setIndent(((CTreeItem) item).getParentIndent() + ctree.getTreeIndent());
			}
			setToggleVisible(((CTreeItem) item).hasItems(), true);
		}
	}

	protected GC internalGC() {
		return ctree.internalGC;
	}

	protected boolean isCellState(int opcode) {
		return ((cellState & opcode) != 0);
	}

	protected boolean isCellStateNormal() {
		return cellState == 0;
	}

	public boolean isCheckCell() {
		return ((CTreeItem) item).getCheckCell() == this;
	}
	
	public boolean isOpen() {
		return open;
	}

	public boolean isSelected() {
		return selected;
	}

	public boolean isTreeCell() {
		return ((CTreeItem) item).getTreeCell() == this;
	}

	public void layout() {}

	protected void layout(Control control) {
		Rectangle area = getClientArea();
		Point size = control.computeSize(-1, -1);
		size.x = (hAlign == SWT.FILL) ? area.width : Math.min(area.width, size.x);
		size.y = (vAlign == SWT.FILL) ? area.height : Math.min(area.height, size.y);
		control.setSize(size.x, size.y);
		locate(control);
	}

	private void layout(int eventType) {
		ctree.layout(eventType, this);
	}

	protected void layoutChild() {}

	protected void layoutChild(Control childControl) {}

	final protected void layoutInternal() {
		if(ghostToggle || toggleVisible) {
			toggleBounds.x = indent;
			toggleBounds.y = 0;
		}
		toggleBounds.width = Math.min(bounds.width, toggleWidth);
		toggleBounds.height = bounds.height;

		Rectangle clientBounds = getClientArea();
		Point clientSize = computeClientSize(clientBounds.width, clientBounds.height);

		if(images == null) {
			iBounds = new Rectangle[0];
		} else {
			iBounds = new Rectangle[images.length];
			for(int i = 0; i < iBounds.length; i++) {
				iBounds[i] = (images[i] != null && !images[i].isDisposed()) ? images[i].getBounds() : new Rectangle(0,0,1,1);
			}
		}

		int x;
		if((style & SWT.CENTER) != 0) {
			x = clientBounds.x+((clientBounds.width-clientSize.x)/2);
		} else if((style & SWT.RIGHT) != 0) {
			x = clientBounds.x+clientBounds.width-clientSize.x;
		} else { // defaults to LEFT
			x = clientBounds.x;
		}

		for(int i = 0; i < iBounds.length; i++) {
			iBounds[i].x = x;
			x += iBounds[i].width;
			if((i != iBounds.length-1) || (text.length() > 0)) x += horizontalSpacing;
		}
		
		tBounds.x = x;

		Point tSize = computeTextSize(clientBounds.width-tBounds.x, bounds.height);
		tBounds.width = tSize.x;
		tBounds.height = tSize.y;

		if((open) || (style & SWT.TOP) != 0) {
			for(int i = 0; i < iBounds.length; i++) {
				iBounds[i].y = clientBounds.y;
			}
			tBounds.y = clientBounds.y;
		} else if((style & SWT.BOTTOM) != 0) {
			toggleBounds.y = clientBounds.y + clientBounds.height - toggleBounds.height;
			for(int i = 0; i < iBounds.length; i++) {
				iBounds[i].y = clientBounds.y + clientBounds.height - iBounds[i].height;
			}
			tBounds.y = clientBounds.y + clientBounds.height - tBounds.height;
		} else { // defaults to CENTER
			toggleBounds.y = clientBounds.y + (clientBounds.height-toggleBounds.height)/2;
			for(int i = 0; i < iBounds.length; i++) {
				iBounds[i].y = clientBounds.y + (clientBounds.height-iBounds[i].height)/2;
			}
			tBounds.y = clientBounds.y + (clientBounds.height-tBounds.height)/2;
		}
	}

	protected void locate(Control control) {
		Point scroll = ctree.getScrollPosition();
		Rectangle area = getClientArea();
		Point loc = new Point(bounds.x+area.x-scroll.x, bounds.y+area.y-scroll.y);
		Point size = control.getSize();
		if(hAlign == SWT.RIGHT) loc.x += (area.width-size.x);
		else if(hAlign == SWT.CENTER) loc.x += ((area.width-size.x)/2);
		if(vAlign == SWT.BOTTOM) loc.y += (area.height-size.y);
		else if(vAlign == SWT.CENTER) loc.y += ((area.height-size.y)/2);
		control.setLocation(loc);
	}

	protected void locateCheck(Button check) {
		Point scroll = ctree.getScrollPosition();
		Rectangle area = getClientArea();
		check.setLocation(
				bounds.x+area.x-scroll.x,
				bounds.y+-scroll.y+(bounds.height-check.computeSize(-1, -1).y)/2
				);
	}

	//	protected abstract void internalLocateChild(Control control) {}
	protected void locateChild() {}

	// TODO: map methods?
	Rectangle mapRectangle(int x, int y, int width, int height) {
		Rectangle r = new Rectangle(x,y,width,height);
		r.x += bounds.x;
		r.y += bounds.y;
		return r;
	}

	Rectangle mapRectangle(Rectangle rect) {
		return mapRectangle(rect.x, rect.y, rect.width, rect.height);
	}

	/**
	 * @param gc the GC upon which the cell will be painted
	 * @param ebounds the bounding rectangle of the Container's paint event
	 */
	void paint(GC gc, Rectangle ebounds) {
		bounds.width = item.getCellColumn(this).getWidth();
		if(!painted || bounds.isEmpty()) return;

		updateColors();

		if(boundsOld == null) boundsOld = new Rectangle(-1,-1,0,0);
		if(scrollPos == null) scrollPos = new Point(-1,-1);

		bounds.x = item.getCellColumn(this).getLeft();

		boolean didLayout = false;
		if(bounds.width != boundsOld.width || bounds.height != boundsOld.height) {
			layoutInternal();
			if(check != null) locateCheck(check);
			layout();
			if(control != null) layout(control);
			boundsOld.x = bounds.x;
			boundsOld.y = bounds.y;
			boundsOld.width = bounds.width;
			boundsOld.height = bounds.height;
			didLayout = true;
		}

		Point newSPos = ctree.getScrollPosition();
		if(!didLayout && (bounds.x != boundsOld.x || bounds.y != boundsOld.y || 
				scrollPos.x != newSPos.x || scrollPos.y != newSPos.y)) {
			if(!gtk) {
				if(check != null) locateCheck(check);
				if(control != null) locate(control);
			}
			boundsOld.x = bounds.x;
			boundsOld.y = bounds.y;
		}
		if(hasChild && open) {
			if(childControl == null) childControl = createChildControl(ctree);
			didLayout = false;
			if(childBounds.width != childBoundsOld.width || childBounds.height != childBoundsOld.height) {
				layoutChild();
				if(childControl != null) layoutChild(childControl);
				childBoundsOld.x = childBounds.x;
				childBoundsOld.y = childBounds.y;
				childBoundsOld.width = childBounds.width;
				childBoundsOld.height = childBounds.height;
				didLayout = true;
			}
			if(!didLayout && (childBounds.x != childBoundsOld.x || childBounds.y != childBoundsOld.y || 
					scrollPos.x != newSPos.x || scrollPos.y != newSPos.y)) {
				locateChild();
//				if(childControl != null) locateChild(childControl);
				childBoundsOld.x = childBounds.x;
				childBoundsOld.y = childBounds.y;
			}
		}
		scrollPos = newSPos;

		if(gtk) {
			paintCell(gc, new Point(bounds.x-ebounds.x,bounds.y-ebounds.y));
		} else {
			paintCell(gc, new Point(bounds.x-scrollPos.x-ebounds.x,bounds.y-scrollPos.y-ebounds.y));
		}
	}

	public void paintCell(GC gc, Point offset) {
		if(activeBackground != null) gc.setBackground(activeBackground);
		if(activeForeground != null) gc.setForeground(activeForeground);

		// background
		gc.fillRectangle(
				offset.x,
				offset.y,
				bounds.width,
				bounds.height
		);

		// images
		for(int i = 0; i < iBounds.length; i++) {
			if(!images[i].isDisposed()) {
				gc.drawImage(images[i], offset.x+iBounds[i].x, offset.y+iBounds[i].y);
			}
		}

		// text
		if(getText().length() > 0) {
			Font bf = gc.getFont();
			if(getFont() != null) gc.setFont(getFont());
			gc.drawText(getText(), offset.x+tBounds.x, offset.y+tBounds.y);
			if(getFont() != null) gc.setFont(bf);
		}

		if(((CTreeItem) item).getTreeCell() == this) {
			paintChildLines(gc, offset);
		}
		
		// toggle (it changes the colors again so paint it last...)
		if(toggleVisible) {
			paintToggle(gc, offset);
		}
	}

	private void paintChildLines(GC gc, Point offset) {
		if(win32) {
			int gline = ctree.getGridLineWidth();
			Rectangle ibounds = item.getBounds();
			ibounds.y++;
			Rectangle tbounds = getBounds();
			tbounds.y++;
			int x1 = 0, x2 = 0, y1 = 0, y2 = 0;
			int x = toggleBounds.x + (toggleBounds.width/2) - offset.x;
			int y = tbounds.y + (tbounds.height/2) - offset.y;
			gc.setForeground(ctree.getDisplay().getSystemColor(SWT.COLOR_DARK_GRAY));
			gc.setLineDash(new int[] { 1, 1 });
			x1 = x;
			x2 = x+toggleBounds.width/2;
			y1 = y;
			y2 = y;
			if(gline % 2 == 0) {
				if(y1 % 2 == 1) y1 -= 1;
				if(y2 % 2 == 1) y2 -= 1;
			}
			gc.drawLine(x1, y1, x2, y2);
			CTreeItem it = (CTreeItem) item;
			int index = Arrays.asList(
					it.hasParentItem() ? 
							it.getParentItem().getItems() :
								ctree.getItems()
								).indexOf(it);
			int count = it.hasParentItem() ? 
					it.getParentItem().getItemCount() : 
						ctree.getItemCount();
			if(index > 0 || it.hasParentItem()) {
				x1 = x;
				x2 = x;
				y1 = ibounds.y - offset.y;
				y2 = y;
				if(gline % 2 == 0) {
					if(y1 % 2 == 1) y1 -= 1;
					if(y2 % 2 == 1) y2 -= 1;
				}
				gc.drawLine(x1, y1, x2, y2);
			}
			if(index < count - 1) {
				x1 = x;
				x2 = x;
				y1 = y;
				y2 = (it.hasParentItem() ?
						it.getParentItem().getItem(index+1).getCellBounds()[0].y :
							ctree.getItem(index+1).getCellBounds()[0].y)
							- offset.y;
				if(gline % 2 == 0) {
					if(y1 % 2 == 1) y1 -= 1;
					if(y2 % 2 == 1) y2 -= 1;
				}
				gc.drawLine(x1, y1, x2, y2);
			}
			x1 = x;
			x2 = x;
			y1 = ibounds.y - offset.y;
			y2 = ibounds.y+ibounds.height - offset.y;
			if(gline % 2 == 0) {
				if(y1 % 2 == 1) y1 -= 1;
				if(y2 % 2 == 1) y2 -= 1;
			}
			while(it.hasParentItem()) {
				x1 = x2 -= ((CTree) ctree).getTreeIndent();
				it = it.getParentItem();
				index = Arrays.asList(
						it.hasParentItem() ? 
								it.getParentItem().getItems() :
									ctree.getItems()
									).indexOf(it);
				count = it.hasParentItem() ? 
						it.getParentItem().getItemCount() : 
							ctree.getItemCount();
				if(index < count - 1) {
					gc.drawLine(x1, y1, x2, y2);
				}
			}
			if(open && ((CTreeItem) item).hasItems()) {
				x1 = x2 = x + ((CTree) ctree).getTreeIndent();
				y1 = tbounds.y + tbounds.height - offset.y;
				y2 = ibounds.y + ibounds.height - offset.y;
				if(gline % 2 == 0) {
					if(y1 % 2 == 1) y1 -= 1;
					if(y2 % 2 == 1) y2 -= 1;
				}
				gc.drawLine(x1, y1, x2, y2);
			}
			gc.setLineDash(null);
		}
	}

	protected void paintToggle(GC gc, Point offset) {
		double x = ((toggleBounds.width - pointsWidth) / 2) + offset.x;
		double y = ((toggleBounds.height - pointsWidth) / 2) + toggleBounds.y + offset.y;
		int[] data = open ? openPoints : closedPoints;
		int[] pts = new int[data.length];
		for (int i = 0; i < data.length; i += 2) {
			pts[i] = data[i] + (int) x;
			pts[i+1] = data[i+1] + (int) y;
		}

		if(gtk){
			gc.setForeground(border);
			gc.setBackground((mouseOverToggle && !mouseDown) ? bgOver : bgNorm);
			gc.setAntialias(SWT.ON);
			gc.fillPolygon(pts);
			gc.drawPolygon(pts);
			gc.setAdvanced(false);
		} else if(carbon) {
			// TODO: carbon toggle
			gc.setBackground(ctree.getDisplay().getSystemColor(SWT.COLOR_GRAY));
			gc.setAntialias(SWT.ON);
			gc.fillPolygon(pts);
			gc.setAdvanced(false);
		} else {
			gc.setBackground(bgNorm);
			gc.fillRoundRectangle(
					(int) x,
					(int) y,
					pointsWidth,
					pointsWidth,
					3, 3);
			Color color = new Color(ctree.getDisplay(), 223, 229, 234);
			gc.setForeground(color);
			gc.drawLine(
					(int) x,
					(int) y + pointsWidth-2,
					(int) x + pointsWidth,
					(int) y + pointsWidth-2
			);
			gc.drawLine(
					(int) x + pointsWidth-1,
					(int) y,
					(int) x + pointsWidth-1,
					(int) y + pointsWidth-2
			);
			gc.drawLine(
					(int) x + pointsWidth-2,
					(int) y + pointsWidth-3,
					(int) x + pointsWidth-2,
					(int) y + pointsWidth-3
			);
			color.dispose();
			color = new Color(ctree.getDisplay(), 196, 206, 216);
			gc.setForeground(color);
			color.dispose();
			gc.drawLine(
					(int) x,
					(int) y + pointsWidth-1,
					(int) x + pointsWidth,
					(int) y + pointsWidth-1
			);
			gc.setForeground(border);
			gc.drawRoundRectangle(
					(int) x,
					(int) y,
					pointsWidth,
					pointsWidth,
					3, 3);
			gc.setForeground(fgNorm);
			gc.drawPolyline(pts);
		}

	}

	public void redraw() {
		ctree.redraw(this);
	}

	public void removeListener(int eventType, Listener handler) {
		if(ehandler.remove(eventType, handler)) {
			List controls = getEventManagedControls();
			for(Iterator iter = controls.iterator(); iter.hasNext(); ) {
				Control control = (Control) iter.next();
				control.removeListener(eventType, handler);
			}

//			if((getStyle() & SWT.DROP_DOWN) != 0) {
//			DeferredListener h = new DeferredListener(eventType, handler);
//			for(Iterator i = dlisteners.iterator(); i.hasNext(); ) {
//			if(h.equals(i.next())) i.remove();
//			}
//			}
		}
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		if(pcListeners != null) {
			pcListeners.removePropertyChangeListener(listener);
		}
	}

	public void removePropertyChangeListener(String propertyName, PropertyChangeListener listener) {
		if(pcListeners != null) {
			pcListeners.removePropertyChangeListener(propertyName, listener);
		}
	}
	protected void restoreState(Map memento) {
		// base implementation so subclass do not need to override
	}

	protected Map retrieveState() {
		return Collections.EMPTY_MAP;
	}
	public void setBackground(Color color) {
		storedBackground = color;
		updateColors();
	}
	protected void setBounds(int x, int y, int width, int height) {
		bounds.x = x;
		bounds.y = y;
		bounds.width = width;
		bounds.height = height;
		snap(bounds);
	}

//	public Control getControl() { return control; }
	
	protected void setBounds(Rectangle bounds) {
		setBounds(bounds.x,bounds.y,bounds.width,bounds.height);
	}

	protected void setCellStateFlag(int flag, boolean on) {
		if(on) {
			cellState |= flag;
		} else {
			cellState &= ~flag;
		}
	}

	public void setCheck(boolean check) {
		if(isCheck != check) {
			isCheck = check;
			if(!check) {
				isChecked = false;
				if(this.check != null) {
					this.check.dispose();
					this.check = null;
				}
			}
		}
	}

	public void setChecked(boolean checked) {
		if(isCheck) isChecked = checked;
	}

	/**
	 * Set which columns the Child Area of this CTableTreeCell will span.<br />
	 * The default setting is: start == -1 and len == 1. To span the entire
	 * CTableTree (all columns), then use <code>setChildSpan(0, -1)</code>.
	 * 
	 * @param start
	 *            the column in which the child area will begin. A value of -1
	 *            indicates that the child area should begin in the same column
	 *            as its title area (same cell).
	 * @param len
	 *            how many columns, starting with the one specified by 'start',
	 *            the child area will span. A value of '-1' indicates that the
	 *            child area should span all the way to the end of the last
	 *            column.
	 * @see #getChildSpan()
	 */
	public void setChildSpan(int start, int len) {
		childSpan[0] = start;
		childSpan[1] = len;
	}

	public void setChildVisible(boolean visible) {
		childVisible = visible;
		updateVisibility();
	}

	protected void setControlLayoutData(int horizontalAlignment, int verticalAlignment) {
		hAlign = horizontalAlignment;
		vAlign = verticalAlignment;
	}

	protected void setCursor(int id) {
		ctree.setCursor(ctree.getDisplay().getSystemCursor(id));
	}
	protected void setExclusions(Control exclude) {
		colorExclusions = eventExclusions = Collections.singletonList(exclude);
	}
	
	protected void setExclusions(List colors, List events) {
		colorExclusions = colors;
		eventExclusions = events;
	}

	public boolean setFocus() {
		// TODO setFocus not yet implemented
		return false;
	}
	
	public void setFont(Font font) {
		this.font = font;
//		TODO: redraw();
	}

	public void setForeground(Color color) {
		storedForeground = color;
		updateColors();
	}

	public void setGridLine(boolean isGridLine) {
		if(this.isGridLine != isGridLine) {
			this.isGridLine = isGridLine;
			updateColors();
		}
	}

	public void setImage(Image image) {
		if(image == null) images = new Image[0];
		else images = new Image[] { image };
	}

//	private boolean isClear() {
//		return (text == null || text.length() == 0) && (images == null || images.length == 0);
//	}

	public void setImages(Image[] images) {
		if(images == null) {
			this.images = new Image[0];
		}
		else {
			boolean doit = true;
			for(int i = 0; i < images.length; i++) {
				if(images[i] == null || images[i].isDisposed()) doit = false;
			}
			if(doit) this.images = images;
		}
		ctree.redraw(this);
	}
	
	public void setIndent(int indent) {
		this.indent = indent;
	}
	
	/**
	 * Requests the cell to either open or close
	 * <p>The base implementation creates the Child Area the first time
	 * setOpen(true) is called (if, of course, the cell was created with 
	 * the SWT.DROP_DOWN style).</p>
	 * <p>What exactly open refers to can be overridden by subclasses</p>
	 * @param open if true, the cell will open, otherwise it will close
	 */
	public void setOpen(boolean open) {
		if(this.open != open) {
			this.open = open;
			if(hasChild) {
				// TODO: setOpen - childArea
//				if(childArea == null) {
//				childArea = new SComposite(container, SComposite.NONE);
//				container.addPaintedItemListener(getPaintedItemListener());
//				createChildContents(childArea, getStyle());
//				// TODO: review...
//				int[] types = ehandler.getEventTypes();
//				for(int i = 0; i < types.length; i++) {
//				List controls = getEventManagedControls();
//				for(Iterator iter = controls.iterator(); iter.hasNext(); ) {
//				Control control = (Control) iter.next();
//				control.addListener(types[i], l);
//				}
//				}
//				updateColors();
//				update();
//				}

				updateVisibility();
			}
			layout(SWT.Resize);
		}
	}
	
	void setPainted(boolean painted) {
		if(this.painted != painted) {
			this.painted = painted;
			if(painted) {
				if(check == null) check = createCheck();
				if(control == null) {
					control = createControl(ctree.body);
				} else {
					control.setVisible(true);
				}
			} else {
				boundsOld = null;
				scrollPos = null;
				if(check != null) {
					check.dispose();
					check = null;
				}
				if(control != null) {
					if(holdControl) {
						control.setVisible(false);
					} else {
						control.dispose();
						control = null;
					}
				}
				if(childControl != null) {
					if(holdChild) {
						childControl.setVisible(painted);
					} else {
						childControl.dispose();
						childControl = null;
					}
				}
			}
		}
	}

	/**
	 * Set the selection state of this cell.
	 * @param selected true if selected, false otherwise
	 */
	void setSelected(boolean selected) {
		this.selected = selected;
		updateColors();
	}

	public void setText(String string) {
		if(string != null && !string.equals(getText())) {
			text = string;
			ctree.redraw(this);
		}
	}
	
	/**
	 * Sets the visibility of the cell's Toggle
	 * @param visible
	 */
	public void setToggleVisible(boolean visible) {
		toggleVisible = visible;
		updateVisibility();
	}
	
	/**
	 * Sets the visibility of the cell's Toggle
	 * <p>If ghost is true, then the toggle will occupy space even when not visible
	 *  - this is especially usefull for building trees</p>
	 * @param visible
	 * @param ghost
	 */
	public void setToggleVisible(boolean visible, boolean ghost) {
		toggleVisible = visible;
		ghostToggle = ghost;
		updateVisibility();
	}

	/**
	 * Sets the visibility of the cell
	 * @param visible
	 */
	public void setVisible(boolean visible) {
		this.visible = visible;
		updateVisibility();
	}

	/**
	 * Called during setBounds. To be implemented in sub-classes
	 * @param bounds
	 */
	protected void snap(Rectangle bounds) {
		// subclasses to implement
	}

//	public void setOpen(boolean open) {
//		if(((CTreeItem) item).getTreeCell() == this) {
//			if(this.open != open) {
//				this.open = open;
//				container.layout(open ? SWT.Expand : SWT.Collapse, item);
//			}
//		} else {
//			super.setOpen(open);
//		}
//	}
	
	/**
	 * Updates this cell with its item's data object, as returned by
	 * item.getData(). If getData() returns null, then this method simply returns false.
	 * @return true if this cell (and thus maybe the container) needs its layout updated
	 * @see #update(Object, String[])
	 */
	public boolean update() {
		Object obj = null;
		if(!item.isDisposed()) {
			obj = item.getData();
		}
		return (obj == null) ? false : update(obj, null);
	}

	/**
	 * Updates the given properties of this CTableCell with the given element
	 * <p>Functionality is only provided for a basic cell, subclasses should override</p>
	 * @param element the data to use in updating the cell
	 * @param properties the properties of the cell to be updated
	 * @return true if this cell (and thus maybe its container) needs its layout updated,
	 * actually calling layout on the cell and/or the CContainer is left up to the caller so that
	 * performance improvements can be made - for instance, if several cells are updated, the CContainer
	 * need only be updated once.
	 */
	public boolean update(Object element, String[] properties) {
		// TODO rework the JFace style 'update' functionality
		return false;
	}
	
	/**
	 * Update the background and foreground colors for the cell and any contained
	 * controls.  This method should be called whenever something may change the color
	 * of the cell, such as a change in selection state.
	 */
	protected void updateColors() {
		Color back;
		Color fore;
		if(selected) {
			back = item.ctree.getColors().getItemBackgroundSelected();
			fore = item.ctree.getColors().getItemForegroundSelected();
		} else if(isGridLine){
			back = (storedBackground != null) ? storedBackground : item.ctree.getColors().getGrid();
			fore = (storedForeground != null) ? storedForeground : item.ctree.getColors().getItemForegroundNormal();
		} else {
			back = (storedBackground != null) ? storedBackground : item.ctree.getColors().getItemBackgroundNormal();
			fore = (storedForeground != null) ? storedForeground : item.ctree.getColors().getItemForegroundNormal();
		}

		activeBackground = back;
		activeForeground = fore;

		for(Iterator iter = getColorManagedControls().iterator(); iter.hasNext(); ) {
			Control c = (Control) iter.next();
			c.setBackground(activeBackground);
		}
	}
	
	private void updateVisibility() {
		// TODO: redo updateVisibility
//		if(titleArea != null && !titleArea.isDisposed()) {
//		List controls = getControls(titleArea);
//		for(Iterator i = controls.iterator(); i.hasNext(); ) {
//		((Control) i.next()).setVisible(titleVisible&&visible);
//		}
//		titleArea.setVisible(titleVisible&&visible);
//		}
//		if(childArea != null && !childArea.isDisposed()) {
//		List controls = getControls(childArea);
//		for(Iterator i = controls.iterator(); i.hasNext(); ) {
//		((Control) i.next()).setVisible(childVisible&&open&&visible);
//		}
//		childArea.setVisible(childVisible&&visible);
//		}
	}
}
