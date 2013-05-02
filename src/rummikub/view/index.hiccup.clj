[:h1 "Rummikub"]
[:img {:src "images/rummikub-game-reduced.jpg"}]
[:br]
[:br]
[:input {:type "button" :onclick "window.location='/game?player=1'" :value "New game"}]
[:input {:type "button" :onclick "window.location='/game?player=2'" :value "Join a friend"}]
[:br][:br]
[:form {:action "/game?player=1" :enctype "multipart/form-data" :method "post" }
 [:input {:type "file" :name "gamefile"}]
 [:br]
 [:input {:type "submit" :value "Resume backed up game"}]]
[:br]
[:br]
"Not sure how to play?  Game rules "[:a {:href "http://www.pressmantoy.com/instructions/instruct_rummikub.html" :target "_blank"} "here"] "."
