package com.alexecollins.vbox.maven;

import com.alexecollins.vbox.core.task.CreateDefinition;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;

import java.io.File;

/**
 * Create a new definition from a template. These are created in src/main/vbox.
 *
 * @author alexec (alex.e.c@gmail.com)
 * @goal create-definition
 * @see ListDefinitionsMojo
 */
public class CreateDefinitionMojo extends AbstractVBoxMojo {

	/**
	 * The name of the template to use, e.g. "CentOS_6_3". Defaults to the name for backwards compatibility.
	 *
	 * @parameter property="vbox.template", default-value="${vbox.name}"
	 */
	private String templateName;

	/**
	 * The name of the template to use, e.g. "app1".
	 *
	 * @parameter property="vbox.name"
	 * @required
	 */
	private String name;

	public void execute() throws MojoExecutionException, MojoFailureException {

		try {
			new CreateDefinition(name, new File("src/main/vbox/" + (templateName != null ? templateName : name))).call();
		} catch (Exception e) {
			throw new MojoExecutionException("failed to create definition", e);
		}
	}
}
