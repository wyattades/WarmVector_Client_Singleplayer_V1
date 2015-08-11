package Visual;

import Entity.ThisPlayer;
import Main.Game;

/**
 * Directory: WarmVector_Client_Singleplayer/${PACKAGE_NAME}/
 * Created by Wyatt on 1/24/2015.
 */
public class ScreenMover {

    public int screenVelX, screenVelY, screenPosX, screenPosY;
    private float maxVel;
    private ThisPlayer player;
    private static final float posAccel = 1.0f, negAccel = 1.0f;
    private static final int MaxRadius = 70;

    public ScreenMover(ThisPlayer player) {
        screenVelX = screenVelY = 0;
        screenPosX = screenPosY = 0;
        maxVel = 24;
        this.player = player;
    }

    public void updatePosition() {
        updateVelocity();
        screenPosX = screenVelX;
        screenPosY = screenVelY;
    }

    private void updateVelocity() {

        if (player.vx > 0) screenVelX = (int) Math.min(screenVelX + posAccel, maxVel);
        else if (player.vx < 0) screenVelX = (int) Math.max(screenVelX - posAccel, -maxVel);
        else {
            if (screenVelX > 0) screenVelX = (int) Math.max(screenVelX - negAccel, 0);
            else if (screenVelX < 0) screenVelX = (int) Math.min(screenVelX + negAccel, 0);
        }

        if (player.vy > 0) screenVelY = (int) Math.min(screenVelY + posAccel, maxVel);
        else if (player.vy < 0) screenVelY = (int) Math.max(screenVelY - posAccel, -maxVel);
        else {
            if (screenVelY > 0) screenVelY = (int) Math.max(screenVelY - negAccel, 0);
            else if (screenVelY < 0) screenVelY = (int) Math.min(screenVelY + negAccel, 0);
        }
    }

    public void updateRotation(double mouseX, double mouseY) {
        float rotateRadius = (float) (-MaxRadius * Math.sqrt((mouseX - Game.WIDTH / 2) * (mouseX - Game.WIDTH / 2) + (mouseY - Game.HEIGHT / 2) * (mouseY - Game.HEIGHT / 2)) / (Game.WIDTH / 2));
        //int rotateRadius = -70;
        screenPosX += rotateRadius * Math.cos(player.orient);
        screenPosY += rotateRadius * Math.sin(player.orient);
    }
}