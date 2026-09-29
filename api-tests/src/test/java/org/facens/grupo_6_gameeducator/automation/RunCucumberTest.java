package org.facens.grupo_6_gameeducator.automation;

import io.cucumber.junit.platform.engine.Constants;
import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

/** Entry point rodado pelo Surefire: descobre e executa todas as features de automation/api-tests. */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME,
        value = "pretty, html:target/cucumber-report/cucumber-report.html, junit:target/surefire-reports/TEST-cucumber.xml")
public class RunCucumberTest {
}
