import org.nlogo.headless.HeadlessWorkspace;

import java.io.*;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.*;

public class Example {
  private static int []intervals_ = {30, 36};
  private static String []algs_ = {"balanced-fit", "min-power", "max-utilisation", "first-fit"};

  private ExecutorService es;
  public static void main(String[] argv) throws InterruptedException, IOException, ExecutionException {

    List<Future<Result>> single_results = new ArrayList<>();
    List<Future<Result>> alternate_results = new ArrayList<>();
    List<Future<Result>> enhanced_alternate_results = new ArrayList<>();
    Predictor predictor = new Predictor(algs_, intervals_);
    // try single ones
//    new Thread(() -> {
//      try {
//        predictor.SinglePrediction(single_results);
//      } catch (ExecutionException | InterruptedException e) {
//        throw new RuntimeException(e);
//      }
//    }).start();
//    // and alternate
//    new Thread(() -> {
//      try {
//        predictor.AlternatePrediction(alternate_results);
//      } catch (ExecutionException | InterruptedException | IOException e) {
//        throw new RuntimeException(e);
//      }
//    }).start();
    // enhanced alternate
    new Thread(()-> {
      try {
        predictor.EnhancedAlternatePrediction(enhanced_alternate_results);
      } catch (ExecutionException | InterruptedException | IOException e) {
        throw new RuntimeException(e);
      }
    }).start();
  }
}
class Predictor {

  private String[] algs_;
  private int[] intervals_;

  public Predictor(String[] algs_, int[] intervals_) {
    this.algs_ = algs_;
    this.intervals_ = intervals_;
  }

  public void SinglePrediction (List<Future<Result>> futures) throws ExecutionException, InterruptedException {
    int cnt = 54;
    ExecutorService es = Executors.newFixedThreadPool(algs_.length);
    while (++cnt <= 100) {
      futures.clear();
      for (String alg : algs_) {
        futures.add(es.submit(new Worker(alg, cnt)));
      }
      for (Future<Result> future : futures) {
        future.get();
      }
      System.out.println("run " + cnt + "completed...");
    }
    es.shutdown();
  }
  public void AlternatePrediction (List<Future<Result>> futures) throws ExecutionException, InterruptedException, IOException {
    int cnt = 95;
    while (++cnt <= 100) {
      ExecutorService es = Executors.newFixedThreadPool(intervals_.length);
      for (int interval : intervals_) {
        futures.add(es.submit(new AlternateWorker(interval, cnt)));
      }
      for (Future<Result> future : futures) {
        future.get();
      }
      System.out.println("alternate" + "run" + cnt + "completed");
      es.shutdown();
    }
  }

  public void EnhancedAlternatePrediction(List<Future<Result>> futures) throws ExecutionException, InterruptedException, IOException {
    int cnt = 0;
    int []intervals = {6, 18};
    String []algs = {"balanced-fit", "min-power"};
    while (++cnt <= 100) {
      ExecutorService es = Executors.newFixedThreadPool(1);
      futures.add(es.submit(new EnhancedAlternateWorker(intervals, algs, cnt)));
      for (Future<Result> future : futures) {
        future.get();
      }
      System.out.println("alternate" + "run" + cnt + "completed");
      es.shutdown();
    }
  }
}
/* each worker corresponds to an algorithm */
class Worker implements Callable<Result> {
  private final String name_;
  private HeadlessWorkspace workspace_;

  private int run_;
  private int interval_ = -1;
  public Worker(String alg, int run) {
    this.name_ = alg;
    run_ = run;
  }
  public Worker(String alg, int interval, int run) {
    this.name_ = alg;
    this.interval_ = interval;
    this.run_ = run;
  }
  void setUp() throws IOException {
    workspace_ = HeadlessWorkspace.newInstance();
    workspace_.open("Sunny0-0-1-alpha.nlogo");
    Util.ConfigureS1(workspace_);
    workspace_.command("setup");
    workspace_.command("set service-placement-algorithm " + "\"" + this.name_ + "\"");
    workspace_.command("while [any? services] [go]");
  }
  void setUpWithInterval() throws IOException {
    workspace_ = HeadlessWorkspace.newInstance();
    workspace_.open("Sunny0-0-1-alpha.nlogo");
    workspace_.command("setup");
    workspace_.importWorld("intermediate/interval-" + interval_ + "-temp-world.csv");
    workspace_.command("set service-placement-algorithm " + "\"" + this.name_ + "\"");
    System.out.printf("worker{interval:%d, alg:%s} starts execution%n", this.interval_, this.name_);
    workspace_.command("repeat " + interval_ + "[ go ]");
//    System.out.println("worker: interval set up finished");
  }

