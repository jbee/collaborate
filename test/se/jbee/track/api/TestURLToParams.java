package se.jbee.track.api;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import se.jbee.track.api.Param;
import se.jbee.track.api.Param.Command;
import se.jbee.track.api.Params;

public class TestURLToParams {

	@Test
	public void user() {
		Params params = Params.fromPath("/user/");
		
		assertEquals(2, params.size());
		assertEquals(params.get(Param.viewed), "@");
		assertEquals(params.get(Param.command), Command.list.name());
	}
	
	@Test
	public void userAlias() {
		Params params = Params.fromPath("/user/foo/");
		
		assertEquals(2, params.size());
		assertEquals(params.get(Param.viewed), "foo");
		assertEquals(params.get(Param.command), Command.list.name());
	}
	
	@Test
	public void userOriginAlias() {
		Params params = Params.fromPath("/*/foo/");
		
		assertEquals(2, params.size());
		assertEquals(params.get(Param.viewed), "foo");
		assertEquals(params.get(Param.command), Command.list.name());
	}
	
	@Test
	public void userAliasPage() {
		Params params = Params.fromPath("/user/foo/bar/");
		
		assertEquals(3, params.size());
		assertEquals(params.get(Param.viewed), "foo");
		assertEquals(params.get(Param.page), "bar");
		assertEquals(params.get(Param.command), Command.list.name());
	}
	
	@Test
	public void userAliasPageAsRole() {
		Params params = Params.fromPath("/user/foo/bar/as/baz");
		
		assertEquals(4, params.size());
		assertEquals(params.get(Param.viewed), "foo");
		assertEquals(params.get(Param.page), "bar");
		assertEquals(params.get(Param.role), "baz");
		assertEquals(params.get(Param.command), Command.list.name());
	}

	@Test
	public void outputName() {
		Params params = Params.fromPath("/foo/");
		
		assertEquals(2, params.size());
		assertEquals(params.get(Param.output), "foo");
		assertEquals(params.get(Param.command), Command.list.name());
	}
	
	@Test
	public void outputNameArea() {
		Params params = Params.fromPath("/foo/bar/");
		
		assertEquals(3, params.size());
		assertEquals(params.get(Param.output), "foo");
		assertEquals(params.get(Param.area), "bar");
		assertEquals(params.get(Param.command), Command.list.name());
	}
	
	@Test
	public void outputNameOriginPage() {
		Params params = Params.fromPath("/foo/*/bar");
		
		assertEquals(4, params.size());
		assertEquals(params.get(Param.output), "foo");
		assertEquals(params.get(Param.area), "*");
		assertEquals(params.get(Param.page), "bar");
		assertEquals(params.get(Param.command), Command.list.name());
	}
	
	@Test
	public void outputNameAreaPage() {
		Params params = Params.fromPath("/foo/baz/bar");
		
		assertEquals(4, params.size());
		assertEquals(params.get(Param.output), "foo");
		assertEquals(params.get(Param.area), "baz");
		assertEquals(params.get(Param.page), "bar");
		assertEquals(params.get(Param.command), Command.list.name());
	}
	
	@Test
	public void outputNameAreaPageAsRole() {
		Params params = Params.fromPath("/foo/baz/bar/as/que");
		
		assertEquals(5, params.size());
		assertEquals(params.get(Param.output), "foo");
		assertEquals(params.get(Param.area), "baz");
		assertEquals(params.get(Param.page), "bar");
		assertEquals(params.get(Param.role), "que");
		assertEquals(params.get(Param.command), Command.list.name());
	}
	
	@Test
	public void outputNameVersion() {
		Params params = Params.fromPath("/foo/v/0.4");
		
		assertEquals(3, params.size());
		assertEquals(params.get(Param.output), "foo");
		assertEquals(params.get(Param.version), "0.4");
		assertEquals(params.get(Param.command), Command.version.name());
	}
	
	@Test
	public void outputNameTask() {
		Params params = Params.fromPath("/foo/146");
		
		assertEquals(3, params.size());
		assertEquals(params.get(Param.output), "foo");
		assertEquals(params.get(Param.task), "146");
		assertEquals(params.get(Param.command), Command.details.name());
	}

	@Test
	public void outputNameAreaSerial() {
		Params params = Params.fromPath("/foo/bar/146");
		
		assertEquals(4, params.size());
		assertEquals(params.get(Param.output), "foo");
		assertEquals(params.get(Param.area), "bar");
		assertEquals(params.get(Param.serial), "146");
		assertEquals(params.get(Param.command), Command.details.name());
	}
	
	@Test
	public void outputNameAreaDashSerial() {
		Params params = Params.fromPath("/foo/RFC-146");
		
		assertEquals(4, params.size());
		assertEquals(params.get(Param.output), "foo");
		assertEquals(params.get(Param.area), "RFC");
		assertEquals(params.get(Param.serial), "146");
		assertEquals(params.get(Param.command), Command.details.name());
	}

}
