import csv
import json

# 1. 파일 경로 설정
FILE_2023 = '한국교통안전공단_자동차결함 리콜현황_20231231.csv'
OUTPUT_FILE = 'integrated_recall_data.json'

def parse_csv_file(filename):
    """지정된 CSV 파일을 파싱하여 RECALL 테이블 구조에 맞는 데이터 리스트를 반환합니다."""
    print(f"[{filename}] 파일 파싱 시작...")
    collected_data = []
    try:
        # UTF-8 (BOM 포함 가능성)으로 인코딩 변경
        with open(filename, 'r', encoding='utf-8-sig') as f:
            reader = csv.reader(f)
            next(reader)  # 헤더 건너뛰기

            for row in reader:
                if not row:
                    continue

                # DB의 RECALL 테이블 및 RecallDTO.java 구조에 맞게 데이터 생성
                data = {
                    'maker': row[0],
                    'modelName': row[1],
                    'recallDate': row[4],
                    'recallReason': row[5],
                    'makeStart': "",  # CSV에 없는 정보는 빈 값으로 처리
                    'makeEnd': ""      # CSV에 없는 정보는 빈 값으로 처리
                }
                collected_data.append(data)
        print(f"[{filename}] 파싱 완료. 총 {len(collected_data)}건의 데이터 수집됨.")
    except FileNotFoundError:
        print(f"\n❌ 오류: '{filename}' 파일을 찾을 수 없습니다. 스크립트와 같은 폴더에 CSV 파일이 있는지 확인해 주세요.")
        return None
    except UnicodeDecodeError as e:
        print(f"\n❌ 인코딩 오류: '{filename}' 파일을 UTF-8로 읽는 데 실패했습니다.")
        print(f"   오류 상세: {e}")
        return None
    except Exception as e:
        print(f"\n❌ '{filename}' 파일 처리 중 오류 발생: {e}")
        return None

    return collected_data

def main():
    """메인 함수: CSV 파일을 파싱하고 JSON으로 저장합니다."""
    
    all_data = parse_csv_file(FILE_2023)
    
    if not all_data:
        print("\n⚠️ 처리할 데이터가 없습니다. CSV 파일 내용을 확인해 주세요.")
        return

    # 최종 JSON 파일로 저장
    try:
        with open(OUTPUT_FILE, 'w', encoding='utf-8') as f:
            json.dump(all_data, f, ensure_ascii=False, indent=4)

        print("\n==============================================")
        print(f"✅ 파싱 및 통합 완료! '{OUTPUT_FILE}' 파일이 생성되었습니다.")
        print(f"   총 {len(all_data)}건의 데이터가 저장되었습니다.")
        print("==============================================")
    except Exception as e:
        print(f"\n❌ JSON 파일 저장 중 오류 발생: {e}")


if __name__ == "__main__":
    main()