  @Override
  public Result call() throws Exception {
    // run the simulation
    try {
      if (interval_ != -1) {
        setUpWithInterval();
      } else {
        setUp();
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    // get the result
    Result result = new Result();
    result.setAlg_(this.name_);
    result.setInterval_(this.interval_);
    result.setPower_consumption_((double) workspace_.report("sys-power-consumption-total"));
    result.setService_lifetime_total_((double) workspace_.report("sys-service-lifetime-total"));
    result.setService_mem_sla_vio_((double) workspace_.report("sys-service-mem-sla-vio"));
    result.setService_net_sla_vio_((double) workspace_.report("sys-service-net-sla-vio"));
    result.setService_ops_sla_vio_((double) workspace_.report("sys-service-ops-sla-vio"));
    result.setCpu_avg_usage_((double) workspace_.report("exp-avg-cpu-usage"));
    result.setMem_avg_usage_((double) workspace_.report("exp-avg-mem-usage"));
    result.setNet_avg_usage_((double) workspace_.report("exp-avg-net-usage"));
    result.setMigrations_(
            (double) workspace_.report("sys-migration-event-due-to-consolidation-total") +
            (double) workspace_.report("sys-migration-event-due-to-auto-migration-total")
    );
    if (interval_ == -1) {
      // write to files
      try {
        workspace_.exportPlot("Power Consumption", "sc1/" + this.name_ + "/power/" + "run" + run_ + ".csv");
        workspace_.exportPlot("SLA Violation (Lifetime Extended)", "sc1/" + this.name_ + "/sla/" + "run" + run_ + ".csv");
        workspace_.exportPlot("Avg Resource Utilization(Datacenter)", "sc1/" + this.name_ + "/avg resource utilisation/" + "run" + run_ + ".csv");
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    workspace_.dispose();
    return result;
  }
}
/* alternate using different intervals */
class EnhancedAlternateWorker implements Callable<Result> {
  private HeadlessWorkspace workspace_;
  private int []intervals_;
  private String []algs_;
  private final int run_;
  EnhancedAlternateWorker(int []intervals, String []algs, int run) {
    this.intervals_ = intervals;
    this.run_ = run;
    this.algs_ = algs;
  }
  @Override
  public Result call() throws Exception {
    workspace_ = HeadlessWorkspace.newInstance();
    workspace_.open("Sunny0-0-1-alpha.nlogo");
    Util.ConfigureS1(workspace_);
    workspace_.command("setup");
    String current_alg;
    int current_interval;
    double prev_power = 0.0f;
    double prev_mem_sla = 0.0f;
    double prev_net_sla = 0.0f;
    double prev_cpu_sla = 0.0f;
    double prev_migrations = 0.0f;
    while (true) {
      if ((boolean) workspace_.report("not any? services")) {
        new Thread(()->{
          try {
            workspace_.exportPlot("Power Consumption","sc2-e/power/" + "run"+  run_ + ".csv");
            workspace_.exportPlot("SLA Violation (Lifetime Extended)","sc2-e/sla/" + "run"+  run_ + ".csv");
            workspace_.exportPlot("Avg Resource Utilization(Datacenter)","sc2-e/avg resource utilisation/" + "run"+  run_ + ".csv");

            File f = new File("sc2-e/total # of migration due to consolidation.txt");
            FileWriter fw = new FileWriter(f, true);
            fw.write((double) workspace_.report("sys-migration-event-due-to-consolidation-total") + "\n");
            fw.flush();
            f = new File("sc2-e/total # of migration due to auto migration.txt");
            fw = new FileWriter(f, true);
            fw.write((double) workspace_.report("sys-migration-event-due-to-auto-migration-total") + "\n");
            fw.flush();
            fw.close();
//        workspace_.exportPlot("# of Migration Events triggered by Consolidation","sc2-a/interval" + this.interval_ + "/# of migration by consolidation/" + "run"+  run_ + ".csv");
//        workspace_.exportPlot("# of Migration triggered by Auto Migration","sc2-a/interval" + this.interval_ + "/# of migration by auto migration/" + "run"+  run_ + ".csv");
            workspace_.command("ask servers [ set status \"OFF\" set color white set power 0 reset-server self ]");
            workspace_.dispose();
          } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
          }
        }).start();
        break;
      }
      // map from interval len to its results (balanced-fit, min-power)
      Map<Integer, List<Future<Result>>> local_results_map = new HashMap<>();
      ExecutorService es = Executors.newFixedThreadPool(intervals_.length);
      for (int interval : intervals_) {
        local_results_map.put(interval, new ArrayList<>());
      }
      for (int interval : intervals_) {
        workspace_.exportWorld("intermediate/interval-" + interval + "-temp-world.csv");
        for (String alg : algs_) {
          local_results_map.get(interval).add(es.submit(new Worker(alg, interval, run_)));
        }
      }
      // wait for threads to finish
      for (int interval : intervals_) {
        for (Future<Result> future : local_results_map.get(interval)) {
          future.get();
        }
      }
      es.shutdown();
      // check each combination of interval + alg, assume linearity
      int max_itv = intervals_[0];
      for (int interval : intervals_) {
        max_itv = Math.max(max_itv, interval);
      }
      List<Result> local_results = new ArrayList<>();
      for (int interval : intervals_) {
        // normalize the result
        double r = (double) max_itv / interval;
        for (Future<Result> future : local_results_map.get(interval)) {
          Result re = future.get();
          re.setPower_consumption_((re.getPower_consumption_() - prev_power) * r);
          re.setService_mem_sla_vio_((re.getService_lifetime_total_() - prev_mem_sla) * r);
          re.setService_net_sla_vio_((re.getService_net_sla_vio_() - prev_net_sla) * r);
          re.setService_ops_sla_vio_((re.getService_ops_sla_vio_() - prev_cpu_sla) * r);
          re.setMigrations_((re.getMigrations_() -prev_migrations) * r);
          local_results.add(re);
        }
      }
      Collections.sort(local_results);
      System.out.println("--------results from interval + alg combs--------");
      for (Result re : local_results) {
        System.out.println(re);
      }
      current_alg = local_results.get(0).getAlg_();
      current_interval = local_results.get(0).getInterval_();
      System.out.println("EnhancedAlternateWorker: " + " current alg:" + current_alg + ", current interval:" + current_interval);
      workspace_.command("set service-placement-algorithm " + "\"" + current_alg + "\"");
      workspace_.command("repeat " + current_interval + " [ go ]");
      prev_migrations = (double) workspace_.report("sys-migration-event-due-to-consolidation-total") +
                        (double) workspace_.report("sys-migration-event-due-to-auto-migration-total");
      prev_power = (double) workspace_.report("sys-power-consumption-total");
      prev_net_sla = (double) workspace_.report("sys-service-net-sla-vio");
      prev_mem_sla = (double) workspace_.report("sys-service-mem-sla-vio");
      prev_cpu_sla = (double) workspace_.report("sys-service-ops-sla-vio");

    }
    return null;
  }
}
/* each alternate worker alternates using a fixed interval */
class AlternateWorker implements Callable<Result>{
  private HeadlessWorkspace workspace_;
  private int interval_ = -1;
  private final int run_;
  private static String []algs_ = {"balanced-fit", "min-power", "max-utilisation", "first-fit"};
  AlternateWorker(int interval, int run) {
    this.interval_ = interval;
    this.run_ = run;
  }

  @Override
  public Result call() throws Exception {
    workspace_ = HeadlessWorkspace.newInstance();
    workspace_.open("Sunny0-0-1-alpha.nlogo");
    Util.ConfigureS1(workspace_);
    workspace_.command("setup");
    workspace_.exportWorld("intermediate/interval-" + interval_ + "-temp-world.csv");
    List<Worker> workers = new ArrayList<>();
    for (String s : algs_) {
      workers.add(new Worker(s, interval_, run_));
    }
//    System.out.println("alternate worker: worker initialization complete");
    String current_alg;
    ExecutorService es = Executors.newFixedThreadPool(workers.size());

    while (true) {
      if ((boolean) workspace_.report("not any? services")) {
        new Thread(()->{
          try {
            workspace_.exportPlot("Power Consumption","sc2-a/interval" + this.interval_ + "/power/" + "run"+  run_ + ".csv");
            workspace_.exportPlot("SLA Violation (Lifetime Extended)","sc2-a/interval" + this.interval_ + "/sla/" + "run"+  run_ + ".csv");
            workspace_.exportPlot("Avg Resource Utilization(Datacenter)","sc2-a/interval" + this.interval_ + "/avg resource utilisation/" + "run"+  run_ + ".csv");

            File f = new File("sc2-a/interval" + this.interval_ + "/total # of migration due to consolidation.txt");
            FileWriter fw = new FileWriter(f, true);
            fw.write((double) workspace_.report("sys-migration-event-due-to-consolidation-total") + "\n");
            fw.flush();
            f = new File("sc2-a/interval" + this.interval_ + "/total # of migration due to auto migration.txt");
            fw = new FileWriter(f, true);
            fw.write((double) workspace_.report("sys-migration-event-due-to-auto-migration-total") + "\n");
            fw.flush();
            fw.close();
            workspace_.command("ask servers [ set status \"OFF\" set color white set power 0 reset-server self ]");
            es.shutdown();
            workspace_.dispose();
          } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
          }
//
//        workspace_.exportPlot("# of Migration Events triggered by Consolidation","sc2-a/interval" + this.interval_ + "/# of migration by consolidation/" + "run"+  run_ + ".csv");
//        workspace_.exportPlot("# of Migration triggered by Auto Migration","sc2-a/interval" + this.interval_ + "/# of migration by auto migration/" + "run"+  run_ + ".csv");

        }).start();
        break;
      }
      List<Future<Result>> local_futures = new ArrayList<>();

      for (Worker thread: workers) {
        local_futures.add(es.submit(thread));
      }
      for (Future<Result> future : local_futures) {
        future.get();
      }

      List<Result> local_results = new ArrayList<>();
      for (Future<Result> future : local_futures) {
        local_results.add(future.get());
      }

      Collections.sort(local_results);

//      System.out.println("----------Predicted Result of Interval " + round  + "----------");
//      for (Result future : local_results) {
//        System.out.println(future.getAlg_() + ":");
//        System.out.println("power consumption: " + (future.getPower_consumption_() - prev_power));
//        System.out.println("sla violation total: " + (future.getTotal_sla_vio_() - prev_sla_vio));
//        System.out.println("active server resource usage mean: " + (future.getResource_usage_()));
//        System.out.println("--------------------");
//      }
      current_alg = local_results.get(0).getAlg_();
//      System.out.println("interval " + interval_ + " current alg:" + current_alg);
      workspace_.command("set service-placement-algorithm " + "\"" + current_alg + "\"");
      workspace_.command("repeat " + interval_ + " [ go ]");
      workspace_.exportWorld("intermediate/interval-" + interval_ + "-temp-world.csv");
    }

//    Result alt_result = new Result();
//    alt_result.setPower_consumption_((double) workspace.report("sys-power-consumption-total"));
//    alt_result.setService_lifetime_total_((double) workspace.report("sys-service-lifetime-total"));
//    alt_result.setService_mem_sla_vio_((double) workspace.report("sys-service-mem-sla-vio"));
//    alt_result.setService_net_sla_vio_((double) workspace.report("sys-service-net-sla-vio"));
//    alt_result.setService_ops_sla_vio_((double) workspace.report("sys-service-ops-sla-vio"));
//    alt_result.setCpu_avg_usage_((double) workspace.report("exp-avg-cpu-usage"));
//    alt_result.setMem_avg_usage_((double) workspace.report("exp-avg-mem-usage"));
//    alt_result.setNet_avg_usage_((double) workspace.report("exp-avg-net-usage"));
    return null;
  }
}