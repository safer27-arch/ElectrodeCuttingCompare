package com.example.electrodecutcompare;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.Spanned;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * Lightweight in-app language layer.
 *
 * The existing project intentionally keeps its analysis engine independent from AndroidX.
 * To avoid touching the numeric/vision logic, visible Korean source strings remain the
 * canonical text and are translated at display time.  This also lets old analysis code
 * and new UI cards share one language switch.
 */
public final class LanguageManager {
    public static final String KO="ko", EN="en", PL="pl", UK="uk";
    private static final String PREF="ui_language_pref_v194";
    private static final String KEY="language";
    private static String current=KO;
    private static boolean initialized=false;

    private LanguageManager(){}

    public static void init(Context c){
        if(c==null)return;
        SharedPreferences p=c.getApplicationContext().getSharedPreferences(PREF,Context.MODE_PRIVATE);
        current=p.getString(KEY,KO);
        if(!KO.equals(current)&&!EN.equals(current)&&!PL.equals(current)&&!UK.equals(current))current=KO;
        initialized=true;
    }
    public static String code(){return current;}
    public static void setLanguage(Context c,String code){
        if(!KO.equals(code)&&!EN.equals(code)&&!PL.equals(code)&&!UK.equals(code))code=KO;
        current=code;initialized=true;
        if(c!=null)c.getApplicationContext().getSharedPreferences(PREF,Context.MODE_PRIVATE).edit().putString(KEY,code).apply();
    }
    public static String flag(){
        if(EN.equals(current))return "🇬🇧";
        if(PL.equals(current))return "🇵🇱";
        if(UK.equals(current))return "🇺🇦";
        return "🇰🇷";
    }
    public static String languageName(){
        if(EN.equals(current))return "English";
        if(PL.equals(current))return "Polski";
        if(UK.equals(current))return "Українська";
        return "한국어";
    }

    public static CharSequence t(Context c,CharSequence in){
        if(!initialized && c!=null)init(c);
        return t(in);
    }
    public static CharSequence t(CharSequence in){
        if(in==null)return "";
        if(KO.equals(current))return in;
        String raw=in.toString();
        if(raw.length()==0)return in;
        // Preserve already-localized styled text so highlight spans survive.
        if(in instanceof Spanned && !containsKorean(raw))return in;
        String out=raw;
        List<Entry> list=ENTRIES;
        for(Entry e:list){
            if(out.contains(e.ko))out=out.replace(e.ko,e.get(current));
        }
        return out;
    }

    public static String ts(String in){return t(in).toString();}

    private static boolean containsKorean(String s){
        for(int i=0;i<s.length();i++){char ch=s.charAt(i);if(ch>='가'&&ch<='힣')return true;}
        return false;
    }

