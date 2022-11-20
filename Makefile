sonar_project_key := ""
sonar_host := ""
sonar_login := ""

sonar:
	./gradlew sonarqube \
      -Dsonar.projectKey=${sonar_project_key} \
      -Dsonar.host.url=${sonar_host} \
      -Dsonar.login=${sonar_login}