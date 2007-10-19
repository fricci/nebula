/*******************************************************************************
 * Copyright (c) Emil Crumhorn - Hexapixel.com - emil.crumhorn@gmail.com
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    emil.crumhorn@gmail.com - initial API and implementation
 *******************************************************************************/ 

package org.eclipse.nebula.widgets.ganttchart;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

/**
 * Shows any text right above an event (or any location really, but that's the idea) and does not dispose the shell
 * until told to do so. Any recurring calls simply re-position the shell.
 */
class GanttDateTip {

    private static Shell shell;
    private static int yLoc;
    private static CLabel label;

    public static void makeDialog(IColorManager colorManager, String text, Point location, int marker) {    	
        if (shell != null && shell.isDisposed() == false) {
            location = new Point(location.x, location.y);

            // move shell to new location
            shell.setLocation(location.x, location.y);

            // update text
            if (yLoc == marker) {
                label.setText(text);
                return;
            }

            shell.dispose();
        }

        yLoc = marker;

        shell = new Shell(Display.getDefault().getActiveShell(), SWT.ON_TOP);
        RowLayout rl = new RowLayout(); 
        rl.marginLeft = 1;
        rl.marginRight = 1;
        rl.marginTop = 1;
        rl.marginBottom = 1;
        
        shell.setBackground(colorManager.getTooltipBackgroundColor());
        shell.setLayout(rl);
        
        Composite comp = new Composite(shell, SWT.NULL);
        comp.setBackground(colorManager.getTooltipBackgroundColor());

        GridLayout fl = new GridLayout();
        fl.numColumns = 1;
        fl.marginHeight = 0;
        fl.marginWidth = 0;
        comp.setLayout(fl);

        label = new CLabel(comp, SWT.LEFT);
        label.setBackground(colorManager.getTooltipBackgroundColor());
        label.setForeground(colorManager.getTooltipForegroundColor());
        label.setText(text);

        shell.pack();

        // show above code inside, automatically below otherwise
        location = new Point(location.x, location.y);

        shell.setLocation(location);
        shell.setVisible(true);
    }

    public static void kill() {
        if (shell != null && shell.isDisposed() == false) {
            shell.dispose();
        }
    }

    public static boolean isActive() {
        return (shell != null && !shell.isDisposed());
    }
}