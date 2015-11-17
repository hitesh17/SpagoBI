package it.eng.qbe.script.groovy;

import it.eng.spagobi.utilities.groovy.GroovySandboxTest;
import junit.framework.TestCase;

public class GroovyScriptAPITest extends TestCase {

	private static final String SCRIPT = "import it.eng.qbe.script.groovy.GroovyScriptAPI; a=new GroovyScriptAPI();c=a.getImage();";

	public void testGetLink() {
		GroovySandboxTest.expectException(true, SCRIPT);
		GroovySandboxTest.expectException(false, SCRIPT, GroovyScriptAPI.class);
	}

}