    private static final class Entry{
        final String ko,en,pl,uk;
        Entry(String ko,String en,String pl,String uk){this.ko=ko;this.en=en;this.pl=pl;this.uk=uk;}
        String get(String code){if(EN.equals(code))return en;if(PL.equals(code))return pl;if(UK.equals(code))return uk;return ko;}
    }
    private static final List<Entry> ENTRIES=new ArrayList<>();
    private static void a(String ko,String en,String pl,String uk){ENTRIES.add(new Entry(ko,en,pl,uk));}
    static{
        // Long UI phrases first. Sorting by Korean source length below protects shorter token replacements.
        a("CUTTER MOTION · A/B\nINSPECTOR · 전극 컷팅 공정 진단","CUTTER MOTION · A/B\nINSPECTOR · Electrode Cutting Process Diagnosis","RUCH MODUŁU TNĄCEGO · A/B\nINSPEKTOR · Diagnostyka procesu cięcia elektrody","РУХ РІЗАЛЬНОГО МОДУЛЯ · A/B\nІНСПЕКТОР · Діагностика процесу різання електрода");
        a("Cutter Profile (형상별 Golden/ROI/Trend 분리)","Cutter Profile (separate Golden/ROI/Trend by profile)","Profil noża (osobne Golden/ROI/Trend dla profilu)","Профіль різака (окремі Golden/ROI/Trend для профілю)");
        a("ONE-TOUCH · A 기준영상(정상 권장)과 B 비교영상(검사 대상)을 선택한 뒤 전체 검사를 실행합니다.","ONE-TOUCH · Select A reference video (normal recommended) and B test video, then run the inspection.","ONE-TOUCH · Wybierz film referencyjny A (zalecany stan prawidłowy) i film testowy B, a następnie uruchom kontrolę.","ONE-TOUCH · Виберіть еталонне відео A (рекомендовано нормальний стан) і тестове відео B, потім запустіть перевірку.");
        a("TOP3 비교 이미지 · 왼쪽 A 기준 / 오른쪽 B 문제 Cycle · 누르면 확대. 표준/정밀검사는 촬영각 보정 후 차이 강조도 표시합니다.","TOP3 comparison image · A reference on left / B problem Cycle on right · tap to zoom. Standard/detailed modes also show differences after camera-angle correction.","Obraz porównania TOP3 · po lewej referencja A / po prawej problemowy Cycle B · dotknij, aby powiększyć. Tryb standardowy/dokładny pokazuje też różnice po korekcji kąta kamery.","Зображення порівняння TOP3 · ліворуч еталон A / праворуч проблемний Cycle B · натисніть для збільшення. Стандартний/детальний режим також показує різниці після корекції кута камери.");
        a("빠른검사 v1.9.4 · 문제 Cycle TOP3와 문제 동작구간을 먼저 찾습니다. 무거운 정밀검사는 제외해 현장 확인 시간을 줄입니다.","Fast inspection v1.9.4 · Finds TOP3 problem Cycles and problem motion phases first. Heavy detailed analysis is skipped to shorten field check time.","Szybka kontrola v1.9.4 · Najpierw znajduje TOP3 problemowych cykli i problemowe fazy ruchu. Pomija ciężką analizę dokładną, aby skrócić czas kontroli na linii.","Швидка перевірка v1.9.4 · Спочатку знаходить TOP3 проблемних циклів і проблемні фази руху. Важкий детальний аналіз пропускається, щоб скоротити час перевірки на лінії.");
        a("표준검사 · Cycle Diagnosis에 Event, 공통진동 분리, Top3/Golden을 추가합니다. 일상 점검용 권장 모드입니다.","Standard inspection · Adds Event, common-vibration separation and Top3/Golden to Cycle Diagnosis. Recommended for routine checks.","Kontrola standardowa · Dodaje Event, separację drgań wspólnych oraz Top3/Golden do diagnostyki Cycle. Zalecana do kontroli codziennej.","Стандартна перевірка · Додає Event, відокремлення спільної вібрації та Top3/Golden до діагностики Cycle. Рекомендовано для щоденних перевірок.");
        a("정밀검사 · 기존 전체 분석 8개 + ROI/Jerk까지 수행합니다. 시간이 더 걸리지만 상세 원인 확인에 적합합니다.","Detailed inspection · Runs all 8 analyses plus ROI/Jerk. Takes longer but is suited to detailed cause checks.","Kontrola dokładna · Uruchamia wszystkie 8 analiz oraz ROI/Jerk. Trwa dłużej, ale nadaje się do szczegółowej analizy przyczyn.","Детальна перевірка · Виконує всі 8 аналізів плюс ROI/Jerk. Триває довше, але підходить для детального пошуку причин.");
        a("전체 검사항목 한눈 요약 · 큰 차이부터 바로 확인","All inspection items at a glance · check the largest differences first","Wszystkie pozycje kontroli na jednym ekranie · najpierw największe różnice","Усі пункти перевірки на одному екрані · спочатку найбільші відмінності");
        a("한눈에 보는 문제 지도 · 어느 Cycle / 어느 동작이 문제인지","Problem map at a glance · which Cycle / which motion is different","Mapa problemu · który Cycle / który ruch się różni","Карта проблеми · який Cycle / який рух відрізняється");
        a("▶ 문제 Cycle TOP3 · 눌러서 즉시 A/B 비교","▶ TOP3 problem Cycles · tap for immediate A/B comparison","▶ TOP3 problemowych cykli · dotknij, aby od razu porównać A/B","▶ TOP3 проблемних циклів · натисніть для миттєвого порівняння A/B");
        a("● 빨강 큰 차이   ● 노랑 확인 필요   ● 초록 안정   ● 회색 빠른검사 제외","● Red large difference   ● Yellow check   ● Green stable   ● Gray skipped in fast mode","● Czerwony duża różnica   ● Żółty sprawdź   ● Zielony stabilnie   ● Szary pominięto w szybkim trybie","● Червоний велика різниця   ● Жовтий перевірити   ● Зелений стабільно   ● Сірий пропущено у швидкому режимі");
        a("A · 기준영상 선택 (정상 권장)","A · Select reference video (normal recommended)","A · Wybierz film referencyjny (zalecany stan prawidłowy)","A · Виберіть еталонне відео (рекомендовано нормальний стан)");
        a("B · 비교영상 선택 (검사 대상)","B · Select test video (inspection target)","B · Wybierz film testowy (obiekt kontroli)","B · Виберіть тестове відео (об'єкт перевірки)");
        a("▶ 통합검사 START · 선택 모드로 자동 분석","▶ START INSPECTION · automatic analysis in selected mode","▶ START KONTROLI · automatyczna analiza w wybranym trybie","▶ СТАРТ ПЕРЕВІРКИ · автоматичний аналіз у вибраному режимі");
        a("ⓘ 분석 가이드 · 용어사전","ⓘ Analysis guide · glossary","ⓘ Przewodnik analizy · słownik","ⓘ Посібник з аналізу · словник");
        a("통합검사 Summary 대기","Inspection summary · waiting","Podsumowanie kontroli · oczekiwanie","Підсумок перевірки · очікування");
        a("⚠ 최우선 문제 · 검사 후 자동 표시","⚠ Highest-priority issue · shown after inspection","⚠ Problem o najwyższym priorytecie · pokaże się po kontroli","⚠ Проблема найвищого пріоритету · з'явиться після перевірки");
        a("문제 Cycle TOP3 · 검사 후 표시","TOP3 problem Cycles · shown after inspection","TOP3 problemowych cykli · pokaże się po kontroli","TOP3 проблемних циклів · з'явиться після перевірки");
        a("기준영상 신뢰도 · 검사 후 표시","Reference-video reliability · shown after inspection","Wiarygodność filmu referencyjnego · pokaże się po kontroli","Надійність еталонного відео · з'явиться після перевірки");
        a("종합판정 · 검사 대기","Overall status · waiting for inspection","Ocena ogólna · oczekiwanie na kontrolę","Загальний статус · очікування перевірки");
        a("▼ 전문가 상세분석 보기","▼ Show expert details","▼ Pokaż analizę ekspercką","▼ Показати експертний аналіз");
        a("▲ 전문가 상세분석 닫기","▲ Hide expert details","▲ Ukryj analizę ekspercką","▲ Сховати експертний аналіз");
        a("자동 보정 + A/B 비교 분석","Auto correction + A/B comparison","Autokorekcja + porównanie A/B","Автокорекція + порівняння A/B");
        a("Cycle 자동 분석 + A/B 동작 패턴 비교","Automatic Cycle analysis + A/B motion-pattern comparison","Automatyczna analiza Cycle + porównanie wzorca ruchu A/B","Автоматичний аналіз Cycle + порівняння шаблону руху A/B");
        a("v1.8 Fast Engine · 문제 Cycle TOP3 · 문제 동작 · 빠른 분석","v1.8 Fast Engine · TOP3 problem Cycles · problem motion · fast analysis","v1.8 Fast Engine · TOP3 problemowych cykli · problemowy ruch · szybka analiza","v1.8 Fast Engine · TOP3 проблемних циклів · проблемний рух · швидкий аналіз");
        a("v0.5 고속 동작 이상검출 · Event 자동 동기화","v0.5 High-speed anomaly detection · automatic Event sync","v0.5 Wykrywanie anomalii szybkiego ruchu · automatyczna synchronizacja Event","v0.5 Виявлення аномалій швидкого руху · автоматична синхронізація Event");
        a("v0.6 Multi-Cycle + Timing + Jerk + 예방 Trend 분석","v0.6 Multi-Cycle + Timing + Jerk + preventive Trend analysis","v0.6 Multi-Cycle + Timing + Jerk + prewencyjna analiza Trend","v0.6 Multi-Cycle + Timing + Jerk + превентивний аналіз Trend");
        a("v0.8 쉬운 진단 · Cutter 운동 / 고정부 진동 분리 + 구간별 분석","v0.8 Easy diagnosis · Cutter motion / fixed-unit vibration separation + phase analysis","v0.8 Łatwa diagnostyka · ruch noża / separacja drgań części stałej + analiza faz","v0.8 Проста діагностика · рух різака / відокремлення вібрації нерухомої частини + аналіз фаз");
        a("v0.7 통합진단 · Top3 이상 순간 + Golden 비교","v0.7 Integrated diagnosis · Top3 anomaly moments + Golden comparison","v0.7 Diagnostyka zintegrowana · Top3 chwil anomalii + porównanie Golden","v0.7 Інтегрована діагностика · Top3 моментів аномалії + порівняння Golden");
        a("v0.4 저장 ROI로 전극 선단 + Gripper + Nip 분석","v0.4 Analyze electrode Tip + Gripper + Nip using saved ROI","v0.4 Analiza końcówki elektrody + Gripper + Nip z zapisanym ROI","v0.4 Аналіз кінчика електрода + Gripper + Nip за збереженим ROI");
        a("현재 결과 Golden 등록 (정상 확인 후)","Register current result as Golden (after confirming normal)","Zapisz bieżący wynik jako Golden (po potwierdzeniu normy)","Зареєструвати поточний результат як Golden (після підтвердження норми)");
        a("분석 결과 이미지 저장","Save analysis result image","Zapisz obraz wyniku analizy","Зберегти зображення результату аналізу");
        a("두 손가락 확대 · 드래그 이동 · 원위치=화면 중앙","Pinch to zoom · drag to move · reset = center","Powiększ dwoma palcami · przeciągnij · reset = środek","Збільшення двома пальцями · перетягування · скидання = центр");
        a("노란 구간 = 문제 동작구간 · 동기화 ON = A/B의 같은 0~100% 위치를 맞춰 비교","Yellow area = problem motion phase · Sync ON = compare A/B at the same 0–100% motion position","Żółty obszar = problemowa faza ruchu · Sync ON = porównanie A/B w tej samej pozycji 0–100% ruchu","Жовта зона = проблемна фаза руху · Sync ON = порівняння A/B в однаковій позиції 0–100% руху");
        a("원속도 비교 = 실제 Cycle Time 차이를 그대로 확인 · 필요하면 동기화를 다시 켜세요.","Original-speed comparison = view the actual Cycle Time difference · turn sync back on when needed.","Porównanie z rzeczywistą prędkością = rzeczywista różnica Cycle Time · w razie potrzeby włącz ponownie synchronizację.","Порівняння з реальною швидкістю = фактична різниця Cycle Time · за потреби знову увімкніть синхронізацію.");
        a("⚠ 지금 문제구간 진입 · ","⚠ Entering problem phase · ","⚠ Wejście w problemową fazę · ","⚠ Вхід у проблемну фазу · ");
        a("⚠ 문제구간만 반복","⚠ Loop problem phase only","⚠ Powtarzaj tylko problemową fazę","⚠ Повторювати лише проблемну фазу");
        a("✓ 문제구간 반복 ON","✓ Problem-phase loop ON","✓ Powtarzanie problemowej fazy ON","✓ Повтор проблемної фази ON");
        a("↻ 전체 다시","↻ Restart all","↻ Od początku","↻ Перезапустити все");
        a("⏸ 일시정지","⏸ Pause","⏸ Pauza","⏸ Пауза");
        a("▶ 계속재생","▶ Continue","▶ Kontynuuj","▶ Продовжити");
        a("✓ 동기화 ON","✓ Sync ON","✓ Synchronizacja ON","✓ Синхронізація ON");
        a("⇄ 원속도 비교","⇄ Original-speed comparison","⇄ Porównanie rzeczywistej prędkości","⇄ Порівняння реальної швидкості");
        a("가이드 닫기","Close guide","Zamknij przewodnik","Закрити посібник");
        a("ANALYSIS GUIDE","ANALYSIS GUIDE","PRZEWODNIK ANALIZY","ПОСІБНИК З АНАЛІЗУ");
        a("ROI 설정","ROI Settings","Ustawienia ROI","Налаштування ROI");
        a("ROI 선택","Select ROI","Wybierz ROI","Виберіть ROI");
        a("위치 이동","Move position","Przesuń pozycję","Перемістити позицію");
        a("크기 조절","Resize","Zmień rozmiar","Змінити розмір");
        a("현재 ROI 저장","Save current ROI","Zapisz bieżące ROI","Зберегти поточне ROI");
        a("선택 설비 ROI 전체 초기화","Reset all ROI for selected machine","Zresetuj wszystkie ROI wybranej maszyny","Скинути всі ROI вибраного обладнання");
        a("초록=Gripper / 주황=전극 선단 / 빨강=Nip\n설비 A/B 설정은 각각 별도로 휴대폰에 저장됩니다.","Green=Gripper / Orange=electrode Tip / Red=Nip\nA/B machine settings are stored separately on the phone.","Zielony=Gripper / Pomarańczowy=końcówka elektrody / Czerwony=Nip\nUstawienia maszyny A/B są zapisywane osobno w telefonie.","Зелений=Gripper / Помаранчевий=кінчик електрода / Червоний=Nip\nНалаштування обладнання A/B зберігаються окремо на телефоні.");

        // Reusable analysis/status phrases.
        a("통합검사 모드","Inspection mode","Tryb kontroli","Режим перевірки");
        a("통합검사 시작","Inspection started","Kontrola rozpoczęta","Перевірку розпочато");
        a("통합검사 진행","Inspection in progress","Kontrola w toku","Перевірка триває");
        a("통합검사 완료","Inspection complete","Kontrola zakończona","Перевірку завершено");
        a("검사 완료","Inspection complete","Kontrola zakończona","Перевірку завершено");
        a("검사시간","Inspection time","Czas kontroli","Час перевірки");
        a("최우선","Top priority","Najwyższy priorytet","Найвищий пріоритет");
        a("우선 확인 TOP 4","TOP 4 to check first","TOP 4 do sprawdzenia","TOP 4 для першочергової перевірки");
        a("전체 검사항목 · 문제 우선도","All inspection items · issue priority","Wszystkie pozycje · priorytet problemu","Усі пункти · пріоритет проблеми");
        a("높을수록 먼저 확인  |  빨강 큰 차이 · 노랑 확인 · 초록 안정","Higher = check first  |  Red large difference · Yellow check · Green stable","Wyżej = sprawdź najpierw  |  Czerwony duża różnica · Żółty sprawdź · Zielony stabilnie","Вище = перевірити спочатку  |  Червоний велика різниця · Жовтий перевірити · Зелений стабільно");
        a("검사항목","Inspection item","Pozycja kontroli","Пункт перевірки");
        a("핵심 확인","Key check","Najważniejsze","Ключова перевірка");
        a("빠른검사 제외","Skipped in fast mode","Pominięto w szybkim trybie","Пропущено у швидкому режимі");
        a("문제 우선도","Issue priority","Priorytet problemu","Пріоритет проблеми");
        a("문제 Cycle TOP3","TOP3 problem Cycles","TOP3 problemowych cykli","TOP3 проблемних циклів");
        a("문제 Cycle","Problem Cycle","Problemowy Cycle","Проблемний Cycle");
        a("문제 동작구간","Problem motion phase","Problemowa faza ruchu","Проблемна фаза руху");
        a("문제 동작","Problem motion","Problemowy ruch","Проблемний рух");
        a("문제구간","Problem phase","Problemowa faza","Проблемна фаза");
        a("문제 집중 구간","Problem concentration phase","Faza koncentracji problemu","Фаза концентрації проблеми");
        a("동작별 차이","Difference by motion phase","Różnica wg fazy ruchu","Різниця за фазою руху");
        a("전체 편차","Overall deviation","Odchylenie całkowite","Загальне відхилення");
        a("구간 편차","Phase deviation","Odchylenie fazy","Відхилення фази");
        a("구간 퍼짐","Phase spread","Rozrzut fazy","Розкид фази");
        a("편차","Deviation","Odchylenie","Відхилення");
        a("차이 큼","Large difference","Duża różnica","Велика різниця");
        a("큰 차이","Large difference","Duża różnica","Велика різниця");
        a("확인 필요","Check needed","Wymaga sprawdzenia","Потрібна перевірка");
        a("확인","Check","Sprawdź","Перевірити");
        a("안정","Stable","Stabilnie","Стабільно");
        a("불안정","Unstable","Niestabilnie","Нестабільно");
        a("양호","Good","Dobrze","Добре");
        a("완료","Complete","Zakończono","Завершено");
        a("대기","Waiting","Oczekiwanie","Очікування");
        a("정상 후보","Normal candidate","Kandydat: normalny","Кандидат: норма");
        a("주의 후보","Watch candidate","Kandydat: uwaga","Кандидат: увага");
        a("이상 후보","Anomaly candidate","Kandydat: anomalia","Кандидат: аномалія");
        a("기준영상 신뢰도","Reference-video reliability","Wiarygodność filmu referencyjnego","Надійність еталонного відео");
        a("기준영상 점검 필요","Reference video needs checking","Film referencyjny wymaga sprawdzenia","Еталонне відео потребує перевірки");
        a("기준영상 점검","Reference-video check","Kontrola filmu referencyjnego","Перевірка еталонного відео");
        a("A 기준영상 점검도","A reference-video risk","Ryzyko filmu referencyjnego A","Ризик еталонного відео A");
        a("B Cycle 불안정도","B Cycle instability","Niestabilność Cycle B","Нестабільність Cycle B");
        a("A↔B 궤적 차이","A↔B trajectory difference","Różnica trajektorii A↔B","Різниця траєкторії A↔B");
        a("고속 Event 차이","High-speed Event difference","Różnica High-speed Event","Різниця High-speed Event");
        a("Timing / Jerk 변화","Timing / Jerk change","Zmiana Timing / Jerk","Зміна Timing / Jerk");
        a("공통진동 / 안정화","Common vibration / settling","Drgania wspólne / stabilizacja","Спільна вібрація / стабілізація");
        a("Smart / Golden 차이","Smart / Golden difference","Różnica Smart / Golden","Різниця Smart / Golden");
        a("ROI 위치 / 흔들림","ROI position / jitter","Pozycja ROI / drganie","Позиція ROI / тремтіння");
        a("빠른검사","Fast inspection","Szybka kontrola","Швидка перевірка");
        a("표준검사","Standard inspection","Kontrola standardowa","Стандартна перевірка");
        a("정밀검사","Detailed inspection","Kontrola dokładna","Детальна перевірка");
        a("Cycle 문제 우선 (추천)","Cycle issues first (recommended)","Najpierw problemy Cycle (zalecane)","Спочатку проблеми Cycle (рекомендовано)");
        a("전체 8개 분석","all 8 analyses","wszystkie 8 analiz","усі 8 аналізів");
        a("A 기준영상","A reference video","Film referencyjny A","Еталонне відео A");
        a("B 비교영상","B test video","Film testowy B","Тестове відео B");
        a("기준영상","reference video","film referencyjny","еталонне відео");
        a("비교영상","test video","film testowy","тестове відео");
        a("정상 권장","normal recommended","zalecany stan prawidłowy","рекомендовано нормальний стан");
        a("검사 대상","inspection target","obiekt kontroli","об'єкт перевірки");
        a("선택 안 됨","Not selected","Nie wybrano","Не вибрано");
        a("대표 프레임","Representative frame","Klatka reprezentatywna","Репрезентативний кадр");
        a("재생","Play","Odtwórz","Відтворити");
        a("비교재생","Compare playback","Porównanie odtwarzania","Порівняльне відтворення");
        a("분석 후 활성화","enabled after analysis","aktywne po analizie","активується після аналізу");
        a("분석 중","Analyzing","Analiza","Аналіз");
        a("분석 완료","Analysis complete","Analiza zakończona","Аналіз завершено");
        a("분석 실패","Analysis failed","Błąd analizy","Помилка аналізу");
        a("저장 완료","Saved","Zapisano","Збережено");
        a("저장 실패","Save failed","Błąd zapisu","Помилка збереження");
        a("먼저 영상을 선택해 주세요.","Select a video first.","Najpierw wybierz film.","Спочатку виберіть відео.");
        a("A/B 영상을 모두 선택해 주세요.","Select both A and B videos.","Wybierz oba filmy A i B.","Виберіть обидва відео A і B.");
        a("먼저 분석을 실행해 주세요.","Run the analysis first.","Najpierw uruchom analizę.","Спочатку запустіть аналіз.");
        a("먼저 Cycle Diagnosis 분석을 실행해 주세요.","Run Cycle Diagnosis first.","Najpierw uruchom diagnostykę Cycle.","Спочатку запустіть діагностику Cycle.");
        a("재생 가능한 Cycle 구간이 부족합니다.","Not enough playable Cycle range.","Za mało zakresu Cycle do odtworzenia.","Недостатньо діапазону Cycle для відтворення.");
        a("선택 모드로 자동 분석","automatic analysis in selected mode","automatyczna analiza w wybranym trybie","автоматичний аналіз у вибраному режимі");
        a("촬영각 자동 보정","Camera-angle auto correction","Automatyczna korekcja kąta kamery","Автокорекція кута камери");
        a("촬영각 보정","Camera-angle correction","Korekcja kąta kamery","Корекція кута камери");
        a("공통진동","common vibration","drgania wspólne","спільна вібрація");
        a("고정부 진동","fixed-unit vibration","drgania części stałej","вібрація нерухомої частини");
        a("Cutter 실제운동","actual Cutter motion","rzeczywisty ruch noża","фактичний рух різака");
        a("Cutter 상대운동","relative Cutter motion","względny ruch noża","відносний рух різака");
        a("재현성 Score","Repeatability Score","Wynik powtarzalności","Оцінка повторюваності");
        a("재현성","Repeatability","Powtarzalność","Повторюваність");
        a("반복성","Repeatability","Powtarzalność","Повторюваність");
        a("반복 재현성","Cycle repeatability","Powtarzalność cyklu","Повторюваність циклу");
        a("흔들림","jitter","drganie","тремтіння");
        a("진입각","entry angle","kąt wejścia","кут входу");
        a("안정 Score","Stability Score","Wynik stabilności","Оцінка стабільності");
        a("전극 선단","electrode Tip","końcówka elektrody","кінчик електрода");
        a("설비 A","Machine A","Maszyna A","Обладнання A");
        a("설비 B","Machine B","Maszyna B","Обладнання B");
        a("사용자 Cutter","Custom Cutter","Własny Cutter","Користувацький Cutter");
        a("대기/초기","Idle/initial","Postój/początek","Очікування/початок");
        a("대기/안정","Idle/stable","Postój/stabilnie","Очікування/стабільно");
        a("전진가속","Forward accel.","Przysp. do przodu","Прискорення вперед");
        a("커팅/충격","Cut/impact","Cięcie/udar","Різання/удар");
        a("복귀가속","Return accel.","Przysp. powrotu","Прискорення повернення");
        a("안정화","Settling","Stabilizacja","Стабілізація");
        a("고속 이동","High-speed move","Szybki ruch","Швидкий рух");
        a("감속/정지","Decel./stop","Zwalnianie/stop","Гальмування/стоп");
        a("가속","Acceleration","Przyspieszenie","Прискорення");
        a("복귀","Return","Powrót","Повернення");
        a("커팅","Cutting","Cięcie","Різання");
        a("좌우 동시 비교","side-by-side comparison","porównanie obok siebie","порівняння пліч-о-пліч");
        a("왼쪽 A 기준","A reference on left","referencja A po lewej","еталон A ліворуч");
        a("오른쪽 B 문제 Cycle","B problem Cycle on right","problemowy Cycle B po prawej","проблемний Cycle B праворуч");
        a("누르면 확대","tap to zoom","dotknij, aby powiększyć","натисніть для збільшення");
        a("상태","Status","Stan","Статус");
        a("정상","Normal","Normalnie","Норма");
        a("주의","Watch","Uwaga","Увага");
        a("이상","Anomaly","Anomalia","Аномалія");
        a("낮음","Low","Niska","Низька");
        a("미등록","Not registered","Nie zarejestrowano","Не зареєстровано");
        a("닫기","Close","Zamknij","Закрити");
        a("저장","Save","Zapisz","Зберегти");
        a("취소","Cancel","Anuluj","Скасувати");
        a("원위치","Reset","Reset","Скинути");
        a("위치 이동","Move position","Przesuń pozycję","Перемістити позицію");
        a("폭 +","Width +","Szerokość +","Ширина +");
        a("폭 −","Width −","Szerokość −","Ширина −");
        a("높이 +","Height +","Wysokość +","Висота +");
        a("높이 −","Height −","Wysokość −","Висота −");
        a("ROI 저장 완료","ROI saved","ROI zapisane","ROI збережено");
        a("ROI 초기화","ROI reset","Reset ROI","ROI скинуто");
        a("프레임 읽기 실패","Failed to read frame","Błąd odczytu klatki","Не вдалося прочитати кадр");
        a("확대 열기 실패","Failed to open zoom","Nie udało się otworzyć powiększenia","Не вдалося відкрити збільшення");
        a("숫자를 다시 입력해 주세요.","Enter the numbers again.","Wprowadź liczby ponownie.","Введіть числа ще раз.");
        a("저장 위치를 만들지 못했습니다.","Could not create save location.","Nie można utworzyć lokalizacji zapisu.","Не вдалося створити місце збереження.");
        a("Pictures/ElectrodeCuttingCompare 에 저장했습니다.","Saved to Pictures/ElectrodeCuttingCompare.","Zapisano w Pictures/ElectrodeCuttingCompare.","Збережено в Pictures/ElectrodeCuttingCompare.");
        a("Golden 기준 등록","Register Golden baseline","Zapisz bazę Golden","Зареєструвати базу Golden");
        a("Golden 등록","Register Golden","Zapisz Golden","Зареєструвати Golden");
        a("Golden 미등록","Golden not registered","Golden niezarejestrowany","Golden не зареєстровано");
        a("Golden 비교 완료","Golden comparison complete","Porównanie Golden zakończone","Порівняння Golden завершено");
        a("문제구간만 반복","Loop problem phase only","Powtarzaj tylko problemową fazę","Повторювати лише проблемну фазу");
        a("Cycle 진행률","Cycle progress","Postęp Cycle","Прогрес Cycle");
        a("Cycle 정보","Cycle information","Informacje o Cycle","Інформація про Cycle");
        a("A 기준 · 정상 Cycle","A reference · normal Cycle","A referencja · normalny Cycle","A еталон · нормальний Cycle");
        a("B 비교 · 문제 Cycle","B test · problem Cycle","B test · problemowy Cycle","B тест · проблемний Cycle");
        a("문제 동작:","Problem motion:","Problemowy ruch:","Проблемний рух:");
        a("검출 Cycle","Detected Cycles","Wykryte Cycles","Виявлені Cycles");
        a("평균 Cycle Time","Average Cycle Time","Średni Cycle Time","Середній Cycle Time");
        a("Cycle 부족","Too few Cycles","Za mało Cycles","Замало Cycles");
        a("불완전 Cycle","Incomplete Cycle","Niepełny Cycle","Неповний Cycle");
        a("개 자동 제외"," auto-excluded"," automatycznie pominięto"," автоматично виключено");
        a("개 제외"," excluded"," pominięto"," виключено");


        a("TOP1 · 문제 Cycle 비교","TOP1 · Problem Cycle comparison","TOP1 · Porównanie problemowego Cycle","TOP1 · Порівняння проблемного Cycle");
        a("빠른검사: 문제 Cycle TOP3와 문제 동작구간을 우선 분석합니다.","Fast inspection: analyzes TOP3 problem Cycles and problem motion phases first.","Szybka kontrola: najpierw analizuje TOP3 problemowych cykli i problemowe fazy ruchu.","Швидка перевірка: спочатку аналізує TOP3 проблемних циклів і проблемні фази руху.");
        a("5단계 Cutter 추정 상태\n① 대기  ② 전진가속  ③ 커팅/충격  ④ 복귀가속  ⑤ 안정화","Estimated 5-phase Cutter status\n① Idle  ② Forward accel.  ③ Cut/impact  ④ Return accel.  ⑤ Settling","Szacowany stan noża w 5 fazach\n① Postój  ② Przysp. do przodu  ③ Cięcie/udar  ④ Przysp. powrotu  ⑤ Stabilizacja","Орієнтовний стан різака у 5 фазах\n① Очікування  ② Прискорення вперед  ③ Різання/удар  ④ Прискорення повернення  ⑤ Стабілізація");
        a("A 기준 대비 B 주요 차이 TOP 3 · 검사 대기","TOP 3 key differences of B vs A · waiting for inspection","TOP 3 głównych różnic B względem A · oczekiwanie na kontrolę","TOP 3 ключових відмінностей B відносно A · очікування перевірки");
        a("Cycle Diagnosis · 검사 대기\n어느 Cycle이 문제인지 + 어떤 동작구간이 문제인지 TOP3로 표시합니다.","Cycle Diagnosis · waiting\nShows TOP3: which Cycle and which motion phase is the issue.","Diagnostyka Cycle · oczekiwanie\nPokazuje TOP3: który Cycle i która faza ruchu są problemem.","Діагностика Cycle · очікування\nПоказує TOP3: який Cycle і яка фаза руху є проблемою.");
        a("SYSTEM READY  •  종합 진단 대기\nA 기준영상과 B 비교영상을 선택하고 분석을 시작하세요.\n결과는 각 분석 카드에 독립적으로 유지됩니다.","SYSTEM READY  •  Waiting for integrated diagnosis\nSelect A reference and B test videos, then start analysis.\nResults remain independently in each analysis card.","SYSTEM READY  •  Oczekiwanie na diagnostykę zintegrowaną\nWybierz film referencyjny A i testowy B, a następnie uruchom analizę.\nWyniki pozostają osobno w każdej karcie analizy.","SYSTEM READY  •  Очікування інтегрованої діагностики\nВиберіть еталонне відео A і тестове B, потім запустіть аналіз.\nРезультати зберігаються окремо в кожній картці аналізу.");
        a("자동 보정 + A/B 비교 · 대기","Auto correction + A/B comparison · waiting","Autokorekcja + porównanie A/B · oczekiwanie","Автокорекція + порівняння A/B · очікування");
        a("Cycle 패턴 비교 · 대기","Cycle pattern comparison · waiting","Porównanie wzorca Cycle · oczekiwanie","Порівняння шаблону Cycle · очікування");
        a("고속 Event 동기화 · 대기","High-speed Event sync · waiting","Synchronizacja High-speed Event · oczekiwanie","Синхронізація High-speed Event · очікування");
        a("v0.5 고속 Event 동기화 / 이상 후보 시점","v0.5 High-speed Event sync / anomaly-candidate moment","v0.5 Synchronizacja High-speed Event / chwila kandydata anomalii","v0.5 Синхронізація High-speed Event / момент-кандидат аномалії");
        a("v0.6 고급 동작 분석 (누르면 확대)","v0.6 Advanced motion analysis (tap to zoom)","v0.6 Zaawansowana analiza ruchu (dotknij, aby powiększyć)","v0.6 Розширений аналіз руху (натисніть для збільшення)");
        a("Cutter 운동 / 고정부 진동 분리 · 대기","Cutter motion / fixed-unit vibration separation · waiting","Ruch noża / separacja drgań części stałej · oczekiwanie","Рух різака / відокремлення вібрації нерухомої частини · очікування");
        a("v0.8 한눈에 보기 (파랑=Cutter 실제운동 / 회색=공통진동 / 자홍=Top3)","v0.8 At a glance (blue=actual Cutter motion / gray=common vibration / magenta=Top3)","v0.8 Podgląd (niebieski=rzeczywisty ruch noża / szary=drgania wspólne / magenta=Top3)","v0.8 Огляд (синій=фактичний рух різака / сірий=спільна вібрація / пурпурний=Top3)");
        a("px → mm Calibration 설정","px → mm Calibration settings","Ustawienia kalibracji px → mm","Налаштування калібрування px → mm");
        a("v0.7 Smart Diagnostic (자홍선 = Top3 이상 후보 순간)","v0.7 Smart Diagnostic (magenta = Top3 anomaly-candidate moments)","v0.7 Smart Diagnostic (magenta = Top3 chwil kandydatów anomalii)","v0.7 Smart Diagnostic (пурпурний = Top3 моментів-кандидатів аномалії)");
        a("v0.4 ROI 위치/크기 설정 · 설비별 저장","v0.4 ROI position/size settings · saved by machine","v0.4 Ustawienia pozycji/rozmiaru ROI · zapis osobno dla maszyny","v0.4 Налаштування позиції/розміру ROI · збереження за обладнанням");
        a("ROI Gripper / Tip / Nip 분석 · 대기","ROI Gripper / Tip / Nip analysis · waiting","Analiza ROI Gripper / Tip / Nip · oczekiwanie","Аналіз ROI Gripper / Tip / Nip · очікування");
        a("영상 A/B를 선택해 주세요.\n분석 이미지/그래프는 터치하면 전체화면 확대됩니다.","Select A/B videos.\nTap analysis images/graphs to zoom full screen.","Wybierz filmy A/B.\nDotknij obrazu/wykresu analizy, aby powiększyć na pełny ekran.","Виберіть відео A/B.\nНатисніть зображення/графік аналізу для повноекранного збільшення.");
        a("A 기준 프레임","A reference frame","Klatka referencyjna A","Еталонний кадр A");
        a("B 보정 프레임","B corrected frame","Skorygowana klatka B","Скоригований кадр B");
        a("Difference Map (빨강 = 변화 큰 영역)","Difference Map (red = large-change area)","Mapa różnic (czerwony = obszar dużej zmiany)","Карта різниць (червоний = зона великої зміни)");
        a("Cycle Motion 비교 (A=파랑 / B=빨강)","Cycle Motion comparison (A=blue / B=red)","Porównanie Cycle Motion (A=niebieski / B=czerwony)","Порівняння Cycle Motion (A=синій / B=червоний)");
        a("v0.4 설비 A ROI 추적 (초록=Gripper / 주황=전극선단 / 빨강=Nip)","v0.4 Machine A ROI tracking (green=Gripper / orange=electrode Tip / red=Nip)","v0.4 Śledzenie ROI maszyny A (zielony=Gripper / pomarańczowy=końcówka elektrody / czerwony=Nip)","v0.4 Відстеження ROI обладнання A (зелений=Gripper / помаранчевий=кінчик електрода / червоний=Nip)");
        a("v0.4 설비 B ROI 추적 (카메라 보정 후)","v0.4 Machine B ROI tracking (after camera correction)","v0.4 Śledzenie ROI maszyny B (po korekcji kamery)","v0.4 Відстеження ROI обладнання B (після корекції камери)");
        a("v0.4 A/B 품질지표 비교","v0.4 A/B quality-metric comparison","v0.4 Porównanie wskaźników jakości A/B","v0.4 Порівняння показників якості A/B");
        a("※ v0.4는 전극 선단/Gripper/Nip을 경량 ROI 방식으로 추적해 진입각·Offset·흔들림·안정 Score를 표시합니다. 아직 실제 치수 Calibration과 Golden/NG 기준 학습 전이므로 품질 NG 확정 판정값이 아니라 추세/비교용 지표입니다.","※ v0.4 uses lightweight ROI tracking for electrode Tip/Gripper/Nip and shows entry angle, Offset, jitter and Stability Score. Until dimensional calibration and Golden/NG limits are validated, these are trend/comparison indicators, not confirmed NG judgments.","※ v0.4 używa lekkiego śledzenia ROI dla końcówki elektrody/Gripper/Nip i pokazuje kąt wejścia, Offset, drganie oraz wynik stabilności. Do czasu walidacji kalibracji wymiarowej i limitów Golden/NG są to wskaźniki trendu/porównania, a nie potwierdzony werdykt NG.","※ v0.4 використовує легке ROI-відстеження кінчика електрода/Gripper/Nip і показує кут входу, Offset, тремтіння та оцінку стабільності. До валідації калібрування розмірів і меж Golden/NG це показники тренду/порівняння, а не підтверджений висновок NG.");


        // v1.9.4 multilingual coverage for dynamic analysis/status text and graph captions.
        a("Language / 언어","Language","Język","Мова");
        a("표준검사 · Cycle + Event + 진동 + Top3","Standard inspection · Cycle + Event + vibration + Top3","Kontrola standardowa · Cycle + Event + drgania + Top3","Стандартна перевірка · Cycle + Event + вібрація + Top3");
        a("핵심 차이","Key difference","Kluczowa różnica","Ключова відмінність");
        a("최대 차이","Maximum difference","Maksymalna różnica","Максимальна відмінність");
        a("재현성 저하","Reduced repeatability","Spadek powtarzalności","Зниження повторюваності");
        a("A 기준 대비 B 비교","B comparison vs A reference","Porównanie B względem referencji A","Порівняння B відносно еталона A");
        a("A 기준 대비 B 주요 차이 TOP 3","TOP 3 key differences of B vs A","TOP 3 głównych różnic B względem A","TOP 3 ключових відмінностей B відносно A");
        a("5단계 Cutter 추정 상태","Estimated 5-phase Cutter status","Szacowany stan noża w 5 fazach","Орієнтовний стан різака у 5 фазах");
        a("통합검사 후 무엇이 다른지 1·2·3 순위로 표시합니다.","After inspection, differences are ranked 1·2·3.","Po kontroli różnice są pokazane w kolejności 1·2·3.","Після перевірки відмінності показуються за рейтингом 1·2·3.");
        a("초기 전진/가속 동작 패턴 차이","Initial forward/acceleration motion difference","Różnica ruchu początkowego/przyspieszenia","Відмінність початкового руху/прискорення");
        a("커팅/충격 구간 동작 차이","Cut/impact phase motion difference","Różnica ruchu w fazie cięcia/udaru","Відмінність руху у фазі різання/удару");
        a("복귀 동작 패턴 차이","Return-motion pattern difference","Różnica wzorca ruchu powrotnego","Відмінність шаблону руху повернення");
        a("정지·안정화 잔류 움직임 차이","Residual-motion difference after stop/settling","Różnica ruchu resztkowego po zatrzymaniu/stabilizacji","Відмінність залишкового руху після зупинки/стабілізації");
        a("주요 동작 차이","Key motion difference","Kluczowa różnica ruchu","Ключова відмінність руху");
        a("차이 강조 · 빨강 = 촬영각 보정 후 A/B 차이가 큰 위치","Difference highlight · red = large A/B difference after camera-angle correction","Wyróżnienie różnicy · czerwony = duża różnica A/B po korekcji kąta kamery","Підсвічення різниці · червоний = велика різниця A/B після корекції кута камери");
        a("빠른검사 프레임 · 좌 A / 우 B · 촬영각 정밀보정은 표준/정밀검사에서 적용","Fast-mode frames · A left / B right · precise camera correction is applied in standard/detailed mode","Klatki szybkiej kontroli · A po lewej / B po prawej · dokładna korekcja kamery w trybie standardowym/dokładnym","Кадри швидкої перевірки · A ліворуч / B праворуч · точна корекція камери у стандартному/детальному режимі");
        a("대표 프레임 추출 중...","Extracting representative frame...","Pobieranie reprezentatywnej klatki...","Виділення репрезентативного кадру...");
        a("프레임을 읽지 못했습니다.","Could not read frame.","Nie udało się odczytać klatki.","Не вдалося прочитати кадр.");
        a("촬영각도·위치 자동 보정 중... 저사양 기기에서는 수 초 걸릴 수 있습니다.","Auto-correcting camera angle/position... This may take a few seconds on slower devices.","Automatyczna korekcja kąta/pozycji kamery... Na wolniejszym urządzeniu może potrwać kilka sekund.","Автокорекція кута/позиції камери... На повільнішому пристрої це може тривати кілька секунд.");
        a("Difference Map 계산 중...","Calculating Difference Map...","Obliczanie mapy różnic...","Обчислення карти різниць...");
        a("검사 중 · 문제 Cycle → 문제 동작 → 편차 순으로 자동 정리합니다.","Inspecting · automatically organizes Problem Cycle → Problem motion → deviation.","Kontrola · automatycznie porządkuje Problemowy Cycle → problemowy ruch → odchylenie.","Перевірка · автоматично впорядковує Проблемний Cycle → проблемний рух → відхилення.");
        a("Cycle 결과 대기","Cycle result · waiting","Wynik Cycle · oczekiwanie","Результат Cycle · очікування");
        a("분석 결과 확인","Review analysis result","Sprawdź wynik analizy","Переглянути результат аналізу");
        a("그래프/표 중심 요약","graph/table summary","podsumowanie wykres/tabela","підсумок графік/таблиця");
        a("한눈에 보는 문제 지도","Problem map at a glance","Mapa problemu","Карта проблеми");
        a("1) 문제 동작 확인  →  2) 문제 Cycle 확인  →  3) 실제 A/B 재생","1) Check problem motion  →  2) Check problem Cycle  →  3) Play actual A/B","1) Sprawdź problemowy ruch  →  2) Sprawdź problemowy Cycle  →  3) Odtwórz A/B","1) Перевірте проблемний рух  →  2) Перевірте проблемний Cycle  →  3) Відтворіть A/B");
        a("아래 TOP1~3 버튼을 누르면 해당 Cycle을 A/B 좌우 동시 비교합니다.","Tap TOP1~3 below to compare that Cycle side-by-side A/B.","Dotknij TOP1~3 poniżej, aby porównać dany Cycle A/B obok siebie.","Натисніть TOP1~3 нижче, щоб порівняти цей Cycle A/B поруч.");
        a("고속 Event 동기화 분석 중... (동일 영상 재분석 시 Cache 재사용)","Analyzing high-speed Event sync... (cache reused for the same video)","Analiza synchronizacji High-speed Event... (dla tego samego filmu używany jest cache)","Аналіз синхронізації High-speed Event... (для того самого відео використовується кеш)");
        a("동일 영상 재분석 시 Cache를 재사용합니다.","Cache is reused when analyzing the same video again.","Cache jest używany ponownie przy ponownej analizie tego samego filmu.","Кеш повторно використовується під час повторного аналізу того самого відео.");
        a("이미지/그래프를 누르면 전체화면 확대가 됩니다.","Tap an image/graph to zoom full screen.","Dotknij obrazu/wykresu, aby powiększyć na pełny ekran.","Натисніть зображення/графік для повноекранного збільшення.");
        a("그래프를 누르면 전체화면 확대됩니다.","Tap the graph to zoom full screen.","Dotknij wykresu, aby powiększyć na pełny ekran.","Натисніть графік для повноекранного збільшення.");
        a("고정부 공통진동 RMS","Fixed-unit common-vibration RMS","RMS wspólnych drgań części stałej","RMS спільної вібрації нерухомої частини");
        a("보정 후 Cutter 운동 RMS","Corrected Cutter-motion RMS","RMS ruchu noża po korekcji","RMS руху різака після корекції");
        a("정지 안정화 약","Settling after stop approx.","Stabilizacja po zatrzymaniu ok.","Стабілізація після зупинки прибл.");
        a("Cycle 반복변동","Cycle repeat variation","Zmienność powtórzeń Cycle","Варіація повторів Cycle");
        a("고정부 흔들림 제거 후 Cutter 상대운동","Cutter relative motion after removing fixed-unit shake","Ruch względny noża po usunięciu drgań części stałej","Відносний рух різака після усунення тремтіння нерухомої частини");
        a("파랑 = 보정된 Cutter 운동   회색 = 고정부 공통진동   자홍 = Top3","Blue = corrected Cutter motion   Gray = common vibration   Magenta = Top3","Niebieski = skorygowany ruch noża   Szary = drgania wspólne   Magenta = Top3","Синій = скоригований рух різака   Сірий = спільна вібрація   Пурпурний = Top3");
        a("구간: 대기/안정 → 가속 → 고속이동 → 감속/정지 (영상 신호 기반 자동 분할)","Phases: idle/stable → acceleration → high-speed motion → decel./stop (auto-segmented from video signal)","Fazy: postój/stabilnie → przyspieszenie → szybki ruch → zwalnianie/zatrzymanie (automatyczny podział z sygnału wideo)","Фази: очікування/стабільно → прискорення → швидкий рух → уповільнення/зупинка (автосегментація за відеосигналом)");
        a("최근 Risk Trend (최대 30회)","Recent Risk Trend (up to 30)","Ostatni trend Risk (do 30)","Останній тренд Risk (до 30)");
        a("Jerk: 파랑=A / 빨강=B","Jerk: blue=A / red=B","Jerk: niebieski=A / czerwony=B","Jerk: синій=A / червоний=B");
        a("파랑=A / 빨강=B / 자홍=Top3 이상 후보 순간","Blue=A / red=B / magenta=Top3 anomaly-candidate moments","Niebieski=A / czerwony=B / magenta=TOP3 chwil kandydatów anomalii","Синій=A / червоний=B / пурпурний=TOP3 моментів-кандидатів аномалії");
        a("B Cycle 편차 Heatmap · 진할수록 평균 궤적에서 멀어짐","B Cycle deviation Heatmap · darker = farther from average trajectory","Heatmap odchylenia Cycle B · ciemniej = dalej od średniej trajektorii","Теплокарта відхилення Cycle B · темніше = далі від середньої траєкторії");
        a("A 평균 vs B 평균 · 노랑=최대 차이 구간","A average vs B average · yellow=maximum-difference phase","Średnia A vs średnia B · żółty=maksymalna różnica","Середнє A vs середнє B · жовтий=фаза максимальної різниці");
        a("● A 기준 평균","● A reference average","● Średnia referencyjna A","● Середнє еталона A");
        a("● B 비교 평균","● B test average","● Średnia testowa B","● Середнє тесту B");
        a("평균 속도 패턴 · A 파랑 / B 빨강","Average speed pattern · A blue / B red","Średni wzorzec prędkości · A niebieski / B czerwony","Середній шаблон швидкості · A синій / B червоний");
        a("전진/복귀 방향 변화와 급가속·급감속의 상대 패턴을 비교","Compare relative pattern of forward/return direction changes and abrupt accel./decel.","Porównanie względnego wzorca zmian kierunku przód/powrót i gwałtownego przysp./zwalniania","Порівняння відносного шаблону зміни напрямку вперед/назад та різкого прискорення/уповільнення");
        a("ROI 정밀분석(참고)","ROI detailed analysis (reference)","Dokładna analiza ROI (referencyjna)","Детальний аналіз ROI (довідково)");
        a("촬영각 보정 후 전극 선단 / Gripper / Nip 상대값 비교","Compare electrode Tip / Gripper / Nip after camera-angle correction","Porównanie końcówki elektrody / Gripper / Nip po korekcji kąta kamery","Порівняння кінчика електрода / Gripper / Nip після корекції кута камери");
        a("설비 A","Machine A","Maszyna A","Обладнання A");
        a("설비 B","Machine B","Maszyna B","Обладнання B");
        a("복원됨:","Restored:","Przywrócono:","Відновлено:");
        a("1위","1st","1. miejsce","1 місце");
        a("2위","2nd","2. miejsce","2 місце");
        a("3위","3rd","3. miejsce","3 місце");
        a("차이","Difference","Różnica","Відмінність");
        a("파랑","blue","niebieski","синій");
        a("빨강","red","czerwony","червоний");
        a("노랑","yellow","żółty","жовтий");
        a("회색","gray","szary","сірий");
        a("자홍","magenta","magenta","пурпурний");
        a("현재","Current","Bieżący","Поточний");
        a("완료","Complete","Zakończono","Завершено");
        a("대기","Waiting","Oczekiwanie","Очікування");

        Collections.sort(ENTRIES,new Comparator<Entry>(){public int compare(Entry x,Entry y){return Integer.compare(y.ko.length(),x.ko.length());}});
    }

