(ns core.main)
(.log js/console "loading core.main")

;open link between two players
;for local dev, periodically run core.controller/check-message
;for production open a Google App Engine Channel.
(defn ^:export start-communication [token]
  (if (js/local.get_is_local)
    (js/setInterval core.controller/check-message 100)
    (js/channel.open-channel token))
  )

;start game
(defn ^:export start []
(if (= 2 js/player)
  ;we must request the game state from player 1
  (do
    (.log js/console "requesting game state")
    (core.controller/send-message
      {:message "request game state"
       :my-window [(.-innerWidth js/window) (.-innerHeight js/window)]}
      core.controller/ready-to-play
      )
    (set! (-> js/document (.getElementById "game_div") .-innerHTML)
          "Waiting for other player..."))
  ;otherwise player 1.  Start new game
  (let [
    [game game-width game-height]
    (-> (core.model/get-game) (core.model/claim 14)
      core.model/my-sort (core.view/set-dims nil nil))
    ]
    (core.model/record-state game)
    (core.view/display-game game game-width game-height)
    (reset! core.model/game game)
    (reset! core.view/game-width game-width)
    (reset! core.view/game-height game-height)
    (reset! core.view/fixed-window-width (.-innerWidth js/window))
    (reset! core.view/fixed-window-height (.-innerHeight js/window))
    ))
)
