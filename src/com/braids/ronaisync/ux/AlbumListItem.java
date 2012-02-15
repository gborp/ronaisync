package com.braids.ronaisync.ux;

import java.awt.Color;

public class AlbumListItem implements ColoredListItem,
		Comparable<AlbumListItem> {

	public enum Type {
		SERVER_ONLY, LOCAL_ONLY, BOTH_SIDE
	}

	private boolean selected;

	private String name;

	private Type type;

	public String[] getColumnNames() {
		return new String[] { "Select", "Name" };
	}

	public Object getValue(int index) {
		if (index == 0) {
			return isSelected();
		} else if (index == 1) {
			return getName();
		}
		throw new RuntimeException("not implemented");
	}

	public String getTooltip(int column) {
		return null;
	}

	public boolean isColumnEditable(int columnIndex) {
		if (columnIndex == 0) {
			return true;
		}
		return false;
	}

	public void setValue(int index, Object value) {
		if (index == 0) {
			selected = (Boolean) value;
		}
	}

	public boolean isSelected() {
		return selected;
	}

	public void setSelected(boolean selected) {
		this.selected = selected;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Type getType() {
		return type;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Color getColor(int index) {
		switch (type) {
		case SERVER_ONLY:
			return Color.BLUE;
		case LOCAL_ONLY:
			return Color.RED;
		case BOTH_SIDE:
			return Color.BLACK;
		default:
			return null;
		}
	}

	public int compareTo(AlbumListItem o) {

		return o.getName().compareTo(getName());
	}
}
