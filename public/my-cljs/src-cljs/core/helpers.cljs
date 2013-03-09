(ns core.helpers)
;auxiliary helpers

(defn map-filter [m f?]
  (into {} (for [[k v] m :when (f? v)]
             [k v])))

(defn disp-style [m]
  (apply str
         (interpose "; "
                    (for [[k v] m] (str (name k) ": "
                                        (if (integer? v)
                                          (str v "px")
                                          v))))))

(defn disp-attrs [m]
  (apply str (for [[k v] m] (format "%s=\"%s\" " (name k)
                                    (if (= :style k)
                                      (disp-style v)
                                      v)))))

;converts [:tagName {:attr "val"} "content"]
;into <tagName attr="val"> content </tagName>
;can be nested
(defn ^:export html [v]
  ;(try
  (if (seq? v)
    (apply str (map html v))
    (if (vector? v)
      (let [attrs (if (map? (second v)) (disp-attrs (second v)) "")
            tag-name (name (first v))
            others (if (map? (second v)) (drop 2 v) (rest v))]
        (format "<%s %s>%s</%s>" tag-name attrs (apply str (map html others)) tag-name))
      (str v)))
  ;(catch js/Object e (.log js/console (.toString v))))
  )
