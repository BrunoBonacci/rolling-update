(ns com.brunobonacci.rolling-update.command-line-test
  (:require [com.brunobonacci.rolling-update.command-line :refer :all]
            [midje.sweet :refer :all]))


(facts
 "empty command line"

 (parse-options "") => {}

 )


(facts
 "global options"

 (parse-options "-h")          => {:help true}
 (parse-options "--help")      => {:help true}
 (parse-options "-v")          => {:version true}
 (parse-options "--version")   => {:version true}
 (parse-options "--dry-run")   => {:dry-run true}
 (parse-options "--dryrun")    => {:dry-run true}
 (parse-options "--show-plan") => {:dry-run true}

 (parse-options "--grace-period 15")  => {:grace-period [15 :seconds]}
 (parse-options "--grace-period 15s") => {:grace-period [15 :seconds]}
 (parse-options "--grace-period 15m") => {:grace-period [15 :minutes]}
 (parse-options "--grace-period 15h") => {:grace-period [15 :hours]}

 )



(facts
 "targets: by name"

 (parse-options "prefix-*")   => {:targets [{:target-name "prefix-*"}]}

 )



(facts
 "targets: by tag"

 (parse-options "tag:service-name=user-service") => {:targets [{:tag {"service-name" "user-service"}}]}
 (parse-options "tag:SomeTagName='some value'") => {:targets [{:tag {"SomeTagName" "some value"}}]}

 )



(facts
 "multiple targets:"

 (parse-options "group1 group2* *three* tag:application=web-server")
 => {:targets
    [{:target-name "group1"}
     {:target-name "group2*"}
     {:target-name "*three*"}
     {:tag {"application" "web-server"}}]}

 )


(facts
 "strategies"

 (parse-options "-s terminate")                  => {:strategy :terminate}
 (parse-options "-s terminate-and-wait")         => {:strategy :terminate}
 (parse-options "--strategy terminate")          => {:strategy :terminate}
 (parse-options "--strategy terminate-and-wait") => {:strategy :terminate}

 (parse-options "-s reboot")                     => {:strategy :reboot}
 (parse-options "-s reboot-and-wait")            => {:strategy :reboot}
 (parse-options "--strategy reboot")             => {:strategy :reboot}
 (parse-options "--strategy reboot-and-wait")    => {:strategy :reboot}
)
