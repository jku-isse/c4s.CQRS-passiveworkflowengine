package counter.query.snapshot;//package pingpong.query.history;

import java.util.Scanner;
import java.util.concurrent.CompletableFuture;

public class CLTool {

    public CompletableFuture<Action> readAction() {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Choose action (store, stop, print, step):");
        String s = scanner.nextLine();
        CompletableFuture<Action> completableFuture = new CompletableFuture<>();
        switch (s) {
            case "store":
                completableFuture.complete(Action.STORE);
                break;
            case "stop":
                completableFuture.complete(Action.STOP);
                break;
            case "print":
                completableFuture.complete(Action.PRINT);
                break;
            default:
                completableFuture.complete(Action.STEP);
        }
        return completableFuture;
    }

    enum Action {STORE, STEP, STOP, PRINT};
}
