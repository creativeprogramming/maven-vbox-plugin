package com.alexecollins.vbox.ant;

import org.junit.Test;

/**
 * @author alexec (alex.e.c@gmail.com)
 */
public class CleanTaskIT extends AbstractTaskTest {


	@Test
	public void testExecute() throws Exception {
		final CreateDefinitionTask defn = new CreateDefinitionTask();
		defn.setName("CentOS_6_3");
		defn.setDir(dir);
		defn.setWork("target");
		defn.execute();
		final CleanTask sut = new CleanTask();
		sut.setDir(dir);
		sut.setWork("target");
		sut.execute();
	}
}