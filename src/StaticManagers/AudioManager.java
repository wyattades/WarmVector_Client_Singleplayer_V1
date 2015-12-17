package StaticManagers;

import Helper.MyMath;

import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import java.util.*;

/**
 * Directory: WarmVector_Client_Singleplayer/StaticManagers/
 * Created by Wyatt on 12/13/2015.
 */
public class AudioManager {

    private static float musicVolume, SFXVolume;
    static {
        updateSettings();
    }

//    private static Map<String, Clip> audioFiles;
//    static {
//        Map<String, Clip> temp = new HashMap<>(FileManager.sounds);
//        audioFiles = Collections.unmodifiableMap(temp);
//    }

    public static void updateSettings() {

        musicVolume = MyMath.map(OutputManager.getSetting("music_volume"), 0, 4, -80, 6);

        SFXVolume = MyMath.map(OutputManager.getSetting("sfx_volume"), 0, 4, -80, 6);

    }

    public static void playSFX(Clip clip) {

//        Clip clip = audioFiles.get(name);

        //reset clip
        clip.stop();
        clip.flush();
        clip.setFramePosition(0);

        //set volume of clip
        FloatControl gainControl =
                (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        gainControl.setValue(SFXVolume);

        clip.start();
    }

    public static void playMusic(Clip clip) {
        //reset clip
        clip.stop();
        clip.flush();
        clip.setFramePosition(0);

        //set volume of clip
        FloatControl gainControl =
                (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        gainControl.setValue(musicVolume);

        clip.start();
    }

    public void pauseAll() {

    }

    public void stopAll() {

    }

}
