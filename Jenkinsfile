pipeline {
    agent any

    parameters {
        choice(
            name: 'ENV',
            choices: ['staging', 'prod'],
            description: 'Target environment'
        )
        choice(
            name: 'SUITE',
            choices: ['smoke', 'regression'],
            description: 'Test suite to run'
        )
    }

    environment {
        // These must be set as Jenkins Global Environment Variables:
        //   STAGING_API_URL  →  e.g. http://staging-server:9090
        //   PROD_API_URL     →  e.g. http://98.93.75.232:9090
        API_BASE_URL = "${params.ENV == 'prod' ? env.PROD_API_URL : env.STAGING_API_URL}"
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Run Tests') {
            steps {
                bat "mvn clean test -DtestGroups=${params.SUITE}"
            }
        }
    }

    post {
        always {
            junit '**/target/surefire-reports/*.xml'
        }
        failure {
            echo "Tests FAILED on ${params.ENV} | suite=${params.SUITE}"
        }
        success {
            echo "Tests PASSED on ${params.ENV} | suite=${params.SUITE}"
        }
    }
}
