import pandas as pd
import numpy as np
import pickle
import os
from konlpy.tag import Okt
from tensorflow.keras.preprocessing.text import Tokenizer
from tensorflow.keras.preprocessing.sequence import pad_sequences
from tensorflow.keras.models import Sequential
from tensorflow.keras.layers import Embedding, LSTM, Dense, Dropout, Bidirectional
from tensorflow.keras.utils import to_categorical
from tensorflow.keras.callbacks import EarlyStopping
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder

# 1. 경로 설정
BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_PATH = os.path.join(BASE_DIR, "data", "recall.csv")
MODEL_DIR = os.path.join(BASE_DIR, "model")
os.makedirs(MODEL_DIR, exist_ok=True)

# 2. 키워드 정의
KEYWORD_MAP = {
    "제동 장치": ["브레이크", "제동", "ABS", "캘리퍼", "마스터 실린더", "EPB", "리타더", "주차", "밀림", "진공 펌프", "페달", "패드", "라이닝"],
    "엔진": ["엔진", "연료", "오일", "가속", "시동", "터보", "배기", "촉매", "흡기", "인젝터", "타이밍", "동력", "누유", "RPM", "냉각수", "라디에이터", "워터펌프", "호스", "크랭크", "피스톤", "점화", "발동기", "배출가스", "가스", "연소"],
    "변속 및 구동 장치": ["변속", "기어", "드라이브 샤프트", "구동", "클러치", "감속기", "4륜", "트랜스퍼", "디퍼렌셜", "샤프트", "추진축", "프로펠러", "액슬", "등속 조인트", "동력 전달"],
    "조향 및 주행 장치": ["스티어링", "핸들", "조향", "MDPS", "너클", "로어암", "타이로드", "링크", "서스펜션", "쇼크 업소버", "타이어", "휠", "볼트", "너트", "밸런스", "얼라인먼트", "스프링", "스트럿", "주행"],
    "전자 장치": ["소프트웨어", "센서", "배선", "퓨즈", "배터리", "BMS", "충전", "전압", "통신", "카메라", "오디오", "네비게이션", "경고등", "램프", "라이트", "점등", "회로", "컨트롤", "ECU", "스위치", "모듈", "스마트키", "릴레이", "시스템", "계기판", "디스플레이", "업데이트", "로직", "영상", "후방", "이미지"],
    "차체 및 실내": ["시트", "좌석", "안전띠", "에어백", "도어", "창유리", "범퍼", "트렁크", "썬루프", "프레임", "차체", "후드", "와이퍼", "거울", "미러", "잠금", "걸쇠", "레일", "수밀성", "누수", "접착", "몰딩", "소화기", "레버", "페인트", "코팅", "내장재"],
    "공조 장치": ["에어컨", "히터", "공조", "송풍", "냉매", "응축수", "PTC", "바람", "컴프레서", "파이프"]
}


def auto_label(text):
    # isinstance = text가 문자타입이 아닐경우 기타 카테고리 부여
    if not isinstance(text, str): return "기타"

    # ", '을 없애고 strip으로 양쪽 공백 대체
    text_clean = text.replace('"', '').replace("'", "").strip()

    # 가중치 부여용 점수 생성
    score = {category: 0 for category in KEYWORD_MAP.keys()}
    found_any = False

    # 모든 카테고리 순회
    for category, keywords in KEYWORD_MAP.items():
        for keyword in keywords:
            if keyword in text_clean:
                # 전처리한 텍스트에서 키워드가 발견되면 해당 카테고리 점수 +1
                score[category] += 1
                found_any = True

    # 키워드가 하나도 없으면 기타 부여
    if not found_any:
        return "기타"

    # score에서 가장 값이 높은 key값 저장
    category = max(score, key=score.get)

    return category


