(ns calculadora.db
  (:require [java-time :as time]
            [clojure.string :as str]
            [calculadora.conexoes :as conexao]))

(def usuarios-db (atom []))
(def transacoes-db (atom []))

(defn resetar []
  (reset! usuarios-db [])
  (reset! transacoes-db []))

(defn todos-os-usuarios [] @usuarios-db)
(defn todas-as-transacoes [] @transacoes-db)

(defn filtrar-por-tipo [tipo]
  (filter #(= tipo (:tipo %)) @transacoes-db))

(defn somar-calorias []
  (reduce
   (fn [acc {:keys [tipo calorias]}]
     (case tipo
       "ganho" (+ acc calorias)
       "perda" (- acc calorias)
       acc))
   0
   @transacoes-db))

(defn adicionar-usuario [usuario]
  (let [id (count @usuarios-db)
        novo (assoc usuario :id id)]
    (swap! usuarios-db conj novo)
    novo))

(defn adicionar-transacao [transacao]
  (let [id (count @transacoes-db)
        nova (assoc transacao :id id)]
    (swap! transacoes-db conj nova)
    nova))

(defn br-para-iso [data-str]
  (let [formatter (time/formatter "dd/MM/yyyy")
        local-date (time/local-date formatter data-str)]
    (time/format "yyyy-MM-dd" local-date)))

(defn converter-para-data [str-data]
  (let [formatter (time/formatter "yyyy-MM-dd")]
    (time/local-date formatter str-data)))

(defn registros-no-intervalo [dinicio dfim]
  (let [inicio (converter-para-data (br-para-iso dinicio))
        fim (converter-para-data (br-para-iso dfim))]
    (filter (fn [{:keys [data]}]
              (let [data-registro (converter-para-data (br-para-iso data))]
                (and (not (time/before? data-registro inicio))
                     (not (time/after? data-registro fim)))))
            @transacoes-db)))

(defn saldo-no-intervalo [dinicio dfim]
  (reduce
   (fn [acc {:keys [tipo calorias]}]
     (case tipo
       "ganho" (+ acc calorias)
       "perda" (- acc calorias)
       acc))
   0
   (registros-no-intervalo dinicio dfim)))

(defn extrair-calorias [s]
  (cond
    (number? s) s
    (string? s)
    (let [partes (str/split s #" ")]
      (Double/parseDouble (first partes)))
    :else 0))

(defn extrair-valor [s]
  (when (string? s)
    (when-let [m (re-find #"\((\d+)" s)]
      (Integer/parseInt (second m)))))

(defn registrar-perda [registro index]
  (let [usuario (first @usuarios-db)]
    (if (nil? usuario)
      {:error "Nenhum usuário cadastrado"}
      (let [peso (:peso usuario)
            minutos (:quantidade registro)
            descricao (:descricao registro)
            resposta (conexao/pegar-gasto-calorias descricao peso minutos)]
        (println "Resposta da API de perda:" resposta)
        (if (or (nil? resposta) (:error resposta))
          {:error "Erro ao registrar perda"}
          (let [atividade (nth resposta index nil)]
            (if (nil? atividade)
              {:error "Índice inválido ou resposta vazia da API"}
              (let [calorias-hora (:calories_per_hour atividade)]
                (if (nil? calorias-hora)
                  {:error "Campo 'calories_per_hour' ausente na resposta"}
                  (let [valor (/ (* minutos calorias-hora) 60)]
                    (adicionar-transacao (merge registro {:valor valor :calorias valor}))))))))))))

(defn registrar-ganho [registro index]
  (let [descricao (:descricao registro)
        resposta (conexao/pegar-ganho-calorias descricao)]
    (if (:error resposta)
      {:error "Erro ao registrar ganho"}
      (let [item (nth resposta index)
            calorias (extrair-calorias (:calorias item))
            peso-ref (or (extrair-valor (:quantidade item)) 100)
            gramas (:quantidade registro)
            valor (/ (* calorias gramas) peso-ref)]
        (adicionar-transacao (merge registro {:valor valor :calorias valor}))))))

(defn nova-transacao [registro index]
  (case (:tipo registro)
    "perda" (registrar-perda registro index)
    "ganho" (registrar-ganho registro index)))







;; testeeeeee