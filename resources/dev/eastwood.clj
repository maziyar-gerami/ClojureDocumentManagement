 (ns dev.eastwood
  (:require [eastwood.lint :as e]
            [org.corfield.build :as bb]))

(defn -r [_] 
  (e/eastwood {:source-paths ["api"]}))

(defn run [opts]
  (let [result (bb/run-task opts [:eastwood])] 
    (if (:some-errors result) opts
        opts)))