if __name__ == "__main__": #insert_lstm.py 파일 직접 실행시에만 실행

    #기본 국가 사이트 csv 파일이 EUC-KR이라 cp949로 인코딩. 실패시 utf-8
    try:
        df = pd.read_csv(CSV_PATH, encoding='cp949')
    except:
        df = pd.read_csv(CSV_PATH, encoding='utf-8')

    #원본 보존 겸 학습 프레임 생성
    train_df = df.copy()

    #위에 만든 auto_label 함수로 리콜사유를 분류해서 category 컬러멩 저장
    train_df['category'] = train_df['리콜사유'].apply(auto_label)

    #라벨링한 csv 파일 생성
    DATA_OUTPUT_PATH = os.path.join(BASE_DIR, "data", "labeled_recall_data.csv")
    train_df.to_csv(DATA_OUTPUT_PATH, index=False, encoding='cp949')

    print(f"학습 데이터 개수: {len(train_df)}개")

    #okt = 한국어 자연어 처리를 위한 형태소 단위로 분리
    #okt 사용 이유 = 한국어는 영어와 달라 조사같은게 많아 한국어를 구분해서 학습시키리면 형태소 분석기가 필수.
    okt = Okt()

    #리콜 사유에 있는 항목을 str로 타입 변경.
    X_text = train_df['리콜사유'].astype(str)
    y_category = train_df['category']

    #형태소 단위로 토큰화된 문장을 만들어 모델 학습용 전처리

    #리콜 사유를 전부 돌며 okt.morphs를 이용해 형태소로 쪼개고 문장을 stem단위로 분리. ex)없어요 = 없다.
    #공백을 join해서 문장으로 합침
    #ex) 엔진 오일이 누유돼요 = ['엔진', '오일', '누유', '되다'] -> 엔진 오일 누유 되다.
    X_tokens = [" ".join(okt.morphs(text, stem=True)) for text in X_text]

    #Tokenizer 객체 생성 -> 전처리된 텍스트를 모아 각각 숫자부여. ex)브레이크 1, 엔진 2 -> 텍스트 문장들을 숫자 나열로 변경. ex)브레이크 고장 = [2, 50]
    tokenizer = Tokenizer()
    tokenizer.fit_on_texts(X_tokens)
    X_seq = tokenizer.texts_to_sequences(X_tokens)

    #LSTM은 시퀀스 길이가 동일해야해서 문장 길이 통일. 빈 공간에 0을 집어넣어서 길이 맞춤.
    MAX_LEN = 100
    X_pad = pad_sequences(X_seq, maxlen=MAX_LEN, padding='post')


    le = LabelEncoder()
    y_enc = le.fit_transform(y_category)
    y_onehot = to_categorical(y_enc)

    X_train, X_test, y_train, y_test = train_test_split(X_pad, y_onehot, test_size=0.2, random_state=42, stratify=y_enc)

    vocab_size = len(tokenizer.word_index) + 1

    model = Sequential([
        Embedding(input_dim=vocab_size, output_dim=64, input_length=MAX_LEN),
        Bidirectional(LSTM(32)),
        Dropout(0.4),
        Dense(32, activation='relu'),
        Dense(len(le.classes_), activation='softmax')
    ])

    model.compile(optimizer='adam', loss='categorical_crossentropy', metrics=['accuracy'])
    early_stop = EarlyStopping(monitor='val_loss', patience=3, restore_best_weights=True)

    print("LSTM 모델 학습 시작")
    model.fit(X_train, y_train, epochs=20, batch_size=32, validation_data=(X_test, y_test), callbacks=[early_stop])

    #  저장
    model.save(os.path.join(MODEL_DIR, "category_lstm_model.h5"))
    with open(os.path.join(MODEL_DIR, "tokenizer.pkl"), "wb") as f:
        pickle.dump(tokenizer, f)
    with open(os.path.join(MODEL_DIR, "label_encoder.pkl"), "wb") as f:
        pickle.dump(le, f)

    print("LSTM 학습 완료")