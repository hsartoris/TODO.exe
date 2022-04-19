(ns web.schema
  (:require [malli.core :as m]))

(def activity-name
  "Arbitrary string name of an activity"
  string?)

(def location
  "lat-lon location of an activity"
  [:orn
   [:coords [:tuple double? double?]]
   [:anywhere [:= "anywhere :)"]]
   [:named-location string?]])

(def cost
  "Dollars etc"
  [:orn
   [:dollar-signs [:enum :$ :$$ :$$$ :$$$$ :$$$$$]]
   [:unknown      [:= "?"]]
   [:free         [:= "free"]]])

(def activity-type
  "string, basically"
  string?)

(def notes string?)

(def activity
  "Malli schema for an activity"
  [:map
   [:name activity-name]
   [:location {:optional true} location]
   [:cost {:optional true} cost]
   [:type {:optional true} activity-type]
   [:notes {:optional true} notes]])
