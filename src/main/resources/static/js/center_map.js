const selectedManufacturerFromServer = '${selectedManufacturer}';

let map, places, infowindow;
let myMarker;
let markers = [];
let listItems = [];

let lastKeyword = null;   // 마지막 검색 키워드
let idleSearchTimeout = null; // 지도 이동 후 디바운스용
let isGlobalNameSearch = false;  // 이름으로 전국 검색 중인지 여부

// =============================
// 1. 지도 초기화
// =============================
function initMap() {
    const container = document.getElementById('map');
    const options = {
        center: new kakao.maps.LatLng(35.1795543, 129.0756416), // 부산시청 좌표
        level: 5
    };	

    map = new kakao.maps.Map(container, options);
    places = new kakao.maps.services.Places();
    infowindow = new kakao.maps.InfoWindow({ zIndex: 1 });

    // 지도 이동/줌 끝났을 때 자동 재검색
    kakao.maps.event.addListener(map, 'idle', onMapIdle);
}

// =============================
// 2. 카테고리 → 검색 키워드 매핑
// =============================
function getKeywordByCategory(code) {
    switch (code) {
        case 'CAR':
            return '자동차 정비소';
        case 'BIKE':
            return '오토바이 정비소';
        case 'TRUCK':
            return '트럭 정비소';
        case 'EV':
            return '전기차 정비소';   // 필요하면 '전기차 충전소' 로 바꿔도 됨
        case 'ALL':
        default:
            return '자동차 정비소';
    }
}

// =============================
// 3. 반경 ↔ 지도 레벨 매핑
//    (대략적인 값이라 이 정도만 맞으면 충분)
// =============================
function getLevelFromRadius(radius) {
    if (radius <= 3000) return 5;
    if (radius <= 5000) return 6;
    if (radius <= 10000) return 7;
    return 8;
}

function getRadiusFromLevel(level) {
    // 지도 줌을 줄였을 때, 자동 검색 반경을 넓혀 주는 용도
    switch (level) {
        case 5: return 3000;
        case 6: return 5000;
        case 7: return 10000;
        case 8: return 15000;
        default: return 20000;
    }
}

// =============================
// 4. 마커 / 리스트 정리
// =============================
function clearMarkers() {
    markers.forEach(m => m.setMap(null));
    markers = [];
}

function clearListActive() {
    listItems.forEach(el => el.classList.remove('active'));
}

// =============================
// 5. 실제 검색 실행 (지도 중심 + 반경 + 키워드)
// =============================
function runKeywordSearch(centerLatLng, radius) {
    if (!lastKeyword) return;

    const listEl = document.getElementById('centerList');
    listEl.innerHTML = '검색 중입니다...';

    clearMarkers();

    const searchOptions = {
        location: centerLatLng,
        radius: radius
    };

    places.keywordSearch(lastKeyword, function (data, status) {
        if (status === kakao.maps.services.Status.OK) {
            renderCenterList(data);
            data.forEach((place, idx) => addMarker(place, idx));
        } else if (status === kakao.maps.services.Status.ZERO_RESULT) {
            listEl.innerHTML = '주변에 검색되는 센터가 없습니다.';
        } else {
            listEl.innerHTML = '검색 중 오류가 발생했습니다.';
        }
    }, searchOptions);
}


// 6. 내 위치 기준 검색
function searchByMyLocation() {
    const category = document.getElementById('categorySelect').value;
    const radius = parseInt(document.getElementById('radiusSelect').value, 10);

	// 다시 주변 검색 모드
	isGlobalNameSearch = false;
	
	// 카테고리 기준 키워드 저장
    lastKeyword = getKeywordByCategory(category);

    if (!navigator.geolocation) {
        alert('이 브라우저에서는 위치 정보를 지원하지 않습니다.');
        return;
    }

    navigator.geolocation.getCurrentPosition(function (pos) {
        const lat = pos.coords.latitude;
        const lng = pos.coords.longitude;
        const center = new kakao.maps.LatLng(lat, lng);

        // 내 위치 마커
        if (myMarker) myMarker.setMap(null);
        myMarker = new kakao.maps.Marker({
            map: map,
            position: center
        });

        // 선택한 반경에 맞춰 지도 축소/확대
        const level = getLevelFromRadius(radius);
        map.setLevel(level);
        map.setCenter(center);

        runKeywordSearch(center, radius);
    }, function () {
        alert('위치 정보를 가져올 수 없습니다. 브라우저 권한을 확인해 주세요.');
    });
}

