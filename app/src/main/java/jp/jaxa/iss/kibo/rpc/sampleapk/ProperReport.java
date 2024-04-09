package jp.jaxa.iss.kibo.rpc.sampleapk;

public class ProperReport {
    public String sex(String b){
        if(b == "tape" || b == "Tape")
            return "kapton_tape";
        if(b == "skrewdriver")
            return "screwdriver";
        if(b == "dropper")
            return "pipette";
        if(b == "glasses")
            return "goggle";
        return b;
    }
}
