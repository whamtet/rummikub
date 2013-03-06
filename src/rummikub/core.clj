(ns rummikub.core
  (:use
    [compojure.core :only (defroutes GET)]
    [compojure.route :only (not-found)]
    [joodo.middleware.view-context :only (wrap-view-context)]
    [joodo.views :only (render-template render-html)]
    [joodo.controllers :only (controller-router)]))

(defroutes rummikub-routes
  (GET "/" [] (render-template "index"))
  (controller-router 'rummikub.controller)
  (not-found (render-template "not_found" :template-root "rummikub/view" :ns `rummikub.view.view-helpers)))

(def app-handler
  (->
    rummikub-routes
    (wrap-view-context :template-root "rummikub/view" :ns `rummikub.view.view-helpers)))

