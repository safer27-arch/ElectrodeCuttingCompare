# ElectrodeCuttingCompare v0.3.1 (GitHub Ready)

저사양 Android 스마트폰에서 동작하도록 설계한 전극 컷팅 설비 A/B 동영상 비교 앱 프로토타입입니다.

## v0.3 추가 기능
- v0.2 기능 전체 유지
- A/B 대표 프레임 기준 촬영각 자동 보정
- Gripper / 전극 선단 / Nip 3개 ROI 경량 추적
- 설비 B는 A 기준 카메라 Transform을 적용한 뒤 ROI 분석
- 전극 선단 → Nip 진입각 추정
- Nip 상대 Offset(px) 계산
- Gripper → 전극 선단 상대거리 계산
- 전극 선단 흔들림(RMS px) 계산
- Gripper 흔들림(RMS px) 계산
- 0~100 상대 안정 Score
- A/B 품질지표 비교 그래프
- ROI가 표시된 분석 화면 저장
- Cycle Motion 비교 결과와 ROI 비교 결과를 하나의 결과 이미지에 함께 저장

## 기존 기능
- 설비 A/B 영상 선택 / 재생
- 대표 프레임 위치 선택
- 회전 / 확대축소 / X-Y 이동 자동 보정
- Difference Map
- CPU 기반 Cycle Motion 분석
- CUT → GRIP → FORWARD → NIP → OPEN → RETURN 단계 후보
- GitHub Actions APK 자동 빌드

## 꼭 알아둘 점
v0.3의 ROI 검출은 무거운 AI 모델 대신 저사양 스마트폰용 Edge/Contrast 기반 휴리스틱입니다.
현재 제공받은 컷팅 설비 영상 형태를 기준으로 넓은 ROI를 설정했으며, 카메라 위치가 크게 달라지거나
설비 구조가 달라지면 ROI 위치 튜닝이 필요할 수 있습니다.

표시되는 px와 Score는 현재 **상대 비교 및 일별 변화 추세용 지표**입니다.
실제 mm 단위로 사용하려면 화면에 보이는 실물 기준치수 1개를 이용한 Calibration이 필요합니다.
NG 판정 기준은 정상 Golden 영상 여러 개와 실제 이상 영상 또는 공정 기준값을 모은 뒤 확정하는 것이 안전합니다.

## 다음 확장 권장
1. ROI 위치를 앱에서 직접 드래그/저장
2. 실물 기준 길이 Calibration → px를 mm로 변환
3. 설비/라인별 Golden 영상 등록
4. 날짜별 결과 CSV 및 추세 그래프
5. 정상/주의/이상 임계값 설정
6. 이상 시 결과 이미지 자동 공유/Telegram 전송

## APK 빌드
프로젝트 ZIP을 압축 해제하여 GitHub 저장소 루트에 업로드합니다.
GitHub Actions > Build Android APK > Run workflow를 실행합니다.
완료 후 `ElectrodeCuttingCompare-debug-apk` artifact에서 `app-debug.apk`를 받을 수 있습니다.

## 권장 환경
- Android 8.0(API 26) 이상
- GPU 불필요
- CPU 기반 경량 분석


## v0.3.1 GitHub 빌드 보완
이 패키지는 GitHub Actions가 Gradle 8.9를 직접 설치한 뒤 `gradle :app:assembleDebug`를 실행합니다.
따라서 Gradle Wrapper(`gradlew`, `gradlew.bat`, `gradle-wrapper.jar`)가 없어도 GitHub에서 APK를 생성할 수 있습니다.
압축 해제 후 보이는 프로젝트 내용물을 모두 Repository 최상위에 올리면 됩니다.
