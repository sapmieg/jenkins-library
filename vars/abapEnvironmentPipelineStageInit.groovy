import groovy.transform.Field
import com.sap.piper.ConfigurationHelper
import com.sap.piper.ConfigurationLoader

import static com.sap.piper.Prerequisites.checkScript

@Field String STEP_NAME = getClass().getName()
@Field Set GENERAL_CONFIG_KEYS = []
@Field STAGE_STEP_KEYS = []
@Field Set STEP_CONFIG_KEYS = GENERAL_CONFIG_KEYS.plus(STAGE_STEP_KEYS)
@Field Set PARAMETER_KEYS = STEP_CONFIG_KEYS
/**
 * This stage initializes the ABAP Environment Pipeline run
 */
void call(Map parameters = [:]) {

    def script = checkScript(this, parameters) ?: this
    def stageName = parameters.stageName?:env.STAGE_NAME

    piperStageWrapper (script: script, stageName: stageName, stashContent: [], stageLocking: false, ordinal: 1, telemetryDisabled: true) {

        checkout scm
        sh '''
        [ -d "jenkins-library" ] && rm -r jenkins-library
        git clone https://github.com/DanielMieg/jenkins-library
        cd jenkins-library
        git checkout abapPipeline
        git log -3
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
        setupCommonPipelineEnvironment script: script, customDefaults: parameters.customDefaults

        // load default & individual configuration
        Map config = ConfigurationHelper.newInstance(this)
            .loadStepDefaults()
            .addIfEmpty('stageConfigResource', 'com.sap.piper/pipeline/abapStageDefaults.yml')
            .addIfEmpty('stashSettings', 'com.sap.piper/pipeline/abapStashSettings.yml')
            .withMandatoryProperty('stageConfigResource')
            .use()

        Map stashConfiguration = readYaml(text: libraryResource(config.stashSettings))
        if (config.verbose) echo "Stash config: ${stashConfiguration}"
        script.commonPipelineEnvironment.configuration.stageStashes = stashConfiguration

        //handling of stage and step activation
        piperInitRunStageConfiguration script: script, stageConfigResource: config.stageConfigResource

        //Config of Addon Pipeline
        script.commonPipelineEnvironment.setValue('addonRepositoryNames', ['Z_DEMO_DM_BRANCH'])
    }
}
