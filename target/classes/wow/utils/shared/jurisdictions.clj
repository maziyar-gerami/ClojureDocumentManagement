(ns wow.utils.shared.jurisdictions
  (:require
   [wow.utils.shared.ds :refer [query!]]))

(defn- build-country-states
  "this could be used for maping
   the joined country-states
   as a nested map"
  {:version "0.9"}
  [data]
  (->> data
       (group-by :country_id)
       (into {}
             (map (fn [[cid countries]]

                    (let [country (into {} countries)]
                      {:country_id      cid
                       :title           (:country_title country)
                       :abbreviation (:country_abbreviation country)
                       :state_provinces
                       (map #(into {}
                                   {:state_province_id
                                    (:state_province_id %)
                                    :title
                                    (:state_province_title %)
                                    :abbreviation
                                    (:state_province_abbreviation %)})
                            countries)}))))))

(defn- build-jurisdictions
  [jurisdictions]
  (let [group-by-country (vals (group-by :country_id jurisdictions))] 
    (into [] (map build-country-states group-by-country))))

(defn retrieve-all-countries
  "Retrieves all countries and their states as a nested map.
   States are grouped by corresponding country.
   
   **example:**
   ```clojure
   (retrieve-all-countries qdb)
   ;;==>
   ;;   {:country_id \"3905b6f9-70cd-4604-83c5-7d281888a6e9\",
   ;;   :title \"Canada\",
   ;;   :state_provinces
   ;;   [{:state_province_id \"1439edbe-e9f0-4ff0-9c70-09c4aa94b74a\",
   ;;   :title \"Alberta\", :abbreviation \"AB\"}
   ;;   {:state_province_id \"42d2d458-8ef2-46e3-8458-16b24b6ffe4e\", 
   ;;   :title \"British Columbia\", :abbreviation \"BC\"}]},
   ;;   {:country_id \"2e8785d8-d65f-46f0-893a-7b67b98fecfb\",
   ;;   :title \"United States\",
   ;;   :state_provinces
   ;;   [{:state_province_id \"8e04a545-e6ad-4c0b-8e86-e8e9e95483fd\",
   ;;   :title \"Alabama\", :abbreviation \"AL\"}
   ;;   {:state_province_id \"ab1d672a-f7cb-4cc6-8b77-fc1fb3c9e32a\", 
   ;;   :title \"Alaska\", :abbreviation \"AK\"}]}
   ```"

  {:version "0.9"}
  [qdb]
  (let [result (query! qdb {:select [:c.country_id
                                     [:c.title :country_title]
                                     [:c.abbreviation :country_abbreviation]
                                     :sp.state_province_id
                                     [:sp.title :state_province_title]
                                     [:sp.abbreviation :state_province_abbreviation]]
                            :from [[:jurisdiction.countries :c]]
                            :join-by [:left [[:jurisdiction.state_provinces :sp]
                                             [:= :c.country_id :sp.country_id]]]})]
    (if-not (empty? result)
      (map #(assoc % :state_provinces
                  (sort-by :title (:state_provinces %))) 
           (build-jurisdictions result))
      [])))


(defn retrieve-country-by-id
  "Retrieves the country and its states which its ID passed to
   this function via cid argument.
   
   **example:**
   ```clojure
   (retrieve-country-by-id qdb 3905b6f9-70cd-4604-83c5-7d281888a6e9)
   ;;==>
   ;;   {:country_id \"3905b6f9-70cd-4604-83c5-7d281888a6e9\",
   ;;   :title \"Canada\",
   ;;   :state_provinces
   ;;   [{:state_province_id \"1439edbe-e9f0-4ff0-9c70-09c4aa94b74a\",
   ;;   :title \"Alberta\", :abbreviation \"AB\"}
   ;;   {:state_province_id \"42d2d458-8ef2-46e3-8458-16b24b6ffe4e\", 
   ;;   :title \"British Columbia\", :abbreviation \"BC\"}]}
   ```"
  {:version "0.9"}
  [qdb cid]
  (let [result (query! qdb {:select [:c.country_id
                                     [:c.title :country_title]
                                     [:c.abbreviation :country_abbreviation]
                                     :sp.state_province_id
                                     [:sp.title :state_province_title]
                                     [:sp.abbreviation :state_province_abbreviation]]
                            :from [[:jurisdiction.countries :c]]
                            :join-by [:left [[:jurisdiction.state_provinces :sp]
                                             [:= :c.country_id :sp.country_id]]]
                            :where [:= :c.country_id cid]})]
    (if-not (empty? result)
      (map #(assoc % :state_provinces
                   (sort-by :title (:state_provinces %)))
           (build-jurisdictions result))
      [])))


(defn retrieve-state-by-id
  "Retrieves the state which its ID passed to this function via sid argument.
   
   **example:**
   ```clojure
   (retrieve-state-by-id qdb 439edbe-e9f0-4ff0-9c70-09c4aa94b74a)
   ;;==>
   ;;   [{:state_province_id \"1439edbe-e9f0-4ff0-9c70-09c4aa94b74a\",
   ;;   :title \"Alberta\", :abbreviation \"AB\"}]
   ```"
  {:version "0.9"}
  [qdb sid]
  (let [result (query! qdb {:select [:sp.state_province_id
                                     :sp.title
                                     :sp.abbreviation
                                     :sp.country_id]
                            :from [[:jurisdiction.state_provinces :sp]]
                            :where [:= :sp.state_province_id sid]})]

    (if-not (empty? result)
      result
      [])))