import com.sap.piper.Utils
import com.cloudbees.groovy.cps.NonCPS
import com.sap.piper.GenerateStageDocumentation
import groovy.transform.Field

import static com.sap.piper.Prerequisites.checkScript

@Field String STEP_NAME = getClass().getName()
@Field Set GENERAL_CONFIG_KEYS = []
@Field STAGE_STEP_KEYS = [
    /** Pulls Software Components / Git repositories into the ABAP Environment instance */
    'abapEnvironmentPullGitRepo'
    'testAbap'
]
@Field Set STEP_CONFIG_KEYS = GENERAL_CONFIG_KEYS.plus(STAGE_STEP_KEYS)
@Field Set PARAMETER_KEYS = STEP_CONFIG_KEYS
/**
 * This stage clones Git repositories / software components to the ABAP Environment instance
 */
void call(Map parameters = [:]) {
    def script = checkScript(this, parameters) ?: this

    def stageName = parameters.stageName?:env.STAGE_NAME

    piperStageWrapper (script: script, stageName: stageName, stashContent: [], stageLocking: false) {
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

        testAbap script: parameters.script
        abapEnvironmentPullGitRepo script: parameters.script
    }

}
