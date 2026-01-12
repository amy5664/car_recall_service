import os
import pickle
import pandas as pd
import numpy as np
from flask import Flask, request, jsonify
from konlpy.tag import Okt
from sklearn.metrics.pairwise import cosine_similarity
from tensorflow.keras.models import load_model
from tensorflow.keras.preprocessing.sequence import pad_sequences

app = Flask(__name__)

# 경로 설정
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
MODEL_DIR = os.path.join(BASE_DIR, "model")
DATA_PATH = os.path.join(BASE_DIR, "data", "labeled_recall_data.csv")

okt = Okt()
STOPWORDS = set(['안녕', '안녕하세요', '질문', '문의', '문제', '차량', '자동차', '기능', '때문', '경우', '발생', '가능성', '확인', '위해'])

def ko_tokenize(text):
    malist = okt.pos(text, stem=True)
    clean_words = []
    for word, tag in malist:
        # 명사, 알파벳, 숫자만 허용
        if tag in ['Noun', 'Alpha', 'Number']:
            if len(word) > 1 and word not in STOPWORDS:
                clean_words.append(word)
    return clean_words

print("모델 및 데이터 로딩")

# 모델 로드
with open(os.path.join(MODEL_DIR, "recall_model.pkl"), "rb") as f:
    logistic_model = pickle.load(f)
with open(os.path.join(MODEL_DIR, "recall_vectorizer.pkl"), "rb") as f:
    tfidf_vectorizer = pickle.load(f)
lstm_model = load_model(os.path.join(MODEL_DIR, "category_lstm_model.h5"))
with open(os.path.join(MODEL_DIR, "tokenizer.pkl"), "rb") as f:
    lstm_tokenizer = pickle.load(f)
with open(os.path.join(MODEL_DIR, "label_encoder.pkl"), "rb") as f:
    le = pickle.load(f)

# 데이터 로드
if os.path.exists(DATA_PATH):
    df_recall = pd.read_csv(DATA_PATH, encoding='cp949')
else:
    df_recall = pd.read_csv(os.path.join(BASE_DIR, "data", "recall.csv"), encoding='cp949')
    df_recall['category'] = "알수없음"


print("데이터 벡터화")
X_all_vec = tfidf_vectorizer.transform(df_recall["리콜사유"].astype(str))

print("서버가 켜졌습니다.")

@app.route("/predict", methods=["POST"])
def predict():
    data = request.get_json()
    defect_text = data.get("defect_text", "").strip()

    if not defect_text:
        return jsonify({"error": "내용을 입력해주세요."}), 400

    tokens = ko_tokenize(defect_text)
    if len(tokens) == 0:
        return jsonify({
            "recall_probability": 0.0,
            "predicted_part": "분석 불가",
            "similar_case": {
                "reason": "분석할 수 있는 핵심 키워드(부품명, 증상 등)가 없습니다.",
                "category": "기타",
                "similarity": 0.0
            }
        })

    input_vec = tfidf_vectorizer.transform([defect_text])
    if input_vec.nnz == 0:
        return jsonify({
            "recall_probability": 0.0,
            "predicted_part": "분석 불가",
            "similar_case": {
                "reason": "학습 데이터에 없는 단어입니다.",
                "category": "기타",
                "similarity": 0.0
            }
        })

    #  정상 수치 분석
    recall_prob = float(logistic_model.predict_proba(input_vec)[0][1])

    # 유사도 분석
    sim_scores = cosine_similarity(input_vec, X_all_vec).flatten()
    best_idx = sim_scores.argmax()
    best_score = float(sim_scores[best_idx])

    # 유사도가 너무 낮으면 0으로 나타내게 설정
    if best_score < 0.1:
        recall_prob = 0.0
        similar_case_reason = "유사한 리콜 사례가 없습니다."
    else:
        similar_case_reason = df_recall.iloc[best_idx]["리콜사유"]

    similar_case = {
        "reason": similar_case_reason,
        "category": df_recall.iloc[best_idx].get("category", "기타"),
        "similarity": best_score
    }

    # LSTM 예측
    tokenized_text = " ".join(okt.morphs(defect_text, stem=True)) # LSTM은 그냥 원래대로 형태소 분석 (단어 제한 X)
    seq = lstm_tokenizer.texts_to_sequences([tokenized_text])
    pad = pad_sequences(seq, maxlen=100, padding='post')
    pred_prob_list = lstm_model.predict(pad, verbose=0)[0]
    pred_category_idx = pred_prob_list.argmax()
    pred_category_name = le.inverse_transform([pred_category_idx])[0]

    return jsonify({
        "recall_probability": recall_prob,
        "predicted_part": pred_category_name,
        "similar_case": similar_case
    })

@app.route("/recommend", methods=["POST"])
def recommend():
    data = request.get_json()
    defect_text = data.get("defect_text", "").strip()


    if not defect_text:
        return jsonify([])


    tokens = ko_tokenize(defect_text)
    if len(tokens) == 0:
        return jsonify([])

    input_vec = tfidf_vectorizer.transform([defect_text])
    if input_vec.nnz == 0:
        return jsonify([])

    sim_scores = cosine_similarity(input_vec, X_all_vec).flatten()
    top_indices = sim_scores.argsort()[::-1][:10]

    recommendations = []
    for idx in top_indices:
        score = float(sim_scores[idx])

        if score < 0.1:
            continue

        rec = {
            "maker": df_recall.iloc[idx]["제작자"],
            "modelName": df_recall.iloc[idx]["차명"],
            "recallDate": str(df_recall.iloc[idx]["리콜개시일"]),
            "recallReason": df_recall.iloc[idx]["리콜사유"],
            "similarity": score
        }
        recommendations.append(rec)

    return jsonify(recommendations)

if __name__ == "__main__":
    app.run(host="0.0.0.0", port=5000)
