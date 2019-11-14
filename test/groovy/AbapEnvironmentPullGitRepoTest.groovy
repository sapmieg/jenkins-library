import java.util.Map
import static org.hamcrest.Matchers.hasItem
import static org.junit.Assert.assertThat

import org.hamcrest.Matchers
import static org.hamcrest.Matchers.containsString
import static org.hamcrest.Matchers.equalTo
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.ExpectedException
import org.junit.rules.RuleChain

import util.BasePiperTest
import util.JenkinsCredentialsRule
import util.JenkinsStepRule
import util.JenkinsLoggingRule
import util.JenkinsReadYamlRule
import util.JenkinsShellCallRule
import util.Rules

import hudson.AbortException

public class AbapEnvironmentPullGitRepoTest extends BasePiperTest {

    private ExpectedException thrown = new ExpectedException()
    private JenkinsStepRule stepRule = new JenkinsStepRule(this)
    private JenkinsLoggingRule loggingRule = new JenkinsLoggingRule(this)
    private JenkinsShellCallRule shellRule = new JenkinsShellCallRule(this)
    private JenkinsCredentialsRule credentialsRule = new JenkinsCredentialsRule(this).withCredentials('test_credentialsId', 'user', 'password')

    @Rule
    public RuleChain ruleChain = Rules.getCommonRules(this)
        .around(new JenkinsReadYamlRule(this))
        .around(thrown)
        .around(stepRule)
        .around(loggingRule)
        .around(credentialsRule)
        .around(shellRule)

    @Before
    public void setup() {
    }

