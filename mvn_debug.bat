@ECHO OFF
rem To enable:> mvn_debug 
rem To disable:> mvn_debug off


IF "%1" == "off" (
    SET MAVEN_OPTS=
) ELSE (
    SET MAVEN_OPTS=-Xdebug -Xnoagent -Djava.compile=NONE -Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005
)
