(ns com.brunobonacci.rolling-update.main
  (:require [com.brunobonacci.rolling-update.command-line :as cli]
            [com.brunobonacci.rolling-update.core :as core]
            [clojure.string :as str]
            [clojure.java.io :as io])
  (:gen-class))


(defn version
  []
  (some-> (io/resource "rolling-update.version") slurp str/trim))



(defn help-page
  []
  (some-> (io/resource "help.txt") slurp (format (version))))



(defn exit-with-error [n msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit n))



(defn list-targets
  [cmd]
  (println "(*) Starting rolling update on:")
  (doseq [asg (->> (core/find-asg (cli/build-filters cmd)) (map :auto-scaling-group-name))]
    (println "    - " asg))
  (println ""))



(defn show-plan
  [cmd]
  (println "(*) Rolling update plan:")
  (let [plan (->> (core/find-asg (cli/build-filters cmd))
                (core/build-plan core/terminate-and-wait-strategy)
                (core/enumerate-maps))]
    (doseq [step plan]
      (printf "    - Step %d/%d: %s\n"
              (inc (:index step))
              (count plan)
              (core/render-action cmd  step))))
  (println ""))


(defn -main
  [& cli]
  (let [cmd (cli/parse-options (str/join " " cli))]

    (cond
      (cli/parse-error? cmd)
      (exit-with-error 1 cmd)

      (:help cmd)
      (exit-with-error 0 (help-page))

      (:version cmd)
      (exit-with-error 0 (format "rolling-update - v%s\n\n" (version)))

      (nil? (:targets cmd))
      (exit-with-error 0 "[no-op] No target selected, please provide a list of names for autoscaling groups to target!")

      (:dry-run cmd)
      (show-plan cmd)

      :else
      (do
        (list-targets cmd)
        (println "Preparing plan... (CTRL-c to abort)")
        (sleep 3000)
        (core/rolling-update cmd (cli/build-filters cmd))))))
