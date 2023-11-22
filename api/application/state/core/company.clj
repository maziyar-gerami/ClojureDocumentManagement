(ns application.state.core.company
  (:require
   [application.state.db.companies.commands :as company-cmd]))


(defn save-company
  [cdb company]
  (company-cmd/register cdb company))

(defn modify-avatar [cdb company]
  (company-cmd/modify-avatar cdb company))

(defn remove-avatar [cdb company]
  (company-cmd/remove-avatar cdb company))

(defn modify-info [cdb company]
  (company-cmd/modify-info cdb company))

