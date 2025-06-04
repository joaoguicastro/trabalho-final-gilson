(ns calculadora.handler
  (:require [compojure.core :as comp]
            [compojure.route :as route]
            [cheshire.core :as json]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]
            [calculadora.db :as db]
            [calculadora.transacoes :as val]
            [calculadora.conexoes :as conexao]))

(defn como-json [conteudo & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string conteudo)})

(comp/defroutes rotas
  (comp/GET "/" [] "Calculadora de Calorias - API")

  ;; Cadastrar usuário
  (comp/POST "/usuario" req
    (let [body (:body req)]
      (if (val/valida-usuario? body)
        (como-json (db/cadastrar-usuario body) 201)
        (como-json {:erro "Usuário inválido"} 400))))

  ;; Registrar transação com cálculo automático de calorias
  (comp/POST "/transacao" req
    (let [{:keys [tipo descricao data quantidade]} (:body req)]
      (cond
        (not (#{:ganho :perda "ganho" "perda"} tipo))
        (como-json {:erro "Tipo deve ser 'ganho' ou 'perda'"} 400)
  
        (nil? descricao)
        (como-json {:erro "Descrição obrigatória"} 400)
  
        :else
        (let [traducao (calculadora.conexoes/traduzir descricao "pt" "en")
              desc-inglês (:translated_text traducao)]
  
          (println "🔎 Descrição original:" descricao)
          (println "🌐 Tradução para inglês:" desc-inglês)
  
          (if (or (nil? desc-inglês) (clojure.string/blank? desc-inglês))
            (como-json {:erro "Tradução inválida ou vazia"} 422)
            (try
              (let [api-data (if (or (= tipo :perda) (= tipo "perda"))
                               (first (calculadora.conexoes/pegar-gasto-calorias desc-inglês))
                               (first (calculadora.conexoes/pegar-ganho-calorias desc-inglês)))
                    _ (println "📡 Dados da API:" api-data)
                    cal-str (or (:calories api-data)
                                (:total_calories api-data)
                                0)
                    calorias (-> cal-str str Double/parseDouble int)
                    quant (or quantidade 1)]
  
                (println "🔥 Calorias encontradas por unidade:" calorias)
  
                (if (pos? calorias)
                  (let [trans {:tipo (name tipo)
                               :descricao descricao
                               :data data
                               :quantidade quant
                               :calorias (* calorias quant)}]
                    (como-json (db/registrar-transacao trans) 201))
                  (como-json {:erro "Calorias não encontradas para a descrição informada"} 422)))
              (catch Exception e
                (println "❌ Erro ao processar transação:" (.getMessage e))
                (como-json {:erro "Erro ao processar calorias ou consultar API externa"} 500))))))))


  ;; Extrato por período
  (comp/GET "/extrato" {{:keys [inicio fim]} :params}
    (como-json {:transacoes (db/transacoes-por-periodo (Integer/parseInt inicio)
                                                       (Integer/parseInt fim))}))

  ;; Saldo por período
  (comp/GET "/saldo" {{:keys [inicio fim]} :params}
    (como-json {:saldo (db/saldo-por-periodo (Integer/parseInt inicio)
                                             (Integer/parseInt fim))}))

  ;; Extrato total
  (comp/GET "/extrato-total" []
    (como-json {:transacoes-extrato (db/transacoes)}))


  ;; Saldo total
  (comp/GET "/saldo-total" []
    (como-json {:saldo (db/saldo-total)}))

  ;; Extrato de ganhos
  (comp/GET "/extrato-ganhos" []
    (como-json {:ganhos (db/transacoes-por-tipo "ganho")}))

  ;; Extrato de perdas
  (comp/GET "/extrato-perdas" []
    (como-json {:perdas (db/transacoes-por-tipo "perda")}))



  (route/not-found "Rota não encontrada")

  (comp/GET "/transacoes" [] (como-json {:transacoes @db/transacoes-db})))

(def app
  (-> rotas
      (wrap-json-body {:keywords? true})
      (wrap-defaults api-defaults)))
