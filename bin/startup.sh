SIP_HOME=$(
  cd "$(dirname "$0")/../" || exit
  pwd
)
export SIP_HOME

JAVA_OPTS='-server -Xmx2048m -Xms1024m -Xmn512m -Xss128m -XX:+HeapDumpOnOutOfMemoryError -XX:HeapDumpPath=./../oom-info.hprof'
#  -Dcom.sun.management.jmxremote=true
#  -Dcom.sun.management.jmxremote.ssl=false
#  -Djava.rmi.server.hostname=43.243.86.227
#  -Dcom.sun.management.jmxremote.port=18999
#  -Dcom.sun.management.jmxremote.authenticate=false
#  -Dcom.cheung.management.console.port=7001
#  -Dio.netty.leakDetection.level=advanced'
export JAVA_OPTS

"${JAVA_HOME}"/bin/java "${JAVA_OPTS}" -classpath "${SIP_HOME}"/conf:"${SIP_HOME}"/lib/* com.cheung.shadowsocks.ServerStart "$1"
