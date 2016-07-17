package aerodude30.com;

import org.powerbot.script.AbstractScript;
import org.powerbot.script.Condition;
import org.powerbot.script.Filter;
import org.powerbot.script.Random;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Npc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.DecimalFormat;
import java.util.concurrent.TimeUnit;

/**
 * Created by cjb on 6/29/2016.
 * These methods are used for converting, formatting, and calculating data to be displayed for the graphics.
 * The scripts Antipattern methods are also bundled in this class to randomize and humanize the script.
 */
public class Util extends AbstractScript<ClientContext> {

    /**
     * Formats experience gained to be in an hourly format
     * @param gained the experience gained
     * @param startTime the start time of the script
     * @return
     */
    String perHour(int gained, long startTime) {
        return formatNumber( (int) ((gained) * 3600000D / (System.currentTimeMillis() - startTime)));
    }

    /**
     * Formats numbers for thousands and millions
     * @param start the start time.
     * @return String value of the formatted number
     */
    String formatNumber(int start) {

        DecimalFormat nf = new DecimalFormat("0.0");
        if((double) start >= 1000000) {
            return nf.format(((double) start / 1000000)) + "m";
        }
        if((double) start >= 1000) {
            return nf.format(((double) start / 1000)) + "k";
        }
        return String.valueOf(start);
    }

    /**
     * Formats a millisecond time into hours/minutes/seconds
     * @param start Start time of the script in milliseconds
     * @return formatted String for runtime in the format (h:m:s)
     */
    String runtime(long start) {
        DecimalFormat nf = new DecimalFormat("00");
        long millis = System.currentTimeMillis() - start;

        long hours = millis / (1000 * 60 * 60);
        millis -= hours * (1000 * 60 * 60);
        long minutes = millis / (1000 * 60);
        millis -= minutes * (1000 * 60);
        long seconds = millis / 1000;

        return nf.format(hours) + ":" + nf.format(minutes) + ":" + nf.format(seconds);
    }

    /**
     * Performs a different anti-pattern action sometimes but not every time
     * to give the bot a more humanlike appearence.
     */
    void antiPattern() {
        int rnd = Random.nextInt(0, 50);
        switch(rnd) {
            case 5:
                ctx.widgets.component(548, 53).click();
                ctx.widgets.component(320, Random.nextInt(1, 20)).hover();

                Condition.sleep(Random.nextInt(2000, 4000));

                ctx.widgets.component(548, 55).click();
                break;

            case 10:
                GameObject object = ctx.objects.select().within(6.0).poll();
                if(object.inViewport()) {
                    object.interact("Examine", object.name());
                } else {
                    ctx.camera.turnTo(object);
                    object.interact("Examine", object.name());
                }
                break;

            case 15:
                ctx.camera.pitch(Random.nextInt(0, 90));
                break;
            case 20:
                if(ctx.movement.energyLevel() > Random.nextInt(50, 100)) {
                    ctx.movement.running(true);
                } else {
                    break;
                }
                break;

            default:
                break;

        }
    }

    void dismissRandom() {
        /* Credit to @laputa.  URL: https://www.powerbot.org/community/topic/1292825-random-event-dismisser/  */
        Npc randomNpc = ctx.npcs.select().within(2.0).select(new Filter<Npc>() {
            @Override
            public boolean accept(Npc npc) {
                return npc.overheadMessage().contains(ctx.players.local().name());
            }

        }).poll();

        if (randomNpc.valid()) {
            String action = randomNpc.name().equalsIgnoreCase("genie") ? "Talk-to" : "Dismiss";
            if (randomNpc.interact(action)) {
                try {
                    TimeUnit.MILLISECONDS.sleep((long) (Random.nextDouble(3, 3.5) * 1000));
                } catch (InterruptedException e) {
                    e.getMessage();
                }
            }

        }
    }

    int getPrice(int id) {
        try {
            URL url = new URL("http://open.tip.it/json/ge_single_item?item=" + id);
            URLConnection con = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(
                    con.getInputStream()));

            String line = "";
            String inputLine;

            while ((inputLine = in.readLine()) != null) {
                line += inputLine;
            }

            in.close();


            if (!line.contains("mark_price"))
                return -1;

            line = line.substring(line.indexOf("mark_price\":\"")
                    + "mark_price\":\"".length());
            line = line.substring(0, line.indexOf("\""));

            line = line.replace(",", "");
            return Integer.parseInt(line);

        } catch (IOException e) {
            e.printStackTrace();
            return -1;
        }
    }
}