(ns com.brunobonacci.rolling-update.core
  (:refer-clojure :exclude [printf])
  (:require [amazonica.aws.autoscaling :as asg]
            [amazonica.aws.ec2 :as ec2]
            [where.core :refer [where]]
            [safely.core :refer [safely sleep]]))



(defn printf
  [& args]
  (apply clojure.core/printf args)
  (flush))



(defn time-in-millis
  [[time unit]]
  (when (and time unit)
    (case unit
      :seconds (* time 1000)
      :minutes (* time 60 1000)
      :hours   (* time 60 60 1000))))



(defn tags-as-map
  [tags]
  (->> tags
     (map (fn [{:keys [key value]}] [(keyword key) value]))
     (into {})))



(defn find-asg
  [target]
  (->>
   (asg/describe-auto-scaling-groups
    {:max-records 100})
   :auto-scaling-groups
   (map #(update % :tags tags-as-map))
   (filter (if (string? target) (where [:auto-scaling-group-name :GLOB-MATCHES? target]) target))))



(defn build-plan
  [strategy asgs]
  (->> asgs
     (mapcat (fn [{:keys [auto-scaling-group-name instances]}]
               (map (fn [i] (assoc i :auto-scaling-group-name auto-scaling-group-name)) instances)))
     (sort-by (juxt :launch-configuration-name :auto-scaling-group-name :availability-zone :instance-id))
     (mapcat strategy)))



(defn terminate-and-wait-strategy
  [instance]
  [{:action :terminate     :target instance}
   {:action :asg-stabilize :target (:auto-scaling-group-name instance)}
   {:action :grace-period  }])



(defn index-by
  [f coll]
  (->> coll
     (map (juxt f identity))
     (into {})))



(defn enumerate-maps
  ([coll]
   (enumerate-maps :index coll))
  ([index-key coll]
   (->> coll
      (map-indexed (fn [i v] (assoc v index-key i)))
      (into []))))



(defn refresh-asg
  [name]
  (->>
   (asg/describe-auto-scaling-groups
    :auto-scaling-group-names [name])
   :auto-scaling-groups
   first))



(defn refresh-asgs
  [names]
  (->>
   (asg/describe-auto-scaling-groups
    :auto-scaling-group-names (apply vector names))
   :auto-scaling-groups
   (index-by :auto-scaling-group-name)))



(defn instance-status [instance-id]
  (try
    (->>
     (ec2/describe-instance-status
      :instance-ids [instance-id]
      :include-all-instances true)
     :instance-statuses
     first)
    (catch Exception x
      (when-not (re-find #"The instance ID .* does not exist" (or (ex-message x) ""))
        (throw x)))))



(defn terminate-instance
  [instance-id & {:keys [wait]}]
  (let [result
        (->>
         (ec2/terminate-instances
          :instance-ids [instance-id])
         :terminating-instances
         first)]
    (if wait
      (safely
       (instance-status instance-id)
       :on-error
       :max-retries 150
       :failed? #(and (get-in % [:instance-state :name]) (not= "terminated" (get-in % [:instance-state :name])))
       :retry-delay [:random 3000 :+/- 0.25])
      result)))



(defmulti perform-action! (fn [_ {:keys [action]}] action))



(defmethod perform-action! :terminate
  [_ action]
  (let [instance  (get-in action [:target :instance-id])
        status    (-> (instance-status instance)
                     (get-in [:instance-state :name])
                     keyword)]
    ;; if instance is still running
    (when (= status :running)
      #_(println "TERMINATING instance:" instance)
      (terminate-instance instance :wait true))))



(defmethod perform-action! :asg-stabilize
  [_ {:keys [target]}]

  (safely
   (let [{:keys [desired-capacity instances]} (refresh-asg target)
         in-service (filter (where [:and [:health-status :is? "Healthy"]
                                    [:lifecycle-state :is? "InService"]])
                            instances)
         alive (->> in-service (map :instance-id) (map instance-status)
                  (filter (where [:and
                                  [(comp :name :instance-state) :is? "running"]
                                  [(comp :status :instance-status) :is? "ok"]
                                  [(comp :status :system-status) :is? "ok"]])))]
     #_(println "desired-capacity:" desired-capacity "alive:" (count alive))
     (if (< (count alive) desired-capacity)
       :wait-more
       :completed))
   :on-error
   :max-retries 150
   :failed? (partial not= :completed)
   :retry-delay [:random 3000 :+/- 0.25]))



(defmethod perform-action! :grace-period
  [{:keys [grace-period]} _]

  (sleep (or (time-in-millis grace-period) 60000)))



(defmulti render-action (fn [_ {:keys [action]}] action))



(defmethod render-action :terminate
  [_ {{:keys [instance-id auto-scaling-group-name]} :target :as action}]
  (format "Terminating: %s / %s..." auto-scaling-group-name instance-id))



(defmethod render-action :asg-stabilize
  [_ {asg :target :as action}]
  (format "Waiting for: %s to stabilize..." asg))



(defmethod render-action :grace-period
  [{:keys [grace-period]} _]
  (let [[time unit] grace-period
        hunit (or (some->> unit name first str) "s")
        amount (str (or time "60") hunit)]

    (format "Waiting for: %s as grace-period for app sync..." amount)))



(defn rolling-update
  [cfg asg-names]
  (let [asgs (find-asg asg-names)
        plan (build-plan terminate-and-wait-strategy asgs)]

    (doseq [step (enumerate-maps plan)]
      (printf "Step %d/%d: %s\n"
              (inc (:index step))
              (count plan)
              (render-action cfg step))
      (perform-action! cfg step))))



(comment

  (->> (find-asg "*bro*")
     (index-by :auto-scaling-group-name)
     #_(map :auto-scaling-group-name))

  (->> (find-asg "*bro*")
     (map :auto-scaling-group-name))


  (def asgs (find-asg "*bro*"))
  (def plan (build-plan terminate-and-wait-strategy asgs))

  (def asgs (find-asg "*zk*"))
  (def plan (build-plan terminate-and-wait-strategy asgs))

  plan

  (rolling-update {}  "*bro*")
  )
