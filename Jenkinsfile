@Library('jenkins-shared-libraries') _

def GRADLE_USER_HOME = workspace().getJobCachePath() + "/.gradle"

// This is here because otherwise it will cron task all branches and we're only interested in the master.
def cronString = "master".equals(BRANCH_NAME) ? "0 10,22 * * 1-5" : ""

pipeline {
    agent {
        label 'alpine-jdk17-mvn3.6-gradle7.4'
    }
    environment {
        GRADLE_USER_HOME = "${GRADLE_USER_HOME}"
        // We don't need a daemon running - the container is disposed after each build.
        // This takes care of:
        // "Starting a Gradle Daemon, 1 incompatible and 3 stopped Daemons could not be reused, use --status for details"
        GRADLE_OPTS = "-Dorg.gradle.daemon=false"
    }
    triggers {
      cron cronString
    }
    stages {
        stage('Node') {
            steps {
                container("gradle") {
                    nodeInfo('gradle')
                }
            }
        }
        stage('Build') {
            steps {
                container("gradle") {
                    withGradle {
                        // we can inline the command, but this looks better in the pipeline view.
                        sh label: "Build and test",
                           script: "gradle build check --stacktrace"
                    }
                }
            }
        }
    }
    post {
        always {
            junit '**/build/test-results/**/*.xml'
            archiveArtifacts artifacts: '**/build/libs/**', fingerprint: true
            archiveArtifacts artifacts: '**/build/publications/**', fingerprint: true
        }
    }

}
