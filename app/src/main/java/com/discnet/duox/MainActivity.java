package com.discnet.duox;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.view.MotionEvent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Color;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.content.Context;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Random;

public class MainActivity extends Activity {
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().getDecorView().setSystemUiVisibility(5894 | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
        setContentView(new Arena(this));
    }

    static class Arena extends View {
        static final float W = 1280f, H = 720f, FLOOR = 596f, R = 27f;
        static final float GRAVITY = 1480f, SPEED = 305f, JUMP = -655f;
        final Paint paint = new Paint(3);
        final Random random = new Random();
        final HashMap<Integer, Integer> pointers = new HashMap<>();
        final boolean[] held = new boolean[12];
        final ArrayList<Shot> shots = new ArrayList<>();
        final ArrayList<Particle> particles = new ArrayList<>();
        final RectF[] platforms = {
            new RectF(0, 596, 1280, 720),
            new RectF(245, 462, 470, 480),
            new RectF(810, 462, 1035, 480),
            new RectF(516, 350, 764, 368)
        };
        final Slime[] players = {new Slime(275, 0xff56dafa, 1), new Slime(1005, 0xffff69af, -1)};
        long previous = 0;
        int round = 1, winner = -1;
        float roundPause = 0, cameraShake = 0;
        Arena(Context ctx) { super(ctx); setLayerType(View.LAYER_TYPE_SOFTWARE, null); resetPositions(); }
        void resetPositions() {
            for (int i=0; i<2; i++) {
                Slime p=players[i]; p.x=i==0?180:1100; p.y=FLOOR-R; p.vx=0; p.vy=0; p.alive=true;
                p.dir=i==0?1:-1; p.jumpLatch=false; p.dashLatch=false; p.shieldLatch=false; p.shield=0; p.shieldCooldown=0; p.dash=0; p.dashActive=0; p.fire=0; p.jumps=0; p.grounded=true;
            }
            shots.clear(); particles.clear(); pointers.clear();
            for(int i=0;i<held.length;i++) held[i]=false;
        }
        void restart() { players[0].score=0; players[1].score=0; winner=-1; round=1; roundPause=0; resetPositions(); }
        @Override protected void onDraw(Canvas raw) {
            super.onDraw(raw);
            long now=System.nanoTime();
            float dt=previous==0 ? .016f : Math.min(.033f,(now-previous)/1_000_000_000f);
            previous=now;
            if(winner<0 && roundPause<=0) update(dt);
            else if(roundPause>0) {
                roundPause-=dt;
                if(roundPause<=0 && winner<0){round++;resetPositions();}
            }
            for(Particle part:particles){part.life-=dt;part.x+=part.vx*dt;part.y+=part.vy*dt;part.vy+=390*dt;}
            for (Iterator<Particle> pit=particles.iterator();pit.hasNext();) { if(pit.next().life<=0) pit.remove(); }
            cameraShake=Math.max(0,cameraShake-dt*30);
            float s=Math.min(getWidth()/W,getHeight()/H);
            float x=(getWidth()-W*s)/2f, y=(getHeight()-H*s)/2f;
            raw.drawColor(0xff101522);
            raw.save();raw.translate(x,y);raw.scale(s,s);
            drawBackground(raw);
            raw.save();
            if(cameraShake>0) raw.translate((random.nextFloat()-.5f)*cameraShake,(random.nextFloat()-.5f)*cameraShake);
            drawArena(raw);
            for(Shot shot:shots) drawShot(raw,shot);
            for(Particle part:particles) drawParticle(raw,part);
            for(Slime p:players) drawSlime(raw,p);
            raw.restore();
            drawHud(raw);
            if(winner>=0) drawOverlay(raw, "PLAYER "+(winner+1)+" VENCEU!", "TOQUE PARA REINICIAR");
            else if(roundPause>0) drawOverlay(raw,"PONTO DO PLAYER "+(players[0].alive?1:2),"PRÓXIMA RODADA");
            drawControls(raw);
            raw.restore();
            postInvalidateDelayed(16);
        }
        void update(float dt) {
            for(int i=0;i<2;i++) {
                Slime p=players[i]; if(!p.alive)continue;
                p.fire=Math.max(0,p.fire-dt);p.dash=Math.max(0,p.dash-dt);p.shield=Math.max(0,p.shield-dt);p.shieldCooldown=Math.max(0,p.shieldCooldown-dt);p.dashActive=Math.max(0,p.dashActive-dt);
                int base=i*6;
                int move=(held[base+1]?1:0)-(held[base]?1:0);
                if(move!=0) p.dir=move;
                float target=move*SPEED;
                p.vx+=(target-p.vx)*Math.min(1,dt*(p.grounded?13:7));
                if(held[base+2] && !p.jumpLatch && p.jumps<2){p.vy=p.jumps==0?JUMP:JUMP*.86f;p.jumps++;p.grounded=false;burst(p.x,p.y+R,0xffffffff,10);}
                p.jumpLatch=held[base+2];
                if(held[base+3] && p.fire==0){p.fire=.38f;shots.add(new Shot(p.x+p.dir*35,p.y-6,p.dir*670,i));burst(p.x+p.dir*35,p.y-6,p.color,5);}
                if(held[base+4] && !p.dashLatch && p.dash<=0){p.dash=2.6f;p.dashActive=.23f;p.vx=p.dir*850;p.vy=Math.min(p.vy,-120);burst(p.x,p.y,p.color,13);}
                p.dashLatch=held[base+4];
                if(held[base+5] && !p.shieldLatch && p.shieldCooldown<=0){p.shield=1.05f;p.shieldCooldown=5.0f;burst(p.x,p.y,0xffb8efff,18);}
                p.shieldLatch=held[base+5];
                p.vy+=GRAVITY*dt;
                float oldBottom=p.y+R;
                p.x+=p.vx*dt;p.y+=p.vy*dt;
                p.grounded=false;
                for(RectF f:platforms) {
                    if(p.vy>=0 && oldBottom<=f.top+9 && p.y+R>=f.top && p.x+R>f.left && p.x-R<f.right){
                        p.y=f.top-R;p.vy=0;p.grounded=true;p.jumps=0;
                    }
                }
                if(p.x<R){p.x=R;p.vx=Math.max(0,p.vx);}
                if(p.x>W-R){p.x=W-R;p.vx=Math.min(0,p.vx);}
                if(p.y>H+70) kill(i);
            }
            Iterator<Shot> it=shots.iterator();
            while(it.hasNext()){
                Shot sh=it.next();sh.life-=dt;sh.x+=sh.vx*dt;
                if(sh.life<=0 || sh.x<0 || sh.x>W){it.remove();continue;}
                Slime target=players[1-sh.owner];
                boolean hit=target.alive && dist(sh.x,sh.y,target.x,target.y)<R+sh.radius;
                if(hit){it.remove();burst(sh.x,sh.y,players[sh.owner].color,18);cameraShake=13;
                    if(target.shield<=0)kill(1-sh.owner);else target.vx+=Math.signum(sh.vx)*120;
                    continue;
                }
                boolean wall=false;
                for(int j=1;j<platforms.length;j++)if(platforms[j].contains(sh.x,sh.y))wall=true;
                if(wall){it.remove();burst(sh.x,sh.y,players[sh.owner].color,8);}
            }
            if(players[0].alive&&players[1].alive && dist(players[0].x,players[0].y,players[1].x,players[1].y)<2*R){
                Slime a=players[0],b=players[1];
                float d=Math.signum(b.x-a.x);if(d==0)d=1;
                a.x-=d*2;b.x+=d*2;
                if(a.dashActive>0 && b.shield<=0 && b.dashActive<=0)kill(1);
                else if(b.dashActive>0 && a.shield<=0 && a.dashActive<=0)kill(0);
                else {a.vx-=d*85;b.vx+=d*85;}
            }
        }
        void kill(int loser){
            if(!players[loser].alive)return;
            players[loser].alive=false;Slime p=players[loser];burst(p.x,p.y,p.color,38);
            cameraShake=24;int winning=1-loser;players[winning].score++;
            roundPause=1.55f;if(players[winning].score>=5){winner=winning;roundPause=0;}
        }
        void burst(float x,float y,int color,int amount){
            for(int i=0;i<amount;i++){float a=random.nextFloat()*6.283f;float v=85+random.nextFloat()*380;
                particles.add(new Particle(x,y,(float)Math.cos(a)*v,(float)Math.sin(a)*v,color,.25f+random.nextFloat()*.6f));}
        }
        static float dist(float x,float y,float ax,float ay){return (float)Math.hypot(x-ax,y-ay);}
        void fill(Canvas c,int color){paint.setColor(color);paint.setStyle(Paint.Style.FILL);paint.setStrokeWidth(1);}
        void rect(Canvas c,float l,float t,float r,float b,float rad,int color){fill(c,color);c.drawRoundRect(l,t,r,b,rad,rad,paint);}
        void circle(Canvas c,float x,float y,float radius,int color){fill(c,color);c.drawCircle(x,y,radius,paint);}
        void text(Canvas c,String str,float x,float y,float size,int color){
            fill(c,color);paint.setTypeface(Typeface.create("sans-serif-black",Typeface.BOLD));
            paint.setTextSize(size);paint.setTextAlign(Paint.Align.CENTER);c.drawText(str,x,y,paint);
        }
        void drawBackground(Canvas c){
            c.drawColor(0xff151b30);
            for(int k=0;k<21;k++){float gx=k*70;fill(c,0x0dffffff);c.drawLine(gx,0,gx,H,paint);}
            for(int k=0;k<12;k++){float gy=k*70;fill(c,0x0dffffff);c.drawLine(0,gy,W,gy,paint);}
            circle(c,640,120,200,0x0a64dff8);
            circle(c,135,180,160,0x0856dafa);
            circle(c,1160,250,180,0x08ff69af);
            text(c,"DUOX",640,91,52,0x25ffffff);
        }
        void drawArena(Canvas c){
            for(int i=0;i<platforms.length;i++){
                RectF f=platforms[i];
                rect(c,f.left,f.top+8,f.right,f.bottom+8,11,0xff090e1b);
                rect(c,f.left,f.top,f.right,f.bottom,10,i==0?0xff33445b:0xff46526b);
                rect(c,f.left,f.top,f.right,f.top+7,5,0xff73b9bf);
            }
        }
        void drawSlime(Canvas c,Slime p){
            if(!p.alive)return;
            c.save();c.translate(p.x,p.y);
            float squash=p.grounded?1.09f:0.94f;
            c.scale(squash,2-squash);
            circle(c,0,4,R+2,0x66000000);
            circle(c,0,0,R,p.color);
            circle(c,-8,-10,9,0x55ffffff);
            circle(c,8,-2,6,Color.WHITE);circle(c,-8,-2,6,Color.WHITE);
            circle(c,8+p.dir*2,-1,2.5f,0xff1a2335);circle(c,-8+p.dir*2,-1,2.5f,0xff1a2335);
            if(p.shield>0){
                fill(c,0x889cefff);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(5);
                c.drawCircle(0,0,R+14,paint);paint.setStyle(Paint.Style.FILL);
            }
            c.restore();
            rect(c,p.x-27,p.y-50,p.x+27,p.y-43,3,0x77000000);
            rect(c,p.x-27,p.y-50,p.x-27+54*(1-p.dash/2.6f),p.y-43,3,p.color);
        }
        void drawShot(Canvas c,Shot s){circle(c,s.x,s.y,s.radius+8,0x33ffffff);circle(c,s.x,s.y,s.radius,players[s.owner].color);circle(c,s.x-4,s.y-5,4,Color.WHITE);}
        void drawParticle(Canvas c,Particle p){circle(c,p.x,p.y,Math.max(1,6*p.life),p.color);}
        void drawHud(Canvas c){
            rect(c,467,12,813,69,24,0xbd080e1c);
            text(c,String.valueOf(players[0].score),552,54,43,players[0].color);
            text(c,"RODADA "+round,640,42,18,0xffe8f3ff);
            text(c,String.valueOf(players[1].score),729,54,43,players[1].color);
            text(c,"PRIMEIRO A 5",640,61,11,0xffa4b3cb);
        }
        void drawOverlay(Canvas c,String headline,String small){
            rect(c,335,236,945,422,26,0xe5091120);
            text(c,headline,640,325,39,Color.WHITE);
            text(c,small,640,372,23,0xffa7ddea);
        }
        final float[] bx={55,145,235,325,415,505,775,865,955,1045,1135,1225};
        final float[] by={638,638,638,638,638,638,638,638,638,638,638,638};
        final String[] names={"◀","▶","↑","●","»","◇","◀","▶","↑","●","»","◇"};
        void drawControls(Canvas c){
            for(int i=0;i<10;i++){
                int color=i<6?0xff56dafa:0xffff69af;
                circle(c,bx[i],by[i],i%6==3?36:33,held[i]?color:0x76344158);
                fill(c,color);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(2);
                c.drawCircle(bx[i],by[i],i%5==3?39:37,paint);paint.setStyle(Paint.Style.FILL);
                text(c,names[i],bx[i],by[i]+12,32,held[i]?0xff101827:Color.WHITE);
                int idx=i/6, ability=i%6;
                float cooldown=ability==3?players[idx].fire:ability==4?players[idx].dash:ability==5?players[idx].shieldCooldown:0;
                float maximum=ability==3?.38f:ability==4?2.6f:ability==5?5f:1f;
                if(cooldown>0){fill(c,0xbbffffff);paint.setStyle(Paint.Style.STROKE);paint.setStrokeWidth(4);
                    c.drawArc(bx[i]-29,by[i]-29,bx[i]+29,by[i]+29,-90,360*Math.min(1,cooldown/maximum),false,paint);
                    paint.setStyle(Paint.Style.FILL);
                }
            }
            text(c,"P1 · AZUL",125,112,21,players[0].color);
            text(c,"P2 · ROSA",1150,112,21,players[1].color);
            text(c,"MOVER    PULO    TIRO    DASH    ESCUDO",275,701,13,0xffa7b0c8);
            text(c,"MOVER    PULO    TIRO    DASH    ESCUDO",1005,701,13,0xffa7b0c8);
        }
        int button(float x,float y){
            for(int i=0;i<10;i++)if(dist(x,y,bx[i],by[i])<39)return i;
            return -1;
        }
        @Override public boolean onTouchEvent(MotionEvent ev){
            float scale=Math.min(getWidth()/W,getHeight()/H);
            float ox=(getWidth()-W*scale)/2f,oy=(getHeight()-H*scale)/2f;
            int act=ev.getActionMasked(),index=ev.getActionIndex();
            if(winner>=0 && act==MotionEvent.ACTION_DOWN){restart();return true;}
            if(act==MotionEvent.ACTION_CANCEL){pointers.clear();}
            else if(act==MotionEvent.ACTION_DOWN||act==MotionEvent.ACTION_POINTER_DOWN){
                int id=ev.getPointerId(index);float x=(ev.getX(index)-ox)/scale,y=(ev.getY(index)-oy)/scale;
                int b=button(x,y);if(b>=0)pointers.put(id,b);
            }else if(act==MotionEvent.ACTION_UP||act==MotionEvent.ACTION_POINTER_UP){
                pointers.remove(ev.getPointerId(index));
            }else if(act==MotionEvent.ACTION_MOVE){
                for(int j=0;j<ev.getPointerCount();j++){
                    int id=ev.getPointerId(j);
                    if(!pointers.containsKey(id))continue;
                    float x=(ev.getX(j)-ox)/scale,y=(ev.getY(j)-oy)/scale;
                    int old=pointers.get(id),b=button(x,y);
                    if(b>=0 && b/6==old/6)pointers.put(id,b);
                    else if(b<0)pointers.remove(id);
                }
            }
            for(int i=0;i<held.length;i++)held[i]=false;
            for(int v:pointers.values())held[v]=true;
            return true;
        }
        static class Slime{
            float x,y,vx,vy,shield,shieldCooldown,fire,dash,dashActive;boolean grounded=true,alive=true,jumpLatch,dashLatch,shieldLatch;int color,dir,score,jumps;
            Slime(float x,int color,int dir){this.x=x;this.color=color;this.dir=dir;}
        }
        static class Shot{
            float x,y,vx,life=1.7f,radius=13f;int owner;
            Shot(float x,float y,float vx,int owner){this.x=x;this.y=y;this.vx=vx;this.owner=owner;}
        }
        static class Particle{
            float x,y,vx,vy,life;int color;
            Particle(float x,float y,float vx,float vy,int color,float life){this.x=x;this.y=y;this.vx=vx;this.vy=vy;this.color=color;this.life=life;}
        }
    }
}
