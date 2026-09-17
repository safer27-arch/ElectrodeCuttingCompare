package com.example.electrodecutcompare;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

public class GuideActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b); setContentView(R.layout.activity_guide);
        if(android.os.Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(true);
        TextView body=findViewById(R.id.guideBody);
        body.setText(Html.fromHtml(content(), Html.FROM_HTML_MODE_LEGACY));
        findViewById(R.id.btnGuideClose).setOnClickListener(v->finish());
    }
    private String content(){return "<h2>분석 가이드 · 왜 / 무엇을 / 어떻게</h2>"+
      "<h3>1. 촬영각 자동 보정</h3><b>왜?</b> A/B 카메라 위치 차이를 설비 차이로 오판하지 않기 위해서입니다.<br><b>어떻게?</b> 고정 구조의 특징을 맞춰 회전·배율·X/Y 위치를 보정합니다.<br><br>"+
      "<h3>2. Cycle 분석</h3><b>무엇?</b> Cutter Module의 반복 운동 패턴을 비교합니다.<br><b>왜?</b> 전진/커팅/복귀 타이밍과 반복성 변화를 찾기 위해서입니다.<br><br>"+
      "<h3>3. Cycle 반복 재현성 / 문제 Cycle 진단</h3><b>무엇?</b> 영상 속 좌→우→좌 왕복을 각각 하나의 Cycle로 자동 분리해 Cycle 1~10을 겹쳐 봅니다.<br><b>왜?</b> 같은 동작을 반복할 때 <b>몇 번째 Cycle이 문제인지</b>, 그리고 그 Cycle의 <b>어떤 동작구간이 문제인지</b> 찾기 위해서입니다.<br><b>어떻게?</b> Cycle 시간을 0~100%로 정규화하고 평균 궤적에서 많이 벗어난 Cycle을 TOP1~3으로 정렬합니다. 각 TOP Cycle은 대기/전진가속/커팅·충격/복귀가속/안정화 중 편차가 가장 큰 동작을 같이 표시합니다.<br><b>Cycle Validation</b> = 지나치게 짧거나 긴 불완전 Cycle을 자동 제외해 반쪽 Cycle 오검출을 줄입니다.<br><b>Repeatability Score</b> = 여러 Cycle이 평균 궤적에 얼마나 촘촘히 겹치는지 나타내는 영상 기반 상대점수입니다.<br><b>Time CV</b> = Cycle 시간의 변동률. 작을수록 반복 시간이 일정합니다.<br><b>A/B 반복재생</b> = A의 대표 정상 Cycle과 B의 Worst Cycle을 좌우에서 원래 속도로 반복 재생합니다.<br><br><h3>검사 모드</h3><b>빠른검사</b>: 문제 Cycle TOP3와 문제 동작을 우선 계산하며 샘플 수를 줄여 현장 확인 시간을 단축합니다.<br><b>표준검사</b>: Cycle + Event + 공통진동 + Top3/Golden을 함께 봅니다.<br><b>정밀검사</b>: ROI/Jerk를 포함한 전체 분석을 수행합니다.<br><br><h3>4. High-Speed Event</h3><b>무엇?</b> 짧고 빠른 충격/이벤트 시점을 찾습니다.<br><b>주의</b> 30 fps 영상은 약 33 ms보다 짧은 현상을 놓칠 수 있습니다.<br><br>"+
      "<h3>5. Multi-Cycle / Jerk</h3><b>Jerk</b> = 가속도가 얼마나 갑자기 변하는지. 쉽게 말하면 ‘탁!’ 하고 급격하게 움직이는 정도입니다.<br><b>Timing CV</b> = Cycle 시간의 반복 편차. 작을수록 반복성이 좋습니다.<br><br>"+
      "<h3>6. Common Vibration 보정</h3><b>핵심</b> 실제로 움직이는 것은 Cutter Module입니다. 고정 유닛이 같이 움직이면 공통진동/카메라 흔들림으로 추정하고 Cutter 이동에서 제거합니다.<br><br>"+
      "<h3>7. ROI 품질 분석</h3><b>ROI</b> = 관심영역. Gripper / 전극 선단(Tip) / Nip만 지정해 위치·각도·흔들림을 추적합니다.<br><b>Jitter</b> = 반복 위치의 흔들림 정도.<br><b>Nip Offset</b> = 기준 Nip 중심과의 위치 편차.<br><br>"+
      "<h3>8. Golden 비교</h3><b>Golden</b> = 정상으로 확인된 기준 상태입니다. 현재 영상이 Golden에서 얼마나 벗어났는지 비교합니다.<br><br>"+
      "<h3>9. Top3 Anomaly</h3>영상 전체에서 정상 패턴과 차이가 큰 순간 3개를 우선 표시합니다. 원인 확정이 아니라 ‘먼저 확인할 순간’입니다.<br><br>"+
      "<h3>구간 해석</h3>대기/안정 → 전진 가속 → 고속 이동 → 커팅/작업 → 복귀 → 감속/정지 → 안정화 순으로 봅니다.<br><br>"+
      "<h3>판정 단어</h3><b>정상 후보</b>: 현재 상대지표에서 큰 변화 없음.<br><b>WATCH</b>: 추세 변화가 있어 확인 권장.<br><b>이상 후보</b>: Golden/A-B 차이가 커 원인 확인이 필요한 상태.<br><br>"+
      "<b>중요:</b> 현재 Score는 영상 기반 상대 비교 지표입니다. 실제 NG 한계는 충분한 정상/불량 데이터와 공정 기준으로 별도 검증해야 합니다.";}
}
