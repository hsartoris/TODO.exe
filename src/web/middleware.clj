(ns web.middleware
  (:require
    ;[reitit.ring.middleware.multipart :as multipart]
    ;[reitit.ring.middleware.parameters :as parameters]
    [ring.middleware.anti-forgery :refer [wrap-anti-forgery]]
    [ring.middleware.multipart-params :refer [wrap-multipart-params]]
    [ring.middleware.params :refer [wrap-params]]
    [ring.middleware.session :as ring-session]
    [ring.util.response :as resp]
    [rum.core :as rum]
    [web.util :as util]))

(defn wrap-nil->404
  "If (handler req) returns nil, returns a 404, optionally by calling not-found
  on the request"
  [handler & [not-found]]
  (let [not-found (or not-found
                      (fn [req]
                        [:h1 "404 Page not found: " [:code (:uri req)]]))]
    (fn [req]
      (if-some [res (handler req)]
        res
        (resp/not-found (not-found req))))))

(defn wrap-ensure-map
  "If the result of (handler req) is not a map, returns {:body (handler req)}"
  [handler]
  (fn [req]
    (let [res (handler req)]
      (if-not (map? res) {:body res} res))))

(defn wrap-apply-template
  "If the body of (handler req) is not hiccup (seq or vector), returns
  unchanged. Otherwise, updates incomplete body (that is, the first element is
  not :html) with (template-fn body req)."
  [handler template-fn]
  (fn [req]
    (let [res (handler req)]
      (if-not (util/hiccup? (:body res))
        res
        (update res :body #(cond-> %
                             (not= :html (first %)) (template-fn req)))))))

(defn wrap-render-hiccup
  "If the body of (handler req) is hiccup, updates by rendering with rum, and
  setting content-type to text/html"
  [handler]
  (fn [req]
    (let [res (handler req)]
      (if-not (util/hiccup? (:body res))
        res
        (-> (resp/content-type res "text/html")
            (update :body #(str "<!DOCTYPE html>\n" (rum/render-static-markup %))))))))

(defn wrap-default-status
  "Unless already set, add provided default status code to response"
  [handler default]
  (fn [req]
    (-> (handler req) (update :status #(or % default)))))

(defn wrap-session
  "Middleware that is broadly identical to ring.middleware.session/wrap-session,
  except in that it treats responses differently:

  | :session key set to | Original   | this      |
  |---------------------|------------|-----------|
  | no :session key     | preserves  | preserves |
  | nil                 | deletes    | deletes   |
  | ^:recreate set      | replaces   | replaces  |
  | :session/clear      | N/A        | deletes   |
  | some (map) value    | *replaces* | *merges*  |

  The last piece is the most important, as it allows handlers to set only the
  portion of a session map that they need to modify, without choosing between
  explicitly referencing the existing session or clobbering the values.
  Also, returning :session/clear is nice IMO - more explicity.
  
  As opposed to the original, this requires options, and will not set a default
  value for :store."
  [handler options]
  ;; replicate functionality of ring-session/session-options
  (let [options (-> options
                    (update :cookie-name #(or % "ring-session"))
                    (update :cookie-attrs #(merge {:path "/"
                                                   :http-only true}
                                                  %
                                                  (when-let [root (:root options)]
                                                    {:path root}))))]
    (fn [req]
      (let [req (ring-session/session-request req options)
            res (handler req)]
        (-> res
            ;; when it doesn't contain anything, session-response will add it
            (cond->
              (contains? res :session) 
              (update :session
                      #(cond
                         (nil? %)             nil
                         (= % :session/clear) nil
                         ;; leave alone - original will handle
                         (:recreate (meta %)) %
                         ;; deviates from original behavior
                         (map? %)             (merge-with merge (:session req) %))))
            (ring-session/session-response req options))))))

(defn stack
  "Get a nice middleware stack.
  Keys:
    - session: goes to wrap-session
    - template: template-fn for wrap-apply-template
    - default-status: default http status
    - not-found: provided to wrap-nil->404"
  [{:keys [default-status session template not-found]}]
  [[wrap-session session]
   ;; TODO: explanations for these in particular
   wrap-params
   wrap-multipart-params
   ;parameters/parameters-middleware
   ;multipart/multipart-middleware
   wrap-anti-forgery
   wrap-render-hiccup
   [wrap-apply-template template]
   [wrap-default-status default-status]
   wrap-ensure-map
   [wrap-nil->404 not-found]])