    @Test
    public void pullSuccessful() {
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*x-csrf-token: fetch.*/, null )
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*POST.*/, /{"d" : { "__metadata" : { "uri" : "https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/sap\/opu\/odata\/sap\/MANAGE_GIT_REPOSITORY\/Pull\/URI" } , "status" : "R", "status_descr" : "RUNNING" }}/)
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/sap\/opu\/odata\/sap\/MANAGE_GIT_REPOSITORY\/Pull\/URI.*/, /{"d" : { "__metadata" : { "uri" : "https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/sap\/opu\/odata\/sap\/MANAGE_GIT_REPOSITORY\/Pull\/URI" } , "status" : "S", "status_descr" : "SUCCESS" }}/)

        helper.registerAllowedMethod("readFile", [String.class], { 
            /HTTP\/1.1 200 OK
            set-cookie: sap-usercontext=sap-client=100; path=\/
            content-type: application\/json; charset=utf-8
            x-csrf-token: TOKEN/
        })

        loggingRule.expect("[abapEnvironmentPullGitRepo] Pull Status: RUNNING")
        loggingRule.expect("[abapEnvironmentPullGitRepo] Entity URI: https://1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY/Pull/URI")
        loggingRule.expect("[abapEnvironmentPullGitRepo] Pull Status: SUCCESS")

        stepRule.step.abapEnvironmentPullGitRepo(script: nullScript, url: 'https://1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY', repositoryName: '/DMO/REPO', credentialsId: 'test_credentialsId')

        assertThat(shellRule.shell[0], containsString(/#!\/bin\/bash curl -I -X GET https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/sap\/opu\/odata\/sap\/MANAGE_GIT_REPOSITORY\/Pull -H 'Authorization: Basic dXNlcjpwYXNzd29yZA==' -H 'Accept: application\/json' -H 'x-csrf-token: fetch' -D headerFileAuth-1.txt/))
        assertThat(shellRule.shell[1], containsString(/#!\/bin\/bash curl -X POST "https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/sap\/opu\/odata\/sap\/MANAGE_GIT_REPOSITORY\/Pull" -H 'Authorization: Basic dXNlcjpwYXNzd29yZA==' -H 'Accept: application\/json' -H 'Content-Type: application\/json' -H 'x-csrf-token: TOKEN' --cookie headerFileAuth-1.txt -D headerFilePost-1.txt -d '{ "sc_name": "\/DMO\/REPO" }'/))
        assertThat(shellRule.shell[2], containsString(/#!\/bin\/bash curl -X GET "https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/sap\/opu\/odata\/sap\/MANAGE_GIT_REPOSITORY\/Pull\/URI" -H 'Authorization: Basic dXNlcjpwYXNzd29yZA==' -H 'Accept: application\/json' -D headerFilePoll-1.txt/))
        assertThat(shellRule.shell[3], containsString(/#!\/bin\/bash rm -f headerFileAuth-1.txt headerFilePost-1.txt headerFilePoll-1.txt/))
    }

    @Test
    public void pullFailsWhilePolling() {
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*x-csrf-token: fetch.*/, "TOKEN")
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*POST.*/, /{"d" : { "__metadata" : { "uri" : "https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/sap\/opu\/odata\/sap\/MANAGE_GIT_REPOSITORY\/Pull\/URI" } , "status" : "R", "status_descr" : "RUNNING" }}/)
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/sap\/opu\/odata\/sap\/MANAGE_GIT_REPOSITORY\/Pull\/URI*/, /{"d" : { "__metadata" : { "uri" : "https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/URI" } , "status" : "E", "status_descr" : "ERROR" }}/)
        
        helper.registerAllowedMethod("readFile", [String.class], {
            /HTTP\/1.1 200 OK
            set-cookie: sap-usercontext=sap-client=100; path=\/
            content-type: application\/json; charset=utf-8/
        })

        loggingRule.expect("[abapEnvironmentPullGitRepo] Pull Status: RUNNING")
        loggingRule.expect("[abapEnvironmentPullGitRepo] Pull Status: ERROR")

        thrown.expect(Exception)
        thrown.expectMessage("[abapEnvironmentPullGitRepo] Pull Failed")

        stepRule.step.abapEnvironmentPullGitRepo(script: nullScript, url: 'https://1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY', repositoryName: '/DMO/REPO', credentialsId: 'test_credentialsId')

    }

    @Test
    public void pullFailsWithPostRequest() {
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*x-csrf-token: fetch.*/, "TOKEN")
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*POST.*/, /{"d" : { "__metadata" : { "uri" : "https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/sap\/opu\/odata\/sap\/MANAGE_GIT_REPOSITORY\/Pull\/URI" } , "status" : "E", "status_descr" : "ERROR" }}/)
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/sap\/opu\/odata\/sap\/MANAGE_GIT_REPOSITORY\/Pull\/URI.*/, /{"d" : { "__metadata" : { "uri" : "https:\/\/1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com\/sap\/opu\/odata\/sap\/MANAGE_GIT_REPOSITORY\/Pull\/URI" } , "status" : "E", "status_descr" : "ERROR" }}/)
        
        helper.registerAllowedMethod("readFile", [String.class], {
            /HTTP\/1.1 200 OK
            set-cookie: sap-usercontext=sap-client=100; path=\/
            content-type: application\/json; charset=utf-8/
        })

        loggingRule.expect("[abapEnvironmentPullGitRepo] Pull Status: ERROR")

        thrown.expect(Exception)
        thrown.expectMessage("[abapEnvironmentPullGitRepo] Pull Failed")

        stepRule.step.abapEnvironmentPullGitRepo(script: nullScript, url: 'https://1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY', repositoryName: '/DMO/REPO', credentialsId: 'test_credentialsId')

    }

    @Test
    public void pullWithErrorResponse() {
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*x-csrf-token: fetch.*/, "TOKEN")
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*POST.*/, /{"error" : { "message" : { "lang" : "en", "value": "text" } }}/)
        
        helper.registerAllowedMethod("readFile", [String.class], {
            /HTTP\/1.1 200 OK
            set-cookie: sap-usercontext=sap-client=100; path=\/
            content-type: application\/json; charset=utf-8/
        })

        thrown.expect(Exception)
        thrown.expectMessage("[abapEnvironmentPullGitRepo] Error: text")

        stepRule.step.abapEnvironmentPullGitRepo(script: nullScript, url: 'https://1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY', repositoryName: '/DMO/REPO', credentialsId: 'test_credentialsId')

    }

    @Test
    public void connectionFails() {
        shellRule.setReturnValue(JenkinsShellCallRule.Type.REGEX, /.*x-csrf-token: fetch.*/, null)

        helper.registerAllowedMethod("readFile", [String.class], {
            /HTTP\/1.1 401 Unauthorized
            set-cookie: sap-usercontext=sap-client=100; path=\/
            content-type: application\/json; charset=utf-8/
        })

        thrown.expect(Exception)
        thrown.expectMessage("[abapEnvironmentPullGitRepo] Error: 401 Unauthorized")

        stepRule.step.abapEnvironmentPullGitRepo(script: nullScript, url: 'https://1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY', repositoryName: '/DMO/REPO', credentialsId: 'test_credentialsId')

    }

    @Test
    public void checkRepositoryProvided() {
       thrown.expect(IllegalArgumentException)
       thrown.expectMessage("Repository / Software Component not provided")
       stepRule.step.abapEnvironmentPullGitRepo(script: nullScript, url: 'https://1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY', credentialsId: 'test_credentialsId')
    }

    @Test
    public void checkUrlProvided() {
       thrown.expect(IllegalArgumentException)
       thrown.expectMessage("URL not provided")
       stepRule.step.abapEnvironmentPullGitRepo(script: nullScript, repositoryName: 'REPO', credentialsId: 'test_credentialsId')
    }

    @Test
    public void rejectHttpUrl() {
        thrown.expect(Exception)
        thrown.expectMessage("[abapEnvironmentPullGitRepo] Error: Please provide a valid URL matching to: ^https://.*/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY\$")
        stepRule.step.abapEnvironmentPullGitRepo(script: nullScript, url: 'http://1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY', repositoryName: '/DMO/REPO', credentialsId: 'test_credentialsId')
    }
    @Test
    public void rejectUrlWithSlash() {
        thrown.expect(Exception)
        thrown.expectMessage("[abapEnvironmentPullGitRepo] Error: Please provide a valid URL matching to: ^https://.*/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY\$")
        stepRule.step.abapEnvironmentPullGitRepo(script: nullScript, url: 'https://1234-abcd-5678-efgh-ijk.abap.eu10.hana.ondemand.com/sap/opu/odata/sap/MANAGE_GIT_REPOSITORY/', repositoryName: '/DMO/REPO', credentialsId: 'test_credentialsId')
    }

    @Test
    public void testHttpHeader() {

        String header = /HTTP\/1.1 401 Unauthorized
            set-cookie: sap-usercontext=sap-client=100; path=\/
            content-type: text\/html; charset=utf-8
            content-length: 9321
            sap-system: Y11
            x-csrf-token: TOKEN
            www-authenticate: Basic realm="SAP NetWeaver Application Server [Y11\/100][alias]"
            sap-server: true
            sap-perf-fesrec: 72927.000000/

        HttpHeaderProperties httpHeader = new HttpHeaderProperties(header)
        assertThat(httpHeader.statusCode, equalTo(401))
        assertThat(httpHeader.statusMessage, containsString("Unauthorized"))
        assertThat(httpHeader.xCsrfToken, containsString("TOKEN"))
    }
}
