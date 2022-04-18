(ns web.server
  "Helper for Jetty server"
  (:require [ring.adapter.jetty :as jetty])
  (:import org.eclipse.jetty.server.Server))
(set! *warn-on-reflection* true)

(def component
  "donut.system component for starting/stopping a Jetty server"
  {:start (fn [{:keys [handler] :as cfg} _ _]
            (jetty/run-jetty handler (dissoc cfg :handler)))
   :stop  (fn [_ ^Server server _]
            (when server (.stop server)))})
