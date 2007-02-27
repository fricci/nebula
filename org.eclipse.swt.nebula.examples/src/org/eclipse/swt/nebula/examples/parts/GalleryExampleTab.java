package org.eclipse.swt.nebula.examples.parts;

import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.nebula.widgets.gallery.AbstractGridGroupRenderer;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryGroupRenderer;
import org.eclipse.nebula.widgets.gallery.DefaultGalleryItemRenderer;
import org.eclipse.nebula.widgets.gallery.Gallery;
import org.eclipse.nebula.widgets.gallery.GalleryItem;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.nebula.examples.AbstractExampleTab;
import org.eclipse.swt.nebula.examples.ExamplesView;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Scale;

public class GalleryExampleTab extends AbstractExampleTab {
	Image eclipseImage = null;

	Gallery g = null;

	DefaultGalleryItemRenderer itemRenderer = null;

	AbstractGridGroupRenderer groupRenderer = null;

	public Control createControl(Composite parent) {
		int style = SWT.V_SCROLL | SWT.MULTI;

		g = new Gallery(parent, style);
		groupRenderer = new DefaultGalleryGroupRenderer();
		groupRenderer.setAutoMargin(true);
		g.setGroupRenderer(groupRenderer);

		itemRenderer = new DefaultGalleryItemRenderer();
		itemRenderer.setShowLabels(true);
		itemRenderer.setDropShadowsSize(5);
		itemRenderer.setDropShadows(false);
		g.setItemRenderer(itemRenderer);

		// Create item iamge
		eclipseImage = ExamplesView.getImage("icons/woman3.png");

		// Add items.
		this.clearAndPopulateGallery(g);

		return g;
	}

	private void clearAndPopulateGallery(Gallery g) {
		g.clearAll();

		if ((g.getStyle() & SWT.VIRTUAL) == 0) {
			this.populateGalleryWithGroups(g);
		} else {
			// Virtual mode.
			// TODO: Virtual mode exemaple
		}
	}

	/**
	 * Add 10 groups containing 50 items each.
	 * 
	 * @param g
	 */
	private void populateGalleryWithGroups(Gallery g) {
		for (int i = 0; i < 10; i++) {
			GalleryItem gi1 = new GalleryItem(g, SWT.None);
			gi1.setImage(eclipseImage);
			gi1.setText("Group " + i + ".jpg");
			gi1.setDescription("Groupe");
			if (i % 2 == 0)
				gi1.setExpanded(true);
			for (int j = 0; j < (10 * (i + 1)); j++) {
				GalleryItem gi2 = new GalleryItem(gi1, SWT.None);
				gi2.setImage(eclipseImage);
				gi2.setText("Eclipse " + i + " " + j + ".jpg");
				gi2.setDescription("Image");
			}
		}
	}

	public String[] createLinks() {
		String[] links = new String[3];

		links[0] = "<a href=\"http://www.eclipse.org/nebula/widgets/gallery/gallery.php\">Gallery Home Page</a>";

		links[1] = "<a href=\"http://www.eclipse.org/nebula/widgets/gallery/snippets.php\">Snippets</a>";

		links[2] = "<a href=\"https://bugs.eclipse.org/bugs/buglist.cgi?query_format=advanced&short_desc_type=allwordssubstr&short_desc=&classification=Technology&product=Nebula&component=Gallery&long_desc_type=allwordssubstr&long_desc=&bug_file_loc_type=allwordssubstr&bug_file_loc=&status_whiteboard_type=allwordssubstr&status_whiteboard=&keywords_type=allwords&keywords=&emailtype1=substring&email1=&emailtype2=substring&email2=&bugidtype=include&bug_id=&votes=&chfieldfrom=&chfieldto=Now&chfieldvalue=&cmdtype=doit&order=Reuse+same+sort+as+last+time&field0-0-0=noop&type0-0-0=noop&value0-0-0=\">Bugs</a>";

		return links;

	}

	Scale scale = null;

	Scale itemWidthScale = null;

	Scale itemHeightScale = null;

	Scale marginsScale = null;

	public void createParameters(Composite parent) {
		GridLayoutFactory.swtDefaults().margins(0, 0).numColumns(3).applyTo(parent);

		// Scale : set item size
		scale = createScale(parent, "Item size", 16, 512, 16);
		scale.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent arg0) {
				if (g != null) {
					groupRenderer.setItemSize(scale.getSelection(), scale.getSelection());
					itemWidthScale.setSelection(scale.getSelection());
					itemHeightScale.setSelection(scale.getSelection());
					g.setGroupRenderer(groupRenderer);
				}
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

		});

		// Scale : set item width
		this.itemWidthScale = createScale(parent, "Item width", 16, 512, 16);
		itemWidthScale.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent arg0) {
				if (g != null) {
					groupRenderer.setItemWidth(itemWidthScale.getSelection());
					g.setGroupRenderer(groupRenderer);
				}
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

		});

		// Scale : set item height
		this.itemHeightScale = createScale(parent, "Item height", 16, 512, 16);
		itemHeightScale.addSelectionListener(new SelectionListener() {

			public void widgetSelected(SelectionEvent arg0) {
				if (g != null) {
					groupRenderer.setItemHeight(itemHeightScale.getSelection());
					g.setGroupRenderer(groupRenderer);
				}
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {
			}

		});

		// Scale : set margins size
		this.marginsScale = createScale(parent, "Margins", 0, 128, 16);
		marginsScale.addSelectionListener(new SelectionListener() {
			public void widgetSelected(SelectionEvent arg0) {
				if (g != null) {
					groupRenderer.setMinMargin(marginsScale.getSelection());
					g.setGroupRenderer(groupRenderer);
				}
			}

			public void widgetDefaultSelected(SelectionEvent arg0) {
			}
		});

	}

	private Scale createScale(Composite parent, String text, int min, int max, int increment) {
		GridData gridData = new GridData();

		Label l = new Label(parent, SWT.NONE);
		l.setText(text);
		gridData.horizontalSpan = 1;
		l.setLayoutData(gridData);

		Scale scale = new Scale(parent, SWT.NONE);
		gridData = new GridData();
		gridData.horizontalAlignment = GridData.FILL;
		gridData.grabExcessHorizontalSpace = true;
		gridData.horizontalSpan = 2;
		scale.setLayoutData(gridData);

		scale.setMaximum(max);
		scale.setMinimum(min);
		scale.setPageIncrement(increment);

		return scale;
	}
}