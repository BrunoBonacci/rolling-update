(defproject com.brunobonacci/rolling-update (-> "./resources/rolling-update.version" slurp .trim)
  :description "A command line tool for automated rolling update of auto-scaling groups."

  :url "https://github.com/BrunoBonacci/rolling-update"

  :license {:name "Apache License 2.0"
            :url "http://www.apache.org/licenses/LICENSE-2.0"}

  :scm {:name "git" :url "https://github.com/BrunoBonacci/rolling-update.git"}

  :dependencies [[org.clojure/clojure "1.10.1"]
                 [com.brunobonacci/safely "0.5.0-alpha7"]
                 [com.brunobonacci/where "0.5.5"]
                 [amazonica "0.3.145" :exclusions
                  [com.amazonaws/aws-java-sdk
                   com.amazonaws/amazon-kinesis-client]]
                 [com.amazonaws/aws-java-sdk-core        "1.11.613"]
                 [com.amazonaws/aws-java-sdk-ec2         "1.11.613"]
                 [com.amazonaws/aws-java-sdk-autoscaling "1.11.613"]
                 [instaparse "1.4.10"]]

  :main com.brunobonacci.rolling-update.main

  :global-vars {*warn-on-reflection* true}

  :jvm-opts ["-server"]

  :bin {:name "rolling-update" :jvm-opts ["-server" "$JVM_OPTS" "-Dfile.encoding=utf-8"]}

  :profiles {:dev {:dependencies [[midje "1.9.9"]
                                  [org.clojure/test.check "0.10.0"]
                                  [criterium "0.4.5"]
                                  [org.slf4j/slf4j-log4j12 "1.7.28"]]
                   :resource-paths ["dev-resources"]
                   :plugins      [[lein-midje "3.2.1"]
                                  [lein-shell "0.5.0"]
                                  [lein-binplus "0.6.5"]]}}
  :aliases
  {"package"
   ["do" "shell" "./bin/package.sh"]}
  )
