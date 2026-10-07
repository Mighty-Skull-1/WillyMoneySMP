package me.moneysmp.managers;

import me.moneysmp.models.SMPPhase;
import org.bukkit.Bukkit;
import org.bukkit.Sound;
import org.bukkit.entity.Player;

public class PhaseManager {
    private SMPPhase currentPhase = SMPPhase.DRAFT;

    public SMPPhase getCurrentPhase() { return currentPhase; }

    public void setPhase(SMPPhase phase) {
        this.currentPhase = phase;
        for (Player p : Bukkit.getOnlinePlayers()) {
            p.sendTitle("§6§lSMP PHASE CHANGED", phase.getDisplayName(), 10, 80, 20);
            p.playSound(p.getLocation(), Sound.UI_TOAST_CHALLENGE_COMPLETE, 1.0f, 1.0f);
        }
        Bukkit.broadcastMessage("§6[Willy Money SMP] Tournament phase updated to: " + phase.getDisplayName());
    }

    public void setPhaseSilently(SMPPhase phase) {
        if (phase != null) this.currentPhase = phase;
    }

    public boolean isPvPAllowed() {
        return currentPhase == SMPPhase.PVP || currentPhase == SMPPhase.FINALE;
    }
}
