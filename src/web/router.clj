(ns web.router
  (:require
    [malli-forms :as mf]
    [reitit.dev.pretty :as pretty]
    [reitit.ring :as ring]
    [web.schema :as schema]))
(set! *warn-on-reflection* true)

(defn resource-route
  "Get a reitit route for serving resources based on a map containing :pattern,
  plus any keys passed to ring/create-resource-handler, e.g. :root, :parameter"
  [{:keys [pattern] :as cfg}]
  [pattern {:name     ::static
            :handler  (ring/create-resource-handler (dissoc cfg :pattern))}])

(defn- mf-middleware
  "Assumes that, on a GET request, the handler will produce a record, and on a
  POST request, the handler expects the parsed value in dest-key"
  [handler {:keys [schema source-key dest-key source-keys]
            :or {source-key :params
                 dest-key   :params
                 source-keys [source-key]}}]
  (fn [req]
    (case (:request-method req)
      :get (mf/render-form schema (handler req))
      :post (let [decoded (mf/handle-submit schema (some req source-keys))]
              (if (mf/parse-failed? decoded)
                @(:form decoded)
                (handler (:value decoded)))))))

(def routes
  [["/new"
    {:name ::new
     :middleware [[mf-middleware {:schema schema/activity}]]
     :get   (constantly nil)
     :post  (fn [activity]
              [:dl
               (for [[k v] activity]
                 (list [:dt k] [:dd v]))])}]])
;(defn- mf-coercer
;  "Coercer for malli-forms"
;  [[path {::mf/keys [schema] :keys [handler] :as data}] opts]
;  (if (and schema handler)
;    (-> (dissoc data :handler)
;        (assoc :get {:handler (fn [_] (mf/render-form schema))}
;               :post 
;

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
