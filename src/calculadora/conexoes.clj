(ns calculadora.conexoes
  (:require [clj-http.client :as client]
            [cheshire.core :as json]))

;; 🔐 Sua chave da API Ninja
(def BURNED_API_KEY "QViLkzAapUT4G7pf4mXhvg==jECcMGGDxsVgJH5F")

;; 🌐 API de calorias queimadas
(def BURNED_API_URL "https://api.api-ninjas.com/v1/caloriesburned?activity=%s")

(defn pegar-gasto-calorias [atividade]
  (let [query (java.net.URLEncoder/encode atividade "UTF-8")
        url (format BURNED_API_URL query)
        resposta (client/get url {:headers {:X-Api-Key BURNED_API_KEY}})]
    (if (= 200 (:status resposta))
      (json/parse-string (:body resposta) true)
      (throw (ex-info "Erro ao acessar a API de queima de calorias"
                      {:status (:status resposta)})))))

;; 🥦 API de nutrição (ganho de calorias)
(defn pegar-ganho-calorias [alimento]
  (try
    (let [query (java.net.URLEncoder/encode alimento "UTF-8")
          url (str "https://world.openfoodfacts.org/cgi/search.pl?search_terms=" query
                   "&search_simple=1&action=process&json=1")
          resposta (client/get url {:as :json})
          produto (-> resposta :body :products first)
          calorias (get-in produto [:nutriments :energy-kcal_100g])]
      ;; Exibe no terminal para depuração
      (println "📦 Produto retornado:" (:product_name produto))
      (println "🔥 Calorias por 100g:" calorias)
      ;; Retorna compatível com o restante do sistema
      [{:calories calorias}])
    (catch Exception e
      (println "❌ Erro na API OpenFoodFacts:" (.getMessage e))
      [])))



;; 🌍 API de tradução via RapidAPI
(def TRANSLATE_API_URL "https://google-api31.p.rapidapi.com/gtranslate")
(def TRANSLATE_API_KEY "4af24503a6mshadf3b1d33832a9cp1118fdjsnfd8ac5bc0d1d")
(def TRANSLATE_API_HOST "google-api31.p.rapidapi.com")

(defn traduzir [texto origem destino]
  (let [resposta (client/post TRANSLATE_API_URL
                              {:headers {"x-rapidapi-key" TRANSLATE_API_KEY
                                         "x-rapidapi-host" TRANSLATE_API_HOST}
                               :content-type :json
                               :form-params {:text texto
                                             :to destino
                                             :from_lang origem}})]
    (if (= 200 (:status resposta))
      (json/parse-string (:body resposta) true)
      {:erro "Erro ao acessar a API de tradução"})))
