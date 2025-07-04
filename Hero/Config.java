package Hero;

public class Config{

    public static final double HP_DANGER_THRESHOLD = 30.0; 
    public static final double HP_RETREAT_THRESHOLD = 20.0; 
    public static final double HP_MEDIUM_THRESHOLD = 50; 
    public static final int SAFE_DISTANCE_FROM_ENEMIES = 3;
    public static final int MAX_CHASE_DISTANCE = 100;
    
    // === SCORING WEIGHTS ===
    public static final double LOOT_VALUE_WEIGHT = 1.0;
    public static final double THREAT_WEIGHT = 2.0;
    public static final double ZONE_RISK_WEIGHT = 3.0;
    public static final double OPPORTUNITY_WEIGHT = 1.5;
    public static final double SURVIVAL_WEIGHT = 4.0;

    public static final double EARLY_PHASE_THRESHOLD = 0.67; 
    public static final double LATE_PHASE_THRESHOLD = 0.075; 
    
    // === ITEM PRIORITIES ===
    public static final int GUN_PRIORITY = 100;
    public static final int HEALING_PRIORITY = 100;
    public static final int SPECIAL_WEAPON_PRIORITY = 70;
    public static final int THROWABLE_PRIORITY = 90;
    public static final int MELEE_PRIORITY = 50;
    public static final int DRAGON_EGG_PRIORITY = 100;
    public static final int CHEST_PRIORITY = 70;
    
    // === COMBAT SETTINGS ===
    public static final int MIN_STREAK_FOR_CAUTION = 3; 
    public static final double CRIT_HP_TARGET = 30.0; 
    public static final double LOW_HP_TARGET = 50.0; 
    public static final double MED_HP_TARGET = 70.0; 
    
    // === ZONE MANAGEMENT ===
    public static final int ZONE_BUFFER_DISTANCE = 2; 
    
    public static int calculateStreakBonus(int streak) {
        if (streak <= 1) return 0;
        int bonus = 0;
        for (int i = 0; i < streak - 1; i++) {
            bonus += 20 * i;
        }
        return bonus;
    }

    public static int calculateZoneDamage(int timeRemaining, int totalTime) {
        if (timeRemaining >= (2.0/3.0) * totalTime) {
            return 5;
        }
        return (int) Math.ceil(5 + (totalTime/3.0 - timeRemaining) / 10.0);
    }
}
