package com.pdftron.pdf.dialog.annotlist.model;

import java.util.ArrayList;

public class AnnotationFilterColorItem extends AnnotationFilterItem {
    private ArrayList<String> allColors;
    private ArrayList<String> selectedColors;

    public AnnotationFilterColorItem(ArrayList<String> selectedColors, ArrayList<String> allColors, boolean isEnabled) {
        this.selectedColors = selectedColors;
        this.allColors = allColors;
        super.isEnabled = isEnabled;
    }

    public ArrayList<String> getSelectedColors() {
        return this.selectedColors;
    }

    public ArrayList<String> getAllColors() {
        return this.allColors;
    }
}
