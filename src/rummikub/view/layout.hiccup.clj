(doctype :html5)
[:html
 [:head
  [:meta {:http-equiv "Content-Type" :content "text/html" :charset "iso-8859-1"}]
  [:title "rummikub"]
  (include-css "/stylesheets/rummikub.css")
  (include-js "/javascript/rummikub.js")]
 [:body
  (eval (:template-body joodo.views/*view-context*))
]]