(ns com.brunobonacci.rolling-update.command-line
  (:require [instaparse.core :as insta :refer [defparser]]
            [where.core :refer [where]]
            [clojure.java.io :as io]))


(defparser parser (io/resource "cli-grammar.ebnf"))



(defn parse-options
  [cli]
  (->> cli
     (parser)
     (insta/transform
      {:help    #(vector :help    true)
       :version #(vector :version true)
       :dry-run #(vector :dry-run  true)

       :target-name  #(array-map :target-name %)
       :tag (fn [[_ k] [_ v]] {:tag {k v}})

       :grace-period (fn [time & [[_ [unit]]]]
                       [:grace-period [(Long/parseLong time) (or unit :seconds)]])
       :strategy (fn [[n]] [:strategy n])
       :command (fn [& args]
                  (loop [cmd {} [arg & args] args]
                    (cond
                      (nil? (first arg))
                      cmd

                      (= :target (first arg))
                      (recur (update cmd :targets (fnil conj []) (second arg)) args)

                      :else
                      (recur (apply assoc cmd arg) args))))
       })))

;;(parse-options "*foo* --show-plan --grace-period 62s")



(defn parse-error?
  [parse-result]
  (insta/failure? parse-result))


;; TOOD: fix assumption of only one key
(defmulti build-filter (comp first keys))



(defmethod build-filter :target-name
  [{:keys [target-name]}]
  [:auto-scaling-group-name :GLOB-MATCHES? target-name])


;; TOOD: fix assumption of only one key
(defmethod build-filter :tag
  [{:keys [tag]}]
  (let [[k v] (first tag)]
    [(comp (keyword k) :tags) :is? v]))



(defn build-filters
  [{:keys [targets]}]
  (where
   (cons :or
         (map build-filter targets))))
