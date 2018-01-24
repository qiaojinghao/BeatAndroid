package hk.ust.cse.comp107x.shootinggame;

/**
 * Created by Jinghao on 01/22/2018.
 */

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.media.SoundPool;
import android.os.Message;
import android.support.v7.app.AlertDialog;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class DrawView extends SurfaceView implements SurfaceHolder.Callback {

    private int width, height;
    private DrawViewThread drawviewthread;

    Context mContext;

    // We can have multiple bullets and explosions
    // keep track of them in ArrayList
    ArrayList<Bullet> bullets;
    ArrayList<Explosion> explosions;
    Cannon cannon;
    //AndroidGuy androidGuy;
    ArrayList<AndroidGuy> androidGuys;
    Score score;

    public DrawView(Context context, AttributeSet attrs) {
        super(context, attrs);

        mContext = context;

        getHolder().addCallback(this);

        setFocusable(true);
        this.requestFocus();

        // create a cannon object
        cannon = new Cannon(Color.BLUE,mContext);

        // create arraylists to keep track of bullets and explosions
        bullets = new ArrayList<Bullet> ();
        explosions = new ArrayList<Explosion>();

        // create the falling Android Guy
        androidGuys = new ArrayList<AndroidGuy>();
        androidGuys.add(new AndroidGuy(Color.RED, mContext));
        score = new Score(Color.BLACK);
    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width,
                               int height) {

    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

        drawviewthread = new DrawViewThread(holder);
        drawviewthread.setRunning(true);
        drawviewthread.start();

    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {

        boolean retry = true;
        drawviewthread.setRunning(false);

        while (retry){
            try {
                drawviewthread.join();
                retry = false;
            }
            catch (InterruptedException e){

            }
        }

    }

    public class DrawViewThread extends Thread{
        private SurfaceHolder surfaceHolder;
        private boolean threadIsRunning = true;

        public DrawViewThread(SurfaceHolder holder){
            surfaceHolder = holder;
            setName("DrawViewThread");
        }

        public void setRunning (boolean running){
            threadIsRunning = running;
        }

        public void run() {
            Canvas canvas = null;

            int count = 0;


            while (threadIsRunning) {

                try {
                    canvas = surfaceHolder.lockCanvas(null);

                    synchronized(surfaceHolder){
                        drawGameBoard(canvas);
                        count++;
                        if(count % 16 == 0){
                            bullets.add(new Bullet(Color.RED, mContext, cannon.getPosition(), (float) (height - 40)));
                            SoundEffects.INSTANCE.playSound(SoundEffects.SOUND_BULLET);
                        }
                        if(count % 48 == 0){
                            AndroidGuy androidGuy = new AndroidGuy(Color.RED, mContext);
                            androidGuy.setBounds(0,0,width,height);
                            androidGuys.add(androidGuy);
                        }

                    }
                    sleep(30);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                finally {
                    if (canvas != null)
                        surfaceHolder.unlockCanvasAndPost(canvas);
                }
            }
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        width = w;
        height = h;

        cannon.setBounds(0,0,width, height);

        for(AndroidGuy androidGuy:androidGuys){
            androidGuy.setBounds(0,0,width,height);
        }
        for (int i = 0; i < bullets.size(); i++ ) {
            bullets.get(i).setBounds(0,0,width,height);
        }

    }

    public void drawGameBoard(Canvas canvas) {
        canvas.drawColor(Color.WHITE);     //if you want another background color
        // Draw the cannon
        cannon.draw(canvas);

        // Draw all the bullets
        for (int i = 0; i < bullets.size(); i++ ) {
            if (bullets.get(i) != null) {
                bullets.get(i).draw(canvas);

                if (bullets.get(i).move() == false) {
                    bullets.remove(i);
                }
            }
        }

        // Draw all the explosions, at those locations where the bullet
        // hits the Android Guy
        for (int i = 0; i < explosions.size(); i++ ) {
            if (explosions.get(i) != null) {
                if (explosions.get(i).draw(canvas) == false) {
                    explosions.remove(i);
                }
            }
        }

        // If the Android Guy is falling, check to see if any of the bullets
        // hit the Guy
        for(int j=0; j<androidGuys.size(); j++) {
            AndroidGuy androidGuy = androidGuys.get(j);
                androidGuy.draw(canvas);

                RectF guyRect = androidGuy.getRect();

                for (int i = 0; i < bullets.size(); i++) {

                    // The rectangle surrounding the Guy and Bullet intersect, then it's a collision
                    // Generate an explosion at that location and delete the Guy and bullet. Generate
                    // a new Android Guy to fall from the top.
                    if (RectF.intersects(guyRect, bullets.get(i).getRect())) {
                        explosions.add(new Explosion(Color.RED, mContext, androidGuy.getX(), androidGuy.getY()));
                        //androidGuy.reset();
                        bullets.remove(i);
                        androidGuys.remove(androidGuy);
                        score.incrementScore();

                        // Play the explosion sound by calling the SoundEffects class
                        //  Generate explosion sound

                        SoundEffects.INSTANCE.playSound(SoundEffects.SOUND_EXPLOSION);

                        break;
                    }

                }
                if (RectF.intersects(guyRect, cannon.getRect())) {
                        SoundEffects.INSTANCE.playSound(SoundEffects.SOUND_EXPLOSION);
                        explosions.add(new Explosion(Color.RED, mContext, androidGuy.getX(), androidGuy.getY()));
                        androidGuys.remove(androidGuy);
                        stopGame();
                        Message msg = new Message();
                        msg.what = 1;
                        handler.sendMessage(msg);
                }

                if(!androidGuy.move()){
                    score.decrementScore();
                }

        }

        score.draw(canvas);
    }

    private android.os.Handler handler = new android.os.Handler(){
        @Override
        public void handleMessage(Message msg){
            switch (msg.what){
                case 1:
                    AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
                    builder.setTitle("Game Over");
                    builder.setCancelable(false);
                    builder.setMessage("Your Score is "+score.getScore());
                    builder.setPositiveButton("Play Again", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            resumeGame();
                        }
                    });
                    builder.show();
                    break;
                default:
                    break;
            }
        }
    };


    // Move the cannon left or right
    public void moveCannonLeft() {
        cannon.moveLeft();
    }

    public void moveCannonRight() {
        cannon.moveRight();
    }

    // Whenever the user shoots a bullet, create a new bullet moving upwards
    public void shootCannon() {

        bullets.add(new Bullet(Color.RED, mContext, cannon.getPosition(), (float) (height - 40)));

        SoundEffects.INSTANCE.playSound(SoundEffects.SOUND_BULLET);
    }

    public void stopGame(){
        if (drawviewthread != null){
            drawviewthread.setRunning(false);
        }
    }

    public void resumeGame(){
        if (drawviewthread != null){
            drawviewthread.setRunning(true);
        }
    }

    public void releaseResources(){

    }

}
