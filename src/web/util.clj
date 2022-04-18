(ns web.util
  "Utility functions for the lazy"
  (:require ring.middleware.anti-forgery))

(defn field->datalog-schema
  "Provide a field spec, consisting of:
  [ident type [doc | cardinality | uniqueness]+]
  where `ident` is a keyword, `type` is a simple keyword, doc is a string,
  cardinality is :many (otherwise :one), uniqueness is one of #{:value :identity}"
  [[ident value-type & tail]]
  (reduce
    (fn [entry item]
      (cond 
        ;; doc
        (string? item) (assoc entry :db/doc item)
        ;; cardinality one
        (= :many item) (assoc entry :db/cardinality :db.cardinality/many)
        ;; uniqueness
        (= :value item) (assoc entry :db/unique :db.unique/value)
        (= :identity item) (assoc entry :db/unique :db.unique/identity, :db/cardinality :db.cardinality/one)
        :else (throw (ex-info "Not sure how to handle this item" {:item item :entry entry}))))
    (cond-> {:db/ident ident
             :db/cardinality :db.cardinality/one} ;; default to cardinality one
      (some? value-type) (assoc :db/valueType (keyword "db.type" (name value-type))))
    tail))
        
(defn hiccup?
  "true if x is either a seq or a vector"
  [x]
  (or (seq? x) (vector? x)))

(defn anti-forgery-field
  "Like ring.util.anti-forgery/anti-forgery-field, but returns hiccup markup"
  []
  [:input {:type "hidden"
           :value ring.middleware.anti-forgery/*anti-forgery-token*
           :id "__anti-forgery-token"
           :name "__anti-forgery-token"}])

