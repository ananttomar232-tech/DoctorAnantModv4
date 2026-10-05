package com.dranant.clinic;

import it.unimi.dsi.fastutil.ints.IntList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CrossCollisionBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Animated construction engine (ticked from ModGameEvents#onServerTick):
 * <ul>
 *   <li>blueprints rise bottom-up with sounds/particles, then fences, walls, panes and stairs auto-connect;</li>
 *   <li>structure templates are placed one horizontal slice at a time;</li>
 *   <li>streams of tasks (a whole city: terrain levelling + hundreds of pieces) run one after another;</li>
 *   <li>erase tasks remove a building top-down with poofs and restore the lawn.</li>
 * </ul>
 */
public final class ConstructionManager {
    public interface Task {
        /** @return true when finished */
        boolean tick(ServerLevel level);

        ResourceKey<Level> dimension();
    }

    private static final List<Task> TASKS = new ArrayList<>();

    public static synchronized void tick(MinecraftServer server) {
        List<Task> snapshot = new ArrayList<>(TASKS);
        for (Task t : snapshot) {
            ServerLevel level = server.getLevel(t.dimension());
            if (level == null || t.tick(level)) TASKS.remove(t);
        }
    }

    public static synchronized void add(Task task) {
        TASKS.add(task);
    }

    public static synchronized boolean isBusy() {
        return !TASKS.isEmpty();
    }

    public static synchronized void clearAll() {
        TASKS.clear();
    }

    // ================================================================== public builders
    /** Animated placement of a blueprint (bottom-up) with fireworks at the end. */
    public static void build(ServerLevel level, Blueprint blueprint, BlockPos fxCenter) {
        add(blueprintTask(level, blueprint, fxCenter, true, 0));
        level.playSound(null, fxCenter, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    /** Creates (without scheduling) a blueprint task; {@code perTick <= 0} = automatic ~3 second build. */
    public static Task blueprintTask(ServerLevel level, Blueprint blueprint, BlockPos fxCenter, boolean finale, int perTick) {
        if (blueprint.clearBox() != null) BuildHelper.clear(level, blueprint.clearBox());
        List<Map.Entry<BlockPos, BlockState>> entries = new ArrayList<>(blueprint.blocks().entrySet());
        entries.sort(Comparator.comparingInt(e -> e.getKey().getY()));
        int budget = perTick > 0 ? perTick : Math.max(30, entries.size() / 60);
        return new BlueprintTask(level.dimension(), entries, blueprint.afterBuild(), fxCenter, budget, finale);
    }

    /** Animated placement of a structure template, one horizontal slice at a time. */
    public static void buildTemplate(ServerLevel level, StructureTemplate template, StructurePlaceSettings settings,
                                     BlockPos origin, BoundingBox box, List<Consumer<ServerLevel>> after, BlockPos fxCenter) {
        add(new TemplateTask(level.dimension(), template, settings, origin, box, after, fxCenter));
        level.playSound(null, fxCenter, SoundEvents.BEACON_ACTIVATE, SoundSource.BLOCKS, 1.0F, 1.2F);
    }

    /** Runs lazily-created tasks one after another (used for the whole city). */
    public static void stream(ServerLevel level, List<Supplier<Task>> stages, Consumer<Float> progress, Consumer<ServerLevel> done) {
        add(new StreamTask(level.dimension(), stages, progress, done));
    }

    /** Levels a rectangle: clears everything above {@code floorY} (up to +clearHeight) and lays grass on a dirt base. */
    public static Task terrainTask(ServerLevel level, BoundingBox area, int floorY, int clearHeight) {
        return new TerrainTask(level.dimension(), area, floorY, clearHeight);
    }

    /** Erases a building top-down, then restores a lawn at {@code floorY}. */
    public static void erase(ServerLevel level, BoundingBox box, int floorY, BlockState floor, Consumer<ServerLevel> after) {
        add(new EraseTask(level.dimension(), box, floorY, floor, after));
        level.playSound(null, new BlockPos(box.getCenter().getX(), floorY, box.getCenter().getZ()), SoundEvents.WITHER_BREAK_BLOCK, SoundSource.BLOCKS, 1.0F, 0.8F);
    }

    // ================================================================== tasks
    private static boolean needsShapeUpdate(BlockState s) {
        Block b = s.getBlock();
        return b instanceof CrossCollisionBlock || b instanceof WallBlock || b instanceof StairBlock;
    }

    private static final class BlueprintTask implements Task {
        private final ResourceKey<Level> dim;
        private final List<Map.Entry<BlockPos, BlockState>> entries;
        private final List<Consumer<ServerLevel>> after;
        private final BlockPos center;
        private final int perTick;
        private final boolean finale;
        private final List<BlockPos> shapeFix = new ArrayList<>();
        private int index;
        private int ticks;

        BlueprintTask(ResourceKey<Level> dim, List<Map.Entry<BlockPos, BlockState>> entries, List<Consumer<ServerLevel>> after,
                      BlockPos center, int perTick, boolean finale) {
            this.dim = dim;
            this.entries = entries;
            this.after = after;
            this.center = center;
            this.perTick = perTick;
            this.finale = finale;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return dim;
        }

        @Override
        public boolean tick(ServerLevel level) {
            ticks++;
            int end = Math.min(entries.size(), index + perTick);
            for (int i = index; i < end; i++) {
                Map.Entry<BlockPos, BlockState> e = entries.get(i);
                level.setBlock(e.getKey(), e.getValue(), Block.UPDATE_CLIENTS);
                if (needsShapeUpdate(e.getValue())) shapeFix.add(e.getKey());
            }
            if (end > index) {
                BlockPos fx = entries.get(index + level.random.nextInt(end - index)).getKey();
                level.sendParticles(ParticleTypes.CLOUD, fx.getX() + 0.5, fx.getY() + 1.0, fx.getZ() + 0.5, 3, 0.4, 0.2, 0.4, 0.01);
                level.sendParticles(ParticleTypes.HAPPY_VILLAGER, fx.getX() + 0.5, fx.getY() + 1.0, fx.getZ() + 0.5, 2, 0.5, 0.3, 0.5, 0.0);
                if (ticks % 3 == 0) {
                    float progress = end / (float) Math.max(1, entries.size());
                    level.playSound(null, fx, SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 0.8F, 0.7F + progress * 0.9F);
                }
            }
            index = end;
            if (index >= entries.size()) {
                for (BlockPos p : shapeFix) {
                    BlockState s = level.getBlockState(p);
                    BlockState fixed = Block.updateFromNeighbourShapes(s, level, p);
                    if (fixed != s) level.setBlock(p, fixed, Block.UPDATE_CLIENTS);
                }
                for (Consumer<ServerLevel> a : after) a.accept(level);
                if (finale) finale(level, center);
                return true;
            }
            return false;
        }
    }

    private static final class TemplateTask implements Task {
        private final ResourceKey<Level> dim;
        private final StructureTemplate template;
        private final StructurePlaceSettings settings;
        private final BlockPos origin;
        private final BoundingBox box;
        private final List<Consumer<ServerLevel>> after;
        private final BlockPos center;
        private final int interval;
        private int y;
        private int ticks;

        TemplateTask(ResourceKey<Level> dim, StructureTemplate template, StructurePlaceSettings settings, BlockPos origin,
                     BoundingBox box, List<Consumer<ServerLevel>> after, BlockPos center) {
            this.dim = dim;
            this.template = template;
            this.settings = settings;
            this.origin = origin;
            this.box = box;
            this.after = after;
            this.center = center;
            this.y = box.minY();
            this.interval = box.getYSpan() > 30 ? 1 : 3;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return dim;
        }

        @Override
        public boolean tick(ServerLevel level) {
            if (++ticks % interval != 0) return false;
            settings.setBoundingBox(new BoundingBox(box.minX(), y, box.minZ(), box.maxX(), y, box.maxZ()));
            template.placeInWorld(level, origin, origin, settings, level.getRandom(), Block.UPDATE_CLIENTS);
            for (int i = 0; i < 6; i++) {
                double px = box.minX() + level.random.nextDouble() * box.getXSpan();
                double pz = box.minZ() + level.random.nextDouble() * box.getZSpan();
                level.sendParticles(ParticleTypes.CLOUD, px, y + 1.0, pz, 2, 0.3, 0.1, 0.3, 0.01);
            }
            float progress = (y - box.minY()) / (float) Math.max(1, box.getYSpan());
            level.playSound(null, new BlockPos(center.getX(), y, center.getZ()), SoundEvents.STONE_PLACE, SoundSource.BLOCKS, 1.0F, 0.7F + progress * 0.9F);
            y++;
            if (y > box.maxY()) {
                settings.setBoundingBox(null);
                for (Consumer<ServerLevel> a : after) a.accept(level);
                finale(level, center);
                return true;
            }
            return false;
        }
    }

    private static final class StreamTask implements Task {
        private final ResourceKey<Level> dim;
        private final List<Supplier<Task>> stages;
        private final Consumer<Float> progress;
        private final Consumer<ServerLevel> done;
        private int stage;
        private Task current;
        private int lastReported = -1;

        StreamTask(ResourceKey<Level> dim, List<Supplier<Task>> stages, Consumer<Float> progress, Consumer<ServerLevel> done) {
            this.dim = dim;
            this.stages = stages;
            this.progress = progress;
            this.done = done;
        }

        @Override
        public ResourceKey<Level> dimension() {
            return dim;
        }

        @Override
        public boolean tick(ServerLevel level) {
            if (current == null) {
                if (stage >= stages.size()) {
                    done.accept(level);
                    return true;
                }
                current = stages.get(stage).get();
            }
            if (current == null || current.tick(level)) {
                current = null;
                stage++;
                int pct = (int) (stage * 100.0F / stages.size());
                if (pct / 10 != lastReported) {
                    lastReported = pct / 10;
                    progress.accept(stage / (float) stages.size());
                }
            }
            return false;
        }
    }

    private static final class TerrainTask implements Task {
        private static final int COLUMNS_PER_TICK = 1400;
        private final ResourceKey<Level> dim;
        private final BoundingBox area;
        private final int floorY;
        private final int clearHeight;
        private long cursor;
        private final long total;

        TerrainTask(ResourceKey<Level> dim, BoundingBox area, int floorY, int clearHeight) {
            this.dim = dim;
            this.area = area;
            this.floorY = floorY;
            this.clearHeight = clearHeight;
            this.total = (long) area.getXSpan() * area.getZSpan();
        }

        @Override
        public ResourceKey<Level> dimension() {
            return dim;
        }

        @Override
        public boolean tick(ServerLevel level) {
            BlockState air = Blocks.AIR.defaultBlockState();
            BlockState grass = Blocks.GRASS_BLOCK.defaultBlockState();
            BlockState dirt = Blocks.DIRT.defaultBlockState();
            BlockState stone = Blocks.STONE.defaultBlockState();
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            long end = Math.min(total, cursor + COLUMNS_PER_TICK);
            for (long i = cursor; i < end; i++) {
                int x = area.minX() + (int) (i % area.getXSpan());
                int z = area.minZ() + (int) (i / area.getXSpan());
                for (int y = floorY + clearHeight; y > floorY; y--) {
                    m.set(x, y, z);
                    if (!level.getBlockState(m).isAir()) level.setBlock(m, air, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                }
                m.set(x, floorY, z);
                level.setBlock(m, grass, Block.UPDATE_CLIENTS);
                for (int dy = 1; dy <= 4; dy++) {
                    m.set(x, floorY - dy, z);
                    BlockState below = level.getBlockState(m);
                    if (below.isAir() || !below.getFluidState().isEmpty() || below.canBeReplaced()) {
                        level.setBlock(m, dy < 3 ? dirt : stone, Block.UPDATE_CLIENTS);
                    }
                }
            }
            if (end > cursor && level.random.nextInt(3) == 0) {
                int x = area.minX() + level.random.nextInt(area.getXSpan());
                int z = area.minZ() + (int) (end / area.getXSpan()) % Math.max(1, area.getZSpan());
                level.sendParticles(ParticleTypes.CLOUD, x, floorY + 1.5, z, 10, 3.0, 0.5, 3.0, 0.02);
            }
            cursor = end;
            if (cursor >= total) {
                AABB box = new AABB(area.minX(), floorY, area.minZ(), area.maxX() + 1, floorY + clearHeight, area.maxZ() + 1);
                for (Entity e : level.getEntitiesOfClass(ItemEntity.class, box)) e.discard();
                return true;
            }
            return false;
        }
    }

    private static final class EraseTask implements Task {
        private final ResourceKey<Level> dim;
        private final BoundingBox box;
        private final int floorY;
        private final BlockState floor;
        private final Consumer<ServerLevel> after;
        private int y;

        EraseTask(ResourceKey<Level> dim, BoundingBox box, int floorY, BlockState floor, Consumer<ServerLevel> after) {
            this.dim = dim;
            this.box = box;
            this.floorY = floorY;
            this.floor = floor;
            this.after = after;
            this.y = box.maxY();
        }

        @Override
        public ResourceKey<Level> dimension() {
            return dim;
        }

        @Override
        public boolean tick(ServerLevel level) {
            BlockPos.MutableBlockPos m = new BlockPos.MutableBlockPos();
            BlockState air = Blocks.AIR.defaultBlockState();
            for (int layer = 0; layer < 2 && y > floorY; layer++, y--) {
                for (int x = box.minX(); x <= box.maxX(); x++)
                    for (int z = box.minZ(); z <= box.maxZ(); z++) {
                        m.set(x, y, z);
                        if (!level.getBlockState(m).isAir()) level.setBlock(m, air, Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
                    }
                level.sendParticles(ParticleTypes.POOF, (box.minX() + box.maxX()) / 2.0, y, (box.minZ() + box.maxZ()) / 2.0,
                        30, box.getXSpan() / 3.0, 0.3, box.getZSpan() / 3.0, 0.02);
                if (y % 3 == 0) level.playSound(null, new BlockPos(box.getCenter().getX(), y, box.getCenter().getZ()), SoundEvents.STONE_BREAK, SoundSource.BLOCKS, 1.0F, 0.8F);
            }
            if (y <= floorY) {
                for (int x = box.minX(); x <= box.maxX(); x++)
                    for (int z = box.minZ(); z <= box.maxZ(); z++) level.setBlock(new BlockPos(x, floorY, z), floor, Block.UPDATE_CLIENTS);
                AABB aabb = new AABB(box.minX(), floorY, box.minZ(), box.maxX() + 1, box.maxY() + 2, box.maxZ() + 1);
                Predicate<Entity> removable = e -> !(e instanceof Player);
                for (Entity e : level.getEntitiesOfClass(Entity.class, aabb, removable)) e.discard();
                after.accept(level);
                level.playSound(null, new BlockPos(box.getCenter().getX(), floorY, box.getCenter().getZ()), SoundEvents.ENDERMAN_TELEPORT, SoundSource.BLOCKS, 1.0F, 0.7F);
                return true;
            }
            return false;
        }
    }

    public static void finale(ServerLevel level, BlockPos center) {
        level.playSound(null, center, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 1.2F, 0.9F);
        level.playSound(null, center, SoundEvents.PLAYER_LEVELUP, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.playSound(null, center, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, SoundSource.BLOCKS, 0.8F, 1.0F);
        level.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, center.getX() + 0.5, center.getY() + 4.0, center.getZ() + 0.5, 200, 6.0, 3.0, 6.0, 0.4);
        int[][] colors = {{0xFF3030, 0xFFD700}, {0x30FF80, 0xFFFFFF}, {0x30C0FF, 0xFF60FF}};
        for (int i = 0; i < 3; i++) {
            ItemStack rocket = new ItemStack(Items.FIREWORK_ROCKET);
            FireworkExplosion explosion = new FireworkExplosion(FireworkExplosion.Shape.LARGE_BALL,
                    IntList.of(colors[i][0], colors[i][1]), IntList.of(0xFFFFFF), true, true);
            rocket.set(DataComponents.FIREWORKS, new Fireworks(1, List.of(explosion)));
            double ox = (i - 1) * 4.0;
            level.addFreshEntity(new FireworkRocketEntity(level, center.getX() + 0.5 + ox, center.getY() + 2.0, center.getZ() + 0.5, rocket));
        }
    }

    private ConstructionManager() {}
}
