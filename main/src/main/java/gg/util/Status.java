package gg.util;

public enum Status{
    warm(3), cold(2), ferm(1), unkn(0);

    private int i;

    Status(int i) {
        this.i = i;
    }

    public int encode(){
        return i;
    }
}