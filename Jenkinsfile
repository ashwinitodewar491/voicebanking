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

                    # Package manager for the installs below: apt (Ubuntu/Debian) or dnf/yum (Amazon Linux).
                    if command -v apt-get >/dev/null 2>&1; then PKG="apt-get"
                    elif command -v dnf >/dev/null 2>&1; then PKG="dnf"
                    else PKG="yum"
                    fi

                    # Install python3 only if it isn't already on the agent.
                    if command -v python3 >/dev/null 2>&1; then
                        echo "python3 already installed - skipping install"
                    else
                        echo "python3 not found - installing with $PKG"
                        [ "$PKG" = "apt-get" ] && sudo -n apt-get update -qq
                        sudo -n $PKG install -y python3
                    fi
                    python3 --version

                    # Install pip only if it isn't already on the agent.
                    if python3 -m pip --version >/dev/null 2>&1; then
                        echo "pip already installed - skipping install"
                    else
                        echo "pip not found - installing with $PKG"
                        [ "$PKG" = "apt-get" ] && sudo -n apt-get update -qq
                        sudo -n $PKG install -y python3-pip
                    fi
                    python3 -m pip --version

                    # EdgeTtsEngine.java runs "python -m edge_tts", but most EC2 images only have
                    # "python3" - add a "python" alias only if it's missing. /usr/local/bin so the
                    # 'Run Tests' stage (and the test JVM) finds it too.
                    if command -v python >/dev/null 2>&1; then
                        echo "python already available - skipping alias"
                    else
                        echo "python not found - linking it to python3"
                        sudo -n ln -sf "$(command -v python3)" /usr/local/bin/python
                    fi
                    python --version

                    # Install edge-tts only if missing. Ubuntu 23.04+ blocks plain "pip install --user"
                    # (externally-managed-environment), so retry with --break-system-packages there.
                    if python3 -m edge_tts --version >/dev/null 2>&1; then
                        echo "edge-tts already installed - skipping install"
                    else
                        echo "edge-tts not found - installing with pip"
                        python3 -m pip install --user --quiet edge-tts \
                            || python3 -m pip install --user --quiet --break-system-packages edge-tts
                    fi
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