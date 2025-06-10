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

  ;; Rota inicial
  (comp/GET "/" [] "Calculadora de Calorias - API")

  ;; Cadastro de usuário
  (comp/POST "/usuario" req
    (let [body (:body req)]
      (if (val/valida-usuario? body)
        (como-json (db/adicionar-usuario body) 201)
        (como-json {:erro "Usuário inválido"} 400))))

  ;; Registro de transação
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


  ;; Extrato por período
  (comp/GET "/extrato" {{:keys [inicio fim]} :params}
    (if (and inicio fim)
      (como-json {:transacoes (db/registros-no-intervalo inicio fim)})
      (como-json {:erro "Parâmetros 'inicio' e 'fim' são obrigatórios no formato dd/MM/yyyy"} 400)))

  ;; Saldo por período
  (comp/GET "/saldo" {{:keys [inicio fim]} :params}
    (if (and inicio fim)
      (como-json {:saldo (db/saldo-no-intervalo inicio fim)})
      (como-json {:erro "Parâmetros 'inicio' e 'fim' são obrigatórios no formato dd/MM/yyyy"} 400)))

  ;; Extrato total
  (comp/GET "/extrato-total" []
    (como-json {:transacoes-extrato (db/todas-as-transacoes)}))

  ;; Saldo total
  (comp/GET "/saldo-total" []
    (como-json {:saldo (db/somar-calorias)}))

  ;; Listagem de usuários
  (comp/GET "/usuarios" []
    (como-json {:usuarios (db/todos-os-usuarios)}))

  ;; Extrato de ganhos
  (comp/GET "/extrato-ganhos" []
    (como-json {:ganhos (db/filtrar-por-tipo "ganho")}))

  ;; Extrato de perdas
  (comp/GET "/extrato-perdas" []
    (como-json {:perdas (db/filtrar-por-tipo "perda")}))

  ;; Todas as transações
  (comp/GET "/transacoes" []
    (como-json {:transacoes (db/todas-as-transacoes)}))

  ;; Rota 404
  (route/not-found "Rota não encontrada"))

;; Aplicação com middlewares
(def app
  (-> rotas
      (wrap-json-body {:keywords? true})
      (wrap-defaults api-defaults)))
