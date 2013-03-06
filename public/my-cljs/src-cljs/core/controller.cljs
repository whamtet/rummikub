(ns core.controller
  (:require [cljs.reader :as reader]
            [core.helpers :as helpers]
            jayq.util
            ))
(.log js/console "loading core.controller")

(def position-record (atom nil))
(defn ^:export record [x y]
  (swap! position-record
         (fn [position-record]
           (update-in position-record [:pos]  #(conj % [x y])))))

(defn ^:export start-drag [id]
  (reset! position-record {:id id :pos []}))

(defn check []
  (.log js/console (.toString @requests-pending)))


(defn ^:export privatize [id left top]
  (let [
    game @core.model/game
    [grid-x grid-y] (core.view/find-next-empty game (core.view/xy->grid left top))
    game (update-in @core.model/game [id]
                    #(-> % (assoc :location (js/local.get_player) :grid-x grid-x
                                  :grid-y grid-y)))
    ]
    (reset! core.model/game game)
    (core.view/display-game game @core.view/game-width @core.view/game-height)
    (post-update-drag game)
    ))

(defn ^:export publicize [id left top]
  (let [
    game (update-in @core.model/game [id] 
          #(-> % (assoc :location "table" :x left :y top) (dissoc :grid-y :grid-x)))
    ]
    (reset! core.model/game game)
    (core.view/display-game game @core.view/game-width @core.view/game-height)
    (post-update-drag game)))

#_(defn ^:export set-location [game id location left top]
    (update-in game [id] #(-> % (assoc :location location :x left :y top) (dissoc :grid-y :grid-x))))
  

;no need to post these two, as you can only do it on your turn, and then update sent on turn change
(defn ^:export sort-tiles []
  (reset! core.model/game
          (-> @core.model/game core.model/my-sort (core.view/display-game @core.view/game-width @core.view/game-height)))
  )

(defn ^:export pick-up []
  (reset! core.model/game
          (-> @core.model/game core.model/pick-up-no-sort (core.view/display-game @core.view/game-width @core.view/game-height)))
  )

(defn send-map [m]
  (let [
    r (js/XMLHttpRequest.)
    ]
    (.open r "POST" "/message" true)
    ;(.log js/console (str "sending " (.toString m)))
    (.send r (.toString m))))

(def requests-pending (atom {}))
(def send-message-id (atom 0))

(defn send-message [m on-confirm]
  (let [
    m (assoc m :recipient (- 3 (js/local.get_player)) 
             :randomizer (rand-int 1000)
             :id (swap! send-message-id inc))
    ]
    (if-not on-confirm
      (do (send-map m) 
        (.log js/console "sending once only")
        )
      (do
        (swap! requests-pending #(assoc % m on-confirm))
        (multi-send true m 4)))))

(defn multi-send [force m its]
  (if (or force (and (contains? @requests-pending m) (< 0 its)))
    (do
      (.log js/console "multi-sending")
      (send-map m)
      (js/setTimeout
        (fn [] (multi-send false m (dec its)))
        4000))))

;these two are just for local testing
(def last-message (atom ""))
(defn check-message []
  (let [
    r (js/XMLHttpRequest.)
    ]
    (set! (.-onreadystatechange r) (fn []
                                     (if (and (= 4 (.-readyState r))
                                              (not= @last-message (.-response r))
                                              (not= "" (.-response r)))
                                       (let [
                                         m (reader/read-string (.-response r))
                                         ]
                                         (reset! last-message (.-response r))
                                         (if (= (js/local.get_player) (:recipient m))
                                           (onmessage m))))))
    (.open r "POST" (format "/messageFor%s.txt?hi=%s" (js/local.get_player) (rand-int 1000)) true)
    (.send r)
    ))

(defn ^:export onmessage-channel [message]
  (-> message .-data reader/read-string onmessage))



(defn onmessage [m]
  (if-let [request (:response-to m)]
    (do
    (if-let [
      f (get @requests-pending request)
      ]
      (do 
      (swap! requests-pending #(dissoc % request))
      (apply f (:args m)))
      ))
    (handle-message m)))
 
(defn handle-message [m]
  (condp = (:message m)
    "request game state"
      (let [
        [game chosen-width chosen-height]
          (apply core.view/set-dims @core.model/game (:my-window m));resize
          ]
          (.log js/console "request game state")
          (reset! core.view/game-width chosen-width)
          (reset! core.view/game-height chosen-height)
          (reset! core.model/game game)
          (core.view/display-game game chosen-width chosen-height)
          (send-message {:response-to m
                   :args [game chosen-width chosen-height 
                          (js/local.get_turn)]} nil)
          )
    "update"
    (start-update m)
    "pass"
    (do (.log js/console "pass") (receive-pass (:game-state m) m)) 
;    "resize"
;    (let [
;      [game chosen-width chosen-height]
;        (apply core.view/set-dims @core.model/game (:my-window m))
;      ]
;      (.log js/console "resize")
;      (core.view/display-game game chosen-width chosen-height)
;      (reset! core.view/game-width chosen-width)
;      (reset! core.view/game-height chosen-height)
;      (reset! core.model/game game)
;      (send-message {:response-to m
;                     :args [(.-innerWidth js/window)
;                            (.-innerHeight js/window)]} nil))
    "rummikub"
    (set! (-> js/document (.getElementById "msg") .-innerHTML)
          (helpers/html [:h2 (format "Player %s has finished!  Check carefully..." (- 3 (js/local.get_player)))]))
     nil
    ))

(def highest-message-updated (atom -1))
(defn ^:export rummikub []
  (set! (-> js/document (.getElementById "msg") .-innerHTML)
        (helpers/html [:h2 "Rummikub!"]))
  (send-message {:message "rummikub"}))

(defn start-update [m]
  (if (> (:id m) @highest-message-updated)
    (do
      (reset! highest-message-updated (:id m))
      (if-let [drag (:drag m)]
            (if (or (core.view/on-table? (first (:pos drag)))
                    (core.view/on-table? (last (:pos drag))))
                    (core.view/animate (:id drag) (:pos drag) m))
            (finish-update m)))))

(defn ^:export finish-update [m]
  (.log js/console "finishing update")
  (reset! core.model/game (-> (:data m) 
                              (core.view/display-game 
                              @core.view/game-width @core.view/game-height))))

(defn receive-pass [game-state m]
  (if (not= (js/local.get_player) (js/local.get_turn))
    (do 
      (core.model/record-state game-state)
      (.load sound-handle)
      (.play sound-handle)
  ;(.log js/console (.toString game-state))
      (reset! core.model/game game-state)
      (js/local.set_turn (js/local.get_player))
      (core.view/display-game game-state @core.view/game-width @core.view/game-height)
      ))
  (.log js/console "sending confirm")
  (send-message {:response-to m :args []} nil));its ok to only send once
                                               ;because we'll receieve another
                                               ;request if this one fails.

(defn disable-element [s]
  (set! (-> js/document (.getElementById s) .-disabled) true))

(def sound-handle (.getElementById js/document "sound_handle"))

(defn ^:export pass []
  (if-not (core.model/same-as-start?)
    (do
      (js/local.set_turn (+ 2 (js/local.get_turn)))
      (core.view/display-game @core.model/game @core.view/game-width @core.view/game-height)
      (send-message {:message "pass" :game-state @core.model/game}
                passed-already))))

(defn passed-already []
  (js/local.set_turn (- 3 (- (js/local.get_turn) 2)))
  (.log js/console "passed already")
  (core.view/display-game @core.model/game @core.view/game-width @core.view/game-height))

#_(defn ready-to-play [game]
  (.log js/console "ready to play"))
(defn ready-to-play [game chosen-width chosen-height game-turn]
  (.log js/console "ready to play")
  (let [
    game (if (some #(= (js/local.get_player) (:location %)) (vals game)) 
           game
           (-> game (core.model/claim 14) core.model/my-sort)
           )
    ]
    
    (js/local.set_turn game-turn)
;    (set! js/turn game-turn)
    (core.view/display-game game chosen-width chosen-height)
    (reset! core.model/game game)
    (reset! core.view/game-width chosen-width)
    (reset! core.view/game-height chosen-height)
    (post-update game)
    #_(.log js/console (.toString game))))

(defn ^:export post-update 
  ([] (post-update @core.model/game))
  ([game]
    (send-message {:message "update"
                 :data game } nil)))

(defn ^:export post-update-drag [game]
  (send-message {:message "update"
                 :data game :drag @position-record} nil))

#_(defn ^:export resize []
  (set! (-> js/document (.getElementById "game_div") .-innerHTML)
        "Resizing...")
  (core.controller/send-message
    {:message "resize" :my-window [(.-innerWidth js/window)
                                   (.-innerHeight js/window)]}
    resize2))

(defn resize2 [other-width other-height]
  (let [
    [game chosen-width chosen-height]
      (core.view/set-dims @core.model/game other-width other-height)
      ]
    (core.view/display-game game chosen-width chosen-height)
    (reset! core.model/game game)
    (reset! core.view/game-width chosen-width)
    (reset! core.view/game-height chosen-height)
    ))