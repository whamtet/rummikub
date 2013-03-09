(ns core.model)
(.log js/console "loading core.model")

;generate a new game
(defn get-game []
  (let [
    game 
    (-> (repeat 2 (for [i (range 1 14) color ["red" "orange" "blue" "black"]]
        {:location "pool" :x 0 :y 0 :number i :color color}))
    flatten
    (conj {:location "pool" :x 0 :y 0 :number 0 :color "red"})
    (conj {:location "pool" :x 0 :y 0 :number 0 :color "black"})
    )
    ids (for [i (range 106)] (str "tile" i))
    f (fn [id tile] (assoc tile :id id))
    game (zipmap ids (map f ids game))
    ]
    game))

;auxiliary function
(defn restore [vals]
  (zipmap (map :id vals) vals))

;pick up n tiles
(defn claim [game n]
  (let [
    available (filter #(= "pool" (:location %)) (vals game))
    [mine rest] (split-at n (shuffle available))
    mine (map #(assoc % :location (js/local.get_player)) mine)
    second-map (restore (concat mine rest));mine and rest are disjoint
    ]
    (merge game second-map)))

;pick up a tile
(defn pick-up-no-sort [game]
  (let [
    new-tile (some #(if (= "pool" (:location %)) %) (shuffle (vals game)))
    my-tiles (filter #(= (js/local.get_player) (:location %)) (vals game))
    my-tiles (for [tile my-tiles] (-> tile (update-in [:grid-x] int) (update-in [:grid-y] int)))
        ;make sure :grid-x and :grid-y are both integers (not strings)
    new-tile-row (condp = (:color new-tile)
                   "red" 0
                   "orange" 0
                   "blue" 1
                   "black" 1)
    tiles-from-this-row (filter #(= new-tile-row (:grid-y %)) my-tiles)

    new-tile-col (inc (apply max (map #(:grid-x %) tiles-from-this-row)))
    new-tile (assoc new-tile :location (js/local.get_player)
                    :grid-x new-tile-col :grid-y new-tile-row)
    ]
    ;(.log js/console (.toString (vec tiles-from-this-row)))
    (assoc game (:id new-tile) new-tile)))

;sort tiles on board
(defn my-sort [game]
  (let [
    mine (filter #(= (js/local.get_player) (:location %)) (vals game))
    set-grid (fn [c i offset]
               (let [
                   ;tiles with color c
                   this-color (sort-by :number (filter #(= c (:color %)) mine))
                   ]
                 ;this-color
                 (for [j (range (count this-color))]
                   (assoc (nth this-color j) :grid-y i :grid-x (+ j offset)))))
    reds (set-grid "red" 0 0)
    blues (set-grid "blue" 1 0)
    offset (inc (max (count reds) (count blues)))
    oranges (set-grid "orange" 0 offset)
    blacks (set-grid "black" 1 offset)
    ]
    ;reds
    (merge game (restore (concat reds oranges blues blacks)))))

(def game (atom nil))
(def state-at-start (atom nil))

;at the beginning of each turn, we record game state
;and only let the player pass if they've done something
(defn record-state [game]
  (reset! state-at-start (game->state game)))
(defn game->state [game]
  (into #{} (map #(select-keys % [:color :number :location]) (vals game))))
(defn same-as-start?
  ([] (same-as-start? @game))
  ([game]
  (= (game->state game) @state-at-start)))

;reposition tiles when resizing board
(defn reposition-tiles [game curr-game-width curr-table-height]
  (into {} (for [[k v] game]
            [k (assoc v :x (min (:x v) curr-game-width)
                      :y (min (:y v) curr-table-height))])))
