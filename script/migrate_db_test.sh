sbt -Dflyway.url=$TEST_MYSQL_URL -Dflyway.user=$MYSQL_USERNAME -Dflyway.password=$MYSQL_PASSWORD flywayClean flywayMigrate 
