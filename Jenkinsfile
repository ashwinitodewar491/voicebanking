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

    environment {
        // Jenkins Global Environment Variables required (Manage Jenkins → Configure System):
        //   AUTOMATION_EMAIL_TO   e.g. team@joshsoftware.com
        SUITE = "${params.SUITE}"
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

            // 3. Send email with the single-file HTML report attached
            // Requires: Email Extension Plugin + SMTP configured in Jenkins global settings
            emailext(
                subject: "VoiceBanking Tests — ${params.ENV.toUpperCase()} | ${params.SUITE.toUpperCase()} | ${currentBuild.currentResult}",
                mimeType: 'text/html',
                body: """
<html>
<body style="font-family: Arial, sans-serif; font-size: 14px;">

<h2 style="color: #2E4057;">Voice Banking — API Test Report</h2>

<table border="1" cellpadding="8" cellspacing="0" style="border-collapse:collapse; width:500px;">
  <tr style="background:#2E4057; color:white;"><td colspan="2"><b>Build Summary</b></td></tr>
  <tr><td><b>Environment</b></td><td>${params.ENV.toUpperCase()}</td></tr>
  <tr><td><b>Suite</b></td><td>${params.SUITE.toUpperCase()}</td></tr>
  <tr style="background:${currentBuild.currentResult == 'SUCCESS' ? '#d5f5e3' : '#fadbd8'};">
    <td><b>Result</b></td><td><b>${currentBuild.currentResult}</b></td>
  </tr>
  <tr><td><b>Build #</b></td><td>${BUILD_NUMBER}</td></tr>
  <tr><td><b>Duration</b></td><td>${currentBuild.durationString}</td></tr>
  <tr><td><b>Console Log</b></td><td><a href="${BUILD_URL}console">${BUILD_URL}console</a></td></tr>
  <tr><td><b>Full Report</b></td><td><a href="${BUILD_URL}Test_Report/">View in Jenkins</a></td></tr>
</table>

<br/>
<p>The full Extent HTML report is attached to this email — open it in any browser.</p>
<p style="color: #888; font-size: 12px;">Voice Banking Automation Suite · Jenkins CI</p>

</body>
</html>
""",
                attachmentsPattern: 'target/extent-report/index.html',
                to: "${env.AUTOMATION_EMAIL_TO ?: 'ashwini.todewar@joshsoftware.com'}"
            )
        }

        failure {
            echo "Tests FAILED on ${params.ENV} | suite=${params.SUITE}"
        }

        success {
            echo "Tests PASSED on ${params.ENV} | suite=${params.SUITE}"
        }
    }
}
