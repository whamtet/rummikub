(ns core.view
  (:require [core.helpers :as helpers]
            [jayq.core :as jayq]
            jayq.util
            )
  )
(.log js/console "loading core.view")

(def max-game-width 600)
(def max-game-height 500)

(def game-width (atom nil))
(def game-height (atom nil))
(def rack-height 100)
;(def table-height (atom nil))

(def fixed-window-width (atom nil))
(def fixed-window-height (atom nil))

;let x,y be placement coordinates on the player's board
;converts to absolute position in pixels
(defn xy->grid [x y]
  [(->
     (/ (- x rack-padding) (+ tile-width tile-margin))
     (quot 1) int)
   (->
     (/ (- y (- @game-height rack-height) rack-padding) (+ tile-height tile-margin))
     (quot 1) int)])

;true if tile is overhanging table
(defn on-table? [[x y]]
  (< y (- @game-height rack-height)))

;after a player has finished dragging a tile, the drag path is sent
;to the opponent's board and animated here.
(defn animate [id points m]
  (let [
    tile-to-animate (jayq/$ (str "#" id))
    points (vec (filter on-table? points))
    [x y] (peek points);last points
    ]
    (.show tile-to-animate)
    ;uses jQuery.animate to drag the tile following opponent's move
    (doseq [[x y] points]
      (.animate tile-to-animate (jayq.util/clj->js
                  {:top y :left x}) 30))
      (.animate tile-to-animate
                              (jayq.util/clj->js {:top y :left x})
                              (jayq.util/clj->js
                                {:complete
                                 (fn [] (core.controller/finish_update m))
                                 :queue true}) 0)))

;adjust game dimensions to the maximum size that can fit
;into both our and the opponent's window
;reposition tiles if necessary
(defn set-dims [game other-window-width other-window-height]
  (let [
    other-window-width (or other-window-width 9999)
    other-window-height (or other-window-height 9999)
    window-width (min other-window-width
                      (-> js/window .-innerWidth))
    window-height (min other-window-height
                       (-> js/window .-innerHeight))
    curr-game-width (min max-game-width (- window-width 20))
    curr-game-height (min max-game-height (- window-height 120))
    curr-table-height (- curr-game-height rack-height)
;    game (or game @core.model/game)
    game (-> game
          (core.model/reposition-tiles curr-game-width curr-table-height)
           #_display-game)
    ]
    [game curr-game-width curr-game-height]))

(def tile-width 20)
(def tile-height 35)
(def base-tile-style
  {:position "absolute" :border "1px solid #999"
   :width tile-width :height tile-height
   :vertical-align "middle"
   :text-align "center"
   :background "#FFFFFF"})

(def tile-margin 3)
(def rack-padding 10)

;finds the next empty spot on the player's board
(defn find-next-empty [game search-from]
  (let [
    my-tiles (filter #(= (js/local.get_player) (:location %)) (vals game))
    taken-positions (set (map #(vector (:grid-x %) (:grid-y %)) my-tiles))
    ;get next position on board
    next (fn [[j i]] (if (= 0 i) [j 1] [(inc j) 0]))
    ]
    (loop [pos search-from]
      (if (contains? taken-positions pos)
        (recur (next pos))
        pos))))

;returns a clojure [:div] representing an html div
;to display a game tile
(defn disp-tile [tile game-width game-height table-height]
  
  (let [
     ;y-position of tile
     top (if (contains? tile :grid-y)
           (+ table-height rack-padding (* (+ tile-height tile-margin) (:grid-y tile)))
           (:y tile))
     ;x-position of tile
     left (if (contains? tile :grid-x)
            (+ rack-padding (* (+ tile-width tile-margin) (:grid-x tile)))
            (:x tile))
     ;map representing inline css of tile
     style (assoc base-tile-style :top top :left left :color (:color tile))
     number (:number tile)
     ]
    [:div {:id (:id tile) :class (str "tilefrom" (:location tile)) 
           :style style
           }
     (if (= 0 number) ":-)" number)]
    ))

;generate html structure to display game
(defn make-game [game game-width game-height]
  (let [
        table-height (- game-height rack-height)
        my-turn? (= (js/local.get_turn) (js/local.get_player))
        ;are there still tiles available to pick up?
        pool-available? (some #(= "pool" (:location %)) (vals game))
        ;enable button when ? otherwise disable it
        enable-when (fn [? m] (if ? m (assoc m :disabled "true")))
        empty-rack? (not (some #(= (js/local.get_player) (:location %)) (vals game)))
        ]
  (list
    ;div containing tiles (table and rack)
    [:div {:id "public" :style {:border "1px solid #999"
                                :max-width game-width
                                }}
      ;div containing table
      [:div {:style {
                    :width game-width
                    :height table-height
                    }
             :id "table"}
       ]
      ;div containing player rack
      [:div {:id "rack" :style {:width game-width :height rack-height
                                :background "#7FFFD4"}}]
     ]
    ;stuff at bottom of board
    [:div {:style {:border "1px solid #999"
                   :width game-width
                   :text-align "center"}}
       ;pick up button
       [:input (enable-when (and pool-available? my-turn?) {:id "pick_up_button" :type "button" :value "Pick up" :onclick "core.controller.pick_up();"})]
       ;pass turn button
       [:input (enable-when (and (not (core.model/same-as-start? game)) my-turn?) {:id "pass_button" :type "button" :value "Pass" :onclick "core.controller.pass();"})]
       ;sort tiles button
       [:input (enable-when my-turn? {:id "sort_tiles_button" :type "button" :value "Sort my tiles" :onclick "core.controller.sort_tiles();"})]
       ;end game button
       [:input (enable-when empty-rack? {:type "button" :value "Rummikub!" :onclick "core.controller.rummikub();"})]
     [:input {:type "button" :value "Back up" :onclick "core.controller.backup();"}]
       (if (= 1 (js/local.get_player))
         [:div {:style {:color "red"}} "You are red"]
         [:div {:style {:color "blue"}} "You are blue"])
       (if (= 1 (js/local.get_turn))
         [:div {:id "msg" :style {:color "red"}} "Red's turn"]
         (if (= 2 (js/local.get_turn))
           [:div {:id "msg" :style {:color "blue"}} "Blue's turn"]
           [:div {:id "msg"} "Passing..."]))
       ]
  ;display each tile
  (for [tile (vals game)]
    (disp-tile tile game-width game-height table-height))
  )))

(def game-div (.getElementById js/document "game_div"))

;actually display the game
(defn display-game [game game-width game-height]
  (set! (.-innerHTML game-div) (helpers/html (make-game game game-width game-height)))
  (if (= (js/local.get_player) (js/local.get_turn))
    ;enable movement of tiles
    (js/controller.after_display game-width game-height (js/local.get_player)))
  ;finally we must hide other tiles
  (let [other-player (- 3 (js/local.get_player))]
    (.hide (jayq/$ (str ".tilefrompool, .tilefrom" other-player))))
  game)
