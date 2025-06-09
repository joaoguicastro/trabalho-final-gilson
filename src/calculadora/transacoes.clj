(ns calculadora.transacoes)

(defn valida-usuario? [usuario]
  (every? #(contains? usuario %)
          [:nome :idade :peso :altura :sexo]))

(defn valida-transacao? [t]
  (and (contains? t :tipo)
       (contains? t :descricao)
       (contains? t :data)
       (contains? t :calorias)
       (#{:ganho :perda} (:tipo t))
       (pos? (:calorias t))))