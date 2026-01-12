import pandas as pd
import os
import pickle
from konlpy.tag import Okt
from sklearn.feature_extraction.text import TfidfVectorizer
from sklearn.linear_model import LogisticRegression
from sklearn.model_selection import train_test_split
from sklearn.calibration import CalibratedClassifierCV

# 절대경로 설정
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
DATA_PATH = os.path.join(BASE_DIR, "data", "recall_with_dummy.csv")
MODEL_DIR = os.path.join(BASE_DIR, "model")
MODEL_PATH = os.path.join(MODEL_DIR, "recall_model.pkl")
VECTORIZER_PATH = os.path.join(MODEL_DIR, "recall_vectorizer.pkl")

os.makedirs(MODEL_DIR, exist_ok=True)


okt = Okt()


STOPWORDS = set(['안녕', '안녕하세요', '질문', '문의', '문제', '차량', '자동차', '기능', '때문', '경우', '발생', '가능성', '확인', '위해'])

def ko_tokenize(text):

    malist = okt.pos(text, stem=True)

    clean_words = []
    for word, tag in malist:
        if tag in ['Noun', 'Alpha', 'Number']:
            if len(word) > 1 and word not in STOPWORDS:
                clean_words.append(word)
    return clean_words

if __name__ == "__main__":

    df = pd.read_csv(DATA_PATH, encoding="cp949")
    X = df["리콜사유"]
    y = df["target"]

    vectorizer = TfidfVectorizer(
        tokenizer=ko_tokenize,
        token_pattern=None,
        max_features=5000,
        ngram_range=(1, 2),
        min_df=2,
        max_df=0.8
    )
    X_vec = vectorizer.fit_transform(X)

    X_train, X_test, y_train, y_test = train_test_split(
        X_vec, y, test_size=0.2, random_state=42, stratify=y
    )


    base_model = LogisticRegression(max_iter=1000, class_weight='balanced', solver='liblinear')
    model = CalibratedClassifierCV(base_model, method='sigmoid', cv=5)
    model.fit(X_train, y_train)


    pickle.dump(model, open(MODEL_PATH, "wb"))
    pickle.dump(vectorizer, open(VECTORIZER_PATH, "wb"))
    print(f"모델 및 벡터라이저 저장 완료: {MODEL_PATH}")
    print("학습 정확도 체크용 로그:", model.score(X_train, y_train))
    print("테스트 정확도 체크용 로그:", model.score(X_test, y_test))