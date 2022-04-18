(ns user
  #_:clj-kondo/ignore
  (:use clojure.pprint clojure.repl)
  (:require
    [clojure.tools.namespace.repl :as repl]
    [donut.system :as ds]
    [donut.system.repl :as ds.repl :refer [clear! signal stop]]
    [donut.system.repl.state :as state :refer [system]]
    web.system))

(def instances
  "Holds ::ds/instances when system running"
  nil)

(def db
  "Holds [::ds/instances :db :conn] when system running"
  nil)

(def router
  "Holds [::ds/instances :http :router] when system running"
  nil)

(defmacro reset-var!
  [v value]
  `(alter-var-root (var ~v) (constantly ~value)))

(defn start []
  (ds.repl/start)
  (reset-var! instances (::ds/instances system))
  (reset-var! db (get-in system [::ds/instances :db :conn]))
  (reset-var! router (get-in system [::ds/instances :http :router])))

(defn restart
  "Basically ds.repl/restart, but points to local start fn"
  []
  (stop)
  (repl/refresh :after 'user/start))
