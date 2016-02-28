package vizio.view;

public class Page {

	public String title;
	public Widget[] left;
	public Widget[] right;

	public Page(String title, Widget... left) {
		this(title, left, new Widget[0]);
	}
	
	public Page(String title, Widget[] left, Widget[] right) {
		super();
		this.title = title;
		this.left = left;
		this.right = right;
	}
	
}
