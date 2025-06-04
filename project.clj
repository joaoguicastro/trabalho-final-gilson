(defproject calculadora-calorias "0.1.0-SNAPSHOT"
  :description "Calculadora de Calorias - Projeto de Programação Funcional"
  :dependencies [[org.clojure/clojure "1.11.1"]
                 [compojure "1.7.0"]
                 [ring/ring-defaults "0.3.3"]
                 [ring/ring-json "0.5.1"]
                 [cheshire "5.11.0"]
                 [clj-http "3.12.3"]]
  :main calculadora.handler
  :plugins [[lein-ring "0.12.5"]]
  :ring {:handler calculadora.handler/app})
