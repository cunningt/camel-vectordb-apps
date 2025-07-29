package demo.baseball.loader;

import java.util.Arrays;
import java.util.List;

import com.opencsv.bean.CsvBindByName;
import com.opencsv.bean.CsvBindByPosition;

public class FangraphsPlayer {

    @CsvBindByName(column = "Name")
    @CsvBindByPosition(position = 0)
    private String name;

    @CsvBindByName(column = "Team")
    @CsvBindByPosition(position = 1)
    private String team;

    @CsvBindByName(column = "Level")
    @CsvBindByPosition(position = 2)
    private String level;

    @CsvBindByName(column = "Age")
    @CsvBindByPosition(position = 3)
    private Float age;

    @CsvBindByName(column = "PA")
    @CsvBindByPosition(position = 4)
    private Integer pa;

    @CsvBindByName(column = "BB%")
    @CsvBindByPosition(position = 5)
    private Float bbPercentage;

    @CsvBindByName(column = "K%")
    @CsvBindByPosition(position = 6)
    private Float kPercentage;

    @CsvBindByName(column = "BB/K")
    @CsvBindByPosition(position = 7)
    private Float bbToK;

    @CsvBindByName(column = "AVG")
    @CsvBindByPosition(position = 8)
    private Float avg;

    @CsvBindByName(column = "OBP")
    @CsvBindByPosition(position = 9)
    private Float obp;

    @CsvBindByName(column = "SLG")
    @CsvBindByPosition(position = 10)
    private Float slg;

    @CsvBindByName(column = "OPS")
    @CsvBindByPosition(position = 11)
    private Float ops;

    @CsvBindByName(column = "ISO")
    @CsvBindByPosition(position = 12)
    private Float iso;

    @CsvBindByName(column = "SPD")
    @CsvBindByPosition(position = 13)
    private Float spd;

    @CsvBindByName(column = "BABIP")
    @CsvBindByPosition(position = 14)
    private Float babip;

    @CsvBindByName(column = "wSB")
    @CsvBindByPosition(position = 15)
    private Float wsb;

    @CsvBindByName(column = "wRC")
    @CsvBindByPosition(position = 16)
    private Float wrc;

    @CsvBindByName(column = "wRAA")
    @CsvBindByPosition(position = 17)
    private Float wraa;

    @CsvBindByName(column = "wOBA")
    @CsvBindByPosition(position = 18)
    private Float woba;

    @CsvBindByName(column = "wRC+")
    @CsvBindByPosition(position = 19)
    private Float wrcPlus;

    @CsvBindByName(column = "PlayerID")
    @CsvBindByPosition(position = 20)
    private String playerID;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTeam() {
        return team;
    }

    public void setTeam(String team) {
        this.team = team;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public Float getAge() {
        return age;
    }

    public void setAge(Float age) {
        this.age = age;
    }

    public Integer getPa() {
        return pa;
    }

    public void setPa(Integer pa) {
        this.pa = pa;
    }

    public Float getBbPercentage() {
        return bbPercentage;
    }

    public void setBbPercentage(Float bbPercentage) {
        this.bbPercentage = bbPercentage;
    }

    public Float getkPercentage() {
        return kPercentage;
    }

    public void setkPercentage(Float kPercentage) {
        this.kPercentage = kPercentage;
    }

    public Float getBbToK() {
        return bbToK;
    }

    public void setBbToK(Float bbToK) {
        this.bbToK = bbToK;
    }

    public Float getAvg() {
        return avg;
    }

    public void setAvg(Float avg) {
        this.avg = avg;
    }

    public Float getObp() {
        return obp;
    }

    public void setObp(Float obp) {
        this.obp = obp;
    }

    public Float getSlg() {
        return slg;
    }

    public void setSlg(Float slg) {
        this.slg = slg;
    }

    public Float getOps() {
        return ops;
    }

    public void setOps(Float ops) {
        this.ops = ops;
    }

    public Float getIso() {
        return iso;
    }

    public void setIso(Float iso) {
        this.iso = iso;
    }

    public Float getSpd() {
        return spd;
    }

    public void setSpd(Float spd) {
        this.spd = spd;
    }

    public Float getBabip() {
        return babip;
    }

    public void setBabip(Float babip) {
        this.babip = babip;
    }

    public Float getWsb() {
        return wsb;
    }

    public void setWsb(Float wsb) {
        this.wsb = wsb;
    }

    public Float getWrc() {
        return wrc;
    }

    public void setWrc(Float wrc) {
        this.wrc = wrc;
    }

    public Float getWraa() {
        return wraa;
    }

    public void setWraa(Float wraa) {
        this.wraa = wraa;
    }

    public Float getWoba() {
        return woba;
    }

    public void setWoba(Float woba) {
        this.woba = woba;
    }

    public Float getWrcPlus() {
        return wrcPlus;
    }

    public void setWrcPlus(Float wrcPlus) {
        this.wrcPlus = wrcPlus;
    }

    public String getPlayerID() {
        return playerID;
    }

    public void setPlayerID(String playerID) {
        this.playerID = playerID;
    }

    public List<Float> toVector() {
        return Arrays.asList(age, bbPercentage, kPercentage, iso, babip);
    }

    public List toSelectArray() {
        return Arrays.asList(age, bbPercentage, kPercentage, iso, babip);
    }

    public List toArray() {
        return Arrays.asList(age, pa, bbPercentage, kPercentage, bbToK, avg, obp, slg, ops, iso, spd, babip, wsb, wrc, wraa, woba, wrcPlus);
    }
}
