package traben.stuck_arrows_for_all;

public final class Stuck_arrows_for_all {
    public static final String MOD_ID = "stuck_arrows_for_all";

    public static boolean isForge = false;
    public static void init(boolean forge) {
        // Write common init code here.
        isForge = forge;
    }
    public static void init() {
        init(false);
    }
}
