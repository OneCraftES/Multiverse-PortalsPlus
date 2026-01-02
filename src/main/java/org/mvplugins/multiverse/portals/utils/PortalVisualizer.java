package org.mvplugins.multiverse.portals.utils;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Vehicle;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.util.Vector;
import org.bukkit.World;
import org.jvnet.hk2.annotations.Service;
import org.mvplugins.multiverse.core.destination.DestinationInstance;
import org.mvplugins.multiverse.external.jakarta.inject.Inject;
import org.mvplugins.multiverse.portals.MVPortal;
import org.mvplugins.multiverse.portals.MultiversePortals;

@Service
public class PortalVisualizer {

    private final MultiversePortals plugin;
    private final Map<MVPortal, BukkitTask> activeVisualizers;
    private final Map<MVPortal, Double> currentAngles; // Keep track of angle per portal
    private final Map<MVPortal, Color> portalColors;
    private final Map<MVPortal, Long> colorResetTimes;
    private final Map<MVPortal, String[]> dynamicDefaults;

    // Default Colors (fallback)
    private static final String DEFAULT_PRIMARY = "#4287F5";
    private static final String DEFAULT_SECONDARY = "#42F5D1";

    @Inject
    public PortalVisualizer(MultiversePortals plugin) {
        this.plugin = plugin;
        this.activeVisualizers = new HashMap<>();
        this.currentAngles = new HashMap<>();
        this.portalColors = new HashMap<>();
        this.colorResetTimes = new HashMap<>();
        this.dynamicDefaults = new HashMap<>();
    }

    public void startVisualizer(final MVPortal portal) {
        if (this.activeVisualizers.containsKey(portal)) {
            return;
        }
        if (!portal.getParticlesEnabled()) {
            return;
        }

        this.plugin.getLogger().info("Starting visualizer for portal: " + portal.getName());
        this.currentAngles.put(portal, 0.0);

        // Resolve dynamic colors
        String[] defaults = { DEFAULT_PRIMARY, DEFAULT_SECONDARY };
        DestinationInstance<?, ?> dest = portal.getDestination();
        if (dest != null) {
            // Use a default location if specific entity context not available (usually fine
            // for general world detection)
            Location loc = dest.getLocation(null).getOrNull();
            if (loc != null && loc.getWorld() != null) {
                World.Environment env = loc.getWorld().getEnvironment();
                if (env == World.Environment.NETHER) {
                    defaults = new String[] { "#FF0000", "#FF8C00" }; // Red, Dark Orange
                } else if (env == World.Environment.THE_END) {
                    defaults = new String[] { "#800080", "#00FF00" }; // Purple, Lime
                }
            }
        }
        this.dynamicDefaults.put(portal, defaults);

        BukkitTask task = new BukkitRunnable() {
            @Override
            public void run() {
                if (!portal.getPortalLocation().isValidLocation() || !portal.getParticlesEnabled()) {
                    stopVisualizer(portal);
                    return;
                }

                Long resetTime = colorResetTimes.get(portal);
                if (resetTime != null && System.currentTimeMillis() > resetTime) {
                    // Reset color
                    portalColors.remove(portal);
                    colorResetTimes.remove(portal);
                }

                displayCircleEffect(portal);

                double angle = currentAngles.getOrDefault(portal, 0.0);
                angle += 0.10471975511965977; // roughly PI/30
                if (angle >= Math.PI * 2) {
                    angle = 0.0;
                }
                currentAngles.put(portal, angle);
            }
        }.runTaskTimer(this.plugin, 0L, 1L);

        this.activeVisualizers.put(portal, task);
    }

    public void stopVisualizer(MVPortal portal) {
        BukkitTask task = this.activeVisualizers.remove(portal);
        if (task != null) {
            task.cancel();
            this.currentAngles.remove(portal);
            this.portalColors.remove(portal);
            this.colorResetTimes.remove(portal);
            this.dynamicDefaults.remove(portal);
        }
    }

