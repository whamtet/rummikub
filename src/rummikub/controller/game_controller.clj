(ns rummikub.controller.game-controller
  (:import [com.google.appengine.api.channel ChannelServiceFactory ChannelMessage]
           java.io.File)
  (:use ring.util.response)
  )

(defn is-local?
  "are we running on localhost?"
  [request]
  (= "localhost" (:server-name request)))

(defn create-channel
  "Creates a persistent connection between the server and client
   using the Channel API.  Returns a key to the channel as a string."
  [channel]
  (let [channelService (ChannelServiceFactory/getChannelService)]
    (.createChannel channelService channel)))

(defn javascript
  "inserts a javascript tag"
  [s request]
  (format "<script src = \"%s%s\"></script>"
          s
          (if (is-local? request)
            (str "?a=" (rand-int 1000))
            "")))

(defn game-controller
  "Servlet for the url mapping /game
   This creates a new game for player 1
   and joins and existing one for player 2"
  [request]
  (let [;player is 1 or 2
        player (-> request :params :player)
        ;a channel name so that each player can update its moves to its opponent
        channel-name (if (= "1" player) "abc" "def")
        ]
    ;Unfortunately there's a bug in the Google App Engine SDK.
    ;The Channel does not work in local development, so we write
    ;to and read from local files to post messages between players.
    ;The files must be cleared before beginning
    (if (is-local? request) (do
                     (spit "public/messageFor1.txt" "")
                     (spit "public/messageFor2.txt" "")
                     (loop []
                       (if (not= "" (slurp "http://localhost:8080/messageFor1.txt"))
                         (recur)))
                     (loop []
                       (if (not= "" (slurp "http://localhost:8080/messageFor2.txt"))
                         (recur)))
                     ))
    ;return html to the client
    (response (str "
<html><head>
<link href=\"/stylesheets/game.css\" rel=\"stylesheet\" type=\"text/css\" />
</head>
<!-- <body onresize = \"core.controller.resize();\"> -->
<body>
<div id = \"game_div\">
</div>
<script>
<!-- some basic parameters to start the game -->
player = "player";
turn = 1;
channel_token = '"(create-channel channel-name)"';
channel = '"channel"';
is_local = "(is-local? request)";

<!--now we must define getters and setters to handle optimizations -->
local = {};
local.get_player = function() {return player;};
local.get_turn = function() {return turn;};
local.set_turn = function(this_turn) {turn = this_turn;};
local.get_is_local = function() {return is_local;};
</script>
<audio id=\"sound_handle\" style=\"display: none;\" src = \"your_turn.wav\" type = \"audio/wav\"></audio>
<!--jquery--><script src=\"//ajax.googleapis.com/ajax/libs/jquery/1.8.3/jquery.min.js\"></script>
<!--jquery-ui--><script src=\"//ajax.googleapis.com/ajax/libs/jqueryui/1.9.2/jquery-ui.min.js\"></script>
<!-- <script src = \"/javascript/jquery.crSpline.js\"></script> -->
<!--direct javascript-->
<script src = \"/_ah/channel/jsapi\"></script>
"(javascript "/javascript/controller.js" request)"
"(javascript "/javascript/view.js" request)"
<script src = \"/javascript/channel.js\"></script>
<script src = \"my-cljs/out/goog/base.js\"></script>
<script src = \"my-cljs/main.js\"></script>
<script>
goog.require('core.model');
goog.require('core.controller');
goog.require('core.view');
goog.require('core.main');
</script>
<!--finally we boot the program-->
<script>
core.main.start();
core.main.start_communication(channel_token);
</script>
</body>
</html>
"
  ))))
