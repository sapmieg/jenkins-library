import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.GenerateStageDocumentation
import groovy.transform.Field

import static com.sap.piper.Prerequisites.checkScript
import com.sap.piper.ConfigurationHelper

@Field String STEP_NAME = getClass().getName()
@Field Set GENERAL_CONFIG_KEYS = []
@Field STAGE_STEP_KEYS = [
    /** Starts an ATC check run on the ABAP Environment instance */
    'abapEnvironmentRunATCCheck'
]
@Field Set STEP_CONFIG_KEYS = GENERAL_CONFIG_KEYS.plus(STAGE_STEP_KEYS)
@Field Set PARAMETER_KEYS = STEP_CONFIG_KEYS
/**
 * This stage runs the ATC Checks
 */
void call(Map parameters = [:]) {
    def script = checkScript(this, parameters) ?: this
    def stageName = parameters.stageName?:env.STAGE_NAME

    Map config = ConfigurationHelper.newInstance(this)
        .loadStepDefaults()
        .mixinGeneralConfig(script.commonPipelineEnvironment, GENERAL_CONFIG_KEYS)
        .mixinStageConfig(script.commonPipelineEnvironment, stageName, STEP_CONFIG_KEYS)
        .mixin(parameters, PARAMETER_KEYS)
        .use()

    piperStageWrapper (script: script, stageName: stageName, stageLocking: false) {
        sh '''
        [ -d "jenkins-library" ] && rm -r jenkins-library
        git clone https://github.com/DanielMieg/jenkins-library
        cd jenkins-library
        git checkout abapPipeline
        git log -1
        '''

        dockerExecute(
            script: script,
            dockerImage: 'golang',
            dockerEnvVars: [GOPATH: '/jenkinsdata/abapPipeline Test/workspace']
        ) {
            sh '''
                cd jenkins-library
                go build -o piper .
                chmod +x piper
                cp piper ..
            '''
        }
        sh '''
        ls -la
        '''
        def utils = new Utils()
        utils.stashWithMessage('piper-bin', 'failed to stash piper binary', 'piper')

        abapEnvironmentRunATCCheck script: parameters.script

    }
}
