package aerodude30.com;

import org.powerbot.script.Area;
import org.powerbot.script.Condition;
import org.powerbot.script.PaintListener;
import org.powerbot.script.PollingScript;
import org.powerbot.script.Random;
import org.powerbot.script.Script;
import org.powerbot.script.Tile;
import org.powerbot.script.rt4.Bank.Amount;
import org.powerbot.script.rt4.ClientContext;
import org.powerbot.script.rt4.Component;
import org.powerbot.script.rt4.GameObject;
import org.powerbot.script.rt4.Npc;

import java.awt.*;
import java.util.concurrent.Callable;


/**
 * Created by cbartram on 7/6/2016.
 */
@Script.Manifest(name = "AeroCraft", properties = "author=aerodude30; topic=1296203; client=4;", description = "Efficiently, crafts Gold Necklaces in Al-Kharid.")
public class AeroCraft extends PollingScript<ClientContext>  implements PaintListener {

    //Constants
    private final int GOLD_BAR = 2357, AMULET_MOULD = 1597, BANK_BOOTH = 6943, FURNACE = 24009, GOLD_AMULET = 1654, START_LEVEL = ctx.skills.level(12);
    private final int[] BANKER = {396, 397};
    private final Component FURNACE_DIALOG = ctx.widgets.component(446, 21);
    private final Area BANK_AREA = new Area(new Tile(3272, 3162, 0), new Tile(3269, 3162, 0), new Tile(3269, 3173, 0), new Tile(3272, 3173, 0));
    private final Area FURNACE_AREA = new Area(new Tile(3274, 3184, 0), new Tile(3274, 3188, 0), new Tile(3279, 3188, 0), new Tile(3279, 3184, 0));
    private final Tile[] pathToFurnace = {
            new Tile(3278, 3167, 0),
            new Tile(3280, 3180, 0),
            FURNACE_AREA.getRandomTile()
    };
    private final Tile[] pathToBank = {
            new Tile(3280, 3180, 0),
            new Tile(3278, 3167, 0),
            BANK_AREA.getRandomTile()
    };

    //Variables
    private int amuletsMade = 0, startExperience = 0;
    private long startTime;
    private String status = "Waiting to start...";

    //Instances
    private Util util = new Util();


    //Enumerations for each State in the script
    private enum State {BANK, TRAVERSE, SMELT, REVERSE}

    /**
     * Returns the current state of the script
     * @return State Returns a state object for the currently active state in the script
     */
    private State getState() {
        if(ctx.inventory.select().id(GOLD_BAR).count() == 0 && BANK_AREA.contains(ctx.players.local().tile())) {
            return State.BANK;
        }

        if(ctx.inventory.select().id(GOLD_AMULET).count() == 27 && ctx.inventory.select().id(GOLD_BAR).count() == 0) {
            return State.REVERSE;
        }

        return FURNACE_AREA.contains(ctx.players.local().tile()) && ctx.inventory.select().id(GOLD_BAR).count() >= 1 ? State.SMELT : State.TRAVERSE;
    }

    @Override
    public void start() {
        startTime = System.currentTimeMillis();
        startExperience = ctx.skills.experience(14);
    }


