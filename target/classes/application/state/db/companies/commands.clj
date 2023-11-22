(ns application.state.db.companies.commands
  (:require
            [wow.utils.shared.ds :refer [command!]]))

(defn- get-add-cmd
  [company] 
  (let [values (select-keys company [:company_id
                                     :title
                                     :avatar
                                     :selected_package])]

    {:insert-into [:document_management.companies]
     :values [values]}))

(defn register
  [cdb company]
  (let [company-add-cmd (get-add-cmd company)

        result (command! cdb [company-add-cmd])]

    (if-not result
      (assoc company :error :internal-error)
      company)))

(defn remove-avatar
  [cdb company]
  (let [cmd {:update [:document_management.companies]
             :set {:avatar nil}
             :where [:= :company_id (:company_id company)]}
        result (command! cdb [cmd])]

    (when result company)))

(defn modify-avatar
  [cdb company]
  (let [{:keys [company_id avatar]} company
        cmd {:update [:document_management.companies]
             :set {:avatar avatar}
             :where [:= :company_id company_id]}
        result (command! cdb [cmd])]

    (when result company)))

(defn modify-info
  [cdb company]
  (let [{:keys [company_id title]} company
        cmd {:update [:document_management.companies]
             :set {:title title}
             :where [:= :company_id company_id]}
        result (command! cdb [cmd])]

    (when result company)))
