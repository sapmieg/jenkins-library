void call(parameters) {
    pipeline {
        agent none
        options {
            skipDefaultCheckout()
        }
        stages {
            stage("GO build project"){
                steps {
                    sh '''
                    [ -d "jenkins-library" ] && rm -r jenkins-library
                    git clone https://github.com/DanielMieg/jenkins-library
                    cd jenkins-library
                    git checkout abapPipeline
                    git log -3
                    '''

                    dockerExecute(
                        script: this,
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
                }
            }
            stage('Init') {
                steps {
                    abapEnvironmentPipelineStageInit script: parameters.script, customDefaults: ['com.sap.piper/pipeline/abapStageOrdinals.yml'].plus(parameters.customDefaults ?: [])
                }
            }

            stage('Prepare System') {
                when {expression {return parameters.script.commonPipelineEnvironment.configuration.runStage?.get(env.STAGE_NAME)}}
                steps {
                    abapEnvironmentPipelineStagePrepareSystem script: parameters.script
                }
            }

            stage('Clone Repositories') {
                when {expression {return parameters.script.commonPipelineEnvironment.configuration.runStage?.get(env.STAGE_NAME)}}
                steps {
                    abapEnvironmentPipelineStageCloneRepositories script: parameters.script
                }
            }

            stage('ATC') {
                when {expression {return parameters.script.commonPipelineEnvironment.configuration.runStage?.get(env.STAGE_NAME)}}
                steps {
                    abapEnvironmentPipelineStageATC script: parameters.script
                }
            }
        }
        post {
            /* https://jenkins.io/doc/book/pipeline/syntax/#post */
            success {buildSetResult(currentBuild)}
            aborted {buildSetResult(currentBuild, 'ABORTED')}
            failure {buildSetResult(currentBuild, 'FAILURE')}
            unstable {buildSetResult(currentBuild, 'UNSTABLE')}
            cleanup {
                abapEnvironmentPipelineStagePost script: parameters.script
            }
        }
    }
}
