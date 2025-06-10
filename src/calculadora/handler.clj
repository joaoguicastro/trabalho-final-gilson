(ns calculadora.handler
  (:require [compojure.core :as comp]
            [compojure.route :as route]
            [cheshire.core :as json]
            [ring.middleware.defaults :refer [wrap-defaults api-defaults]]
            [ring.middleware.json :refer [wrap-json-body]]
            [calculadora.db :as db]
            [calculadora.transacoes :as val]
            [calculadora.conexoes :as conexao]
            [clojure.string :as str]))

(defn como-json [conteudo & [status]]
  {:status (or status 200)
   :headers {"Content-Type" "application/json"}
   :body (json/generate-string conteudo)})

(comp/defroutes rotas

  (comp/GET "/" [] "Calculadora de Calorias - API") 
  
  (comp/POST "/usuario" req
    (let [body (:body req)]
      (if (val/valida-usuario? body)
        (como-json (db/adicionar-usuario body) 201)
        (como-json {:erro "Usuário inválido"} 400))))

  (comp/POST "/transacao" req
  (let [{:keys [tipo descricao data quantidade index]} (:body req)
        index (or index 0)]
    (cond
      (not (#{:ganho :perda "ganho" "perda"} tipo))
      (como-json {:erro "Tipo deve ser 'ganho' ou 'perda'"} 400)

      (nil? descricao)
      (como-json {:erro "Descrição obrigatória"} 400)

      :else
      (try
        (let [descricao-ing (or (conexao/traduzir descricao "pt" "en") descricao)]
          (println "✅ Tradução final:" descricao-ing)
          (let [registro {:tipo (name tipo)
                          :descricao descricao-ing
                          :data data
                          :quantidade quantidade}]
            (como-json (db/nova-transacao registro index))))
        (catch Exception e
          (println "❌ Erro ao registrar transação:" (.getMessage e))
          (como-json {:erro "Erro ao processar transação"} 500))))))


  (comp/GET "/extrato" {{:keys [inicio fim]} :params}
    (if (and inicio fim)
      (como-json {:transacoes (db/registros-no-intervalo inicio fim)})
      (como-json {:erro "Parâmetros 'inicio' e 'fim' são obrigatórios no formato dd/MM/yyyy"} 400)))

  (comp/GET "/saldo" {{:keys [inicio fim]} :params}
    (if (and inicio fim)
      (como-json {:saldo (db/saldo-no-intervalo inicio fim)})
      (como-json {:erro "Parâmetros 'inicio' e 'fim' são obrigatórios no formato dd/MM/yyyy"} 400)))

  (comp/GET "/extrato-total" []
    (como-json {:transacoes-extrato (db/todas-as-transacoes)}))

  (comp/GET "/saldo-total" []
    (como-json {:saldo (db/somar-calorias)}))

  (comp/GET "/usuarios" []
    (como-json {:usuarios (db/todos-os-usuarios)}))

  (comp/GET "/extrato-ganhos" []
    (como-json {:ganhos (db/filtrar-por-tipo "ganho")}))

  (comp/GET "/extrato-perdas" []
    (como-json {:perdas (db/filtrar-por-tipo "perda")}))

  (comp/GET "/transacoes" []
    (como-json {:transacoes (db/todas-as-transacoes)}))

  (route/not-found "Rota não encontrada"))

(def app
  (-> rotas
      (wrap-json-body {:keywords? true})
      (wrap-defaults api-defaults)))
