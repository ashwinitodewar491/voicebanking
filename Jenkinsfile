pipeline {
    agent any

    parameters {
        choice(
            name: 'ENV',
            choices: ['prod', 'stage'],
            description: 'Target environment'
        )
        choice(
            name: 'SUITE',
            choices: ['smoke', 'regression'],
            description: 'Test suite to run'
        )
    }


    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Run Tests') {
            steps {
                bat "mvn clean test -DtestGroups=${params.SUITE} -Denv=${params.ENV}"
            }
        }
    }

    post {
        always {
            // 1. Archive Extent report as a downloadable build artifact
            archiveArtifacts artifacts: 'target/extent-report/index.html', allowEmptyArchive: true

            // 2. Publish surefire XML for Jenkins test trend graph
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'

            // 2. Publish Extent HTML report as a Jenkins build link
            // Requires: HTML Publisher Plugin
            publishHTML(target: [
                allowMissing         : true,
                alwaysLinkToLastBuild: true,
                keepAll              : true,
                reportDir            : 'target/extent-report',
                reportFiles          : 'index.html',
                reportName           : "Test Report — ${params.ENV} | ${params.SUITE}"
            ])

        }

        failure {
            echo "Tests FAILED on ${params.ENV} | suite=${params.SUITE}"
        }

        success {
            echo "Tests PASSED on ${params.ENV} | suite=${params.SUITE}"
        }
    }
}
