package com.pdftron.pdf.dialog.pdflayer;

import com.pdftron.pdf.ocg.Group;

import java.util.ArrayList;

public class PdfLayer {
    private Group group;
    private String name;
    private Boolean checked;
    private boolean isLocked;
    private boolean enabled;
    private int level;
    private ArrayList<PdfLayer> children;
    private PdfLayer parent;

    public PdfLayer(Group group, String name, Boolean checked, boolean isLocked, int level, PdfLayer parent) {
        this.group = group;
        this.name = name;
        this.checked = checked;
        this.isLocked = isLocked;
        this.enabled = true;
        this.level = level;
        this.parent = parent;
        children = new ArrayList<>();
    }

    public boolean hasChildren() {
        if (children != null && children.size() > 0) {
            return true;
        }
        return false;
    }

    public Group getGroup() {
        return group;
    }

    public void setGroup(Group group) {
        this.group = group;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Boolean isChecked() {
        return checked;
    }

    public Boolean getChecked() {
        return checked;
    }

    public void setChecked(Boolean checked) {
        this.checked = checked;
    }

    public boolean isLocked() {
        return isLocked;
    }

    public void setLocked(boolean locked) {
        isLocked = locked;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getLevel() {
        return level;
    }

    public PdfLayer getParent() {
        return parent;
    }

    public void setParent(PdfLayer parent) {
        this.parent = parent;
    }

    public ArrayList<PdfLayer> getChildren() {
        return children;
    }

    public void setChildren(ArrayList<PdfLayer> children) {
        this.children = children;
    }
}
