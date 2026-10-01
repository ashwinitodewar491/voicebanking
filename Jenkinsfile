pipeline {
    agent { label 'voicebanking-automation' }

    parameters {
        choice(name: 'ENV',   choices: ['prod', 'stage', 'dev'],                      description: 'Target environment')
        choice(name: 'SUITE', choices: ['smoke', 'regression', 'BasicSmoke'], description: 'Test suite to run')
    }

    stages {
        stage('Checkout') {
            steps {
                checkout scm
            }
        }

        stage('Verify TTS Tooling') {
            steps {
                sh '''
                    set -e
                    export PATH="$HOME/.local/bin:$PATH"

                    python3 --version
                    python3 -m pip --version

                    python3 -m edge_tts --version >/dev/null 2>&1 || python3 -m pip install --user --quiet edge-tts
                    python3 -m edge_tts --version

                    # Install ffmpeg (static build) only if it isn't already on the agent.
                    if command -v ffmpeg >/dev/null 2>&1; then
                        echo "ffmpeg already installed - skipping install"
                    else
                        echo "ffmpeg not found - installing static build"
                        cd /tmp
                        curl -LO https://johnvansickle.com/ffmpeg/releases/ffmpeg-release-amd64-static.tar.xz
                        tar xf ffmpeg-release-amd64-static.tar.xz
                        sudo -n cp ffmpeg-*-amd64-static/ffmpeg ffmpeg-*-amd64-static/ffprobe /usr/local/bin/
                        rm -rf ffmpeg-release-amd64-static.tar.xz ffmpeg-*-amd64-static
                        cd "$WORKSPACE"
                    fi
                    ffmpeg -version | head -1
                    which ffmpeg

                    java -version

                    # Install Maven (Apache binary) only if it isn't already on the agent.
                    if command -v mvn >/dev/null 2>&1; then
                        echo "Maven already installed - skipping install"
                    else
                        echo "Maven not found - installing Apache Maven 3.9.9"
                        cd /tmp
                        curl -LO https://archive.apache.org/dist/maven/maven-3/3.9.9/binaries/apache-maven-3.9.9-bin.tar.gz
                        sudo -n tar xzf apache-maven-3.9.9-bin.tar.gz -C /opt
                        sudo -n ln -sfn /opt/apache-maven-3.9.9/bin/mvn /usr/local/bin/mvn
                        rm -f apache-maven-3.9.9-bin.tar.gz
                        cd "$WORKSPACE"
                    fi
                    mvn -version
                    which mvn
                '''
            }
        }

        stage('Run Tests') {
            steps {
                sh "mvn clean test -DtestGroups=${params.SUITE} -Denv=${params.ENV}"
            }
        }
    }

    post {
        always {
            archiveArtifacts artifacts: 'target/extent-report/index.html',    allowEmptyArchive: true
            archiveArtifacts artifacts: 'target/dashboard-report/index.html', allowEmptyArchive: true
            junit allowEmptyResults: true, testResults: '**/target/surefire-reports/*.xml'
        }
        failure  { echo "Tests FAILED on ${params.ENV} | suite=${params.SUITE}" }
        unstable { echo "Tests UNSTABLE (failures present) on ${params.ENV} | suite=${params.SUITE}" }
        success  { echo "Tests PASSED on ${params.ENV} | suite=${params.SUITE}" }
    }
}