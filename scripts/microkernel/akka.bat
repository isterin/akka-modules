set AKKA_HOME=%~dp0..
set JAVA_OPTS="-Xmx512M"
set AKKA_CLASSPATH="%AKKA_HOME%\\lib\\*;%AKKA_HOME%\\config"

java %JAVA_OPTS% -cp "%AKKA_CLASSPATH%" -Dakka.home=%AKKA_HOME% akka.kernel.Main