# ElectrodeCuttingCompare v1.9.0

## Easy Problem Finder · Graph + Table

이번 버전은 **사용자가 글을 읽기 전에 그래프와 표만으로 문제 Cycle과 문제 동작을 찾는 것**에 초점을 둡니다.

- 5단계 동작구간 차이 막대그래프
- 문제 집중 동작 자동 강조
- 문제 Cycle TOP3 표
- 그래프/표 터치 전체화면 확대
- 기존 A/B TOP3 비교재생, Heatmap, Fast Engine 유지
- Permanent Signing 유지

# ElectrodeCuttingCompare v1.8.4

현재 핵심: Fast Engine + Cycle Diagnosis + TOP3 핵심 카드 + A/B Worst Cycle Replay.
문제 Cycle과 문제 동작구간을 먼저 보여주고, 상세 그래프/ROI는 아래에서 확인합니다.

# ElectrodeCuttingCompare v1.8.0 Fast Engine

## v1.8 핵심
- 빠른검사에서 **A/B Cycle Trace를 병렬 처리**합니다.
- Android 9(API 28)+에서 **Batch Frame Scan**을 우선 사용해 반복 random seek를 줄입니다.
- 빠른검사는 Cycle 진단에 필요하지 않은 픽셀 Difference/촬영각 정밀 Transform 단계를 생략합니다.
- Batch Scan이 지원되지 않는 영상/기기에서는 Scaled Random Scan으로 자동 fallback 합니다.
- 결과에 **사용 Engine / 샘플 수 / A-B Trace 추출시간 / 전체 검사시간**을 표시합니다.
- Worst Cycle TOP3, 문제 동작구간, Heatmap, A/B 좌우 반복재생은 그대로 유지합니다.
- Permanent Signing을 유지하므로 기존 설치 앱 위에 업데이트합니다.

> 빠른검사는 문제 Cycle 탐색용입니다. 픽셀 Difference, ROI, Jerk 등 상세 원인 분석은 표준/정밀검사를 사용하세요.

# ElectrodeCuttingCompare v1.5.0

## 새 기능: Cycle Repeatability
- Cutter 왕복 1 Cycle 자동 분리
- A/B 각각 최대 10 Cycle 궤적 Overlay
- Cycle별 시간을 0~100%로 정규화해 촬영 길이가 달라도 반복 패턴 비교
- 평균 궤적, Cycle Time CV, 반복 재현성 Score, A/B 최대 차이 구간 표시
- 원터치 통합검사에 자동 포함
- 그래프 터치 시 전체화면 확대/핀치줌

## A/B 사용 원칙
- A = 기준영상(정상 권장)
- B = 비교영상(검사 대상)

## 업데이트 설치
Permanent Signing workflow를 유지합니다. 동일 applicationId + 동일 signing key + 증가된 versionCode를 사용하므로 기존 고정키 앱 위에 업데이트 설치합니다.

# ElectrodeCuttingCompare v1.4.2 Update Test

# ElectrodeCuttingCompare v1.3 Field Dashboard

현장 사용자는 A/B 영상을 선택한 뒤 **통합검사 START** 한 번으로 분석을 실행합니다. 상단 Dashboard에서 종합판정, 5단계 Cutter 추정 상태, Top3 이상순간과 순간 프레임을 먼저 확인하고, 필요할 때만 전문가 상세분석을 펼칩니다.

> 주의: 영상 기반 상대 진단 도구이며 검증된 NG 기준 확보 전에는 불량 확정 판정으로 사용하지 않습니다. 30fps 영상은 약 33ms보다 짧은 이벤트를 놓칠 수 있습니다.

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


## v1.4.3 TOP3 A/B Side-by-Side + Zoom
- TOP 1/2/3 이상 순간을 각 순위별로 **왼쪽 A 기준영상 / 오른쪽 B 비교영상**으로 나란히 표시합니다.
- 각 A/B 비교 이미지는 발생 시점과 구간명을 함께 표시합니다.
- TOP3 이미지를 누르면 기존 전체화면 Zoom 화면으로 이동하여 핀치 확대 및 드래그 이동이 가능합니다.
- B 영상은 가능한 경우 기존 촬영각 자동 보정값을 적용한 비교 프레임으로 표시합니다.
- Permanent Signing workflow를 유지하여 기존 설치 앱 위에 업데이트 설치할 수 있도록 versionCode를 증가시켰습니다.

## v1.6 Cycle Intelligence
v1.5 반복재현성 분석을 확장해 B 비교영상의 Worst Cycle TOP3, Cycle 편차 Heatmap, 5개 동작구간별 반복성 Spread, Cycle Time Trend를 추가했습니다. 통합검사 START 한 번으로 자동 실행되며 결과 그래프는 기존처럼 확대해서 확인할 수 있습니다.

## v1.7.1 Cycle Diagnosis + Fast Inspection + Replay
- 통합검사 모드 3단계: 빠른검사 / 표준검사 / 정밀검사
- 빠른검사는 Cycle Diagnosis를 우선하고 프레임 샘플 수를 줄여 현장 확인 시간을 단축
- High-Speed Event 결과는 통합검사 내 Advanced/Diagnostic에서 Cache로 재사용
- ROI 정밀분석은 이미 계산된 촬영각 보정값을 재사용
- Cycle 경계 검증: 지나치게 짧거나 긴 Cycle을 불완전 Cycle로 제외
- B 비교영상 Worst Cycle TOP1/2/3 각각에 대해 문제 동작구간을 자동 표시
- A 대표 정상 Cycle과 B Worst Cycle을 좌우에서 원래 속도로 반복재생
- 반복재생 화면에서 동시 다시 시작 / 일시정지 지원
- Permanent Signing 유지, versionCode 25

주의: 영상 기반 상대진단입니다. Cycle 구간명과 편차는 센서 실측/NG 확정값이 아니며 실제 불량 기준은 별도 검증이 필요합니다.


## v1.8.2
- TOP3 즉시 A/B 비교재생
- 기준영상 신뢰도 배너
- Permanent Signing 유지


## v1.8.3
- Worst Cycle 좌우 비교재생에 A/B Cycle 진행률(0~100%) 막대 추가
- 현재 A/B 진행률과 Δ%를 실시간 표시
- 진행률 동기화 모드 추가: 짧은 Cycle을 느리게 재생해 같은 동작 위치를 비교
- 원속도 모드는 그대로 유지해 실제 Cycle Time 차이 관찰 가능
- Permanent Signing 유지


## v1.8.4 TOP3 Direct Replay UI Fix
- 문제 Cycle TOP3 결과 바로 아래에 TOP1/TOP2/TOP3 A↔B 비교재생 버튼을 항상 표시하도록 수정했습니다.
- v1.8.3에서 버튼이 활성화되어도 replayPanel이 GONE 상태로 남던 UI 버그를 수정했습니다.
- 각 버튼에 Cycle 번호, 문제 동작구간, 편차율을 표시합니다.
- 버튼을 누르면 A 기준 대표 Cycle과 B 문제 Cycle이 좌우 동시 반복재생됩니다.
- 기존 진행률 0~100%, Δ 진행률, 진행률 동기화 기능은 그대로 유지합니다.
- Permanent Signing 유지: 기존 앱 위에 업데이트 설치합니다.
