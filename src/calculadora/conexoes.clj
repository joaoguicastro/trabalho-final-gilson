(ns calculadora.conexoes
  (:require [clj-http.client :as client]
            [cheshire.core :as json]
            [clojure.string :as str]))

(def BURNED_API_KEY "QViLkzAapUT4G7pf4mXhvg==jECcMGGDxsVgJH5F")
(def BURNED_API_URL "https://api.api-ninjas.com/v1/caloriesburned?activity=%s")

(defn pegar-gasto-calorias [atividade peso duracao]
  (let [query (java.net.URLEncoder/encode atividade "UTF-8")
        url (format BURNED_API_URL query)
        params (cond-> {:activity atividade}
                 peso (assoc :weight peso)
                 duracao (assoc :duration duracao))
        resposta (client/get url {:query-params params
                                  :headers {:X-Api-Key BURNED_API_KEY}})]
    (if (= 200 (:status resposta))
      (json/parse-string (:body resposta) true)
      {:error "Erro ao acessar a API de queima de calorias"})))

(defn pegar-ganho-calorias [alimento]
  (try
    (let [query (java.net.URLEncoder/encode alimento "UTF-8")
          url (str "https://world.openfoodfacts.org/cgi/search.pl?search_terms=" query
                   "&search_simple=1&action=process&json=1")
          resposta (client/get url {:as :json})
          produto (-> resposta :body :products first)
          calorias (get-in produto [:nutriments :energy-kcal_100g])]
      (println "Produto retornado:" (:product_name produto))
      (println "Calorias por 100g:" calorias)
      [{:calorias calorias
        :quantidade "100g"}])
    (catch Exception e
      (println "Erro na API OpenFoodFacts:" (.getMessage e))
      [])))

(def TRANSLATE_API_URL "https://google-api31.p.rapidapi.com/gtranslate")
(def TRANSLATE_API_KEY "4af24503a6mshadf3b1d33832a9cp1118fdjsnfd8ac5bc0d1d")
(def TRANSLATE_API_HOST "google-api31.p.rapidapi.com")

(defn traduzir [texto origem destino]
  (try
    (let [resposta (client/post TRANSLATE_API_URL
                                {:headers {"x-rapidapi-key" TRANSLATE_API_KEY
                                           "x-rapidapi-host" TRANSLATE_API_HOST}
                                 :content-type :json
                                 :form-params {:text texto
                                               :to destino
                                               :from_lang origem}})
          body (json/parse-string (:body resposta) true)
          texto-traduzido (:translated_text body)]
      (if (and texto-traduzido (not (str/blank? texto-traduzido)))
        texto-traduzido
        nil))
    (catch Exception e
      (println "❌ Erro ao traduzir:" (.getMessage e))
      nil)))
