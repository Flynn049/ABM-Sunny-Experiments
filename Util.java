import org.nlogo.headless.HeadlessWorkspace;

import java.util.Random;

public class Util {
    public static void ConfigureS1(HeadlessWorkspace workspace) {
        workspace.command("set rand-seed " + new Random().nextInt());
        workspace.command("set power-model-method \"stepwise simple linear regression\"");
        workspace.command("set server-consolidation-strategy \"within datacentre\"");
        workspace.command("set service-lifetime \"[100 300]\"");
        workspace.command("set consolidation-interval 12");
        workspace.command(("set power-estimation-method \"mean\""));
        workspace.command("set rack-level-heterogeneity? true");
        workspace.command("set evaluation-stage \"2\"");
        workspace.command("set scheduler-history-length 5");
        workspace.command("set auto-migration? true");
        workspace.command("set server-standby-strategy \"all-off\"");
        workspace.command("set server-model \"[2 3 4 5]\"");
        workspace.command("set display-migration-movement? false");
        workspace.command("set show-trace-on? false");
    }
}
