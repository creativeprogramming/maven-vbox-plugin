package com.alexecollins.maven.plugins.vbox.mediaregistry;

import com.alexecollins.maven.plugins.vbox.AbstractTest;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.xml.bind.JAXB;

/**
 * @author alex.collins
 */
@RunWith(Parameterized.class)
public class MediaRegistryTest extends AbstractTest {

	public MediaRegistryTest(final String name) {
		super(name);
	}

	@Test
	public void testName() throws Exception {
		final MediaRegistry sut = JAXB.unmarshal(MediaRegistry.class.getResource("/" + name + "/MediaRegistry.xml"), MediaRegistry.class);

		assert sut.getDVDImages().getDVDImage() != null;
	}
}