package com.alvinyho.topographapp;

/**
 * Created by Alistair on 1/9/2016.
 */
public class IMCcolors {
    private double[] current;
    private double[] currentint = {0,0,0};
    private final double[] end;
    private double[] step = {0,0,0};
    public IMCcolors(double start[], double end[], double totalCount) {
        this.current=start;
        this.end=end;
        this.step[0]=(end[0] - start[0]) / totalCount;
        this.step[1]=(end[1] - start[1]) / totalCount;
        this.step[2]=(end[2] - start[2]) / totalCount;

    }
    public boolean hasNext() {
        return current[0] < (end[0] + step[0]/2); //MAY stop floating point error
    }
    public double[] getNextFloat() {
        current[0] += step[0];
        current[1] += step[1];
        current[2] += step[2];
        return current;
    }
}
