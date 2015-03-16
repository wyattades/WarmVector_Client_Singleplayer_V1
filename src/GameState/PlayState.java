package GameState;

import Entity.Enemy;
import Entity.Entity;
import Entity.Player;
import Entity.ThisPlayer;
import Entity.Weapon.Weapon;
import HUD.GUI;
import HUD.MouseCursor;
import Main.Game;
import Manager.FileManager;
import Manager.GameStateManager;
import Manager.InputManager;
import Map.TileMap;
import Visual.Animation;
import Visual.Bullet;
import Visual.Shadow2D;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

/**
 * Created by Wyatt on 1/25/2015.
 */
public class PlayState extends GameState {


    private int px,py;
    private HashMap<String, ArrayList<Entity>> entityList;
    private TileMap tileMap;
    private GUI gui;
    private ArrayList<Bullet> bullets;
    private ArrayList<Animation> animations;
    //private Shadow2D shadow;
    private int level;
    private MouseCursor cursor;
    private Robot robot;
    private ThisPlayer thisPlayer;

    public PlayState(GameStateManager gsm) {
        super(gsm);
    }

    public void init() {
        try {
            robot = new Robot();
        } catch (AWTException e) {
            e.printStackTrace();
        }
        bullets = new ArrayList<Bullet>();
        animations = new ArrayList<Animation>();
        tileMap = new TileMap(FileManager.TILESET1, FileManager.BACKGROUND1);
        entityList = tileMap.setEntities();
        gui = new GUI((ThisPlayer) entityList.get("thisPlayer").get(0));
        thisPlayer = (ThisPlayer) entityList.get("thisPlayer").get(0);
        cursor = new MouseCursor(FileManager.CURSOR);
        //shadow = new Shadow(thisPlayer.x,thisPlayer.y,entityList);
        //shadow = new Shadow2D((int)thisPlayer.x,(int)thisPlayer.y,thisPlayer.orient,entityList);
    }

    public void draw(Graphics2D g) {
        g.setColor(tileMap.backgroundColor);
        g.fillRect(0, 0, Game.WIDTH, Game.HEIGHT); //background
        AffineTransform oldT = g.getTransform();
        g.scale(Game.SCALEFACTOR, Game.SCALEFACTOR);
        g.translate(gui.screenPosX+(-px + Game.WIDTH / 2 /Game.SCALEFACTOR),gui.screenPosY+(-py + Game.HEIGHT / 2 /Game.SCALEFACTOR));

        for (Bullet b : bullets) {
            b.draw(g);
        }
        tileMap.draw(g);
        for (Entity entity : entityList.get("weapon")) {
            Weapon w = (Weapon) entity;
            w.draw(g);
        }
        //shadow.update((thisPlayer.x),(thisPlayer.y),thisPlayer.orient,entityList);
        //shadow.draw(g);
        if (thisPlayer.weapon != null) {
            thisPlayer.updateWeapon();
            thisPlayer.weapon.draw(g);
        }
        for (Entity entity : entityList.get("enemy")) {
            Enemy e = (Enemy) entity;
            e.draw(g);
            if (e.weapon != null) {
                e.updateWeapon();
                e.weapon.draw(g);
            }
        }
        thisPlayer.draw(g);
        for (Animation a : animations) {
            a.draw(g);
        }
        g.setTransform(oldT);
        cursor.draw(g);
    }

