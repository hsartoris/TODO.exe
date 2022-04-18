(ns web.system
  (:require
    [clojure.edn :as edn]
    [clojure.java.io :as io]
    [datahike.api :as d]
    [donut.system :as ds]
    [ring.middleware.session.memory]
    [web.middleware :as mw]
    [web.router :as router]
    [web.server :as server]
    [web.util :as util]))

;; this is a bit excessive
(defn ->%1
  "Returns a function that calls `f` with only its first argument"
  [f]
  (fn [x _ _] (f x)))

(def base-system
  "TODO: put in static config"
  {::ds/defs
   {:env    {:port          8083
             :prefix        ""
             :schema        "db_schema.edn"}
    :db     {:schema        {:conf  {:source (ds/ref [:env :schema])}
                             :start (fn [cfg _ _]
                                      (or (some->> cfg :source io/resource slurp
                                                   (edn/read-string {:readers {'db/field util/field->datalog-schema}}))
                                          []))}
             :conn          {:conf  {:store       {:backend :mem}}
                                     ;:initial-tx  (ds/ref :schema)}
                             :start (fn [cfg inst _]
                                      (when-not (d/database-exists? cfg)
                                        (d/create-database cfg))
                                      (or inst (d/connect cfg)))
                             :stop  (fn [_ conn _]
                                      (some-> conn d/release)
                                      nil)}}
    :http   {:404-fn        (fn [req] {:status 404, :body (str "Not found: " (:uri req))})
             :static-route  {:conf  {:pattern    "/static/*path"
                                     :root       "public"
                                     :parameter  :path
                                     :not-found-handler (ds/ref :404-fn)}
                             :start (->%1 router/resource-route)}
             :routes        [(ds/ref [:env :prefix])
                             (ds/ref :static-route)]
             :session-store {:start (fn [_ _ _] (ring.middleware.session.memory/memory-store))}
             :middleware    {:conf  {:default-status  200
                                     :not-found       (ds/ref :404-fn)
                                     :session         {:store (ds/ref :session-store)
                                                       :cookie-attrs {:same-site  :strict
                                                                      :secure     true
                                                                      :http-only  true}}}
                             :start (->%1 mw/stack)}
             :router        {:conf  {:routes      (ds/ref :routes)
                                     :middleware  (ds/ref :middleware)}
                             :start (->%1 router/router)}
             :defaults      {:conf  {:not-found           (ds/ref :404-fn)
                                     :method-not-allowed  {:status 405, :body "Method not allowed"}
                                     :not-acceptable      {:status 406, :body "No acceptable content"}}
                             :start (fn [cfg _ _] (update-vals cfg #(if (fn? %) % (constantly %))))}
             :handler       {:conf  {:router            (ds/ref :router)
                                     :default-handlers  (ds/ref :defaults)}
                             :start (->%1 router/handler)}
             ;; TODO: port as default
             :server        (assoc web.server/component
                                   :conf {:handler (ds/ref :handler)
                                          :port    (ds/ref [:env :port])
                                          :join?   false})}}})

(defmethod ds/named-system :base [_] base-system)
(defmethod ds/named-system ::ds/repl [_] (ds/system :base))