// =============================
// 6-1. 정비소 이름으로 "전체" 검색
// =============================
function searchByName() {
    const input = document.getElementById('centerNameInput');
    if (!input) return;

    const keyword = input.value.trim();
    if (!keyword) {
        alert('검색할 정비소 이름을 입력해 주세요.');
        input.focus();
        return;
    }

	isGlobalNameSearch = true;
    lastKeyword = keyword;

    const listEl = document.getElementById('centerList');
    listEl.innerHTML = '검색 중입니다...';

    clearMarkers();

    const allResults = [];

    function handleResult(data, status, pagination) {
        if (status === kakao.maps.services.Status.OK) {
            // 이번 페이지 결과 누적
            Array.prototype.push.apply(allResults, data);

            // 최대 3페이지까지만 가져오기 (원하면 5까지 늘릴 수도 있음)
            if (pagination.hasNextPage && pagination.current < 3) {
                pagination.gotoPage(pagination.current + 1);
                return;
            }

            // 여기까지 오면 전체 페이지 수집 완료
            if (allResults.length === 0) {
                listEl.innerHTML = '검색 결과가 없습니다.';
                return;
            }

            // 리스트 + 마커 렌더링
            renderCenterList(allResults);
            allResults.forEach((place, idx) => addMarker(place, idx));

            // 결과 전체가 보이도록 지도 bounds 조정
            const bounds = new kakao.maps.LatLngBounds();
            allResults.forEach(place => {
                bounds.extend(new kakao.maps.LatLng(place.y, place.x));
            });
            map.setBounds(bounds);

        } else if (status === kakao.maps.services.Status.ZERO_RESULT) {
            listEl.innerHTML = '검색 결과가 없습니다.';
        } else {
            listEl.innerHTML = '검색 중 오류가 발생했습니다.';
        }
    }

    // 옵션에 location / radius 안 줌 → 전국 검색
    places.keywordSearch(keyword, handleResult, {
        size: 15    // 페이지당 개수 (기본 15)
    });
}

// =============================
// 7. 현재 지도 중심 기준으로 검색 (버튼 없이도 사용)
// =============================
function searchByCurrentCenterWithSelectedRadius() {
    const category = document.getElementById('categorySelect').value;
    const radius = parseInt(document.getElementById('radiusSelect').value, 10);

	// 다시 주변 검색 모드
	isGlobalNameSearch = false;
	
    // 항상 현재 카테고리로 lastKeyword 갱신
    lastKeyword = getKeywordByCategory(category);

    const center = map.getCenter();
    const level = getLevelFromRadius(radius);

    map.setLevel(level);
    map.setCenter(center);

    runKeywordSearch(center, radius);
}

// =============================
// 8. 지도 이동/축소 후 자동 재검색
//    - lastKeyword만 있으면 "현재 화면 중심" 기준 재검색
// =============================
function onMapIdle() {
    if (!lastKeyword) return; // 아직 검색 한 번도 안 했으면 무시

	if (isGlobalNameSearch) return;
	
    if (idleSearchTimeout) clearTimeout(idleSearchTimeout);

    idleSearchTimeout = setTimeout(function () {
        const center = map.getCenter();
        const level = map.getLevel();
        const radius = getRadiusFromLevel(level);

        runKeywordSearch(center, radius);
    }, 400); // 드래그 끝난 뒤 0.4초 대기 후 검색 (디바운스)
}

// =============================
// 9. 마커 추가 (카카오맵 길찾기 목적지 세팅 포함)
// =============================
function addMarker(place, index) {
    const position = new kakao.maps.LatLng(place.y, place.x);
    const marker = new kakao.maps.Marker({
        map: map,
        position: position
    });

    kakao.maps.event.addListener(marker, 'click', function () {
        openInfoWindow(place, marker, index);
    });

    markers.push(marker);
}

