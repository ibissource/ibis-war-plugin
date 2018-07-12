/*
   Copyright 2018 Integration Partners

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/
package org.ibissource.maven;

import org.apache.maven.AbstractMavenLifecycleParticipant;
import org.apache.maven.MavenExecutionException;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.model.Model;
import org.apache.maven.model.Plugin;
import org.apache.maven.model.PluginContainer;
import org.apache.maven.model.PluginExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.component.annotations.Component;
import org.codehaus.plexus.logging.LogEnabled;
import org.codehaus.plexus.logging.Logger;

import com.google.common.base.Strings;

/**
 * Lifecycle participant that is meant to kick in Maven3 to lessen the needed POM changes. It will silently "scan" the
 * projects, disable all executions of maven-war-plugin, and "install" itself instead.
 * 
 * TODO Add tests : https://github.com/sonatype/nexus-maven-plugins/blob/66b8287cdf4f8142ffca4716ad1faed6df6928bd/staging/maven-plugin/src/test/java/org/sonatype/nexus/maven/staging/deploy/DeployLifecycleParticipantTest.java
 * See https://github.com/sonatype/nexus-maven-plugins/blob/66b8287cdf4f8142ffca4716ad1faed6df6928bd/staging/maven-plugin/src/main/java/org/sonatype/nexus/maven/staging/deploy/DeployLifecycleParticipant.java#L148
 * 
 * @author Niels Meijer
 * @author cstamas
 */
@Component(role = AbstractMavenLifecycleParticipant.class, hint = "org.ibissource.maven.IbisLifecycleParticipant")
public class IbisLifecycleParticipant extends AbstractMavenLifecycleParticipant implements LogEnabled {

	public static String MAVEN_WAR_PLUGIN_GROUP_ID = "org.apache.maven.plugins";
	public static String MAVEN_WAR_PLUGIN_ARTIFACT_ID = "maven-war-plugin";

	public static String IBIS_WAR_PLUGIN_GROUP_ID = "org.ibissource";
	public static String IBIS_WAR_PLUGIN_ARTIFACT_ID = "ibis-war-plugin";

	private Logger logger;

	@Override
	public void enableLogging(Logger logger) {
		this.logger = logger;
	}

	@Override
	public void afterProjectsRead(MavenSession session) throws MavenExecutionException {
		int skipped = 0;

		displayIafLogo();

		for (MavenProject project : session.getProjects()) {
			final Plugin ibisWarPlugin = getIbisWarPlugin(project.getModel());
			if (ibisWarPlugin != null) {
				// skip the maven-war-plugin
				Plugin mavenWarPlugin = getMavenWarPlugin(project.getModel());
				if (mavenWarPlugin != null) {
					// TODO: better would be to remove them targeted?
					// But this mojo has only 3 goals, but only one of them is usable in builds ("deploy")
					mavenWarPlugin.getExecutions().clear();

					// add executions to ibis-war-plugin
					PluginExecution execution = new PluginExecution();
					execution.setId("injected-ibis-war-plugin");
					execution.getGoals().add("war");
					execution.setPhase("package");
					execution.setConfiguration(ibisWarPlugin.getConfiguration());
					ibisWarPlugin.getExecutions().add(execution);
	
					// count this in
					skipped++;
				}
			}
		}
		if (skipped > 0) {
			logger.info("Replaced (" + skipped + ") execution(s) of "+MAVEN_WAR_PLUGIN_ARTIFACT_ID+" with "
					+ IBIS_WAR_PLUGIN_ARTIFACT_ID);
		}
		else {
			logger.info("No executions of "+MAVEN_WAR_PLUGIN_ARTIFACT_ID+" were replaced");
		}
	}

	private void displayIafLogo() {
		logger.info("  _____            ______ ");
		logger.info(" |_   _|    /\\    |  ____|");
		logger.info("   | |     /  \\   | |__   ");
		logger.info("   | |    / /\\ \\  |  __|  ");
		logger.info("  _| |_  / ____ \\ | |     ");
		logger.info(" |_____|/_/    \\_\\|_|     ");
		logger.info("            Ibis War Plugin");
		logger.info(" ");
	}

	/**
	 * Returns the maven-war-plugin from build/plugins section of model or {@code null} if not present.
	 */
	protected Plugin getMavenWarPlugin(Model model) {
		if (model.getBuild() != null) {
			return getMavenWarPluginFromContainer(model.getBuild());
		}
		return null;
	}

	/**
	 * Returns the ibis-war-plugin from build/plugins section of model or {@code null} if not present.
	 */
	protected Plugin getIbisWarPlugin(Model model) {
		if (model.getBuild() != null) {
			return getIbisWarPluginFromContainer(model.getBuild());
		}
		return null;
	}

	/**
	 * Returns the maven-war-plugin from pluginContainer or {@code null} if not present.
	 */
	protected Plugin getMavenWarPluginFromContainer(PluginContainer pluginContainer) {
		return getPluginByGAFromContainer(MAVEN_WAR_PLUGIN_GROUP_ID, MAVEN_WAR_PLUGIN_ARTIFACT_ID, pluginContainer);
	}

	/**
	 * Returns the ibis-war-plugin from pluginContainer or {@code null} if not present.
	 */
	protected Plugin getIbisWarPluginFromContainer(PluginContainer pluginContainer) {
		return getPluginByGAFromContainer(IBIS_WAR_PLUGIN_GROUP_ID, IBIS_WAR_PLUGIN_ARTIFACT_ID, pluginContainer);
	}

	protected Plugin getPluginByGAFromContainer(String groupId, String artifactId, PluginContainer pluginContainer) {
		Plugin result = null;

		for (Plugin plugin : pluginContainer.getPlugins()) {
			logger.debug("Found plugin ["+plugin.getArtifactId()+"]");
			if (Strings.nullToEmpty(groupId).equals(Strings.nullToEmpty(plugin.getGroupId()))
					&& Strings.nullToEmpty(artifactId).equals(Strings.nullToEmpty(plugin.getArtifactId()))) {
				if (result != null) {
					throw new IllegalStateException("The build contains multiple versions of plugin " + groupId + ":"
							+ artifactId);
				}
				result = plugin;
			}

		}
		return result;
	}
}
