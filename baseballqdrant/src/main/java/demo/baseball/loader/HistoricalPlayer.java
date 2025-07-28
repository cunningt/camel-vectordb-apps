package demo.baseball.loader;

import java.util.Arrays;
import java.util.List;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

public class HistoricalPlayer {

    @CsvBindByName(column = "UID")
    @CsvBindByPosition(position = 0)
    private String uid;

    @CsvBindByName(column = "Nameurl")
    @CsvBindByPosition(position = 1)
    private String nameurl;

    @CsvBindByName(column = "Name")
    @CsvBindByPosition(position = 2)
    private String name;

    @CsvBindByName(column = "Year")
    @CsvBindByPosition(position = 3)
    private Integer year;

    @CsvBindByName(column = "Age")
    @CsvBindByPosition(position = 4)
    private Float age;

    @CsvBindByName(column = "League")
    @CsvBindByPosition(position = 5)
    private String league;

    @CsvBindByName(column = "Level")
    @CsvBindByPosition(position = 6)
    private String level;

    @CsvBindByName(column = "PA")
    @CsvBindByPosition(position = 7)
    private Integer pa;

    @CsvBindByName(column = "ISO")
    @CsvBindByPosition(position = 8)
    private Float iso;

    @CsvBindByName(column = "BB%")
    @CsvBindByPosition(position = 9)
    private Float bbPercentage;

    @CsvBindByName(column = "BABIP")
    @CsvBindByPosition(position = 10)
    private Float babip;

    @CsvBindByName(column = "K%")
    @CsvBindByPosition(position = 11)
    private Float kPercentage;

    @CsvBindByName(column = "WAR")
    @CsvBindByPosition(position = 12)
    private Float war;

    @CsvBindByName(column = "WARDEF")
    @CsvBindByPosition(position = 13)
    private Float avg;

    @CsvBindByName(column = "WAROFF")
    @CsvBindByPosition(position = 14)
    private Float waroff;

    @CsvBindByName(column = "WARREP")
    @CsvBindByPosition(position = 15)
    private Float warrep;


    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getNameurl() {
        return nameurl;
    }

    public void setNameurl(String nameurl) {
        this.nameurl = nameurl;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public Float getAge() {
        return age;
    }

    public void setAge(Float age) {
        this.age = age;
    }

    public String getLeague() {
        return league;
    }

    public void setLeague(String league) {
        this.league = league;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Integer getPa() {
        return pa;
    }

    public void setPa(Integer pa) {
        this.pa = pa;
    }

    public Float getIso() {
        return iso;
    }

    public void setIso(Float iso) {
        this.iso = iso;
    }

    public Float getBbPercentage() {
        return bbPercentage;
    }

    public void setBbPercentage(Float bbPercentage) {
        this.bbPercentage = bbPercentage;
    }

    public Float getBabip() {
        return babip;
    }

    public void setBabip(Float babip) {
        this.babip = babip;
    }

    public Float getkPercentage() {
        return kPercentage;
    }

    public void setkPercentage(Float kPercentage) {
        this.kPercentage = kPercentage;
    }

    public Float getWar() {
        return war;
    }

    public void setWar(Float war) {
        this.war = war;
    }

    public Float getAvg() {
        return avg;
    }

    public void setAvg(Float avg) {
        this.avg = avg;
    }

    public Float getWaroff() {
        return waroff;
    }

    public void setWaroff(Float waroff) {
        this.waroff = waroff;
    }

    public Float getWarrep() {
        return warrep;
    }

    public void setWarrep(Float warrep) {
        this.warrep = warrep;
    }

    public List<Float> toVector() {
        return Arrays.asList(age/40, bbPercentage, kPercentage, iso, babip);
    }

    public List toSelectArray() {
        return Arrays.asList(age, bbPercentage, kPercentage, iso, babip);
    }

    public List toArray() {
        return Arrays.asList(name, age, pa, bbPercentage, kPercentage, iso, babip, bbPercentage, kPercentage, war, waroff, warrep);
    }
}
