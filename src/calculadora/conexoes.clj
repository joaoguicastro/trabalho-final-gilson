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

(def TRANSLATE_API_URL "https://deep-translate1.p.rapidapi.com/language/translate/v2")
(def TRANSLATE_API_KEY "d4dc508d7cmsh2ca39c84f349520p13da2djsn20e6d424a7f5")
(def TRANSLATE_API_HOST "deep-translate1.p.rapidapi.com")

(defn traduzir [texto origem destino]
  (try
    (let [resposta (client/post TRANSLATE_API_URL
                                {:headers {"x-rapidapi-key" TRANSLATE_API_KEY
                                           "x-rapidapi-host" TRANSLATE_API_HOST
                                           "content-type" "application/json"}
                                 :body (json/generate-string {:q texto
                                                              :source origem
                                                              :target destino})})
          body (json/parse-string (:body resposta) true)]

      (println "RESPOSTA TRADUÇÃO:" body)

      (let [traduzido (get-in body [:data :translations :translatedText 0])]
        (println "Tradução final:" traduzido)
        (if (and traduzido (not (str/blank? traduzido)))
          traduzido
          texto)))

    (catch Exception e
      (println "Erro ao traduzir com Deep Translate, usando original:" (.getMessage e))
      texto)))