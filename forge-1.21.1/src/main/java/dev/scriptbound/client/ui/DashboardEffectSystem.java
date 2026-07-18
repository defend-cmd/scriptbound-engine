package dev.scriptbound.client.ui;

import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class DashboardEffectSystem {
    private final List<EffectParticle> particles = new ArrayList<>();
    private final Random random = new Random();
    private long lastTime = 0;

    public void updateAndRender(GuiGraphics graphics, int width, int height) {
        if (!UiConfig.visualEffects) {
            if (!particles.isEmpty()) particles.clear();
            return;
        }

        String t = UiConfig.theme;
        boolean hasParticles = false;
        long time = System.currentTimeMillis();
        float dt = (lastTime == 0) ? 0.016f : (time - lastTime) / 1000f;
        lastTime = time;

        if (t.equals("sakura+")) {
            hasParticles = true;
            spawnParticles(width, height, 2, "sakura", 80);
        } else if (t.equals("winter+")) {
            hasParticles = true;
            spawnParticles(width, height, 3, "snow", 150);
            renderVignette(graphics, width, height, 0x22FFFFFF);
        } else if (t.equals("autumn+")) {
            hasParticles = true;
            spawnParticles(width, height, 2, "leaf", 60);
        } else if (t.equals("forest+")) {
            hasParticles = true;
            spawnParticles(width, height, 1, "firefly", 40);
        } else if (t.equals("ocean+")) {
            hasParticles = true;
            spawnParticles(width, height, 2, "bubble", 60);
        } else if (t.equals("space+")) {
            hasParticles = true;
            spawnParticles(width, height, 1, "star", 200);
        } else if (t.equals("void+")) {
            hasParticles = true;
            spawnParticles(width, height, 1, "void", 50);
        } else if (t.equals("fire+") || t.equals("nether+")) {
            hasParticles = true;
            spawnParticles(width, height, 3, "ember", 100);
        } else if (t.equals("storm+")) {
            hasParticles = true;
            spawnParticles(width, height, 8, "rain", 200);
            if (random.nextInt(400) == 0) {
                graphics.fill(0, 0, width, height, 0x44FFFFFF);
            }
        } else if (t.equals("ender+") || t.equals("amethyst+")) {
            hasParticles = true;
            spawnParticles(width, height, 2, "sparkle", 80);
        } else if (t.equals("minecraft+")) {
            hasParticles = true;
            spawnParticles(width, height, 2, "pixel", 50);
        } else if (t.equals("redstone+")) {
            hasParticles = true;
            spawnParticles(width, height, 2, "redstone", 60);
        } else if (t.equals("cyber+") || t.equals("terminal+")) {
            renderScanlines(graphics, width, height, time);
        } else if (t.equals("liminal+")) {
            if (random.nextInt(100) == 0) {
                graphics.fill(0, 0, width, height, 0x11000000);
            }
        } else if (t.equals("spring_legacy")) {
            hasParticles = true;
            spawnParticles(width, height, 2, "leaf", 60);
        }

        if (!hasParticles && !particles.isEmpty()) {
            particles.clear();
        }

        if (hasParticles) {
            RenderSystem.enableBlend();
            RenderSystem.defaultBlendFunc();

            for (int i = particles.size() - 1; i >= 0; i--) {
                EffectParticle p = particles.get(i);
                updateParticle(p, dt, time, width, height);
                if (p.dead) {
                    particles.remove(i);
                } else {
                    renderParticle(graphics, p, time);
                }
            }

            RenderSystem.disableBlend();
        }
    }

    private void spawnParticles(int width, int height, int spawnRate, String type, int max) {
        if (particles.size() < max && random.nextInt(100) < spawnRate * 5) {
            particles.add(new EffectParticle(width, height, type, random));
        }
    }

    private void renderScanlines(GuiGraphics graphics, int width, int height, long time) {
        int color = UiConfig.theme.equals("cyber+") ? 0x1100FFFF : 0x1100FF00;
        int offset = (int) ((time / 50) % 4);
        for (int y = offset; y < height; y += 4) {
            graphics.fill(0, y, width, y + 1, color);
        }
    }

    private void renderVignette(GuiGraphics graphics, int width, int height, int color) {

        graphics.fill(0, 0, width, 20, color);
        graphics.fill(0, height - 20, width, height, color);
        graphics.fill(0, 20, 20, height - 20, color);
        graphics.fill(width - 20, 20, width, height - 20, color);
    }

    private void updateParticle(EffectParticle p, float dt, long time, int width, int height) {
        switch (p.type) {
            case "sakura":
            case "leaf":
            case "snow":
                p.y += p.speed * dt * 60;
                p.x += Math.sin((time + p.seed) * p.swaySpeed) * p.swayAmount;
                if (p.y > height + 10) p.dead = true;
                break;
            case "rain":
                p.y += p.speed * dt * 200;
                p.x -= p.speed * dt * 20;
                if (p.y > height + 20) p.dead = true;
                break;
            case "bubble":
            case "ember":
            case "redstone":
                p.y -= p.speed * dt * 40;
                p.x += Math.sin((time + p.seed) * p.swaySpeed) * p.swayAmount;
                if (p.y < -10) p.dead = true;
                break;
            case "firefly":
            case "sparkle":
            case "void":
                p.x += Math.cos((time + p.seed) * p.swaySpeed) * p.speed * dt * 20;
                p.y += Math.sin((time + p.seed) * p.swaySpeed) * p.speed * dt * 20;
                p.life -= dt;
                if (p.life <= 0) p.dead = true;
                break;
            case "star":
            case "pixel":
                p.y -= p.speed * dt * 10;
                if (p.y < -10) p.dead = true;
                break;
        }
    }

    private void renderParticle(GuiGraphics graphics, EffectParticle p, long time) {
        int color = 0xFFFFFFFF;
        int size = p.size;

        switch (p.type) {
            case "sakura": color = 0xFFFFB7C5; break;
            case "snow": color = 0xAAFFFFFF; break;
            case "leaf": color = (p.seed % 2 == 0) ? 0xFFD2691E : 0xFF8B4513; break;
            case "firefly": color = 0x88AAFF88; break;
            case "bubble": color = 0x6600E5FF; break;
            case "ember": color = (p.seed % 2 == 0) ? 0xFFFF4500 : 0xFFFF8C00; break;
            case "rain": color = 0x4488AADD; size = 2; break;
            case "sparkle": color = 0xAAEE88FF; break;
            case "void": color = 0x44110022; break;
            case "star": color = 0x88FFFFFF; break;
            case "redstone": color = 0xAAFF0000; break;
            case "pixel": color = 0x44FFFFFF; break;
        }

        if (p.type.equals("rain")) {
            graphics.fill((int)p.x, (int)p.y, (int)p.x + 1, (int)p.y + p.size * 4, color);
        } else if (p.type.equals("pixel")) {
            graphics.fill((int)p.x, (int)p.y, (int)p.x + size * 2, (int)p.y + size * 2, color);
        } else {

            graphics.fill((int)p.x, (int)p.y, (int)p.x + size, (int)p.y + size, color);
        }
    }

    private static class EffectParticle {
        float x, y;
        float speed;
        float swaySpeed, swayAmount;
        int size;
        long seed;
        String type;
        boolean dead = false;
        float life = 10f;

        EffectParticle(int width, int height, String type, Random random) {
            this.type = type;
            this.seed = random.nextLong();
            this.size = 1 + random.nextInt(3);

            if (type.equals("bubble") || type.equals("ember") || type.equals("redstone")) {
                this.x = random.nextInt(width);
                this.y = height + 10;
                this.speed = 0.5f + random.nextFloat() * 1.5f;
            } else if (type.equals("firefly") || type.equals("sparkle") || type.equals("void")) {
                this.x = random.nextInt(width);
                this.y = random.nextInt(height);
                this.speed = 0.2f + random.nextFloat();
                this.life = 5f + random.nextFloat() * 10f;
            } else if (type.equals("star") || type.equals("pixel")) {
                this.x = random.nextInt(width);
                this.y = height + 10;
                this.speed = 0.1f + random.nextFloat() * 0.5f;
            } else {

                this.x = random.nextInt(width);
                this.y = -20;
                this.speed = 0.5f + random.nextFloat() * 1.5f;
                if (type.equals("rain")) this.speed *= 3;
            }

            this.swaySpeed = 0.001f + random.nextFloat() * 0.002f;
            this.swayAmount = 0.5f + random.nextFloat() * 1.5f;
        }
    }
}
