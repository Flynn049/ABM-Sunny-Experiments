public class Result implements Comparable<Result>{

    public String getAlg_() {
        return alg_;
    }

    public void setAlg_(String alg_) {
        this.alg_ = alg_;
    }

    public double getPower_consumption_() {
        return power_consumption_;
    }

    public void setPower_consumption_(double power_consumption_) {
        this.power_consumption_ = power_consumption_;
    }

    public double getService_lifetime_total_() {
        return service_lifetime_total_;
    }

    public void setService_lifetime_total_(double service_lifetime_total_) {
        this.service_lifetime_total_ = service_lifetime_total_;
    }

    public double getService_ops_sla_vio_() {
        return service_ops_sla_vio_;
    }

    public void setService_ops_sla_vio_(double service_ops_sla_vio_) {
        this.service_ops_sla_vio_ = service_ops_sla_vio_;
    }

    public double getService_mem_sla_vio_() {
        return service_mem_sla_vio_;
    }

    public void setService_mem_sla_vio_(double service_mem_sla_vio_) {
        this.service_mem_sla_vio_ = service_mem_sla_vio_;
    }

    public double getService_net_sla_vio_() {
        return service_net_sla_vio_;
    }

    public void setService_net_sla_vio_(double service_net_sla_vio_) {
        this.service_net_sla_vio_ = service_net_sla_vio_;
    }

    public double getTotal_sla_vio_() {
        return service_mem_sla_vio_ + service_net_sla_vio_ + service_ops_sla_vio_;
    }

    public double getCpu_avg_usage_() {
        return cpu_avg_usage_;
    }

    public void setCpu_avg_usage_(double cpu_avg_usage_) {
        this.cpu_avg_usage_ = cpu_avg_usage_;
    }

    public double getMem_avg_usage_() {
        return mem_avg_usage_;
    }

    public void setMem_avg_usage_(double mem_avg_usage_) {
        this.mem_avg_usage_ = mem_avg_usage_;
    }

    public double getNet_avg_usage_() {
        return net_avg_usage_;
    }

    public void setNet_avg_usage_(double net_avg_usage_) {
        this.net_avg_usage_ = net_avg_usage_;
    }
    public double getResource_usage_() {
        return (this.cpu_avg_usage_ + this.mem_avg_usage_ + this.net_avg_usage_) / 3;
    }

    public int getInterval_() {
        return interval_;
    }

    public void setInterval_(int interval_) {
        this.interval_ = interval_;
    }
    @Override
    public String toString() {
        String s = String.format("{power: %f, sla: %f, ru: %f, interval: %d, alg: %s}", getPower_consumption_(), getTotal_sla_vio_(), getResource_usage_(), interval_, alg_);
        return s;
    }

    private String alg_;
    private double power_consumption_;
    private double service_lifetime_total_;
    private double service_ops_sla_vio_;
    private double service_mem_sla_vio_;
    private double service_net_sla_vio_;

    private double cpu_avg_usage_;
    private double mem_avg_usage_;
    private double net_avg_usage_;

    public double getMigrations_() {
        return migrations_;
    }

    public void setMigrations_(double migrations_) {
        this.migrations_ = migrations_;
    }

    private int interval_;
    private double migrations_;

    @Override
    public int compareTo(Result o) {
        double res = this.getPower_consumption_() - o.getPower_consumption_();
        double epsilon_ = 0.00001f;
        // a = b
        if (res >= 0 && res <= epsilon_) {
            res = this.getTotal_sla_vio_() - o.getTotal_sla_vio_();
            if (res >= 0 && res <= epsilon_) {
                res = this.getResource_usage_() - o.getResource_usage_();
                if (res >= 0 && res <= epsilon_) {
                    res = this.migrations_ - o.migrations_;
                    if (res >= 0 && res <= epsilon_) {
                        return 0;
                    }
                    if (res > epsilon_) {
                        return 1;
                    }
                    return -1;
                }
                if (res > epsilon_) {
                    return  1;
                }
                return -1;
            }
            if (res > epsilon_) {
                return 1;
            }
            return -1;
        }
        // a > b
        if (res > epsilon_) {
            return 1;
        }
        // a < b
        return -1;
    }
}