    public static String guideHtml(){
        if(EN.equals(current))return "<h2>Analysis Guide · Why / What / How</h2>"+
                "<h3>1. Camera-angle correction</h3><b>Why?</b> To avoid mistaking camera-position differences for machine differences.<br><b>How?</b> Fixed structures are matched to correct rotation, scale and X/Y position.<br><br>"+
                "<h3>2. Cycle analysis</h3><b>What?</b> Compares the repeated motion pattern of the Cutter Module.<br><b>Why?</b> To find changes in forward/cut/return timing and repeatability.<br><br>"+
                "<h3>3. Cycle repeatability / problem-Cycle diagnosis</h3>The left→right→left motion is split into individual Cycles and normalized to 0–100%. TOP1–3 Cycles that deviate most from the mean trajectory are shown together with the motion phase having the largest deviation.<br><b>Repeatability Score</b>: closer to 100 means more stable repetition.<br><b>Time CV</b>: lower means more consistent Cycle time.<br><b>A/B replay</b>: replays a representative normal A Cycle and the worst B Cycle side-by-side.<br><br>"+
                "<h3>Inspection modes</h3><b>Fast</b>: prioritizes TOP3 problem Cycles and motion phases.<br><b>Standard</b>: adds Event, common vibration and Top3/Golden.<br><b>Detailed</b>: runs full analysis including ROI/Jerk.<br><br>"+
                "<h3>4. High-Speed Event</h3>Finds short, fast impact/event moments. A 30 fps video can miss events shorter than about 33 ms.<br><br>"+
                "<h3>5. Multi-Cycle / Jerk</h3><b>Jerk</b> is a sudden change in acceleration. <b>Timing CV</b> is Cycle-time variation.<br><br>"+
                "<h3>6. Common-vibration correction</h3>The Cutter Module is the intended moving object. Motion shared by fixed units is treated as common vibration/camera shake and removed from Cutter motion.<br><br>"+
                "<h3>7. ROI analysis</h3>Tracks Gripper / electrode Tip / Nip position, angle and jitter. Nip Offset is the displacement from the reference Nip center.<br><br>"+
                "<h3>8. Golden comparison</h3>Golden is a verified normal baseline used to measure deviation of the current video.<br><br>"+
                "<h3>9. Top3 Anomaly</h3>Shows the three moments that differ most from the normal pattern. They are moments to check first, not confirmed root causes.<br><br>"+
                "<b>Important:</b> Scores are video-based relative indicators. Actual NG limits require separate validation using sufficient normal/defect data and process standards.";
        if(PL.equals(current))return "<h2>Przewodnik analizy · Dlaczego / Co / Jak</h2>"+
                "<h3>1. Korekcja kąta kamery</h3><b>Dlaczego?</b> Aby nie uznać różnicy ustawienia kamery za różnicę maszyny.<br><b>Jak?</b> Dopasowywane są stałe elementy, a następnie korygowane obrót, skala i pozycja X/Y.<br><br>"+
                "<h3>2. Analiza Cycle</h3>Porównuje powtarzalny ruch modułu tnącego, aby wykryć zmiany czasu ruchu do przodu, cięcia, powrotu i powtarzalności.<br><br>"+
                "<h3>3. Powtarzalność Cycle / diagnostyka problemowego Cycle</h3>Ruch lewo→prawo→lewo jest dzielony na osobne Cycles i normalizowany do 0–100%. TOP1–3 pokazuje cykle najbardziej oddalone od średniej trajektorii oraz fazę ruchu o największej różnicy.<br><b>Wynik powtarzalności</b>: bliżej 100 = stabilniej.<br><b>Time CV</b>: mniej = bardziej stały czas.<br><b>Odtwarzanie A/B</b>: normalny Cycle A i najgorszy Cycle B obok siebie.<br><br>"+
                "<h3>Tryby kontroli</h3><b>Szybki</b>: TOP3 cykli i faz ruchu.<br><b>Standardowy</b>: dodaje Event, drgania wspólne i Top3/Golden.<br><b>Dokładny</b>: pełna analiza z ROI/Jerk.<br><br>"+
                "<h3>4. High-Speed Event</h3>Wyszukuje krótkie i szybkie uderzenia/zdarzenia. Film 30 fps może pominąć zdarzenia krótsze niż ok. 33 ms.<br><br>"+
                "<h3>5. Multi-Cycle / Jerk</h3><b>Jerk</b> = nagła zmiana przyspieszenia. <b>Timing CV</b> = zmienność czasu Cycle.<br><br>"+
                "<h3>6. Korekcja drgań wspólnych</h3>Docelowo porusza się moduł tnący. Wspólny ruch elementów stałych jest traktowany jako drganie/kamera i odejmowany od ruchu noża.<br><br>"+
                "<h3>7. Analiza ROI</h3>Śledzi pozycję, kąt i drganie Gripper / końcówki elektrody / Nip.<br><br>"+
                "<h3>8. Golden</h3>Zweryfikowany normalny stan odniesienia do porównania bieżącego filmu.<br><br>"+
                "<h3>9. Top3 Anomaly</h3>Pokazuje trzy chwile najbardziej różniące się od wzorca. To punkty do sprawdzenia, a nie potwierdzona przyczyna.<br><br>"+
                "<b>Ważne:</b> Wyniki są względnymi wskaźnikami z obrazu. Rzeczywiste limity NG wymagają osobnej walidacji danych prawidłowych/wadliwych i standardów procesu.";
        if(UK.equals(current))return "<h2>Посібник з аналізу · Чому / Що / Як</h2>"+
                "<h3>1. Корекція кута камери</h3><b>Чому?</b> Щоб не сприймати різницю положення камери як різницю обладнання.<br><b>Як?</b> Нерухомі елементи зіставляються, після чого коригуються обертання, масштаб і позиція X/Y.<br><br>"+
                "<h3>2. Аналіз Cycle</h3>Порівнює повторюваний рух різального модуля, щоб знаходити зміни часу руху вперед, різання, повернення та повторюваності.<br><br>"+
                "<h3>3. Повторюваність Cycle / діагностика проблемного Cycle</h3>Рух ліворуч→праворуч→ліворуч розділяється на окремі Cycles і нормалізується до 0–100%. TOP1–3 показує цикли з найбільшим відхиленням від середньої траєкторії та фазу руху з найбільшою різницею.<br><b>Оцінка повторюваності</b>: ближче до 100 = стабільніше.<br><b>Time CV</b>: менше = стабільніший час.<br><b>Відтворення A/B</b>: нормальний Cycle A та найгірший Cycle B пліч-о-пліч.<br><br>"+
                "<h3>Режими перевірки</h3><b>Швидкий</b>: TOP3 циклів і проблемні фази руху.<br><b>Стандартний</b>: додає Event, спільну вібрацію та Top3/Golden.<br><b>Детальний</b>: повний аналіз з ROI/Jerk.<br><br>"+
                "<h3>4. High-Speed Event</h3>Шукає короткі швидкі удари/події. Відео 30 fps може пропустити події коротші приблизно за 33 мс.<br><br>"+
                "<h3>5. Multi-Cycle / Jerk</h3><b>Jerk</b> = різка зміна прискорення. <b>Timing CV</b> = варіація часу Cycle.<br><br>"+
                "<h3>6. Корекція спільної вібрації</h3>Цільовий рухомий об'єкт — різальний модуль. Спільний рух нерухомих вузлів розглядається як вібрація/тремтіння камери та віднімається від руху різака.<br><br>"+
                "<h3>7. Аналіз ROI</h3>Відстежує позицію, кут і тремтіння Gripper / кінчика електрода / Nip.<br><br>"+
                "<h3>8. Golden</h3>Перевірений нормальний еталон для порівняння поточного відео.<br><br>"+
                "<h3>9. Top3 Anomaly</h3>Показує три моменти, що найбільше відрізняються від нормального шаблону. Це моменти для першої перевірки, а не підтверджена причина.<br><br>"+
                "<b>Важливо:</b> Оцінки є відносними показниками на основі відео. Реальні межі NG потребують окремої валідації достатніх нормальних/дефектних даних і стандартів процесу.";
        return "<h2>분석 가이드 · 왜 / 무엇을 / 어떻게</h2>"+
                "<h3>1. 촬영각 자동 보정</h3><b>왜?</b> A/B 카메라 위치 차이를 설비 차이로 오판하지 않기 위해서입니다.<br><b>어떻게?</b> 고정 구조의 특징을 맞춰 회전·배율·X/Y 위치를 보정합니다.<br><br>"+
                "<h3>2. Cycle 분석</h3><b>무엇?</b> Cutter Module의 반복 운동 패턴을 비교합니다.<br><b>왜?</b> 전진/커팅/복귀 타이밍과 반복성 변화를 찾기 위해서입니다.<br><br>"+
                "<h3>3. Cycle 반복 재현성 / 문제 Cycle 진단</h3>영상 속 좌→우→좌 왕복을 각각 하나의 Cycle로 자동 분리해 Cycle 1~10을 겹쳐 봅니다. 같은 동작을 반복할 때 몇 번째 Cycle과 어떤 동작구간이 문제인지 찾습니다.<br><b>재현성 Score</b> = 100에 가까울수록 반복 동작이 안정적입니다.<br><b>Time CV</b> = Cycle 시간의 변동률. 작을수록 일정합니다.<br><br>"+
                "<h3>검사 모드</h3><b>빠른검사</b>: 문제 Cycle TOP3와 문제 동작을 우선 계산합니다.<br><b>표준검사</b>: Cycle + Event + 공통진동 + Top3/Golden.<br><b>정밀검사</b>: ROI/Jerk 포함 전체 분석.<br><br>"+
                "<h3>4. High-Speed Event</h3>짧고 빠른 충격/이벤트를 찾습니다. 30fps 영상은 약 33ms보다 짧은 현상을 놓칠 수 있습니다.<br><br>"+
                "<h3>5. Multi-Cycle / Jerk</h3>Jerk는 가속도가 갑자기 변하는 정도, Timing CV는 Cycle 시간의 반복 편차입니다.<br><br>"+
                "<h3>6. Common Vibration 보정</h3>고정 유닛이 같이 움직이면 공통진동/카메라 흔들림으로 추정하고 Cutter 이동에서 제거합니다.<br><br>"+
                "<h3>7. ROI 품질 분석</h3>Gripper / 전극 선단(Tip) / Nip의 위치·각도·흔들림을 추적합니다.<br><br>"+
                "<h3>8. Golden 비교</h3>정상으로 확인된 기준 상태와 현재 영상을 비교합니다.<br><br>"+
                "<h3>9. Top3 Anomaly</h3>정상 패턴과 차이가 큰 순간 3개를 먼저 보여줍니다.<br><br>"+
                "<b>중요:</b> 현재 Score는 영상 기반 상대 비교 지표입니다. 실제 NG 한계는 충분한 정상/불량 데이터와 공정 기준으로 별도 검증해야 합니다.";
    }
}
