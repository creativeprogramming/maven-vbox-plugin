package com.alexecollins.util;

import com.alexecollins.vbox.core.TestContext;
import com.alexecollins.vbox.core.Work;
import org.apache.commons.io.FileUtils;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertTrue;

public class ImageUtilsTestTest {
    @Test
    public void testCreateImage() throws Exception {
        final File dest = new File("src.iso");
        ImageUtils.createImage(new Work(new TestContext()), new File("src"), dest);
        assertTrue(dest.exists());
	    FileUtils.forceDelete(dest);
    }
}
