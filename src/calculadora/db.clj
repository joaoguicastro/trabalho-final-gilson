(ns calculadora.db)

(def usuarios (atom []))
(def transacoes-db (atom []))

(defn limpar [] (do (reset! usuarios []) (reset! transacoes-db [])))

(defn transacoes [] @transacoes-db)

(defn trasacoes-extrato [] @transacoes-db)

(defn transacoes-por-tipo [tipo]
  (filter #(= tipo (:tipo %)) @transacoes-db))

(defn saldo-total []
  (reduce (fn [acc {:keys [tipo calorias]}]
            (if (= tipo "ganho")
              (+ acc calorias)
              (- acc calorias)))
          0
          @transacoes-db))

(defn cadastrar-usuario [usuario]
  (let [novo (merge usuario {:id (count @usuarios)})]
    (swap! usuarios conj novo)
    novo))

(defn registrar-transacao [transacao]
  (let [nova (merge transacao {:id (count @transacoes-db)})]
    (swap! transacoes-db conj nova)
    nova))

(defn transacoes-por-tipo [tipo]
  (filter #(= tipo (:tipo %)) @transacoes-db))

(defn transacoes-por-periodo [inicio fim]
  (filter #(let [d (:data %)]
             (and (>= d inicio) (<= d fim)))
          @transacoes-db))

(defn saldo-por-periodo [inicio fim]
  (reduce (fn [acc {:keys [tipo calorias data]}]
            (if (and (>= data inicio) (<= data fim))
              (if (= tipo "ganho") (+ acc calorias) (- acc calorias))
              acc))
          0
          @transacoes-db))
