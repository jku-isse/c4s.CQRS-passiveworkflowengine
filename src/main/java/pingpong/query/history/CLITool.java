package pingpong.query.history;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class CLITool {

    public CompletableFuture<Action> readAction() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose action (store, step):");
        String s = scanner.nextLine();
        CompletableFuture<Action> completableFuture = new CompletableFuture<>();
        switch (s) {
            case "step":
                completableFuture.complete(Action.STEP);
                break;
            case "store":
                completableFuture.complete(Action.STORE);
                break;
            default:
                completableFuture.complete(Action.ERROR);
        }
        return completableFuture;
    }

    enum Action {STORE, STEP, ERROR};
}
