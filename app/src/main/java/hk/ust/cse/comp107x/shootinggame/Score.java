package hk.ust.cse.comp107x.shootinggame;

import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

/**
 * Created by Jinghao on 01/22/2018.
 */
public class Score {

    private Paint paint;
    private int score;

    // Constructor
    public Score(int color) {
        paint = new Paint();
        // Set the font face and size of drawing text
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(48);
        paint.setColor(color);

        // TODO initialize score
        score = 0;
    }

    public void incrementScore() {
        // TODO Increment score
        score++;
    }

    public void decrementScore() {
        // TODO Decrement score
        score--;
    }

    public int getScore() { return score; }

    public void reset(){
        score = 0;
    }

    public void draw(Canvas canvas) {

        // TODO use drawText(String, x co-ordinate, y-coordinate, paint) to
        // draw text on the canvas. Position the text at (10,30).
        canvas.drawText("Score: " + score, 10, 40, paint);
    }
}