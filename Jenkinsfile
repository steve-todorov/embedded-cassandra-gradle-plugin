@Library('jenkins-shared-libraries') _

def GRADLE_USER_HOME = workspace().getJobCachePath() + "/.gradle"

def IS_MASTER_BRANCH = 'master'.equals(env.BRANCH_NAME);

// This is here because otherwise it will cron task all branches and we're only interested in the master.
def cronString = IS_MASTER_BRANCH ? "H 10-12 * * 1" : ""

def fetchVersionFromPropertiesFile() {
    return readProperties(defaults: [:], file: "${env.WORKSPACE}/gradle.properties").getOrDefault('version', 'not-found');
}

pipeline {
    agent {
        label 'alpine-jdk17-mvn3.6-gradle7.4'
    }
    environment {
        CI = true

        // This is used to determine if we should deploy or not.
        VERSION = fetchVersionFromPropertiesFile()

        // Gradle settings
        GRADLE_USER_HOME = "${GRADLE_USER_HOME}"
        // We don't need a daemon running - the container is disposed after each build.
        // This takes care of:
        // "Starting a Gradle Daemon, 1 incompatible and 3 stopped Daemons could not be reused, use --status for details"
        GRADLE_OPTS = "-Dorg.gradle.daemon=false"
    }
    triggers {
        cron cronString
    }
    options {
        disableConcurrentBuilds abortPrevious: true
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
            post {
                always {
                    recordIssues(tools: [java()])
                }
            }
        }
        stage('Deploy') {
            when {
                beforeInput true
                expression {
                    IS_MASTER_BRANCH || isDeployableTempVersion()
                }
            }
            steps {
                container("gradle") {
                    withGradle {
                        withCredentials([usernamePassword(credentialsId: 'a71d834c-7af9-4611-94de-06b1bcf0cf40',
                                                          passwordVariable: 'ORG_GRADLE_PROJECT_carlspringPassword',
                                                          usernameVariable: 'ORG_GRADLE_PROJECT_carlspringUsername')])
                        {
                            // we can inline the command, but this looks better in the pipeline view.
                            sh label: "Deploy",
                               script: "gradle publishAllPublicationsToCarlspringRepository"
                        }
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
        failure {
            withCredentials([string(credentialsId: '2d4041b1-de6f-4a60-99e1-7d77ba9affa0', variable: 'MM_TOKEN')]) {
                script {
                    def endpoint = "https://mattermost.carlspring.org/hooks/${env.MM_TOKEN}"
                    mattermostSend color: "#ED2938", endpoint: endpoint, channel: '#iris',
                                   message: "[${env.JOB_NAME} ${env.BUILD_NUMBER}](${env.BUILD_URL}) has failed!"
                }
            }
        }
        fixed {
            withCredentials([string(credentialsId: '2d4041b1-de6f-4a60-99e1-7d77ba9affa0', variable: 'MM_TOKEN')]) {
                script {
                    def endpoint = "https://mattermost.carlspring.org/hooks/${env.MM_TOKEN}"
                    mattermostSend color: "#28CC2D", endpoint: endpoint, channel: '#iris',
                                   message: "[${env.JOB_NAME} ${env.BUILD_NUMBER}](${env.BUILD_URL}) has fixed previous failure"
                }
            }
        }
    }

}
