package com.example.electrodecutcompare;

/** Four-language field labels. Raw spreadsheet column identifiers remain English. */
public final class ReviewText {
 private ReviewText(){}
 public static String of(String ko,String en,String pl,String uk){String c=LanguageManager.code();return "en".equals(c)?en:"pl".equals(c)?pl:"uk".equals(c)?uk:ko;}
 public static String title(){return of("커터 측정형 비교","Cutter measured comparison","Pomiarowe porównanie noża","Вимірювальне порівняння різака");}
 public static String phase(int p){String[][] names={{"출발 끝점","Start endpoint","Punkt początkowy","Початкова точка"},{"전진","Forward travel","Ruch do przodu","Рух уперед"},{"전진 끝점 / 방향전환","Far endpoint / reversal","Koniec ruchu / zmiana kierunku","Кінцева точка / зміна напрямку"},{"복귀","Return travel","Ruch powrotny","Зворотний рух"},{"복귀 끝점 / 정지","Return endpoint / stop","Punkt powrotu / zatrzymanie","Точка повернення / зупинка"}};String[] s=names[Math.max(0,Math.min(4,p))];return of(s[0],s[1],s[2],s[3]);}
 public static String reason(String code){
  switch(code){
   case "MEASURABLE_NOT_NG":return of("측정 조건 통과 · 정상/불량 판정 아님","Measurement checks passed · not OK/NG","Warunki pomiaru spełnione · nie ocena OK/NG","Умови вимірювання виконано · не оцінка OK/NG");
   case "TRACK_LOST":return of("추적 손실 · 판정 보류","Tracking loss · assessment withheld","Utrata śledzenia · ocena wstrzymana","Втрата відстеження · оцінку відкладено");
   case "FEW_FRAMES":return of("읽은 프레임 부족","Too few decoded frames","Za mało zdekodowanych klatek","Замало декодованих кадрів");
   case "FEW_COMPLETE_CYCLES":return of("완전한 왕복 3회 미만 · 판정 보류","Fewer than 3 complete cycles · withheld","Mniej niż 3 pełne cykle · ocena wstrzymana","Менше 3 повних циклів · оцінку відкладено");
   case "UNDERSAMPLED":return of("사이클당 12프레임 미만 · 정밀모드/고속촬영 필요","Under 12 frames/cycle · detailed mode/high-FPS recording needed","Poniżej 12 klatek/cykl · tryb dokładny/nagranie wysokich FPS","Менше 12 кадрів/цикл · детальний режим/висока частота кадрів");
   case "NO_RESOLVED_MOTION":case "SMALL_MOTION":return of("이동량이 작아 분해능 부족","Motion too small to resolve","Ruch za mały dla rozdzielczości","Рух замалий для цієї роздільності");
   default:return code;
  }
 }
 public static String caveat(){return of("영상 기반 상대 비교입니다. 큰 차이=불량이 아닙니다. 커팅/접촉 시점, 힘, 실제 mm는 별도 확인이 필요합니다.","Relative video comparison. Large differences are not defects. Cutting/contact, force and physical mm need separate verification.","Względne porównanie wideo. Duża różnica nie oznacza wady. Cięcie/styk, siłę i rzeczywiste mm należy zweryfikować osobno.","Відносне порівняння відео. Велика різниця не означає дефект. Різання/контакт, силу та фізичні мм слід перевіряти окремо.");}
 public static String basis(boolean a){return a?of("A 중앙 궤적 기준 (정상 미보증)","A median trajectory (not certified normal)","Mediana trajektorii A (bez gwarancji normy)","Медіанна траєкторія A (норма не гарантована)"):of("A 기준 불확실 → B 내부 반복 비교","A reference uncertain → B internal comparison","Niepewna referencja A → porównanie wewnętrzne B","Непевний еталон A → внутрішнє порівняння B");}
}
