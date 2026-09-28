import com.example.electrodecutcompare.MotionCore;
import java.util.*;
public class MotionCoreTest {
 static void check(boolean ok,String s){if(!ok)throw new AssertionError(s);System.out.println("PASS "+s);}
 static List<MotionCore.Sample> trace(int n,double period,double amplitude,double drift){List<MotionCore.Sample> s=new ArrayList<>();for(int i=0;i<n;i++){double t=i/60.0,x=40+amplitude*(1-Math.cos(2*Math.PI*t/period))/2+drift*t;s.add(new MotionCore.Sample(t,x,30,true));}return s;}
 public static void main(String[] args){
  MotionCore.Analysis a=MotionCore.analyze(trace(600,1,80,0),false);check(a.usable,"periodic usable");check(a.cycles.size()>=8,"complete cycles");check(Math.abs(a.medianTime-1)<.03,"PTS duration");check(a.spreadPct<.01,"identical repeat dispersion");
  MotionCore.Analysis flat=MotionCore.analyze(trace(400,1,0,0),false);check(!flat.usable&&flat.cycles.isEmpty(),"static rejected no invented cycles");
  List<MotionCore.Sample> lost=trace(600,1,80,0);for(int i=100;i<230;i++)lost.get(i).valid=false;MotionCore.Analysis b=MotionCore.analyze(lost,false);check(!b.usable,"tracking gap held");
  MotionCore.Analysis slow=MotionCore.analyze(trace(1000,1.5,80,0),false);MotionCore.Comparison cmp=MotionCore.compare(a,slow);check(Math.abs(cmp.cycleTimeDeltaPct-50)<3,"duration differences not DTW-hidden");check(cmp.rawShapePct<2,"normalized same shape");
  List<MotionCore.Sample> change=trace(600,1,80,0);for(int i=360;i<420;i++)change.get(i).x=40+1.2*(change.get(i).x-40);MotionCore.Analysis amp=MotionCore.analyze(change,false);check(amp.cycles.stream().mapToDouble(c->c.amplitude).max().orElse(0)-amp.cycles.stream().mapToDouble(c->c.amplitude).min().orElse(0)>.15,"stroke variation retained");
  MotionCore.Analysis drift=MotionCore.analyze(trace(600,1,80,.5),false);check(drift.cycles.get(drift.cycles.size()-1).startOffset-drift.cycles.get(0).startOffset>.03,"endpoint drift retained");
  List<MotionCore.Sample> low=trace(500,1,80,0);for(int i=0;i<low.size();i++)low.get(i).sec=i/8.0; // still 60 points/cycle: change real wave below
  List<MotionCore.Sample> alias=new ArrayList<>();for(int i=0;i<80;i++)alias.add(new MotionCore.Sample(i/30.0,40+40*(1-Math.cos(2*Math.PI*i/8)),30,true));MotionCore.Analysis al=MotionCore.analyze(alias,false);check(!al.usable,"too few samples per cycle held");
  int w=120,h=90;byte[] p=new byte[w*h];new Random(19).nextBytes(p);byte[] q=new byte[w*h];for(int y=0;y<h;y++)for(int x=0;x<w;x++){int sx=x-8,sy=y+3;if(sx>=0&&sx<w&&sy>=0&&sy<h)q[y*w+x]=(byte)Math.min(255,20+(p[sy*w+sx]&255)*.75);}
  MotionCore.Gray g=new MotionCore.Gray(w,h,p),g2=new MotionCore.Gray(w,h,q);MotionCore.Match m=MotionCore.locate(g2,new MotionCore.Patch(g,50,40,9,9),50,40,20);check(m.ok&&m.x==58&&m.y==37,"NCC position under gain offset");
  List<MotionCore.Sample> variable=new ArrayList<>();double clock=0;for(int cyc=0;cyc<8;cyc++){double duration=cyc==4?1.8:1.0;int frames=(int)Math.round(duration*60);for(int j=0;j<frames;j++)variable.add(new MotionCore.Sample(clock+j/60.0,40+40*(1-Math.cos(2*Math.PI*j/frames)),30,true));clock+=duration;}
  MotionCore.Analysis v=MotionCore.analyze(variable,false);check(v.cycles.stream().anyMatch(c->c.seconds>1.7&&c.timeOutlier),"long cycle retained and flagged");
  List<MotionCore.Sample> uneven=new ArrayList<>();double time=0;for(int i=0;i<600;i++){time+=(i%2==0?.012:.021);uneven.add(new MotionCore.Sample(time,40+40*(1-Math.cos(2*Math.PI*time)),30,true));}MotionCore.Analysis u=MotionCore.analyze(uneven,false);check(u.usable&&Math.abs(u.medianTime-1)<.04,"irregular timestamps preserve duration");
  byte[] tiles=new byte[w*h];byte[] tile=new byte[25];new Random(24).nextBytes(tile);for(int y=0;y<h;y++)for(int x=0;x<w;x++)tiles[y*w+x]=tile[(y%5)*5+x%5];MotionCore.Gray repeat=new MotionCore.Gray(w,h,tiles);MotionCore.Match ambiguous=MotionCore.locate(repeat,new MotionCore.Patch(repeat,50,40,9,9),50,40,20);check(!ambiguous.ok,"ambiguous repeated texture withheld");
  System.out.println("ALL PASSED");
 }
}
