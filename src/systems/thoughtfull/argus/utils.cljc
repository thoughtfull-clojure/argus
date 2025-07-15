(ns ^:no-doc systems.thoughtfull.argus.utils)

(defn ident
  [o]
  (when (or (string? o) (symbol? o) (keyword? o))
    (cond-> (str o)
      (keyword? o) (subs 1))))
