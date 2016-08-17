package net.easymodo.asagi.model;

public class UpdateIPs {
    private final int num;
    private final int unique_ips;

    public UpdateIPs(int num, int unique_ips) {
        this.num = num;
        this.unique_ips = unique_ips;
    }

    public int getNum() {
        return num;
    }

    public int getUnique_ips() {
        return unique_ips;
    }
}
