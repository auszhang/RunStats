package com.example.runstats;

import static java.lang.String.valueOf;

public class Time {
    private int hour;
    private int minute;
    private int second;

    public Time(int hours, int minutes, int seconds) {
        this.hour = hours;
        this.minute = minutes;
        this.second = seconds;
    }

    // Get Methods
    public int getHour() {
        return hour;
    }

    public int getMinute() {
        return minute;
    }

    public int getSecond() {
        return second;
    }

    // Set Methods
    public void setHour(int newHour) {
        hour = newHour;
    }

    public void setMinute(int newMinute) {
        minute = newMinute;
    }

    public void setSecond(int newSecond) {
        second = newSecond;
    }

    // Format in x hours, y minutes, and z seconds
    public String stringifyTime() {
        return valueOf(hour) + " hours, " + valueOf(minute) + " minutes, and " + valueOf(second) + " seconds";
    }
}
