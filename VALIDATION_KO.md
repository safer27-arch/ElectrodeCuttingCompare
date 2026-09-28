# v2.0.0 검증 기록

2026-09-27 작성. 합성 시험과 소스 검사 기록이며 설비 정확도 성적서가 아닙니다.

## 이 환경에서 실행한 시험

`MotionCoreTest.java`를 javac로 컴파일한 뒤 실행했습니다. 아래 15개 assertion이 통과했습니다.

1. 주기 궤적의 측정 조건 통과
2. 완전 사이클 검출
3. 실제 타임스탬프의 사이클 길이 보존
4. 동일한 회차의 반복 퍼짐이 거의 0
5. 정지 궤적에서 가짜 사이클 생성 안 함
6. 긴 추적 손실에서 판정 보류
7. 50% 사이클 시간 차이를 DTW로 숨기지 않음
8. 동일 형태의 시간 정규화 비교
9. 특정 회차 스트로크 증가 보존
10. 누적 끝점 위치 밀림 보존
11. 회차당 표본 수 부족에서 보류
12. 밝기 이득/오프셋 변화가 있는 합성 이미지의 알려진 위치 이동 NCC 추적
13. 긴 회차 삭제하지 않고 시간 특이 플래그 표시
14. 불균일 시간 간격에서도 사이클 시간 보존
15. 반복 무늬가 모호할 때 추적 매칭 보류

`ExportWorkbookTest.java`를 실행하여 합성 XLSX를 생성했습니다. ZIP 내부 OOXML, 한글/폴란드어/우크라이나어와 XML 특수문자, 숫자 소수6자리, `=1+1`의 수식 실행 방지를 확인했습니다. artifact_tool로 다시 읽어 수치/텍스트를 검사하고 B_Cycles 표의 렌더링을 확인했습니다. 시험 파일은 실제 설비 데이터가 아닙니다.

프로젝트 Java 34파일은 Java compiler parser로 **문법만** 검사했습니다. Android API 형식/링크 검사와 같지 않습니다. XML 및 ZIP 구조도 검사했습니다. 자세한 자동 결과는 같은 폴더의 최종 검증 로그에 기록합니다.

## 실행하지 못한 항목

- Android SDK를 이용한 전체 APK 컴파일, DEX/리소스 링크, 서명 완료 APK 실행
- 실제 삼성폰의 MediaCodec YUV 코덱 지원, 온도·메모리·시간 측정
- 원래 사용자 A/B 영상의 수동 정답/실측센서 대비 정확도
- 카카오톡·메일 실제 앱에서 첨부 열기, 이미지 순서/크기 제한
- 네 언어 전체 문자열 길이·접근성·화면 회전·앱 중단/복원 실기기 검사

따라서 ‘생산 검증 완료’, ‘정확도 보장’, ‘1초 분석 보장’으로 해석하면 안 됩니다. 순차 고밀도 추적은 기존 프레임 일부만 읽는 모드보다 느릴 수 있습니다. 이전 모드는 전문가 참고용으로 남아 있습니다.

## 현장 첫 테스트 권장 순서

1. 현재 고정서명으로 GitHub Actions 빌드가 초록색인지 확인합니다.
2. 기존 앱을 삭제하지 않고 버전2.0.0 APK로 업데이트합니다.
3. 짧은 H.264 영상 한 개를 A와 B에 동일하게 지정합니다. 양쪽에 동일한 세 영역을 지정합니다.
4. 실제 왕복 횟수와 앱 횟수를 대조합니다. 같은 영상의 A/B 시간/형태 차이가 거의0인지 확인합니다.
5. 다른 영상을 B에 넣고 영역을 지정합니다. 불확실 결과는 ‘불량’ 대신 보류하는지 확인합니다.
6. 실제 센서/수동 라벨과 비교하여 NCC/샘플/사이클 기준을 조정합니다.
7. 보고서를 먼저 미리 보고 자신에게 카톡/메일 시험 전송합니다. 수신 앱에서 이미지와 Excel을 직접 열어 확인합니다.

## 재현 명령 (Java 17 이상)

```sh
mkdir -p /tmp/cutter-tests
javac -d /tmp/cutter-tests app/src/main/java/com/example/electrodecutcompare/MotionCore.java app/src/main/java/com/example/electrodecutcompare/ReviewXlsx.java validation/MotionCoreTest.java validation/ExportWorkbookTest.java
java -cp /tmp/cutter-tests MotionCoreTest
java -cp /tmp/cutter-tests ExportWorkbookTest /tmp/cutter-test.xlsx
```
