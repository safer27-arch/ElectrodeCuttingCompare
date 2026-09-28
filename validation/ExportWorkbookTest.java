import com.example.electrodecutcompare.ReviewXlsx;
import java.io.File;import java.util.*;
public final class ExportWorkbookTest {
 public static void main(String[] args)throws Exception {
  ReviewXlsx.Sheet s=new ReviewXlsx.Sheet("Summary").row("Item / 항목","Value / 값").row("Dataset","SYNTHETIC EXPORT TEST - NOT MACHINE DATA").row("한국어 / Polski / Українська","정지 / powrót / зворотний рух").row("PTS precision (s)",1.234567).row("Literal, never a formula","=1+1").row("Escaping","A&B <ROI> \"test\"");
  ReviewXlsx.Sheet c=new ReviewXlsx.Sheet("B_Cycles").row("Cycle","Duration (s)","Shape RMS (%stroke)","Time flag").row(1,1.000001,0.15,"").row(2,1.800023,0.26,"CHECK");
  ReviewXlsx.write(new File(args[0]),Arrays.asList(s,c));System.out.println("PASS exporter completed: "+args[0]);
 }
}
