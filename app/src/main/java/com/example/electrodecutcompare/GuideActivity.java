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
      "<h3>3. High-Speed Event</h3><b>무엇?</b> 짧고 빠른 충격/이벤트 시점을 찾습니다.<br><b>주의</b> 30 fps 영상은 약 33 ms보다 짧은 현상을 놓칠 수 있습니다.<br><br>"+
      "<h3>4. Multi-Cycle / Jerk</h3><b>Jerk</b> = 가속도가 얼마나 갑자기 변하는지. 쉽게 말하면 ‘탁!’ 하고 급격하게 움직이는 정도입니다.<br><b>Timing CV</b> = Cycle 시간의 반복 편차. 작을수록 반복성이 좋습니다.<br><br>"+
      "<h3>5. Common Vibration 보정</h3><b>핵심</b> 실제로 움직이는 것은 Cutter Module입니다. 고정 유닛이 같이 움직이면 공통진동/카메라 흔들림으로 추정하고 Cutter 이동에서 제거합니다.<br><br>"+
      "<h3>6. ROI 품질 분석</h3><b>ROI</b> = 관심영역. Gripper / 전극 선단(Tip) / Nip만 지정해 위치·각도·흔들림을 추적합니다.<br><b>Jitter</b> = 반복 위치의 흔들림 정도.<br><b>Nip Offset</b> = 기준 Nip 중심과의 위치 편차.<br><br>"+
      "<h3>7. Golden 비교</h3><b>Golden</b> = 정상으로 확인된 기준 상태입니다. 현재 영상이 Golden에서 얼마나 벗어났는지 비교합니다.<br><br>"+
      "<h3>8. Top3 Anomaly</h3>영상 전체에서 정상 패턴과 차이가 큰 순간 3개를 우선 표시합니다. 원인 확정이 아니라 ‘먼저 확인할 순간’입니다.<br><br>"+
      "<h3>구간 해석</h3>대기/안정 → 전진 가속 → 고속 이동 → 커팅/작업 → 복귀 → 감속/정지 → 안정화 순으로 봅니다.<br><br>"+
      "<h3>판정 단어</h3><b>정상 후보</b>: 현재 상대지표에서 큰 변화 없음.<br><b>WATCH</b>: 추세 변화가 있어 확인 권장.<br><b>이상 후보</b>: Golden/A-B 차이가 커 원인 확인이 필요한 상태.<br><br>"+
      "<b>중요:</b> 현재 Score는 영상 기반 상대 비교 지표입니다. 실제 NG 한계는 충분한 정상/불량 데이터와 공정 기준으로 별도 검증해야 합니다.";}
}
