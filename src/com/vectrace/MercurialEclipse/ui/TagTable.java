/*******************************************************************************
 * Copyright (c) 2008 VecTrace (Zingo Andersen) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Jerome Negre              - implementation
 *******************************************************************************/
package com.vectrace.MercurialEclipse.ui;

import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;

import com.vectrace.MercurialEclipse.model.Tag;

/**
 * 
 * @author Jerome Negre <jerome+hg@jnegre.org>
 */
public class TagTable extends Composite {

    private final static Font PARENT_FONT = JFaceResources.getFontRegistry().getItalic(
            JFaceResources.DIALOG_FONT);

    private Table table;
    private int[] parents;
    private boolean showTip = true;

    public TagTable(Composite parent) {
        super(parent, SWT.NONE);

        this.setLayout(new GridLayout());
        this.setLayoutData(new GridData());

        table = new Table(this, SWT.SINGLE | SWT.BORDER | SWT.FULL_SELECTION | SWT.V_SCROLL
                | SWT.H_SCROLL);
        table.setLinesVisible(true);
        table.setHeaderVisible(true);
        GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
        table.setLayoutData(data);

        String[] titles = { "Rev", "Global", "Tag", "Local" };
        int[] widths = { 50, 150, 300, 70 };
        for (int i = 0; i < titles.length; i++) {
            TableColumn column = new TableColumn(table, SWT.NONE);
            column.setText(titles[i]);
            column.setWidth(widths[i]);
        }
    }

    public void hideTip() {
        this.showTip = false;
    }

    public void highlightParents(int[] newParents) {
        this.parents = newParents;
    }

    public void setTags(Tag[] tags) {
        table.removeAll();
        for (Tag tag : tags) {
            if (showTip || !"tip".equals(tag.getName())) {
                TableItem row = new TableItem(table, SWT.NONE);
                if (parents != null && isParent(tag.getRevision())) {
                    row.setFont(PARENT_FONT);
                }
                row.setText(0, Integer.toString(tag.getRevision()));
                row.setText(1, tag.getGlobalId());
                row.setText(2, tag.getName());
                row.setText(3, tag.isLocal() ? "local" : "");
                row.setData(tag);
            }
        }
    }

    public Tag getSelection() {
        TableItem[] selection = table.getSelection();
        if (selection.length == 0) {
            return null;
        }
        return (Tag) selection[0].getData();
    }

    public void addSelectionListener(SelectionListener listener) {
        table.addSelectionListener(listener);
    }

    private boolean isParent(int r) {
        switch (parents.length) {
            case 2:
                if (r == parents[1]) {
                    return true;
                }
            case 1:
                if (r == parents[0]) {
                    return true;
                }
            default:
                return false;
        }
    }

}