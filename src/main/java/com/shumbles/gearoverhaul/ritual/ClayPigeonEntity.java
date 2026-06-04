package com.shumbles.gearoverhaul.ritual;

import net.minecraft.entity.EntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.PersistentProjectileEntity;
import net.minecraft.entity.projectile.thrown.ThrownItemEntity;
import net.minecraft.item.Item;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * A clay pigeon launched by a Skeet Launcher: a thrown-item entity that arcs up and falls. An
 * arrow passing through it (from a player's bow) "shatters" it as a hit; landing on the ground is
 * a miss. Both report back to the launcher that fired it.
 */
public class ClayPigeonEntity extends ThrownItemEntity {
	private BlockPos launcher;

	public ClayPigeonEntity(EntityType<? extends ThrownItemEntity> type, World world) {
		super(type, world);
	}

	public void setLauncher(BlockPos pos) {
		this.launcher = pos;
	}

	@Override
	protected Item getDefaultItem() {
		return RitualItems.CLAY_PIGEON;
	}

	@Override
	public void tick() {
		super.tick();
		World world = getEntityWorld();
		if (world.isClient() || !isAlive()) {
			return;
		}
		// Detect an arrow sweeping through this tick (segment test, so fast arrows don't tunnel).
		Box box = getBoundingBox().expand(0.3);
		for (PersistentProjectileEntity arrow : world.getEntitiesByClass(PersistentProjectileEntity.class,
				box.expand(2.0), a -> a.isAlive() && a.getOwner() instanceof PlayerEntity)) {
			Vec3d pos = new Vec3d(arrow.getX(), arrow.getY(), arrow.getZ());
			Vec3d prev = pos.subtract(arrow.getVelocity());
			if (box.raycast(prev, pos).isPresent()) {
				onShot((ServerWorld) world, (PlayerEntity) arrow.getOwner());
				arrow.discard();
				return;
			}
		}
	}

	@Override
	protected void onCollision(HitResult hitResult) {
		// Only the ground matters (a miss); pass through entities untouched.
		if (hitResult.getType() == HitResult.Type.BLOCK && !getEntityWorld().isClient()) {
			onMiss((ServerWorld) getEntityWorld());
		}
	}

	@Override
	protected void onBlockHit(BlockHitResult blockHitResult) {
		if (!getEntityWorld().isClient()) {
			onMiss((ServerWorld) getEntityWorld());
		}
	}

	private void onShot(ServerWorld world, PlayerEntity shooter) {
		world.playSound(null, getBlockPos(), SoundEvents.BLOCK_DECORATED_POT_SHATTER, SoundCategory.PLAYERS, 1.0f, 1.3f);
		world.spawnParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 12, 0.2, 0.2, 0.2, 0.05);
		if (launcher != null && world.getBlockEntity(launcher) instanceof SkeetLauncherBlockEntity be) {
			be.recordHit(world, shooter);
		}
		discard();
	}

	private void onMiss(ServerWorld world) {
		world.playSound(null, getBlockPos(), SoundEvents.BLOCK_GLASS_BREAK, SoundCategory.BLOCKS, 1.0f, 0.9f);
		world.spawnParticles(ParticleTypes.POOF, getX(), getY(), getZ(), 8, 0.2, 0.1, 0.2, 0.02);
		if (launcher != null && world.getBlockEntity(launcher) instanceof SkeetLauncherBlockEntity be) {
			be.recordMiss(world);
		}
		discard();
	}
}
