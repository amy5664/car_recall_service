import pandas as pd
import random
import os

BASE_DIR = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
CSV_PATH = os.path.join(BASE_DIR, "data", "recall.csv")
OUTPUT_PATH = os.path.join(BASE_DIR, "data", "recall_with_dummy.csv")

# 비리콜 문장 생성
subjects = ["차량", "엔진", "전자 장치", "공조 장치", "센서", "배터리 관리 시스템", "내부 장치", "제동 장치"]
issues = ["일시적인 오류", "간헐적인 소음", "경미한 진동", "반응 지연", "표시등 점멸", "출력 저하", "센서 오작동", "정상 상태"]
effects = [
    "안전에는 영향이 없습니다.",
    "성능에는 문제가 없습니다.",
    "주행에는 큰 영향이 없습니다.",
    "차량 운행에는 지장이 없습니다.",
    "사용자에게 위험을 주지 않습니다.",
    "모든 기능이 정상 작동합니다.",
    "차량 상태가 정상입니다.",
    "문제가 없습니다."
]

# 각종 더미 문장 추가
def generate_nonrecall_reason():
    type_ = random.random()

    if type_ < 0.4:
        return f"{random.choice(subjects)}에서 {random.choice(issues)}가 발생할 수 있으나 {random.choice(effects)}"

    elif type_ < 0.7:
        complaints = ["시트 가죽에 주름이 있습니다", "에어컨 냄새가 납니다", "컵홀더가 헐겁습니다",
                      "네비게이션 터치가 가끔 느립니다", "와이퍼 소리가 큽니다", "도어 닫는 소리가 둔탁합니다"]
        return random.choice(complaints)

    else:
        normals = ["차량 상태가 전반적으로 양호합니다.", "점검 결과 이상이 없습니다.", "소모품 교체 주기가 되었습니다."]
        return random.choice(normals)

def create_nonrecall_dummy(df, ratio=3):
    nonrecalls = []
    for _, row in df.iterrows():
        for _ in range(ratio):
            new_row = row.copy()
            new_row["리콜사유"] = generate_nonrecall_reason()
            new_row["target"] = 0  # 비리콜
            nonrecalls.append(new_row)
    dummy_df = pd.DataFrame(nonrecalls)
    return dummy_df

if __name__ == "__main__":
    df = pd.read_csv(CSV_PATH, encoding="cp949")
    df["target"] = 1  # 리콜 데이터는 1

    dummy_df = create_nonrecall_dummy(df, ratio=2)

    full_df = pd.concat([df, dummy_df], ignore_index=True)
    os.makedirs(os.path.dirname(OUTPUT_PATH), exist_ok=True)
    full_df.to_csv(OUTPUT_PATH, index=False, encoding="cp949")
    print(f" 더미를 포함한 파일 생성. {OUTPUT_PATH}")