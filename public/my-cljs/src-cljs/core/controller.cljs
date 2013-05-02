(ns core.controller
  (:require [cljs.reader :as reader]
            [core.helpers :as helpers]
            [clojure.string :as string]
            jayq.util
            ))
(.log js/console "loading core.controller")

;position-record stores the coordinates as
;a rummikub tile is dragged accross the screen
(def position-record (atom nil))

;records a point of travel as the tile is dragged
(defn ^:export record [x y]
  (swap! position-record
         (fn [position-record]
           (update-in position-record [:pos]  #(conj % [x y])))))

;clears tile positions to start recording a new tile
(defn ^:export start-drag [id]
  (reset! position-record {:id id :pos []}))

;put a tile dragged from the table onto the player's board.
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
;place a tile onto the table.
(defn ^:export publicize [id left top]
  (let [
    game (update-in @core.model/game [id]
          #(-> % (assoc :location "table" :x left :y top) (dissoc :grid-y :grid-x)))
    ]
    (reset! core.model/game game)
    (core.view/display-game game @core.view/game-width @core.view/game-height)
    (post-update-drag game)))

(defn ^:export backup []
  (.open js/window (str "data:rummikub/data," (string/replace (str @core.model/game) #" " "_"))))

#_(defn ^:export set-location [game id location left top]
    (update-in game [id] #(-> % (assoc :location location :x left :y top) (dissoc :grid-y :grid-x))))


;no need to post these two, as you can only do it on your turn, and then update sent on turn change
;sort tiles
(defn ^:export sort-tiles []
  (reset! core.model/game
          (-> @core.model/game core.model/my-sort (core.view/display-game @core.view/game-width @core.view/game-height)))
  )
;pick up a tile
(defn ^:export pick-up []
  (reset! core.model/game
          (-> @core.model/game core.model/pick-up-no-sort (core.view/display-game @core.view/game-width @core.view/game-height)))
  )
;serialize a map and post it to /message
;in Clojurescript serializing means just converting to a string
;all data structures have a simple string representation
(defn send-map [m]
  (let [
    r (js/XMLHttpRequest.)
    ]
    (.open r "POST" "/message" true)
    ;(.log js/console (str "sending " (.toString m)))
    (.send r (.toString m))))

(def requests-pending (atom {}))
(def send-message-id (atom 0))


;there are two ways two send a message
;if on-confirm is null, it is sent once only without confirmation
;routine moves are sent this way
;for critical messages such as ending a turn, on-confirm is a function
;which is executed with the opponent's response as an argument.
(defn send-message [m on-confirm]
  (let [
    m (assoc m :recipient (- 3 (js/local.get_player))
             :randomizer (rand-int 1000000);can't remember what this is for
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

;Due to a bug in Google App Engine, the Channel API does not work on localhost
;As a substitute, check message is periodically called to check for changes
;in /messageForX.txt where X is player number.
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

;callbacks for receiving messages from an opponent
;if the message contains :response-to the handler is
;called directly, otherwise the message is passed to handle-message
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
;opponent wants your game state
;their message contains their board dimensions (reduced for a small screen)
;if these are smaller than yours, reduce your board size to match and then
;send them your game state
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
;opponent is announcing an move made as part of their turn
    (start-update m)
    "pass"
;opponent is passing the turn to you
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
;opponent has placed all their tiles and won!
    (set! (-> js/document (.getElementById "msg") .-innerHTML)
          (helpers/html [:h2 (format "Player %s has finished!  Check carefully..." (- 3 (js/local.get_player)))]))
     nil
    ))

(def highest-message-updated (atom -1))

;player has won.  Display message and inform opponent.
(defn ^:export rummikub []
  (set! (-> js/document (.getElementById "msg") .-innerHTML)
        (helpers/html [:h2 "Rummikub!"]))
  (send-message {:message "rummikub"}))

;update board
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

;callback when opponent is passing turn to you.
(defn receive-pass [game-state m]
  (when (not= (js/local.get_player) (js/local.get_turn))
    (core.model/record-state game-state)
    (.load sound-handle)
    (.play sound-handle)
    (reset! core.model/game game-state)
    (js/local.set_turn (js/local.get_player))
    (core.view/display-game game-state @core.view/game-width @core.view/game-height)
    )
  (.log js/console "sending confirm")
  (send-message {:response-to m :args []} nil));its ok to only send once
                                               ;because we'll receieve another
                                               ;request if this one fails.

(defn disable-element [s]
  (set! (-> js/document (.getElementById s) .-disabled) true))

(def sound-handle (.getElementById js/document "sound_handle"))

;pass turn to opponent
(defn ^:export pass []
  (if-not (core.model/same-as-start?)
    (do
      (js/local.set_turn (+ 2 (js/local.get_turn)))
      (core.view/display-game @core.model/game @core.view/game-width @core.view/game-height)
      (send-message {:message "pass" :game-state @core.model/game}
                passed-already))))

;callback after opponent has acknowledged passing turn to them
(defn passed-already []
  (js/local.set_turn (- 3 (- (js/local.get_turn) 2)))
  (.log js/console "passed already")
  (core.view/display-game @core.model/game @core.view/game-width @core.view/game-height))

;player two has received game state from player one
;and is now ready to begin
(defn ready-to-play [game chosen-width chosen-height game-turn]
  (.log js/console "ready to play")
  (let [
    ;if player is new, pick up tiles
    game (if (some #(= (js/local.get_player) (:location %)) (vals game))
           game
           (-> game (core.model/claim 14) core.model/my-sort)
           )
    ]

    (js/local.set_turn game-turn)
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
