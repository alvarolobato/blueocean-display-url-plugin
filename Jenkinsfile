pipeline {
  agent { docker 'maven' }
  stages {
    stage('build') {
      steps {
        sh 'mvn clean install' 
      }
    }
  }
  post {
    always {
      junit 'target/**/*.xml'
    }
  }
}
