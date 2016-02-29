package vizio.view;

public class Page {

	public String title;
	public Column[] columns;

	public Page(String title, Widget... widgets) {
		this(title, new Column[] { new Column(widgets) } );
	}

	public Page(String title, Column... columns) {
		super();
		this.title = title;
		this.columns = columns;
	}

}
