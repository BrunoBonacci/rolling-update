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



(defn exit-with-msg [n msg]
  (binding [*out* *err*]
    (println msg))
  (System/exit n))



(defn list-targets
  [cmd]
  (println "(*) Rolling update targets:")
  (let [targets (->> (core/find-asg (cli/build-filters cmd)) (map :auto-scaling-group-name))]
    (doseq [asg targets]
      (println "    - " asg))
    (println "")
    targets))



(defn list-and-check-targets
  [cmd]
  (when-not (seq (list-targets cmd))
    (exit-with-msg 5 "No target found with given selection!")))



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
      (exit-with-msg 1 cmd)

      (:help cmd)
      (exit-with-msg 0 (help-page))

      (:version cmd)
      (exit-with-msg 0 (format "rolling-update - v%s\n\n" (version)))

      (nil? (:targets cmd))
      (exit-with-msg 0 "[no-op] No target selected, please provide a list of names for autoscaling groups to target!")

      (:dry-run cmd)
      (do
        (list-and-check-targets cmd)
        (show-plan cmd))

      :else
      (do
        (list-and-check-targets cmd)
        (println "Preparing plan... (CTRL-c to abort)")
        (Thread/sleep 3000)
        (core/rolling-update cmd (cli/build-filters cmd))))))
