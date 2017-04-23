package simple;
/*
 * Quick and simple approach based on shared mutable state and lock ordering to avoid deadlock.
 *
 * Easy: <200 LOC, plain Java, only lightweight Spark Java is dependency (web framework)
 * Fast: total time spent <1h
 * Not scalable: is supposed to run one one server
 */

import spark.Spark;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

class Log {
    public static void d(String msg) {
        System.out.println(msg);
    }
}

class Account {
    public final String nr;
    private long amount;

    public Account(String nr) {
        this.nr = nr;
        this.amount = 0L;
    }

    public long getAmount() { return amount; }

    public long deposit(long depositAmount) {
        // Ignoring possible integer overflow issue
        this.amount += depositAmount;
        return this.amount;
    }

    public long withdraw(long withdrawAmount) {
        // Ignoring possible integer underflow issue
        this.amount -= withdrawAmount;
        return this.amount;
    }

    public static final Map<String, Account> STORAGE = new HashMap<>();
    public static final int NUMBER_OF_ACCOUNTS = 10;
    static {
        Random random = new Random(42L);
        for (int i=0; i<NUMBER_OF_ACCOUNTS; i++) {
            String nr = String.format("%03d", i);
            Account account = new Account(nr);
            account.deposit(random.nextInt(100) * 1000);
            STORAGE.put(nr, account);
            Log.d("Account " + nr + " created with balance " + account.getAmount());
        }
    }
}

class Transfer {
    public static void execute(Account src, Account dst, long amount) {
        if (src == null || dst == null) {
            throw new IllegalArgumentException("Account cannot be null");
        }
        if (amount <= 0) {
            throw new IllegalArgumentException("Amount must be >0");
        }

        // Ordering locks to avoid deadlock
        Object outer = dst;
        Object inner = src;
        if ( src.nr.compareTo(dst.nr) <= 0 ) {
            outer = src;
            inner = dst;
        }

        synchronized (outer) {
            synchronized (inner) {
                src.withdraw(amount);
                dst.deposit(amount);
            }
        }
    }
}

class Server {
    public static void start() {
        Spark.port(8080);

        Spark.get("/balance/:act", (req, res) -> {
            String nr = req.params("act");
            Account act = Account.STORAGE.get(nr);
            if (act == null) {
                res.status(404);
                return "Not Found";
            } else {
                return act.getAmount();
            }
        });

        // Execution of transfer is not idempotent and changes state - POST in RESTful semantics
        Spark.post("/transfer/:src/:dst/:amt", (req, res) -> {
            String srcNr = req.params("src");
            String dstNr = req.params("dst");
            Long amount = Long.parseLong(req.params("amt"));

            Account src = Account.STORAGE.get(srcNr);
            Account dst = Account.STORAGE.get(dstNr);

            if (src == null) {
                res.status(404);
                return "SRC account not found";
            } else if (dst == null) {
                res.status(404);
                return "DST account not found";
            }

            try {
                Transfer.execute(src, dst, amount);
                res.status(200);
                return "OK";
            } catch (Exception e) {
                res.status(400);
                return e.getMessage();
            }
        });

        Spark.post("/shutdown", (req, res) -> {
            stop();
            return "stopped";
        });
    }

    public static void stop() {
        Spark.stop();
    }
}

public class JMain {
    public static void main(String args[]) {
        Server.start();
    }
}