    @Override
    public void poll() {
        util.dismissRandom();
        System.out.println(ctx.inventory.select().id(GOLD_AMULET).count() == 27);
        State state = getState();
        System.out.println(state);
        switch (state) {
            case BANK:
                status = "Banking...";
                if(ctx.bank.inViewport() && !ctx.bank.opened()) {
                    final Npc banker = ctx.npcs.select().id(BANKER).shuffle().poll();

                    ctx.camera.turnTo(banker);

                    Boolean bankOrBanker = Random.nextBoolean();

                    if (bankOrBanker) {
                        ctx.bank.open();
                    } else {
                        status = "Interacting with Banker NPC";
                        ctx.camera.turnTo(banker);
                        banker.interact("Bank", banker.name());
                    }

                } else {
                    if(ctx.inventory.select().id(AMULET_MOULD).count() != 1) {
                        status = "Withdrawing Mould";
                        ctx.bank.withdraw(AMULET_MOULD, 1);
                    }

                    if(ctx.inventory.select().id(GOLD_AMULET).count() >= 1) {
                        status = "Depositing Amulets";
                        ctx.bank.deposit(GOLD_AMULET, Amount.ALL);
                        ctx.bank.withdraw(GOLD_BAR, 27);
                    }
                    ctx.bank.close();
                }
                break;

            case TRAVERSE:
                status = "Walking to Furnace";
                ctx.movement.newTilePath(pathToFurnace).traverse();

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return !ctx.players.local().inMotion();
                    }
                }, 2500, 10);

                break;

            case SMELT:
                final GameObject furnace = ctx.objects.select().id(FURNACE).poll();

                if(furnace.inViewport() && !FURNACE_DIALOG.visible()) {
                    status = "Using Gold on Furnace...";

                    //use the gold bar on the furnace
                    ctx.inventory.select().id(GOLD_BAR).poll().interact("Use", "Gold bar");
                    furnace.click(true);

                    if(FURNACE_DIALOG.visible()) {
                        status = "Waiting, Smelting...";
                        FURNACE_DIALOG.interact("Make-10");

                        Condition.wait(new Callable<Boolean>() {
                            @Override
                            public Boolean call() throws Exception {
                                return ctx.players.local().animation() != 899;
                            }
                        }, 2500, 10);
                    }
                }

                break;

            case REVERSE:
                status = "Walking to Bank";
                ctx.movement.newTilePath(pathToBank).traverse();

                Condition.wait(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        return !ctx.players.local().inMotion();
                    }
                }, 2500, 10);
                break;
        }

    }

    @Override
    public void repaint(Graphics graphics) {
        Graphics2D g = (Graphics2D) graphics;

        g.setColor(new Color(222, 71, 0));
        final Point pt =  ctx.input.getLocation();
        int x = (int) pt.getX();
        int y = (int) pt.getY();
        g.drawOval(pt.x - 8, pt.y - 8, 15, 15);
        g.drawLine(x, y - 5, x, y + 5);
        g.drawLine(x - 5, y, x + 5, y);

        int expGained = ctx.skills.experience(14) - startExperience;
        g.setRenderingHints(new RenderingHints(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF));
        g.setColor(new Color(136, 136, 136, 117));
        g.fillRect(3, 3, 195, 185);
        g.setStroke(new BasicStroke(3));
        g.setColor(new Color(255, 136, 0));
        g.drawRect(3, 3, 195, 185);
        g.setColor(new Color(255, 136, 0));
        g.drawLine(12, 31, 187, 31);
        g.setColor(new Color(255, 255, 255));
        g.setFont(new Font("Arial", 0, 14));
        g.drawString("AeroCraft", 61, 25);
        g.setColor(new Color(255, 255, 255));
        g.setFont(new Font("Arial", 0, 11));
        g.drawString("Time Running: " , 13, 48);
        g.drawString("Crafting Exp Gained: ", 14, 65);
        g.drawString("Crafting/hour: ", 14, 84);
        g.drawString("Starting Level: ", 15, 103);
        g.drawString("Current Level: ", 15, 123);
        g.drawString("Status: ", 16, 141);
        g.drawString("Amulets Made: ", 16, 159);
        g.drawString("Profit: ", 16, 178);
        g.drawString(util.runtime(startTime), 85, 49);
        g.drawString(String.valueOf(expGained), 110, 65);//
        g.drawString(util.perHour(expGained, startTime), 76, 84);//
        g.drawString(String.valueOf(START_LEVEL), 87, 103);
        g.drawString(String.valueOf(ctx.skills.level(14)), 86, 123);
        g.drawString(status, 52, 141);
        g.drawString(String.valueOf(amuletsMade), 96, 159);
        g.drawString(String.valueOf(util.formatNumber(util.getPrice(GOLD_AMULET) * amuletsMade)), 48, 178);

    }
}