    public void update() {
        thisPlayer = (ThisPlayer) entityList.get("thisPlayer").get(0);
        thisPlayer.update();
        thisPlayer.updateAngle(cursor.x, cursor.y);
        px = thisPlayer.x;
        py = thisPlayer.y;
        gui.updatePosition();
        thisPlayer.stopMove();
        gui.updateRotation(cursor.x, cursor.y);
        for (Bullet b : bullets) {
            b.update();
        }
        for (Entity entity : entityList.get("weapon")) {
            Weapon w = (Weapon)entity;
            w.updateCollideBox();
        }
        for (Entity entity : entityList.get("enemy")) {
            Enemy e = (Enemy) entity;
            e.normalBehavior(px, py);
            e.update();
            if (e.shooting) addBullets(e);
            if (e.life < 0) entityList.get("weapon").add(e.weapon);
        }
        for (Animation a : animations) {
            a.update();
        }

        for (int i = bullets.size() - 1; i >= 0; i--) {
            if (!bullets.get(i).state) bullets.remove(i); // <-- remove "object" or "index location"???
        }
        for (int i = animations.size() - 1; i >= 0; i--) {
            if (!animations.get(i).state) animations.remove(i);
        }

        //Entity removing outcomes:
        Iterator<HashMap.Entry<String, ArrayList<Entity>>> it = entityList.entrySet().iterator();
        while (it.hasNext()) {
            HashMap.Entry<String, ArrayList<Entity>> entry = it.next();
            for (int i = entry.getValue().size() - 1; i >= 0; i--) {
                if (!entry.getValue().get(i).state) {
                    if (!entry.getKey().equals("thisPlayer")) {
                        if (entry.getKey().equals("enemy")) {
                            Enemy e = (Enemy) entry.getValue().get(i);
                            entityList.get("weapon").add(e.getWeapon());
                        }
                        entry.getValue().remove(i);
                    } else { //player dies
                        gsm.setState(GameStateManager.PLAY);
                    }
                }
            }
        }
    }

    private void addBullets(Player p) {
        if (p.weapon != null && Game.currentTimeMillis() - p.shootTime > p.weapon.rate && p.weapon.ammo > 0) {
            for (int i = 0; i < p.weapon.amount; i++) {
                Bullet b = new Bullet(p.x, p.y, p.orient, p.weapon.spread, p.weapon.damage, entityList,p);
                for (Bullet.CollidePoint point : b.collidePoints) {
                    animations.add(new Animation(point.x, point.y, b.orient, 2, point.hitColor, FileManager.HIT));
                }
                bullets.add(b);
            }
            p.shootTime = Game.currentTimeMillis();
        }
    }

    public void inputHandle() {
        robot.mouseMove(Game.WIDTH / 2, Game.HEIGHT / 2);
        cursor.updatePosition(InputManager.mouse.x-Game.WIDTH/2, InputManager.mouse.y-Game.HEIGHT/2);

        if (InputManager.isKeyPressed("ESCAPE")) System.exit(0);
        if (InputManager.isKeyPressed("LEFT") && !InputManager.isKeyPressed("RIGHT"))
            thisPlayer.updateVelX(-ThisPlayer.topSpeed);
        else if (InputManager.isKeyPressed("RIGHT") && !InputManager.isKeyPressed("LEFT"))
            thisPlayer.updateVelX(ThisPlayer.topSpeed);
        if (InputManager.isKeyPressed("UP") && !InputManager.isKeyPressed("DOWN"))
            thisPlayer.updateVelY(-ThisPlayer.topSpeed);
        else if (InputManager.isKeyPressed("DOWN") && !InputManager.isKeyPressed("UP"))
            thisPlayer.updateVelY(ThisPlayer.topSpeed);

        if (InputManager.isMousePressed("LEFTMOUSE")) {
            addBullets(thisPlayer);
        }

        if (InputManager.isMouseClicked("RIGHTMOUSE") && Game.currentTimeMillis() - InputManager.getMouseTime("RIGHTMOUSE") > 500) {
            InputManager.setMouseTime("RIGHTMOUSE", Game.currentTimeMillis());
            if (thisPlayer.weapon != null) {
                entityList.get("weapon").add(thisPlayer.getWeapon());
                thisPlayer.weapon = null;
                thisPlayer.sprite = FileManager.PLAYER0;
                for (int i = 0; i < entityList.get("weapon").size() - 1; i++) {
                    Weapon w = (Weapon) entityList.get("weapon").get(i);
                    if (thisPlayer.collideBox.intersects(w.collideBox)) {
                        thisPlayer.weapon = w;
                        thisPlayer.sprite = FileManager.PLAYER0G;
                        entityList.get("weapon").remove(w);
                        break;
                    }
                }
            } else {
                for (int i = 0; i < entityList.get("weapon").size(); i++) {
                    Weapon w = (Weapon) entityList.get("weapon").get(i);
                    if (thisPlayer.collideBox.intersects(w.collideBox)) {
                        thisPlayer.weapon = w;
                        thisPlayer.sprite = FileManager.PLAYER0G;
                        entityList.get("weapon").remove(w);
                        break;
                    }
                }
            }
        }
        if (InputManager.isKeyPressed("ALT")) gsm.setPaused(true);

    }

}