// 말풍선 내용 생성 + 리스트 활성화
// 말풍선 내용 생성 + 리스트 활성화
function openInfoWindow(place, marker, index) {
    const addr = place.road_address_name || place.address_name || '';
    const phone = place.phone || '';

    // 현재 클릭한 장소를 목적지로 넣기 (좌표 기반)
    const link =
        'https://map.kakao.com/link/to/' +
        encodeURIComponent(place.place_name) + ',' +
        place.y + ',' + place.x;

		const content =
		        '<div style="' +
		            'padding:8px 10px 6px;' +        // 아래 패딩 줄이기
		            'font-size:12px;' +
		            'line-height:1.5;' +
		            'max-width:240px;' +
		            'box-sizing:border-box;' +
		            'word-break:keep-all;' +
		        '">' +

            // 상호명
            '<div style="font-weight:600;margin-bottom:4px;color:#111827;">'
                + place.place_name +
            '</div>' +

            // 주소
            (addr
                ? '<div style="margin-bottom:2px;color:#4b5563;">' + addr + '</div>'
                : ''
            ) +

            // 전화번호
            (phone
                ? '<div style="margin-top:2px;margin-bottom:6px;color:#6b7280;">TEL: ' + phone + '</div>'
                : ''
            ) +

            // 버튼을 따로 감싸는 영역
			'<div style="margin-top:2px;color:#6b7280;white-space:nowrap;">' +
			            (phone ? 'TEL: ' + phone : '') +
			            '<a href="' + link + '" target="_blank" ' +
			               'style="' +
			                 'display:inline-block;' +
			                 'margin-left:8px;' +          // 전화번호랑 간격
			                 'padding:2px 8px;' +          // 버튼 높이 줄이기
			                 'border-radius:999px;' +
			                 'border:1px solid #2563eb;' +
			                 'font-size:11px;' +
			                 'text-decoration:none;' +
			                 'color:#2563eb;' +
			                 'background:#ffffff;' +
			                 'vertical-align:middle;' +
			               '">' +
			               '카카오맵 길찾기' +
			            '</a>' +
			          '</div>' +

			        '</div>';

    infowindow.setContent(content);
    if (marker) {
        infowindow.open(map, marker);
    } else {
        infowindow.open(map);
    }

    clearListActive();
    const listItem = listItems[index];
    if (listItem) {
        listItem.classList.add('active');
        listItem.scrollIntoView({ behavior: 'smooth', block: 'center' });
    }
}


// =============================
// 10. 리스트 렌더링
// =============================
function renderCenterList(data) {
    const listEl = document.getElementById('centerList');
    listEl.innerHTML = '';
    listItems = [];

    data.forEach((place, index) => {
        const item = document.createElement('div');
        item.className = 'center-item';
        item.innerHTML =
            '<div class="center-item-name">[' + (index + 1) + '] ' + place.place_name + '</div>' +
            '<div class="center-item-addr">' +
            (place.road_address_name || place.address_name || '') +
            '</div>' +
            (place.phone ? '<div class="center-item-tel">TEL: ' + place.phone + '</div>' : '');

        item.addEventListener('click', function () {
            clearListActive();
            item.classList.add('active');

            const pos = new kakao.maps.LatLng(place.y, place.x);
            map.setCenter(pos);
            map.setLevel(4);

            const marker = markers[index];
            if (marker) {
                kakao.maps.event.trigger(marker, 'click');
            } else {
                // 혹시 마커가 없으면 직접 말풍선만 띄움
                openInfoWindow(place, null, index);
                infowindow.setPosition(pos);
            }
        });

        listEl.appendChild(item);
        listItems.push(item);
    });
}

// =============================
// 11. 초기 바인딩
// =============================
document.addEventListener('DOMContentLoaded', function () {
    initMap();

    // 내 위치 기준 검색 버튼
    document.getElementById('btnMyLocation')
        .addEventListener('click', searchByMyLocation);

    // 반경 변경 → 현재 화면 중심 기준으로 다시 검색
    document.getElementById('radiusSelect')
        .addEventListener('change', function () {
            searchByCurrentCenterWithSelectedRadius();
        });

    // 카테고리 변경 → 현재 화면 중심 기준으로 다시 검색
    document.getElementById('categorySelect')
        .addEventListener('change', function () {
            searchByCurrentCenterWithSelectedRadius();
        });
		
		// 정비소 이름 검색 버튼
		   const btnNameSearch = document.getElementById('btnNameSearch');
		   const inputName = document.getElementById('centerNameInput');

		   if (btnNameSearch && inputName) {
		       btnNameSearch.addEventListener('click', searchByName);

		       // 엔터로도 검색
		       inputName.addEventListener('keydown', function (e) {
		           if (e.key === 'Enter') {
		               e.preventDefault();
		               searchByName();
		           }
		       });
		   }
		
    // 필요하면 페이지 진입 시 기본 카테고리로 자동 검색하고 싶을 때:
    // searchByCurrentCenterWithSelectedRadius();
});