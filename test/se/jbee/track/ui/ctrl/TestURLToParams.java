package se.jbee.track.ui.ctrl;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TestURLToParams {

	@Test
	public void user() {
		Params params = Params.fromPath("/user/");
		
		assertEquals(2, params.size());
		assertEquals(params.get(Param.viewed), "@");
		assertEquals(params.get(Param.command), Action.list.name());
	}
	
	@Test
	public void userAlias() {
		Params params = Params.fromPath("/user/foo/");
		
		assertEquals(2, params.size());
		assertEquals(params.get(Param.viewed), "foo");
		assertEquals(params.get(Param.command), Action.list.name());
	}
	
	@Test
	public void userAliasSite() {
		Params params = Params.fromPath("/user/foo/bar/");
		
		assertEquals(3, params.size());
		assertEquals(params.get(Param.viewed), "foo");
		assertEquals(params.get(Param.site), "bar");
		assertEquals(params.get(Param.command), Action.list.name());
	}
	
	@Test
	public void userAliasSiteAsRole() {
		Params params = Params.fromPath("/user/foo/bar/as/baz");
		
		assertEquals(4, params.size());
		assertEquals(params.get(Param.viewed), "foo");
		assertEquals(params.get(Param.site), "bar");
		assertEquals(params.get(Param.role), "baz");
		assertEquals(params.get(Param.command), Action.list.name());
	}

	@Test
	public void productName() {
		Params params = Params.fromPath("/product/foo/");
		
		assertEquals(2, params.size());
		assertEquals(params.get(Param.product), "foo");
		assertEquals(params.get(Param.command), Action.list.name());
	}
	
	@Test
	public void productNameArea() {
		Params params = Params.fromPath("/product/foo/bar/");
		
		assertEquals(3, params.size());
		assertEquals(params.get(Param.product), "foo");
		assertEquals(params.get(Param.area), "bar");
		assertEquals(params.get(Param.command), Action.list.name());
	}
	
	@Test
	public void productNameOriginSite() {
		Params params = Params.fromPath("/product/foo/*/bar");
		
		assertEquals(4, params.size());
		assertEquals(params.get(Param.product), "foo");
		assertEquals(params.get(Param.area), "*");
		assertEquals(params.get(Param.site), "bar");
		assertEquals(params.get(Param.command), Action.list.name());
	}
	
	@Test
	public void productNameAreaSite() {
		Params params = Params.fromPath("/product/foo/baz/bar");
		
		assertEquals(4, params.size());
		assertEquals(params.get(Param.product), "foo");
		assertEquals(params.get(Param.area), "baz");
		assertEquals(params.get(Param.site), "bar");
		assertEquals(params.get(Param.command), Action.list.name());
	}
	
	@Test
	public void productNameAreaSiteAsRole() {
		Params params = Params.fromPath("/product/foo/baz/bar/as/que");
		
		assertEquals(5, params.size());
		assertEquals(params.get(Param.product), "foo");
		assertEquals(params.get(Param.area), "baz");
		assertEquals(params.get(Param.site), "bar");
		assertEquals(params.get(Param.role), "que");
		assertEquals(params.get(Param.command), Action.list.name());
	}
	
	@Test
	public void productNameVersion() {
		Params params = Params.fromPath("/product/foo/v/0.4");
		
		assertEquals(3, params.size());
		assertEquals(params.get(Param.product), "foo");
		assertEquals(params.get(Param.version), "0.4");
		assertEquals(params.get(Param.command), Action.version.name());
	}
	
	@Test
	public void productNameTask() {
		Params params = Params.fromPath("/product/foo/146");
		
		assertEquals(3, params.size());
		assertEquals(params.get(Param.product), "foo");
		assertEquals(params.get(Param.task), "146");
		assertEquals(params.get(Param.command), Action.details.name());
	}

	@Test
	public void productNameAreaSerial() {
		Params params = Params.fromPath("/product/foo/bar/146");
		
		assertEquals(4, params.size());
		assertEquals(params.get(Param.product), "foo");
		assertEquals(params.get(Param.area), "bar");
		assertEquals(params.get(Param.serial), "146");
		assertEquals(params.get(Param.command), Action.details.name());
	}

}