    public void refreshVisualizer(MVPortal portal) {
        stopVisualizer(portal);
        startVisualizer(portal);
    }

    private void displayCircleEffect(MVPortal portal) {
        MultiverseRegion region = portal.getPortalLocation().getRegion();
        if (region == null)
            return;

        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();
        double heightOffset = calculateHeightOffset(min, max);

        Location center = new Location(region.getWorld().getBukkitWorld().getOrNull(),
                min.getX() + (region.getWidth() / 2.0),
                min.getY(),
                min.getZ() + (region.getDepth() / 2.0));
        center.add(0.0, heightOffset, 0.0);

        String[] defaults = this.dynamicDefaults.getOrDefault(portal,
                new String[] { DEFAULT_PRIMARY, DEFAULT_SECONDARY });
        String primaryHex = portal.getParticleColorPrimary();
        String secondaryHex = portal.getParticleColorSecondary();

        // If config is default, use dynamic default
        if (DEFAULT_PRIMARY.equals(primaryHex))
            primaryHex = defaults[0];
        if (DEFAULT_SECONDARY.equals(secondaryHex))
            secondaryHex = defaults[1];

        Color colorPrimary = parseColor(primaryHex, DEFAULT_PRIMARY);
        Color colorSecondary = parseColor(secondaryHex, DEFAULT_SECONDARY);
        Color currentColor = this.portalColors.getOrDefault(portal, colorPrimary);

        int particleCount = portal.getParticleCircleCount();
        double circleRadius = portal.getParticleCircleRadius();
        double particleSize = portal.getParticleCircleSize();
        double currentAngle = this.currentAngles.getOrDefault(portal, 0.0);

        for (int i = 0; i < particleCount; ++i) {
            double angle = currentAngle + Math.PI * 2 * (double) i / (double) particleCount;
            double x = circleRadius * Math.cos(angle);
            double z = circleRadius * Math.sin(angle);
            Location particleLoc = center.clone().add(x, 0.0, z);

            float progress = (float) (angle / (Math.PI * 2));
            // Ensure progress is 0-1
            while (progress > 1.0f)
                progress -= 1.0f;
            while (progress < 0.0f)
                progress += 1.0f;

            Color transitionColor = interpolateColor(currentColor, colorSecondary, progress);
            Particle.DustOptions dustOptions = new Particle.DustOptions(transitionColor, (float) particleSize);
            particleLoc.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 1, 0.0, 0.0, 0.0, 0.0, dustOptions);
        }
    }

    public void displayReactiveEffect(Entity entity, MVPortal portal) {
        if (!portal.getParticlesEnabled())
            return;

        MultiverseRegion region = portal.getPortalLocation().getRegion();
        if (region == null)
            return;

        Vector min = region.getMinimumPoint();
        Vector max = region.getMaximumPoint();
        Location center = new Location(region.getWorld().getBukkitWorld().getOrNull(),
                min.getX() + (region.getWidth() / 2.0),
                min.getY() + calculateHeightOffset(min, max),
                min.getZ() + (region.getDepth() / 2.0));

        Color color = getEntityColor(entity, portal);
        this.portalColors.put(portal, color);
        this.colorResetTimes.put(portal, System.currentTimeMillis() + 2000L);

        double circleRadius = portal.getParticleCircleRadius();
        double particleSize = portal.getParticleCircleSize();

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 10;

            @Override
            public void run() {
                if (this.ticks >= maxTicks) {
                    this.cancel();
                    return;
                }
                double progress = (double) this.ticks / 10.0;
                double radius = circleRadius * progress;

                for (int i = 0; i < 16; ++i) {
                    double angle = (double) this.ticks * 0.5 + (double) i * Math.PI * 2.0 / 16.0;
                    double x = radius * Math.cos(angle);
                    double y = progress * 0.5;
                    double z = radius * Math.sin(angle);
                    Location particleLoc = center.clone().add(x, y, z);
                    Particle.DustOptions dustOptions = new Particle.DustOptions(color, (float) (particleSize * 1.5));
                    particleLoc.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 1, 0.0, 0.0, 0.0, 0.0,
                            dustOptions);
                }
                ++this.ticks;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    public void displayDestinationEffect(Location destination, Entity sourceEntity, MVPortal portal) {
        if (!portal.getParticlesEnabled() || destination == null)
            return;

        Color color = getEntityColor(sourceEntity, portal);
        double particleSize = portal.getParticleCircleSize();

        new BukkitRunnable() {
            int ticks = 0;
            final int maxTicks = 40;

            @Override
            public void run() {
                if (this.ticks >= maxTicks) {
                    this.cancel();
                    return;
                }
                double progress = (double) this.ticks / 40.0;
                double radius = 1.0 * (1.0 - progress * 0.5);

                for (int i = 0; i < 8; ++i) {
                    double angle = (double) this.ticks * 0.5 + (double) i * Math.PI * 2.0 / 8.0;
                    double x = radius * Math.cos(angle);
                    double y = progress * 2.0;
                    double z = radius * Math.sin(angle);
                    Location particleLoc = destination.clone().add(x, y, z);
                    Particle.DustOptions dustOptions = new Particle.DustOptions(color, (float) (particleSize * 1.2));
                    particleLoc.getWorld().spawnParticle(Particle.REDSTONE, particleLoc, 1, 0.0, 0.0, 0.0, 0.0,
                            dustOptions);
                }
                ++this.ticks;
            }
        }.runTaskTimer(this.plugin, 0L, 1L);
    }

    public void cleanup() {
        this.activeVisualizers.values().forEach(BukkitTask::cancel);
        this.activeVisualizers.clear();
        this.currentAngles.clear();
        this.portalColors.clear();
        this.colorResetTimes.clear();
    }

    // Helpers

    private Color getEntityColor(Entity entity, MVPortal portal) {
        if (entity instanceof Player) {
            return parseColor(portal.getParticleColorPlayer(), "#42F55F");
        }
        if ("ENDER_PEARL".equals(entity.getType().name())) {
            return parseColor(portal.getParticleColorPearl(), "#8842F5");
        }
        if (entity instanceof Vehicle) {
            return parseColor(portal.getParticleColorVehicle(), "#F542A1");
        }
        return parseColor(portal.getParticleColorEntity(), "#F5A142");
    }

    private Color parseColor(String hex, String defaultHex) {
        try {
            if (hex == null || hex.isEmpty())
                hex = defaultHex;
            if (hex.startsWith("#"))
                hex = hex.substring(1);
            int rgb = Integer.parseInt(hex, 16);
            return Color.fromRGB(rgb);
        } catch (IllegalArgumentException e) {
            // Fallback
            return Color.fromRGB(Integer.parseInt(defaultHex.substring(1), 16));
        }
    }

    private Color interpolateColor(Color c1, Color c2, float factor) {
        int r = (int) (c1.getRed() + (c2.getRed() - c1.getRed()) * factor);
        int g = (int) (c1.getGreen() + (c2.getGreen() - c1.getGreen()) * factor);
        int b = (int) (c1.getBlue() + (c2.getBlue() - c1.getBlue()) * factor);
        return Color.fromRGB(Math.min(255, Math.max(0, r)), Math.min(255, Math.max(0, g)),
                Math.min(255, Math.max(0, b)));
    }

    private double calculateHeightOffset(Vector min, Vector max) {
        double width = Math.abs(max.getX() - min.getX());
        double height = Math.abs(max.getY() - min.getY());
        double length = Math.abs(max.getZ() - min.getZ());
        if (width > height || length > height) {
            return 2.0;
        }
        return height / 2.0;
    }
}
