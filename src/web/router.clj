(ns web.router
  (:require
    [reitit.dev.pretty :as pretty]
    [reitit.ring :as ring]))
(set! *warn-on-reflection* true)

(defn resource-route
  "Get a reitit route for serving resources based on a map containing :pattern,
  plus any keys passed to ring/create-resource-handler, e.g. :root, :parameter"
  [{:keys [pattern] :as cfg}]
  [pattern {:name     ::static
            :handler  (ring/create-resource-handler (dissoc cfg :pattern))}])

(defn router
  "Get a reitit.ring/router based on a map containing :routes and :middleware"
  [{:keys [routes middleware]}]
  (ring/router routes
               {:exception pretty/exception
                :data {:middleware middleware}}))

(defn handler
  "Get a reitit.ring/ring-handler based on a map container :router and
  :default-handlers"
  [{:keys [router default-handlers]}]
  (ring/ring-handler
    router
    (ring/routes
      (ring/redirect-trailing-slash-handler {:method :strip})
      (ring/create-default-handler default-handlers))))
