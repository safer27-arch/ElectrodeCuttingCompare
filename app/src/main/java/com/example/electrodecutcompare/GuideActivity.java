package com.example.electrodecutcompare;

import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.view.View;
import android.widget.TextView;

public class GuideActivity extends Activity {
    @Override protected void onCreate(Bundle b){
        super.onCreate(b); LanguageManager.init(this); setContentView(R.layout.activity_guide);
        if(android.os.Build.VERSION.SDK_INT>=30)getWindow().setDecorFitsSystemWindows(true);
        TextView body=findViewById(R.id.guideBody);
        body.setText(Html.fromHtml(content(), Html.FROM_HTML_MODE_LEGACY));
        findViewById(R.id.btnGuideClose).setOnClickListener(v->finish());
    }
    private String content(){
        String guide=ReviewText.of(
            "v2.0 측정형 비교 사용법\n\n1. A/B 영상을 선택합니다. 처음에는 같은 영상을 양쪽에 넣어 시험하세요.\n2. 측정형 통합검사를 열고 영상마다 커터의 고유 무늬1곳, 고정부2곳을 지정합니다. A/B가 반대로 보이면 방향 반전을 확인하세요.\n3. 시작 후 측정 상태를 먼저 봅니다. 보류는 불량이 아니라 추적/샘플/사이클 조건이 부족하다는 뜻입니다.\n4. 표와 그래프에서 형태 차이와 실제 시간 차이를 따로 확인합니다. TOP3는 형태 편차 순이며 불량 확정 순위가 아닙니다.\n5. 비교재생은 같은 진행률을 맞춘 보조 확인입니다. 실제 동작 시점의 완전한 정합을 보장하지 않습니다.\n6. 결과 내보내기에서 요약·전체 회차 표·TOP3 전후 A/B 합성 사진·Excel·ZIP을 만듭니다. 공유 앱과 수신자는 직접 선택하세요.\n\n영상은 최대30초, 샘플은 최대30/60/120fps입니다. 휴대폰 성능과 코덱에 따라 속도가 달라집니다. 기존 0~100점은 새 모드에서 사용하지 않습니다. 다음 내용은 이전 참고 분석의 설명입니다.",
            "v2.0 measured comparison\n\n1. Select A/B clips; first test the same clip on both sides.\n2. Select one textured cutter region and two distant fixed-frame regions in each clip. Check direction reversal when views differ.\n3. Check measurement status first. Withheld means tracking, samples or cycles are insufficient, not a defect.\n4. Read shape and actual duration separately. TOP3 ranks shape differences, not certified defects.\n5. Synchronized playback aligns cycle progress, not necessarily the exact physical event.\n6. Export overview, all-cycle tables, TOP3 before/peak/after A/B composites, Excel or ZIP. Choose the sharing app and recipient yourself.\n\nAt most 30 seconds, sampled up to30/60/120fps. Runtime depends on device and codec. Previous0–100 scores are not used. Below is the previous reference-analysis guide.",
            "v2.0 porównanie pomiarowe\n\n1. Wybierz filmy A/B; zacznij od tego samego filmu po obu stronach.\n2. Wskaż detal noża i dwa odległe stałe punkty ramy w każdym filmie. Sprawdź odwrócenie kierunku.\n3. Najpierw sprawdź stan pomiaru. Wstrzymanie oznacza brak wystarczających danych, a nie wadę.\n4. Porównaj osobno kształt i rzeczywisty czas. TOP3 to różnice kształtu, nie potwierdzone wady.\n5. Odtwarzanie synchronizuje postęp cyklu, nie gwarantuje zgodności każdego zdarzenia fizycznego.\n6. Eksportuj podsumowanie, wszystkie cykle, obrazy A/B przed/w chwili/po różnicy, Excel lub ZIP. Sam wybierz aplikację i odbiorcę.\n\nMaksymalnie30sekund i30/60/120fps. Czas zależy od urządzenia i kodeka. Stare punkty0–100 nie są używane. Niżej znajduje się poprzednia instrukcja pomocnicza.",
            "v2.0 вимірювальне порівняння\n\n1. Виберіть A/B; спершу перевірте однакове відео з обох боків.\n2. Позначте деталь різака й дві віддалені нерухомі точки рами на кожному відео. Перевірте зміну напрямку.\n3. Спершу дивіться стан вимірювання. Відкладення означає нестачу даних, а не дефект.\n4. Порівнюйте форму й фактичний час окремо. TOP3 — відмінності форми, не підтверджені дефекти.\n5. Синхронізація узгоджує прогрес циклу, а не гарантує збіг усіх фізичних подій.\n6. Експортуйте підсумок, усі цикли, зображення A/B до/під час/після відмінності, Excel або ZIP. Самі виберіть застосунок і одержувача.\n\nМаксимум30секунд і30/60/120кадрів/с. Час залежить від пристрою й кодека. Старі бали0–100 не використовуються. Нижче — попередня довідкова інструкція.");
        return "<h2>v2.0.0</h2><p>"+Html.escapeHtml(guide).replace("\n","<br>")+"</p><hr><h3>Legacy / 참고용</h3>"+LanguageManager.guideHtml();
    }
}
