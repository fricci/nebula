/*******************************************************************************
 * Copyright (c) 2004, 2007 Boeing.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Boeing - initial API and implementation
 *******************************************************************************/
package org.eclipse.nebula.widgets.xviewer.customize;

import java.util.logging.Level;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.nebula.widgets.xviewer.XViewer;
import org.eclipse.nebula.widgets.xviewer.util.XViewerException;
import org.eclipse.nebula.widgets.xviewer.util.internal.XViewerLog;
import org.eclipse.swt.graphics.Image;

/**
 * Generic Label Provider showing Descriptive Name as text
 * 
 * @author Donald G. Dunne
 */
public class CustomizeDataLabelProvider implements ILabelProvider {

   private final XViewer xViewer;

   public CustomizeDataLabelProvider(XViewer xViewer) {
      this.xViewer = xViewer;
   }

   @Override
   public Image getImage(Object arg0) {
      try {
         return ((CustomizeData) arg0).getImage(xViewer.getCustomizeMgr().isCustomizationUserDefault(
            (CustomizeData) arg0));
      } catch (XViewerException ex) {
         return null;
      }
   }

   @Override
   public String getText(Object arg0) {
      try {
         CustomizeData custom = (CustomizeData) arg0;
         String text =
            (xViewer.getCustomizeMgr().isCustomizationUserDefault(custom) ? "(Default) " : "") + custom.getName();
         return text;
      } catch (XViewerException ex) {
         XViewerLog.log(CustomizeDataLabelProvider.class, Level.SEVERE, ex.toString(), ex);
         return "Exception: " + ex.getLocalizedMessage();
      }
   }

   @Override
   public void addListener(ILabelProviderListener arg0) {
      // do nothing
   }

   @Override
   public void dispose() {
      // do nothing
   }

   @Override
   public boolean isLabelProperty(Object arg0, String arg1) {
      return false;
   }

   @Override
   public void removeListener(ILabelProviderListener arg0) {
      // do nothing
   }

}
